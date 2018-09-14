resource "azurerm_key_vault_secret" "idam-url" {
  name      = "idam-url"
  value     = "${data.azurerm_key_vault_secret.idam-api.value}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "s2s-api" {
  name      = "s2s-api"
  value     = "${local.s2sCnpUrl}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "s2s-auth" {
  name      = "s2s-auth"
  value     = "${data.azurerm_key_vault_secret.sscs-s2s-secret.value}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "sysupdate-user" {
  name      = "sysupdate-user"
  value     = "${data.azurerm_key_vault_secret.idam-sscs-systemupdate-user.value}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "sysupdate-pass" {
  name      = "sysupdate-pass"
  value     = "${data.azurerm_key_vault_secret.idam-sscs-systemupdate-password.value}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "notif-api" {
  name      = "notif-api"
  value     = "${data.azurerm_key_vault_secret.sscs-notify-api-key.value}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "notif-test-api" {
  name      = "notif-test-api"
  value     = "${data.azurerm_key_vault_secret.sscs-notify-api-test-key.value}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "mac-secret" {
  name      = "mac-secret"
  value     = "${data.azurerm_key_vault_secret.email-mac-secret.value}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "idam-oauth-secret" {
  name      = "idam-oauth-secret"
  value     = "${data.azurerm_key_vault_secret.idam-sscs-oauth2-client-secret.value}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "idam-oauth-user" {
  name      = "idam-oauth-user"
  value     = "${var.idam_oauth2_client_id}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "ccd-api" {
  name      = "ccd-api"
  value     = "${local.ccdApi}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "ccd-jid" {
  name      = "ccd-jid"
  value     = "${var.core_case_data_jurisdiction_id}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "ccd-tid" {
  name      = "ccd-tid"
  value     = "${var.core_case_data_case_type_id}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "s2s-micro" {
  name      = "s2s-micro"
  value     = "${var.idam_s2s_auth_microservice}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "idam-redirect" {
  name      = "idam-redirect"
  value     = "${var.idam_redirect_url}"
  vault_uri = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}
