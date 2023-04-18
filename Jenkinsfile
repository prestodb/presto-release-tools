pipeline {

    agent {
        kubernetes {
            defaultContainer 'maven'
            yamlFile 'agent.yaml'
        }
    }

    environment {
        GITHUB_OSS_TOKEN_ID = 'github-token-presto-release-bot'

        SONATYPE_NEXUS_CREDS    = credentials('presto-sonatype-nexus-creds')
        SONATYPE_NEXUS_USERNAME = "$SONATYPE_NEXUS_CREDS_USR"
        SONATYPE_NEXUS_PASSWORD = "$SONATYPE_NEXUS_CREDS_PSW"
        GPG_SECRET     = credentials('presto-release-gpg-secret')
        GPG_TRUST      = credentials("presto-release-gpg-trust")
        GPG_PASSPHRASE = credentials("presto-release-gpg-passphrase")
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '500'))
        timeout(time: 5, unit: 'HOURS')
    }

    parameters {
        string(name: 'PRESTO_REPO_BRANCH_NAME',
               defaultValue: '',
               description: 'usually the stable release branch name, like <pre>release-0.280</pre>')
        string(name: 'PRESTO_RELEASE_VERSION',
               defaultValue: '',
               description: 'usually a stable release version, like <pre>0.280</pre>')
    }

    stages {
        stage('Setup') {
            steps {
                sh 'apt update && apt install -y bash git gpg'
            }
        }

        stage ('Load Presto Source') {
            steps {
                checkout $class: 'GitSCM',
                        branches: [[name: "*/${PRESTO_REPO_BRANCH_NAME}"]],
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
                    git branch
                    git switch -c ${PRESTO_REPO_BRANCH_NAME}
                    git branch
                    mvn versions:set -DnewVersion="${PRESTO_RELEASE_VERSION}" -DgenerateBackupPoms=false -ntp
                '''
            }
        }

        stage ('Setup GPG') {
            steps {
                sh '''#!/bin/bash -ex
                    export GPG_TTY=${TTY}
                    echo $GPG_TTY
                    gpg --batch --import ${GPG_SECRET}
                    echo ${GPG_TRUST} | gpg --import-ownertrust -
                    gpg --list-secret-keys
                    echo "allow-loopback-pinentry" >> ~/.gnupg/gpg-agent.conf
                '''
            }
        }

        stage ('Release Maven Artifacts') {
            options {
                retry(3)
            }
            steps {
                echo 'release all jars and the server tarball to Maven Central'
                sh '''#!/bin/bash -ex
                    export GPG_TTY=${TTY}

                    cd presto
                    mvn -s ${WORKSPACE}/settings.xml -V -B -U -e -T2C clean deploy \
                        -Dgpg.passphrase=${GPG_PASSPHRASE} \
                        -Dmaven.wagon.http.retryHandler.count=8 \
                        -DskipTests \
                        -DstagingProfileId=28a0d8c4350ed \
                        -DkeepStagingRepositoryOnFailure=true \
                        -DkeepStagingRepositoryOnCloseRuleFailure=true \
                        -DautoReleaseAfterClose=true \
                        -DstagingProgressTimeoutMinutes=60 \
                        -Poss-release \
                        -Pdeploy-to-ossrh \
                        -pl '!presto-docs,!presto-test-coverage,!presto-native-execution'
                '''
            }
        }
    }
}
