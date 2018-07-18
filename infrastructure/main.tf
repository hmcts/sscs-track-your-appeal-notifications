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
  path = "secret/${var.infrastructure_env}/sscs/${var.notification_key}"
}

data "vault_generic_secret" "sscs_notify_api_test_key" {
  path = "secret/${var.infrastructure_env}/sscs/${var.notification_test_key}"
}

data "vault_generic_secret" "mac_secret" {
  path = "secret/${var.infrastructure_env}/sscs/sscs_email_mac_secret_text"
}

locals {
  aseName = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"

  previewVaultName       = "${var.product}-${var.component}"
  nonPreviewVaultName    = "${var.product}-${var.component}-${var.env}"
  vaultName              = "${(var.env == "preview") ? local.previewVaultName : local.nonPreviewVaultName}"

  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"
  local_ase = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "core-compute-aat" : "core-compute-saat" : local.aseName}"

  ccdApi = "http://ccd-data-store-api-${local.local_env}.service.${local.local_ase}.internal"
  s2sCnpUrl = "http://rpe-service-auth-provider-${local.local_env}.service.${local.local_ase}.internal"
}

module "track-your-appeal-notifications" {
  source       = "git@github.com:contino/moj-module-webapp?ref=master"
  product      = "${var.product}-${var.component}"
  location     = "${var.location}"
  env          = "${var.env}"
  ilbIp        = "${var.ilbIp}"
  is_frontend  = false
  subscription = "${var.subscription}"
  capacity     = "${(var.env == "preview") ? 1 : 2}"
  common_tags = "${var.common_tags}"

  app_settings = {
    INFRASTRUCTURE_ENV = "${var.env}"
    MANAGEMENT_SECURITY_ENABLED = "${var.management_security_enabled}"

    CORE_CASE_DATA_API_URL = "${local.ccdApi}"
    CORE_CASE_DATA_JURISDICTION_ID = "${var.core_case_data_jurisdiction_id}"
    CORE_CASE_DATA_CASE_TYPE_ID = "${var.core_case_data_case_type_id}"

    IDAM_URL = "${data.vault_generic_secret.idam_api.data["value"]}"

    IDAM.S2S-AUTH.TOTP_SECRET = "${data.vault_generic_secret.sscs_s2s_secret.data["value"]}"
    IDAM.S2S-AUTH = "${local.s2sCnpUrl}"
    IDAM.S2S-AUTH.MICROSERVICE = "${var.idam_s2s_auth_microservice}"

    IDAM_SSCS_SYSTEMUPDATE_USER = "${data.vault_generic_secret.idam_sscs_systemupdate_user.data["value"]}"
    IDAM_SSCS_SYSTEMUPDATE_PASSWORD = "${data.vault_generic_secret.idam_sscs_systemupdate_password.data["value"]}"

    IDAM_OAUTH2_CLIENT_ID = "${var.idam_oauth2_client_id}"
    IDAM_OAUTH2_CLIENT_SECRET = "${data.vault_generic_secret.idam_oauth2_client_secret.data["value"]}"
    IDAM_OAUTH2_REDIRECT_URL = "${var.idam_redirect_url}"

    NOTIFICATION_API_KEY = "${data.vault_generic_secret.sscs_notify_api_key.data["value"]}"
    NOTIFICATION_API_TEST_KEY = "${data.vault_generic_secret.sscs_notify_api_test_key.data["value"]}"
    EVIDENCE_SUBMISSION_INFO_LINK = "${var.evidence_submission_info_link}"
    SSCS_MANAGE_EMAILS_LINK = "${var.sscs_manage_emails_link}"
    SSCS_TRACK_YOUR_APPEAL_LINK = "${var.sscs_track_your_appeal_link}"
    HEARING_INFO_LINK = "${var.hearing_info_link}"
    CLAIMING_EXPENSES_LINK = "${var.claiming_expenses_link}"
    JOB_SCHEDULER_POLL_INTERVAL = "${var.job_scheduler_poll_interval}"
    EMAIL_MAC_SECRET_TEXT = "${data.vault_generic_secret.mac_secret.data["value"]}"

    // db vars
    JOB_SCHEDULER_DB_HOST     = "${module.db-notif.host_name}"
    JOB_SCHEDULER_DB_PORT     = "${module.db-notif.postgresql_listen_port}"
    JOB_SCHEDULER_DB_PASSWORD = "${module.db-notif.postgresql_password}"
    JOB_SCHEDULER_DB_USERNAME = "${module.db-notif.user_name}"
    JOB_SCHEDULER_DB_NAME     = "${module.db-notif.postgresql_database}"
    JOB_SCHEDULER_DB_CONNECTION_OPTIONS = "?ssl"
    MAX_ACTIVE_DB_CONNECTIONS = 70
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
  source          = "git@github.com:hmcts/moj-module-postgres?ref=master"
  product         = "${var.product}-${var.component}-postgres-db"
  location        = "${var.location}"
  env             = "${var.env}"
  postgresql_user = "${var.postgresql_user}"
  database_name   = "${var.database_name}"
  common_tags     = "${var.common_tags}"
}
