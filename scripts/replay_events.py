#!/usr/bin/env python3
"""
SentinelX Security Event Replay and Generator Script.
Zero-cost, local-first event generator using Python standard library.

Usage:
    python replay_events.py [--base-url URL] [--email EMAIL] [--password PASSWORD]
"""

import argparse
import json
import sys
import time
import urllib.request
import urllib.error
from datetime import datetime, timezone

def make_request(url, method="GET", payload=None, token=None):
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    data = json.dumps(payload).encode("utf-8") if payload else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)

    try:
        with urllib.request.urlopen(req) as response:
            res_body = response.read().decode("utf-8")
            return response.status, json.loads(res_body) if res_body else {}
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(err_body)
        except Exception:
            return e.code, {"error": err_body}
    except Exception as e:
        print(f"[!] Network error connecting to {url}: {e}")
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser(description="SentinelX Event Replay Utility")
    parser.add_argument("--base-url", default="http://localhost:8085/api/v1", help="SentinelX API base URL")
    parser.add_argument("--email", default="analyst@alpha.local", help="User email")
    parser.add_argument("--password", default="DevAnalyst123!", help="User password")
    parser.add_argument("--attack-replay", action="store_true", help="Replay attack sequences (brute force, port scan) and verify alerts")
    args = parser.parse_args()

    print(f"[*] Authenticating against SentinelX at {args.base_url}...")
    status, login_res = make_request(
        f"{args.base_url}/auth/login",
        method="POST",
        payload={"email": args.email, "password": args.password}
    )

    if status != 200 or "token" not in login_res:
        print(f"[-] Authentication failed (HTTP {status}): {login_res}")
        sys.exit(1)

    token = login_res["token"]
    tenant_slug = login_res.get("tenantSlug", "unknown")
    print(f"[+] Authenticated successfully as {args.email} (Tenant: {tenant_slug})")

    # Sample events
    now_iso = datetime.now(timezone.utc).isoformat()
    sample_events = [
        {
            "timestamp": now_iso,
            "sourceType": "linux",
            "sourceName": "bastion-host-01",
            "eventType": "authentication_failure",
            "severity": "HIGH",
            "host": "bastion-host-01",
            "sourceIp": "198.51.100.45",
            "username": "root",
            "message": "Failed SSH password for root from 198.51.100.45 port 42812 ssh2",
            "idempotencyKey": f"ssh-fail-{int(time.time())}-1"
        },
        {
            "timestamp": now_iso,
            "sourceType": "linux",
            "sourceName": "bastion-host-01",
            "eventType": "authentication_failure",
            "severity": "HIGH",
            "host": "bastion-host-01",
            "sourceIp": "198.51.100.45",
            "username": "admin",
            "message": "Failed SSH password for admin from 198.51.100.45 port 42814 ssh2",
            "idempotencyKey": f"ssh-fail-{int(time.time())}-2"
        },
        {
            "timestamp": now_iso,
            "sourceType": "firewall",
            "sourceName": "fw-gateway-east",
            "eventType": "port_scan",
            "severity": "MEDIUM",
            "host": "fw-gateway-east",
            "sourceIp": "198.51.100.45",
            "destinationIp": "10.0.1.15",
            "message": "TCP SYN scan detected across ports 21-445",
            "idempotencyKey": f"port-scan-{int(time.time())}-3"
        }
    ]

    print(f"\n[*] Submitting {len(sample_events)} synthetic security events...")
    for idx, event in enumerate(sample_events, 1):
        status, res = make_request(f"{args.base_url}/events", method="POST", payload=event, token=token)
        if status in (200, 201):
            dup_tag = " [DUPLICATE]" if res.get("duplicate") else ""
            print(f"  [+] Event {idx} ({event['eventType']}): ID {res['id']}{dup_tag}")
        else:
            print(f"  [-] Event {idx} failed (HTTP {status}): {res}")

    # Test Idempotency
    print("\n[*] Testing Idempotency duplicate submission...")
    status, dup_res = make_request(
        f"{args.base_url}/events",
        method="POST",
        payload=sample_events[0],
        token=token
    )
    if status == 200 and dup_res.get("duplicate") is True:
        print(f"[+] Idempotency verified: duplicate flag returned true for ID {dup_res['id']}")
    else:
        print(f"[-] Idempotency response unexpected (HTTP {status}): {dup_res}")

    # Query event stream
    print("\n[*] Fetching recent events for current tenant...")
    status, search_res = make_request(f"{args.base_url}/events?size=5", method="GET", token=token)
    if status == 200:
        events = search_res.get("content", [])
        print(f"[+] Total stored events for tenant: {search_res.get('totalElements', len(events))}")
        for e in events:
            print(f"    - [{e['severity']}] {e['eventType']} from {e['sourceName']} ({e['host'] or 'no-host'}) at {e['eventTime']}")

    # Attack replay & alert verification (SX-0405)
    if args.attack_replay:
        print("\n" + "=" * 60)
        print("[*] SX-0405: Launching Synthetic Attack Replay Harness...")
        print("=" * 60)

        # 1. Synthetic Brute Force Attack
        target_attacker_ip = f"203.0.113.{int(time.time()) % 200 + 10}"
        print(f"\n[+] Replaying brute force attack sequence (5 rapid failed logins from {target_attacker_ip})...")
        for i in range(1, 6):
            bf_event = {
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "sourceType": "linux",
                "sourceName": "ssh-gateway",
                "eventType": "authentication_failure",
                "severity": "HIGH",
                "host": "auth-portal-prod",
                "sourceIp": target_attacker_ip,
                "username": f"user_{i}",
                "message": f"Failed password attempt #{i} for user_{i}",
                "idempotencyKey": f"bf-replay-{int(time.time()*1000)}-{i}"
            }
            s, r = make_request(f"{args.base_url}/events", method="POST", payload=bf_event, token=token)
            print(f"    - Attempt #{i}: HTTP {s} (ID: {r.get('id', 'N/A')})")

        # 2. Query Generated Alerts
        print("\n[+] Inspecting generated alerts from detection engine...")
        status, alert_res = make_request(f"{args.base_url}/alerts", method="GET", token=token)
        if status == 200:
            alerts = alert_res.get("content", [])
            print(f"[+] Total alerts found: {alert_res.get('totalElements', len(alerts))}")
            for a in alerts:
                print(f"    - [{a['severity']}] {a['ruleKey']}: '{a['title']}' | Status: {a['status']} | Count: {a['eventCount']}")
                if a.get('id'):
                    # Test lifecycle update (SX-0404)
                    print(f"    -> Updating alert {a['id']} to INVESTIGATING...")
                    patch_status, patch_res = make_request(
                        f"{args.base_url}/alerts/{a['id']}",
                        method="PATCH",
                        payload={"status": "INVESTIGATING"},
                        token=token
                    )
                    if patch_status == 200:
                        print(f"       [OK] Lifecycle updated: {patch_res.get('status')}")

    print("\n[+] Event replay completed successfully.")

if __name__ == "__main__":
    main()
