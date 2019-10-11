/*

 This job creates one job:
 - recover-tracking-logs

 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * BUCKET: S3 Bucket to examine for missing tracking logs (required)
    * SSH_ACCESS_CREDENTIALS: ssh access credentials, should be defined on the folder (required)
    * CONFIGURATION_REPO: name of config repo, default is https://github.com/edx/configuration.git
    * CONFIGURATION_BRANCH: default is master
    * PRIAVTE_CONFIGURATION_REPO: name of private config repo (required)
    * PRIVATE_CONFIGURATION_BRANCH: default is master
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLER_NAME: folder, default is Monitoring
    * AWS_REGION: AWS region to use, default is us-east-1
 
 This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format

*/
package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator

class MinosLifecycle {
    public static def job = { dslFactory, extraVars ->
        dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/recover-tracking-logs") {

            wrappers common_wrappers
            
            assert extraVars.containsKey('SSH_ACCESS_CREDENTIALS') : "Please define SSH_ACCESS_CREDENTIALS"
           
            wrappers {
                credentialsBinding{
                    file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                    string('ROLE_ARN', "minos-lifecycle-${deployment}-role-arn")
                } 
                sshAgent(extraVars.get('SSH_ACCESS_CREDENTIALS')) 
            }

            logRotator common_logrotator

            properties {
                rebuild {
                    autoRebuild(false)
                    rebuildDisabled(false)
                }
            }

#            triggers {
#                cron('H/5 * * * *')
#            }

            parameters{
                stringParam('BUCKET', extraVars.get('BUCKET')
                        'Git repo containing edX configuration.')
                stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                        'Git repo containing edX configuration.')
                stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                        'e.g. tagname or origin/branchname')
                stringParam('PRIVATE_CONFIGURATION_REPO', extraVars.get('PRIVATE_CONFIGURATION_REPO'),
                        'Git repo containing edX configuration.')
                stringParam('PRIVATE_CONFIGURATION_BRANCH', extraVars.get('PRIVATE_CONFIGURATION_BRANCH', 'master'),
                        'e.g. tagname or origin/branchname')
            }
        
            multiscm{
                git {
                    remote {
                        url('$CONFIGURATION_REPO')
                        branch('$CONFIGURATION_BRANCH')
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('configuration')
                    }
                }
                git {
                    remote {
                        url('$PRIVATE_CONFIGURATION_REPO')
                        branch('$PRIVATE_CONFIGURATION_BRANCH')
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('private-configuration')
                    }
                }
            }

            environmentVariables {
                env('AWS_REGION',extraVars.get('AWS_REGION','us-east-1'))
            }

            steps {

                virtualenv {
                    pythonName('System-CPython-3.6')
                    nature("shell")
                    systemSitePackages(false)
                    command(dslFactory.readFileFromWorkspace("devops/resources/recover_tracking_logs.sh"))
                }

#                String snitch =  inner_config.get('snitch','')
#                if (snitch) {
#                    shell("curl $snitch")
#                }
            }

            if (extraVars.get('NOTIFY_ON_FAILURE')){
                publishers {
                    mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)
                }
            }

        }

}
