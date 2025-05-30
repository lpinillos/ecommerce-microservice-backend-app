pipeline {
    agent any

    tools {
        maven 'mvn'
        jdk 'JDK_11'
    }

    environment {
        DOCKERHUB_USER = 'luispi18'
        DOCKER_CREDENTIALS_ID = 'docker-pwd'
        SERVICES = 'api-gateway cloud-config favourite-service order-service payment-service product-service proxy-client service-discovery shipping-service user-service locust'
        K8S_NAMESPACE = 'ecommerce'
        KUBECONFIG = 'C:\\Users\\ACER\\.kube\\config'
    }

    parameters {
        booleanParam(
            name: 'GENERATE_RELEASE_NOTES',
            defaultValue: true,
            description: 'Generate automatic release notes'
        )
        string(
            name: 'BUILD_TAG',
            defaultValue: "${env.BUILD_ID}",
            description: 'Tag for release notes identification'
        )
    }

    stages {
        stage('Init') {
            steps {
                script {
                    def profileConfig = [
                        master : ['prod', '-prod'],
                        release: ['stage', '-stage']
                    ]
                    def config = profileConfig.get(env.BRANCH_NAME, ['dev', '-dev'])

                    env.SPRING_PROFILES_ACTIVE = config[0]
                    env.IMAGE_TAG = config[0]
                    env.DEPLOYMENT_SUFFIX = config[1]

                    echo "Branch: ${env.BRANCH_NAME}"
                    echo "Spring profile: ${env.SPRING_PROFILES_ACTIVE}"
                    echo "Image tag: ${env.IMAGE_TAG}"
                    echo "Deployment suffix: ${env.DEPLOYMENT_SUFFIX}"
                }
            }
        }

        stage('Ensure Namespace') {
            steps {
                script {
                    def ns = env.K8S_NAMESPACE
                    bat "kubectl get namespace ${ns} || kubectl create namespace ${ns}"
                }
            }
        }

        stage('Checkout') {
            steps {
                git branch: "${env.BRANCH_NAME}", url: 'https://github.com/lpinillos/ecommerce-microservice-backend-app.git'
            }
        }

        stage('Verify Tools') {
            steps {
                bat 'java -version'
                bat 'mvn -version'
                bat 'docker --version'
                bat 'kubectl config current-context'
            }
        }

        stage('Build & Package') {
            when { anyOf { branch 'master'; branch 'stage' ; branch 'dev'} }
            steps {
                bat "mvn clean package -DskipTests"
            }
        }

        stage('Build & Push Docker Images') {
            when { anyOf { branch 'master'; branch 'stage' ; branch 'dev'} }
            steps {
                withCredentials([string(credentialsId: "${DOCKER_CREDENTIALS_ID}", variable: 'password')]) {
                    bat "docker login -u ${DOCKERHUB_USER} -p ${password}"

                    script {
                        SERVICES.split().each { service ->
                            bat "docker build -t ${DOCKERHUB_USER}/${service}:${IMAGE_TAG} .\\${service}"
                            bat "docker push ${DOCKERHUB_USER}/${service}:${IMAGE_TAG}"
                        }
                    }
                }
            }
        }

        stage('Unit Tests') {
            when { anyOf { branch 'dev'; } }
            steps {
                script {
                    ['user-service', 'product-service', 'payment-service'].each {
                        bat "mvn test -pl ${it}"
                    }
                }
            }
        }

        stage('Integration Tests') {
            when { anyOf { branch 'stage'; } }
            steps {
                script {
                    ['user-service', 'product-service'].each {
                        bat "mvn verify -pl ${it}"
                    }
                }
            }
        }

        stage('E2E Tests') {
            when { anyOf { branch 'stage'; } }
            steps {
                bat "mvn verify -pl e2e-tests"
            }
        }

        stage('Start containers for testing') {
            when { anyOf { branch 'stage'; } }
            steps {
                script {
                    powershell '''
                    function Wait-ForHealthCheck {
                        param(
                            [string]$Url,
                            [string]$ServiceName,
                            [int]$TimeoutSeconds = 300
                        )

                        Write-Host "Waiting for $ServiceName..."
                        $startTime = Get-Date

                        do {
                            try {
                                $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 5 -ErrorAction SilentlyContinue
                                if ($response.status -eq "UP") {
                                    Write-Host "$ServiceName is healthy!"
                                    return $true
                                }
                            }
                            catch {
                                # Retry until successful
                            }

                            Start-Sleep -Seconds 5
                            $elapsed = (Get-Date) - $startTime

                            if ($elapsed.TotalSeconds -gt $TimeoutSeconds) {
                                Write-Host "Timeout waiting for $ServiceName"
                                return $false
                            }

                            Write-Host "Waiting for $ServiceName... ($([int]$elapsed.TotalSeconds)s)"
                        } while ($true)
                    }

                    function Wait-ForHealthCheckWithJq {
                        param(
                            [string]$Url,
                            [string]$ServiceName,
                            [int]$TimeoutSeconds = 300
                        )

                        Write-Host "Waiting for $ServiceName..."
                        $startTime = Get-Date

                        do {
                            try {
                                $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 5 -ErrorAction SilentlyContinue
                                if ($response.status -eq "UP") {
                                    Write-Host "$ServiceName is healthy!"
                                    return $true
                                }
                            }
                            catch {
                                # Retry until successful
                            }

                            Start-Sleep -Seconds 5
                            $elapsed = (Get-Date) - $startTime

                            if ($elapsed.TotalSeconds -gt $TimeoutSeconds) {
                                Write-Host "Timeout waiting for $ServiceName"
                                return $false
                            }

                            Write-Host "Waiting for $ServiceName... ($([int]$elapsed.TotalSeconds)s)"
                        } while ($true)
                    }

                    try {
                        Write-Host "Creating Docker network..."
                        docker network create ecommerce-test 2>$null
                        if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne 1) {
                            throw "Error creating Docker network"
                        }

                        Write-Host "Starting ZIPKIN..."
                        docker run -d --name zipkin-container --network ecommerce-test -p 9411:9411 openzipkin/zipkin
                        if ($LASTEXITCODE -ne 0) { throw "Error starting Zipkin" }

                        Write-Host "Starting EUREKA..."
                        docker run -d --name service-discovery-container --network ecommerce-test -p 8761:8761 `
                            -e SPRING_PROFILES_ACTIVE=dev `
                            -e SPRING_ZIPKIN_BASE_URL=http://zipkin-container:9411 `
                            luispi18/service-discovery:${env:IMAGE_TAG}
                        if ($LASTEXITCODE -ne 0) { throw "Error starting Eureka" }

                        if (!(Wait-ForHealthCheck -Url "http://localhost:8761/actuator/health" -ServiceName "EUREKA")) {
                            throw "Eureka could not be started correctly"
                        }

                        Write-Host "Starting CLOUD-CONFIG..."
                        docker run -d --name cloud-config-container --network ecommerce-test -p 9296:9296 `
                            -e SPRING_PROFILES_ACTIVE=dev `
                            -e SPRING_ZIPKIN_BASE_URL=http://zipkin-container:9411 `
                            -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://service-discovery-container:8761/eureka/ `
                            -e EUREKA_INSTANCE=cloud-config-container `
                            luispi18/cloud-config:${env:IMAGE_TAG}
                        if ($LASTEXITCODE -ne 0) { throw "Error starting Cloud Config" }

                        if (!(Wait-ForHealthCheck -Url "http://localhost:9296/actuator/health" -ServiceName "CLOUD-CONFIG")) {
                            throw "CLOUD-CONFIG could not be started correctly"
                        }

                        Write-Host "Starting ORDER-SERVICE..."
                        docker run -d --name order-service-container --network ecommerce-test -p 8300:8300 `
                            -e SPRING_PROFILES_ACTIVE=dev `
                            -e SPRING_ZIPKIN_BASE_URL=http://zipkin-container:9411 `
                            -e SPRING_CONFIG_IMPORT=optional:configserver:http://cloud-config-container:9296 `
                            -e EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-discovery-container:8761/eureka `
                            -e EUREKA_INSTANCE=order-service-container `
                            luispi18/order-service:${env:IMAGE_TAG}
                        if ($LASTEXITCODE -ne 0) { throw "Error starting Order Service" }

                        if (!(Wait-ForHealthCheckWithJq -Url "http://localhost:8300/order-service/actuator/health" -ServiceName "ORDER-SERVICE")) {
                            throw "ORDER-SERVICE could not be started correctly"
                        }

                        Write-Host "Starting PAYMENT..."
                        docker run -d --name payment-service-container --network ecommerce-test -p 8400:8400 `
                            -e SPRING_PROFILES_ACTIVE=dev `
                            -e SPRING_ZIPKIN_BASE_URL=http://zipkin-container:9411 `
                            -e SPRING_CONFIG_IMPORT=optional:configserver:http://cloud-config-container:9296 `
                            -e EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-discovery-container:8761/eureka `
                            -e EUREKA_INSTANCE=payment-service-container `
                            luispi18/payment-service:${env:IMAGE_TAG}
                        if ($LASTEXITCODE -ne 0) { throw "Error starting Payment Service" }

                        if (!(Wait-ForHealthCheckWithJq -Url "http://localhost:8400/payment-service/actuator/health" -ServiceName "PAYMENT-SERVICE")) {
                            throw "PAYMENT-SERVICE could not be started correctly"
                        }

                        Write-Host "Starting PRODUCT..."
                        docker run -d --name product-service-container --network ecommerce-test -p 8500:8500 `
                            -e SPRING_PROFILES_ACTIVE=dev `
                            -e SPRING_ZIPKIN_BASE_URL=http://zipkin-container:9411 `
                            -e SPRING_CONFIG_IMPORT=optional:configserver:http://cloud-config-container:9296 `
                            -e EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-discovery-container:8761/eureka `
                            -e EUREKA_INSTANCE=product-service-container `
                            luispi18/product-service:${env:IMAGE_TAG}
                        if ($LASTEXITCODE -ne 0) { throw "Error starting Product Service" }

                        if (!(Wait-ForHealthCheckWithJq -Url "http://localhost:8500/product-service/actuator/health" -ServiceName "PRODUCT-SERVICE")) {
                            throw "PRODUCT-SERVICE could not be started correctly"
                        }

                        Write-Host "Starting SHIPPING..."
                        docker run -d --name shipping-service-container --network ecommerce-test -p 8600:8600 `
                            -e SPRING_PROFILES_ACTIVE=dev `
                            -e SPRING_ZIPKIN_BASE_URL=http://zipkin-container:9411 `
                            -e SPRING_CONFIG_IMPORT=optional:configserver:http://cloud-config-container:9296 `
                            -e EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-discovery-container:8761/eureka `
                            -e EUREKA_INSTANCE=shipping-service-container `
                            luispi18/shipping-service:${env:IMAGE_TAG}
                        if ($LASTEXITCODE -ne 0) { throw "Error starting Shipping Service" }

                        if (!(Wait-ForHealthCheckWithJq -Url "http://localhost:8600/shipping-service/actuator/health" -ServiceName "SHIPPING-SERVICE")) {
                            throw "SHIPPING-SERVICE could not be started correctly"
                        }

                        Write-Host "Starting USER..."
                        docker run -d --name user-service-container --network ecommerce-test -p 8700:8700 `
                            -e SPRING_PROFILES_ACTIVE=dev `
                            -e SPRING_ZIPKIN_BASE_URL=http://zipkin-container:9411 `
                            -e SPRING_CONFIG_IMPORT=optional:configserver:http://cloud-config-container:9296 `
                            -e EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-discovery-container:8761/eureka `
                            -e EUREKA_INSTANCE=user-service-container `
                            luispi18/user-service:${env:IMAGE_TAG}
                        if ($LASTEXITCODE -ne 0) { throw "Error starting User Service" }

                        if (!(Wait-ForHealthCheckWithJq -Url "http://localhost:8700/user-service/actuator/health" -ServiceName "USER-SERVICE")) {
                            throw "USER-SERVICE could not be started correctly"
                        }

                        Write-Host "Starting FAVOURITE..."
                        docker run -d --name favourite-service-container --network ecommerce-test -p 8800:8800 `
                            -e SPRING_PROFILES_ACTIVE=dev `
                            -e SPRING_ZIPKIN_BASE_URL=http://zipkin-container:9411 `
                            -e SPRING_CONFIG_IMPORT=optional:configserver:http://cloud-config-container:9296 `
                            -e EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-discovery-container:8761/eureka `
                            -e EUREKA_INSTANCE=favourite-service-container `
                            luispi18/favourite-service:${env:IMAGE_TAG}
                        if ($LASTEXITCODE -ne 0) { throw "Error starting Favourite Service" }

                        if (!(Wait-ForHealthCheckWithJq -Url "http://localhost:8800/favourite-service/actuator/health" -ServiceName "FAVOURITE-SERVICE")) {
                            throw "FAVOURITE-SERVICE could not be started correctly"
                        }

                        Write-Host "All containers are up and healthy."
                    }
                    catch {
                        Write-Host "Error: $_"
                        Write-Host "Cleaning up containers..."

                        $containers = @(
                            "favourite-service-container",
                            "user-service-container",
                            "shipping-service-container",
                            "product-service-container",
                            "payment-service-container",
                            "order-service-container",
                            "cloud-config-container",
                            "service-discovery-container",
                            "zipkin-container"
                        )

                        foreach ($container in $containers) {
                            docker stop $container 2>$null
                            docker rm $container 2>$null
                        }

                        docker network rm ecommerce-test 2>$null
                        throw "Failed to start containers"
                    }
                    '''
                }
            }
        }

        stage('Run Load Tests with Locust') {
            when { anyOf { branch 'stage'; } }
            steps {
                script {
                    bat '''
                    echo Starting Locust for order-service...
                    docker run --rm --network ecommerce-test ^
                      -v "%CD%\\locust:/mnt" ^
                      -v "%CD%\\locust-results:/app" ^
                      luispi18/locust:%IMAGE_TAG% ^
                      -f /mnt/test/order-service/locustfile.py ^
                      --host http://order-service-container:8300 ^
                      --headless -u 5 -r 1 -t 1m ^
                      --csv order-service-stats --csv-full-history

                    echo Starting Locust for payment-service...
                    docker run --rm --network ecommerce-test ^
                      -v "%CD%\\locust:/mnt" ^
                      -v "%CD%\\locust-results:/app" ^
                      luispi18/locust:%IMAGE_TAG% ^
                      -f /mnt/test/payment-service/locustfile.py ^
                      --host http://payment-service-container:8400 ^
                      --headless -u 5 -r 1 -t 1m ^
                      --csv payment-service-stats --csv-full-history

                    echo Starting Locust for favourite-service...
                    docker run --rm --network ecommerce-test ^
                      -v "%CD%\\locust:/mnt" ^
                      -v "%CD%\\locust-results:/app" ^
                      luispi18/locust:%IMAGE_TAG% ^
                      -f /mnt/test/favourite-service/locustfile.py ^
                      --host http://favourite-service-container:8800 ^
                      --headless -u 5 -r 1 -t 1m ^
                      --csv favourite-service-stats --csv-full-history

                    echo Tests completed
                    '''
                }
            }
        }

        stage('Run Stress Tests with Locust') {
            when { anyOf { branch 'stage'; } }
            steps {
                script {
                    bat '''
                    echo Starting Locust for stress testing...

                    docker run --rm --network ecommerce-test ^
                    -v "%CD%\\locust:/mnt" ^
                    -v "%CD%\\locust-results:/app" ^
                    luispi18/locust:%IMAGE_TAG% ^
                    -f /mnt/test/order-service/locustfile.py ^
                    --host http://order-service-container:8300 ^
                    --headless -u 5 -r 1 -t 1m ^
                    --csv order-service-stress --csv-full-history

                    docker run --rm --network ecommerce-test ^
                    -v "%CD%\\locust:/mnt" ^
                    -v "%CD%\\locust-results:/app" ^
                    luispi18/locust:%IMAGE_TAG% ^
                    -f /mnt/test/payment-service/locustfile.py ^
                    --host http://payment-service-container:8400 ^
                    --headless -u 5 -r 1 -t 1m ^
                    --csv payment-service-stress --csv-full-history

                    docker run --rm --network ecommerce-test ^
                    -v "%CD%\\locust:/mnt" ^
                    -v "%CD%\\locust-results:/app" ^
                    luispi18/locust:%IMAGE_TAG% ^
                    -f /mnt/test/favourite-service/locustfile.py ^
                    --host http://favourite-service-container:8800 ^
                    --headless -u 5 -r 1 -t 1m ^
                    --csv favourite-service-stress --csv-full-history

                    echo Stress tests completed
                    '''
                }
            }
        }

        stage('Stop and remove containers') {
            when { anyOf { branch 'stage'; } }
            steps {
                script {
                    bat """
                    echo Stopping and removing containers...

                    docker rm -f locust || exit 0
                    docker rm -f favourite-service-container || exit 0
                    docker rm -f user-service-container || exit 0
                    docker rm -f shipping-service-container || exit 0
                    docker rm -f product-service-container || exit 0
                    docker rm -f payment-service-container || exit 0
                    docker rm -f order-service-container || exit 0
                    docker rm -f cloud-config-container || exit 0
                    docker rm -f service-discovery-container || exit 0
                    docker rm -f zipkin-container || exit 0

                    echo All containers removed
                    """
                }
            }
        }

        stage('Deploy Common Config') {
            when { anyOf { branch 'master'; } }
            steps {
                bat "kubectl apply -f k8s\\common-config.yaml -n ${K8S_NAMESPACE}"
            }
        }

        stage('Deploy Core Services') {
            when { anyOf { branch 'master'; } }
            steps {
                bat "kubectl apply -f k8s\\zipkin -n ${K8S_NAMESPACE}"
                bat "kubectl rollout status deployment/zipkin -n ${K8S_NAMESPACE} --timeout=200s"

                bat "kubectl apply -f k8s\\service-discovery -n ${K8S_NAMESPACE}"
                bat "kubectl set image deployment/service-discovery service-discovery=${DOCKERHUB_USER}/service-discovery:${IMAGE_TAG} -n ${K8S_NAMESPACE}"
                bat "kubectl rollout status deployment/service-discovery -n ${K8S_NAMESPACE} --timeout=400s"

                bat "kubectl apply -f k8s\\cloud-config -n ${K8S_NAMESPACE}"
                bat "kubectl set image deployment/cloud-config cloud-config=${DOCKERHUB_USER}/cloud-config:${IMAGE_TAG} -n ${K8S_NAMESPACE}"
                bat "kubectl rollout status deployment/cloud-config -n ${K8S_NAMESPACE} --timeout=350s"
            }
        }

        stage('Deploy Microservices') {
            when { anyOf { branch 'master'; } }
            steps {
                script {
                    echo "Deploy microservices stage"
                    // Commented out deployment code
                }
            }
        }

        stage('Generate Release Notes') {
            when {
                expression { params.GENERATE_RELEASE_NOTES }
            }
            steps {
                script {
                    echo "GENERATE RELEASE NOTES"
                    generateReleaseNotes()
                }
            }
        }
    }

    post {
        success {
            script {
                echo "Pipeline completed successfully for ${env.BRANCH_NAME} branch."
                echo "Environment: ${env.SPRING_PROFILE}"

                if (env.BRANCH_NAME == 'master') {
                    echo "Production deployment completed successfully!"
                } else if (env.BRANCH_NAME == 'release') {
                    echo "Staging deployment completed successfully!"
                } else {
                    echo "Development tests completed successfully!"
                }
            }
        }
        failure {
            script {
                echo "Pipeline failed for ${env.BRANCH_NAME} branch."
                echo "Check the logs for details."
                echo "Notify the development team about the failure."
            }
        }
        unstable {
            script {
                echo "Pipeline completed with warnings for ${env.BRANCH_NAME} branch."
                echo "Some tests may have failed. Review test reports."
            }
        }
        always {
            script {
                if (params.GENERATE_RELEASE_NOTES) {
                    archiveArtifacts artifacts: 'release-notes-*.md', allowEmptyArchive: true
                }
            }
        }
    }
}

