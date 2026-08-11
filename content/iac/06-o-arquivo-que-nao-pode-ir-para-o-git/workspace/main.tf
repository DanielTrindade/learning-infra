resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name  = "mirante-painel"
  image = docker_image.web.image_id

  env = [
    "SENHA_DO_PAINEL=${var.senha_do_painel}",
  ]

  ports {
    internal = 80
    external = 8076
  }
}

output "senha_em_uso" {
  value     = var.senha_do_painel
  sensitive = true
}
