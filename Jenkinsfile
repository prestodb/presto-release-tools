pipeline {

    agent {
        kubernetes {
            defaultContainer 'maven'
            yamlFile 'agent.yaml'
        }
    }

    environment {
        AWS_CREDENTIAL_ID  = 'aws-jenkins'
        AWS_S3_PREFIX      = 's3://oss-presto-release/presto'
        DOCKER_PUBLIC      = 'docker.io/prestodb'
        GITHUB_TOKEN_ID    = 'github-token-presto-release-bot'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '100'))
        disableConcurrentBuilds()
        disableResume()
        overrideIndexTriggers(false)
        timeout(time: 3, unit: 'HOURS')
        timestamps()
    }

    triggers {
        cron('H 10 * * 2')
    }

    stages {
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
                    git switch -C master
                    git config --global user.email "oss-release-bot@prestodb.io"
                    git config --global user.name "oss-release-bot"
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
                    env.EDGE_BRANCH = "release-" + env.PRESTO_EDGE_RELEASE_VERSION
                }
            }
        }

        stage ('Create Release Branch') {
            options {
                retry(5)
            }
            steps {
                withCredentials([
                        usernamePassword(
                            credentialsId: "${GITHUB_TOKEN_ID}",
                            passwordVariable: 'GIT_PASSWORD',
                            usernameVariable: 'GIT_USERNAME')]) {
                    sh '''
                        ORIGIN="https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/prestodb/presto.git"
                        cd presto
                        git checkout master
                        git reset --hard
                        git branch -D ${EDGE_BRANCH} || echo OK
                        rm release.properties || echo OK
                        git checkout -b ${EDGE_BRANCH}
                        git log -5

                        unset MAVEN_CONFIG && ./mvnw --batch-mode release:prepare \
                            -DautoVersionSubmodules=true -DgenerateBackupPoms=false -DskipTests \
                            -DdevelopmentVersion="${PRESTO_EDGE_RELEASE_VERSION}.1-SNAPSHOT" \
                            -DreleaseVersion="${PRESTO_EDGE_RELEASE_VERSION}" \
                            -Dtag="${PRESTO_EDGE_RELEASE_VERSION}"
                        git log -5
                        git push --follow-tags --set-upstream ${ORIGIN} ${EDGE_BRANCH}
                    '''
                }
            }
        }

        stage ('Build Docker Images') {
            options {
                retry(3)
            }
            steps {
                sh 'sleep 10'
                script {
                    def downstream =
                        build job: '/prestodb/presto/' + env.EDGE_BRANCH,
                            wait: true,
                            parameters: [
                                booleanParam(name: 'PUBLISH_ARTIFACTS_ON_CURRENT_BRANCH', value: true)
                            ]
                    env.PRESTO_BUILD_VERSION = downstream.buildVariables.PRESTO_BUILD_VERSION
                    env.DOCKER_IMAGE = downstream.buildVariables.DOCKER_IMAGE
                }
            }
        }

        stage ('Push Docker Images') {
            environment {
                DOCKERHUB_PRESTODB_CREDS = credentials('docker-hub-prestodb-push-token')
            }
            steps {
                container('dind') {
                    sh '''
                        docker pull "${DOCKER_IMAGE}-amd64"
                        docker pull "${DOCKER_IMAGE}-arm64"
                        docker pull "${DOCKER_IMAGE}"
                        docker tag "${DOCKER_IMAGE}-amd64" "${DOCKER_PUBLIC}/presto:${PRESTO_EDGE_RELEASE_VERSION}-amd64"
                        docker tag "${DOCKER_IMAGE}-arm64" "${DOCKER_PUBLIC}/presto:${PRESTO_EDGE_RELEASE_VERSION}-arm64"
                        docker tag "${DOCKER_IMAGE}"       "${DOCKER_PUBLIC}/presto:${PRESTO_EDGE_RELEASE_VERSION}"
                        docker image ls
                        echo ${DOCKERHUB_PRESTODB_CREDS_PSW} | docker login --username ${DOCKERHUB_PRESTODB_CREDS_USR} --password-stdin
                        docker push ${DOCKER_PUBLIC}/presto:${PRESTO_EDGE_RELEASE_VERSION}-amd64
                        docker push ${DOCKER_PUBLIC}/presto:${PRESTO_EDGE_RELEASE_VERSION}-arm64
                        docker push ${DOCKER_PUBLIC}/presto:${PRESTO_EDGE_RELEASE_VERSION}
                    '''
                }
            }
        }

        stage ('Upload Release Packages') {
            steps {
                withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${AWS_CREDENTIAL_ID}",
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    sh '''
                        aws s3 ls "s3://oss-prestodb/presto/${PRESTO_BUILD_VERSION}/"
                        aws s3 cp "s3://oss-prestodb/presto/${PRESTO_BUILD_VERSION}/" "${AWS_S3_PREFIX}/${PRESTO_EDGE_RELEASE_VERSION}/" --recursive --no-progress
                    '''
                }
            }
        }

        stage ('Run TPC-H SF1') {
            steps {
                build job: '/oss-presto-pipelines/oss-release/dvt-prestodb-benchto',
                    wait: true,
                    parameters: [
                        string(name: 'PRESTO_BRANCH',       value:  env.EDGE_BRANCH),
                        string(name: 'PRESTO_CLUSTER_SIZE', value:  'tiny'),
                        string(name: 'BENCHTO_WORKLOAD',    value:  'tpch')
                    ]
            }
        }

        stage ('Run TPC-H/DS SF100') {
            steps {
                build job: '/oss-presto-pipelines/oss-release/dvt-prestodb-benchto',
                    wait: false,
                    parameters: [
                        string(name: 'PRESTO_BRANCH',       value:  env.EDGE_BRANCH),
                        string(name: 'PRESTO_CLUSTER_SIZE', value:  'small'),
                        string(name: 'BENCHTO_WORKLOAD',    value:  'tpch')
                    ]

                build job: '/oss-presto-pipelines/oss-release/dvt-prestodb-benchto',
                    wait: false,
                    parameters: [
                        string(name: 'PRESTO_BRANCH',       value:  env.EDGE_BRANCH),
                        string(name: 'PRESTO_CLUSTER_SIZE', value:  'small'),
                        string(name: 'BENCHTO_WORKLOAD',    value:  'tpcds')
                    ]
            }
        }
    }
}
