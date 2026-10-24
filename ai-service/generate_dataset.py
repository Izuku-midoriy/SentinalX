import argparse
import random
from pathlib import Path
import numpy as np
import pandas as pd

from config import DATA_DIR

COLUMNS = [
    "destination_port",
    "flow_duration",
    "total_fwd_packets",
    "total_bwd_packets",
    "total_length_fwd_packets",
    "total_length_bwd_packets",
    "fwd_packet_length_max",
    "fwd_packet_length_min",
    "fwd_packet_length_mean",
    "fwd_packet_length_std",
    "bwd_packet_length_max",
    "bwd_packet_length_min",
    "bwd_packet_length_mean",
    "bwd_packet_length_std",
    "flow_bytes_per_s",
    "flow_packets_per_s",
    "flow_iat_mean",
    "flow_iat_std",
    "flow_iat_max",
    "flow_iat_min",
    "fwd_iat_total",
    "bwd_iat_total",
    "fwd_psh_flags",
    "bwd_psh_flags",
    "fwd_urg_flags",
    "bwd_urg_flags",
    "fin_flag_count",
    "syn_flag_count",
    "rst_flag_count",
    "psh_flag_count",
    "ack_flag_count",
    "urg_flag_count",
    "down_up_ratio",
    "average_packet_size",
    "init_win_bytes_forward",
    "init_win_bytes_backward",
    "label"
]


