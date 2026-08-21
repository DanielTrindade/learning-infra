locals {
  nome_do_container = "mirante-${var.ambiente}"
}

resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name  = local.nome_do_container
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = var.porta
  }
}

output "endereco" {
  value       = "http://localhost:${var.porta}"
  description = "onde o serviço deste ambiente responde"
}
