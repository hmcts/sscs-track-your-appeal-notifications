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

variable "infrastructure_env" {
  default     = "test"
  description = "Infrastructure environment to point to"
}

variable "ilbIp" {}

variable "subscription" {
  type = "string"
}

variable "core_case_data_jurisdiction_id" {
  default = "SSCS"
}

variable "core_case_data_case_type_id" {
  default = "Benefit"
}

variable "idam_s2s_auth_microservice" {
  default = "sscs"
}

variable "idam_oauth2_client_id" {
  default = "sscs"
}

variable "evidence_submission_info_link" {
  type    = "string"
  default = "http://localhost:3000/evidence/appeal_id"
}

variable "sscs_manage_emails_link" {
  type    = "string"
  default = "http://localhost:3000/manage-email-notifications/mac"
}

variable "sscs_track_your_appeal_link" {
  type    = "string"
  default = "http://localhost:3000/trackyourappeal/appeal_id"
}

variable "hearing_info_link" {
  type    = "string"
  default = "http://localhost:3000/abouthearing/appeal_id"
}

variable "claiming_expenses_link" {
  type    = "string"
  default = "http://localhost:3000/expenses/appeal_id"
}

variable "online_hearing_link" {
  type    = "string"
  default = "http://localhost:8090"
}

variable "job_scheduler_poll_interval" {
  type    = "string"
  default = "30000"
}

variable "idam_redirect_url" {
  default = "https://sscs-case-loader-sandbox.service.core-compute-sandbox.internal"
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

variable "hours_start_time" {
  type = "string"
  default = "9"
}

variable "hours_end_time" {
  type = "string"
  default = "17"
}

variable "bundled_letters_on" {
  type = "string"
  default = false
}

variable "letters_on" {
  type = "string"
  default = false
}