def generate_flow_record(label: str) -> dict:
    """Generates a realistic single network flow record matching CIC-IDS2017 schema."""
    if label == "BENIGN":
        port = random.choice([80, 443, 53, 8080, 8443, 21, 25])
        duration = float(np.random.lognormal(mean=7.5, sigma=1.0))
        fwd_pkts = int(max(1, np.random.poisson(lam=8)))
        bwd_pkts = int(max(1, np.random.poisson(lam=10)))
        fwd_len = float(fwd_pkts * np.random.uniform(50, 400))
        bwd_len = float(bwd_pkts * np.random.uniform(200, 1400))
        syn = 1 if random.random() < 0.9 else 0
        ack = 1
        rst = 1 if random.random() < 0.05 else 0
        psh = 1 if random.random() < 0.4 else 0
        avg_pkt_sz = (fwd_len + bwd_len) / max(1, (fwd_pkts + bwd_pkts))
        return {
            "destination_port": port,
            "flow_duration": max(100.0, duration),
            "total_fwd_packets": fwd_pkts,
            "total_bwd_packets": bwd_pkts,
            "total_length_fwd_packets": fwd_len,
            "total_length_bwd_packets": bwd_len,
            "fwd_packet_length_max": max(40.0, fwd_len / fwd_pkts * 1.5),
            "fwd_packet_length_min": max(0.0, fwd_len / fwd_pkts * 0.4),
            "fwd_packet_length_mean": fwd_len / fwd_pkts,
            "fwd_packet_length_std": np.random.uniform(5.0, 30.0),
            "bwd_packet_length_max": max(100.0, bwd_len / bwd_pkts * 1.8),
            "bwd_packet_length_min": max(0.0, bwd_len / bwd_pkts * 0.2),
            "bwd_packet_length_mean": bwd_len / bwd_pkts,
            "bwd_packet_length_std": np.random.uniform(20.0, 150.0),
            "flow_bytes_per_s": (fwd_len + bwd_len) / (max(1.0, duration) / 1e6),
            "flow_packets_per_s": (fwd_pkts + bwd_pkts) / (max(1.0, duration) / 1e6),
            "flow_iat_mean": duration / max(1, fwd_pkts + bwd_pkts),
            "flow_iat_std": np.random.uniform(10.0, 100.0),
            "flow_iat_max": duration * 0.6,
            "flow_iat_min": np.random.uniform(1.0, 50.0),
            "fwd_iat_total": duration * 0.8,
            "bwd_iat_total": duration * 0.7,
            "fwd_psh_flags": 0,
            "bwd_psh_flags": 0,
            "fwd_urg_flags": 0,
            "bwd_urg_flags": 0,
            "fin_flag_count": 1 if random.random() < 0.3 else 0,
            "syn_flag_count": syn,
            "rst_flag_count": rst,
            "psh_flag_count": psh,
            "ack_flag_count": ack,
            "urg_flag_count": 0,
            "down_up_ratio": float(bwd_pkts / max(1, fwd_pkts)),
            "average_packet_size": avg_pkt_sz,
            "init_win_bytes_forward": random.choice([29200, 65535, 14600]),
            "init_win_bytes_backward": random.choice([28960, 65535, 14600]),
            "label": "BENIGN"
        }

    elif label == "PortScan":
        port = random.randint(1, 1024) if random.random() < 0.7 else random.randint(1025, 65535)
        duration = float(np.random.uniform(5.0, 250.0))
        fwd_pkts = int(random.choice([1, 2]))
        bwd_pkts = int(random.choice([0, 1]))
        fwd_len = float(fwd_pkts * random.choice([0, 40, 60]))
        bwd_len = float(bwd_pkts * random.choice([0, 40]))
        return {
            "destination_port": port,
            "flow_duration": duration,
            "total_fwd_packets": fwd_pkts,
            "total_bwd_packets": bwd_pkts,
            "total_length_fwd_packets": fwd_len,
            "total_length_bwd_packets": bwd_len,
            "fwd_packet_length_max": max(fwd_len, 40.0),
            "fwd_packet_length_min": 0.0,
            "fwd_packet_length_mean": fwd_len / fwd_pkts,
            "fwd_packet_length_std": 0.0,
            "bwd_packet_length_max": bwd_len,
            "bwd_packet_length_min": 0.0,
            "bwd_packet_length_mean": bwd_len / max(1, bwd_pkts),
            "bwd_packet_length_std": 0.0,
            "flow_bytes_per_s": (fwd_len + bwd_len) / (max(1.0, duration) / 1e6),
            "flow_packets_per_s": (fwd_pkts + bwd_pkts) / (max(1.0, duration) / 1e6),
            "flow_iat_mean": duration / max(1, fwd_pkts),
            "flow_iat_std": 0.0,
            "flow_iat_max": duration,
            "flow_iat_min": duration,
            "fwd_iat_total": duration,
            "bwd_iat_total": 0.0,
            "fwd_psh_flags": 0,
            "bwd_psh_flags": 0,
            "fwd_urg_flags": 0,
            "bwd_urg_flags": 0,
            "fin_flag_count": 0,
            "syn_flag_count": 1,
            "rst_flag_count": 1 if bwd_pkts > 0 else 0,
            "psh_flag_count": 0,
            "ack_flag_count": 0 if bwd_pkts == 0 else 1,
            "urg_flag_count": 0,
            "down_up_ratio": float(bwd_pkts / fwd_pkts),
            "average_packet_size": (fwd_len + bwd_len) / (fwd_pkts + bwd_pkts),
            "init_win_bytes_forward": random.choice([1024, 2048, 512]),
            "init_win_bytes_backward": -1 if bwd_pkts == 0 else 0,
            "label": "PortScan"
        }

    elif label == "DoS-Hulk":
        port = random.choice([80, 443, 8080])
        duration = float(np.random.uniform(50000.0, 500000.0))
        fwd_pkts = int(np.random.uniform(40, 300))
        bwd_pkts = int(np.random.uniform(2, 20))
        fwd_len = float(fwd_pkts * np.random.uniform(200, 600))
        bwd_len = float(bwd_pkts * np.random.uniform(100, 300))
        return {
            "destination_port": port,
            "flow_duration": duration,
            "total_fwd_packets": fwd_pkts,
            "total_bwd_packets": bwd_pkts,
            "total_length_fwd_packets": fwd_len,
            "total_length_bwd_packets": bwd_len,
            "fwd_packet_length_max": 800.0,
            "fwd_packet_length_min": 0.0,
            "fwd_packet_length_mean": fwd_len / fwd_pkts,
            "fwd_packet_length_std": np.random.uniform(50.0, 150.0),
            "bwd_packet_length_max": 500.0,
            "bwd_packet_length_min": 0.0,
            "bwd_packet_length_mean": bwd_len / bwd_pkts,
            "bwd_packet_length_std": np.random.uniform(20.0, 80.0),
            "flow_bytes_per_s": (fwd_len + bwd_len) / (max(1.0, duration) / 1e6),
            "flow_packets_per_s": (fwd_pkts + bwd_pkts) / (max(1.0, duration) / 1e6),
            "flow_iat_mean": duration / max(1, fwd_pkts),
            "flow_iat_std": np.random.uniform(5.0, 20.0),
            "flow_iat_max": duration * 0.1,
            "flow_iat_min": 1.0,
            "fwd_iat_total": duration,
            "bwd_iat_total": duration * 0.4,
            "fwd_psh_flags": 0,
            "bwd_psh_flags": 0,
            "fwd_urg_flags": 0,
            "bwd_urg_flags": 0,
            "fin_flag_count": 0,
            "syn_flag_count": 1,
            "rst_flag_count": 1 if random.random() < 0.6 else 0,
            "psh_flag_count": 1,
            "ack_flag_count": 1,
            "urg_flag_count": 0,
            "down_up_ratio": float(bwd_pkts / fwd_pkts),
            "average_packet_size": (fwd_len + bwd_len) / (fwd_pkts + bwd_pkts),
            "init_win_bytes_forward": 29200,
            "init_win_bytes_backward": 0,
            "label": "DoS-Hulk"
        }

    elif label == "DDoS":
        port = random.choice([80, 443])
        duration = float(np.random.uniform(2000.0, 80000.0))
        fwd_pkts = int(np.random.uniform(15, 80))
        bwd_pkts = int(np.random.uniform(0, 5))
        fwd_len = float(fwd_pkts * np.random.uniform(100, 400))
        bwd_len = float(bwd_pkts * np.random.uniform(0, 100))
        return {
            "destination_port": port,
            "flow_duration": duration,
            "total_fwd_packets": fwd_pkts,
            "total_bwd_packets": bwd_pkts,
            "total_length_fwd_packets": fwd_len,
            "total_length_bwd_packets": bwd_len,
            "fwd_packet_length_max": 600.0,
            "fwd_packet_length_min": 0.0,
            "fwd_packet_length_mean": fwd_len / fwd_pkts,
            "fwd_packet_length_std": np.random.uniform(20.0, 90.0),
            "bwd_packet_length_max": max(0.0, bwd_len),
            "bwd_packet_length_min": 0.0,
            "bwd_packet_length_mean": bwd_len / max(1, bwd_pkts),
            "bwd_packet_length_std": 0.0,
            "flow_bytes_per_s": (fwd_len + bwd_len) / (max(1.0, duration) / 1e6),
            "flow_packets_per_s": (fwd_pkts + bwd_pkts) / (max(1.0, duration) / 1e6),
            "flow_iat_mean": duration / max(1, fwd_pkts),
            "flow_iat_std": np.random.uniform(2.0, 15.0),
            "flow_iat_max": duration * 0.2,
            "flow_iat_min": 1.0,
            "fwd_iat_total": duration,
            "bwd_iat_total": duration * 0.2,
            "fwd_psh_flags": 0,
            "bwd_psh_flags": 0,
            "fwd_urg_flags": 0,
            "bwd_urg_flags": 0,
            "fin_flag_count": 0,
            "syn_flag_count": 1,
            "rst_flag_count": 1 if random.random() < 0.4 else 0,
            "psh_flag_count": 1 if random.random() < 0.5 else 0,
            "ack_flag_count": 1 if bwd_pkts > 0 else 0,
            "urg_flag_count": 0,
            "down_up_ratio": float(bwd_pkts / fwd_pkts),
            "average_packet_size": (fwd_len + bwd_len) / (fwd_pkts + max(1, bwd_pkts)),
            "init_win_bytes_forward": 29200,
            "init_win_bytes_backward": -1 if bwd_pkts == 0 else 28960,
            "label": "DDoS"
        }

    else:  # SSH-Patator
        port = 22
        duration = float(np.random.uniform(5000.0, 60000.0))
        fwd_pkts = int(np.random.uniform(8, 25))
        bwd_pkts = int(np.random.uniform(8, 25))
        fwd_len = float(fwd_pkts * np.random.uniform(80, 250))
        bwd_len = float(bwd_pkts * np.random.uniform(100, 300))
        return {
            "destination_port": port,
            "flow_duration": duration,
            "total_fwd_packets": fwd_pkts,
            "total_bwd_packets": bwd_pkts,
            "total_length_fwd_packets": fwd_len,
            "total_length_bwd_packets": bwd_len,
            "fwd_packet_length_max": 400.0,
            "fwd_packet_length_min": 0.0,
            "fwd_packet_length_mean": fwd_len / fwd_pkts,
            "fwd_packet_length_std": np.random.uniform(10.0, 50.0),
            "bwd_packet_length_max": 500.0,
            "bwd_packet_length_min": 0.0,
            "bwd_packet_length_mean": bwd_len / bwd_pkts,
            "bwd_packet_length_std": np.random.uniform(10.0, 50.0),
            "flow_bytes_per_s": (fwd_len + bwd_len) / (max(1.0, duration) / 1e6),
            "flow_packets_per_s": (fwd_pkts + bwd_pkts) / (max(1.0, duration) / 1e6),
            "flow_iat_mean": duration / max(1, fwd_pkts + bwd_pkts),
            "flow_iat_std": np.random.uniform(10.0, 80.0),
            "flow_iat_max": duration * 0.4,
            "flow_iat_min": 2.0,
            "fwd_iat_total": duration * 0.9,
            "bwd_iat_total": duration * 0.9,
            "fwd_psh_flags": 0,
            "bwd_psh_flags": 0,
            "fwd_urg_flags": 0,
            "bwd_urg_flags": 0,
            "fin_flag_count": 1 if random.random() < 0.5 else 0,
            "syn_flag_count": 1,
            "rst_flag_count": 1 if random.random() < 0.3 else 0,
            "psh_flag_count": 1,
            "ack_flag_count": 1,
            "urg_flag_count": 0,
            "down_up_ratio": float(bwd_pkts / fwd_pkts),
            "average_packet_size": (fwd_len + bwd_len) / (fwd_pkts + bwd_pkts),
            "init_win_bytes_forward": 29200,
            "init_win_bytes_backward": 28960,
            "label": "SSH-Patator"
        }


