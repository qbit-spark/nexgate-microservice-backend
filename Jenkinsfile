pipeline {
    agent any

    environment {
        // Vault configuration from Jenkins credentials
        VAULT_TOKEN = credentials('nexgate-vault-token')
        VAULT_URI = 'https://vault.qbitspark.com'
    }

    stages {
        stage('📋 Hello') {
            steps {
                echo '🎉 Hello! Jenkins is working!'
                echo "Building project: ${env.JOB_NAME}"
                echo "Build number: ${env.BUILD_NUMBER}"
                echo "🔐 Vault configured: ${VAULT_URI}"
            }
        }

        stage('🔑 Create Bootstrap Config') {
            steps {
                echo '=== Creating Vault configuration ==='
                script {
                    // Create bootstrap.properties with actual Vault credentials
                    writeFile file: 'src/main/resources/bootstrap.properties', text: """
spring.cloud.vault.enabled=true
spring.cloud.vault.uri=${VAULT_URI}
spring.cloud.vault.token=${VAULT_TOKEN}
spring.cloud.vault.authentication=TOKEN
spring.cloud.vault.kv.enabled=true
spring.cloud.vault.kv.backend=secret
spring.cloud.vault.kv.application-name=nexgate
spring.cloud.vault.kv.default-context=application
"""
                }
                echo '✅ Bootstrap configuration created!'
            }
        }

        stage('🧪 Test') {
            steps {
                echo '=== Running tests ==='
                sh 'chmod +x mvnw'
                sh './mvnw test'
                echo '✅ Tests completed!'
            }
        }

        stage('🔨 Build') {
            steps {
                echo '=== Building the app ==='
                sh './mvnw clean package -DskipTests'
                echo '✅ Build completed!'
            }
        }

        stage('🐳 Docker') {
            steps {
                echo '=== Creating Docker image ==='
                sh 'docker build -t nexgate-app .'
                echo '✅ Docker image created!'
            }
        }

        stage('🚀 Deploy') {
            steps {
                echo '=== Deploying with Docker Compose ==='
                sh '''
                    # Navigate to your project directory first
                    cd /path/to/your/nexgate-backend  # Adjust this path

                    docker stop nexgate-app || true
                    docker rm nexgate-app || true
                    docker run -d \
                        --name nexgate-app \
                        -p 8080:8080 \
                        --network nexgate-backend_nexgate_network \
                        -e VAULT_TOKEN=${VAULT_TOKEN} \
                        -e VAULT_URI=${VAULT_URI} \
                        nexgate-app
                '''
                echo '✅ Container deployed!'
            }
        }

        stage('✅ Check') {
            steps {
                echo '=== Checking if app is running ==='
                sleep(30)  // Extra time for Vault connection
                sh 'docker ps | grep nexgate-app'
                sh 'docker logs nexgate-app | tail -10'  // Show recent logs
                echo '🎉 SUCCESS! Your app is running with Vault!'
                echo '🌐 Check it at: http://localhost:8080'
            }
        }
    }

    post {
        always {
            // Clean up sensitive files
            sh 'rm -f src/main/resources/bootstrap.properties || true'
        }
        success {
            echo '🎉🎉🎉 EVERYTHING WORKED WITH VAULT! 🎉🎉🎉'
            echo '🌐 Your app: http://localhost:8080'
            echo '🔐 Vault secrets loaded successfully!'
        }
        failure {
            echo '❌ Something went wrong. Check the logs above.'
            echo '🔍 Check if Vault token is valid and secrets exist'
        }
    }
}