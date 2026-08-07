---
id: kubernetes/08-persistencia
titulo: Estado que sobrevive ao Pod
dificuldade: guiado
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-08
---
# Estado que sobrevive ao Pod

Pod é descartável; dado nem sempre pode ser. Você vai pedir armazenamento à camada de
storage do cluster, montar o volume num Deployment e provar que o arquivo atravessa a
substituição do Pod.

## Claim não é volume

Abra `estado.yaml`. O PersistentVolumeClaim declara uma necessidade: tamanho e modo de
acesso. A StorageClass padrão do cluster provisiona um PersistentVolume compatível. O Pod
consome o **claim**, não escolhe disco nem caminho do Node.

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-08 apply -f estado.yaml
kubectl --context docker-desktop -n learning-infra-k8s-08 get pvc,pv
kubectl --context docker-desktop -n learning-infra-k8s-08 rollout status deployment/escritor
```

PV é recurso do cluster; PVC pertence ao namespace. `Bound` indica casamento entre oferta
e pedido. StorageClass e provisionador definem como o volume real nasce.

## Grave e destrua a identidade efêmera

O processo inicial cria `bicho.txt` apenas quando ele ainda não existe. Leia:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-08 exec deployment/escritor -- cat /dados/bicho.txt
```

Agora apague o Pod:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-08 delete pod -l app=escritor
kubectl --context docker-desktop -n learning-infra-k8s-08 rollout status deployment/escritor
kubectl --context docker-desktop -n learning-infra-k8s-08 exec deployment/escritor -- cat /dados/bicho.txt
```

Pod, container e filesystem gravável da imagem foram substituídos. PVC e arquivo não.

## O detalhe que evita perda acidental

Apagar Deployment não apaga automaticamente um PVC criado separadamente. Já a política
de retenção do PV decide o que acontece com o volume depois que o claim é apagado. Observe
`persistentVolumeReclaimPolicy` do PV provisionado — `Delete` e `Retain` têm consequências
diferentes.

## Verifique

A Verificação exige o claim em `Bound` e lê `capivara` a partir do Pod atual. Ela não
aceita arquivo gravado apenas na camada efêmera do container.

## O que você aprendeu

PVC expressa demanda, PV representa volume e StorageClass automatiza provisionamento.
Pod e volume têm ciclos de vida diferentes. Persistência no Kubernetes é uma ligação
entre APIs e um provisionador, não simplesmente um diretório do host.
