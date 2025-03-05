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
                sh '''
                    echo "Cleaning up old Docker images..."
                    docker system prune -a -f --volumes
                '''
            }
        }
        stage('Build Common Libraries') {
            steps {
                sh './gradlew --stop'
                sh './gradlew --no-daemon clean'
                sh './gradlew :common:build --parallel --build-cache'
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
}

def buildAndPushDockerImage(serviceName) {
    dir(serviceName) {
        sh 'if [ ! -x gradlew ]; then chmod +x gradlew; fi'
        sh 'echo "org.gradle.jvmargs=-Xms512m -Xmx2048m -Dfile.encoding=UTF-8" > gradle.properties'
        
        sh './gradlew --stop'
        sh './gradlew --no-daemon clean'
        sh './gradlew bootJar --build-cache --parallel --configure-on-demand --continue'

        withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_HUB_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
            def latestTag = getLatestDockerTag(serviceName)

            sh """
                echo $DOCKER_PASSWORD | docker login -u $DOCKER_HUB_USER --password-stdin
                if docker pull $DOCKER_HUB_USER/${serviceName}:${latestTag}; then
                    docker build --cache-from=$DOCKER_HUB_USER/${serviceName}:${latestTag} -t $DOCKER_HUB_USER/${serviceName}:${env.BUILD_NUMBER} .
                else
                    docker build -t $DOCKER_HUB_USER/${serviceName}:${env.BUILD_NUMBER} .
                fi
                docker push $DOCKER_HUB_USER/${serviceName}:${env.BUILD_NUMBER}
            """
        }

        sh './gradlew --stop'  // 빌드 종료 후 Gradle 데몬 정리
    }
}


def updateArgoCDManifest(serviceName) {
    sh """
        sed -i 's|\\(image: .*/${serviceName}:\\)[^ ]*|\\1${env.BUILD_NUMBER}|' apps/${serviceName}.yaml
        git add apps/${serviceName}.yaml
    """
}

def getLatestDockerTag(serviceName) {
    def latestTag = sh(
        script: "curl -s https://hub.docker.com/v2/repositories/$DOCKER_HUB_USER/${serviceName}/tags/?page_size=10 | jq -r '.results | sort_by(.tag_last_pushed) | last(.[]).name'",
        returnStdout: true
    ).trim()
    return latestTag ?: "0" // 최신 태그가 없으면 기본값 0 사용
}
