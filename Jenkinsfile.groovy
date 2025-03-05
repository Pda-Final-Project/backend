pipeline {
    agent any
    environment {
        DOCKER_HUB_USER = "hamgeonwook"
    }
    stages {
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [[name: 'develop']],
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
                stage('Execution Service') {
                    steps {
                        buildAndPushDockerImage('execution-service')
                    }
                }
                stage('Filling Service') {
                    steps {
                        buildAndPushDockerImage('filling-service')
                    }
                }
                stage('Gateway Service') {  
                    steps {
                        buildAndPushDockerImage('gateway')
                    }
                }
                stage('Matching Service') {  
                    steps {
                        buildAndPushDockerImage('matching-service')
                    }
                }
                stage('Notification Service') {
                    steps {
                        buildAndPushDockerImage('notification-service')
                    }
                }
                stage('Settlement Service') {
                    steps {
                        buildAndPushDockerImage('settlement-service')
                    }
                }
                stage('User Service') {
                    steps {
                        buildAndPushDockerImage('user-service')
                    }
                }
                stage('Order Service') {
                    steps {
                        buildAndPushDockerImage('order-service')
                    }
                }
            }
        }
        stage('ArgoCD Manifest Update') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [[name: 'main']],
                    userRemoteConfigs: [[
                        url: 'https://github.com/Pda-Final-Project/argocd.git',
                        credentialsId: 'geonwook'
                    ]]
                ])
                dir('apps') {
                    updateArgoCDManifest('execution-service')
                    updateArgoCDManifest('filling-service')
                    updateArgoCDManifest('gateway')
                    updateArgoCDManifest('matching-service')
                    updateArgoCDManifest('notification-service')
                    updateArgoCDManifest('settlement-service')
                    updateArgoCDManifest('user-service')
                    updateArgoCDManifest('order-service')
                    
                    sshagent(credentials: ['github-credentials']) {
                        sh "git commit -m '[UPDATE] v${env.BUILD_NUMBER} image versioning'"
                        sh "git remote set-url origin git@github.com:Pda-Final-Project/argocd.git"
                        sh "git push -u origin main"
                    }
                }
            }
        }
    }
}

def buildAndPushDockerImage(serviceName) {
    dir(serviceName) {
        sh "chmod +x gradlew"
        sh "./gradlew clean bootJar"
        script {
            def image = docker.build("$DOCKER_HUB_USER/${serviceName}:${env.BUILD_NUMBER}")
            docker.withRegistry('https://registry.hub.docker.com/repository/docker/', 'docker-hub-credentials') {
                image.push("${env.BUILD_NUMBER}")
            }
        }
    }
}

def updateArgoCDManifest(serviceName) {
    sh "sed -i 's/${serviceName}:.*/${serviceName}:${env.BUILD_NUMBER}/' ${serviceName}.yaml"
    sh "git add ${serviceName}.yaml"
}
