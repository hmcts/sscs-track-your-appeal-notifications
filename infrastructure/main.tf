resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location}"

  tags = "${merge(var.common_tags,
    map("lastUpdated", "${timestamp()}")
    )}"
}

data "azurerm_key_vault" "sscs_key_vault" {
  name = "${local.azureVaultName}"
  resource_group_name = "${local.azureVaultName}"
}

data "azurerm_key_vault_secret" "sscs-s2s-secret" {
  name = "sscs-s2s-secret"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam-sscs-systemupdate-user" {
  name = "idam-sscs-systemupdate-user"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam-sscs-systemupdate-password" {
  name = "idam-sscs-systemupdate-password"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam-sscs-oauth2-client-secret" {
  name = "idam-sscs-oauth2-client-secret"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam-api" {
  name = "idam-api"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam-s2s-api" {
  name = "idam-s2s-api"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "sscs-notify-api-key" {
  name = "notification-key"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "sscs-notify-api-test-key" {
  name = "notification-test-key"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "email-mac-secret" {
  name = "sscs-email-mac-secret-text"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

locals {
  local_ase = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"

  ccdApi    = "http://ccd-data-store-api-${var.env}.service.${local.local_ase}.internal"
  s2sCnpUrl = "http://rpe-service-auth-provider-${var.env}.service.${local.local_ase}.internal"
  cohApi    = "http://coh-cor-${var.env}.service.${local.local_ase}.internal"
  documentStore = "http://dm-store-${var.env}.service.${local.local_ase}.internal"
  pdfService    = "http://cmc-pdf-service-${var.env}.service.${local.local_ase}.internal"

  azureVaultName = "sscs-${var.env}"
}

module "track-your-appeal-notifications" {
  source       = "git@github.com:contino/moj-module-webapp?ref=master"
  product      = "${var.product}-${var.component}"
  location     = "${var.location}"
  env          = "${var.env}"
  ilbIp        = "${var.ilbIp}"
  is_frontend  = false
  subscription = "${var.subscription}"
  capacity     = 2
  common_tags  = "${var.common_tags}"
  asp_rg       = "${var.product}-${var.component}-${var.env}"
  asp_name     = "${var.product}-${var.component}-${var.env}"

  app_settings = {
    INFRASTRUCTURE_ENV          = "${var.env}"

    CORE_CASE_DATA_API_URL         = "${local.ccdApi}"
    CORE_CASE_DATA_JURISDICTION_ID = "${var.core_case_data_jurisdiction_id}"
    CORE_CASE_DATA_CASE_TYPE_ID    = "${var.core_case_data_case_type_id}"

    COH_URL = "${local.cohApi}"

    IDAM_URL = "${data.azurerm_key_vault_secret.idam-api.value}"

    IDAM.S2S-AUTH.TOTP_SECRET  = "${data.azurerm_key_vault_secret.sscs-s2s-secret.value}"
    IDAM.S2S-AUTH              = "${local.s2sCnpUrl}"
    IDAM.S2S-AUTH.MICROSERVICE = "${var.idam_s2s_auth_microservice}"

    IDAM_SSCS_SYSTEMUPDATE_USER = "${data.azurerm_key_vault_secret.idam-sscs-systemupdate-user.value}"
    IDAM_SSCS_SYSTEMUPDATE_PASSWORD = "${data.azurerm_key_vault_secret.idam-sscs-systemupdate-password.value}"

    IDAM_OAUTH2_CLIENT_ID     = "${var.idam_oauth2_client_id}"
    IDAM_OAUTH2_CLIENT_SECRET = "${data.azurerm_key_vault_secret.idam-sscs-oauth2-client-secret.value}"
    IDAM_OAUTH2_REDIRECT_URL  = "${var.idam_redirect_url}"

    NOTIFICATION_API_KEY          = "${data.azurerm_key_vault_secret.sscs-notify-api-key.value}"
    NOTIFICATION_API_TEST_KEY     = "${data.azurerm_key_vault_secret.sscs-notify-api-test-key.value}"
    EVIDENCE_SUBMISSION_INFO_LINK = "${var.evidence_submission_info_link}"
    SSCS_MANAGE_EMAILS_LINK       = "${var.sscs_manage_emails_link}"
    SSCS_TRACK_YOUR_APPEAL_LINK   = "${var.sscs_track_your_appeal_link}"
    HEARING_INFO_LINK             = "${var.hearing_info_link}"
    CLAIMING_EXPENSES_LINK        = "${var.claiming_expenses_link}"
    JOB_SCHEDULER_POLL_INTERVAL   = "${var.job_scheduler_poll_interval}"
    EMAIL_MAC_SECRET_TEXT         = "${data.azurerm_key_vault_secret.email-mac-secret.value}"
    ONLINE_HEARING_LINK           = "${var.online_hearing_link}"

    PDF_API_URL                   = "${local.pdfService}"

    // db vars
    JOB_SCHEDULER_DB_HOST               = "${module.db-notif.host_name}"
    JOB_SCHEDULER_DB_PORT               = "${module.db-notif.postgresql_listen_port}"
    JOB_SCHEDULER_DB_PASSWORD           = "${module.db-notif.postgresql_password}"
    JOB_SCHEDULER_DB_USERNAME           = "${module.db-notif.user_name}"
    JOB_SCHEDULER_DB_NAME               = "${module.db-notif.postgresql_database}"
    JOB_SCHEDULER_DB_CONNECTION_OPTIONS = "?ssl"
    MAX_ACTIVE_DB_CONNECTIONS           = 70

    HOURS_START_TIME                    = "${var.hours_start_time}"
    HOURS_END_TIME                      = "${var.hours_end_time}"

    DOCUMENT_MANAGEMENT_URL = "${local.documentStore}"

    BUNDLED_LETTERS_ON                  = "${var.bundled_letters_on}"

    LETTERS_ON                          = "${var.letters_on}"
  }
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
