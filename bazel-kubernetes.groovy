/* 
Author: Roberto Becchini (roberto.becchini@windriver.com)
Date: 4 Oct 2021

Note:
I used the Bazel Docker image l.gcr.io/google/bazel:latest'. Alternate image not tried yet: docker.io/insready/bazel:latest
*/

pipeline {
    agent {
        kubernetes {
            label 'roberto'  
            idleMinutes 5  
            yamlFile 'build-pod.yaml'  
            defaultContainer 'bazel'
        }
    }

    parameters {
		string(
			name: 'GITURL',
			defaultValue: '',
			description: 'URL of the git project to compile'
		)
        string(
			name: 'BRANCH',
			defaultValue: 'main',
			description: 'Git branch'
		)
		string(
			name: 'PRJ',
			defaultValue: '',
			description: 'What bezel will build: #bezel build PRJ'
		)
    }
    stages {
        stage ("Git clone") {
            steps {
                git branch:"${params.BRANCH}", url:"${params.GITURL}"
            }
        }
        stage ("Versions") {
            steps {
                script {
                    sh "clang --version"
                    sh "java -version"
                    sh "python3 -V"
                    sh "bazel version"
                }
            }
        }
        stage ("Build") {
            steps {
                script {
                    sh "export USER=\"wrlbuild\"; bazel build ${params.PRJ}"               
                }

            }
        }
    }
}
