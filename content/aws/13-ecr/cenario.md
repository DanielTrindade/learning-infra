---
id: aws/13-ecr
titulo: Uma imagem imutável do build ao registry
dificuldade: assistido
ministack: true
---
# Uma imagem imutável do build ao registry

Construa a imagem `learning-infra-api:1.0`, crie um repositório ECR com tags imutáveis e
publique a imagem no registry local do MiniStack.

## Repositório e autenticação

```powershell
$endpoint = "http://127.0.0.1:4566"
aws --endpoint-url $endpoint ecr create-repository `
  --repository-name learning-infra-api `
  --image-tag-mutability IMMUTABLE `
  --image-scanning-configuration scanOnPush=true

aws --endpoint-url $endpoint ecr get-login-password |
  docker login --username AWS --password-stdin 127.0.0.1:4566
```

Faça build, tag e push:

```powershell
docker build -t learning-infra-api:1.0 .
docker tag learning-infra-api:1.0 127.0.0.1:4566/learning-infra-api:1.0
docker push 127.0.0.1:4566/learning-infra-api:1.0
aws --endpoint-url $endpoint ecr describe-images --repository-name learning-infra-api
```

Tente publicar conteúdo diferente com a mesma tag. Imutabilidade transforma a tag em
identidade confiável para deploy; uma tag `latest` mutável esconde qual artefato rodou.

## Limite

Push/pull e manifests são reais no registry local. O token é fixo e o scanner retorna
achados vazios: ausência de findings no MiniStack não prova que a imagem é segura.

