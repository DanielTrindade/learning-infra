resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_network" "interna" {
  name = "mirante-interna"
}

resource "docker_volume" "conteudo" {
  name = "mirante-conteudo"
}

resource "docker_container" "web" {
  name  = "mirante-web"
  image = "nginx:1.27-alpine"

  ports {
    internal = 80
    external = 8074
  }
}
