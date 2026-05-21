#!/usr/bin/env bash
set -euo pipefail

# Export common environment variables for tests
export MINIO_ENDPOINT="http://minio:9000"
export PRESTO_SERVER="http://presto-coordinator:8080"
export PRESTO_CLI_VERSION="${PRESTO_VERSION:-0.296}"
FALLBACK_VERSION="0.296"

echo "Installing OS dependencies..."
apt-get update
apt-get install -y --no-install-recommends \
  curl \
  ca-certificates \
  bash
rm -rf /var/lib/apt/lists/*

echo "Downloading presto-cli ${PRESTO_CLI_VERSION}..."

if curl -f -L \
  https://github.com/prestodb/presto/releases/download/${PRESTO_CLI_VERSION}/presto-cli-${PRESTO_CLI_VERSION}-executable.jar \
  -o /usr/local/bin/presto-cli.jar 2>/dev/null; then
  echo "Successfully downloaded presto-cli ${PRESTO_CLI_VERSION}"
else
  echo "Failed to download presto-cli ${PRESTO_CLI_VERSION}, falling back to ${FALLBACK_VERSION}..."
  curl -L \
    https://github.com/prestodb/presto/releases/download/${FALLBACK_VERSION}/presto-cli-${FALLBACK_VERSION}-executable.jar \
    -o /usr/local/bin/presto-cli.jar
  echo "Successfully downloaded presto-cli ${FALLBACK_VERSION}"
  export PRESTO_CLI_VERSION="${FALLBACK_VERSION}"
fi

chmod +x /usr/local/bin/presto-cli.jar

echo "Downloading MinIO client (mc)..."
curl -L \
  https://dl.min.io/client/mc/release/linux-amd64/mc \
  -o /usr/local/bin/mc
chmod +x /usr/local/bin/mc

echo "Waiting for MinIO..."
until curl -s ${MINIO_ENDPOINT}/minio/health/ready >/dev/null; do
  sleep 1
done

echo "Waiting for Presto coordinator to be available..."
until curl -s ${PRESTO_SERVER}/v1/info >/dev/null; do
  echo "  Presto coordinator not ready yet, waiting..."
  sleep 2
done
echo "Presto coordinator is responding"

echo "Waiting for Presto to be fully ready (checking cluster state)..."
MAX_RETRIES=60
RETRY_COUNT=0
until [ $RETRY_COUNT -ge $MAX_RETRIES ]; do
  # Check if Presto can execute a simple query
  if java -jar /usr/local/bin/presto-cli.jar \
    --server presto-coordinator:8080 \
    --execute "SELECT 1" \
    --output-format CSV \
    >/dev/null 2>&1; then
    echo "Presto is fully ready and can execute queries"
    break
  fi

  RETRY_COUNT=$((RETRY_COUNT + 1))
  if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
    echo "ERROR: Presto did not become ready within the timeout period"
    exit 1
  fi

  echo "  Presto not ready yet (attempt $RETRY_COUNT/$MAX_RETRIES), waiting..."
  sleep 10
done

echo "Tools installation completed"
echo "Running tests..."

# Track test results
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
declare -a FAILED_TEST_NAMES

# Run all test scripts in the tests directory
for test_script in /tests/*.sh; do
  if [ -f "$test_script" ]; then
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    test_name=$(basename "$test_script")

    echo ""
    echo "=========================================="
    echo "Executing: $test_name"
    echo "=========================================="

    if bash "$test_script"; then
      echo "✅ $test_name PASSED"
      PASSED_TESTS=$((PASSED_TESTS + 1))
    else
      echo "❌ $test_name FAILED"
      FAILED_TESTS=$((FAILED_TESTS + 1))
      FAILED_TEST_NAMES+=("$test_name")
    fi
  fi
done

echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo "Total tests: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $FAILED_TESTS"

if [ $FAILED_TESTS -gt 0 ]; then
  echo ""
  echo "Failed tests:"
  for test_name in "${FAILED_TEST_NAMES[@]}"; do
    echo "  - $test_name"
  done
  exit 1
fi

echo ""
echo "All tests completed successfully! 🎉"
exit 0
