# Docker Image Tests

This directory contains integration tests for Presto Docker images.

## Structure

```
docker-image-tests/
├── docker-compose.yml       # Docker Compose configuration
├── test.sh                  # Main test runner script
├── entrypoint.sh            # Tool installation and test orchestration
├── tests/                   # Individual test scripts
│   ├── s3_test.sh           # S3 integration test
│   └── s3_test.sql          # SQL for S3 test
└── presto/                  # Presto configuration
```

## How It Works

1. **entrypoint.sh**:
   - Installs required tools (presto-cli, mc)
   - Waits for services to be ready
   - Verifies Presto can execute queries before running tests
   - Exports common environment variables (`MINIO_ENDPOINT`, `PRESTO_SERVER`)
   - Discovers and runs all `.sh` test scripts in the `tests/` folder
   - Tracks test results and provides summary

2. **tests/**: Contains individual test scripts that are automatically discovered and executed
   - Each test must exit with code 0 for success
   - Any other exit code indicates failure

3. **test.sh**: Wrapper script that runs docker compose and reports overall results

## Environment Variables

The following environment variables are exported by `entrypoint.sh` and available to all tests:

- `MINIO_ENDPOINT`: MinIO server endpoint (default: `http://minio:9000`)
- `PRESTO_SERVER`: Presto coordinator endpoint (default: `http://presto-coordinator:8080`)
- `PRESTO_CLI_VERSION`: Presto CLI version (default: `0.296`)

## Adding New Tests

To add a new test:

1. Create a new test script in the `tests/` directory (e.g., `tests/my_test.sh`)
2. Make it executable: `chmod +x tests/my_test.sh`
3. Use `exit 0` for success, any other exit code for failure
4. The script will be automatically picked up and executed by `entrypoint.sh`

Example test structure:
```bash
#!/usr/bin/env bash
set -euo pipefail

echo "=== Running My Test ==="

# Your test logic here
# Use environment variables: $MINIO_ENDPOINT, $PRESTO_SERVER

java -jar /usr/local/bin/presto-cli.jar \
  --server presto-coordinator:8080 \
  --catalog hive \
  --schema default \
  --file /tests/my_test.sql \
  --output-format CSV \
  > /tmp/my_test_result.txt

# Validate results
if grep -q "expected_value" /tmp/my_test_result.txt; then
  echo "Test validation successful"
  exit 0
else
  echo "Test validation failed"
  exit 1
fi
```

## Running Tests Locally

```bash
cd docker-image-tests
./test.sh
```

To test a specific Presto version:
```bash
PRESTO_VERSION=0.296 ./test.sh
```

The test runner will:
- Start all services via Docker Compose
- Run all tests in the `tests/` directory
- Display a summary of passed/failed tests
- Exit with code 0 if all tests pass, 1 if any test fails

## Running Tests in GitHub Actions

### Manual Trigger

You can manually trigger the tests from the GitHub Actions UI:

1. Go to the "Actions" tab in your repository
2. Select "Docker Image Tests" workflow
3. Click "Run workflow"
4. Enter the image tag to test (default: `latest`)
5. Click "Run workflow"

### Called from Another Workflow in the Same Repository

You can call this workflow from another GitHub Action in the same repository:

```yaml
jobs:
  test-docker-image:
    uses: ./.github/workflows/docker-image-tests.yml
    with:
      image_tag: '0.296'
```

### Called from Another Repository

You can call this workflow from a different repository by referencing the full repository path:

```yaml
jobs:
  test-docker-image:
    uses: your-org/presto-release-tools/.github/workflows/docker-image-tests.yml@master
    with:
      image_tag: '0.296'
```

**Note:** When calling from another repository:
- Replace `your-org/presto-release-tools` with the actual organization and repository name
- You can specify a branch, tag, or commit SHA after the `@` symbol (e.g., `@master`, `@v1.0.0`, `@abc123`)
- The calling repository must have access to the workflow repository
