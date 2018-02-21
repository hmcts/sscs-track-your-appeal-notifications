variable "product" {
  type    = "string"
  default = "track-your-appeal-notifications"
}

variable "location" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "infrastructure_env" {
  default     = "test"
  description = "Infrastructure environment to point to"
}

variable "management_security_enabled" {
  type    = "string"
  default = "true"
}

variable "ilbIp"{}

variable "subscription" {
  type = "string"
}
