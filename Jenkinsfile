pipeline {
    agent any

    environment {
        VAULT_TOKEN = credentials('nexgate-vault-token')
        VAULT_URI = credentials('nexgate-vault-uri')
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
        }

        stage('🐳 Docker') {
            steps {
                echo 'Building Docker image...'
                sh '''
                    docker build -t nexgate-app .
                '''
                echo 'Docker image ready!'
            }
        }

        stage('🚀 Deploy') {
            steps {
                echo 'Deploying application...'
                sh '''
                    # Stop old container
                    docker stop nexgate-app || true
                    docker rm nexgate-app || true

                    # Start new container
                    docker run -d \
                        --name nexgate-app \
                        -p 8080:8080 \
                        -e VAULT_TOKEN=${VAULT_TOKEN} \
                        -e VAULT_URI=${VAULT_URI} \
                        nexgate-app
                '''
                echo 'Deployment complete!'
            }
        }

        stage('✅ Check') {
            steps {
                echo 'Checking application...'
                sleep(20)
                sh '''
                    docker ps | grep nexgate-app
                    docker logs nexgate-app | tail -5
                '''
                echo '🎉 Done! App running at http://localhost:8080'
            }
        }
    }

    post {
        always {
            sh 'rm -f src/main/resources/bootstrap.properties || true'
        }
    }
}