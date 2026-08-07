---
id: aws/12-aurora
titulo: API Aurora sem fingir storage distribuído
dificuldade: autônomo
ministack: true
infraestruturaRealAws: true
---
# API Aurora sem fingir storage distribuído

Crie um DB Cluster `learning-infra-aurora` com engine `aurora-postgresql`, database
`app`, usuário `admin` e senha sintética. Acrescente uma instância membro chamada
`learning-infra-aurora-writer` e aguarde o cluster ficar disponível.

## Objetivo operacional

- identificar endpoint do cluster e endpoint da instância;
- registrar a tag `Objetivo=aprender-limites` no ARN do cluster;
- criar uma tabela e ler o mesmo dado pela interface SQL/RDS Data compatível disponível;
- inspecionar membros com `describe-db-clusters`.

Use a AWS CLI no endpoint local e evite copiar ARNs: capture-os com `--query`.

## A pergunta mais importante

O que este laboratório **não** provou?

- storage distribuído em seis cópias;
- quorum ou durabilidade Aurora;
- failover entre zonas de disponibilidade;
- replica lag, autoscaling e performance Aurora;
- isolamento por subnet/security group.

O MiniStack oferece API de DB Cluster e banco compatível em container. Chamar isso de
“Aurora local completo” seria transformar uma substituição útil em uma conclusão falsa.

