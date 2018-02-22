#!groovy
properties([
        [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/hmcts/track-your-appeal-notifications.git'],
        pipelineTriggers([[$class: 'GitHubPushTrigger']])
])

@Library("Infrastructure")

def product = "sscs-track-your-appeal"
def component = "notif"

withPipeline("java", product, component) {
    enableSlackNotifications('#sscs-tech')
}

