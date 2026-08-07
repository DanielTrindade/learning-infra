---
id: kubernetes/02-reconciliacao
titulo: Reconciliação e Pods descartáveis
dificuldade: guiado
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-02
---
# Reconciliação e Pods descartáveis

Um Pod direto não volta se você o apagar. Kubernetes fica interessante quando um
**controller** observa uma intenção durável e corrige divergências. Aqui um Deployment
vai manter três réplicas sem você conhecer o nome de nenhuma delas.

## Três objetos, uma intenção

Abra `deployment.yaml` e aplique:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-02 apply -f deployment.yaml
kubectl --context docker-desktop -n learning-infra-k8s-02 get deployment,replicaset,pod
```

Você declarou um Deployment. Ele criou um ReplicaSet, e o ReplicaSet criou Pods. Veja a
cadeia de `ownerReferences`:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-02 get rs -o yaml
kubectl --context docker-desktop -n learning-infra-k8s-02 get pods -o yaml
```

O hash no nome do ReplicaSet vem do template do Pod. Quando o template muda, surge outro
ReplicaSet — é assim que um rollout consegue manter versão anterior e nova ao mesmo tempo.

## Provoque divergência

Liste os Pods, escolha qualquer nome e apague:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-02 get pods
kubectl --context docker-desktop -n learning-infra-k8s-02 delete pod <nome-do-pod>
kubectl --context docker-desktop -n learning-infra-k8s-02 get pods --watch
```

O Pod não “ressuscita”. O ReplicaSet percebe que há menos réplicas do que em `spec` e cria
outro, com outra identidade. Saia do `--watch` com `Ctrl+C`.

## Mude a intenção, não o efeito

O arquivo começa com duas réplicas. Altere `spec.replicas` para **3** e reaplique:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-02 apply -f deployment.yaml
kubectl --context docker-desktop -n learning-infra-k8s-02 rollout status deployment/web
```

Você também poderia usar `kubectl scale`, mas aí arquivo e cluster discordariam. No
próximo `apply`, o arquivo venceria. Declarativo só funciona quando existe uma fonte de
verdade deliberada.

## Conditions são melhores que contagem

`3/3 Pods` não basta para dizer que um rollout terminou: eles podem existir sem estar
prontos. Consulte as Conditions do Deployment:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-02 get deployment web -o jsonpath='{.status.conditions}'
```

## Verifique

A Verificação exige intenção e observação: `spec.replicas` igual a 3 e
`status.availableReplicas` igual a 3.

## O que você aprendeu

Controllers executam loops de reconciliação. Deployment controla ReplicaSets; ReplicaSet
controla Pods. Pods são substituíveis, e estabilidade vem do controller. Alterar o estado
observado sem alterar a fonte declarativa produz uma correção temporária.
