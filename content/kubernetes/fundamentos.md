# Fundamentos do Kubernetes

Docker resolve o empacotamento e a execução de um container. Kubernetes resolve o que
acontece quando você precisa de muitos containers, em mais de uma máquina, com a
expectativa de que eles continuem no ar sem intervenção manual. Antes de decorar
objetos e comandos, vale construir o modelo mental: quem guarda a intenção, quem
executa essa intenção e o que acontece quando a realidade diverge dela.

Esta leitura prepara os Cenários práticos. Ela não substitui o terminal: ao terminar,
faça o Questionário para descobrir o que revisar e siga para os laboratórios mesmo que
ainda não tenha atingido 80%. A recomendação não é um bloqueio.

## O problema: containers em escala pedem um plano

Com Docker puro, cada decisão operacional é sua: em qual máquina o container roda, o
que acontece quando o processo morre às três da manhã, como uma segunda réplica
encontra a primeira, para onde vai o tráfego enquanto uma versão nova sobe. Em uma
máquina só, essas decisões cabem em alguns scripts. Em dez máquinas e cinquenta
serviços, elas viram um sistema distribuído improvisado — e improvisado mal.

Um orquestrador assume esse plano: você declara quantas réplicas devem existir, de qual
imagem, com quais recursos e regras, e o sistema se encarrega de colocar cada workload
em algum Node, reiniciar o que morre e redistribuir o que ficou órfão quando uma
máquina falha. A mudança de postura é o ponto central: deixar de comandar cada
execução e passar a descrever um estado que o cluster mantém.

## Estado desejado e reconciliação

Quase tudo no Kubernetes gira em torno de uma ideia: **objetos são registros de
intenção**. Ao aplicar um manifesto, você não liga processos — você grava no cluster um
estado desejado (`spec`). O cluster registra separadamente o estado observado
(`status`) e trabalha para aproximar um do outro.

Esse trabalho é feito por **controllers**, cada um responsável por um tipo de objeto e
executando sempre o mesmo laço:

```diagrama
tipo: ciclo
visual: reconciliacao
titulo: Observar → comparar → agir
passos:
  - titulo: observar
    detalhe: estado atual
  - titulo: comparar
    detalhe: estado desejado
  - titulo: agir
    detalhe: corrigir a diferença
retorno: repete enquanto o objeto existir
```

A consequência prática é profunda: apagar um Pod gerenciado não resolve um problema,
porque o controller recria outro segundos depois. Mudanças duram quando você altera o
estado desejado — o manifesto — e não quando edita a realidade à mão. `kubectl edit`
num Pod solto parece funcionar; a próxima reconciliação desfaz o gesto. Também por isso
sintomas como `CrashLoopBackOff` são **estados observados**, não diagnósticos: o laço
registra que o processo morre e reinicia com backoff; a causa continua sendo sua
investigação — configuração, imagem, recursos ou probes.

## A arquitetura do cluster

Um cluster tem dois lados. O **control plane** guarda e decide; os **Nodes** executam.
Toda interação passa por uma única porta: o **API server**. O `kubectl`, os controllers
e os agentes dos Nodes conversam com ele — não entre si.

```diagrama
tipo: fluxo
visual: arquitetura-cluster
titulo: Da API ao Pod
passos:
  - titulo: kubectl
    detalhe: envia o manifesto
  - titulo: API server
    detalhe: valida e registra a intenção
    lateral:
      titulo: etcd · scheduler · controller manager
      detalhe: guarda, agenda e reconcilia
      tom: destaque
  - titulo: kubelet
    detalhe: materializa o Pod no Node
    lateral:
      titulo: container runtime · kube-proxy
      detalhe: executam containers e programam a rede
  - titulo: Pod
    detalhe: processos, rede e volumes compartilhados
    tom: destaque
legenda: A API é o ponto de encontro; os componentes observam e atualizam o estado por ela.
```

O **scheduler** escolhe em qual Node cada Pod cabe; o **kubelet** daquele Node garante
que os containers declarados existam de verdade; o **etcd** guarda todo o estado do
cluster. Na prática, essa separação explica dois comportamentos que confundem
iniciantes: um Pod pode ficar `Pending` sem nenhum container ter sido criado (o
scheduler ainda não encontrou Node) e um objeto pode existir na API sem que nada esteja
rodando (a intenção foi aceita, a execução ainda não convergiu).

## Objetos: do Deployment ao Pod

Um **Pod** é a unidade atômica: um ou mais containers que compartilham endereço de
rede e volumes, agendados juntos em um único Node. Pods são descartáveis por desenho —
nascem com IP novo, morrem sem cerimônia. Por isso quase ninguém cria Pods nus em
produção: usa-se um objeto de workload que os gerencia.

O caminho mais comum tem três elos, ligados por **labels** e **selectors**:

