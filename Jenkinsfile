pipeline {
  agent {
    // label 'aws'
    label 'master'
  }
  tools {
    maven 'maven-3.6.3'
  }
  stages {
    stage('Initialize') {
      steps {
        sh '''
          echo "PATH = ${PATH}"
          echo "M2_HOME = ${M2_HOME}"
        '''
      }
    }
    stage('Build JAR') {
      steps {
        dir('SpringBootweb') {
          sh 'mvn -f ./pom.xml -Dmaven.test.skip=true clean package'
          sh "mv target/demo-0.0.1-SNAPSHOT.jar target/demo-${BUILD_ID}-SNAPSHOT.jar"
          sh 'ls -la target'
        }
      }
    }
    stage('run-on-pr') {
      when {
        expression { env.CHANGE_ID ==~ /.*/ }
      }
      steps {
        echo "this is a pull request"
      }
    }
  }
  post {
    always {
      dir('SpringBootweb/target') {
        archiveArtifacts artifacts: '*.jar', onlyIfSuccessful: true, fingerprint: true
      }
    }
    cleanup {
      dir('SpringBootweb/target') {
        sh "rm demo-${BUILD_ID}-SNAPSHOT.jar"
      }
    }
  }
}