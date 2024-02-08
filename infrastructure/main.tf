provider "azurerm" {
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "postgres_network"
  subscription_id            = var.aks_subscription_id
}

data "azurerm_key_vault" "sscs_key_vault" {
  name                = local.azureVaultName
  resource_group_name = local.azureVaultName
}

locals {
  azureVaultName = "sscs-${var.env}"
}



module "notification-scheduler-db-flexible" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }
  source = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  env    = var.env
  product       = var.product
  component     = var.component
  name          = "${var.product}-${var.component}-postgres-v15-db"
  business_area = "CFT" # sds or cft
  force_user_permissions_trigger = "1"
  # The original subnet is full, this is required to use the new subnet for new databases
  subnet_suffix = "expanded"
  pgsql_databases = [
    {
      name : var.database_name
    }
  ]
  pgsql_version = "15"
  # The ID of the principal to be granted admin access to the database server.
  # On Jenkins it will be injected for you automatically as jenkins_AAD_objectId.
  # Otherwise change the below:
  admin_user_object_id = var.jenkins_AAD_objectId
  common_tags = var.common_tags
  pgsql_server_configuration = [
    {
      name  = "azure.extensions"
      value = "plpgsql,pg_stat_statements,pg_buffercache,hypopg"
    }
  ]
  //Below attributes needs to be overridden for Perftest & Prod
  pgsql_sku            = var.pgsql_sku
  pgsql_storage_mb     = var.pgsql_storage_mb
}

# FlexibleServer v15 creds
resource "azurerm_key_vault_secret" "POSTGRES-USER-FLEX-V15" {
  name         = "${var.component}-POSTGRES-USER-FLEX-V15"
  value        = module.notification-scheduler-db-flexible.username
  key_vault_id = data.azurerm_key_vault.sscs_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS-FLEX-V15" {
  name         = "${var.component}-POSTGRES-PASS-FLEX-V15"
  value        = module.notification-scheduler-db-flexible.password
  key_vault_id = data.azurerm_key_vault.sscs_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST-FLEX-V15" {
  name         = "${var.component}-POSTGRES-HOST-FLEX-V15"
  value        = module.notification-scheduler-db-flexible.fqdn
  key_vault_id = data.azurerm_key_vault.sscs_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT-FLEX-V15" {
  name         = "${var.component}-POSTGRES-PORT-FLEX-V15"
  value        = "5432"
  key_vault_id = data.azurerm_key_vault.sscs_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE-FLEX-V15" {
  name         = "${var.component}-POSTGRES-DATABASE-FLEX-V15"
  value        = var.database_name
  key_vault_id = data.azurerm_key_vault.sscs_key_vault.id
}


