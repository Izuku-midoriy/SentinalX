import os
from contextlib import asynccontextmanager
from typing import List
import httpx
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from config import OLLAMA_BASE_URL, OLLAMA_MODEL, SERVICE_HOST, SERVICE_PORT
from model_loader import model_manager
from schemas import (
    BatchScoreRequest,
    BatchScoreResponse,
    HealthResponse,
    IncidentSummaryRequest,
    IncidentSummaryResponse,
    NetworkFlowFeatures,
    ScoreResponse,
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    print("[*] Starting SentinelX Python AI / ML Service...")
    model_manager.reload()
    yield
    print("[*] Shutting down SentinelX Python AI / ML Service...")


app = FastAPI(
    title="SentinelX AI/ML Service",
    description="Local-first microservice for network flow threat classification and LLM incident summarization.",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


async def check_ollama_availability() -> bool:
    try:
        async with httpx.AsyncClient(timeout=1.0) as client:
            resp = await client.get(f"{OLLAMA_BASE_URL}/api/version")
            return resp.status_code == 200
    except Exception:
        return False


@app.get("/health", response_model=HealthResponse)
async def health_check():
    ollama_ok = await check_ollama_availability()
    return HealthResponse(
        status="UP",
        model_loaded=model_manager.is_loaded,
        model_name=model_manager.model_name,
        model_version=model_manager.model_version,
        classes=model_manager.classes,
        ollama_connected=ollama_ok
    )


@app.post("/api/v1/score", response_model=ScoreResponse)
async def score_flow(flow: NetworkFlowFeatures):
    """
    Score a single network flow record against the trained CIC-IDS2017 classifier.
    Returns prediction, calibrated risk score, confidence, and explanation.
    """
    return model_manager.predict_flow(flow)


@app.post("/api/v1/score/batch", response_model=BatchScoreResponse)
async def score_batch(request: BatchScoreRequest):
    """Score a batch of network flow records."""
    if len(request.flows) > 500:
        raise HTTPException(status_code=400, detail="Batch size limited to 500 flows per request.")
    results = [model_manager.predict_flow(f) for f in request.flows]
    return BatchScoreResponse(
        model_name=model_manager.model_name,
        model_version=model_manager.model_version,
        results=results
    )


@app.post("/api/v1/model/reload")
async def reload_model():
    """Reloads the serialized model artifact from disk without restarting the server."""
    model_manager.reload()
    return {
        "status": "reloaded",
        "model_loaded": model_manager.is_loaded,
        "model_name": model_manager.model_name,
        "model_version": model_manager.model_version
    }


@app.post("/api/v1/summarize", response_model=IncidentSummaryResponse)
async def summarize_incident(req: IncidentSummaryRequest):
    """
    Summarizes incident telemetry into actionable triage notes.
    Queries local Ollama instance if available, otherwise generates a deterministic expert template.
    """
    ollama_ok = await check_ollama_availability()

    if ollama_ok:
        try:
            prompt = (
                f"You are a Senior SOC Analyst assistant for SentinelX.\n"
                f"Analyze the following security incident and provide a concise 3-paragraph triage summary:\n"
                f"- Incident Title: {req.title}\n"
                f"- Severity: {req.severity}\n"
                f"- Description: {req.description or 'None'}\n"
                f"- Linked Alert Count: {req.alert_count}\n"
                f"- Alert Samples: {', '.join(req.sample_alert_titles[:5])}\n"
                f"- Source IPs: {', '.join(req.source_ips[:5])}\n"
                f"- Destination IPs: {', '.join(req.destination_ips[:5])}\n\n"
                f"Format with:\n"
                f"1. Executive Summary\n"
                f"2. Observed Indicators & Patterns\n"
                f"3. Recommended Containment / Remediation Steps."
            )

            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.post(
                    f"{OLLAMA_BASE_URL}/api/generate",
                    json={
                        "model": OLLAMA_MODEL,
                        "prompt": prompt,
                        "stream": False
                    }
                )
                if resp.status_code == 200:
                    ollama_resp = resp.json().get("response", "")
                    return IncidentSummaryResponse(
                        incident_id=req.incident_id,
                        ai_provider="local-ollama",
                        model_name=OLLAMA_MODEL,
                        summary_markdown=ollama_resp,
                        suggested_actions=[
                            "Verify affected host logs",
                            "Cross-check source IPs against local threat intel",
                            "Quarantine host if multiple high-severity alerts persist"
                        ]
                    )
        except Exception as e:
            print(f"[!] Ollama call failed, falling back to local template: {e}")

    # Graceful fallback: Structured expert triage template
    src_str = ", ".join(req.source_ips[:3]) if req.source_ips else "Internal Host"
    dst_str = ", ".join(req.destination_ips[:3]) if req.destination_ips else "Target Services"

    summary_md = (
        f"### 🛡️ Incident Analysis: {req.title}\n\n"
        f"**Severity Level:** `{req.severity}` | **Alert Volume:** `{req.alert_count} linked alerts`\n\n"
        f"#### 1. Executive Summary\n"
        f"This incident escalated from **{req.alert_count} correlated alerts**, primarily concerning "
        f"events targeting `{dst_str}` from `{src_str}`. The activity exhibits indicators of "
        f"repeated probing or automated threat actor behavior.\n\n"
        f"#### 2. Key Evidence & Correlated Indicators\n"
        f"- **Primary Target(s):** `{dst_str}`\n"
        f"- **Identified Threat Source(s):** `{src_str}`\n"
        f"- **Correlated Rules:** {', '.join(req.sample_alert_titles[:3]) if req.sample_alert_titles else 'Deterministic Detection Rules'}\n\n"
        f"#### 3. Recommended Investigation Steps\n"
        f"1. Verify authentication logs on destination systems for anomalous successful logins.\n"
        f"2. Check if source IP `{src_str}` is external or an infected internal host.\n"
        f"3. Apply firewall/containment rule if brute force or scanning continues across tenants."
    )

    suggested = [
        f"Inspect network traffic for source IP(s): {src_str}",
        "Confirm if any credentials were breached in authentication logs",
        "Mark incident as UNDER_INVESTIGATION in SOC console"
    ]

    return IncidentSummaryResponse(
        incident_id=req.incident_id,
        ai_provider="sentinelx-heuristic-triage",
        model_name="deterministic-expert-v1",
        summary_markdown=summary_md,
        suggested_actions=suggested
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host=SERVICE_HOST, port=SERVICE_PORT, reload=True)
