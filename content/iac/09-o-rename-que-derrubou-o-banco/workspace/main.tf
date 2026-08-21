resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_volume" "dados" {
  name = "mirante-estoque"
}

resource "docker_container" "banco" {
  name  = "mirante-banco"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = 8079
  }

  volumes {
    volume_name    = docker_volume.dados.name
    container_path = "/var/lib/mirante"
  }
}
