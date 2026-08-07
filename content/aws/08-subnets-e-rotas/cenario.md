---
id: aws/08-subnets-e-rotas
titulo: Pública não é um nome de subnet
dificuldade: autônomo
ministack: true
---
# Pública não é um nome de subnet

Uma subnet é pública quando sua route table tem caminho direto para um Internet Gateway.
Uma subnet privada não ganha esse caminho só porque sua tag diz `privada`; para saída IPv4,
ela normalmente aponta a um NAT Gateway localizado em subnet pública.

## Objetivo

Usando AWS CLI ou CloudFormation, construa:

- VPC `10.30.0.0/16` com tag `learning-infra-rotas`;
- uma subnet pública `10.30.0.0/24` e uma privada `10.30.10.0/24`;
- Internet Gateway anexado à VPC;
- route table `learning-infra-rt-publica` com `0.0.0.0/0 → igw-*`;
- NAT Gateway na subnet pública;
- route table `learning-infra-rt-privada` com `0.0.0.0/0 → nat-*`;
- associações corretas entre subnet e route table.

Comece inventariando IDs em variáveis, nunca copiando o primeiro item de uma lista sem
filtro. Use tags e `--filters`.

## O que não deve ser alegado

As rotas são metadata no MiniStack. A Verificação conclui que a topologia **representa**
subnets pública e privada; nenhum pacote passou pelo IGW ou NAT, e NACL/security groups
não filtraram tráfego local.

