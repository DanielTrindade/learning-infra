---
id: kubernetes/13-kustomize
titulo: O mesmo manifesto em ambientes diferentes
dificuldade: autônomo
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-13
---
# O mesmo manifesto em ambientes diferentes

O diretório `base/` descreve uma aplicação com uma réplica e `AMBIENTE=base`. O overlay
`overlays/producao/` deve produzir três réplicas e `AMBIENTE=producao`, sem copiar o
Deployment inteiro.

## O objetivo

Complete `overlays/producao/kustomization.yaml` e aplique com o Kustomize embutido no
`kubectl`:

```powershell
kubectl --context docker-desktop kustomize overlays/producao
kubectl --context docker-desktop -n learning-infra-k8s-13 diff -k overlays/producao
kubectl --context docker-desktop -n learning-infra-k8s-13 apply -k overlays/producao
```

`diff` usa código de saída diferente de zero quando há diferenças; isso não significa que
o comando quebrou.

## O que observar no build

O `configMapGenerator` acrescenta um hash ao nome da ConfigMap. Quando conteúdo muda, o
nome muda, e a referência no Deployment é reescrita junto. Isso altera o Pod template e
provoca rollout sem um `rollout restart` manual.

O overlay deve conter apenas diferença de ambiente:

- `replicas: 3` para o Deployment `catalogo`;
- merge da ConfigMap `configuracao` com `AMBIENTE=producao`.

Consulte o resultado aplicado:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-13 get deployment,configmap,pod
kubectl --context docker-desktop -n learning-infra-k8s-13 exec deployment/catalogo -- printenv AMBIENTE
```

## Verifique

A Verificação olha o resultado, não a estrutura dos diretórios: três réplicas disponíveis
e variável `producao` dentro do processo.

## O que você aprendeu

Kustomize transforma objetos sem templating textual. Base preserva intenção comum; overlay
declara diferenças. Geradores com hash conectam mudança de configuração a rollout. Sempre
inspecione o build antes de aplicar: abstração de manifesto não elimina revisão.
