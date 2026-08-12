#!/usr/bin/env sh
set -eu

PYTHON_BIN="${PYTHON:-python3}"
PYTHONPATH="services/ai-worker" "${PYTHON_BIN}" -m unittest discover -s services/ai-worker/tests
