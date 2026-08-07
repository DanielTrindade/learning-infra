---
id: aws/06-cloudformation
titulo: Infraestrutura declarada, não uma sequência de cliques
dificuldade: assistido
ministack: true
---
# Infraestrutura declarada, não uma sequência de cliques

O arquivo `stack.yaml` descreve um bucket, uma fila e uma tabela. Ele está incompleto:
a tabela não tem sort key e a fila não tem visibility timeout explícito.

## Objetivo

Corrija o template e implante pelo endpoint local:

```powershell
$endpoint = "http://127.0.0.1:4566"
aws --endpoint-url $endpoint cloudformation deploy `
  --stack-name learning-infra-dados `
  --template-file stack.yaml

aws --endpoint-url $endpoint cloudformation describe-stack-events `
  --stack-name learning-infra-dados
```

Depois altere uma propriedade no template e execute `deploy` novamente. Observe que a
operação atualiza a intenção declarada; não é necessário reproduzir manualmente cada
`create-*`.

## O que CloudFormation não resolve

IaC torna mudanças reproduzíveis, mas não torna uma arquitetura correta. O engine do
MiniStack cobre os recursos usados aqui, sem reproduzir quotas, IAM ou todos os estados
intermediários da AWS.

