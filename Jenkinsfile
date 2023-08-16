pipeline {

    agent {
        kubernetes {
            defaultContainer 'maven'
            yamlFile 'agent.yaml'
        }
    }

    environment {
        GITHUB_TOKEN_ID = 'github-token-wanglinsong'
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '500'))
        timeout(time: 1, unit: 'HOURS')
    }

    parameters {
        string(name: 'VERSION_TO_BE_RELEASED',
               defaultValue: '',
               description: 'the version of presto-docs to be released, such as 0.279'
        )
        booleanParam(name: 'MARK_AS_CURRENT',
                     defaultValue: false,
                     description: 'mark the new version as current'
        )
        string(name: 'VERSION_CURRENTLY_RELEASED',
               defaultValue: '',
               description: 'the version of presto-docs that has been released, such as 0.278'
        )
    }

    stages {
        stage ('Setup') {
            steps {
                sh 'apt update && apt install -y curl git unzip'
            }
        }

        stage ('Load Presto Repo') {
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
                             url: 'https://github.com/prestodb/presto.git'
                         ]]
                sh '''
                    cd presto
                    git config --global --add safe.directory ${WORKSPACE}/presto
                '''
            }
        }

        stage ('Load Website Repo') {
            steps {
                checkout $class: 'GitSCM',
                         branches: [[name: '*/source']],
                         doGenerateSubmoduleConfigurations: false,
                         extensions: [[
                             $class: 'RelativeTargetDirectory',
                             relativeTargetDir: 'prestodb.github.io'
                         ]],
                         submoduleCfg: [],
                         userRemoteConfigs: [[
                             credentialsId: "${GITHUB_TOKEN_ID}",
                             url: 'https://github.com/wanglinsong/prestodb.github.io.git'
                         ]]
                sh '''
                    cd prestodb.github.io
                    git config --global --add safe.directory ${WORKSPACE}/prestodb.github.io
                    git config --global user.email "presto-release-bot@prestodb.io"
                    git config --global user.name "presto-release-bot"
                    git switch source
                    git branch -vv
                    git remote add upstream https://github.com/prestodb/prestodb.github.io.git
                    git fetch upstream
                    git rebase upstream/source
                '''
            }
        }

        stage ('Update Docs') {
            steps {
                sh '''#!/bin/bash -ex
                    cd prestodb.github.io
                    ls -al

                    PRESTO_GIT_REPO=../presto
                    TARGET=website/static/docs/${VERSION_TO_BE_RELEASED}
                    CURRENT=website/static/docs/current

                    if [[ -e ${TARGET} ]]; then
                        echo "already exists: ${TARGET}"
                        exit 100
                    fi

                    curl -O https://repo1.maven.org/maven2/com/facebook/presto/presto-docs/${VERSION_TO_BE_RELEASED}/presto-docs-${VERSION_TO_BE_RELEASED}.zip
                    unzip presto-docs-${VERSION_TO_BE_RELEASED}.zip
                    mv html ${TARGET}
                    unlink ${CURRENT}
                    ln -sf ${VERSION_TO_BE_RELEASED} ${CURRENT}
                    git add ${TARGET} ${CURRENT}
                    git status

                    DATE=$(TZ=America/Los_Angeles date '+%B %d, %Y')
                    echo "Update the version number and stats in javascript for rendering across the site"
                    VERSION_JS=website/static/static/js/version.js

                    echo "const presto_latest_presto_version = '${VERSION_TO_BE_RELEASED}';" > ${VERSION_JS}
                    GIT_LOG="git -C ../presto log --use-mailmap ${VERSION_CURRENTLY_RELEASED}..${VERSION_TO_BE_RELEASED}"
                    NUM_COMMITS=$(${GIT_LOG} --format='%aE' | wc -l | awk '{$1=$1;print}')
                    NUM_CONTRIBUTORS=$(${GIT_LOG} --format='%aE' | sort | uniq | wc -l | awk '{$1=$1;print}')
                    NUM_COMMITTERS=$(${GIT_LOG} --format='%cE' | sort | uniq | wc -l | awk '{$1=$1;print}')
                    echo "const presto_latest_num_commits = ${NUM_COMMITS};" >> ${VERSION_JS}
                    echo "const presto_latest_num_contributors = ${NUM_CONTRIBUTORS};" >> ${VERSION_JS}
                    echo "const presto_latest_num_committers = ${NUM_COMMITTERS};" >> ${VERSION_JS}
                    echo "const presto_latest_date = '${DATE}';" >> ${VERSION_JS}
                    cat ${VERSION_JS}
                    git add ${VERSION_JS}
                    git status
                '''
            }
        }

        stage ('Push Updates') {
            steps {
                withCredentials([
                        usernamePassword(
                            credentialsId: "${GITHUB_TOKEN_ID}",
                            passwordVariable: 'GIT_PASSWORD',
                            usernameVariable: 'GIT_USERNAME')]) {
                    sh '''
                        cd prestodb.github.io
                        ORIGIN="https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/wanglinsong/prestodb.github.io.git"

                        git status
                        git commit -m "Add ${VERSION_TO_BE_RELEASED} docs"
                        git checkout -b "${VERSION_TO_BE_RELEASED}-docs"
                        git push --set-upstream ${ORIGIN} relase-${VERSION_TO_BE_RELEASED}-docs
                    '''
                }
            }
        }
    }
}
