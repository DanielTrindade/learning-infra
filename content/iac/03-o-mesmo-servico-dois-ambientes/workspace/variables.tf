variable "ambiente" {
  type        = string
  description = "nome do ambiente — aparece no nome do container"

  validation {
    condition     = contains(["homologacao", "producao"], var.ambiente)
    error_message = "ambiente precisa ser homologacao ou producao."
  }
}

variable "porta" {
  type        = number
  description = "porta publicada no host"

  validation {
    condition     = var.porta >= 8070 && var.porta <= 8079
    error_message = "a porta precisa estar no bloco 8070-8079 reservado para esta Trilha."
  }
}
