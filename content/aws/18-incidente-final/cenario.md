---
id: aws/18-incidente-final
titulo: O stack verde com uma arquitetura errada
dificuldade: mestre
ministack: true
---
# O stack verde com uma arquitetura errada

O template `incidente.yaml` foi aprovado porque “o deploy ficou verde”. Isso não basta.
Há problemas de modelagem que produzem perda de histórico, reprocessamento infinito,
consultas caras e exposição excessiva.

## Contrato de saída

Corrija e implante `learning-infra-incidente` para que:

- o bucket `learning-infra-incidente-arquivos` tenha versionamento;
- a fila principal use visibility timeout 60 e redrive após três tentativas;
- exista a DLQ `learning-infra-incidente-dlq`;
- a tabela `learning-infra-incidente-eventos` use `origem` + `ocorridoEm`;
- a VPC use `10.80.0.0/16` e tenha quatro subnets em duas AZs;
- o security group da API aceite TCP/8080 apenas de `10.80.0.0/16`.

Use eventos do stack e consultas por serviço para diagnosticar. Um `CREATE_COMPLETE`
é condição necessária, nunca evidência suficiente de arquitetura correta.

## Revisão de fidelidade

Ao terminar, classifique cada evidência:

- S3, DynamoDB e SQS: data plane local exercitável;
- VPC, subnets e security group: somente estrutura do plano de controle;
- disponibilidade entre AZs, IAM, custo, quotas e tráfego: ainda exigem sandbox AWS.

Esse inventário de lacunas faz parte do trabalho profissional: o emulador reduz ciclos
de feedback, mas não substitui todos os testes na nuvem.

