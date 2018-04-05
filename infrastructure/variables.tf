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

variable "evidence_submission_info_link" {
  type = "string"
  default = "http://localhost:3000/evidence/appeal_id"
}

variable "sscs_manage_emails_link" {
  type = "string"
  default = "http://localhost:3000/manage-email-notifications/mac"
}

variable "sscs_track_your_appeal_link" {
  type = "string"
  default = "http://localhost:3000/trackyourappeal/appeal_id"
}

variable "hearing_info_link" {
  type = "string"
  default = "http://localhost:3000/abouthearing/appeal_id"
}

variable "claiming_expenses_link" {
  type = "string"
  default = "http://localhost:3000/expenses/appeal_id"
}

variable "job_scheduler_enabled" {
  type = "string"
  default = "false"
}

variable "job_scheduler_url" {
  type = "string"
  default = "http://localhost:8484"
}

variable "job_scheduler_callbackUrl" {
  type = "string"
  default = "http://localhost:8080"
}