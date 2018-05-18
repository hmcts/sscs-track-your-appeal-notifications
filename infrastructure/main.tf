provider "vault" {
  address = "https://vault.reform.hmcts.net:6200"
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location}"
}

data "vault_generic_secret" "sscs_s2s_secret" {
  path = "secret/${var.infrastructure_env}/ccidam/service-auth-provider/api/microservice-keys/sscs"
}

data "vault_generic_secret" "idam_sscs_systemupdate_user" {
  path = "secret/${var.infrastructure_env}/ccidam/idam-api/sscs/systemupdate/user"
}

data "vault_generic_secret" "idam_sscs_systemupdate_password" {
  path = "secret/${var.infrastructure_env}/ccidam/idam-api/sscs/systemupdate/password"
}

data "vault_generic_secret" "idam_oauth2_client_secret" {
  path = "secret/${var.infrastructure_env}/ccidam/idam-api/oauth2/client-secrets/sscs"
}

data "vault_generic_secret" "idam_api" {
  path = "secret/${var.infrastructure_env}/sscs/idam_api"
}

data "vault_generic_secret" "idam_s2s_api" {
  path = "secret/${var.infrastructure_env}/sscs/idam_s2s_api"
}

data "vault_generic_secret" "sscs_notify_api_key" {
  path = "secret/${var.infrastructure_env}/sscs/sscs_notify_api_new_key"
}

data "vault_generic_secret" "mac_secret" {
  path = "secret/${var.infrastructure_env}/sscs/sscs_email_mac_secret_text"
}

locals {
  aseName = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"

  localCcdApi = "http://ccd-data-store-api-${var.env}.service.${local.aseName}.internal"
  CcdApi = "${var.env == "preview" ? "http://ccd-data-store-api-aat.service.core-compute-aat.internal" : local.localCcdApi}"

  previewVaultName       = "${var.product}-${var.component}"
  nonPreviewVaultName    = "${var.product}-${var.component}-${var.env}"
  vaultName              = "${(var.env == "preview") ? local.previewVaultName : local.nonPreviewVaultName}"
}

module "track-your-appeal-notifications" {
  source       = "git@github.com:contino/moj-module-webapp?ref=master"
  product      = "${var.product}-${var.component}"
  location     = "${var.location}"
  env          = "${var.env}"
  ilbIp        = "${var.ilbIp}"
  is_frontend  = false
  subscription = "${var.subscription}"


  app_settings = {
    POSTGRES_HOST = "${module.db-notif.host_name}"
    POSTGRES_PORT = "${module.db-notif.postgresql_listen_port}"
    POSTGRES_DATABASE = "${module.db-notif.postgresql_database}"
    POSTGRES_USER = "${module.db-notif.user_name}"
    POSTGRES_PASSWORD = "${module.db-notif.postgresql_password}"
    MAX_ACTIVE_DB_CONNECTIONS = 70

    MANAGEMENT_SECURITY_ENABLED = "${var.management_security_enabled}"

    CORE_CASE_DATA_API_URL = "${local.CcdApi}"
    CORE_CASE_DATA_USER_ID = "${var.core_case_data_user_id}"
    CORE_CASE_DATA_JURISDICTION_ID = "${var.core_case_data_jurisdiction_id}"
    CORE_CASE_DATA_CASE_TYPE_ID = "${var.core_case_data_case_type_id}"

    IDAM_URL = "${data.vault_generic_secret.idam_api.data["value"]}"

    IDAM.S2S-AUTH.TOTP_SECRET = "${data.vault_generic_secret.sscs_s2s_secret.data["value"]}"
    IDAM.S2S-AUTH = "${data.vault_generic_secret.idam_s2s_api.data["value"]}"
    IDAM.S2S-AUTH.MICROSERVICE = "${var.idam_s2s_auth_microservice}"

    IDAM_SSCS_SYSTEMUPDATE_USER = "${data.vault_generic_secret.idam_sscs_systemupdate_user.data["value"]}"
    IDAM_SSCS_SYSTEMUPDATE_PASSWORD = "${data.vault_generic_secret.idam_sscs_systemupdate_password.data["value"]}"

    IDAM_OAUTH2_CLIENT_ID = "${var.idam_oauth2_client_id}"
    IDAM_OAUTH2_CLIENT_SECRET = "${data.vault_generic_secret.idam_oauth2_client_secret.data["value"]}"
    IDAM_OAUTH2_REDIRECT_URL = "${var.idam_redirect_url}"

    NOTIFICATION_API_KEY = "${data.vault_generic_secret.sscs_notify_api_key.data["value"]}"
    EVIDENCE_SUBMISSION_INFO_LINK = "${var.evidence_submission_info_link}"
    SSCS_MANAGE_EMAILS_LINK = "${var.sscs_manage_emails_link}"
    SSCS_TRACK_YOUR_APPEAL_LINK = "${var.sscs_track_your_appeal_link}"
    HEARING_INFO_LINK = "${var.hearing_info_link}"
    CLAIMING_EXPENSES_LINK = "${var.claiming_expenses_link}"
    JOB_SCHEDULER_ENABLED = "${var.job_scheduler_enabled}"
    JOB_SCHEDULER_URL = "${var.job_scheduler_url}"
    JOB_SCHEDULER_CALLBACK_URL = "${var.job_scheduler_callbackUrl}"
    APPEAL_RECEIVED_EMAIL_TEMPLATE_ID = "b90df52f-c628-409c-8875-4b0b9663a053"
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
    SYA_APPEAL_CREATED_EMAIL_TEMPLATE_ID = "01293b93-b23e-40a3-ad78-2c6cd01cd21c"
    EMAIL_MAC_SECRET_TEXT = "${data.vault_generic_secret.mac_secret.data["value"]}"
  }
}

module "sscs-tya-notif-key-vault" {
  source              = "git@github.com:hmcts/moj-module-key-vault?ref=master"
  name                = "${local.vaultName}"
  product             = "${var.product}"
  env                 = "${var.env}"
  tenant_id           = "${var.tenant_id}"
  object_id           = "${var.jenkins_AAD_objectId}"
  resource_group_name = "${azurerm_resource_group.rg.name}"
  product_group_object_id = "300e771f-856c-45cc-b899-40d78281e9c1"
}

module "db-notif" {
  source          = "git@github.com:hmcts/moj-module-postgres?ref=cnp-449-tactical"
  product         = "${var.product}-${var.component}-postgres-db"
  location        = "${var.location}"
  env             = "${var.env}"
  postgresql_user = "${var.postgresql_user}"
  database_name   = "${var.database_name}"
}
