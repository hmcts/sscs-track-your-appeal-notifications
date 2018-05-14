output "vaultUri" {
  value = "${module.sscs-tya-notif-key-vault.key_vault_uri}"
}

output "vaultName" {
  value = "${local.vaultName}"
}

output "microserviceName" {
  value = "${var.component}"
}
