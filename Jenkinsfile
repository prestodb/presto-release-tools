AGENT_YAML = '''
    apiVersion: v1
    kind: Pod
    metadata:
      namespace: oss-agent
    spec:
      nodeSelector:
        eks.amazonaws.com/nodegroup: eks-oss-presto-dynamic-managed-ng
      serviceAccountName: oss-agent
      containers:
      - name: maven
        image: maven:3.8.6-openjdk-8-slim
        tty: true
        resources:
          requests:
            memory: "6Gi"
            cpu: "2000m"
          limits:
            memory: "6Gi"
            cpu: "2000m"
        command:
        - cat
'''

pipeline {

    agent {
        kubernetes {
            defaultContainer 'maven'
            yaml AGENT_YAML
        }
    }

    environment {
        GITHUB_OSS_TOKEN_ID = 'github-token-presto-release-bot'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '500'))
        disableConcurrentBuilds()
        disableResume()
        overrideIndexTriggers(false)
        timeout(time: 1, unit: 'HOURS')
        timestamps()
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

        stage ('Update Version in Master') {
            steps {
                withCredentials([usernamePassword(
                        credentialsId: "${GITHUB_OSS_TOKEN_ID}",
                        passwordVariable: 'GIT_PASSWORD',
                        usernameVariable: 'GIT_USERNAME')]) {
                    sh '''
                        cd presto
                        git reset --hard ${PRESTO_RELEASE_SHA}
                        git --no-pager log --since="40 days ago" --graph --pretty=format:'%C(auto)%h%d%Creset %C(cyan)(%cd)%Creset %C(green)%cn <%ce>%Creset %s'

                        unset MAVEN_CONFIG && ./mvnw release:prepare --batch-mode \
                            -DskipTests \
                            -DautoVersionSubmodules \
                            -DdevelopmentVersion=${PRESTO_RELEASE_VERSION} \
                            -DreleaseVersion=${PRESTO_RELEASE_VERSION}
                        head -n 18 pom.xml
                        git log --pretty="format:%ce: %s" -5

                        ORIGIN="https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/prestodb/presto.git"
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
                        git checkout ${PRESTO_RELEASE_VERSION}
                        git switch -c release-${PRESTO_RELEASE_VERSION}
                        git branch
                        git log --pretty="format:%ce: %s" -3

                        ORIGIN="https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/prestodb/presto.git"
                        git push --set-upstream ${ORIGIN} release-${PRESTO_RELEASE_VERSION}
                    '''
                }
            }
        }
    }
}
