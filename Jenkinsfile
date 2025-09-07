pipeline {
    agent any

    stages {
        stage('📋 Hello') {
            steps {
                echo '🎉 Hello! Jenkins is working!'
                echo "Building project: ${env.JOB_NAME}"
                echo "Build number: ${env.BUILD_NUMBER}"
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
                sh 'docker build -t nexgate-test .'
                echo '✅ Docker image created!'
            }
        }

        stage('🚀 Deploy') {
            steps {
                echo '=== Deploying locally ==='
                script {
                    // Stop old container if it exists
                    sh 'docker stop nexgate-local || echo "No container to stop"'
                    sh 'docker rm nexgate-local || echo "No container to remove"'

                    // Start new container
                    sh '''
                        docker run -d \
                            --name nexgate-local \
                            -p 8080:8080 \
                            nexgate-test
                    '''

                    echo '✅ Container started!'
                }
            }
        }

        stage('✅ Check') {
            steps {
                echo '=== Checking if app is running ==='
                script {
                    // Wait for app to start
                    sleep(20)

                    // Check if container is running
                    sh 'docker ps | grep nexgate-local'

                    echo '🎉 SUCCESS! Your app is running!'
                    echo '🌐 Check it at: http://localhost:8080'
                }
            }
        }
    }

    post {
        success {
            echo '🎉🎉🎉 EVERYTHING WORKED! 🎉🎉🎉'
            echo '✅ Tests passed'
            echo '✅ App built'
            echo '✅ Docker image created'
            echo '✅ App deployed'
            echo '🌐 Your app is running at: http://localhost:8080'
        }
        failure {
            echo '❌ Something went wrong. Check the logs above.'
        }
    }
}