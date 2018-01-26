module "track-your-appeal-notifications" {
  source   = "git@github.com:contino/moj-module-webapp?ref=master"
  product  = "${var.product}-notifications"
  location = "${var.location}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"

  app_settings = {
    MANAGEMENT_SECURITY_ENABLED = "${var.management_security_enabled}"
  }
}
