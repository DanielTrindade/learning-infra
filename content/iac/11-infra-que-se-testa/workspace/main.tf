resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name  = "mirante-${var.ambiente}"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = var.porta
  }
}

output "nome_do_container" {
  value = docker_container.web.name
}
