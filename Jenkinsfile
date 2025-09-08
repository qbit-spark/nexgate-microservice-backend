pipeline {
    agent any

    environment {
        VAULT_TOKEN = credentials('nexgate-vault-token')
        VAULT_URI = credentials('nexgate-vault-uri')
        APP_NAME = 'nexgate-app'
        APP_IMAGE = 'nexgate-app:latest'
    }

    stages {
        stage('📋 Hello') {
            steps {
                echo '🎉 Nexgate Simple Deployment Pipeline!'
                echo "Building project: ${env.JOB_NAME}"
                echo "Build number: ${env.BUILD_NUMBER}"
            }
        }

        stage('🔍 Infrastructure Discovery') {
            steps {
                echo '🔍 Discovering existing infrastructure...'
                script {
                    def networks = sh(
                        script: 'docker network ls --filter name=nexgate --format "{{.Name}}"',
                        returnStdout: true
                    ).trim().split('\n')

                    if (networks && networks[0]) {
                        env.TARGET_NETWORK = networks[0]
                        echo "✅ Found network: ${env.TARGET_NETWORK}"
                    } else {
                        env.TARGET_NETWORK = 'bridge'
                        echo "⚠️ No nexgate network found, using default bridge"
                    }
                }
            }
        }

        stage('🔑 Create Bootstrap Config') {
            steps {
                echo '🔑 Creating Vault bootstrap configuration...'
                script {
                    writeFile file: 'src/main/resources/bootstrap.properties', text: """
spring.cloud.vault.enabled=true
spring.cloud.vault.uri=${VAULT_URI}
spring.cloud.vault.token=${VAULT_TOKEN}
spring.cloud.vault.authentication=TOKEN
spring.cloud.vault.kv.enabled=true
spring.cloud.vault.kv.backend=secret
spring.cloud.vault.kv.application-name=nexgate
spring.cloud.vault.kv.default-context=application
spring.cloud.vault.fail-fast=false
spring.cloud.vault.connection-timeout=5000
spring.cloud.vault.read-timeout=15000
"""
                }
                echo '✅ Bootstrap configuration created!'
            }
        }

        stage('🧪 Test') {
            steps {
                echo '🧪 Running tests...'
                sh '''
                    chmod +x mvnw
                    ./mvnw test -Dspring.profiles.active=test
                '''
                echo '✅ Tests completed!'
            }
        }

        stage('🔨 Build') {
            steps {
                echo '🔨 Building application...'
                sh '''
                    ./mvnw clean package -DskipTests
                '''
                echo '✅ Build completed!'
            }
        }

        stage('🐳 Docker Build') {
            steps {
                echo '🐳 Building Docker image...'
                sh '''
                    docker build -t ${APP_IMAGE} .
                '''
                echo '✅ Docker image built!'
            }
        }

        stage('🚀 Deploy') {
            steps {
                echo '🚀 Deploying application...'
                sh '''
                    echo "🛑 Stopping existing application..."
                    docker stop ${APP_NAME} || true
                    docker rm ${APP_NAME} || true

                    echo "🚀 Starting new application..."
                    docker run -d \
                        --name ${APP_NAME} \
                        --network ${TARGET_NETWORK} \
                        -p 8080:8080 \
                        -e VAULT_TOKEN=${VAULT_TOKEN} \
                        -e VAULT_URI=${VAULT_URI} \
                        -e SPRING_PROFILES_ACTIVE=docker \
                        --restart unless-stopped \
                        ${APP_IMAGE}

                    echo "✅ Application deployed!"
                '''
            }
        }

        stage('✅ Health Check') {
            steps {
                echo '✅ Running health checks...'
                script {
                    retry(6) {
                        sleep(15)
                        sh '''
                            echo "=== Checking application health ==="
                            curl -f http://localhost:8080/actuator/health

                            echo "=== Recent logs ==="
                            docker logs --tail=10 ${APP_NAME}
                        '''
                    }
                }
                echo '🎉 Application is healthy!'
            }
        }

        stage('📊 Summary') {
            steps {
                echo '📊 Deployment summary...'
                sh '''
                    echo "🎉 DEPLOYMENT SUCCESSFUL!"
                    echo ""
                    echo "🌐 Application: http://localhost:8080"
                    echo "🔐 Vault: ${VAULT_URI}"
                    echo "🔧 Network: ${TARGET_NETWORK}"
                    echo ""
                    echo "🛠️ Management:"
                    echo "  • Logs: docker logs -f ${APP_NAME}"
                    echo "  • Restart: docker restart ${APP_NAME}"
                    echo ""
                '''
            }
        }
    }

    post {
        always {
            sh 'rm -f src/main/resources/bootstrap.properties || true'
        }
        success {
            echo '🎉 Deployment completed successfully!'
        }
        failure {
            echo '❌ Deployment failed!'
            sh '''
                echo "=== Failure logs ==="
                docker logs ${APP_NAME} || echo "No app logs available"
            '''
        }
    }
}