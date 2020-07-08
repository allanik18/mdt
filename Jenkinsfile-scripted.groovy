#!/usr/bin/env groovy

import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def info(message) {
  println "\u001B[32mINFO: ${message}\u001B[m"
}
def warning(message) {
  println "\u001B[33mWARNING: ${message}\u001B[m"
}
def error(message){
  println "\u001B[31mERROR: ${message}\u001B[m"
}

def when(boolean condition, body) {
  def config = [:]
  body.resolveStrategy = Closure.OWNER_FIRST
  body.delegate = config

  if (condition) {
    body()
  } else {
  warning("Stage ${STAGE_NAME} skipped")
  Utils.markStageSkippedForConditional(STAGE_NAME)
  }
}
// Using my personal laptop to run Jenkins, Nexus & Sonar on docker in order to not exceed AWS freetier
// I was able to run this pipeline on AWS EC2 Jenkins-slave successfully
// Deploy steps runs on AWS EC2 Jenkins-slave

node('master') {
  def mvnTool = tool name: 'maven-3.6.3', type: 'maven'
  ansiColor('xterm') {
    try {
      stage ('Checkout SCM') {
        git credentialsId: 'github', url: 'git@github.com:allanik18/mdt.git'
      }
      stage('SonarQube') {
        when (env.CHANGE_ID != null) {
          echo "Sonar scan"
          dir ('SpringBootweb') {
            withSonarQubeEnv(installationName: 'sonarQube') {
              sh """${mvnTool}/bin/mvn -f ./pom.xml \
                sonar:sonar \
                -Dsonar.projectKey=com.example.demo
                """
            }
          }
        }
      }
      stage("Quality Gate"){
        when (env.CHANGE_ID != null) {
          timeout(time: 10, unit: 'SECONDS') {
            def qg = waitForQualityGate(webhookSecretId: 'sonarQube')
            if (qg.status != 'OK') {
              error "Pipeline aborted due to quality gate failure: ${qg.status}"
            }
          }
        }
      }
      stage ('Build') {
        dir ('SpringBootweb') {
          sh "${mvnTool}/bin/mvn -f ./pom.xml -Dmaven.test.skip=true clean package"
          sh "mv target/demo-0.0.1-SNAPSHOT.jar target/demo-${BUILD_ID}-SNAPSHOT.jar"
          sh 'ls -la target'
        }
      }
      stage ('Archive artifact') {
        dir('SpringBootweb/target') {
          archiveArtifacts artifacts: '*.jar', onlyIfSuccessful: true
        }
      }
      stage ('Deploy'){
        build job: '02-deploy-s',
          parameters: [string(name: 'ARTIFACT_VERSION',
                              value: "${BUILD_ID}")]
      }
      stage ('cleanup ws') {
        sh "rm SpringBootweb/target/demo-${BUILD_ID}-SNAPSHOT.jar"
      }
    } catch (e) {
      currentBuild.result = 'FAILED'
      throw e
      error("Pipeline failed with error: ${e}")
    } finally {
      def currentResult = currentBuild.result ?: 'SUCCESS'
      def previousResult = currentBuild.previousBuild?.result
      if (previousResult != null && previousResult != currentResult && currentResult == 'SUCCESS') {
        info("Pipeline FIXED")
      } else if (currentResult == 'SUCCESS') {
        info("Pipeline completed successfully")
      }
      if (currentResult == 'UNSTABLE') {
        warning("Pipeline UNSTABLE")
      }
    }
  }
}