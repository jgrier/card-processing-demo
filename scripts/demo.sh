#!/usr/bin/env bash
# Full demo walkthrough for payment pipeline
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESTATE_URL="${RESTATE_URL:-http://localhost:8080}"

pause() {
    echo ""
    echo "--- Press ENTER to continue ---"
    read -r
}

run() {
    echo "$ $*"
    "$@"
}

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║   Card Payment Processing Pipeline — Powered by Restate    ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "End-to-end payment lifecycle, five paradigms, one system:"
echo "  1. Synchronous authorization (stateful RPC)"
echo "  2. Clearing file fan-out (event-driven processing)"
echo "  3. Per-payment processing (workflow orchestration)"
echo "  4. End-of-day settlement (batch processing)"
echo "  5. Merchant reconciliation (real-time serving layer)"
echo ""

# Reset clearing file
run "$SCRIPT_DIR/reset.sh"
pause

echo "════════════════════════════════════════════════════════════════"
echo " STEP 1: Stateful RPC — Payment Authorizations"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "Each authorization is a Virtual Object keyed by authId."
echo "Exclusive access — no concurrent mutations to the same auth."
echo "These are three independent RPC invocations."
echo ""

run "$SCRIPT_DIR/create-auth.sh" AUTH-001 MERCHANT-ACME 150.00
pause

run "$SCRIPT_DIR/create-auth.sh" AUTH-002 MERCHANT-ACME 275.50
pause

run "$SCRIPT_DIR/create-auth.sh" AUTH-003 MERCHANT-GLOBEX 89.99
pause

echo "════════════════════════════════════════════════════════════════"
echo " STEP 2: Event-Driven Processing — Clearing File Ingestion"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "The clearing file has accumulated 3 events from the auths above."
echo "ClearingGateway fans out each event as a separate workflow"
echo "invocation via send() — fire-and-forget into Restate's log."
echo "The gateway returns immediately."
echo ""
echo "Contents of clearing file:"
echo ""
cat "$SCRIPT_DIR/../data/clearing-file.json" | python3 -m json.tool
pause

echo "Sending clearing file to ClearingGateway..."
echo ""
run "$SCRIPT_DIR/send-clearing-file.sh"
pause

echo "════════════════════════════════════════════════════════════════"
echo " STEP 3: Workflow Orchestration — Clearing Workflows"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "Each ClearingWorkflow[paymentId] is a durable workflow:"
echo "  1. Checks auth via stateful RPC to PaymentAuth"
echo "  2. Calls multi-rail API (durable side effect, ~30% failure → auto-retry)"
echo "  3. Adds payment to MerchantSettlement"
echo "  4. Registers merchant in SettlementWindow"
echo ""
echo "Check the Restate UI to see the workflows: http://localhost:9070"
pause

echo "════════════════════════════════════════════════════════════════"
echo " STEP 4: Serving Layer — Merchant Reconciliation (pre-settlement)"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "getReconciliation() is a @Shared handler — concurrent reads"
echo "against live durable state, no locking required."
echo ""

run "$SCRIPT_DIR/query-recon.sh" MERCHANT-ACME
echo ""
run "$SCRIPT_DIR/query-recon.sh" MERCHANT-GLOBEX
pause

echo "════════════════════════════════════════════════════════════════"
echo " STEP 5: Batch Processing — Trigger Settlement"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "SettlementWindow fans out settle() to all merchants as parallel"
echo "futures via DurableFuture.all() — awaits all with error visibility."
echo "Each merchant: calculates fees (2.1% + \$0.10/txn), credits via"
echo "multi-rail, records settlement."
echo ""

run "$SCRIPT_DIR/trigger-settlement.sh"
pause

echo "════════════════════════════════════════════════════════════════"
echo " STEP 6: Serving Layer — Merchant Reconciliation (post-settlement)"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "Payments moved from unsettled → settled. Settlement records created."
echo ""

run "$SCRIPT_DIR/query-recon.sh" MERCHANT-ACME
echo ""
run "$SCRIPT_DIR/query-recon.sh" MERCHANT-GLOBEX
pause

echo "════════════════════════════════════════════════════════════════"
echo " STEP 7: Idempotency — Re-send the same clearing file"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "Sending the exact same clearing file again."
echo "Workflows are keyed by paymentId — Restate won't re-execute them."
echo "Exactly-once processing with zero extra code."
echo ""

run "$SCRIPT_DIR/send-clearing-file.sh"
echo ""
echo "Waiting 5 seconds..."
sleep 5
echo ""
echo "Querying reconciliation — nothing should have changed:"
echo ""
run "$SCRIPT_DIR/query-recon.sh" MERCHANT-ACME
echo ""
run "$SCRIPT_DIR/query-recon.sh" MERCHANT-GLOBEX
pause

echo "════════════════════════════════════════════════════════════════"
echo " STEP 8: Durable Execution — Clearing before authorization"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "Sending a clearing event for AUTH-LATE — which doesn't exist yet."
echo "The workflow sees NOT_FOUND, creates an Awakeable, and suspends."
echo "No polling, no retry loops — the workflow is parked, consuming nothing."
echo "When authorize() runs, it resolves the Awakeable and the workflow wakes up."
echo "Watch the Restate UI: http://localhost:9070"
echo ""

echo '$ curl -X POST .../ClearingGateway/ingest -d {...PAY-LATE, AUTH-LATE...}'
curl -s -X POST "$RESTATE_URL/ClearingGateway/ingest" \
  -H 'Content-Type: application/json' \
  -d '{"events":[{"paymentId":"PAY-LATE","authId":"AUTH-LATE","merchantId":"MERCHANT-ACME","amount":500.00,"currency":"USD","cardNetwork":"AMEX","timestamp":"2026-04-16T00:00:00Z"}]}'
echo ""
echo ""
echo "==> Workflow is suspended on Awakeable. Check the UI."
pause

echo "Now creating the authorization — authorize() resolves the Awakeable."
echo ""
echo '$ curl -X POST .../PaymentAuth/AUTH-LATE/authorize -d {...500.00...}'
curl -s -X POST "$RESTATE_URL/PaymentAuth/AUTH-LATE/authorize" \
  -H 'Content-Type: application/json' \
  -d '{"merchantId":"MERCHANT-ACME","cardNumber":"3782-XXXX","amount":500.00,"currency":"USD"}' | python3 -m json.tool
echo ""
echo "Waiting for workflow to complete..."
sleep 5
echo ""
run "$SCRIPT_DIR/query-recon.sh" MERCHANT-ACME

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                    Demo Complete!                           ║"
echo "╚══════════════════════════════════════════════════════════════╝"
