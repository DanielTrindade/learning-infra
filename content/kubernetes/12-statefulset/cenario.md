---
id: kubernetes/12-statefulset
titulo: Identidade estável antes de escala
dificuldade: guiado
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-12
---
# Identidade estável antes de escala

Deployment trata réplicas como intercambiáveis. Alguns sistemas precisam de ordem,
hostname previsível e volume próprio por réplica. StatefulSet oferece essas garantias,
mas não transforma qualquer aplicação em banco distribuído.

## O conjunto

Abra `estado.yaml`. Ele combina:

- Service headless (`clusterIP: None`) para DNS direto dos Pods;
- StatefulSet com duas réplicas ordenadas;
- `volumeClaimTemplates`, que cria um PVC independente para cada ordinal.

Aplique e acompanhe:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-12 apply -f estado.yaml
kubectl --context docker-desktop -n learning-infra-k8s-12 get pod,pvc --watch
```

Por padrão, `banco-1` espera `banco-0` ficar pronto. Os nomes não carregam hash porque
identidade é parte do contrato: `banco-0`, `banco-1`.

## Identidade de rede e de disco

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-12 exec banco-0 -- hostname
kubectl --context docker-desktop -n learning-infra-k8s-12 exec banco-1 -- hostname
kubectl --context docker-desktop -n learning-infra-k8s-12 exec banco-0 -- cat /dados/identidade
kubectl --context docker-desktop -n learning-infra-k8s-12 get pvc
```

Cada Pod grava o próprio hostname em seu volume. Apague `banco-0`, espere voltar e leia o
arquivo novamente. O Pod mantém ordinal e reassocia o claim do mesmo ordinal.

O Service headless não entrega um único IP balanceado. O DNS expõe identidades individuais
como `banco-0.banco` e `banco-1.banco`, o que permite que membros de um sistema stateful
se encontrem.

## O que StatefulSet não faz

Ele não replica dados, não elege líder, não cria quorum e não faz backup. Essas propriedades
pertencem ao software ou a um operador. Kubernetes fornece primitivas de identidade e
ciclo de vida; consistência do dado continua sendo problema da aplicação.

## Verifique

A Verificação exige duas réplicas prontas, Service headless e o arquivo de `banco-0`
preservando sua identidade.

## O que você aprendeu

StatefulSet associa ordinal, DNS e PVC estáveis. Headless Service expõe os membros sem um
VIP balanceador. Escala stateful é deliberadamente ordenada, e infraestrutura não substitui
o protocolo distribuído da aplicação.
