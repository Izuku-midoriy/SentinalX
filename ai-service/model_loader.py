import json
from pathlib import Path
from typing import Dict, List, Optional, Tuple
import joblib
import numpy as np
import pandas as pd

from config import ARTIFACTS_DIR, METADATA_FILE, MODEL_FILE
from schemas import NetworkFlowFeatures, ScoreResponse


class ModelManager:
    def __init__(self):
        self.pipeline = None
        self.metadata = {}
        self.classes: List[str] = []
        self.model_name: str = "uninitialized"
        self.model_version: str = "none"
        self.is_loaded: bool = False
        self.reload()

    def reload(self):
        """Loads or reloads the serialized joblib pipeline and metadata."""
        if MODEL_FILE.exists() and METADATA_FILE.exists():
            try:
                self.pipeline = joblib.load(MODEL_FILE)
                with open(METADATA_FILE, "r") as f:
                    self.metadata = json.load(f)
                self.model_name = self.metadata.get("model_name", "sentinelx-flow-model")
                self.model_version = self.metadata.get("model_version", "1.0.0")
                self.classes = self.metadata.get("classes", [])
                self.is_loaded = True
                print(f"[+] Loaded ML model: {self.model_name} (v{self.model_version}) with {len(self.classes)} classes.")
            except Exception as e:
                print(f"[!] Failed to load model artifact: {e}")
                self.is_loaded = False
        else:
            self.is_loaded = False
            self.model_name = "sentinelx-heuristic-baseline"
            self.model_version = "0.0.1-untrained"
            self.classes = ["BENIGN", "PortScan", "DoS-Hulk", "DDoS", "SSH-Patator"]

    def predict_flow(self, flow: NetworkFlowFeatures) -> ScoreResponse:
        """
        Infers attack prediction and calibrated risk score for a single network flow.
        Gracefully falls back to heuristic scoring if model has not been trained yet.
        """
        flow_dict = {k: v for k, v in flow.model_dump().items() if v is not None}

        if self.is_loaded and self.pipeline is not None:
            df = pd.DataFrame([flow_dict])
            # Ensure only expected features are fed
            if "features" in self.metadata:
                expected_cols = self.metadata["features"]
                # Missing columns get imputed by the pipeline
                for c in expected_cols:
                    if c not in df.columns:
                        df[c] = np.nan
                df = df[expected_cols]

            pred_class = str(self.pipeline.predict(df)[0])

            # Class probabilities
            if hasattr(self.pipeline, "predict_proba"):
                probs = self.pipeline.predict_proba(df)[0]
                classes = list(self.pipeline.classes_)
                prob_dict = {cls: float(probs[i]) for i, cls in enumerate(classes)}
                confidence = float(max(probs))
            else:
                prob_dict = {pred_class: 1.0}
                confidence = 0.85

            is_threat = (pred_class != "BENIGN")

            # Risk score calculation (0 - 100)
            if not is_threat:
                risk_score = int(round((1.0 - confidence) * 20))  # Max 20 for benign
                explanation = f"Flow classified as {pred_class} with {confidence*100:.1f}% confidence."
            else:
                base_weight = 80 if pred_class in ["DoS-Hulk", "DDoS"] else 65
                risk_score = min(100, int(round(base_weight * confidence + 10)))
                explanation = (
                    f"ML model flagged potential {pred_class} network attack "
                    f"with {confidence*100:.1f}% confidence. (Port {flow.destination_port}, "
                    f"{flow.total_fwd_packets} fwd pkts, {flow.flow_bytes_per_s:.1f} B/s)."
                )

            return ScoreResponse(
                model_name=self.model_name,
                model_version=self.model_version,
                prediction=pred_class,
                is_threat=is_threat,
                risk_score=risk_score,
                confidence=round(confidence, 4),
                class_probabilities={k: round(v, 4) for k, v in prob_dict.items()},
                explanation=explanation,
                model_status="active"
            )

        else:
            # Fallback heuristic when model is not yet trained by user
            return self._heuristic_fallback(flow)

    def _heuristic_fallback(self, flow: NetworkFlowFeatures) -> ScoreResponse:
        """Lightweight heuristic fallback when user hasn't executed train.py yet."""
        is_threat = False
        pred_class = "BENIGN"
        confidence = 0.50
        risk_score = 10
        explanation = "Model artifact not yet trained. Run 'py ai-service/train.py' to activate full ML classifier."

        # Basic heuristic flags
        if flow.syn_flag_count > 0 and flow.ack_flag_count == 0 and flow.total_bwd_packets == 0 and flow.flow_duration < 500:
            pred_class = "PortScan"
            is_threat = True
            confidence = 0.70
            risk_score = 65
            explanation = "Heuristic check flagged potential PortScan (SYN without ACK, zero backward packets)."
        elif flow.flow_bytes_per_s > 100000 or flow.flow_packets_per_s > 500:
            pred_class = "DoS-Hulk"
            is_threat = True
            confidence = 0.75
            risk_score = 80
            explanation = "Heuristic check flagged abnormally high throughput / packet velocity characteristic of DoS."

        return ScoreResponse(
            model_name=self.model_name,
            model_version=self.model_version,
            prediction=pred_class,
            is_threat=is_threat,
            risk_score=risk_score,
            confidence=confidence,
            class_probabilities={cls: (0.8 if cls == pred_class else 0.05) for cls in self.classes},
            explanation=explanation,
            model_status="fallback_untrained"
        )


model_manager = ModelManager()
