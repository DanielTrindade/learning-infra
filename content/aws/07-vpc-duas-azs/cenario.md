---
id: aws/07-vpc-duas-azs
titulo: Uma VPC organizada em duas zonas de disponibilidade
dificuldade: assistido
ministack: true
---
# Uma VPC organizada em duas zonas de disponibilidade

Modele uma VPC `10.20.0.0/16` com quatro subnets sem sobreposição:

- pública A: `10.20.0.0/24`, em `us-east-1a`;
- privada A: `10.20.10.0/24`, em `us-east-1a`;
- pública B: `10.20.1.0/24`, em `us-east-1b`;
- privada B: `10.20.11.0/24`, em `us-east-1b`.

O `rede.yaml` contém a VPC e duas subnets. Complete as duas restantes e mantenha tags
que expressem papel e zona.

```powershell
$endpoint = "http://127.0.0.1:4566"
aws --endpoint-url $endpoint cloudformation deploy `
  --stack-name learning-infra-vpc --template-file rede.yaml

aws --endpoint-url $endpoint ec2 describe-vpcs `
  --filters Name=tag:Name,Values=learning-infra-vpc
aws --endpoint-url $endpoint ec2 describe-subnets `
  --filters Name=tag:Projeto,Values=learning-infra
```

## Plano de controle, não rede real

O MiniStack valida objetos, CIDRs, tags e relacionamentos. Ele não cria interfaces nem
encaminha pacotes. Asserções deste Cenário provam a topologia declarada — não conectividade
entre subnets nem tolerância a falha de uma AZ.

