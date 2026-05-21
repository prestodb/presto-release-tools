#!/usr/bin/env bash
set -e

echo "Starting Docker Compose tests..."
echo ""

# Run docker compose and capture exit code
docker compose up --abort-on-container-exit --exit-code-from init-job

EXIT_CODE=$?

echo ""
echo "=========================================="
echo "Docker Image Test Results"
echo "=========================================="

if [ $EXIT_CODE -eq 0 ]; then
  echo "✅ All tests PASSED"
  exit 0
else
  echo "❌ Tests FAILED (exit code: $EXIT_CODE)"
  echo ""
  echo "Check the logs above for details"
  exit 1
fi
