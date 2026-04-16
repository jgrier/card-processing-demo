#!/usr/bin/env bash
# Usage: ./scripts/create-auth.sh <authId> <merchantId> <amount> [currency]
set -euo pipefail

AUTH_ID="${1:?Usage: create-auth.sh <authId> <merchantId> <amount> [currency]}"
MERCHANT_ID="${2:?Usage: create-auth.sh <authId> <merchantId> <amount> [currency]}"
AMOUNT="${3:?Usage: create-auth.sh <authId> <merchantId> <amount> [currency]}"
CURRENCY="${4:-USD}"
CARD_NETWORK="${5:-AMEX}"

RESTATE_URL="${RESTATE_URL:-http://localhost:8080}"
DATA_DIR="$(cd "$(dirname "$0")/../data" && pwd)"

echo "$ curl -X POST $RESTATE_URL/PaymentAuth/$AUTH_ID/authorize -d {merchantId: $MERCHANT_ID, amount: $AMOUNT}"
RESULT=$(curl -s -X POST "$RESTATE_URL/PaymentAuth/$AUTH_ID/authorize" \
  -H 'Content-Type: application/json' \
  -d "{\"merchantId\":\"$MERCHANT_ID\",\"cardNumber\":\"3782-XXXX-XXXX-1234\",\"amount\":$AMOUNT,\"currency\":\"$CURRENCY\"}")

echo "$RESULT" | python3 -m json.tool 2>/dev/null || echo "$RESULT"

# Generate a payment ID and append a clearing event to the clearing file
PAYMENT_ID="PAY-$(date +%s)-$RANDOM"
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# Read existing events, append new one
EXISTING=$(cat "$DATA_DIR/clearing-file.json")
NEW_EVENT="{\"paymentId\":\"$PAYMENT_ID\",\"authId\":\"$AUTH_ID\",\"merchantId\":\"$MERCHANT_ID\",\"amount\":$AMOUNT,\"currency\":\"$CURRENCY\",\"cardNetwork\":\"$CARD_NETWORK\",\"timestamp\":\"$TIMESTAMP\"}"

UPDATED=$(echo "$EXISTING" | python3 -c "
import sys, json
data = json.load(sys.stdin)
data['events'].append(json.loads('$NEW_EVENT'))
print(json.dumps(data, indent=2))
")
echo "$UPDATED" > "$DATA_DIR/clearing-file.json"

echo "==> Clearing event $PAYMENT_ID appended to clearing-file.json"
