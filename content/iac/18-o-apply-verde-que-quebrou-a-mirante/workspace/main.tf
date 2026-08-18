resource "docker_volume" "estoque" {
  name = "mirante-estoque"
}

resource "kubernetes_namespace" "mirante" {
  metadata {
    name = "learning-infra-iac-18"
  }
}

resource "kubernetes_deployment_v1" "web" {
  metadata {
    name      = "mirante-web"
    namespace = kubernetes_namespace.mirante.metadata[0].name
  }

  spec {
    replicas = 3

    selector {
      match_labels = {
        app = "mirante-web"
      }
    }

    template {
      metadata {
        labels = {
          app = "mirante-web"
        }
      }

      spec {
        container {
          name  = "web"
          image = "nginx:1.27-alpine"

          port {
            container_port = 80
          }
        }
      }
    }
  }
}

resource "aws_sqs_queue" "pedidos" {
  name = "mirante-pedidos"
}

module "ambiente" {
  for_each = {
    homologacao = 8072
    producao    = 8071
  }

  source   = "./modules/ambiente"
  ambiente = each.key
  porta    = var.portas[each.key]
  volume   = docker_volume.estoque.name
}
