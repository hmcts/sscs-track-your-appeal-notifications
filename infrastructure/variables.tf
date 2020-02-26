variable "product" {
  type = "string"
}

variable "component" {
  type = "string"
}

variable "location" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "ilbIp" {}

variable "subscription" {
  type = "string"
}

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  type = "string"
}

variable "postgresql_user" {
  default = "notification"
}

variable "database_name" {
  default = "notification"
}

variable "common_tags" {
  type = "map"
}

variable "appinsights_instrumentation_key" {
  description = "Instrumentation key of the App Insights instance this webapp should use. Module will create own App Insights resource if this is not provided"
  default     = ""
}