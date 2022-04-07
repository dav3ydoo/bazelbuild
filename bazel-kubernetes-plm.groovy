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
			description: 'URL of the git project to compile'
		)
        string(
			name: 'PARAM2',
			defaultValue: 'main',
			description: 'Git branch'
		)
		string(
			name: 'PARAM3',
			defaultValue: '',
			description: 'What bezel will build: #bezel build PRJ'
		)
        string(
            name: 'PARAM4',
            defaultValue: 'robertoBuilds',
            description: 'Artifacts root path'
        )
    }
    stages {
        stage("Install Minio client") {
			when {
				expression {
					env.INTERNAL_ARTIFACTS_HTTP != null
				}
			}
			steps {
				script {
					sh "mkdir -p ~/.mc"
					sh "chmod 777 ~/.mc"
					sh "wget https://dl.min.io/client/mc/release/linux-amd64/mc"
					sh "chmod +x mc"
					withCredentials([usernamePassword(credentialsId: 'artifacts-credentials', usernameVariable: 'MINIO_ACCESS_KEY', passwordVariable: 'MINIO_SECRET_KEY')]) {
						sh './mc config host add minio \"${INTERNAL_ARTIFACTS_HTTP}\" \"${MINIO_ACCESS_KEY}\" \"${MINIO_SECRET_KEY}\"'
					}
				}
			}
		}
        stage ("Git clone") {
            steps {
                git branch:"${params.PARAM2}", url:"${params.PARAM1}"
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
                    sh "export USER=\"wrlbuild\"; bazel build ${params.PARAM3}"               
                }

            }
        }
stage("Store artifacts") {
			when {
				allOf {
					expression {
						env.INTERNAL_ARTIFACTS_HTTP != null
					}
					expression {
						params.PARAM4 != null
					}
				}
			}
			steps {
				script {
					sh "ls -al ."
					sh "./mc mb minio/plm/robertoBecchini/${params.PARAM4}/${BUILD_NUMBER}"
                    sh "tar cfh bazelOutput.tar ./bazel-bazelBuild ./bazel-bin ./bazel-out ./bazel-testlogs"
					sh "./mc cp ./bazelOutput.tar minio/plm/robertoBecchini/${params.PARAM4}/${BUILD_NUMBER}"
				}
			}
		}
        stage ("Output to PLM") {
            steps {
                script {
                    // The following is required for the PLM result processing
                    print """BEGIN OUTPUT{"output1": "${PARAM1} and ${PARAM2}"}END OUTPUT"""
                }
            }
        }
    }
}