```diagrama
tipo: fluxo
visual: hierarquia-workload
titulo: Deployment → ReplicaSet → Pods
passos:
  - titulo: Deployment
    detalhe: define template, réplicas e rollout
  - titulo: ReplicaSet
    detalhe: mantém a contagem atual
    lateral:
      titulo: labels e selectors
      detalhe: ligam intenção e Pods
      tom: destaque
  - titulo: Pods
    detalhe: execuções substituíveis
    tom: sucesso
legenda: Altere o Deployment; os objetos abaixo são consequência.
```

Um **namespace** divide o cluster em escopos lógicos — é dentro de um namespace que
seus objetos vivem nos Cenários desta Trilha. E todo objeto carrega, além de `spec`, um
`status` com conditions e uma trilha de **eventos**: quando algo não converge, a ordem
de leitura é `kubectl get` para o inventário, `kubectl describe` para conditions e
eventos, `kubectl logs` para o processo — nesta sequência, antes de abrir um shell.

## Rede: Services, DNS e endpoints

Cada Pod recebe um IP efêmero, que morre com ele. Confiar nesses endereços é acoplar
seu sistema a algo que a reconciliação pode trocar a qualquer momento. O **Service**
existe exatamente para isso: um nome DNS estável e um IP virtual que encaminha para os
Pods que combinam com seu selector.

O mecanismo importa mais que o nome: o Service não "contém" Pods. Um controller observa
o selector e mantém objetos **EndpointSlice** com os IPs dos Pods que combinam e estão
prontos. Se o selector erra uma label, a lista fica vazia e o Service resolve, mas não
tem para quem mandar — o clássico "Service existe e ninguém responde".

```diagrama
tipo: fluxo
visual: service-endpoints
titulo: DNS → Service → endpoints
passos:
  - titulo: Pod cliente
    detalhe: chama api pelo nome
  - titulo: DNS do cluster
    detalhe: resolve o Service
  - titulo: Service
    detalhe: encaminha sem executar a aplicação
    lateral:
      titulo: EndpointSlice
      detalhe: lista Pods selecionados e prontos
      tom: destaque
  - titulo: Pod de destino
    detalhe: recebe a conexão
    tom: sucesso
legenda: Readiness remove o Pod dos endpoints sem reiniciá-lo.
```

O tipo padrão, `ClusterIP`, só existe dentro do cluster. Para alcançar um Service de
fora — do seu navegador, por exemplo — usa-se `NodePort`, que abre uma porta em todos
os Nodes. No cluster local desta Trilha, é esse mecanismo que liga o `127.0.0.1` da sua
máquina a um workload lá dentro.

## Agendamento e recursos

O scheduler decide onde cada Pod roda, e a moeda dessa decisão são os **requests**:
quanto de CPU e memória você declara que o container precisa. Um Pod que pede mais
memória do que qualquer Node tem livre fica `Pending` para sempre — não há erro, apenas
nenhum lugar onde ele caiba. **Limits** são a outra metade do contrato: o cgroup do
Node os impõe em execução. Estourar o limite de memória termina em `OOMKilled`;
estourar CPU resulta em throttling, não em morte.

Do par request/limit nasce a classe de qualidade do Pod (`Guaranteed`, `Burstable`,
`BestEffort`), que decide quem é despejado primeiro quando o Node fica sem recursos.
E quando a topologia importa — este workload precisa de um Node específico, estas
réplicas não podem cair juntas — entram node selectors, taints e tolerations,
affinities e regras de distribuição. Todos são filtros e preferências de agendamento;
nenhum move um Pod que já está rodando.

## Configuração sem reconstruir a imagem

Uma imagem que muda a cada ambiente é um artefato quebrado: o mesmo build deve rodar em
qualquer namespace, variando apenas a configuração. **ConfigMaps** guardam valores não
confidenciais; **Secrets** guardam credenciais. Ambos entram no Pod como variáveis de
ambiente ou arquivos montados.

Duas armadilhas merecem atenção. Primeira: o valor de um Secret em YAML é base64, e
base64 é codificação, não criptografia — qualquer pessoa com leitura na API recupera o
texto claro. Por isso os Cenários usam somente valores sintéticos. Segunda: variável de
ambiente é lida na criação do processo; editar o ConfigMap não muda o processo que já
roda. A correção declarativa é provocar um novo rollout — Pods novos, configuração nova
— e não editar nada em execução.

## Saúde de verdade: probes

"O processo existe" é a afirmação mais fraca que um cluster pode fazer. As **probes**
declaram checagens reais e cada uma controla uma consequência diferente: a *startup*
segura as demais até a aplicação terminar de subir; a *liveness* falhando reinicia o
container; a *readiness* falhando tira o Pod dos EndpointSlices — vivo, mas fora do
tráfego.

Esse trio explica sintomas que parecem contraditórios: um Pod `Running` que não recebe
requisições (readiness falhando), um container que reinicia em loop minutos depois de
"funcionar" (liveness cedo demais numa app lenta), um rollout que nunca termina (Pods
novos nunca ficam prontos). Uma probe rasa — que responde 200 sem tocar a dependência
crítica — produz confiança falsa, o mesmo defeito de um healthcheck superficial no
Compose.

## Persistência e identidade estável

