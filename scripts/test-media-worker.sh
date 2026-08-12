#!/usr/bin/env sh
set -eu

PYTHON_BIN="${PYTHON:-python3}"
PYTHON_TAG=$("${PYTHON_BIN}" - <<'PY'
import sys
print(f"py{sys.version_info.major}{sys.version_info.minor}")
PY
)
DEPS_ROOT="${WISHPOOL_MEDIA_WORKER_TEST_DEPS:-/tmp/wishpool-media-worker-test-deps}"
DEPS_DIR="${DEPS_ROOT}/${PYTHON_TAG}"
PYTHONPATH_VALUE="services/media-worker:${DEPS_DIR}"

if ! PYTHONPATH="${PYTHONPATH_VALUE}" "${PYTHON_BIN}" - <<'PY' >/dev/null 2>&1
import boto3
import PIL
import pytest
import requests
PY
then
  "${PYTHON_BIN}" -m pip install --quiet \
    --no-cache-dir \
    --trusted-host pypi.org \
    --trusted-host files.pythonhosted.org \
    ${WISHPOOL_PIP_INSTALL_ARGS:-} \
    --target "${DEPS_DIR}" \
    -r services/media-worker/requirements.txt
fi

PYTHONPATH="${PYTHONPATH_VALUE}" "${PYTHON_BIN}" -m pytest services/media-worker/tests
