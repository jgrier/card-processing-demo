#!/usr/bin/env bash
# Start Restate and the app. Run this in Terminal 1.
set -euo pipefail

cd "$(dirname "$0")/.."

export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

echo "==> Building shadow jar..."
./gradlew shadowJar -q

echo "==> Starting Restate server..."
docker compose down -t 3 2>/dev/null || true
docker compose up -d

echo "==> Waiting for Restate to be ready..."
until curl -s -o /dev/null http://localhost:9070/health; do sleep 0.5; done

echo "==> Starting app on port 9080..."
java -jar build/libs/card-processing-demo-all.jar
