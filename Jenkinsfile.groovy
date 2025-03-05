pipeline {
    agent any
    environment {
        GIT_CREDENTIALS_ID = "github-credentials"
        GIT_SSH = "git-ssh"
    }
    stages {
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [[name: 'P3-96-Feat/Jenkins&ArgoCD']],
                    userRemoteConfigs: [[
                        url: 'https://github.com/Pda-Final-Project/backend.git',
                        credentialsId: GIT_CREDENTIALS_ID
                    ]]
                ])
            }
        }
        stage('Cleanup Docker Cache') {
            steps {
                script {
                    def diskUsage = sh(script: """df -h | grep '/$' | awk '{print $5}' | sed 's/%//'""", returnStdout: true).trim().toInteger()
                    if (diskUsage > 80) {
                        sh 'docker system prune -a -f --volumes'
                    }
                }
            }
        }
        stage('Build Common Libraries') {
            steps {
                sh 'echo "org.gradle.daemon=true" >> gradle.properties'
                sh 'echo "org.gradle.parallel=true" >> gradle.properties'
                sh 'echo "org.gradle.workers.max=2" >> gradle.properties'
                sh './gradlew build --build-cache --parallel --configure-on-demand --continue'
            }
        }
        stage('Build & Push Docker Images') {
            parallel {
                stage('Execution Service') {
                    steps {
                        buildAndPushDockerImage('execution-service')
                    }
                }
                stage('Data Service') {
                    steps {
                        buildAndPushDockerImage('data-service')
                    }
                }
                stage('Gateway Service') {  
                    steps {
                        buildAndPushDockerImage('gateway')
                    }
                }
            }
        }
    }
    post {
        always {
            sh './gradlew --stop'
        }
    }
}

def buildAndPushDockerImage(serviceName) {
    dir(serviceName) {
        sh 'if [ ! -x gradlew ]; then chmod +x gradlew; fi'
        
        withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_HUB_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
            sh """
                echo $DOCKER_PASSWORD | docker login -u $DOCKER_HUB_USER --password-stdin
                if docker pull $DOCKER_HUB_USER/${serviceName}:latest; then
                    docker build --cache-from=$DOCKER_HUB_USER/${serviceName}:latest -t $DOCKER_HUB_USER/${serviceName}:${env.BUILD_NUMBER} .
                else
                    docker build -t $DOCKER_HUB_USER/${serviceName}:${env.BUILD_NUMBER} .
                fi
                docker push $DOCKER_HUB_USER/${serviceName}:${env.BUILD_NUMBER}
            """
        }
    }
}
