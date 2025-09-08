pipeline {
    agent any

    environment {
        VAULT_TOKEN = credentials('nexgate-vault-token')
        VAULT_URI = credentials('nexgate-vault-uri')
        APP_NAME = 'nexgate-app'
        APP_PORT = '8080'
    }

    stages {
        stage('🔨 Build') {
            steps {
                echo 'Building application...'
                sh '''
                    # Create bootstrap config
                    mkdir -p src/main/resources
                    cat > src/main/resources/bootstrap.properties << EOF
spring.cloud.vault.enabled=true
spring.cloud.vault.uri=${VAULT_URI}
spring.cloud.vault.token=${VAULT_TOKEN}
spring.cloud.vault.authentication=TOKEN
spring.cloud.vault.kv.enabled=true
spring.cloud.vault.kv.backend=secret
spring.cloud.vault.kv.application-name=nexgate
EOF

                    # Build JAR
                    chmod +x mvnw
                    ./mvnw clean package -DskipTests
                '''
                echo 'Build complete!'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('🐳 Docker') {
            steps {
                echo 'Building Docker image...'
                sh '''
                    # Remove old images to save space
                    docker rmi ${APP_NAME}:latest || true

                    # Build new image
                    docker build -t ${APP_NAME}:latest .
                '''
                echo 'Docker image ready!'
            }
        }

        stage('🚀 Deploy') {
            steps {
                echo 'Deploying application...'
                sh '''
                    # Stop and remove old container
                    docker stop ${APP_NAME} || true
                    docker rm ${APP_NAME} || true

                    # Start new container with health check
                    docker run -d \
                        --name ${APP_NAME} \
                        -p ${APP_PORT}:${APP_PORT} \
                        -e VAULT_TOKEN=${VAULT_TOKEN} \
                        -e VAULT_URI=${VAULT_URI} \
                        --restart unless-stopped \
                        ${APP_NAME}:latest
                '''
                echo 'Deployment initiated!'
            }
        }

        stage('✅ Health Check') {
            steps {
                echo 'Performing health check...'
                script {
                    def maxRetries = 12
                    def retryDelay = 10

                    for (int i = 1; i <= maxRetries; i++) {
                        echo "Health check attempt ${i}/${maxRetries}..."

                        def containerRunning = sh(
                            script: 'docker ps --filter "name=${APP_NAME}" --filter "status=running" | grep -q ${APP_NAME}',
                            returnStatus: true
                        ) == 0

                        if (containerRunning) {
                            // Check if app is responding
                            def appHealthy = sh(
                                script: 'curl -f -s http://localhost:${APP_PORT}/actuator/health || curl -f -s http://localhost:${APP_PORT}/',
                                returnStatus: true
                            ) == 0

                            if (appHealthy) {
                                echo '✅ Application is healthy!'
                                sh 'docker logs ${APP_NAME} | tail -10'
                                echo '🎉 Deployment successful! App running at http://localhost:${APP_PORT}'
                                return
                            }
                        }

                        if (i < maxRetries) {
                            echo "App not ready yet, waiting ${retryDelay} seconds..."
                            sleep(retryDelay)
                        }
                    }

                    // If we get here, health check failed
                    echo '❌ Health check failed!'
                    sh '''
                        echo "Container status:"
                        docker ps -a | grep ${APP_NAME} || echo "No container found"
                        echo "Container logs:"
                        docker logs ${APP_NAME} | tail -20 || echo "No logs available"
                    '''
                    error('Application failed to start properly')
                }
            }
        }
    }

    post {
        always {
            script {
                // Clean up bootstrap properties file
                sh 'rm -f src/main/resources/bootstrap.properties || true'

                echo 'Pipeline execution summary:'
                if (currentBuild.result == 'SUCCESS') {
                    echo '✅ Pipeline completed successfully'
                } else if (currentBuild.result == 'FAILURE') {
                    echo '❌ Pipeline failed'
                    // Show container logs for debugging
                    sh '''
                        echo "=== Container Status ==="
                        docker ps -a | grep ${APP_NAME} || echo "No container found"
                        echo "=== Recent Container Logs ==="
                        docker logs ${APP_NAME} | tail -30 || echo "No logs available"
                    '''
                } else {
                    echo "⚠️  Pipeline completed with status: ${currentBuild.result}"
                }
            }
        }
        success {
            echo '🎉 Deployment pipeline completed successfully!'
        }
        failure {
            echo '💥 Pipeline failed. Check the logs above for details.'
        }
        cleanup {
            // Optional: Clean up Docker images to save space
            sh '''
                echo "Cleaning up old Docker images..."
                docker image prune -f || true
            '''
        }
    }
}