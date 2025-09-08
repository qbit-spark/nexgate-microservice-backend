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

                    # Clean up immediately after build
                    rm -f src/main/resources/bootstrap.properties
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
                    echo "=== Container Status ==="
                    docker ps | grep nexgate-app || echo "Container not running!"

                    echo "=== Recent Logs ==="
                    docker logs nexgate-app | tail -5 || echo "No logs available"

                    echo "=== Health Check ==="
                    curl -f http://localhost:8080/actuator/health || curl -f http://localhost:8080/ || echo "App not responding"
                '''
                echo '🎉 Done! App should be running at http://localhost:8080'
            }
        }
    }

    // Remove post section entirely to avoid context issues
}