def generate_dataset(total_rows: int = 12000, output_path: Path = None) -> Path:
    if output_path is None:
        output_path = DATA_DIR / "cic_sample.csv"

    print(f"[*] Generating {total_rows:,} synthetic CIC-IDS2017 network flow records...")
    
    # Class distribution (realistic SOC baseline):
    # BENIGN: ~65%, DoS-Hulk: ~15%, PortScan: ~10%, DDoS: ~6%, SSH-Patator: ~4%
    classes = [
        ("BENIGN", int(total_rows * 0.65)),
        ("DoS-Hulk", int(total_rows * 0.15)),
        ("PortScan", int(total_rows * 0.10)),
        ("DDoS", int(total_rows * 0.06)),
        ("SSH-Patator", total_rows - int(total_rows * 0.65) - int(total_rows * 0.15) - int(total_rows * 0.10) - int(total_rows * 0.06))
    ]

    records = []
    for label, count in classes:
        print(f"    - Generating {count:,} samples for class: {label}")
        for _ in range(count):
            records.append(generate_flow_record(label))

    random.shuffle(records)
    df = pd.DataFrame(records)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(output_path, index=False)
    print(f"[+] Dataset successfully created at: {output_path}")
    print(f"[+] Total samples: {len(df):,}, Features: {len(df.columns) - 1}")
    return output_path


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate synthetic CIC-IDS2017 flow dataset")
    parser.add_argument("--rows", type=int, default=12000, help="Total number of records to generate")
    parser.add_argument("--output", type=str, default=None, help="Output CSV path")
    args = parser.parse_args()

    out = Path(args.output) if args.output else None
    generate_dataset(total_rows=args.rows, output_path=out)
