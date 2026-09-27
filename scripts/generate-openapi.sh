#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

NODE_BIN="node"
if ! command -v "${NODE_BIN}" >/dev/null 2>&1 && command -v node.exe >/dev/null 2>&1; then
  NODE_BIN="node.exe"
fi

"${NODE_BIN}" node_modules/@openapitools/openapi-generator-cli/main.js generate --config packages/api-contracts/openapi-generator/kotlin-server.yaml
"${NODE_BIN}" node_modules/@openapitools/openapi-generator-cli/main.js generate --config packages/api-contracts/openapi-generator/dart-client.yaml
"${NODE_BIN}" node_modules/@openapitools/openapi-generator-cli/main.js generate --config packages/api-contracts/openapi-generator/typescript-contract.yaml
