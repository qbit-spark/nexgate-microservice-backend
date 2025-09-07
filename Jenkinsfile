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
                sh 'docker build -t nexgate-app .'
                echo '✅ Docker image created!'
            }
        }

        stage('🚀 Deploy') {
            steps {
                echo '=== Deploying locally ==='
                sh '''
                    docker stop nexgate-app || true
                    docker rm nexgate-app || true
                    docker run -d \
                        --name nexgate-app \
                        -p 8080:8080 \
                        nexgate-app
                '''
                echo '✅ Container started!'
            }
        }

        stage('✅ Check') {
            steps {
                echo '=== Checking if app is running ==='
                sleep(20)
                sh 'docker ps | grep nexgate-app'
                echo '🎉 SUCCESS! Your app is running!'
                echo '🌐 Check it at: http://localhost:8080'
            }
        }
    }

    post {
        success {
            echo '🎉🎉🎉 EVERYTHING WORKED! 🎉🎉🎉'
            echo '🌐 Your app: http://localhost:8080'
        }
        failure {
            echo '❌ Something went wrong. Check the logs above.'
        }
    }
}