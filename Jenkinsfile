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

    parameters {
        booleanParam(name: 'RELEASE_DRYRUN',
                     defaultValue: true,
                     description: 'dryrun without creating a PR to update master branch'
        )
    }

    triggers {
        cron('0 19 1-7 * 2')
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
                sh 'unset MAVEN_CONFIG && cd presto/ && ./mvnw versions:set -DremoveSnapshot'
                script {
                    env.PRESTO_RELEASE_VERSION = sh(
                        script: 'unset MAVEN_CONFIG && cd presto/ && ./mvnw org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout',
                        returnStdout: true).trim()
                }
                echo "Presto release version ${PRESTO_RELEASE_VERSION}"
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
                    script {
                        env.DOCKER_IMAGE_TAG = sh(
                            script: 'scripts/get-releasable-docker-image-tag.sh',
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

        stage('Release to Maven Central') {
            steps {
                echo 'release all jars to Maven Central'
            }
        }

        stage ('Create Release Tarballs') {
            steps {
                withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${AWS_CREDENTIAL_ID}",
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    script {
                        sh '''
                            aws s3 cp "${AWS_S3_PREFIX}/${PRESTO_BUILD_VERSION}/" "${AWS_S3_PREFIX}/${PRESTO_RELEASE_VERSION}/" --recursive
                        '''
                    }
                }
            }
        }

        stage ('Create Release Docker Image') {
            agent {
                kubernetes {
                    defaultContainer 'dind'
                    yamlFile 'agent-dind.yaml'
                }
            }
            steps {
                withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${AWS_CREDENTIAL_ID}",
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    script {
                        sh '''
                            docker pull "${AWS_ECR}/oss-presto/presto:${DOCKER_IMAGE_TAG}"
                            docker tag "${AWS_ECR}/oss-presto/presto:${DOCKER_IMAGE_TAG}" "${AWS_ECR}/oss-presto/presto:${PRESTO_RELEASE_VERSION}"
                            docker image ls
                            docker push "${AWS_ECR}/oss-presto/presto:${PRESTO_RELEASE_VERSION}"
                        '''
                    }
                }
            }
        }
    }

    post {
        cleanup {
            cleanWs()
        }
    }
}
