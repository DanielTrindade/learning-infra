variable "senha_do_painel" {
  type        = string
  description = "senha do painel administrativo — de exemplo, nunca uma senha real"
  sensitive   = true
  default     = "senha-de-exemplo-nao-use"
}
