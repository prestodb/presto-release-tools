#!/bin/sh

PRESTO_RELEASE_VERSION=${PRESTO_RELEASE_VERSION:?need an env var like PRESTO_RELEASE_VERSION=0.278}

TAGS=($(aws ecr list-images --repository-name oss-presto/presto \
    | jq -r '.imageIds[].imageTag | select( . != null)' \
    | grep "${PRESTO_RELEASE_VERSION}-20" \
    | sort -r))

for TAG in $TAGS
do
    echo $TAG
    sha=$(echo $TAG | awk -F- '{print $3}')
    if git merge-base --is-ancestor $sha HEAD
    then
        echo done
        echo $TAG > release-tag.txt
        break
    fi
done

cat release-tag.txt
