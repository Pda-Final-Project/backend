pipeline {
    agent any
    environment {
        DOCKER_HUB_USER = "kimdongjae"
    }
    stages {
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/develop']],
                    userRemoteConfigs: [[
                        url: 'https://github.com/Pda-Final-Project/backend.git',
                        credentialsId: 'github-credentials'
                    ]]
                ])
            }
        }
        stage('Build JARs') {
            steps {
                sh './gradlew clean build -x test'
            }
        }
        stage('Build & Push Docker Images') {
            parallel {
                stage('execution Service') {
                   steps {
                       script {
                           def jarName = sh(script: "ls execution-service/build/libs/ | grep .jar", returnStdout: true).trim()
                           sh """
                               docker build --build-arg JAR_FILE=${jarName} -t $DOCKER_HUB_USER/execution-service:latest -f execution-service/Dockerfile execution-service/
                               docker push $DOCKER_HUB_USER/execution-service:latest
                           """
                       }
                   }
                }
                stage('Filling Service') {
                   steps {
                       script {
                           def jarName = sh(script: "ls filling-service/build/libs/ | grep .jar", returnStdout: true).trim()
                           sh """
                               docker build --build-arg JAR_FILE=${jarName} -t $DOCKER_HUB_USER/filling-service:latest -f filling-service/Dockerfile filling-service/
                               docker push $DOCKER_HUB_USER/filling-service:latest
                           """
                      }
                   }
                }
                stage('Matching Service') {
                   steps {
                       script {
                           def jarName = sh(script: "ls matching-service/build/libs/ | grep .jar", returnStdout: true).trim()
                           sh """
                               docker build --build-arg JAR_FILE=${jarName} -t $DOCKER_HUB_USER/matching-service:latest -f matching-service/Dockerfile matching-service/
                               docker push $DOCKER_HUB_USER/matching-service:latest
                           """
                       }
                   }
                }
                stage('Matching Service') {
                    steps {
                        script {
                            def jarName = sh(script: "ls matching-service/build/libs/ | grep .jar", returnStdout: true).trim()
                            sh """
                                docker build --build-arg JAR_FILE=${jarName} -t $DOCKER_HUB_USER/matching-service:latest -f matching-service/Dockerfile matching-service/
                                docker push $DOCKER_HUB_USER/matching-service:latest
                            """
                        }
                    }
                }
                stage('Notification Service') {
                    steps {
                        script {
                            def jarName = sh(script: "ls notification-service/build/libs/ | grep .jar", returnStdout: true).trim()
                            sh """
                                docker build --build-arg JAR_FILE=${jarName} -t $DOCKER_HUB_USER/notification-service:latest -f notification-service/Dockerfile notification-service/
                                docker push $DOCKER_HUB_USER/notification-service:latest
                            """
                        }
                    }
                }
                stage('Settlement Service') {
                    steps {
                        script {
                            def jarName = sh(script: "ls settlement-service/build/libs/ | grep .jar", returnStdout: true).trim()
                            sh """
                                docker build --build-arg JAR_FILE=${jarName} -t $DOCKER_HUB_USER/settlement-service:latest -f settlement-service/Dockerfile settlement-service/
                                docker push $DOCKER_HUB_USER/settlement-service:latest
                            """
                        }
                    }
                }
                stage('User Service') {
                    steps {
                        script {
                            def jarName = sh(script: "ls user-service/build/libs/ | grep .jar", returnStdout: true).trim()
                            sh """
                                docker build --build-arg JAR_FILE=${jarName} -t $DOCKER_HUB_USER/user-service:latest -f user-service/Dockerfile user-service/
                                docker push $DOCKER_HUB_USER/user-service:latest
                            """
                        }
                    }
                }
                stage('Order Service') {
                    steps {
                        script {
                            def jarName = sh(script: "ls order-service/build/libs/ | grep .jar", returnStdout: true).trim()
                            sh """
                                docker build --build-arg JAR_FILE=${jarName} -t $DOCKER_HUB_USER/order-service:latest -f order-service/Dockerfile order-service/
                                docker push $DOCKER_HUB_USER/order-service:latest
                            """
                        }
                    }
                }
            }
        }
    }
}
