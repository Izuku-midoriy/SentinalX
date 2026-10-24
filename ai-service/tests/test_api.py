import pytest
from fastapi.testclient import TestClient
import sys
from pathlib import Path

# Add parent directory to sys.path
sys.path.append(str(Path(__file__).resolve().parent.parent))

from main import app

client = TestClient(app)


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert "classes" in data
    assert isinstance(data["classes"], list)


def test_score_flow_endpoint():
    payload = {
        "destination_port": 80,
        "flow_duration": 1500.0,
        "total_fwd_packets": 5,
        "total_bwd_packets": 5,
        "total_length_fwd_packets": 250.0,
        "total_length_bwd_packets": 1200.0,
        "flow_bytes_per_s": 1450.0,
        "flow_packets_per_s": 10.0,
        "syn_flag_count": 1,
        "ack_flag_count": 1
    }
    response = client.post("/api/v1/score", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "prediction" in data
    assert "risk_score" in data
    assert 0 <= data["risk_score"] <= 100
    assert 0.0 <= data["confidence"] <= 1.0
    assert "explanation" in data


def test_score_flow_invalid_port():
    payload = {
        "destination_port": 999999  # Invalid port
    }
    response = client.post("/api/v1/score", json=payload)
    assert response.status_code == 422  # Pydantic validation error


def test_summarize_incident():
    payload = {
        "incident_id": "test-inc-123",
        "title": "Multiple SSH Authentication Failures",
        "severity": "HIGH",
        "description": "50 failed attempts within 2 minutes",
        "alert_count": 3,
        "sample_alert_titles": ["Brute Force SSH Attack", "High Ingestion Volume"],
        "source_ips": ["192.168.1.100"],
        "destination_ips": ["10.0.0.5"]
    }
    response = client.post("/api/v1/summarize", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["incident_id"] == "test-inc-123"
    assert "summary_markdown" in data
    assert len(data["suggested_actions"]) > 0