def generateReleaseNotes() {
    echo "Generating automatic Release Notes..."

    try {
        def buildTag = params.BUILD_TAG ?: env.BUILD_ID
        def releaseNotesFile = "release-notes-${buildTag}.md"

        def gitCommit = bat(returnStdout: true, script: 'git rev-parse HEAD').trim()
        def gitBranch = env.BRANCH_NAME ?: 'unknown'
        def buildDate = new Date().format('yyyy-MM-dd HH:mm:ss')

        def recentCommits = ""
        try {
           recentCommits = bat(returnStdout: true, script: 'git log --oneline --since="3 days ago" -n 10').trim()
            if (!recentCommits) {
                recentCommits = "No recent commits found in the last 3 days"
            }
        } catch (Exception e) {
            recentCommits = "Could not retrieve recent commits: ${e.message}"
        }

        def deploymentStatus = ""
        switch(env.BRANCH_NAME) {
            case 'master':
                deploymentStatus = "Successfully deployed to PRODUCTION environment"
                break
            case 'release':
                deploymentStatus = "Successfully deployed to STAGING environment"
                break
            default:
                deploymentStatus = "Tests completed for DEVELOPMENT environment"
        }

        def releaseNotes = """
# Release Notes - Build ${buildTag}

## Build Information
- **Build Number**: ${env.BUILD_NUMBER}
- **Build Tag**: ${buildTag}
- **Branch**: ${gitBranch}
- **Environment**: ${env.SPRING_PROFILE}
- **Date**: ${buildDate}
- **Git Commit**: ${gitCommit}
- **Jenkins URL**: ${env.BUILD_URL}

## Deployed Services (${env.SPRING_PROFILE} environment)
${SERVICES.split().collect { "- ${it}" }.join('\n')}

## Additional Infrastructure
- zipkin (monitoring)
- Kubernetes namespace: ${env.K8S_NAMESPACE}

## Test Results Summary
- **Unit Tests**: ${shouldRunTests() ? 'EXECUTED' : 'SKIPPED'}
- **Integration Tests**: ${shouldRunIntegrationTests() ? 'EXECUTED' : 'SKIPPED'}
- **E2E Tests**: ${shouldRunE2ETests() ? 'EXECUTED' : 'SKIPPED'}

## Recent Changes
```
${recentCommits}
```

## Docker Images Built
${env.BRANCH_NAME == 'master' ? SERVICES.split().collect { "- ${DOCKERHUB_USER}/${it}:${env.IMAGE_TAG}" }.join('\n') : 'No Docker images built for this branch'}

## Deployment Configuration
- **Spring Profile**: ${env.SPRING_PROFILE}
- **Image Tag**: ${env.IMAGE_TAG}
- **Deployment Suffix**: ${env.DEPLOYMENT_SUFFIX}
- **Kubernetes Namespace**: ${env.K8S_NAMESPACE}

## Deployment Status
${deploymentStatus}

## Pipeline Execution Details
- **Started**: ${new Date(currentBuild.startTimeInMillis).format('yyyy-MM-dd HH:mm:ss')}
- **Duration**: ${currentBuild.durationString}
- **Triggered by**: ${env.BUILD_CAUSE ?: 'Manual/SCM'}

---
*Generated automatically by Jenkins Pipeline on ${buildDate}*
*Pipeline: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}*
"""

        writeFile(file: releaseNotesFile, text: releaseNotes)

        echo "Release Notes generated successfully: ${releaseNotesFile}"
        echo "File will be save as artifact"

        echo """
RELEASE NOTES SUMMARY
Build: ${buildTag}
Branch: ${gitBranch}
Environment: ${env.SPRING_PROFILE}
Date: ${buildDate}
File: ${releaseNotesFile}
"""

    } catch (Exception e) {
        echo "Error generating Release Notes: ${e.message}"
        echo "Pipeline will continue without Release Notes"

        def fallbackFile = "release-notes-${params.BUILD_TAG ?: env.BUILD_ID}-minimal.md"
        def minimalNotes = """
# Release Notes - Build ${params.BUILD_TAG ?: env.BUILD_ID}

**Error**: Could not generate complete release notes due to: ${e.message}

## Basic Information
- Build Number: ${env.BUILD_NUMBER}
- Branch: ${env.BRANCH_NAME}
- Environment: ${env.SPRING_PROFILE}
- Date: ${new Date().format('yyyy-MM-dd HH:mm:ss')}

Pipeline executed successfully despite release notes generation error.
"""
        writeFile(file: fallbackFile, text: minimalNotes)
        echo "Minimal release notes created: ${fallbackFile}"
    }
}

def shouldRunTests() {
    return env.BRANCH_NAME in ['dev', 'master', 'release'] || env.BRANCH_NAME.startsWith('feature/')
}

def shouldRunIntegrationTests() {
    return env.BRANCH_NAME == 'master' || env.BRANCH_NAME.startsWith('feature/') ||
           (env.BRANCH_NAME != 'master' && env.BRANCH_NAME != 'release')
}

def shouldRunE2ETests() {
    return env.BRANCH_NAME == 'master' || env.BRANCH_NAME.startsWith('feature/') ||
           (env.BRANCH_NAME != 'master' && env.BRANCH_NAME != 'release')
}