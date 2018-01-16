variable "product" {
  type    = "string"
  default = "sscs-track-your-appeal"
}

variable "location" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "infrastructure_env" {
  default     = "dev"
  description = "Infrastructure environment to point to"
}

variable "tya_notfications_server_port" {
  type    = "string"
  default = "8081"
}


variable "management_security_enabled" {
  type    = "string"
  default = "true"
}


