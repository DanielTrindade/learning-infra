resource "kubernetes_namespace" "mirante" {
  metadata {
    name = "learning-infra-iac-14"
  }
}

resource "kubernetes_config_map_v1" "config" {
  metadata {
    name      = "mirante-config"
    namespace = kubernetes_namespace.mirante.metadata[0].name
  }

  data = {
    mensagem = "v1"
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

          volume_mount {
            name       = "config"
            mount_path = "/etc/mirante"
            read_only  = true
          }
        }

        volume {
          name = "config"

          config_map {
            name = kubernetes_config_map_v1.config.metadata[0].name
          }
        }
      }
    }
  }
}
