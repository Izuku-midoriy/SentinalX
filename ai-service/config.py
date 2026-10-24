import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
ARTIFACTS_DIR = BASE_DIR / "artifacts"
DATA_DIR = BASE_DIR / "data"

MODEL_FILE = ARTIFACTS_DIR / "model_v1.joblib"
METADATA_FILE = ARTIFACTS_DIR / "model_metadata.json"
FEATURE_SCHEMA_FILE = ARTIFACTS_DIR / "feature_schema.json"

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:3b")

SERVICE_HOST = os.getenv("AI_SERVICE_HOST", "0.0.0.0")
SERVICE_PORT = int(os.getenv("AI_SERVICE_PORT", "8000"))

# Ensure directories exist
ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)
DATA_DIR.mkdir(parents=True, exist_ok=True)
