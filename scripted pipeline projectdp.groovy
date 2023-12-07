node() {
    withCredentials([usernamePassword(credentialsId: 'docker-login', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
        // Set Docker environment variables
        env.DOCKER_USERNAME = DOCKER_USERNAME
        env.DOCKER_PASSWORD = DOCKER_PASSWORD

        // Now you can use env.DOCKER_USERNAME and env.DOCKER_PASSWORD in subsequent steps
    } 

    
    def mavenHome

    // Stages for building and deploying the Maven web application
    stage("1. Get Code") {
        git branch: 'main', url: 'https://github.com/erobzy/maven-web-application.git'
    }

    stage("2. Test + Build") {
        mavenHome = tool name: "maven386"
        sh "${mavenHome}/bin/mvn clean package"
    }

    stage("3. Code Quality Analysis") {
        sh "${mavenHome}/bin/mvn sonar:sonar"
    }

    stage("4. Upload Artifacts") {
        sh "${mavenHome}/bin/mvn deploy"
    }

    stage("5. Deploy to UAT Tomcat App Server") {
        deploy adapters: [tomcat9(credentialsId: 'TOMCAT-CREDENTIALS', path: '', url: 'http://54.145.112.153:8080/')], contextPath: null, onFailure: false, war: 'target/*war'
    }

    stage("6. Approval Gate") {
        timeout(time: 1, unit: 'HOURS') {
            input message: "Please review the application and give feedback for Go-No-Go"
        }
    }

    stage("7. Deploy to PROD Tomcat App Server") {
        deploy adapters: [tomcat9(credentialsId: 'TOMCAT-CREDENTIALS', path: '', url: 'http://54.145.112.153:8080/')], contextPath: null, onFailure: false, war: 'target/*war'
    }

    stage("8. Notifications") {
        emailext body: '''Hi All, 
            Please find the status of the project

            Regards
            DataPandas''', recipientProviders: [buildUser(), contributor(), culprits(), developers(), requestor()], subject: 'CompareTheMarket Project build status', to: 'devopsengineers@datapandas.com'
    }

    stage('Build and Push Docker Image') {
            
                script {
                    // Log in to Docker Hub
                    sh "docker login -u ${env.DOCKER_USERNAME} -p ${env.DOCKER_PASSWORD}"

                    // Build and push Docker image
                    sh "docker build -t erobzy/mvn-app:latest ."
                    sh "docker push erobzy/mvn-app:latest"
                
            }
        }
}
