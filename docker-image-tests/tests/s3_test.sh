#!/usr/bin/env bash
set -euo pipefail

echo "=== Running S3 Test ==="

echo "Configuring MinIO client..."
mc alias set local ${MINIO_ENDPOINT} minio minio123

echo "Creating bucket..."
mc mb -p local/warehouse || true

echo "Cleaning table location..."
mc rm -r --force local/warehouse/users || true

echo "Creating directory marker for external table..."
echo -n "" | mc pipe local/warehouse/users/.keep

echo "Running S3 SQL test..."
java -jar /usr/local/bin/presto-cli.jar \
  --server presto-coordinator:8080 \
  --catalog hive \
  --schema default \
  --file /tests/s3_test.sql \
  --output-format CSV \
  > /tmp/s3_test_result.txt

# Verify the test result
if grep -q "Charlie" /tmp/s3_test_result.txt; then
  echo "S3 test validation: Data successfully written and read from S3"
  exit 0
else
  echo "S3 test validation failed: Expected data not found"
  exit 1
fi
