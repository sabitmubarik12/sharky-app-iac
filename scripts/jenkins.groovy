pipeline {
    agent any
    environment {
        TARGET_URL = 'http://localhost:8085'  // Access through port-forwarding
        ZAP_PATH = '/var/lib/jenkins/ZAP/ZAP_2.15.0/zap.sh'
        ZAP_API_KEY = 'j1b1q6npi9e6dt3n1p2j1p8lvc'
        ZAP_PORT = '8081'
        GOOGLE_PROJECT = 'your-gcp-project-id'
        REGION = 'your-region'
    }
    tools { nodejs "NodeJS" }
    stages {
        stage('Checkout Repositories') {
            steps {
                script {
                    // Checkout Sharky-Service GitHub Repo
                    dir('Sharky-service') {
                        checkout([$class: 'GitSCM',
                                  branches: [[name: 'develop']],
                                  userRemoteConfigs: [[credentialsId: 'CI_GITHUB_CREDENTIALS',
                                                      url: 'https://github.com/your-repo/Sharky-service.git']]])
                    }
                    // Checkout Infra Repo
                    dir('infrastructure') {
                        checkout([$class: 'GitSCM',
                                  branches: [[name: 'develop']],
                                  userRemoteConfigs: [[credentialsId: 'CI_GITHUB_CREDENTIALS',
                                                      url: 'https://github.com/your-repo/k8s-assets.git']]])
                    }
                }
            }
        }
        stage('Build and Push Docker Image') {
            steps {
                script {
                    dir('Sharky-service') {
                        def COMMIT_ID = sh(script: "git rev-parse --short=7 HEAD", returnStdout: true).trim()
                        def IMAGE_NAME = "gcr.io/${GOOGLE_PROJECT}/sharky-service:${COMMIT_ID}"

                        // Build and push Docker image
                        sh """
                        gcloud auth configure-docker
                        docker build -t ${IMAGE_NAME} .
                        docker push ${IMAGE_NAME}
                        """
                        env.COMMIT_ID = COMMIT_ID
                        env.IMAGE_NAME = IMAGE_NAME
                    }
                }
            }
        }
        stage('Security Scanning') {
            parallel {
                stage('Dependency Check') {
                    steps {
                        sh '''
                        chmod +x /path/to/dependency-check.sh
                        /path/to/dependency-check.sh --project Sharky-service --out .
                        '''
                        archiveArtifacts artifacts: 'dependency-check-report.xml', allowEmptyArchive: true
                    }
                }
                stage('Secret Scanning') {
                    environment {
                        GG_TOKEN = credentials('gg-token')
                    }
                    steps {
                        dir('Sharky-service') {
                            sh '''
                            echo "Authenticating ggshield..."
                            echo "${GG_TOKEN}" | ggshield auth login --method=token
                            echo "Running ggshield secret scan..."
                            ggshield secret scan path . --recursive
                            '''
                        }
                    }
                }
                stage('SCA Analysis') {
                    steps {
                        dir('Sharky-service') {
                            sh '''
                            npm install
                            snyk test
                            '''
                        }
                    }
                }
                stage('DAST') {
                    steps {
                        sh '''
                        ${ZAP_PATH} -daemon -config api.key=${ZAP_API_KEY} -port ${ZAP_PORT} &
                        sleep 10
                        zap-cli --zap-url http://localhost:${ZAP_PORT} --api-key ${ZAP_API_KEY} open-url ${TARGET_URL}
                        zap-cli --zap-url http://localhost:${ZAP_PORT} --api-key ${ZAP_API_KEY} spider ${TARGET_URL}
                        zap-cli --zap-url http://localhost:${ZAP_PORT} --api-key ${ZAP_API_KEY} active-scan ${TARGET_URL}
                        zap-cli --zap-url http://localhost:${ZAP_PORT} --api-key ${ZAP_API_KEY} report -o zap_report.html -f html
                        zap-cli --zap-url http://localhost:${ZAP_PORT} --api-key ${ZAP_API_KEY} shutdown
                        '''
                        archiveArtifacts artifacts: 'zap_report.html', allowEmptyArchive: true
                    }
                }
            }
        }
        stage('Update GKE Deployment') {
            steps {
                script {
                    dir('infrastructure') {
                        // Set up kubeconfig
                        sh """
                        gcloud container clusters get-credentials your-cluster-name --region ${REGION} --project ${GOOGLE_PROJECT}
                        """

                        // Update Kubernetes deployment with new image
                        sh """
                        kubectl set image deployment/sharky-service sharky-service=${IMAGE_NAME} -n your-namespace
                        """
                    }
                }
            }
        }
        stage('Setup Port Forwarding') {
            steps {
                script {
                    sh """
                    gcloud container clusters get-credentials your-cluster-name --region ${REGION} --project ${GOOGLE_PROJECT}
                    kubectl port-forward svc/sharky-service 8085:80 -n your-namespace &
                    sleep 10
                    """
                }
            }
        }
    }
    post {
        success {
            emailext subject: 'Pipeline Success: ${env.JOB_NAME}',
                     body: 'Pipeline completed successfully.',
                     to: 'recipient@example.com'
        }
        failure {
            emailext subject: 'Pipeline Failure: ${env.JOB_NAME}',
                     body: 'Pipeline encountered errors.',
                     to: 'recipient@example.com'
        }
        always {
            deleteDir()  // Clean up workspace
        }
    }
}
