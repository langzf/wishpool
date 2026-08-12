#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

openapi-generator-cli generate --config packages/api-contracts/openapi-generator/kotlin-server.yaml
openapi-generator-cli generate --config packages/api-contracts/openapi-generator/dart-client.yaml
openapi-generator-cli generate --config packages/api-contracts/openapi-generator/typescript-client.yaml
