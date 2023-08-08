pipeline {

    agent {
        kubernetes {
            defaultContainer 'dind'
            yamlFile 'agent.yaml'
        }
    }

    environment {
        DOCKER_PUBLIC = 'docker.io/prestodb'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '100'))
        disableConcurrentBuilds()
        disableResume()
        overrideIndexTriggers(false)
        timeout(time: 2, unit: 'HOURS')
        timestamps()
    }

    parameters {
        string(name: 'PRESTO_RELEASE_VERSION',
               defaultValue: '',
               description: 'usually a stable release version, also a git tag, like <pre>0.283</pre>')
    }

    stages {
        stage ('Build Docker Images') {
            options {
                retry(3)
            }
            steps {
                sh 'sleep 10'
                script {
                    def downstream =
                        build job: '/prestodb/presto/' + env.PRESTO_RELEASE_VERSION,
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
                        docker tag "${DOCKER_IMAGE}-amd64" "${DOCKER_PUBLIC}/presto:${PRESTO_RELEASE_VERSION}-amd64"
                        docker tag "${DOCKER_IMAGE}-arm64" "${DOCKER_PUBLIC}/presto:${PRESTO_RELEASE_VERSION}-arm64"
                        docker tag "${DOCKER_IMAGE}"       "${DOCKER_PUBLIC}/presto:${PRESTO_RELEASE_VERSION}"
                        docker image ls
                        echo ${DOCKERHUB_PRESTODB_CREDS_PSW} | docker login --username ${DOCKERHUB_PRESTODB_CREDS_USR} --password-stdin
                        docker push ${DOCKER_PUBLIC}/presto:${PRESTO_RELEASE_VERSION}-amd64
                        docker push ${DOCKER_PUBLIC}/presto:${PRESTO_RELEASE_VERSION}-arm64
                        docker push ${DOCKER_PUBLIC}/presto:${PRESTO_RELEASE_VERSION}
                    '''
                }
            }
        }
    }
}
