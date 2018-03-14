provider "vault" {
  address = "https://vault.reform.hmcts.net:6200"
}

data "vault_generic_secret" "sscs_notify_api_key" {
  path = "secret/${var.infrastructure_env}/sscs/sscs_notify_api_new_key"
}

data "vault_generic_secret" "s2s_url" {
  path = "secret/${var.infrastructure_env}/sscs/idam_s2s_api"
}

module "track-your-appeal-notifications" {
  source       = "git@github.com:contino/moj-module-webapp?ref=master"
  product      = "${var.product}-notif"
  location     = "${var.location}"
  env          = "${var.env}"
  ilbIp        = "${var.ilbIp}"
  is_frontend  = false
  subscription = "${var.subscription}"


  app_settings = {
    S2S_URL = "${data.vault_generic_secret.s2s_url.data["value"]}"
    MANAGEMENT_SECURITY_ENABLED = "${var.management_security_enabled}"
    NOTIFICATION_API_KEY = "${data.vault_generic_secret.sscs_notify_api_key.data["value"]}"
    EVIDENCE_SUBMISSION_INFO_LINK = "${var.evidence_submission_info_link}}"
    SSCS_MANAGE_EMAILS_LINK = "${var.sscs_manage_emails_link}"
    SSCS_TRACK_YOUR_APPEAL_LINK = "${var.sscs_track_your_appeal_link}"
    HEARING_INFO_LINK = "${var.hearing_info_link}"
    CLAIMING_EXPENSES_LINK = "${var.claiming_expenses_link}"
  }
}
