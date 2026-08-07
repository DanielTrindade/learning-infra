---
id: kubernetes/09-job-falhando
titulo: Um Job que insiste em falhar
dificuldade: mestre
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-09
manifestosIniciais: setup
---
# Um Job que insiste em falhar

Um processamento chamado `relatorio` deveria terminar uma vez e registrar
**relatorio pronto** no log. Ele não termina. Descubra a causa, corrija o manifesto e
faça o Job chegar à Condition `Complete`.

## O ambiente

O objeto já foi aplicado a partir de `setup/job.yaml`. Não há passo a passo nem indicação
da linha defeituosa.

Você já tem as ferramentas necessárias:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-09 get job,pod
kubectl --context docker-desktop -n learning-infra-k8s-09 describe job relatorio
kubectl --context docker-desktop -n learning-infra-k8s-09 logs job/relatorio
kubectl --context docker-desktop -n learning-infra-k8s-09 get events --sort-by=.lastTimestamp
```

## O que torna Job diferente

Deployment procura execução contínua; Job procura conclusões bem-sucedidas. Uma saída com
código diferente de zero pode criar novas tentativas até `backoffLimit`. `restartPolicy`
de Jobs aceita `Never` ou `OnFailure`, não `Always`.

O Pod template de um Job é praticamente imutável. Depois de editar, a operação correta é
apagar e recriar o Job — não editar o Pod derivado:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-09 delete job relatorio
kubectl --context docker-desktop -n learning-infra-k8s-09 apply -f setup/job.yaml
```

## Verifique

A Verificação exige Condition `Complete=True` e o texto esperado no log. Um Job que apenas
parou de tentar, com Condition `Failed`, continua reprovado.

## O que você aprendeu

Job modela conclusão, backoff limita repetição e exit code faz parte da API operacional
do processo. Pods falhos permanecem como evidência. Controllers não sabem se uma tarefa
é semanticamente idempotente: repetir com segurança é responsabilidade do programa.
