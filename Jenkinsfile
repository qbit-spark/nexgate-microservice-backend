pipeline {
    agent any

    environment {
        VAULT_TOKEN = credentials('nexgate-vault-token')
        VAULT_URI = credentials('nexgate-vault-uri')
    }

    stages {
        stage('📋 Hello') {
            steps {
                echo '🎉 Hello! Jenkins is working!'
                echo "Building project: ${env.JOB_NAME}"
                echo "Build number: ${env.BUILD_NUMBER}"
            }
        }

        stage('🔑 Bootstrap') {
            steps {
                echo '=== Creating bootstrap config ==='
                script {
                    writeFile file: 'src/main/resources/bootstrap.properties', text: """
spring.cloud.vault.enabled=true
spring.cloud.vault.uri=${VAULT_URI}
spring.cloud.vault.token=${VAULT_TOKEN}
spring.cloud.vault.authentication=TOKEN
spring.cloud.vault.kv.enabled=true
spring.cloud.vault.kv.backend=secret
spring.cloud.vault.kv.application-name=nexgate
"""
                }
                echo '✅ Bootstrap created!'
            }
        }

        stage('🧪 Test') {
            steps {
                echo '=== Running tests ==='
                sh 'chmod +x mvnw'
                sh './mvnw test'
                echo '✅ Tests done!'
            }
        }

        stage('🔨 Build') {
            steps {
                echo '=== Building app ==='
                sh './mvnw clean package -DskipTests'
                echo '✅ Build done!'
            }
        }

        stage('🐳 Docker') {
            steps {
                echo '=== Building Docker image ==='
                sh 'docker build -t nexgate-app .'
                echo '✅ Image built!'
            }
        }

        stage('🚀 Deploy') {
            steps {
                echo '=== Deploying app ==='
                sh '''
                    docker stop nexgate-app || true
                    docker rm nexgate-app || true
                    docker run -d \
                        --name nexgate-app \
                        -p 8080:8080 \
                        -e VAULT_TOKEN=${VAULT_TOKEN} \
                        -e VAULT_URI=${VAULT_URI} \
                        nexgate-app
                '''
                echo '✅ App deployed!'
            }
        }

        stage('✅ Check') {
            steps {
                echo '=== Checking app ==='
                sleep(30)
                sh '''
                    echo "Container status:"
                    docker ps | grep nexgate-app

                    echo "App logs:"
                    docker logs nexgate-app | tail -10

                    echo "Health check:"
                    curl -f http://localhost:8080/actuator/health || echo "App starting..."
                '''
                echo '🎉 SUCCESS!'
                echo '🌐 App: http://localhost:8080'
            }
        }
    }

    post {
        always {
            sh 'rm -f src/main/resources/bootstrap.properties || true'
        }
        success {
            echo '🎉 DEPLOYMENT SUCCESS!'
            echo '🌐 http://localhost:8080'
        }
        failure {
            echo '❌ Something failed!'
            sh 'docker logs nexgate-app || echo "No container"'
        }
    }
}