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

                    echo "=== Build Complete ==="
                    ls -la target/*.jar
                '''
                echo 'Build complete!'
            }
        }

        stage('🐳 Docker Build') {
            steps {
                echo 'Building Docker image...'
                sh '''
                    echo "=== Building Docker Image ==="
                    docker build -t nexgate-app:latest .

                    echo "=== Image Created ==="
                    docker images | grep nexgate-app
                '''
                echo 'Docker image ready!'
            }
        }

        stage('🚀 Deploy') {
            steps {
                echo 'Deploying application...'
                sh '''
                    echo "=== Stopping Old Container ==="
                    docker stop nexgate-app || echo "No container to stop"
                    docker rm nexgate-app || echo "No container to remove"

                    echo "=== Starting New Container ==="
                    docker run -d \
                        --name nexgate-app \
                        --restart unless-stopped \
                        -p 8080:8080 \
                        -e VAULT_TOKEN=${VAULT_TOKEN} \
                        -e VAULT_URI=${VAULT_URI} \
                        nexgate-app:latest

                    echo "=== Deployment Complete ==="
                    sleep 5
                    docker ps | grep nexgate-app
                '''
                echo 'Deployment complete!'
            }
        }

        stage('✅ Health Check') {
            steps {
                echo 'Checking application health...'
                sh '''
                    echo "=== Waiting for App to Start ==="
                    sleep 30

                    echo "=== Container Status ==="
                    docker ps | grep nexgate-app || echo "Container not running!"

                    echo "=== Application Logs ==="
                    docker logs nexgate-app | tail -15

                    echo "=== Health Check ==="
                    for i in {1..5}; do
                        echo "Health check attempt $i/5..."
                        if curl -f -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
                            echo "✅ App is healthy!"
                            curl -s http://localhost:8080/actuator/health
                            break
                        elif curl -f -s http://localhost:8080/ > /dev/null 2>&1; then
                            echo "✅ App is responding!"
                            break
                        else
                            echo "⏳ App not ready yet, waiting..."
                            sleep 10
                        fi
                    done

                    echo "=== Final Status ==="
                    docker ps
                '''
                echo '🎉 Deployment Complete!'
                echo '📱 Your app: http://localhost:8080'
                echo '🔧 Jenkins: http://localhost:8081'
            }
        }
    }
}