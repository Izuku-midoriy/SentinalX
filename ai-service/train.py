import argparse
import json
import time
from datetime import datetime
from pathlib import Path
import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.metrics import classification_report, confusion_matrix, f1_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from config import ARTIFACTS_DIR, DATA_DIR, FEATURE_SCHEMA_FILE, METADATA_FILE, MODEL_FILE
from generate_dataset import generate_dataset

# Identifiers to exclude to prevent data leakage and memorization
LEAKAGE_COLUMNS = [
    "src_ip", "source_ip", "dst_ip", "destination_ip",
    "timestamp", "flow_id", "id", "tenant_id"
]


def print_banner(text: str):
    print("\n" + "=" * 70)
    print(f" {text}")
    print("=" * 70)


def format_confusion_matrix(cm: np.ndarray, labels: list) -> str:
    lines = []
    header = f"{'True \\ Pred':<15}" + "".join(f"{lbl[:10]:>12}" for lbl in labels)
    lines.append(header)
    lines.append("-" * len(header))
    for i, row_lbl in enumerate(labels):
        row_str = f"{row_lbl[:14]:<15}" + "".join(f"{cm[i, j]:>12}" for j in range(len(labels)))
        lines.append(row_str)
    return "\n".join(lines)


def train_and_evaluate(data_path: Path, model_type: str = "rf", output_dir: Path = None):
    if output_dir is None:
        output_dir = ARTIFACTS_DIR
    output_dir.mkdir(parents=True, exist_ok=True)

    print_banner("SENTINELX AI/ML ENGINE — CIC-IDS2017 NETWORK FLOW TRAINER")

    # Step 1: Check / Load Dataset
    if not data_path.exists():
        print(f"[!] Dataset not found at '{data_path}'.")
        print("[*] Generating synthetic baseline dataset adhering to CIC-IDS2017 schema...")
        data_path = generate_dataset(total_rows=12000, output_path=data_path)

    print(f"\n[Step 1/6] Loading dataset from: {data_path}")
    df = pd.read_csv(data_path)
    print(f"    - Raw samples loaded: {len(df):,}")
    print(f"    - Columns present: {len(df.columns)}")

    # Target column extraction
    target_col = "label" if "label" in df.columns else "Label"
    if target_col not in df.columns:
        raise ValueError(f"Target column 'label' not found in dataset. Columns: {list(df.columns)}")

    y = df[target_col].astype(str)
    X = df.drop(columns=[target_col])

    # Step 2: Leakage Audit
    print("\n[Step 2/6] Auditing features for potential target/identifier leakage...")
    dropped_cols = []
    for col in X.columns:
        if col.lower() in LEAKAGE_COLUMNS:
            dropped_cols.append(col)
    if dropped_cols:
        print(f"    - [LEAKAGE SHIELD] Dropped {len(dropped_cols)} identifier columns: {dropped_cols}")
        X = X.drop(columns=dropped_cols)
    else:
        print("    - [PASS] Zero identifier leakage columns detected.")

    # Clean non-numeric or infinite values
    X = X.replace([np.inf, -np.inf], np.nan)
    feature_names = list(X.columns)
    print(f"    - Final clean feature count: {len(feature_names)}")

    # Class distribution
    print("\n[Step 3/6] Class distribution breakdown:")
    class_counts = y.value_counts()
    for cls_name, count in class_counts.items():
        pct = (count / len(y)) * 100
        print(f"    - {cls_name:<16}: {count:>6,} ({pct:>5.1f}%)")

    # Step 4: Stratified Train / Val / Test Partitioning
    print("\n[Step 4/6] Creating stratified 70% Train, 15% Validation, 15% Test splits...")
    X_train, X_temp, y_train, y_temp = train_test_split(
        X, y, test_size=0.30, random_state=42, stratify=y
    )
    X_val, X_test, y_val, y_test = train_test_split(
        X_temp, y_temp, test_size=0.50, random_state=42, stratify=y_temp
    )
    print(f"    - Training set:   {len(X_train):>6,} samples")
    print(f"    - Validation set: {len(X_val):>6,} samples")
    print(f"    - Test set:       {len(X_test):>6,} samples")

    # Step 5: Model Training
    models_to_run = []
    if model_type in ["rf", "both"]:
        models_to_run.append(("RandomForest", RandomForestClassifier(
            n_estimators=100,
            max_depth=15,
            min_samples_split=5,
            class_weight="balanced_subsample",
            random_state=42,
            n_jobs=-1
        )))

    if model_type in ["xgb", "both"]:
        try:
            from xgboost import XGBClassifier
            from sklearn.preprocessing import LabelEncoder
            models_to_run.append(("XGBoost", "xgb_model"))
        except ImportError:
            print("    [!] XGBoost package not installed. Skipping XGBoost comparison.")

    results = {}
    best_pipeline = None
    best_f1 = -1.0
    best_name = ""

    classes_sorted = sorted(list(np.unique(y_train)))

    for name, clf in models_to_run:
        print_banner(f"Training & Benchmarking: {name}")
        t0 = time.time()

        if name == "RandomForest":
            pipeline = Pipeline([
                ("imputer", SimpleImputer(strategy="median")),
                ("scaler", StandardScaler()),
                ("classifier", clf)
            ])
            print("    - Fitting preprocessing pipeline and Random Forest trees...")
            pipeline.fit(X_train, y_train)
            train_duration = time.time() - t0
            print(f"    - Training finished in {train_duration:.2f} seconds.")

            # Validation evaluation
            y_val_pred = pipeline.predict(X_val)
            val_f1 = f1_score(y_val, y_val_pred, average="macro")
            print(f"    - Validation Macro-F1: {val_f1:.4f}")

            # Test evaluation
            t_inf_start = time.time()
            y_test_pred = pipeline.predict(X_test)
            inf_duration = time.time() - t_inf_start
            us_per_sample = (inf_duration / len(X_test)) * 1e6

            test_f1_macro = f1_score(y_test, y_test_pred, average="macro")
            test_f1_weighted = f1_score(y_test, y_test_pred, average="weighted")

            cm = confusion_matrix(y_test, y_test_pred, labels=classes_sorted)
            report = classification_report(y_test, y_test_pred, labels=classes_sorted, output_dict=True)

            print("\n--- Confusion Matrix (Held-out Test Partition) ---")
            print(format_confusion_matrix(cm, classes_sorted))

            print("\n--- Classification Report (Per-Class Precision, Recall, F1) ---")
            print(classification_report(y_test, y_test_pred, labels=classes_sorted, digits=4))

            print(f"[*] Macro-Average F1:    {test_f1_macro:.4f}")
            print(f"[*] Weighted-Average F1: {test_f1_weighted:.4f}")
            print(f"[*] Inference Latency:   {us_per_sample:.2f} µs/sample")

            results[name] = {
                "macro_f1": float(test_f1_macro),
                "weighted_f1": float(test_f1_weighted),
                "us_per_sample": float(us_per_sample),
                "train_duration_s": float(train_duration),
                "classification_report": report
            }

            if test_f1_macro > best_f1:
                best_f1 = test_f1_macro
                best_pipeline = pipeline
                best_name = name

        elif name == "XGBoost":
            from xgboost import XGBClassifier
            from sklearn.preprocessing import LabelEncoder

            le = LabelEncoder()
            y_train_enc = le.fit_transform(y_train)
            y_val_enc = le.transform(y_val)
            y_test_enc = le.transform(y_test)

            xgb_clf = XGBClassifier(
                n_estimators=100,
                max_depth=6,
                learning_rate=0.1,
                eval_metric="mlogloss",
                random_state=42,
                n_jobs=-1
            )
            pipeline = Pipeline([
                ("imputer", SimpleImputer(strategy="median")),
                ("scaler", StandardScaler()),
                ("classifier", xgb_clf)
            ])
            print("    - Fitting preprocessing pipeline and XGBoost gradient-boosted trees...")
            pipeline.fit(X_train, y_train_enc)
            train_duration = time.time() - t0
            print(f"    - Training finished in {train_duration:.2f} seconds.")

            y_test_pred_enc = pipeline.predict(X_test)
            y_test_pred = le.inverse_transform(y_test_pred_enc)

            test_f1_macro = f1_score(y_test, y_test_pred, average="macro")
            test_f1_weighted = f1_score(y_test, y_test_pred, average="weighted")
            report = classification_report(y_test, y_test_pred, labels=classes_sorted, output_dict=True)

            print(f"\n[*] XGBoost Macro-Average F1:    {test_f1_macro:.4f}")
            print(f"[*] XGBoost Weighted-Average F1: {test_f1_weighted:.4f}")

            results[name] = {
                "macro_f1": float(test_f1_macro),
                "weighted_f1": float(test_f1_weighted),
                "train_duration_s": float(train_duration),
                "classification_report": report
            }

            if test_f1_macro > best_f1:
                best_f1 = test_f1_macro
                best_pipeline = pipeline
                best_name = name

    # Step 6: Artifact Serialization & Versioning
    print_banner("Step 6: Serializing Model Artifact & Feature Contract")
    target_model_file = output_dir / "model_v1.joblib"
    target_metadata_file = output_dir / "model_metadata.json"
    target_schema_file = output_dir / "feature_schema.json"

    print(f"[*] Saving best model ({best_name}) to: {target_model_file}")
    joblib.dump(best_pipeline, target_model_file)

    metadata = {
        "model_name": f"sentinelx-flow-{best_name.lower()}",
        "model_version": "1.0.0",
        "created_at": datetime.utcnow().isoformat() + "Z",
        "framework": "scikit-learn" if best_name == "RandomForest" else "xgboost",
        "primary_model": best_name,
        "dataset_origin": "CIC-IDS2017 schema (leakage-shielded)",
        "classes": classes_sorted,
        "features": feature_names,
        "feature_count": len(feature_names),
        "split_strategy": "stratified 70/15/15",
        "metrics": results,
        "hardware_target": "CPU (zero-cost local inference)"
    }

    with open(target_metadata_file, "w") as f:
        json.dump(metadata, f, indent=2)
    print(f"[+] Metadata and evaluation metrics saved to: {target_metadata_file}")

    schema_contract = {
        "version": "1.0.0",
        "required_features": feature_names,
        "feature_count": len(feature_names),
        "target_classes": classes_sorted
    }
    with open(target_schema_file, "w") as f:
        json.dump(schema_contract, f, indent=2)
    print(f"[+] Feature contract saved to: {target_schema_file}")

    print_banner("TRAINING COMPLETE & MODEL SERIALIZED")
    print("Next steps:")
    print("1. Start the FastAPI microservice:")
    print("   py -m uvicorn main:app --port 8000 --reload")
    print("2. Test live scoring:")
    print("   curl http://localhost:8000/health")
    print("======================================================================\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Train SentinelX Supervised Network Flow Classifier")
    parser.add_argument("--data-path", type=str, default=str(DATA_DIR / "cic_sample.csv"),
                        help="Path to training CSV (defaults to data/cic_sample.csv)")
    parser.add_argument("--model", type=str, choices=["rf", "xgb", "both"], default="rf",
                        help="Model to train: 'rf' (Random Forest), 'xgb' (XGBoost), or 'both'")
    parser.add_argument("--output-dir", type=str, default=str(ARTIFACTS_DIR),
                        help="Directory to save model artifacts")
    args = parser.parse_args()

    train_and_evaluate(
        data_path=Path(args.data_path),
        model_type=args.model,
        output_dir=Path(args.output_dir)
    )
