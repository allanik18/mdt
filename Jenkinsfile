pipeline {
  // triggers {
  //   pollSCM 'H * * * *'
  // }
  agent any
  // environment {
  //   DOCKER_PREFIX = "alla18/spring-boot"
  //   DOCKER_IMAGE = "${env.DOCKER_PREFIX}:${BUILD_ID}"
  // }
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
        ansiColor('xterm') {
          sh 'mvn -f SpringBootweb/pom.xml -Dmaven.test.skip=true clean package'
          // sh "cp spring-boot/spring-boot-samples/spring-boot-sample-web-ui/target/spring-boot-sample-web-ui-2.1.14.BUILD-SNAPSHOT.jar spring-boot-sample-web-ui-2.1.14.BUILD-${BUILD_ID}.jar"
          sh 'ls -la SpringBootweb/target'
        }
      }
    }
  }
}