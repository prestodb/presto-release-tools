pipeline {

    agent {
        kubernetes {
            defaultContainer 'maven'
            yamlFile 'agent.yaml'
        }
    }

    environment {
        GITHUB_TOKEN_ID      = 'github-token-presto-release-bot'
        AWS_CREDENTIAL_ID    = 'aws-jenkins'
        AWS_DEFAULT_REGION   = 'us-east-1'
        AWS_ECR              = credentials('aws-ecr-private-registry')
        AWS_ECR_PUBLIC       = 'public.ecr.aws/c0e3k9s8'
        AWS_S3_PREFIX        = 's3://oss-jenkins/artifact/presto'
        S3_URL_BASE          = 'https://oss-presto-release.s3.amazonaws.com/presto'
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '500'))
        timeout(time: 1, unit: 'HOURS')
    }

    stages {
        stage('Setup') {
            steps {
                sh 'apt update && apt install -y awscli git jq tree'
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
                    git config --global user.email "oss-release-bot@prestodb.io"
                    git config --global user.name "oss-release-bot"
                    git branch
                    git switch -c master
                    mvn versions:set -DremoveSnapshot -ntp
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
                    sh '''#!/bin/bash -ex
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
                        credentialsId: "${GITHUB_TOKEN_ID}",
                        passwordVariable: 'GIT_PASSWORD',
                        usernameVariable: 'GIT_USERNAME')]) {
                    sh '''
                        cd presto
                        git reset --hard ${PRESTO_RELEASE_SHA}
                        git --no-pager log --since="40 days ago" --graph --pretty=format:'%C(auto)%h%d%Creset %C(cyan)(%cd)%Creset %C(green)%cn <%ce>%Creset %s'

                        mvn release:branch --batch-mode  \
                            -DbranchName=release-${PRESTO_RELEASE_VERSION} \
                            -DgenerateBackupPoms=false
                        git branch
                        ORIGIN="https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/prestodb/presto.git"
                        git push --set-upstream --dry-run ${ORIGIN} master
                    '''
                }
            }
        }

        stage ('Push Release Branch/Tag') {
            steps {
                withCredentials([usernamePassword(
                        credentialsId: "${GITHUB_TOKEN_ID}",
                        passwordVariable: 'GIT_PASSWORD',
                        usernameVariable: 'GIT_USERNAME')]) {
                    sh '''
                        cd presto
                        ORIGIN="https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/prestodb/presto.git"
                        git checkout release-${PRESTO_RELEASE_VERSION}
                        git tag -a ${PRESTO_RELEASE_VERSION} -m "stable release ${PRESTO_RELEASE_VERSION}"
                        git push --dry-run ${ORIGIN} ${PRESTO_RELEASE_VERSION}
                        mvn versions:set -DnewVersion="${PRESTO_RELEASE_VERSION}.1-SNAPSHOT" -DgenerateBackupPoms=false
                        git add .
                        git commit -m "Update stable release branch development version to ${PRESTO_RELEASE_VERSION}.1-SNAPSHOT"
                        git push --set-upstream --dry-run ${ORIGIN} release-${PRESTO_RELEASE_VERSION}
                    '''
                }
            }
        }

        stage ('Generate Release Notes') {
            steps {
                withCredentials([usernamePassword(
                        credentialsId: "${GITHUB_TOKEN_ID}",
                        passwordVariable: 'GIT_PASSWORD',
                        usernameVariable: 'GIT_USERNAME')]) {
                    sh '''
                        cd presto
                        git checkout master
                        # src/release/release-notes.sh $GIT_USERNAME $GIT_PASSWORD
                    '''
                }
            }
        }

        stage ('Rlease Maven Artifacts') {
            environment {
                SONATYPE_NEXUS_CREDS    = credentials('presto-sonatype-nexus-creds')
                SONATYPE_NEXUS_USERNAME = "$SONATYPE_NEXUS_CREDS_USR"
                SONATYPE_NEXUS_PASSWORD = "$SONATYPE_NEXUS_CREDS_PSW"
                GPG_SECRET     = credentials('presto-release-gpg-secret')
                GPG_TRUST      = credentials("presto-release-gpg-trust")
                GPG_PASSPHRASE = credentials("presto-release-gpg-passphrase")
            }
            steps {
                echo 'release all jars and the server tarball to Maven Central'
                sh '''
                    gpg --batch --import ${GPG_SECRET}}
                    gpg --import-ownertrust ${GPG_TRUST}

                    cd presto
                    export GPG_TTY=$(tty)
                    mvn -s ${WORKSPACE}/settings.xml -V -B -U -e -T2C clean deploy \
                        -DaltDeploymentRepository=your_repo_id::file:/tmp/alt-repo
                        -Dgpg.passphrase=${GPG_PASSPHRASE} \
                        -Dmaven.artifact.threads=20 \
                        -Dair.test.jvmsize=5g -Dmaven.wagon.http.retryHandler.count=3 \
                        -DskipTests \
                        -Poss-release \
                        -Pdeploy-to-ossrh \
                        -DstagingProfileId=28a0d8c4350ed \
                        -DkeepStagingRepositoryOnFailure=true \
                        -DkeepStagingRepositoryOnCloseRuleFailure=true \
                        -DautoReleaseAfterClose=true \
                        -DstagingProgressTimeoutMinutes=10 \
                        -pl '!presto-test-coverage,!presto-native-execution' 2>&1 | tee /root/mvn-deploy-$(date +%Y%m%dT%H%M%S).log
                '''
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
            when {
                expression {false}
            }
            steps {
                container('dind') {
                    sh 'apk update && apk add aws-cli'
                    withCredentials([[
                            $class: 'AmazonWebServicesCredentialsBinding',
                            credentialsId: "${AWS_CREDENTIAL_ID}",
                            accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                            secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                        script {
                            sh '''
                                aws ecr get-login-password | docker login --username AWS --password-stdin ${AWS_ECR}
                                docker pull "${AWS_ECR}/oss-presto/presto:${DOCKER_IMAGE_TAG}"
                                docker tag "${AWS_ECR}/oss-presto/presto:${DOCKER_IMAGE_TAG}" "${AWS_ECR_PUBLIC}/presto:${PRESTO_RELEASE_VERSION}"
                                docker image ls

                                aws ecr-public get-login-password | docker login --username AWS --password-stdin ${AWS_ECR_PUBLIC}
                                docker push "${AWS_ECR_PUBLIC}/presto:${PRESTO_RELEASE_VERSION}"
                            '''
                        }
                    }
                }
            }
        }
    }
}
