#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

NODE_BIN="node"
if ! command -v "${NODE_BIN}" >/dev/null 2>&1 && command -v node.exe >/dev/null 2>&1; then
  NODE_BIN="node.exe"
fi
GENERATOR=("${NODE_BIN}" node_modules/@openapitools/openapi-generator-cli/main.js)
CONFIG="packages/api-contracts/openapi-generator/typescript-contract.yaml"
EXPECTED="packages/api-contracts/contract-ts"
TMP_DIR="$(mktemp -d "${ROOT_DIR}/.openapi-contract-check.XXXXXX")"
trap 'rm -rf "${TMP_DIR}"' EXIT
OUTPUT_DIR="${TMP_DIR}/contract-ts"
OUTPUT_ARG="${OUTPUT_DIR}"
if [[ "${NODE_BIN}" == "node.exe" ]] && command -v wslpath >/dev/null 2>&1; then
  OUTPUT_ARG="$(wslpath -w "${OUTPUT_DIR}")"
fi

echo "Checking OpenAPI generated contract types (offline; no download requested)..."
"${GENERATOR[@]}" generate --config "${CONFIG}" --output "${OUTPUT_ARG}" 2>&1 | tee "${TMP_DIR}/generator.log"

if grep -Eiq 'Downloading|downloaded' "${TMP_DIR}/generator.log"; then
  echo "FAIL: generator output indicates a download or network access." >&2
  exit 1
fi

if ! diff -ruN --exclude='.openapi-generator' "${EXPECTED}" "${OUTPUT_DIR}" > "${TMP_DIR}/diff.txt"; then
  echo "FAIL: generated OpenAPI contract types differ from ${EXPECTED}."
  grep -E -A8 -B2 'RewardSummary|^diff |^@@' "${TMP_DIR}/diff.txt" | head -80 || head -80 "${TMP_DIR}/diff.txt"
  exit 1
fi

echo "PASS: generated OpenAPI contract types are stable and match ${EXPECTED}."
