pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk   'JDK-17'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            parallel {
                stage('auth-service') {
                    steps {
                        dir('auth-service') {
                            sh 'mvn clean package -Dmaven.test.skip=true -B'
                        }
                    }
                }
                stage('candidate-service') {
                    steps {
                        dir('candidate-service') {
                            sh 'mvn clean package -Dmaven.test.skip=true -B'
                        }
                    }
                }
                stage('job-offer-service') {
                    steps {
                        dir('job-offer-service') {
                            sh 'mvn clean package -Dmaven.test.skip=true -B'
                        }
                    }
                }
                stage('ai-service') {
                    steps {
                        dir('ai-service') {
                            sh 'mvn clean package -Dmaven.test.skip=true -B'
                        }
                    }
                }
                stage('api-gateway') {
                    steps {
                        dir('api-gateway') {
                            sh 'mvn clean package -Dmaven.test.skip=true -B'
                        }
                    }
                }
            }
        }

        stage('Build Images') {
            steps {
                sh '''
                    podman build -t job-platform/auth-service:${BUILD_NUMBER}      ./auth-service
                    podman build -t job-platform/candidate-service:${BUILD_NUMBER}  ./candidate-service
                    podman build -t job-platform/job-offer-service:${BUILD_NUMBER}  ./job-offer-service
                    podman build -t job-platform/ai-service:${BUILD_NUMBER}         ./ai-service
                    podman build -t job-platform/api-gateway:${BUILD_NUMBER}        ./api-gateway
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    python -m podman_compose down --remove-orphans
                    python -m podman_compose up -d
                '''
            }
        }
    }

    post {
        success {
            echo 'Build #${BUILD_NUMBER} réussi'
        }
        failure {
            echo 'Build #${BUILD_NUMBER} échoué'
        }
        always {
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
        }
    }
}