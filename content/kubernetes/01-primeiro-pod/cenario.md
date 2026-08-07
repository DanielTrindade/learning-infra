---
id: kubernetes/01-primeiro-pod
titulo: O primeiro objeto no cluster
dificuldade: guiado
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-01
---
# O primeiro objeto no cluster

No Docker você pediu a um daemon para iniciar um processo. No Kubernetes você declara
um **estado desejado** numa API. Vários componentes observam essa declaração e trabalham
até o estado real se aproximar dela. Este Cenário começa por um Pod para tornar esse
ciclo visível sem esconder nada atrás de um controlador.

## Antes do primeiro comando

Ao iniciar, a plataforma recriou o namespace `learning-infra-k8s-01`. Todo comando usa
também `--context docker-desktop`: isso é intencional. Um `kubectl` pode conhecer vários
clusters, inclusive clusters reais, e um laboratório nunca deve depender do contexto que
por acaso estiver selecionado.

Defina estas duas ideias antes de continuar:

- **cluster** é o conjunto formado pelo control plane e pelos Nodes que executam carga;
- **namespace** é um recorte lógico da API, não uma máquina nem um cluster menor.

## O manifesto

Abra `pod.yaml` no diretório de trabalho. Ele descreve identidade em `metadata` e estado
desejado em `spec`. `apiVersion` e `kind` dizem à API qual esquema interpretar.

Envie o objeto:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-01 apply -f pod.yaml
```

`apply` não “roda o YAML”. Ele calcula uma alteração e a envia ao API server. O scheduler
escolhe um Node; o kubelet daquele Node pede a imagem ao runtime e mantém o container de
acordo com a especificação.

## Estado desejado e estado observado

Veja o resumo e depois o objeto inteiro:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-01 get pod web -o wide
kubectl --context docker-desktop -n learning-infra-k8s-01 get pod web -o yaml
```

No YAML devolvido, compare `spec` com `status`. Você escreveu o primeiro; componentes do
cluster escreveram o segundo. Campos como `podIP`, `nodeName`, `phase` e `conditions` não
estavam no seu arquivo porque são observações, não pedidos.

Agora use três lentes diferentes sobre o mesmo objeto:

```powershell
kubectl --context docker-desktop -n learning-infra-k8s-01 describe pod web
kubectl --context docker-desktop -n learning-infra-k8s-01 logs web
kubectl --context docker-desktop -n learning-infra-k8s-01 exec web -- hostname
```

`describe` combina estado e Events; `logs` consulta a saída do processo; `exec` inicia um
processo novo dentro do container existente. Nenhum deles substitui os outros.

## Experimente uma mudança inválida

Troque `restartPolicy: Always` por `Sometimes` e tente aplicar. A API rejeita antes de
qualquer Node fazer trabalho. Esse é um limite importante: YAML válido não implica objeto
Kubernetes válido. Reverta para `Always` depois.

## Verifique

A Verificação exige o Pod `web` em `Running` e a label `app: web`. A label parece detalhe,
mas será o mecanismo que liga recursos diferentes nos próximos Cenários.

## O que você aprendeu

`kubectl` conversa com o API server. `spec` expressa intenção e `status` registra observação.
Scheduler escolhe Node; kubelet mantém containers. Namespace organiza objetos; contexto
escolhe cluster. Um Pod é a menor unidade agendável — útil para aprender, mas descartável
demais para operar diretamente em produção.
