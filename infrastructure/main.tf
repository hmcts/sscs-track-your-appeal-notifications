provider "vault" {
  //  # It is strongly recommended to configure this provider through the
  //  # environment variables described above, so that each user can have
  //  # separate credentials set in the environment.
  //  #
  //  # This will default to using $VAULT_ADDR
  //  # But can be set explicitly
  address = "https://vault.reform.hmcts.net:6200"
}

data "vault_generic_secret" "sscs_notify_api_key" {
  path = "secret/test/sscs/sscs_notify_api_new_key"
}



module "track-your-appeal-notifications" {
  source   = "git@github.com:contino/moj-module-webapp?ref=master"
  product  = "${var.product}-notifications"
  location = "${var.location}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"

  app_settings = {
    S2S_URL = "${var.s2s-url}"
    MANAGEMENT_SECURITY_ENABLED = "${var.management_security_enabled}"
    NOTIFICATION_API_KEY = "${data.vault_generic_secret.sscs_notify_api_key.data["value"]}"
  }
}
