# Pesquisa — trilha aprofundada de Kubernetes

> Estado: proposta para implementação
>
> Data da pesquisa: 2026-08-07
>
> Escopo: ambiente local Windows + Docker Desktop, usando `kind` e `kubectl`

## Resumo executivo

A recomendação é uma trilha de **14 Cenários**: treze formam o núcleo e o último,
sobre HPA, fica condicionado à instalação confiável do pipeline de métricas. É um
salto deliberado em relação aos cinco Cenários de Docker: a trilha começa no modelo
de objetos e nos control loops, passa por rede, configuração, saúde, recursos,
escalonamento, estado e segurança, e termina com incidentes em que mais de uma camada
parece culpada.

O ambiente deve ser um cluster `kind` compartilhado, com um control plane e dois
workers. Cada Cenário usa um namespace próprio, recriado ao iniciar e removido ao
trocar de Cenário. O acesso precisa usar **kubeconfig isolado** e o contexto explícito
`kind-learning-infra`; confiar em `current-context` seria um risco inaceitável, pois um
comando de preparação ou limpeza poderia atingir outro cluster do autor.

A Verificação também precisa evoluir. Substrings de saída de `kubectl` são frágeis e
não cobrem negações. O vocabulário recomendado verifica condições, JSONPath,
contagens, permissões RBAC e conectividade positiva ou negativa com timeout. Como os
controllers são assíncronos, as Asserções devem fazer polling limitado, não uma leitura
única imediatamente após uma mudança.

## Método e fontes

A pesquisa começou pelo Context7 MCP, como exigido. A biblioteca resolvida foi
`/websites/kubernetes_io` (documentação oficial do Kubernetes, trust score 9,9) e
foram consultados dois recortes: arquitetura/workloads/operação e
rede/storage/segurança. Lacunas de ambiente local foram complementadas apenas com
fontes primárias em `kubernetes.io` e `kind.sigs.k8s.io`.

As escolhas abaixo partem de três fatos estruturais:

