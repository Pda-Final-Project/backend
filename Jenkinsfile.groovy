pipeline {
    agent any
    environment {
        GIT_CREDENTIALS_ID = "github-credentials"
        GIT_SSH = "git-ssh"
        PYTHON_CRAWLER_IMAGE = "python-crawler"
    }
    stages {
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM',
                          branches: [[name: 'P3-121-Feat/파이썬-크롤러-개발']],
                          userRemoteConfigs: [[
                                                      url: 'https://github.com/Pda-Final-Project/backend.git',
                                                      credentialsId: GIT_CREDENTIALS_ID
                                              ]]
                ])
            }
        }
        stage('Detect Changes') {
            steps {
                script {
                    def changedFiles = sh(script: "git diff --name-only HEAD~1", returnStdout: true).trim().split("\n")
                    def changedModules = changedFiles.collect { file -> file.split("/")[0] }.unique()

                    // Jenkinsfile.groovy 파일은 변경된 모듈 목록에서 제외
                    changedModules = changedModules.findAll { it != "Jenkinsfile.groovy" }

                    echo "Changed Modules: ${changedModules}"

                    if (changedModules.contains("common")) {
                        echo "Common module changed. Building all services."
                        env.CHANGED_MODULES = "all"
                    } else {
                        env.CHANGED_MODULES = changedModules.join(",")
                    }
                }
            }
        }

        stage('Build Common Module') {
            when {
                expression { env.CHANGED_MODULES.contains("common") || env.CHANGED_MODULES == "all" }
            }
            steps {
                dir('common') {
                    sh 'if [ ! -x gradlew ]; then chmod +x gradlew; fi'
                    sh './gradlew clean build -x test -Pprod --no-daemon -Dorg.gradle.jvmargs="-Xmx1024m"'
                }
            }
        }
        stage('Build & Push Docker Images') {
            steps {
                script {
                    def modulesToBuild = env.CHANGED_MODULES == "all" ?
                            ['execution-service', 'data-service', 'filling-service', 'gateway',
                             'matching-service', 'notification-service', 'settlement-service',
                             'user-service', 'order-service', 'python-crawler']
                            : env.CHANGED_MODULES.tokenize(",")

                    def parallelStages = [:]
                    modulesToBuild.each { module ->
                        parallelStages[module] = {
                            if (module == "python-crawler") {
                                buildAndPushPythonCrawler()
                            } else {
                                buildAndPushDockerImage(module)
                            }
                        }
                    }

                    if (parallelStages.size() > 0) {
                        parallel parallelStages
                    } else {
                        echo "✅ No services need to be built."
                    }
                }
            }
        }
        stage('Cleanup Gradle Daemon') {
            steps {
                sh './gradlew --stop'
            }
        }
        stage('ArgoCD Manifest Update') {
            when {
                expression { env.CHANGED_MODULES != "" }
            }
            steps {
                checkout([$class: 'GitSCM',
                          branches: [[name: 'main']],
                          userRemoteConfigs: [[
                                                      url: 'https://github.com/Pda-Final-Project/argocd.git',
                                                      credentialsId: GIT_CREDENTIALS_ID
                                              ]]
                ])

                withCredentials([usernamePassword(credentialsId: GIT_CREDENTIALS_ID, usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                    sh """
                        git config --global user.email "tomy8964@naver.com"
                        git config --global user.name "tomy8964"
                        
                        git remote set-url origin https://$GIT_USER:$GIT_PASS@github.com/Pda-Final-Project/argocd.git
                        
                        git checkout main
                        git fetch origin main
                        git reset --hard origin/main
                        git pull --rebase origin main
                    """
                }

                dir('apps') {
                    sh 'ls -l'
                    script {
                        def changedModulesList = env.CHANGED_MODULES.tokenize(",")
                        changedModulesList.each { module ->
                            updateArgoCDManifest(module)
                        }
                    }
                }
            }
        }
        stage('Commit & Push Updates') {
            when {
                expression { env.CHANGED_MODULES != "" }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: GIT_CREDENTIALS_ID, usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                    sh """
                        git config --global user.email "tomy8964@naver.com"
                        git config --global user.name "tomy8964"
        
                        git remote set-url origin https://$GIT_USER:$GIT_PASS@github.com/Pda-Final-Project/argocd.git
                        
                        git add apps/*.yaml
                        
                        if ! git diff --cached --quiet; then
                            git commit -m '[UPDATE] v${env.BUILD_NUMBER} image versioning'
                            git push origin main
                        else
                            echo "✅ No changes to commit and push"
                        fi
                    """
                }
            }
        }
    }
}

// Docker 빌드 및 푸시 함수
def buildAndPushDockerImage(serviceName) {
    dir(serviceName) {
        sh 'if [ ! -x gradlew ]; then chmod +x gradlew; fi'

        sh 'echo "org.gradle.jvmargs=-Xms512m -Xmx2048m -Dfile.encoding=UTF-8 -XX:+HeapDumpOnOutOfMemoryError" > gradle.properties'
        sh 'echo "org.gradle.daemon.idleTimeout=60000" >> gradle.properties'

        sh './gradlew clean build -x test --no-daemon -Pprod --parallel -Dspring.profiles.active=prod'

        withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_HUB_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
            sh """
                echo $DOCKER_PASSWORD | docker login -u $DOCKER_HUB_USER --password-stdin
                docker build -t $DOCKER_HUB_USER/${serviceName}:${env.BUILD_NUMBER} .
                docker push $DOCKER_HUB_USER/${serviceName}:${env.BUILD_NUMBER}
            """
        }
    }
}

// Python Crawler 빌드 및 푸시 함수
def buildAndPushPythonCrawler() {
    dir('python-crawler') {
        sh 'echo "Building Python Crawler Docker Image..."'

        withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_HUB_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
            sh """
                echo $DOCKER_PASSWORD | docker login -u $DOCKER_HUB_USER --password-stdin
                docker build -t $DOCKER_HUB_USER/${PYTHON_CRAWLER_IMAGE}:${env.BUILD_NUMBER} .
                docker push $DOCKER_HUB_USER/${PYTHON_CRAWLER_IMAGE}:${env.BUILD_NUMBER}
            """
        }
    }
}

// ArgoCD 매니페스트 업데이트 함수
def updateArgoCDManifest(serviceName) {
    if (serviceName == "python-crawler") {
        def pythonCrawlerManifests = [
                "update-chart",
                "update-fillings",
                "update-news",
                "init-chart",
                "init-fillings",
                "init-stock",
                "stock-price-listener"
        ]
        pythonCrawlerManifests.each { manifest ->
            sh """
                sed -i 's|\\(image: .*/${PYTHON_CRAWLER_IMAGE}:\\)[^ ]*|\\1${env.BUILD_NUMBER}|' ${manifest}.yaml
                git add apps/${manifest}.yaml
            """
        }
    } else {
        sh """
            sed -i 's|\\(image: .*/${serviceName}:\\)[^ ]*|\\1${env.BUILD_NUMBER}|' apps/${serviceName}.yaml
            git add apps/${serviceName}.yaml
        """
    }
}
