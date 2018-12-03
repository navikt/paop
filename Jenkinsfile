#!/usr/bin/env groovy

pipeline {
    agent any

    environment {
        APPLICATION_NAME = 'paop'
        APPLICATION_SERVICE = 'TODO'
        APPLICATION_COMPONENT = 'TODO'
        FASIT_ENVIRONMENT = 'q1'
        ZONE = 'fss'
        DOCKER_SLUG = 'integrasjon'
    }

    stages {
            stage('initialize') {
                steps {
                    script {
                        init action: 'default'
                        sh './gradlew clean'
                        applicationVersionGradle = sh(script: './gradlew -q printVersion', returnStdout: true).trim()
                        env.APPLICATION_VERSION = "${applicationVersionGradle}"
                        if (applicationVersionGradle.endsWith('-SNAPSHOT')) {
                            env.APPLICATION_VERSION = "${applicationVersionGradle}.${env.BUILD_ID}-${env.COMMIT_HASH_SHORT}"
                        }
                        init action: 'updateStatus'
                    }
                }
            }
            stage('build') {
                steps {
                    sh './gradlew build -x test'
                }
            }
            stage('run tests (unit & intergration)') {
                steps {
                    sh './gradlew test'
                }
            }
            stage('create uber jar') {
                steps {
                    sh './gradlew shadowJar'
                    slackStatus status: 'passed'
                }
            }
            stage('deploy') {
                steps {
                    dockerUtils action: 'createPushImage'
                    nais action: 'validate'
                    nais action: 'upload'
                    deployApp action: 'jiraPreprod'
                }
            }
        }
        post {
            always {
                postProcess action: 'always'
            }
            success {
                postProcess action: 'success'
            }
            failure {
                postProcess action: 'failure'
            }
        }
}