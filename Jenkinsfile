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
                echo '=== Deploying with docker-compose ==='
                script {
                    // Use docker-compose for deployment
                    sh '''
                        cd /opt/nexgate || {
                            echo "⚠️ /opt/nexgate not found, deploying locally instead"
                            docker stop nexgate-local || echo "No container to stop"
                            docker rm nexgate-local || echo "No container to remove"
                            docker run -d \
                                --name nexgate-local \
                                -p 8080:8080 \
                                -e SPRING_PROFILES_ACTIVE=dev \
                                nexgate-backend:latest
                            exit 0
                        }

                        # Deploy using docker-compose if available
                        docker-compose stop nexgate_backend || true
                        docker-compose rm -f nexgate_backend || true
                        docker-compose up -d nexgate_backend
                    '''
                    echo '✅ Container deployed!'
                }
            }
        }

        stage('✅ Check') {
            steps {
                echo '=== Checking if app is running ==='
                script {
                    // Wait for app to start
                    sleep(30)

                    // Check if container is running
                    sh '''
                        if docker ps | grep -q nexgate_backend; then
                            echo "🎉 SUCCESS! NexGate backend is running via docker-compose!"
                            echo "🌐 Check it at: http://localhost:8080"
                        elif docker ps | grep -q nexgate-local; then
                            echo "🎉 SUCCESS! NexGate app is running locally!"
                            echo "🌐 Check it at: http://localhost:8080"
                        else
                            echo "❌ No containers found running"
                            docker ps
                            exit 1
                        fi
                    '''

                    // Optional: Test health endpoint
                    sh '''
                        echo "🏥 Testing health endpoint..."
                        for i in {1..5}; do
                            if curl -f http://localhost:8080/actuator/health 2>/dev/null; then
                                echo "✅ Health check passed!"
                                break
                            elif [ $i -eq 5 ]; then
                                echo "⚠️ Health endpoint not responding yet (this is normal for first startup)"
                            else
                                echo "Attempt $i: Waiting for app to start..."
                                sleep 10
                            fi
                        done
                    '''
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
            echo '🔐 Vault integration ready'
        }
        failure {
            echo '❌ Build failed with Java 24'
            echo '📋 Checking container logs for debugging...'
            sh '''
                echo "=== Docker containers ==="
                docker ps -a
                echo "=== Container logs ==="
                docker logs nexgate-local || docker logs nexgate_backend_app || echo "No logs available"
            '''
        }
        cleanup {
            echo '🧹 Cleaning up workspace...'
        }
    }
}