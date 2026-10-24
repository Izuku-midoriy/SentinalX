# 11 — AI/ML Model Strategy

- Product: SentinelX
- Status: Proposed baseline; models are not yet trained or validated
- Cost requirement: No paid APIs or services required
- Last updated: 2026-10-12

## 1. Model strategy

SentinelX uses distinct approaches for distinct tasks. Deterministic detection rules remain the primary mechanism for known security patterns. A supervised machine-learning classifier can classify network-flow records that match its training schema. An optional local large language model can summarize evidence for analysts.

Do not average scores from models that analyze different input types. Preserve each finding, its source, model/rule version, and supporting evidence. Any correlation must use explicit, documented logic.

## 2. Planned models

### Model A — Random Forest baseline
- Framework: scikit-learn
- Task: Supervised classification of network-flow records into the classes supported by the selected dataset.
- Dataset candidate: CIC-IDS2017.
- Purpose: Establish a reproducible baseline before trying more complex models.
- Output: Predicted class and calibrated/appropriately interpreted confidence only if the probability behavior is validated.
- Status: Planned; not trained or benchmarked.

### Model B — XGBoost comparison
- Framework: XGBoost, subject to dependency compatibility and license review.
- Task: Same feature and label task as the Random Forest model.
- Purpose: Compare performance, inference cost, model size, and class-wise errors on the same data split.
- Selection: Choose based on held-out metrics and operational constraints, not popularity or training accuracy alone.
- Status: Planned; not trained or benchmarked.

### Model C — Optional local LLM for analyst assistance
- Initial candidate: Qwen3 4B Instruct through Ollama, subject to verifying the exact model tag, model license, and hardware compatibility at implementation time.
- Task: Summarize supplied alert evidence and propose investigation steps.
- It is not the primary detector and does not decide permissions or execute commands.
- Inputs must be bounded and minimized; log contents are untrusted and may contain prompt-injection attempts.
- Output must be labeled as AI-generated and grounded in event/alert identifiers.
- Status: Optional candidate; not installed or validated.

### Optional anomaly detection
Only add an anomaly detector after a clear input domain and evaluation method are defined. Do not treat an anomaly score as a probability of maliciousness without validation.

## 3. Dataset plan

CIC-IDS2017 is the initial candidate for network-flow classification. Before use:
1. Read the official dataset description and terms.
2. Confirm the feature definitions and labels.
3. Inspect class imbalance, missing values, duplicates, and possible label artifacts.
4. Remove identifiers or features that leak the target or make train/test evaluation unrealistic.
5. Split data in a way that reduces leakage; consider time/day or scenario-based splits where appropriate.
6. Fit preprocessing only on the training partition.
7. Record dataset version/source, preprocessing version, split method, and random seeds.
8. Do not publish raw data unless its terms permit it.

Important scope limitation: CIC-IDS2017 is network-flow data. A model trained on it must not be described as a general classifier for arbitrary Windows, Linux, authentication, or application logs. Those require different features and potentially separate datasets/models.

Official dataset page: https://www.unb.ca/cic/datasets/ids-2017.html

## 4. Training and evaluation pipeline

1. Download dataset manually from its official source after reviewing terms.
2. Create a reproducible preprocessing script.
3. Define target labels and document any class grouping.
4. Create train/validation/test splits before fitting preprocessing.
5. Train a simple baseline and Random Forest.
6. Train XGBoost using the same split and comparable evaluation.
7. Report confusion matrix, precision, recall, F1-score per class, macro-F1, weighted-F1, and inference time.
8. Investigate false negatives and false positives; do not rely on accuracy alone.
9. Compare to a simple baseline.
10. Save the model artifact with feature schema, preprocessing pipeline, library versions, dataset reference, model version, and metrics.
11. Add tests for input validation, expected output schema, reproducibility, and model loading.
12. Integrate through a bounded FastAPI endpoint only after offline evaluation passes.

Avoid data leakage: do not fit scalers/encoders on the full dataset before splitting, and do not let near-duplicate flows from the same capture scenario make the test set unrealistically easy.

## 5. Inference contract

The model service should accept only the documented numeric/categorical feature schema used during training. It should:
- Validate required fields, ranges, and payload size.
- Return model name/version, predicted class, and documented score semantics.
- Reject incompatible input rather than guessing or silently filling arbitrary values.
- Apply timeouts and bounded concurrency.
- Log operational metadata without storing sensitive raw payloads unnecessarily.
- Fail gracefully; deterministic detection rules continue to work if AI is unavailable.

## 6. SentinelX integration

- Rules detect known patterns from normalized events and event history.
- The network-flow classifier handles only records converted to its training schema.
- AI output is stored separately from rule evidence.
- Correlation logic must document which event identifiers, time windows, and conditions connect findings.
- Never average unrelated model scores.
- No model may authorize users, alter tenant scope, or execute shell/containment commands.

## 7. Acceptance criteria

- [ ] Dataset terms and provenance reviewed.
- [ ] Preprocessing and split are reproducible.
- [ ] Leakage risks are documented and tested where possible.
- [ ] Baseline, Random Forest, and XGBoost results are compared fairly.
- [ ] Per-class metrics and confusion matrix are recorded.
- [ ] Model artifact contains version and feature schema.
- [ ] FastAPI input validation and tests pass.
- [ ] Rule-based detection works with the AI service stopped.
- [ ] UI labels model output as a model prediction, not confirmed fact.
- [ ] No paid API is required.
