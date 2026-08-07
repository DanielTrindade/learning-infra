---
id: aws/09-ec2-e-security-group
titulo: Uma instância registrada não é uma máquina virtual
dificuldade: autônomo
ministack: true
---
# Uma instância registrada não é uma máquina virtual

Há um serviço HTTP que deveria aceitar tráfego somente da VPC `10.40.0.0/16` na porta
8080. Modele VPC, subnet, security group e uma instância EC2 chamada
`learning-infra-api`.

## Requisitos

- o security group não pode usar `0.0.0.0/0` na porta 8080;
- a regra deve aceitar TCP/8080 apenas de `10.40.0.0/16`;
- a instância usa `ami-local-12345678`, tipo `t3.micro` e a subnet criada;
- a tag `Name` identifica a instância;
- registre user data que documente como a API seria iniciada.

Use `run-instances`, capture o ID e inspecione com `describe-instances`. Trate IDs e
estados como dados retornados pela API, não como strings previsíveis.

## Limite deliberado

O MiniStack não inicia uma VM EC2. Não há SSH, cloud-init, ENI real nem enforcement do
security group. Este Cenário verifica intenção de plano de controle. Testar a porta 8080
localmente seria um falso positivo e não faz parte da Verificação.

