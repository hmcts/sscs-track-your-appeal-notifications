provider "azurerm" {
  features {}
}

data "azurerm_key_vault" "sscs_key_vault" {
  name = "${local.azureVaultName}"
  resource_group_name = "${local.azureVaultName}"
}

resource "azurerm_key_vault_secret" "notification_job_scheduler_db_password" {
  name         = "notification-job-scheduler-db-password"
  value        = "${module.db-notif.postgresql_password}"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

locals {
  azureVaultName = "sscs-${var.env}"
}



module "db-notif" {
  source          = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product         = "${var.product}-${var.component}-postgres-db"
  location        = "${var.location}"
  env             = "${var.env}"
  postgresql_user = "${var.postgresql_user}"
  database_name   = "${var.database_name}"
  common_tags     = "${var.common_tags}"
  subscription          = "${var.subscription}"
}
