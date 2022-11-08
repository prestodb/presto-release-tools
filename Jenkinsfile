pipeline {

    agent {
        kubernetes {
            defaultContainer 'maven'
            yamlFile 'agent.yaml'
        }
    }

    environment {
        AWS_CREDENTIAL_ID  = 'aws-jenkins'
        AWS_DEFAULT_REGION = 'us-east-1'
        AWS_ECR            = credentials('aws-ecr-private-registry')
        AWS_S3_PREFIX      = 's3://oss-jenkins/artifact/presto'
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
                             credentialsId: 'github-personal-token-wanglinsong',
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
                         ],[
                             $class: 'CloneOption',
                             shallow: true,
                             noTags:  true,
                             depth:   1,
                             timeout: 10
                         ]],
                         submoduleCfg: [],
                         userRemoteConfigs: [[
                             credentialsId: 'github-personal-token-wanglinsong',
                             url: 'https://github.com/prestodb/presto'
                         ]]
                dir('presto') {
                    sh 'unset MAVEN_CONFIG && ./mvnw versions:set -DremoveSnapshot -ntp'
                    script {
                        env.PRESTO_STABLE_RELEASE_VERSION = sh(
                            script: 'unset MAVEN_CONFIG && ./mvnw org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -ntp -DforceStdout',
                            returnStdout: true).trim()
                    }
                }
                echo "Presto next stable release version ${PRESTO_STABLE_RELEASE_VERSION}"
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
                    script {
                        env.DOCKER_IMAGE_TAG = sh(
                            script: 'scripts/get-releasable-docker-image-tag.sh',
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
                dir('presto') {
                    sh '''
                        EDGE_RELEASES=$(git branch -r | grep "edge-${PRESTO_STABLE_RELEASE_VERSION}") || EDGE_RELEASES=''
                        EDGE_N=$(echo $EDGE_RELEASES | grep -v ^$ | wc -l)
                        PRESTO_EDGE_RELEASE_VERSION="${PRESTO_STABLE_RELEASE_VERSION}-edge$((EDGE_N+1))"
                        echo "new presto edge release version: ${PRESTO_EDGE_RELEASE_VERSION}"
                        echo ${PRESTO_EDGE_RELEASE_VERSION} > PRESTO_EDGE_RELEASE_VERSION.version
                    '''
                    script {
                        env.PRESTO_EDGE_RELEASE_VERSION = sh(
                            script: 'cat PRESTO_EDGE_RELEASE_VERSION.version',
                            returnStdout: true).trim()
                    }
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
                        aws s3 cp ${AWS_S3_PREFIX}/${PRESTO_BUILD_VERSION}/ ${AWS_S3_PREFIX}/${PRESTO_EDGE_RELEASE_VERSION}/ --recursive --no-progress
                    '''
                }
            }
        }

        stage ('Create Release Docker Image') {
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
                            docker tag "${AWS_ECR}/oss-presto/presto:${DOCKER_IMAGE_TAG}" "${AWS_ECR}/oss-presto/presto:${PRESTO_EDGE_RELEASE_VERSION}"
                            docker image ls
                            docker push ${AWS_ECR}/oss-presto/presto:${PRESTO_EDGE_RELEASE_VERSION}
                        '''
                    }
                }
            }
        }

        stage ('Create Release Branch') {
            steps {
                dir('presto') {
                    withCredentials([
                            usernamePassword(credentialsId: 'github-personal-token-wanglinsong',
                                             passwordVariable: 'GIT_PASSWORD',
                                             usernameVariable: 'GIT_USERNAME')]) {
                        sh '''
                            ORIGIN="https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/prestodb/presto.git"
                            EDGE_BRANCH="edge-${PRESTO_EDGE_RELEASE_VERSION}"
                            git checkout ${PRESTO_RELEASE_SHA}
                            git reset --hard
                            git checkout -b ${EDGE_BRANCH}
                            unset MAVEN_CONFIG && ./mvnw --batch-mode release:update-versions -DautoVersionSubmodules=true -DdevelopmentVersion="${PRESTO_EDGE_RELEASE_VERSION}-SNAPSHOT"
                            git status | grep pom.xml | grep -v versionsBackup  | awk '{print $2}' | xargs git add
                            git status
                            git diff pom.xml | cat
                            git config --global user.email "wanglinsong@gmail.com"
                            git config --global user.name "Linsong Wang"
                            git commit -m "branch of ${PRESTO_EDGE_RELEASE_VERSION}"
                            git branch -r | grep edge-
                            git push --set-upstream ${ORIGIN} ${EDGE_BRANCH}
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo "release artifacts: ${AWS_S3_PREFIX}/${PRESTO_EDGE_RELEASE_VERSION}/"
            echo "docker image: ${AWS_ECR}/oss-presto/presto:${PRESTO_EDGE_RELEASE_VERSION}"
        }
        cleanup {
            cleanWs()
        }
    }
}
