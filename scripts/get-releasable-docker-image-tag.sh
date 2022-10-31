#!/bin/sh

PRESTO_RELEASE_VERSION=${PRESTO_RELEASE_VERSION:?need an env var like PRESTO_RELEASE_VERSION=0.278}

aws ecr list-images --repository-name oss-presto/presto \
    | jq -r '.imageIds[].imageTag | select( . != null)' \
    | grep "${PRESTO_RELEASE_VERSION}-202" \
    | sort | tail -n 1
