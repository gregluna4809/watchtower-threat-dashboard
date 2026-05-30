import json
import os
import re
import shlex
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone


PID_PATTERN = re.compile(r"pid=(\d+)")
VALID_PROTOCOLS = {"tcp": "TCP", "udp": "UDP", "tcp6": "TCP", "udp6": "UDP"}


def main():
    backend_url = os.environ.get("WATCHTOWER_BACKEND_URL", "http://watchtower-backend:8088/internal/v1/host-snapshots")
    token = os.environ.get("WATCHTOWER_OBSERVER_TOKEN", "")
    interval = float(os.environ.get("WATCHTOWER_OBSERVER_INTERVAL_SECONDS", "3"))
    command = shlex.split(os.environ.get("WATCHTOWER_SS_COMMAND", "ss -tunap"))

    if not token:
        print("WATCHTOWER_OBSERVER_TOKEN is required", file=sys.stderr)
        return 1

    while True:
        try:
            observed_at = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
            lines = run_command(command)
            snapshots = parse_ss(lines)
            post_snapshots(backend_url, token, observed_at, snapshots)
            print(f"posted {len(snapshots)} host socket snapshots", flush=True)
        except Exception as ex:
            print(f"host observer poll failed: {ex}", file=sys.stderr, flush=True)
        time.sleep(interval)


def run_command(command):
    completed = subprocess.run(
        command,
        check=True,
        capture_output=True,
        text=True,
        timeout=5,
    )
    return completed.stdout.splitlines()


def parse_ss(lines):
    snapshots = []
    for raw_line in lines:
        line = (raw_line or "").strip()
        if not line or line.lower().startswith("netid "):
            continue

        columns = line.split()
        if len(columns) < 6:
            continue

        protocol = VALID_PROTOCOLS.get(columns[0].lower())
        if protocol is None:
            continue

        local = parse_address_port(columns[4], local=True)
        remote = parse_address_port(columns[5], local=False)
        if local is None:
            continue

        snapshot = {
            "protocol": protocol,
            "localIp": local[0],
            "localPort": local[1],
            "remoteIp": None if remote is None else remote[0],
            "remotePort": None if remote is None else remote[1],
            "state": normalize_state(columns[1]),
            "pid": parse_pid(columns[6:]),
        }
        snapshots.append(snapshot)
    return snapshots


def parse_address_port(value, local):
    if not value or value == "*:*":
        return None

    separator = value.rfind(":")
    if separator < 0 or separator == len(value) - 1:
        return None

    address = clean_address(value[:separator])
    raw_port = value[separator + 1:]
    if raw_port == "*":
        return None
    if address == "*" and not local:
        return None

    try:
        port = int(raw_port)
    except ValueError:
        return None
    if port < 1 or port > 65535:
        return None

    if address == "*":
        address = "0.0.0.0"
    return address, port


def clean_address(address):
    if address.startswith("[") and address.endswith("]"):
        address = address[1:-1]
    zone_index = address.find("%")
    if zone_index >= 0:
        address = address[:zone_index]
    return address


def normalize_state(state):
    if not state:
        return None
    normalized = state.upper().replace("-", "_")
    if normalized == "ESTAB":
        return "ESTABLISHED"
    if normalized == "LISTEN":
        return "LISTENING"
    return normalized


def parse_pid(columns):
    for column in columns:
        match = PID_PATTERN.search(column)
        if match:
            return int(match.group(1))
    return None


def post_snapshots(backend_url, token, observed_at, snapshots):
    payload = json.dumps({
        "observedAt": observed_at,
        "snapshots": snapshots,
    }).encode("utf-8")
    request = urllib.request.Request(
        backend_url,
        data=payload,
        method="POST",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=5) as response:
            if response.status != 200:
                raise RuntimeError(f"backend returned HTTP {response.status}")
    except urllib.error.HTTPError as ex:
        raise RuntimeError(f"backend returned HTTP {ex.code}") from ex


if __name__ == "__main__":
    raise SystemExit(main())
