variable "ambiente" {
  type        = string
  description = "nome do ambiente"
  default     = "homologacao"

  validation {
    condition     = contains(["homologacao", "producao"], var.ambiente)
    error_message = "ambiente precisa ser homologacao ou producao."
  }
}

variable "porta" {
  type        = number
  description = "porta publicada no host"
  default     = 8071
}