O filesystem do container é tão descartável quanto o Pod. Para estado que sobrevive,
o workload pede um **PersistentVolumeClaim**: "preciso de tanto espaço, com este modo
de acesso". O cluster faz a ligação com um **PersistentVolume** — provisionado
dinamicamente por uma **StorageClass** — e o dado passa a viver fora do ciclo de vida
do Pod.

Bancos e sistemas com estado precisam de mais: identidade estável. O **StatefulSet**
nomeia os Pods com ordinais previsíveis (`app-0`, `app-1`), dá a cada um seu próprio
PVC via `volumeClaimTemplates` e os endereça por um Service headless. Apagar o Pod
`app-0` traz de volta um novo `app-0`, com o mesmo volume e o mesmo nome — o oposto do
descarte anônimo do Deployment. Note que remover o StatefulSet não remove os volumes:
apagar dados é uma decisão separada, deliberada.

## Segurança: identidade e mínimo privilégio

Todo Pod fala com a API como alguém: a **ServiceAccount** montada nele. O que essa
identidade pode fazer é decidido pelo **RBAC** — Roles listam verbos sobre recursos
(`get` em `configmaps`, por exemplo), RoleBindings amarram a Role à identidade. A
pergunta de diagnóstico é sempre a mesma e tem comando próprio:
`kubectl auth can-i get configmap/<nome>` responde `yes` ou `no` antes de você perder
tempo com outras hipóteses.

A segunda frente é o que o próprio Pod pode ser. O **Pod Security Admission** aplica
perfis por namespace; o perfil `restricted` exige usuário não-root, capabilities
restritas e bloqueio de escalação de privilégio — a mesma disciplina do Cenário de
Docker, agora imposta pelo cluster e não pela sua atenção. A regra que atravessa as
duas frentes é a de sempre: conceda o mínimo que o workload precisa, e nada além.

## O cluster local desta Trilha

Os Cenários rodam no cluster **kind** provisionado pelo Docker Desktop: um control
plane e dois workers executando como containers na VM do Docker. Dois workers não são
luxo — o Cenário de agendamento precisa de dois Nodes que aceitem workloads para que
distribuição e afinidade sejam exercícios reais, e não teoria.

Dois guardrails mantêm o laboratório seguro. Primeiro: todos os comandos e Verificações
fixam o contexto `docker-desktop` explicitamente, para que nenhum exercício atinja
outro cluster que esteja selecionado no seu `kubectl`. Segundo: cada Cenário vive num
namespace próprio `learning-infra-k8s-*`, recriado ao Iniciar e removido ao trocar de
Cenário — a mesma invariante de um ambiente por vez que você já conhece do Docker.
Objetos com escopo de cluster ficam de fora do ciclo de limpeza, de propósito.

## Da teoria aos Cenários

Os Cenários seguintes transformam o modelo mental em evidência no seu cluster:

- **01 — O primeiro objeto no cluster:** manifesto, `spec`/`status` e o ciclo de diagnóstico;
- **02 — Reconciliação e Pods descartáveis:** estado desejado e o controller que traz de volta;
- **03 — Um Service que não encontra ninguém:** selector, labels e EndpointSlice vazio;
- **04 — Configuração sem reconstruir imagem:** ConfigMap, Secret e rollout consciente;
- **05 — Vivo, mas fora do tráfego:** probes e o EndpointSlice que esvazia;
- **06 — O Pod que nenhum Node aceita:** requests, limits e o scheduler sem candidatos;
- **07 — Uma versão que nunca chega:** rollout travado, histórico e rollback;
- **08 — Estado que sobrevive ao Pod:** PVC, StorageClass e a recriação do dado;
- **09 — Um Job que insiste em falhar:** retry, deadline e idempotência;
- **10 — Identidade e processo com o mínimo necessário:** ServiceAccount, RBAC e Pod Security;
- **11 — Réplicas que não podem cair juntas:** taints, tolerations e distribuição entre Nodes;
- **12 — Identidade estável antes de escala:** StatefulSet, ordinal e volume por réplica;
- **13 — O mesmo manifesto em ambientes diferentes:** Kustomize, bases e overlays;
- **14 — O incidente das duas verdades:** o diagnóstico em que duas camadas parecem culpadas.

Use o Questionário como recuperação ativa: responda sem procurar no texto, veja o
feedback e volte apenas às seções indicadas. Depois, abra o terminal. Compreensão sem
prática é frágil; prática sem modelo mental vira tentativa e erro.

### Referências para continuar

- [Componentes do Kubernetes](https://kubernetes.io/docs/concepts/overview/components/)
- [Objetos no Kubernetes](https://kubernetes.io/docs/concepts/overview/working-with-objects/)
- [Controllers](https://kubernetes.io/docs/concepts/architecture/controller/)
- [Service](https://kubernetes.io/docs/concepts/services-networking/service/)
- [Probes de liveness, readiness e startup](https://kubernetes.io/docs/concepts/workloads/pods/probes/)
- [Gerenciamento de recursos para Pods e containers](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)
