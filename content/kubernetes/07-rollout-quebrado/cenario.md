---
id: kubernetes/07-rollout-quebrado
titulo: Uma versão que nunca chega
dificuldade: autônomo
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-07
manifestosIniciais: setup
---
# Uma versão que nunca chega

Um Deployment deveria manter três réplicas de nginx, mas o rollout não termina. Recupere
o serviço usando a imagem `nginx:1.27-alpine` e deixe as três réplicas disponíveis.

## O que você tem

O manifesto aplicado está em `setup/deployment.yaml`. Você pode alterar o arquivo ou usar
um comando imperativo, mas ao final arquivo e cluster devem concordar.

Não há passo a passo. Estas ferramentas delimitam o problema:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-07 get deployment,replicaset,pod
kubectl --context docker-desktop -n learning-infra-k8s-07 rollout status deployment/web --timeout=20s
kubectl --context docker-desktop -n learning-infra-k8s-07 rollout history deployment/web
kubectl --context docker-desktop -n learning-infra-k8s-07 describe pod -l app=web
kubectl --context docker-desktop -n learning-infra-k8s-07 get events --sort-by=.lastTimestamp
```

## Um detalhe operacional importante

`rollout undo` só ajuda quando existe uma revisão saudável anterior. Este namespace nasceu
com a versão quebrada; portanto histórico não é backup que apareceu magicamente. Em um
incidente real, você precisa de uma referência conhecida — tag imutável, digest ou revisão
anterior — para decidir para onde voltar.

Depois de corrigir, acompanhe até a Condition `Available` e os contadores convergirem.
Observe também como o Deployment mantém ReplicaSets antigos com zero réplicas para
suportar histórico.

## Verifique

A Verificação exige a imagem conhecida no Pod template e três réplicas disponíveis.
Editar apenas um Pod não passa: o ReplicaSet o substituiria usando o template quebrado.

## O que você aprendeu

Rollout é uma transição controlada entre templates, não sinônimo de `apply`. Events
explicam `ImagePullBackOff`; histórico só é útil quando houve uma revisão boa; e a
correção durável acontece no Deployment, não no Pod derivado.
