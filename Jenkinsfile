pipeline {

    agent {
        kubernetes {
            defaultContainer 'maven'
            yamlFile 'agent.yaml'
        }
    }

    environment {
        GITHUB_TOKEN_ID    = 'github-token-presto-release-bot'
        AWS_CREDENTIAL_ID  = 'aws-jenkins'
        AWS_DEFAULT_REGION = 'us-east-1'
        AWS_ECR            = credentials('aws-ecr-private-registry')
        AWS_S3_PREFIX      = 's3://oss-jenkins/artifact/presto'
        DOCKER_PUBLIC      = 'docker.io/prestodb'
        S3_URL_BASE        = 'https://oss-presto-release.s3.amazonaws.com/presto'
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '500'))
        timeout(time: 1, unit: 'HOURS')
    }

    triggers {
        cron('H 12 * * 2')
    }

    stages {
        stage ('Setup') {
            steps {
                sh 'apt update && apt install -y awscli git jq tree'
            }
        }

        stage ('Load Scripts') {
            steps {
                checkout $class: 'GitSCM',
                         branches: [[name: '*/master']],
                         doGenerateSubmoduleConfigurations: false,
                         extensions: [[
                             $class: 'RelativeTargetDirectory',
                             relativeTargetDir: 'presto-release-tools'
                         ],[
                             $class: 'CloneOption',
                             shallow: true,
                             noTags:  true,
                             depth:   1,
                             timeout: 10
                         ]],
                         submoduleCfg: [],
                         userRemoteConfigs: [[
                             credentialsId: "${GITHUB_TOKEN_ID}",
                             url: 'https://github.com/prestodb/presto-release-tools'
                         ]]
                echo 'all Jenkins pipeline related scripts are located in folder ./presto-release-tools/scripts'
            }
        }

        stage ('Load Presto Source') {
            steps {
                checkout $class: 'GitSCM',
                         branches: [[name: '*/master']],
                         doGenerateSubmoduleConfigurations: false,
                         extensions: [[
                             $class: 'RelativeTargetDirectory',
                             relativeTargetDir: 'presto'
                         ]],
                         submoduleCfg: [],
                         userRemoteConfigs: [[
                             credentialsId: "${GITHUB_TOKEN_ID}",
                             url: 'https://github.com/prestodb/presto.git'
                         ]]
                sh '''
                    cd presto
                    git config --global --add safe.directory ${PWD}
                    git config --global user.email "presto-release-bot@prestodb.io"
                    git config --global user.name "presto-release-bot"
                    unset MAVEN_CONFIG && ./mvnw versions:set -DremoveSnapshot -ntp
                '''
                script {
                    env.PRESTO_STABLE_RELEASE_VERSION = sh(
                        script: 'unset MAVEN_CONFIG && cd presto && ./mvnw org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -ntp -DforceStdout',
                        returnStdout: true).trim()
                }
                echo "Presto stable release version ${PRESTO_STABLE_RELEASE_VERSION}"
                sh '''
                    cd presto
                    git reset --hard
                '''
            }
        }

        stage ('Search Artifacts') {
            steps {
                echo "query for presto docker images with release version ${PRESTO_STABLE_RELEASE_VERSION}"
                withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${AWS_CREDENTIAL_ID}",
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    sh '''#!/bin/bash -ex
                        cd presto
                        TAGS=($(aws ecr list-images --repository-name oss-presto/presto \
                            | jq -r '.imageIds[].imageTag | select( . != null)' \
                            | grep "${PRESTO_STABLE_RELEASE_VERSION}-20" \
                            | sort -r))
                        for TAG in $TAGS
                        do
                            echo $TAG
                            sha=$(echo $TAG | awk -F- '{print $3}')
                            if git merge-base --is-ancestor $sha HEAD
                            then
                                echo $TAG > release-tag.txt
                                break
                            fi
                        done
                        ls -al
                        cat release-tag.txt
                    '''
                    script {
                        env.DOCKER_IMAGE_TAG = sh(
                            script: 'cat presto/release-tag.txt',
                            returnStdout: true).trim()
                        env.PRESTO_BUILD_VERSION = env.DOCKER_IMAGE_TAG.substring(0, env.DOCKER_IMAGE_TAG.lastIndexOf('-'));
                        env.PRESTO_RELEASE_SHA = env.PRESTO_BUILD_VERSION.substring(env.PRESTO_BUILD_VERSION.lastIndexOf('-') + 1);
                    }
                }
                sh 'printenv | sort'
                echo "${AWS_S3_PREFIX}/${PRESTO_BUILD_VERSION}/presto-server-${PRESTO_STABLE_RELEASE_VERSION}.tar.gz"
                echo "${AWS_ECR}/oss-presto/presto:${DOCKER_IMAGE_TAG}"
            }
        }

        stage ('Set Release Version') {
            steps {
                sh '''
                    cd presto
                    EDGE_N=$(git branch -r | grep "release-${PRESTO_STABLE_RELEASE_VERSION}-edge[0-9]$" | wc -l)
                    PRESTO_EDGE_RELEASE_VERSION="${PRESTO_STABLE_RELEASE_VERSION}-edge$((EDGE_N+1))"
                    echo "new presto edge release version: ${PRESTO_EDGE_RELEASE_VERSION}"
                    echo ${PRESTO_EDGE_RELEASE_VERSION} > PRESTO_EDGE_RELEASE_VERSION.version
                '''
                script {
                    env.PRESTO_EDGE_RELEASE_VERSION = sh(
                        script: 'cat presto/PRESTO_EDGE_RELEASE_VERSION.version',
                        returnStdout: true).trim()
                }
            }
        }

        stage ('Create Release Packages') {
            steps {
                withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${AWS_CREDENTIAL_ID}",
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    sh '''
                        aws s3 ls "${AWS_S3_PREFIX}/${PRESTO_BUILD_VERSION}/"
                        aws s3 cp "${AWS_S3_PREFIX}/${PRESTO_BUILD_VERSION}/" "s3://oss-presto-release/presto/${PRESTO_EDGE_RELEASE_VERSION}/" --recursive --no-progress
                    '''
                    echo "${S3_URL_BASE}/${PRESTO_EDGE_RELEASE_VERSION}/presto-server-${PRESTO_STABLE_RELEASE_VERSION}.tar.gz"
                    echo "${S3_URL_BASE}/${PRESTO_EDGE_RELEASE_VERSION}/presto-cli-${PRESTO_STABLE_RELEASE_VERSION}-executable.jar"
                }
            }
        }

        stage ('Create Release Docker Image') {
            environment {
                DOCKERHUB_PRESTODB_CREDS = credentials('docker-hub-prestodb-push-token')
            }
            steps {
                container('dind') {
                    sh 'apk update && apk add aws-cli'
                    withCredentials([[
                            $class: 'AmazonWebServicesCredentialsBinding',
                            credentialsId: "${AWS_CREDENTIAL_ID}",
                            accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                            secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                        ]]) {
                        sh '''
                            aws ecr get-login-password | docker login --username AWS --password-stdin ${AWS_ECR}
                            docker pull "${AWS_ECR}/oss-presto/presto:${DOCKER_IMAGE_TAG}"
                            docker tag "${AWS_ECR}/oss-presto/presto:${DOCKER_IMAGE_TAG}" "${DOCKER_PUBLIC}/presto:${PRESTO_EDGE_RELEASE_VERSION}"
                            docker image ls
                            echo ${DOCKERHUB_PRESTODB_CREDS_PSW} | docker login --username ${DOCKERHUB_PRESTODB_CREDS_USR} --password-stdin
                            docker push ${DOCKER_PUBLIC}/presto:${PRESTO_EDGE_RELEASE_VERSION}
                        '''
                    }
                }
            }
        }

        stage ('Create Release Branch') {
            steps {
                withCredentials([
                        usernamePassword(
                            credentialsId: "${GITHUB_TOKEN_ID}",
                            passwordVariable: 'GIT_PASSWORD',
                            usernameVariable: 'GIT_USERNAME')]) {
                    sh '''
                        cd presto
                        ORIGIN="https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/prestodb/presto.git"
                        EDGE_BRANCH="release-${PRESTO_EDGE_RELEASE_VERSION}"
                        git reset --hard
                        git checkout ${PRESTO_RELEASE_SHA}
                        git checkout -b ${EDGE_BRANCH}
                        git tag -a ${PRESTO_EDGE_RELEASE_VERSION} -m "edge release ${PRESTO_EDGE_RELEASE_VERSION}"
                        git push ${ORIGIN} ${PRESTO_EDGE_RELEASE_VERSION}

                        unset MAVEN_CONFIG && ./mvnw --batch-mode release:update-versions -DautoVersionSubmodules=true \
                            -DdevelopmentVersion="${PRESTO_EDGE_RELEASE_VERSION}-SNAPSHOT"
                        git config -l
                        git status | grep pom.xml | grep -v versionsBackup  | awk '{print $2}' | xargs git add
                        git status
                        git commit \
                            -m "Prepare for next development iteration - ${PRESTO_EDGE_RELEASE_VERSION}-SNAPSHOT" \
                            --author="oss-release-bot <oss-release-bot@prestodb.io>"
                        git log -n 3
                        git branch
                        git push --set-upstream ${ORIGIN} ${EDGE_BRANCH}
                    '''
                }
            }
        }

        stage ('Run Benchto') {
            steps {
                build job: '/oss-presto-pipelines/oss-release/dvt-prestodb-benchto',
                    wait: false,
                    parameters: [
                        string(name: 'PRESTO_BRANCH', value: "release-" + env.PRESTO_EDGE_RELEASE_VERSION)
                    ]
            }
        }
    }
}
