pipeline {
    agent any

    tools {
        jdk 'jdk21'
        maven 'maven'
    }

    environment {
        MAVEN_OPTS = '-Xmx2g -XX:MaxMetaspaceSize=512m'
        MODULE_SUBSET = 'schemaplexai-common,schemaplexai-model,schemaplexai-dao,schemaplexai-gateway,schemaplexai-system,schemaplexai-agent-engine'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn clean compile -pl $MODULE_SUBSET -am -q'
            }
        }

        stage('Test & Verify') {
            steps {
                sh 'mvn verify -pl $MODULE_SUBSET -am'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                    jacoco execPattern: '**/target/jacoco.exec',
                          classPattern: '**/target/classes',
                          sourcePattern: '**/src/main/java'
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/target/surefire-reports/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: '**/target/site/jacoco/**', allowEmptyArchive: true
        }
    }
}
