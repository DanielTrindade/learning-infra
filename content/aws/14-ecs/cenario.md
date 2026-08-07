---
id: aws/14-ecs
titulo: Task definition não garante uma task saudável
dificuldade: autônomo
ministack: true
infraestruturaRealAws: true
---
# Task definition não garante uma task saudável

O ECS do MiniStack registra o plano de controle e executa tasks como containers Docker
reais. Ele não é Fargate e não cria uma instância EC2 escondida.

## Objetivo

1. Crie o cluster `learning-infra-ecs`.
2. Registre `task-definition.json`.
3. Rode uma task e aguarde `RUNNING`.
4. Confirme que `http://127.0.0.1:18080` responde.
5. Pare a task, corrija qualquer erro na definição e rode novamente.

```powershell
$endpoint = "http://127.0.0.1:4566"
aws --endpoint-url $endpoint ecs create-cluster --cluster-name learning-infra-ecs
aws --endpoint-url $endpoint ecs register-task-definition `
  --cli-input-json file://task-definition.json
$task = aws --endpoint-url $endpoint ecs run-task `
  --cluster learning-infra-ecs --task-definition learning-infra-web `
  --query "tasks[0].taskArn" --output text
aws --endpoint-url $endpoint ecs describe-tasks `
  --cluster learning-infra-ecs --tasks $task
```

Use os labels `com.amazonaws.ecs.*` para relacionar o container real à task, sem aceitar
qualquer nginx da máquina como evidência.

## Limites

`awslogs` é aceito, mas não entrega stdout ao CloudWatch Logs neste emulador. Mudanças de
estado também não disparam EventBridge. O HTTP prova o processo local, não Fargate,
ENI, IAM task role, autoscaling ou billing ECS.

