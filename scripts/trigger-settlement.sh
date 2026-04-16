#!/usr/bin/env bash
# Triggers settlement for today's date
set -euo pipefail

RESTATE_URL="${RESTATE_URL:-http://localhost:8080}"
DATE="${1:-$(date +%Y-%m-%d)}"

echo "$ curl -X POST $RESTATE_URL/SettlementWindow/$DATE/triggerSettlement"
RESULT=$(curl -s -X POST "$RESTATE_URL/SettlementWindow/$DATE/triggerSettlement")

echo "$RESULT" | python3 -m json.tool 2>/dev/null || echo "$RESULT"
