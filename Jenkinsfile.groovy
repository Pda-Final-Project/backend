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
                    def diskUsage = sh(script: "df -h | awk '/ \\/\$/ {print \$5}' | sed 's/%//'", returnStdout: true).trim().toInteger()
                    if (diskUsage > 80) {
                        sh 'docker system prune -a -f --volumes'
                    }
                }
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
                        credentialsId: GIT_CREDENTIALS_ID
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

                    withCredentials([usernamePassword(credentialsId: GIT_CREDENTIALS_ID, usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                        sh """
                            git config --global user.email "tomy8964@naver.com"
                            git config --global user.name "tomy8964"
                            git commit -m '[UPDATE] v${env.BUILD_NUMBER} image versioning'
                            git remote set-url origin https://$GIT_USER:$GIT_PASS@github.com/Pda-Final-Project/argocd.git
                            git push origin main
                        """
                    }
                }
            }
        }
    }
    post {  // ✅ post 블록을 stages 블록 밖으로 이동
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
                docker build -t $DOCKER_HUB_USER/${serviceName}:${env.BUILD_NUMBER} .
                docker push $DOCKER_HUB_USER/${serviceName}:${env.BUILD_NUMBER}
            """
        }
    }
}

def updateArgoCDManifest(serviceName) {
    sh """
        sed -i 's|\\(image: .*/${serviceName}:\\)[^ ]*|\\1${env.BUILD_NUMBER}|' apps/${serviceName}.yaml
        git add apps/${serviceName}.yaml
    """
}
