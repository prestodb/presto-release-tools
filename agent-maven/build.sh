#!/bin/bash -ex

AWS_PROFILE="oss-ci"
AWS_REGION="us-east-1"
AWS_ECR="public.ecr.aws/oss-presto"
DOCKER_REPO="${AWS_ECR}/agent-maven-jdk11"
DOCKER_TAG="$(TZ=UTC date +%Y%m%d)H$(git rev-parse --short=7 HEAD)"
DOCKER_IMAGE="${DOCKER_REPO}:${DOCKER_TAG}"
printenv | sort

docker buildx build --platform=linux/amd64 -t ${DOCKER_IMAGE} .
aws ecr-public get-login-password --region ${AWS_REGION} --profile ${AWS_PROFILE} | \
    docker login --username AWS --password-stdin ${AWS_ECR}
docker push ${DOCKER_IMAGE}
