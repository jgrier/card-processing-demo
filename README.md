# Card Payment Processing Pipeline — Powered by Restate

An end-to-end card payment processing pipeline running entirely in [Restate](https://restate.dev). The demo models a real-world payment lifecycle:

1. **Synchronous authorization** — merchants submit auth requests as stateful RPCs
2. **Clearing file processing** — a batch file is fanned out into parallel workflow invocations
3. **Per-merchant accumulation** — payments stream into merchant settlement objects throughout the day
4. **End-of-day settlement** — batch fan-out settles all merchants in parallel, calculating fees and crediting accounts
5. **Real-time serving layer** — merchants query their current reconciliation status at any time via shared handlers

Five paradigms — stateful RPC, event-driven streaming, workflow orchestration, batch processing, and real-time serving — unified in one system, one deployment, one programming model.

## Architecture

| Component | Restate Primitive | Key | Paradigm |
|---|---|---|---|
| PaymentAuth | Virtual Object | authId | Stateful RPC |
| ClearingGateway | Service (stateless) | — | Event-driven fan-out |
| ClearingWorkflow | Workflow | paymentId | Durable workflow |
| MerchantSettlement | Virtual Object | merchantId | Incremental aggregation + batch |
| SettlementWindow | Virtual Object | date string | Batch fan-out trigger |

### Flow

```
Auth Request ──► PaymentAuth[authId].authorize()       (stateful RPC)
                         │
Clearing File ──► ClearingGateway.ingest()              (streaming fan-out via send())
                         │
                  ClearingWorkflow[paymentId].run()      (durable workflow)
                    ├── getAuth() ← PaymentAuth
                    ├── debitCardholder() ← MultiRail API (ctx.run, ~50% failure)
                    ├── addPayment() → MerchantSettlement
                    └── addMerchant() → SettlementWindow
                         │
Trigger ──────► SettlementWindow[date].triggerSettlement() (batch fan-out)
                    └── settle() → MerchantSettlement[]   (parallel futures)
                         │
Query ────────► MerchantSettlement[id].getReconciliation() (shared handler)
```

## Prerequisites

- Java 21+
- Docker (for Restate server)

## Quick Start

```bash
# Terminal 1 — build, start Restate, start the app
./scripts/start.sh

# Terminal 2 — register and run the demo
./scripts/register.sh
./scripts/demo.sh
```

## Demo Scripts

| Script | Purpose |
|---|---|
| `scripts/start.sh` | Build, start Restate, start the app |
| `scripts/register.sh` | Register deployment with Restate |
| `scripts/create-auth.sh <authId> <merchantId> <amount>` | Create auth + append clearing event |
| `scripts/send-clearing-file.sh` | Send accumulated clearing file |
| `scripts/trigger-settlement.sh [date]` | Trigger batch settlement |
| `scripts/query-recon.sh <merchantId>` | Query reconciliation report |
| `scripts/reset.sh` | Reset clearing file for fresh run |
| `scripts/demo.sh` | Full guided walkthrough |

## Key Demo Moments

**Idempotency** — Re-send the same clearing file. Workflows are keyed by paymentId, so Restate won't re-execute them. Exactly-once processing with zero extra code.

**Late authorization** — Send a clearing event before its authorization exists. The workflow creates an Awakeable and suspends. When `authorize()` runs, it resolves the Awakeable and the workflow completes instantly. No polling, no retry loops.

**Transient failures** — The multi-rail API has a ~50% failure rate. Restate durably retries side effects automatically. Watch the app logs to see failures followed by successful retries.
