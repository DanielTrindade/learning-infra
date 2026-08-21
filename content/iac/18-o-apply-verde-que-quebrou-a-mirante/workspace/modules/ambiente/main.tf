resource "docker_container" "web" {
  name  = "mirante-${var.ambiente}"
  image = "nginx:1.27-alpine"

  ports {
    internal = 80
    external = var.porta
  }

  volumes {
    volume_name    = var.volume
    container_path = "/var/lib/mirante"
  }
}
