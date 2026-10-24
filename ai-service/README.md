# SentinelX AI / ML Microservice (Phase 8)

Local-first, zero-cost machine learning service for supervised network-flow threat classification (CIC-IDS2017 schema) and local LLM incident summarization via Ollama.

---

## 🚀 Step-by-Step Guide to Train and Run the Model

Follow these steps directly in your terminal to train the model and start the inference microservice.

### Step 1: Open Terminal & Navigate to `ai-service`
Open PowerShell or Command Prompt:
```powershell
cd "c:\Users\kunal\OneDrive - Vidyalankar Institute of Technology\Desktop\SentinelX\SentinelX\ai-service"
```

### Step 2: Create and Activate Virtual Environment
```powershell
py -m venv venv
.\venv\Scripts\activate
```
*(When activated, your prompt will show `(venv)` at the beginning).*

### Step 3: Install Required Libraries
```powershell
pip install -r requirements.txt
```

---

### Step 4: Run Model Training & Evaluation in Terminal

You can choose either of the following commands:

#### Option A: Train Baseline Random Forest (Recommended)
Automatically generates 12,000 synthetic flow samples matching the CIC-IDS2017 schema, audits for leakage, trains scikit-learn Random Forest, and outputs per-class metrics and confusion matrix:
```powershell
py train.py --model rf
```

#### Option B: Compare Random Forest vs XGBoost Side-by-Side
Evaluates both Random Forest and XGBoost on the exact same train/validation/test split and selects the highest Macro-F1 model:
```powershell
py train.py --model both
```

#### What you will see in the terminal:
1. **Class Distribution Breakdown**: Proportion of `BENIGN`, `PortScan`, `DoS-Hulk`, `DDoS`, and `SSH-Patator` samples.
2. **Leakage Shield Audit**: Confirms source/destination IP addresses, timestamps, and identifiers were dropped.
3. **Partitioning**: 70% Train, 15% Validation, 15% Held-out Test split.
4. **Fitting Progress**: Preprocessing pipeline and tree ensemble execution.
5. **Evaluation Output**:
   - ASCII Confusion Matrix
   - Precision, Recall, and F1-score for each threat class
   - Macro-average F1 and Weighted-average F1
   - Microsecond inference latency benchmark
6. **Artifact Export**: Saves `artifacts/model_v1.joblib` and `artifacts/model_metadata.json`.

---

### Step 5: Start the FastAPI Inference Microservice
Once training completes, start the local API service:
```powershell
py -m uvicorn main:app --port 8000 --reload
```

The service will be live at:
- **Health Check & Model Metadata**: `http://localhost:8000/health`
- **Interactive Swagger API Docs**: `http://localhost:8000/docs`

---

### Step 6: Test Model Inference (Optional Curl / PowerShell)

In a second terminal window:
```powershell
curl -X POST "http://localhost:8000/api/v1/score" -H "Content-Type: application/json" -d "{\"destination_port\": 80, \"flow_duration\": 200, \"total_fwd_packets\": 1, \"total_bwd_packets\": 0, \"syn_flag_count\": 1, \"ack_flag_count\": 0}"
```

---

### Step 7: (Optional) Local LLM Summaries with Ollama
SentinelX supports local LLM incident summaries at ₹0 cost.
1. Download Ollama from [https://ollama.com](https://ollama.com).
2. Run:
   ```bash
   ollama run qwen2.5:3b
   ```
3. SentinelX's `/api/v1/summarize` endpoint will automatically detect Ollama running on port 11434 and generate natural language triage narratives! If Ollama is not installed, it gracefully falls back to structured expert triage templates.
