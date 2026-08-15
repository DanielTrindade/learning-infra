variable "porta" {
  type    = number
  default = 8072
}

resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name  = "mirante-web"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = var.porta
  }

  lifecycle {
    precondition {
      condition     = var.porta >= 8070 && var.porta <= 8079
      error_message = "a porta precisa estar no bloco 8070-8079 reservado para esta Trilha."
    }
  }
}
