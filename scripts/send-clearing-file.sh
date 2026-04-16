#!/usr/bin/env bash
# Sends the accumulated clearing file to ClearingGateway
set -euo pipefail

RESTATE_URL="${RESTATE_URL:-http://localhost:8080}"
DATA_DIR="$(cd "$(dirname "$0")/../data" && pwd)"

echo "$ curl -X POST $RESTATE_URL/ClearingGateway/ingest -d @data/clearing-file.json"
RESULT=$(curl -s -X POST "$RESTATE_URL/ClearingGateway/ingest" \
  -H 'Content-Type: application/json' \
  -d @"$DATA_DIR/clearing-file.json")

echo "==> $RESULT"
