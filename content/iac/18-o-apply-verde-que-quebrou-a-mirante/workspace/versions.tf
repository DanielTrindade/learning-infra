terraform {
  required_version = "= 1.15.8"

  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 4.5"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 3.2"
    }
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.58"
    }
  }
}

provider "docker" {}

provider "kubernetes" {
  config_path    = "~/.kube/config"
  config_context = "docker-desktop"
}

provider "aws" {
  region                      = "us-east-1"
  access_key                  = "local"
  secret_key                  = "local"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    s3       = "http://127.0.0.1:4566"
    sqs      = "http://127.0.0.1:4566"
    dynamodb = "http://127.0.0.1:4566"
  }
}
