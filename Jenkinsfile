pipeline {

    agent {
        kubernetes {
            defaultContainer 'maven'
            yamlFile 'agent.yaml'
        }
    }

    environment {
        GITHUB_OSS_TOKEN_ID = 'github-token-presto-release-bot'
        AWS_CREDENTIAL_ID   = 'aws-jenkins'
        AWS_DEFAULT_REGION  = 'us-east-1'
        AWS_ECR             = credentials('aws-ecr-private-registry')
        AWS_S3_PREFIX       = 's3://oss-jenkins/artifact/presto'
        S3_URL_BASE         = 'https://oss-presto-release.s3.amazonaws.com/presto'
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '500'))
        timeout(time: 1, unit: 'HOURS')
    }

    stages {
        stage('Setup') {
            steps {
                sh 'apt update && apt install -y awscli git jq make python3 python3-venv tree'
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
                            credentialsId: "${GITHUB_OSS_TOKEN_ID}",
                            url: 'https://github.com/prestodb/presto.git'
                        ]]
                sh '''
                    cd presto
                    git config --global --add safe.directory ${PWD}
                    git config --global user.email "oss-release-bot@prestodb.io"
                    git config --global user.name "oss-release-bot"
                    git config pull.rebase false
                    git branch
                    git switch -c master
                    unset MAVEN_CONFIG && ./mvnw versions:set -DremoveSnapshot -ntp
                '''
                script {
                    env.PRESTO_RELEASE_VERSION = sh(
                        script: 'cd presto && mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -ntp -DforceStdout',
                        returnStdout: true).trim()
                }
                echo "Presto release version ${PRESTO_RELEASE_VERSION}"
                sh '''
                    cd presto
                    git reset --hard
                '''
            }
        }

        stage ('Search Artifacts') {
            steps {
                echo "query for presto docker images with release version ${PRESTO_RELEASE_VERSION}"
                withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${AWS_CREDENTIAL_ID}",
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    sh '''#!/usr/bin/env bash -ex
                        cd presto
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
                echo "${AWS_S3_PREFIX}/${PRESTO_BUILD_VERSION}/presto-server-${PRESTO_RELEASE_VERSION}.tar.gz"
                echo "${AWS_ECR}/oss-presto/presto:${DOCKER_IMAGE_TAG}"
            }
        }

        stage ('Update Version in Master') {
            steps {
                withCredentials([usernamePassword(
                        credentialsId: "${GITHUB_OSS_TOKEN_ID}",
                        passwordVariable: 'GIT_PASSWORD',
                        usernameVariable: 'GIT_USERNAME')]) {
                    sh '''#!/usr/bin/env bash -ex
                        cd presto
                        git reset --hard ${PRESTO_RELEASE_SHA}
                        git --no-pager log --since="40 days ago" --graph --pretty=format:'%C(auto)%h%d%Creset %C(cyan)(%cd)%Creset %C(green)%cn <%ce>%Creset %s'

                        PRESTO_BUILD_VERSION_MAJOR=$(echo ${PRESTO_RELEASE_VERSION} | awk -F. '{print $1}')
                        PRESTO_BUILD_VERSION_MINOR=$(echo ${PRESTO_RELEASE_VERSION} | awk -F. '{print $2}')
                        unset MAVEN_CONFIG && ./mvnw release:prepare --batch-mode -DskipTests \
                            -DautoVersionSubmodules \
                            -DdevelopmentVersion="${PRESTO_BUILD_VERSION_MAJOR}.$((PRESTO_BUILD_VERSION_MINOR+1))-SNAPSHOT"

                        ORIGIN="https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/prestodb/presto.git"
                        head -n 18 pom.xml
                        git pull ${ORIGIN} master
                        git log --pretty="format:%ce: %s" -5
                        git push --follow-tags --set-upstream ${ORIGIN} master

                    '''
                }
            }
        }

        stage ('Push Release Branch') {
            steps {
                withCredentials([usernamePassword(
                        credentialsId: "${GITHUB_OSS_TOKEN_ID}",
                        passwordVariable: 'GIT_PASSWORD',
                        usernameVariable: 'GIT_USERNAME')]) {
                    sh '''
                        cd presto
                        ORIGIN="https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/prestodb/presto.git"

                        git checkout ${PRESTO_RELEASE_VERSION}
                        git switch -c release-${PRESTO_RELEASE_VERSION}

                        unset MAVEN_CONFIG && ./mvnw versions:set -DnewVersion="${PRESTO_RELEASE_VERSION}.1-SNAPSHOT" -DgenerateBackupPoms=false
                        git add .
                        git commit -m "Update stable release branch development version to ${PRESTO_RELEASE_VERSION}.1-SNAPSHOT"
                        git log --pretty="format:%ce: %s" -5
                        git push --set-upstream ${ORIGIN} release-${PRESTO_RELEASE_VERSION}
                    '''
                }
            }
        }

        stage ('Release Maven Artifacts') {
            steps {
                echo 'release all jars and the server tarball to Maven Central'
                build job: 'pipeline-release-maven-artifacts',
                    wait: false,
                    parameters: [
                        string(name: 'PRESTO_REPO_BRANCH_NAME', value: 'release-' + env.PRESTO_RELEASE_VERSION),
                        string(name: 'PRESTO_RELEASE_VERSION',  value: env.PRESTO_RELEASE_VERSION)
                    ]
            }
        }

        stage ('Generate Release Notes') {
            when {
                expression { false }
            }
            steps {
                withCredentials([usernamePassword(
                        credentialsId: 'github-token-presto-release-notes',
                        passwordVariable: 'GIT_PASSWORD',
                        usernameVariable: 'GIT_USERNAME')]) {
                    sh '''
                        cd presto
                        git checkout master
                        # src/release/release-notes.sh ${GIT_USERNAME} ${GIT_PASSWORD}
                        echo "this will be done manually"
                    '''
                }
            }
        }

        stage ('Release Tarballs') {
            steps {
                withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${AWS_CREDENTIAL_ID}",
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    script {
                        sh '''
                            aws s3 cp "${AWS_S3_PREFIX}/${PRESTO_BUILD_VERSION}/" "s3://oss-presto-release/presto/${PRESTO_RELEASE_VERSION}/" --recursive --no-progress
                        '''
                        echo "${S3_URL_BASE}/${PRESTO_RELEASE_VERSION}/presto-server-${PRESTO_RELEASE_VERSION}.tar.gz"
                        echo "${S3_URL_BASE}/${PRESTO_RELEASE_VERSION}/presto-cli-${PRESTO_RELEASE_VERSION}-executable.jar"
                    }
                }
            }
        }

        stage ('Release Docker Images') {
            environment {
                DOCKER_PUBLIC = 'docker.io/prestodb'
                DOCKERHUB_PRESTODB_CREDS = credentials('docker-hub-prestodb-push-token')
            }
            steps {
                container('dind') {
                    sh 'apk update && apk add aws-cli'
                    withCredentials([[
                            $class: 'AmazonWebServicesCredentialsBinding',
                            credentialsId: "${AWS_CREDENTIAL_ID}",
                            accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                            secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                        sh '''
                            aws ecr get-login-password | docker login --username AWS --password-stdin ${AWS_ECR}
                            docker pull "${AWS_ECR}/oss-presto/presto:${DOCKER_IMAGE_TAG}"
                            docker tag "${AWS_ECR}/oss-presto/presto:${DOCKER_IMAGE_TAG}" "${DOCKER_PUBLIC}/presto:${PRESTO_RELEASE_VERSION}"
                            docker image ls

                            echo ${DOCKERHUB_PRESTODB_CREDS_PSW} | docker login --username ${DOCKERHUB_PRESTODB_CREDS_USR} --password-stdin
                            docker push "${DOCKER_PUBLIC}/presto:${PRESTO_RELEASE_VERSION}"
                        '''
                    }
                }
            }
        }
    }
}
