---
id: kubernetes/06-recursos-e-scheduler
titulo: O Pod que nenhum Node aceita
dificuldade: assistido
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-06
manifestosIniciais: setup
---
# O Pod que nenhum Node aceita

O Deployment existe, mas o Pod permanece `Pending`. Não há erro de imagem nem de
aplicação: o container ainda nem começou. Descubra qual promessa torna o Pod impossível
de agendar e ajuste-a para um valor honesto.

## Requests e limits respondem perguntas diferentes

- `requests` é o que o scheduler reserva ao escolher um Node;
- `limits` é o teto imposto durante execução;
- CPU acima do limit sofre throttling; memória acima do limit pode terminar em OOMKill.

Scheduler trabalha com requests, não com o uso instantâneo exibido por um monitor. Um
Node aparentemente ocioso pode rejeitar um Pod se a capacidade **alocável** já estiver
prometida.

## Diagnostique antes de corrigir

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-06 get pod
kubectl --context docker-desktop -n learning-infra-k8s-06 describe pod -l app=web
kubectl --context docker-desktop get nodes
kubectl --context docker-desktop describe node
```

Leia o Event `FailedScheduling`. Ele traz a decisão do scheduler. Depois compare o
request em `setup/deployment.yaml` com `Allocatable` do Node.

## O objetivo

Faça o Pod ficar disponível com estes valores:

- request de CPU: `25m`;
- request de memória: `32Mi`;
- limit de CPU: `100m`;
- limit de memória: `64Mi`.

Edite e reaplique:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-06 apply -f setup/deployment.yaml
kubectl --context docker-desktop -n learning-infra-k8s-06 rollout status deployment/web
```

`25m` são 25 milicores, ou 2,5% de um core. `Mi` usa potência de 2; `M` usa potência de
10. A unidade ausente também tem significado: CPU `1` é um core inteiro, não um milicore.

## QoS não é decoração

Depois do rollout, inspecione `status.qosClass` do Pod. Como requests e limits diferem,
a classe tende a ser `Burstable`. Sob pressão de memória, a classe participa da ordem de
eviction; por isso valores não são só ferramenta de billing.

## Verifique

A Verificação exige o Deployment disponível e os requests exatos. Diminuir para zero
faria o Pod caber, mas apagaria a informação que o scheduler precisa.

## O que você aprendeu

`Pending` pede Events, não logs. Requests influenciam agendamento e reserva; limits
governam execução. Capacidade, allocatable, consumo e alocação são números diferentes.
Valores arbitrariamente altos e ausência total de valores são erros opostos.
