module "track-your-appeal-notifications" {
  source   = "git@github.com:contino/moj-module-webapp?ref=0.0.78"
  product  = "${var.product}-notifications"
  location = "${var.location}"
  env      = "${var.env}"
  asename  = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"

  app_settings = {
    PORT="${var.tya_notfications_server_port}"
    MANAGEMENT_SECURITY_ENABLED="${var.management_security_enabled}"
  }
}
