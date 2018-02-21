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

variable "management_security_enabled" {
  type    = "string"
  default = "true"
}

variable "s2s-url" {
  default = "http://betaDevBccidamS2SLB.reform.hmcts.net"
}

variable "ilbIp"{}

variable "subscription" {
  type = "string"
}
