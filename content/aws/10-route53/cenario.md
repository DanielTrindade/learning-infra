---
id: aws/10-route53
titulo: O registro existe, mas seu computador não o resolve
dificuldade: autônomo
ministack: true
---
# O registro existe, mas seu computador não o resolve

Crie uma hosted zone privada `learning.internal.` associada a uma VPC local e um registro
`app.learning.internal.` do tipo A com valor `10.50.10.25` e TTL 60.

## Método

1. Crie a VPC e capture `VpcId`.
2. Use `route53 create-hosted-zone` com `--hosted-zone-config PrivateZone=true` e VPC.
3. Capture o ID da zona.
4. Aplique `change-resource-record-sets` usando `mudanca.json`.
5. Inspecione com `list-resource-record-sets`.

O parâmetro `--caller-reference` deve ser único e estável para a tentativa; ele oferece
idempotência na API de criação.

## A armadilha

`Resolve-DnsName app.learning.internal` no host não é um teste válido: o MiniStack não
altera o resolver do Windows. Health checks e DNSSEC também são metadata. A evidência
correta aqui é o record set no plano de controle.

