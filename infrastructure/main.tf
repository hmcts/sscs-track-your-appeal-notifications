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
  source   = "git@github.com:contino/moj-module-webapp?ref=master"
  product  = "${var.product}-notif"
  location = "${var.location}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"

  app_settings = {
    S2S_URL = "${data.vault_generic_secret.s2s_url.data["value"]}"
    MANAGEMENT_SECURITY_ENABLED = "${var.management_security_enabled}"
    NOTIFICATION_API_KEY = "${data.vault_generic_secret.sscs_notify_api_key.data["value"]}"
  }
}
