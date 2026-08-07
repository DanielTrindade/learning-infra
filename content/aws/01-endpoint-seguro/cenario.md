---
id: aws/01-endpoint-seguro
titulo: Uma conta local que não pode virar produção
dificuldade: guiado
ministack: true
---
# Uma conta local que não pode virar produção

O primeiro risco de um laboratório AWS não é técnico: é executar um comando destrutivo
na conta errada. Neste Cenário você prova o endpoint, a região e a identidade antes de
criar qualquer recurso.

## Configure uma sessão descartável

No PowerShell usado para os exercícios:

```powershell
$endpoint = "http://127.0.0.1:4566"
$env:AWS_ACCESS_KEY_ID = "000000000000"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

aws --endpoint-url $endpoint sts get-caller-identity
```

O endpoint explícito é obrigatório mesmo com as variáveis configuradas. Ele é o
guardrail visível que impede o comando de alcançar `amazonaws.com`.

Crie um bucket-marcador e confirme onde ele existe:

```powershell
aws --endpoint-url $endpoint s3api create-bucket --bucket learning-infra-identidade
aws --endpoint-url $endpoint s3api list-buckets
aws --endpoint-url $endpoint s3api get-bucket-location --bucket learning-infra-identidade
```

Em `us-east-1`, a localização do bucket pode aparecer como `None`. Isso não significa
que o bucket não exista; é uma peculiaridade histórica da API.

## O que foi provado

Você provou que a AWS CLI está falando com o MiniStack e que o recurso foi criado na
conta efêmera. Ainda não provou credenciais, IAM nem isolamento de uma conta AWS real.

