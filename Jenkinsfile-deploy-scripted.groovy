#!/usr/bin/env groovy

def serviceRedeploy(artifact) {
  sh """
    echo "Restart service with new artifact: demo-${artifact}-SNAPSHOT.jar"
    sudo rm /opt/app/app.jar
    sudo ln -s /home/jenkins/workspace/02-deploy-s/demo-${artifact}-SNAPSHOT.jar /opt/app/app.jar
    sudo systemctl restart myapp
  """
  println "\u001B[32mINFO: Wait for service start...\u001B[m"
  sleep(10)
}

def serviceStatus() {
  return sh (
    script: 'curl -s -o /dev/null -w "%{http_code}" http://localhost:8080',
    returnStdout: true
  ).trim()
}

def getContent() {
  return sh (
  script: 'curl -sSf http://localhost:8080/hello/world',
  returnStdout: true
  ).trim()
}

properties([parameters([string(defaultValue: 'latest', description: '', name: 'ARTIFACT_VERSION', trim: false)])])
node('aws') {
  ansiColor('xterm') {
    try {
      stage ('Deploy') {
        copyArtifacts filter: '*.jar', fingerprintArtifacts: true, projectName: '02-build-s-mb/develop', selector: specific('${ARTIFACT_VERSION}')
        serviceRedeploy(ARTIFACT_VERSION)
      }
      stage ('Integration tests') {
        if( serviceStatus() == '200' && getContent() == "Hello world") {
            println "\u001B[32mINFO: Integration tests passed\u001B[m"
        } else {
          println "\u001B[31mERROR: Integration tests failed\u001B[m"
          currentBuild.result = 'FAILED'
          throw "Integration tests failed"
        }
      }
    }
    catch (e) {
      currentBuild.result = 'FAILED'
      throw e
      error("Pipeline failed with error: ${e}")
    }
  }
}