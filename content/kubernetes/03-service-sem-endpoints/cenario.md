---
id: kubernetes/03-service-sem-endpoints
titulo: Um Service que não encontra ninguém
dificuldade: assistido
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-03
manifestosIniciais: setup
---
# Um Service que não encontra ninguém

O ambiente já contém dois Deployments e um Service. Os Pods estão `Running`, mas o
cliente não alcança `http://web`. Seu trabalho é encontrar a ligação ausente e corrigi-la.

## O problema que Service resolve

Pods ganham IPs, mas são efêmeros: uma substituição muda nome e endereço. Um Service
oferece identidade de rede estável e seleciona backends por labels. O controller de
EndpointSlice materializa o conjunto atual de destinos.

Isso significa que há três perguntas diferentes:

1. o Service existe e tem ClusterIP?
2. o seletor encontra Pods?
3. os Pods encontrados estão prontos?

## Observe antes de editar

O ambiente veio de `setup/stack.yaml`. Não procure o defeito primeiro; observe o cluster:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-03 get pod,service,endpointslice
kubectl --context docker-desktop -n learning-infra-k8s-03 describe service web
kubectl --context docker-desktop -n learning-infra-k8s-03 get pods --show-labels
kubectl --context docker-desktop -n learning-infra-k8s-03 exec deployment/cliente -- wget -qO- http://web
```

`ClusterIP` sem endpoints é uma placa apontando para lugar nenhum. Compare
`spec.selector` do Service com `metadata.labels` do template do Deployment.

## Corrija de forma declarativa

Edite `setup/stack.yaml` e reaplique o arquivo inteiro:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-03 apply -f setup/stack.yaml
```

Não recrie Pods se você alterou apenas o Service. O controller deve atualizar o
EndpointSlice assim que o seletor passar a casar.

Teste pelo cliente, de dentro da rede do cluster:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-03 exec deployment/cliente -- wget -qO- http://web
```

O nome curto `web` resolve porque cliente e Service estão no mesmo namespace. Em outro,
seria necessário `web.learning-infra-k8s-03` ou o nome completo em `cluster.local`.

## Verifique

A Verificação consulta o EndpointSlice e faz uma requisição a partir do Pod cliente.
Expor porta no Windows não faz parte deste Cenário: `ClusterIP` é interno por definição.

## O que você aprendeu

Service não procura nomes de Deployment; ele seleciona labels de Pods. ClusterIP é
estável, EndpointSlices mudam com os backends, e DNS fornece descoberta. `Running` em
todos os Pods não prova que a rede lógica está conectada.
