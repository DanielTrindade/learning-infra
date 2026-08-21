resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name    = "mirante-web"
  image   = docker_image.web.image_id
  restart = "no"

  ports {
    internal = 80
    external = 8071
  }
}
