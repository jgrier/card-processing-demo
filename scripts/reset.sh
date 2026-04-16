#!/usr/bin/env bash
# Resets the clearing file for a fresh demo run
set -euo pipefail

DATA_DIR="$(cd "$(dirname "$0")/../data" && pwd)"

echo '{"events":[]}' > "$DATA_DIR/clearing-file.json"
echo "==> Clearing file reset"
