#!/usr/bin/env groovy

def info(message) {
  println "\u001B[32mINFO: ${message}\u001B[m"
}
def warning(message) {
  println "\u001B[33mWARNING: ${message}\u001B[m"
}
def error(message){
  println "\u001B[31mERROR: ${message}\u001B[m"
}

node('master') {
  ansiColor('xterm') {
    try {
      stage ('Checkout SCM') {
        git credentialsId: 'github', url: 'git@github.com:allanik18/mdt.git'
      }
      stage ('Build') {
        def mvnTool = tool name: 'maven-3.6.3', type: 'maven'
        dir ('SpringBootweb') {
          sh "${mvnTool}/bin/mvn -f ./pom.xml -Dmaven.test.skip=true clean package"
          sh "mv target/demo-0.0.1-SNAPSHOT.jar target/demo-${BUILD_ID}-SNAPSHOT.jar"
          sh 'ls -la target'
        }
      }
      stage ('Archive artifact') {
        dir('SpringBootweb/target') {
          archiveArtifacts artifacts: '*.jar', onlyIfSuccessful: true
          sh "rm demo-${BUILD_ID}-SNAPSHOT.jar"
        }
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