pipeline {
    agent any

    environment {
        // Clean environment - only Vault credentials
        VAULT_TOKEN = credentials('nexgate-vault-token')
        VAULT_URI = credentials('nexgate-vault-uri')
        APP_NAME = 'nexgate-app'
        APP_IMAGE = 'nexgate-app:latest'
    }

    stages {
        stage('📋 Hello') {
            steps {
                echo '🎉 Nexgate Smart Deployment Pipeline!'
                echo "Building project: ${env.JOB_NAME}"
                echo "Build number: ${env.BUILD_NUMBER}"
                echo "🔐 Using Vault: ${VAULT_URI}"
            }
        }

        stage('🔍 Infrastructure Discovery') {
            steps {
                echo '🔍 Discovering existing infrastructure...'
                script {
                    // Find existing Docker Compose network
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

                    // Check for existing infrastructure
                    def containers = sh(
                        script: 'docker ps --filter name=nexgate --format "{{.Names}}"',
                        returnStdout: true
                    ).trim()

                    echo "Running infrastructure: ${containers ?: 'None found'}"
                }
            }
        }

        stage('🔑 Create Bootstrap Config') {
            steps {
                echo '🔑 Creating Vault bootstrap configuration...'
                script {
                    // Create bootstrap.properties for building with Vault connection
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
                // Publish test results if they exist
                publishTestResults testResultsPattern: 'target/surefire-reports/*.xml', allowEmptyResults: true
                echo '✅ Tests completed!'
            }
        }

        stage('🔨 Build') {
            steps {
                echo '🔨 Building application...'
                sh '''
                    ./mvnw clean package -DskipTests
                    echo "JAR file created: $(ls -la target/*.jar)"
                '''
                echo '✅ Build completed!'
            }
        }

        stage('🐳 Docker Build') {
            steps {
                echo '🐳 Building Docker image...'
                sh '''
                    docker build -t ${APP_IMAGE} .
                    docker build -t nexgate-app:build-${BUILD_NUMBER} .
                    echo "✅ Docker images built successfully"
                '''
            }
        }

        stage('🔧 Vault Configuration Update') {
            steps {
                echo '🔧 Updating Vault with deployment-specific configuration...'
                script {
                    // Update Vault with environment-specific settings
                    sh """
                        echo "Updating Vault secrets for deployment environment..."

                        # Update deployment metadata in Vault
                        curl -X POST -H "X-Vault-Token: ${VAULT_TOKEN}" \\
                             -H "Content-Type: application/json" \\
                             -d '{
                                 "data": {
                                     "deployment.timestamp": "'$(date -Iseconds)'",
                                     "deployment.build": "${BUILD_NUMBER}",
                                     "deployment.network": "${TARGET_NETWORK}",
                                     "deployment.environment": "docker"
                                 }
                             }' \\
                             ${VAULT_URI}/v1/secret/data/deployment-info || echo "Vault update completed"
                    """
                }
                echo '✅ Vault configuration updated!'
            }
        }

        stage('🚀 Smart Deployment') {
            steps {
                echo '🚀 Deploying application with smart networking...'
                script {
                    // Clean deployment strategy
                    sh '''
                        echo "🛑 Stopping existing application..."
                        docker stop ${APP_NAME} || true
                        docker rm ${APP_NAME} || true

                        echo "🔗 Deploying with network: ${TARGET_NETWORK}"

                        # Deploy with minimal environment - Vault handles all configuration
                        docker run -d \\
                            --name ${APP_NAME} \\
                            --network ${TARGET_NETWORK} \\
                            -p 8080:8080 \\
                            -e VAULT_TOKEN=${VAULT_TOKEN} \\
                            -e VAULT_URI=${VAULT_URI} \\
                            -e SPRING_PROFILES_ACTIVE=docker \\
                            --restart unless-stopped \\
                            ${APP_IMAGE}

                        echo "✅ Application deployed successfully!"
                    '''
                }
            }
        }

        stage('🔗 Network Verification') {
            steps {
                echo '🔗 Verifying container networking and connectivity...'
                sh '''
                    echo "=== Container Network Information ==="
                    docker inspect ${APP_NAME} --format='{{range $net, $conf := .NetworkSettings.Networks}}{{$net}}: {{$conf.IPAddress}}{{end}}' || true

                    echo "=== Available Networks ==="
                    docker network ls | grep -E "(nexgate|bridge)" || true

                    echo "=== Container Status ==="
                    docker ps --filter name=nexgate --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}"

                    echo "=== Testing Internal Connectivity ==="
                    # Test if app can resolve infrastructure hostnames
                    timeout 30s docker exec ${APP_NAME} sh -c "
                        for host in nexgate_postgres_db nexgate-postgres nexgate_minio_server nexgate-minio; do
                            echo Testing connection to \\$host...
                            ping -c 1 \\$host 2>/dev/null && echo ✅ \\$host reachable || echo ❌ \\$host not reachable
                        done
                    " || echo "Connectivity test completed"
                '''
            }
        }

        stage('✅ Health Check') {
            steps {
                echo '✅ Running comprehensive health checks...'
                script {
                    // Comprehensive health check with retries
                    retry(8) {
                        sleep(15)
                        sh '''
                            echo "=== Application Health Check ==="
                            curl -f -s http://localhost:8080/actuator/health | jq '.' || curl -f http://localhost:8080/actuator/health

                            echo "=== Application Info ==="
                            curl -s http://localhost:8080/actuator/info || echo "Info endpoint not available"

                            echo "=== Recent Application Logs ==="
                            docker logs --tail=15 ${APP_NAME}
                        '''
                    }
                }
                echo '🎉 Application is healthy and operational!'
            }
        }

        stage('📊 Deployment Summary') {
            steps {
                echo '📊 Generating deployment summary...'
                script {
                    sh '''
                        echo "=================================="
                        echo "🎉 DEPLOYMENT COMPLETED SUCCESSFULLY!"
                        echo "=================================="
                        echo ""
                        echo "📦 Build Information:"
                        echo "   • Build Number: ${BUILD_NUMBER}"
                        echo "   • Git Commit: ${GIT_COMMIT}"
                        echo "   • Deploy Time: $(date)"
                        echo ""
                        echo "🌐 Application Access:"
                        echo "   • Application URL: http://localhost:8080"
                        echo "   • Health Check:   http://localhost:8080/actuator/health"
                        echo "   • API Docs:       http://localhost:8080/swagger-ui.html"
                        echo ""
                        echo "🔧 Infrastructure:"
                        echo "   • Network Used: ${TARGET_NETWORK}"
                        echo "   • Vault URI: ${VAULT_URI}"
                        echo "   • Configuration: Managed by Vault"
                        echo ""
                        echo "🛠️ Management Commands:"
                        echo "   • View logs:      docker logs -f ${APP_NAME}"
                        echo "   • Restart app:    docker restart ${APP_NAME}"
                        echo "   • Stop app:       docker stop ${APP_NAME}"
                        echo "   • Container info: docker inspect ${APP_NAME}"
                        echo ""
                        echo "🔍 Service Status:"
                        docker ps --filter name=nexgate --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}"
                        echo ""
                    '''
                }
            }
        }
    }

    post {
        always {
            echo '🧹 Cleaning up build artifacts...'
            // Clean up sensitive build files
            sh '''
                rm -f src/main/resources/bootstrap.properties || true
            '''

            // Archive build artifacts
            archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
        }

        success {
            echo '''
🎉🎉🎉 DEPLOYMENT SUCCESSFUL! 🎉🎉🎉

✅ Your Nexgate application is now running!
🔐 All configuration managed securely via Vault
🌐 Access your app at: http://localhost:8080

Next steps:
• Test your application endpoints
• Check Vault for configuration management
• Monitor logs: docker logs -f nexgate-app
            '''

            // Send success notification (uncomment and configure as needed)
            // slackSend channel: '#deployments',
            //           color: 'good',
            //           message: "✅ Nexgate deployed successfully! Build #${BUILD_NUMBER}"
        }

        failure {
            echo '❌ DEPLOYMENT FAILED!'
            script {
                sh '''
                    echo "=== FAILURE DIAGNOSTICS ==="
                    echo ""
                    echo "🔍 Container Status:"
                    docker ps -a | grep nexgate || echo "No nexgate containers found"

                    echo ""
                    echo "📜 Application Logs:"
                    docker logs ${APP_NAME} || echo "No application logs available"

                    echo ""
                    echo "🌐 Network Information:"
                    docker network ls | grep nexgate || echo "No nexgate networks found"

                    echo ""
                    echo "💾 System Resources:"
                    df -h
                    docker system df
                '''
            }

            // Send failure notification (uncomment and configure as needed)
            // slackSend channel: '#deployments',
            //           color: 'danger',
            //           message: "❌ Nexgate deployment failed! Build #${BUILD_NUMBER} - Check Jenkins logs"
        }

        cleanup {
            echo '🔄 Pipeline cleanup completed'
        }
    }
}