- Um cluster tem control plane e worker nodes; API server, scheduler e controllers
  têm responsabilidades distintas. [Kubernetes Components](https://kubernetes.io/docs/concepts/overview/components/)
- Objetos são registros de intenção, e controllers aproximam continuamente o estado
  atual do estado desejado. [Objects in Kubernetes](https://kubernetes.io/docs/concepts/overview/working-with-objects/),
  [Controllers](https://kubernetes.io/docs/concepts/architecture/controller/)
- `kind` executa nodes Kubernetes como containers e suporta cluster multi-node por
  configuração, embora esses nodes não acrescentem capacidade física real nem
  isolamento forte. [kind Quick Start](https://kind.sigs.k8s.io/docs/user/quick-start/),
  [kind Configuration](https://kind.sigs.k8s.io/docs/user/configuration/)

## Resultado pedagógico esperado

Ao terminar, o leitor deve conseguir:

1. Explicar o caminho API → controller → scheduler → kubelet, distinguindo `spec`,
   `status` e eventos.
2. Escolher entre Pod, Deployment, StatefulSet, Job e CronJob.
3. Diagnosticar `Pending`, `CrashLoopBackOff`, `ImagePullBackOff`, probe falhando,
   Service sem EndpointSlices e `OOMKilled` antes de alterar recursos ao acaso.
4. Fazer rollout e rollback, configurar descoberta por Service/DNS e expor uma rota
   local de modo reproduzível.
5. Usar ConfigMap e Secret sem confundir base64 com criptografia.
6. Definir requests, limits, QoS e regras de scheduling conscientemente.
7. Persistir estado com PVC/StatefulSet, operar Jobs idempotentes e aplicar menor
   privilégio com RBAC, Pod Security e NetworkPolicy.
8. Raciocinar sobre disponibilidade com réplicas, distribuição, PDB e, quando o
   add-on estiver estável, HPA.

## Ambiente local recomendado

### Auditoria desta máquina

Leitura não destrutiva feita em 2026-08-07:

| Item | Estado observado |
|---|---|
| Docker Engine | disponível, `29.6.1` |
| `kubectl` | disponível via Docker Desktop, `v1.36.1` |
| `kind` | ausente |

O `kubectl` deve ficar no máximo uma minor version distante do API server. A
documentação de instalação no Windows registra essa política e alerta que o Docker
Desktop também põe uma cópia do binário no `PATH`.
[Install and Set Up kubectl on Windows](https://kubernetes.io/docs/tasks/tools/install-kubectl-windows/)

### Requisitos

- Docker Desktop em modo **Linux containers**. `kind` no Windows não funciona com
  Windows containers. [kind Known Issues](https://kind.sigs.k8s.io/docs/user/known-issues/)
- `kind` instalado e uma combinação `kind`/`kindest/node` pinada a partir das release
  notes da mesma versão. A documentação só garante a imagem de node com a versão de
  `kind` para a qual ela foi publicada. Nunca usar `latest`; pinar tag **e digest**.
  [kind Node Image](https://kind.sigs.k8s.io/docs/design/node-image/),
  [kind Configuration](https://kind.sigs.k8s.io/docs/user/configuration/)
- `kubectl` compatível com a versão pinada do cluster.
- Estimativa a validar no protótipo: 4 CPUs, 8 GiB de RAM disponíveis ao Docker
  Desktop e 12 GiB livres em disco para três nodes, imagens e add-ons. É uma margem de
  engenharia, não um requisito publicado pelo Kubernetes.
- Rede na primeira preparação, ou todas as imagens e manifestos já pinados e
  pré-carregados. `kind load docker-image` permite carregar imagens locais no cluster;
  tags `latest` devem ser evitadas ou acompanhadas de `imagePullPolicy` explícita.
  [kind Quick Start](https://kind.sigs.k8s.io/docs/user/quick-start/)

### Perfil único do cluster

Nome do cluster: `learning-infra`. Contexto gerado: `kind-learning-infra`.

```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: learning-infra
nodes:
  - role: control-plane
    extraPortMappings:
      - containerPort: 30092
        hostPort: 8092
        listenAddress: "127.0.0.1"
        protocol: TCP
  - role: worker
  - role: worker
```

O mapeamento `127.0.0.1:8092 → NodePort 30092` é proposital. No Docker Desktop, a rede
dos containers/nodes não é exposta diretamente ao host; `extraPortMappings` é a opção
cross-platform, e para NodePort o `containerPort` do node deve ser igual ao
`service.nodePort`. [kind Known Issues](https://kind.sigs.k8s.io/docs/user/known-issues/),
[kind Configuration](https://kind.sigs.k8s.io/docs/user/configuration/)

O API server deve continuar no default loopback e numa porta aleatória. O próprio
`kind` desaconselha expor o cluster fora de loopback, pois não entrega uma estratégia
de segurança ou atualização de produção.
[kind Configuration](https://kind.sigs.k8s.io/docs/user/configuration/)

### Kubeconfig isolado: guardrail obrigatório

Criar o cluster com um arquivo exclusivo, por exemplo
`data/kubernetes/kubeconfig.yaml`, ignorado pelo Git:

```powershell
kind create cluster `
  --name learning-infra `
  --config .\content\kubernetes\cluster.yaml `
  --kubeconfig .\data\kubernetes\kubeconfig.yaml `
  --wait 5m
```

`kind` documenta que `--kubeconfig` carrega somente o arquivo indicado, sem merge com
`~/.kube/config`. Toda chamada da plataforma deve repetir os dois guardrails:

```text
kubectl --kubeconfig <caminho-absoluto> --context kind-learning-infra ...
```

Não basta definir `KUBECONFIG` no shell que iniciou o backend, e nunca se deve executar
`kubectl config use-context`. [kind Quick Start](https://kind.sigs.k8s.io/docs/user/quick-start/)

Antes de qualquer preparação ou limpeza, conferir:

1. o contexto exato é `kind-learning-infra`;
2. `kind get clusters` contém exatamente `learning-infra`;
3. `kubectl get --raw=/readyz --request-timeout=5s` responde;
4. os nodes esperados são `learning-infra-control-plane` e dois workers.

### Add-ons que precisam ser decisão de plataforma

Não os esconder dentro de um Cenário:

- **Storage:** StatefulSet requer volumes pré-provisionados ou um provisioner. A
  plataforma deve instalar e pinar uma StorageClass de laboratório, e o bootstrap só
  termina quando um PVC-smoke-test fica `Bound`.
  [StatefulSets](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/),
  [Persistent Volumes](https://kubernetes.io/docs/concepts/storage/persistent-volumes/)
- **NetworkPolicy:** a API existir não prova que a política é aplicada; enforcement
  depende do plugin de rede. O bootstrap deve executar uma prova comportamental de
  allow/deny. Se o CNI padrão não passar, criar o cluster com
  `disableDefaultCNI: true` e instalar uma implementação compatível pinada.
  [Network Policies](https://kubernetes.io/docs/concepts/services-networking/network-policies/),
  [kind Configuration](https://kind.sigs.k8s.io/docs/user/configuration/)
- **Métricas:** HPA por CPU depende de `metrics.k8s.io`, geralmente provida por um
  metrics-server separado. Por isso o Cenário 14 é condicionado ao add-on ficar
  reproduzível no Windows/kind. [Horizontal Pod Autoscaling](https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/)

## Estrutura da trilha

| # | Cenário | Dificuldade | Tempo alvo | Problema dominante |
|---:|---|---|---:|---|
| 01 | Do manifesto ao Pod | Guiado | 45–60 min | Entender API, `spec/status` e o ciclo de diagnóstico |
| 02 | O controller traz de volta | Guiado | 60 min | Réplicas desaparecem e o estado desejado converge |
| 03 | Um rollout que nunca termina | Assistido | 60–75 min | Imagem inválida e rollback |
| 04 | O Service existe, mas não tem backends | Assistido | 75 min | Selector/porta errados e EndpointSlice vazio |
| 05 | Da rede do cluster ao `localhost` | Assistido | 60 min | ClusterIP, NodePort e exposição no Docker Desktop |
| 06 | Configuração sem rebuild | Assistido | 75–90 min | ConfigMap, Secret e atualização de Pods |
| 07 | Healthy não significa Ready | Assistido | 90 min | Probes mal configuradas removem tráfego ou reiniciam a app |
| 08 | Pending e OOMKilled | Autônomo | 90–120 min | Requests, limits, QoS, quota e pressão de memória |
| 09 | O scheduler tem restrições | Autônomo | 90–120 min | Labels, taints, affinity e topology spread |
| 10 | Estado que sobrevive ao Pod | Assistido | 90–120 min | PVC, StorageClass, StatefulSet e identidade estável |
| 11 | Jobs que falham sem loop infinito | Autônomo | 90 min | Retry, deadline, concorrência e idempotência |
| 12 | Acesso mínimo e Pod seguro | Autônomo | 120 min | ServiceAccount, RBAC e Pod Security Admission |
| 13 | A rede ficou segura e o DNS morreu | Mestre | 120–150 min | NetworkPolicy default-deny com exceções mínimas |
| 14 | Pico de carga durante manutenção | Mestre, condicional | 120–180 min | HPA, requests, distribuição e PDB |

O total esperado é de 20–26 horas. Cenários longos devem salvar o progresso das
Asserções, mas a conclusão continua sendo atômica no domínio atual.

## Detalhamento dos Cenários

### 01 — Do manifesto ao Pod

**Missão:** criar um Pod declarativamente, observar o objeto aceito pela API e provar
que o container serve o marcador esperado.

**Conceitos:** `apiVersion`, `kind`, `metadata`, `spec`, `status`, namespace, labels,
Pod/container, imagem/comando, `kubectl apply/get/describe/logs/exec`, eventos e
JSONPath. O texto deve mostrar que Pod é descartável; ainda não ensinar Deployment.

**Diagnóstico obrigatório:** comparar o YAML local com
`kubectl get pod -o yaml`, ler condições e eventos antes de abrir um shell.

**Problema prático:** primeiro manifesto contém porta/comando incoerente; o leitor
usa status, evento e log para corrigi-lo.

Fontes: [Pods](https://kubernetes.io/docs/concepts/workloads/pods/),
[Pod Lifecycle](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/),
[Objects in Kubernetes](https://kubernetes.io/docs/concepts/overview/working-with-objects/).

### 02 — O controller traz de volta

**Missão:** substituir o Pod nu por Deployment com três réplicas, apagar um Pod e
observar ReplicaSet/Deployment criarem outro.

**Conceitos:** estado desejado, reconciliation, selectors imutáveis, template,
ownerReferences, Deployment → ReplicaSet → Pod, scale e self-healing.

**Problema prático:** o workspace entrega um Pod nu e um rascunho local de Deployment;
o leitor precisa alinhar selector e labels antes de aplicar (a API rejeita a
inconsistência). Depois, provoca a perda de um Pod e observa UID e nome mudarem sem
perder a capacidade desejada.

Controllers tentam continuamente aproximar estado atual e desejado; Deployment é a
abstração indicada para réplicas stateless intercambiáveis.
[Controllers](https://kubernetes.io/docs/concepts/architecture/controller/),
[Workload Management](https://kubernetes.io/docs/concepts/workloads/controllers/).

### 03 — Um rollout que nunca termina

**Missão:** diagnosticar `ImagePullBackOff` numa nova revisão, entender
`ProgressDeadlineExceeded` e recuperar por rollback.

**Conceitos:** rolling update, `maxSurge`, `maxUnavailable`, revision history,
`rollout status/history/undo`, imagem pinada e `imagePullPolicy`.

**Problema prático:** o ambiente inicial tem uma versão saudável. O roteiro pede que o
leitor publique deliberadamente uma tag inexistente, observe a revisão travada e a
recupere. Ele não deve “consertar” editando Pods; deve agir no template do Deployment
ou fazer rollback. Assim a preparação continua declarativa e não depende de aplicar
duas revisões com uma espera escondida entre elas.

Kubernetes reporta rollout parado, mas não o reverte sozinho; a revisão é criada por
mudança no Pod template, não por scale.
[Deployments](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/).

### 04 — O Service existe, mas não tem backends

**Missão:** restaurar comunicação `frontend → api` sem usar IP de Pod.

**Conceitos:** IP efêmero de Pod, ClusterIP, selector, `port/targetPort`, portas
nomeadas, CoreDNS e EndpointSlice.

**Problema prático:** o Service resolve por DNS, mas um selector incorreto deixa o
EndpointSlice sem endpoints; depois existe ainda um `targetPort` errado. O roteiro de
diagnóstico é DNS → Service → EndpointSlices → labels → Pod direto.

Services desacoplam clientes dos Pods mutáveis, e o controller atualiza EndpointSlices
conforme os selectors. [Service](https://kubernetes.io/docs/concepts/services-networking/service/),
[DNS for Services and Pods](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/),
[Debug Services](https://kubernetes.io/docs/tasks/debug/debug-application/debug-service/).

### 05 — Da rede do cluster ao `localhost`

**Missão:** expor uma aplicação no único endereço reservado,
`http://127.0.0.1:8092`, via `NodePort: 30092`.

**Conceitos:** ClusterIP versus NodePort versus LoadBalancer, rede do node-container,
`extraPortMappings` e limites de um cluster local. O texto deve explicar Ingress e
Gateway API como próxima camada, mas não instalar um controller incidentalmente.

**Problema prático:** Service começa como ClusterIP e, depois, usa NodePort diferente
do port mapping do `kind`; o leitor alinha as duas pontas.

Kubernetes não fornece por si só o balanceador externo de `type: LoadBalancer`. Além
disso, só criar um Ingress não produz tráfego sem controller; a documentação recomenda
Gateway para novos recursos, pois Ingress está congelado.
[Service](https://kubernetes.io/docs/concepts/services-networking/service/),
[Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/),
[kind Configuration](https://kind.sigs.k8s.io/docs/user/configuration/).

**Expansão posterior:** se `cloud-provider-kind` for adotado e pinado, o guia oficial
atual permite exercitar Ingress/Gateway sem controller de terceiros.
[kind Ingress](https://kind.sigs.k8s.io/docs/user/ingress/)

### 06 — Configuração sem rebuild

**Missão:** variar comportamento sem reconstruir a imagem, separando dado público e
credencial sintética.

**Conceitos:** ConfigMap, Secret, env, volume projetado, referência por chave,
imutabilidade do processo, rollout provocado por mudança de template e checksum como
padrão de implantação.

**Problema prático:** aplicação entra em `CrashLoopBackOff` por chave ausente; depois
continua com valor velho porque env vars não mudam no processo já criado. O leitor
corrige a referência e força uma nova revisão de modo consciente.

ConfigMap existe para configuração não confidencial. Secret usa base64, que **não é
criptografia**, e seus dados são armazenados sem criptografia at rest por padrão; o
Cenário só pode usar valores falsos e nunca gravá-los em logs.
[ConfigMaps](https://kubernetes.io/docs/concepts/configuration/configmap/),
[Good practices for Kubernetes Secrets](https://kubernetes.io/docs/concepts/security/secrets-good-practices/).

### 07 — Healthy não significa Ready

**Missão:** corrigir probes que reiniciam uma app lenta e que mantêm Pods vivos fora
dos backends do Service.

**Conceitos:** startup, liveness e readiness; mecanismos HTTP/TCP/exec; thresholds,
delay, timeout, restart count, EndpointSlice readiness e encerramento gracioso.

**Problema prático:** liveness começa cedo demais e readiness aponta para o caminho
errado. O leitor usa eventos, `lastState`, logs atuais/anteriores e EndpointSlices.

Liveness/startup falhando reinicia o container; readiness falhando remove seu IP dos
EndpointSlices. Uma startup probe suspende as outras até o início completar.
[Liveness, Readiness, and Startup Probes](https://kubernetes.io/docs/concepts/workloads/pods/probes/).

### 08 — Pending e OOMKilled

**Missão:** resolver dois sintomas diferentes sem aumentar recursos cegamente.

**Conceitos:** requests usados no scheduling, limits aplicados por cgroups, CPU
throttling, memory OOM, `lastState.terminated.reason`, QoS (`BestEffort`, `Burstable`,
`Guaranteed`), LimitRange e ResourceQuota.

**Problema prático:** um Pod está `Pending` porque pede mais memória do que qualquer
node; outro reinicia por limite artificialmente pequeno. O workload de memória deve
ter limite baixo e controlado (dezenas de MiB), nunca pressionar o host inteiro.

O scheduler considera requests, mesmo que o uso atual esteja baixo; exceder limite de
memória pode acionar OOM kill, enquanto limite de CPU causa throttling.
[Resource Management for Pods and Containers](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/),
[Pod Quality of Service Classes](https://kubernetes.io/docs/concepts/workloads/pods/pod-qos/).

### 09 — O scheduler tem restrições

**Missão:** colocar workload especializado no worker certo e distribuir réplicas entre
os dois workers.

**Conceitos:** node labels, `nodeSelector`, required/preferred affinity,
taint/toleration, `NoSchedule`, pod anti-affinity e topology spread/maxSkew.

**Problema prático:** o cluster base traz um worker com label de pool especializado e
taint estável. Um Pod fica `Pending` até receber seleção e toleration coerentes; um
Deployment fica concentrado até ganhar distribuição por hostname.

Toleration apenas permite o agendamento; não o garante. Affinity e topology spread
participam de filtro/scoring do scheduler.
[Taints and Tolerations](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/),
[Assigning Pods to Nodes](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/),
[Pod Topology Spread Constraints](https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/).

### 10 — Estado que sobrevive ao Pod

**Missão:** gravar um marcador, recriar o Pod e encontrá-lo na mesma identidade de
storage.

**Conceitos:** volume efêmero versus PV/PVC, binding, StorageClass, access modes,
reclaim policy, StatefulSet, ordinal, Headless Service e `volumeClaimTemplates`.

**Problema prático:** uma app inicialmente usa `emptyDir`; ao recriar o Pod o dado
some. O leitor migra para PVC/StatefulSet, apaga o Pod e comprova o marcador no Pod de
mesmo ordinal.

StatefulSet mantém identidade de rede e storage, mas apagar/scalar o StatefulSet não
apaga automaticamente seus volumes por segurança.
[StatefulSets](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/),
[Persistent Volumes](https://kubernetes.io/docs/concepts/storage/persistent-volumes/),
[Storage Classes](https://kubernetes.io/docs/concepts/storage/storage-classes/).

### 11 — Jobs que falham sem loop infinito

**Missão:** transformar uma tarefa frágil em execução limitada e idempotente, depois
agendá-la sem sobreposição.

**Conceitos:** Job, `restartPolicy: Never`, `backoffLimit`,
`activeDeadlineSeconds`, terminal conditions, TTL/history; CronJob, schedule,
`concurrencyPolicy`, deadline e idempotência.

**Problema prático:** Job falha por argumento e tenta repetidamente; CronJob demora
mais que o intervalo e acumula concorrência. O leitor limita retry/duração e escolhe
`Forbid` conscientemente.

Jobs aplicam backoff exponencial e podem falhar por limite de tentativas ou deadline.
CronJob pode, em certas circunstâncias, criar duas execuções ou nenhuma; a tarefa deve
ser idempotente.
[Jobs](https://kubernetes.io/docs/concepts/workloads/controllers/job/),
[CronJob](https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/).

### 12 — Acesso mínimo e Pod seguro

**Missão:** permitir que uma ServiceAccount leia só uma ConfigMap e fazer o workload
passar pelo perfil `restricted`, sem conceder `cluster-admin`.

**Conceitos:** identidade versus autorização, ServiceAccount, token projetado,
Role/RoleBinding, verbs/resources/resourceNames, `kubectl auth can-i`; Pod Security
Admission `enforce/audit/warn`; non-root, seccomp, capabilities e
`allowPrivilegeEscalation`.

**Problema prático:** a app recebe `Forbidden`; uma tentativa insegura de Pod é
rejeitada no admission. A solução mínima concede `get` numa ConfigMap específica e
declara um `securityContext` compatível.

Este Cenário requer que a plataforma crie o namespace já com um perfil Pod Security
allowlisted (por exemplo, metadado de domínio `politicaPod: restricted`); a fixture não
deve ganhar permissão genérica para criar/alterar Namespace.

Pod Security Admission aplica políticas no namespace na criação do Pod. O perfil
restricted exige, entre outros controles Linux, non-root, seccomp, capabilities
restritas e bloqueio de privilege escalation.
[Using RBAC Authorization](https://kubernetes.io/docs/reference/access-authn-authz/rbac/),
[RBAC Good Practices](https://kubernetes.io/docs/concepts/security/rbac-good-practices/),
[Pod Security Admission](https://kubernetes.io/docs/concepts/security/pod-security-admission/),
[Pod Security Standards](https://kubernetes.io/docs/concepts/security/pod-security-standards/).

### 13 — A rede ficou segura e o DNS morreu

**Missão:** partir de default-deny e liberar somente `frontend → api`, além do DNS
necessário; um Pod intruso deve continuar bloqueado.

**Conceitos:** isolamento por ingress/egress, selectors de Pod/namespace, regras
aditivas, portas L4, DNS e limitações de NetworkPolicy.

**Problema prático:** a política default-deny corta também DNS. O sintoma inicial
parece “Service quebrado”. O leitor separa resolução de nome de conectividade, libera
DNS e depois a comunicação autorizada sem criar `allow-all`.

Sem políticas, tráfego é permitido; default-deny e allow rules mudam esse estado. Uma
negação de egress também bloqueia DNS se não houver exceção.
[Network Policies](https://kubernetes.io/docs/concepts/services-networking/network-policies/).

### 14 — Pico de carga durante manutenção (condicional)

**Missão:** manter serviço disponível enquanto a carga sobe e um worker entra em
manutenção.

**Conceitos:** HPA v2, metrics API, CPU utilization baseada em requests, comportamento
de scale up/down, réplicas mínimas, topology spread, PodDisruptionBudget, eviction e
`kubectl drain`.

**Problema prático:** HPA mostra target desconhecido porque o Deployment não declara
CPU request; depois, um PDB impossível (`minAvailable` igual às réplicas) bloqueia a
manutenção. O leitor corrige requests, limites do HPA e orçamento de disrupção, e
termina com os dois workers novamente schedulable; `drain`/`cordon` não pode vazar para
o próximo Cenário.

O HPA ajusta a escala conforme métricas observadas; para CPU utilization, requests são
parte do denominador. PDB limita evictions voluntárias, mas não protege contra delete
direto nem governa rolling update do controller.
[Horizontal Pod Autoscaling](https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/),
[Disruptions](https://kubernetes.io/docs/concepts/workloads/pods/disruptions/).

**Gate:** só publicar este Cenário quando `metrics.k8s.io` passar um smoke test por
cinco execuções limpas de cluster. Se isso não for reproduzível, manter HPA como
apêndice conceitual e terminar a trilha no incidente de NetworkPolicy.

## Matriz de Verificação automatizável

As expressões abaixo são contratos, não comandos literais. O motor deve resolver os
recursos como JSON e comparar valores tipados. Quando houver espera por controller,
usar polling curto com timeout total explícito.

| # | Evidência final automatizável | Tipo recomendado |
|---:|---|---|
| 01 | Namespace `Active`; Pod `Running` e `Ready=True`; imagem esperada; endpoint interno devolve marcador | condição + JSONPath + conectividade |
| 02 | Deployment `Available=True`; `availableReplicas=3`; três Pods selecionados; ownership passa por ReplicaSet | condição + contagem + ownerReference |
| 03 | rollout completo; imagem do template é a versão saudável; revisão é maior que 1; nenhum Pod em image pull backoff | condição + JSONPath + ausência de estado |
| 04 | Service tem ClusterIP; EndpointSlices têm dois endpoints `ready`; cliente resolve DNS e recebe resposta da API | JSONPath + contagem + conectividade |
| 05 | Service tem `nodePort=30092`; `http://127.0.0.1:8092` responde com marcador | JSONPath + HTTP host |
| 06 | Deployment referencia ConfigMap e Secret pelas chaves corretas; réplica atual está Ready; endpoint mostra configuração pública sem revelar o Secret | referência estruturada + condição + HTTP |
| 07 | startup/liveness/readiness têm mecanismos e paths esperados; Deployment `Available=True`; EndpointSlice possui endpoint ready | JSONPath + condição |
| 08 | requests/limits dentro da faixa didática; QoS esperada; Deployment Ready; nenhum container atual tem `OOMKilled`; Pod impossível deixou de estar Pending | quantidades + JSONPath + condição negativa |
| 09 | Pod especializado está no node com label correto; toleration exata; réplicas ocupam dois nodeNames distintos; topology constraint existe | JSONPath + conjunto distinto + contagem |
| 10 | PVC `Bound`; StatefulSet com réplicas Ready; Headless Service com `clusterIP=None`; marcador presente no volume do ordinal 0 | condição + JSONPath + exec read-only |
| 11 | Job tem condition `Complete=True`; limites de retry/deadline definidos; CronJob usa `Forbid`, mantém histórico limitado e não tem Jobs ativos sobrepostos | condição + JSONPath + contagem |
| 12 | `auth can-i get configmap/<nome>` é `yes`; `auth can-i get secrets` é `no`; namespace aplica `restricted`; workload Ready e securityContext cumpre o perfil | RBAC booleana + JSONPath + condição |
| 13 | default-deny existe; DNS do frontend funciona; frontend→api sucede; intruso→api falha dentro do timeout; nenhuma regra `allow-all` existe | inventário + conectividade positiva/negativa |
| 14 | API de métricas disponível; HPA `AbleToScale=True` e `ScalingActive=True`; carga controlada eleva réplicas acima do mínimo; PDB tem budget coerente; réplicas estão em dois nodes e ambos estão schedulable ao final | condição + polling + contagem + conjunto distinto |

### Vocabulário de Asserções recomendado

1. `kubernetes_condicao`
   - `recurso`, `nome`, `condicao`, `status`, `namespace`, `timeout`.
2. `kubernetes_jsonpath`
   - comparação tipada `igual`, `contem`, `maior_que`, `menor_que`, `vazio` ou
     `nao_vazio`; não comparar a tabela humana de `kubectl get`.
3. `kubernetes_contagem`
   - recurso + label/field selector + operador/valor.
4. `kubernetes_rbac`
   - subject, verb, resource, optional resourceName e resultado booleano esperado,
     implementado com `kubectl auth can-i`.
5. `kubernetes_conectividade`
   - origem preexistente, destino, protocolo, timeout e `resultado: permite|nega`.
     A Verificação não deve criar Pods de debug.
6. `http_responde` com destino host e timeout, reutilizado no Cenário 05.

Todos recebem kubeconfig/contexto da plataforma, não do conteúdo. A descrição exibida
ao leitor deve dizer **qual propriedade está errada**, sem imprimir comando que entrega
a solução nem dados de Secret.

### Regras para evitar falso positivo e flakiness

- Recriar o namespace e aguardar sua remoção terminar antes de aplicar fixtures.
- Fazer `kubectl wait`/poll de condição com timeout, nunca `sleep` fixo.
- Diferenciar `not found`, timeout e estado reprovado; são diagnósticos diferentes.
- Toda chamada de rede deve ter timeout de conexão e de operação.
- Testar negação como negação: timeout/connection refused esperado precisa ser
  representado por uma Asserção própria. `comando_produz` não basta.
- Verificações devem ser observadoras. Não criar volume, apagar Pod, drenar node,
  escalar Deployment nem disparar Job para “testar”. Ações destrutivas pertencem ao
  exercício ou à preparação declarada.
- Saída de `kubectl` deve ser JSON/JSONPath; texto tabular muda com versão e locale.
- A invariante inicial correta é: **a Verificação geral falha antes da solução**.
  Não exigir que todas as Asserções falhem: em ambientes quebrados, cluster saudável,
  DNS funcionando ou alguns Pods Ready são âncoras diagnósticas úteis.

## Impacto no modelo atual da plataforma

O working tree já introduz `contextoKubernetes`, `namespaceKubernetes` e
`manifestosIniciais`, recriando o namespace e aplicando fixtures ao iniciar. Essa é a
direção correta, com quatro reforços:

1. **Kubeconfig isolado como dado da plataforma:** acrescentar o caminho absoluto a
   toda execução; não aceitar esse caminho no frontmatter de conteúdo.
2. **Fixtures namespaced:** `manifestosIniciais` deve rejeitar objetos cluster-scoped.
   O flag `--namespace` não torna `Node`, `PersistentVolume`, `ClusterRole`,
   `StorageClass` ou CRD namespaced. Kubernetes documenta que nem todos os objetos
   pertencem a namespace. [Namespaces](https://kubernetes.io/docs/concepts/overview/working-with-objects/namespaces/)
3. **Estado base no cluster config/bootstrap:** labels e taints estáveis dos workers,
   CNI, StorageClass e metrics pipeline opcional pertencem ao perfil compartilhado,
   não a um Cenário. Isso evita mutação cluster-scoped entre aulas.
4. **Inventário de limpeza:** qualquer exceção cluster-scoped inevitável deve carregar
   label `learning-infra.dev/scenario=<id>`, estar numa allowlist e ser removida
   explicitamente. Não executar hooks arbitrários vindos do conteúdo.
5. **Política do namespace tipada:** quando um Cenário precisar de Pod Security,
   expor um campo fechado como `politicaPod: restricted` e fazer o gerenciador aplicar
   somente os labels conhecidos durante a criação. Não aceitar mapas de labels nem
   manifestos de Namespace arbitrários.

O frontmatter proposto para um Cenário comum permanece pequeno:

```yaml
contextoKubernetes: kind-learning-infra
namespaceKubernetes: li-k8s-04
manifestosIniciais: bootstrap
```

O motor deve validar namespace pelo padrão `li-k8s-[0-9]{2}` e contexto por igualdade
exata. O cluster é compartilhado para evitar minutos de recriação a cada Cenário;
`kind delete cluster --name learning-infra` fica numa ação explícita de reset, não no
teardown normal. A remoção de cluster inexistente é idempotente segundo o guia do
`kind`. [kind Quick Start](https://kind.sigs.k8s.io/docs/user/quick-start/)

## Riscos e mitigação

| Risco | Consequência | Mitigação |
|---|---|---|
| Contexto/kubeconfig errado | Preparação ou teardown atinge cluster real | kubeconfig isolado, contexto constante, checagem de nome/nodes antes de toda mutação |
| Manifesto cluster-scoped em fixture | Cenário deixa lixo ou altera todos os Cenários | bloquear kinds cluster-scoped; addons só no bootstrap; allowlist rotulada para exceções |
| Namespace preso em `Terminating` | próximo início falha ou vê estado velho | timeout com diagnóstico de finalizers; nunca seguir aplicando após teardown incompleto |
| Secret real em YAML/log/checklist | vazamento para Git, UI ou processo | apenas valores sintéticos; nunca mostrar `.data`; checar referências/permissões, não conteúdo |
| Porta 8092 ocupada | cluster não cria ou HTTP atinge app alheia | preflight do owner da porta e loopback; reservar somente 8092/NodePort 30092 |
| IP dos nodes inacessível no Windows | teste funciona no cluster e falha no host | `extraPortMappings`; não usar node IP em Asserção host |
| OOM/carga sem limite | Docker Desktop ou outras aulas ficam instáveis | imagens e cargas pinadas, limits baixos, deadline e teardown; HPA com teto pequeno |
| CNI não aplica NetworkPolicy | Cenário 13 aprova configuração sem isolamento real | smoke test comportamental no bootstrap e conectividade negativa na Verificação |
| StorageClass ausente ou dependente de internals do kind | PVC permanece Pending | provisioner pinado como addon e smoke test `Bound`; não depender de detalhe não suportado da node image |
| metrics-server/certificados/timing | HPA mostra `<unknown>` e fica flaky | gate de cinco clusters limpos; polling; tornar #14 condicional |
| Imagem/tag mutável ou registry indisponível | ImagePullBackOff alheio à aula | tags/digests pinados, preload por `kind load`, `imagePullPolicy` explícita |
| Controller ainda convergindo | falso negativo intermitente | condições + polling com timeout; sem `sleep` fixo |
| Verificação muta ambiente | falso positivo em chamada seguinte | operações read-only; preparação e exercício separados da Verificação |
| PDB mal interpretado | falsa sensação de HA | ensinar que delete direto e rolling update não são governados pelo PDB; testar eviction separadamente |

## Roteiro de diagnóstico transversal

Todo Cenário deve reforçar a mesma sequência, acrescentando camadas quando necessário:

1. `kubectl get <recursos> -o wide` — inventário e estado alto nível.
2. `kubectl describe` — condições e Events daquele objeto.
3. `kubectl get events --sort-by=.lastTimestamp` — ordem dos acontecimentos.
4. `kubectl logs`, e `kubectl logs --previous` quando houve restart.
5. `kubectl get ... -o yaml/jsonpath` — comparar `spec`, `status`, selectors,
   ownerReferences e lastState.
6. Para tráfego: DNS → Service → EndpointSlices → readiness → Pod direto → policy.
7. Para Pending: Events → requests/allocatable → selectors/affinity →
   taints/tolerations → PVC.

`CrashLoopBackOff` é um estado/condição observável do loop de falha com backoff, não a
causa; logs, configuração, recursos e probes ainda precisam ser investigados.
[Pod Lifecycle](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/)

## Ordem de implementação sugerida

1. Guardrails de kubeconfig/contexto e bootstrap do cluster pinado.
2. Asserções estruturadas + testes do motor, incluindo negação e polling.
3. Cenários 01–04 e smoke test completo do ciclo iniciar/verificar/teardown.
4. Porta 8092 e Cenário 05.
5. Cenários 06–09.
6. Add-on/storage smoke test e Cenário 10.
7. Cenários 11–12.
8. CNI/enforcement smoke test e Cenário 13.
9. metrics pipeline; publicar o 14 somente se passar o gate.

Cada Cenário só está pronto quando:

- iniciar duas vezes produz o mesmo ambiente;
- a Verificação geral falha antes da solução, com pelo menos um diagnóstico útil;
- o caminho didático conclui sem comandos ocultos;
- trocar de Cenário remove o namespace anterior;
- iniciar novamente depois de concluído volta a reprovar;
- nenhuma Asserção depende de tabela humana, current-context, IP aleatório ou timing
  sem timeout;
- testes automatizados cobrem leitura do conteúdo, lifecycle, falha inicial e sucesso
  final contra cluster real.
