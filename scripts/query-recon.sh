#!/usr/bin/env bash
# Usage: ./scripts/query-recon.sh <merchantId>
set -euo pipefail

MERCHANT_ID="${1:?Usage: query-recon.sh <merchantId>}"
RESTATE_URL="${RESTATE_URL:-http://localhost:8080}"

echo "$ curl -X POST $RESTATE_URL/MerchantSettlement/$MERCHANT_ID/getReconciliation"
RESULT=$(curl -s -X POST "$RESTATE_URL/MerchantSettlement/$MERCHANT_ID/getReconciliation")

echo "$RESULT" | python3 -m json.tool 2>/dev/null || echo "$RESULT"
