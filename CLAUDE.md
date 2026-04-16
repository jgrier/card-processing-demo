# Card Payment Processing System — Restate Demo

## Build & Run

```bash
# Requires Java 21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# Build
./gradlew shadowJar

# Start Restate + app (Terminal 1)
./scripts/start.sh

# Register + run demo (Terminal 2)
./scripts/register.sh
./scripts/demo.sh
```

## Stack

- Java 21, Gradle 8.10, Shadow plugin for fat jar
- Restate SDK 2.6.0 (`dev.restate:sdk-java-http`)
- Restate Server 1.6 (Docker)
- App listens on port 9080, Restate ingress on 8080, admin/UI on 9070

## Architecture

Five Restate services modeling a card payment lifecycle:

- **PaymentAuth** (Virtual Object, keyed by authId) — stateful RPC for authorization
- **ClearingGateway** (Service, stateless) — fans out clearing file events via `send()`
- **ClearingWorkflow** (Workflow, keyed by paymentId) — orchestrates per-payment processing
- **MerchantSettlement** (Virtual Object, keyed by merchantId) — accumulates payments, settles
- **SettlementWindow** (Virtual Object, keyed by date) — batch settlement fan-out

## Key Patterns

- `send()` for fire-and-forget fan-out (ClearingGateway → ClearingWorkflow)
- `ctx.run()` for durable side effects with automatic retry (multi-rail API calls)
- `DurableFuture.all()` for parallel fan-out with error visibility (settlement)
- `@Shared` handlers for concurrent reads (getAuth, getReconciliation)
- Awakeables for cross-service signaling (workflow waits for late auth)
- Generic state keys use `TypeTag.of(new TypeRef<...>() {})`
- `Instant` fields are stored as `String` to avoid Jackson JSR310 module issues
