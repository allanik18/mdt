#!/usr/bin/env groovy

node('master') {
  try {
    stage ('Build') {
      git url: 'https://github.com/cyrille-leclerc/multi-module-maven-project'
      withMaven(maven: 'maven-3.6.3') {
        sh 'mvn -f SpringBootweb/pom.xml -Dmaven.test.skip=true clean package'
        sh "cp SpringBootweb/target/demo-0.0.1-SNAPSHOT.jar demo-${BUILD_ID}-SNAPSHOT.jar"
        sh 'ls -la SpringBootweb/target'
      }
    }
  } catch (e) {
    currentBuild.result = 'FAILED'
    throw e
  } finally {
    def currentResult = currentBuild.result ?: 'SUCCESS'
    def previousResult = currentBuild.previousBuild?.result
    if (previousResult != null && previousResult != currentResult && currentResult == 'SUCCESS') {
      log.info("FIXED")
    } else if (currentResult == 'SUCCESS') {
      log.info("SUCCESS")
    }
    if (currentResult == 'UNSTABLE') {
      log.warning("UNSTABLE")
    }
    dir('SpringBootweb/target') {
      archiveArtifacts artifacts: '*.jar', onlyIfSuccessful: true
      sh "rm demo-${BUILD_ID}-SNAPSHOT.jar"
    }
  }
}