provider "vault" {
  address = "https://vault.reform.hmcts.net:6200"
}

data "vault_generic_secret" "sscs_notify_api_key" {
  path = "secret/${var.infrastructure_env}/sscs/sscs_notify_api_new_key"
}

data "vault_generic_secret" "s2s_url" {
  path = "secret/${var.infrastructure_env}/sscs/idam_s2s_api"
}

data "vault_generic_secret" "mac_secret" {
  path = "secret/${var.infrastructure_env}/sscs/sscs_email_mac_secret_text"
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
    EVIDENCE_SUBMISSION_INFO_LINK = "${var.evidence_submission_info_link}"
    SSCS_MANAGE_EMAILS_LINK = "${var.sscs_manage_emails_link}"
    SSCS_TRACK_YOUR_APPEAL_LINK = "${var.sscs_track_your_appeal_link}"
    HEARING_INFO_LINK = "${var.hearing_info_link}"
    CLAIMING_EXPENSES_LINK = "${var.claiming_expenses_link}"
    JOB_SCHEDULER_ENABLED = "${var.job_scheduler_enabled}"
    JOB_SCHEDULER_URL = "${var.job_scheduler_url}"
    JOB_SCHEDULER_CALLBACK_URL = "${var.job_scheduler_callbackUrl}"
    APPEAL_RECEIVED_EMAIL_TEMPLATE_ID = "dd955503-42f4-45f8-a692-39377a0f340f"
    RESPONSE_RECEIVED_EMAIL_TEMPLATE_ID = "1e13bd2d-9ae0-4030-89b9-a0ef65ec36ef"
    EVIDENCE_RECEIVED_EMAIL_TEMPLATE_ID = "c5654134-2e13-4541-ac73-334a5b5cdbb6"
    EVIDENCE_RECEIVED_SMS_TEMPLATE_ID = "74bda35f-040b-4355-bda3-faf0e4f5ae6e"
    HEARING_ADJOURNED_EMAIL_TEMPLATE_ID = "07b9738d-d6ba-4736-86f6-7102776f57a2"
    HEARING_ADJOURNED_SMS_TEMPLATE_ID = "31d9ff36-ca22-4a22-b794-c53904f614c4"
    HEARING_POSTPONED_EMAIL_TEMPLATE_ID = "08959288-e09a-472d-80b8-af79bfcbb437"
    APPEAL_LAPSED_EMAIL_TEMPLATE_ID = "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11"
    APPEAL_WITHDRAWN_EMAIL_TEMPLATE_ID = "8620e023-f663-477e-a771-9cfad50ee30f"
    SUBSCRIPTION_CREATED_SMS_TEMPLATE_ID = "18444f5f-8834-49e9-a6ae-bfe7f50db2b8"
    HEARING_BOOKED_EMAIL_TEMPLATE_ID = "fee16753-0bdb-43f1-9abb-b14b826e3b26"
    EMAIL_MAC_SECRET_TEXT = "${data.vault_generic_secret.mac_secret.data["value"]}"
  }
}
