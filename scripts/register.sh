#!/usr/bin/env bash
# Register the app with Restate. Run this in Terminal 2 after start.sh is up.
set -euo pipefail

RESTATE_URL="${RESTATE_URL:-http://localhost:9070}"

echo "==> Waiting for app to be ready on port 9080..."
until curl -s -o /dev/null http://localhost:9080/health; do sleep 0.5; done

echo "==> Registering deployment with Restate..."
RESULT=$(curl -s -X POST "$RESTATE_URL/deployments" \
  -H 'Content-Type: application/json' \
  -d '{"uri":"http://host.docker.internal:9080"}')

COUNT=$(echo "$RESULT" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['services']))")
echo "==> Registered $COUNT services"

echo ""
echo "Ready. Restate UI: http://localhost:9070"
echo ""
echo "Run ./scripts/demo.sh to start the demo."
