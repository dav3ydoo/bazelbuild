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
			name: 'PARAM1',
			defaultValue: '',
			description: 'Some string to print out'
		)
    }
    stages {
        stage('Hello') {
            steps {
                echo "HELLO " + "${params.PARAM1}"
            }
        }
        stage ("Output to PLM") {
            steps {
                script {
                    // The following is required for the PLM result processing
                    print """BEGIN OUTPUT{"output1": "${PARAM1}"}END OUTPUT"""
                }
            }
        }
    }
}