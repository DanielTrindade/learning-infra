resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "homologacao" {
  name  = "mirante-homologacao"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = 8077
  }
}

resource "docker_container" "producao" {
  name  = "mirante-producao"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = 8078
  }
}
