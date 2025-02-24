pipeline {
    agent any
    environment {
        DOCKER_HUB_REPO = "kimdongjae/finpago-backend"
    }
    stages {
        stage('Checkout') {
            steps {
                git branch: 'develop', url: 'https://github.com/Pda-Final-Project/backend.git'
            }
        }
        stage('Build Docker Image') {
            steps {
                sh 'docker build -t $DOCKER_HUB_REPO:latest .'
            }
        }
        stage('Login to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                }
            }
        }
        stage('Push to Docker Hub') {
            steps {
                sh 'docker push $DOCKER_HUB_REPO:latest'
            }
        }
    }
}
