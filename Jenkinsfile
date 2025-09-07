pipeline {
    agent {
        docker {
            image 'maven:3.9.9-openjdk-24-slim'
            args '-v $HOME/.m2:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    stages {
        stage('📋 Hello') {
            steps {
                echo '🎉 Hello! Jenkins with Java 24 is working!'
                echo "Building project: ${env.JOB_NAME}"
                echo "Build number: ${env.BUILD_NUMBER}"
                sh 'java -version'
                sh 'mvn -version'
            }
        }

        stage('🧪 Test') {
            steps {
                echo '=== Running tests with Java 24 ==='
                sh 'chmod +x mvnw'
                sh './mvnw test'
                echo '✅ Tests completed!'
            }
        }

        stage('🔨 Build') {
            steps {
                echo '=== Building the app with Java 24 ==='
                sh './mvnw clean package -DskipTests'
                echo '✅ Build completed!'
            }
        }

        stage('🐳 Docker') {
            steps {
                echo '=== Creating Docker image ==='
                sh 'docker build -t nexgate-backend:latest .'
                echo '✅ Docker image created!'
            }
        }

        stage('🚀 Deploy') {
            steps {
                echo '=== Deploying application ==='
                script {
                    try {
                        // Try docker-compose first
                        sh '''
                            if [ -d "/opt/nexgate" ]; then
                                echo "Using docker-compose deployment..."
                                cd /opt/nexgate
                                docker-compose stop nexgate_backend || true
                                docker-compose rm -f nexgate_backend || true
                                docker-compose up -d nexgate_backend
                            else
                                echo "Using local deployment..."
                                docker stop nexgate-local || true
                                docker rm nexgate-local || true
                                docker run -d \
                                    --name nexgate-local \
                                    -p 8080:8080 \
                                    -e SPRING_PROFILES_ACTIVE=dev \
                                    nexgate-backend:latest
                            fi
                        '''
                        echo '✅ Container deployed!'
                    } catch (Exception e) {
                        echo "❌ Deployment failed: ${e.getMessage()}"
                        currentBuild.result = 'FAILURE'
                        throw e
                    }
                }
            }
        }

        stage('✅ Check') {
            steps {
                echo '=== Checking if app is running ==='
                script {
                    try {
                        // Wait for app to start
                        sleep(30)

                        // Check if container is running
                        sh '''
                            if docker ps | grep -q "nexgate"; then
                                echo "🎉 SUCCESS! NexGate app is running!"
                                echo "🌐 Check it at: http://localhost:8080"

                                # Test health endpoint (optional)
                                echo "🏥 Testing health endpoint..."
                                for i in {1..3}; do
                                    if curl -f http://localhost:8080/actuator/health 2>/dev/null; then
                                        echo "✅ Health check passed!"
                                        break
                                    else
                                        echo "Attempt $i: Waiting for app..."
                                        sleep 10
                                    fi
                                done
                            else
                                echo "❌ No NexGate containers found running"
                                docker ps
                                exit 1
                            fi
                        '''
                    } catch (Exception e) {
                        echo "❌ Health check failed: ${e.getMessage()}"
                        // Show container logs for debugging
                        sh '''
                            echo "=== Container logs for debugging ==="
                            docker logs nexgate-local 2>/dev/null || echo "No nexgate-local logs"
                            docker logs nexgate_backend_app 2>/dev/null || echo "No nexgate_backend_app logs"
                            echo "=== All containers ==="
                            docker ps -a
                        '''
                        currentBuild.result = 'FAILURE'
                        throw e
                    }
                }
            }
        }
    }

    post {
        success {
            echo '🎉🎉🎉 EVERYTHING WORKED WITH JAVA 24! 🎉🎉🎉'
            echo '✅ Java 24 compilation successful'
            echo '✅ Tests passed'
            echo '✅ App built with Spring Boot 3.5.5'
            echo '✅ Docker image created'
            echo '✅ App deployed successfully'
            echo '🌐 Your NexGate app is running at: http://localhost:8080'
        }
        failure {
            echo '❌ Build failed with Java 24'
            // Note: Can't run 'sh' commands in post section with docker agent
            // Debugging was moved to the stages above
        }
        cleanup {
            echo '🧹 Pipeline cleanup completed'
        }
    }
}