---
id: aws/15-alb-ecs
titulo: O listener existe, mas a rota não chega à task
dificuldade: mestre
ministack: true
infraestruturaRealAws: true
---
# O listener existe, mas a rota não chega à task

Construa uma task ECS na porta 18081 e um ALB chamado `learning-infra-alb`. A rota
`/saude` deve atravessar o data plane do ALB e chegar ao container.

## Cadeia que precisa estar íntegra

```text
requisição com Host do ALB
  → listener :80
  → regra de path /saude
  → target group HTTP:80
  → target host.docker.internal:18081
  → nginx da task ECS
```

O workspace traz a task definition. Crie cluster/task, VPC e duas subnets metadata,
load balancer, target group `ip`, listener e registro do target. Capture ARNs a partir
das respostas.

Para testar o endpoint local sem depender de DNS externo:

```powershell
curl.exe --resolve learning-infra-alb.alb.localhost:4566:127.0.0.1 `
  http://learning-infra-alb.alb.localhost:4566/saude
```

Se receber 502/503, diagnostique na ordem: rule, action, targets, porta publicada e
container. Não use `describe-target-health` como prova final: MiniStack armazena health
checks, mas não os executa.

## O que foi provado

Host/path routing e proxy HTTP local. Subnets, security groups, cross-zone e health
check não reproduzem a rede do ALB na AWS.

