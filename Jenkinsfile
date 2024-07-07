AGENT_YAML = '''
    apiVersion: v1
    kind: Pod
    metadata:
      namespace: oss-agent
    spec:
      serviceAccountName: oss-agent
      containers:
      - name: dind
        image: docker:24.0.7-dind-alpine3.19
        securityContext:
          privileged: true
        tty: true
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
'''

pipeline {

    agent {
        kubernetes {
            defaultContainer 'dind'
            yaml AGENT_YAML
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
        timeout(time: 4, unit: 'HOURS')
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
                    env.NATIVE_DOCKER_IMAGE = downstream.buildVariables.NATIVE_DOCKER_IMAGE
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
                        docker buildx create --name="container" --driver=docker-container --bootstrap
                        echo ${DOCKERHUB_PRESTODB_CREDS_PSW} | docker login --username ${DOCKERHUB_PRESTODB_CREDS_USR} --password-stdin
                        docker buildx imagetools create --builder container --progress tty -t ${DOCKER_PUBLIC}/presto:${PRESTO_RELEASE_VERSION} "${DOCKER_IMAGE}"
                        docker buildx imagetools create --builder container --progress tty -t ${DOCKER_PUBLIC}/presto:latest "${DOCKER_IMAGE}"

                        docker pull ${NATIVE_DOCKER_IMAGE}
                        docker tag ${NATIVE_DOCKER_IMAGE} ${DOCKER_PUBLIC}/presto-native:${PRESTO_RELEASE_VERSION}
                        docker tag ${NATIVE_DOCKER_IMAGE} ${DOCKER_PUBLIC}/presto-native:latest
                        docker push ${DOCKER_PUBLIC}/presto-native:${PRESTO_RELEASE_VERSION}
                        docker push ${DOCKER_PUBLIC}/presto-native:latest
                    '''
                }
            }
        }
    }
}
