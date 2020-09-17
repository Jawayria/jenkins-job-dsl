package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class BackupVertica {
    public static def job = { dslFactory, allVars ->
        allVars.get('VERTICA_BACKUPS').each { backup, backup_config ->
            dslFactory.job("backup-vertica-$backup") {
                logRotator common_log_rotator(backup_config)
                wrappers common_wrappers(backup_config)
                publishers common_publishers(backup_config)
                triggers common_triggers(backup_config)
                parameters {
                    textParam('BACKUP_CMD', backup_config.get('VERTICA_BACKUP_CMD'), '')
                    stringParam('NOTIFY', '$PAGER_NOTIFY', 'Space separated list of emails to send notifications to.')
                }
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/backup-vertica.sh'))
                    if (allVars.get('OPSGENIE_HEARTBEAT_NAME') && allVars.get('OPSGENIE_HEARTBEAT_KEY')){
                        shell("curl -X GET 'https://api.opsgenie.com/v2/heartbeats/" + allVars.get('OPSGENIE_HEARTBEAT_NAME') + "/ping' -H 'Authorization: GenieKey " + allVars.get('OPSGENIE_HEARTBEAT_KEY') + "'")
                    }
                }
            }
        }
    }
}
