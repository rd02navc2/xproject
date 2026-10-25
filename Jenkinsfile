pipeline {
  agent any
  environment {
    REGISTRY = 'registry.example.com/xproject'      // 改成你的 Registry
    TAG      = "${env.BUILD_NUMBER}"
    CRED_ID  = 'docker-registry-cred'               // Jenkins 內建立的帳密憑證
  }
  options { timestamps(); disableConcurrentBuilds() }
  stages {
    stage('Checkout') { steps { checkout scm } }
    stage('Backend Test') {
      agent { docker { image 'maven:3.9-eclipse-temurin-21'; args '-v $HOME/.m2:/root/.m2' } }
      steps { dir('backend') { sh 'mvn -B test' } }
    }
    stage('Build Images') {
      steps {
        sh 'docker build -t $REGISTRY/xproject-device:$TAG ./backend'
        sh 'docker build -t $REGISTRY/xproject-web:$TAG ./frontend'
      }
    }
    stage('Push') {
      steps {
        withCredentials([usernamePassword(credentialsId: env.CRED_ID, usernameVariable: 'U', passwordVariable: 'P')]) {
          sh '''echo "$P" | docker login $REGISTRY -u "$U" --password-stdin
                docker push $REGISTRY/xproject-device:$TAG
                docker push $REGISTRY/xproject-web:$TAG'''
        }
      }
    }
    stage('Deploy') {
      when { branch 'main' }
      steps { sh 'docker compose up -d --pull always' }
    }
  }
  post { always { sh 'docker logout $REGISTRY || true' } }
}
