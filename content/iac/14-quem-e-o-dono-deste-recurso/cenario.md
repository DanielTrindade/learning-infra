---
id: iac/14-quem-e-o-dono-deste-recurso
titulo: Quem é o dono deste recurso?
dificuldade: assistido
terraform: true
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-iac-14
---
# Quem é o dono deste recurso?

Duas pessoas mexeram no mesmo Deployment na mesma tarde: uma por `terraform apply`, outra
por `kubectl edit`. As duas disseram que funcionou. Uma das duas estava errada — e não sabia.

O Cenário 13 deixou a pergunta no ar. O controller do Deployment reconcilia sozinho e para
sempre; o Terraform reconcilia quando mandam. Os dois laços dos Fundamentos — o mesmo, em
cadências diferentes — convivem no mesmo objeto, e cada um escreve de um jeito. Quando duas
pessoas escrevem no mesmo recurso por caminhos diferentes, quem vence? Este Cenário responde
que a pergunta certa não é "quem é o dono do recurso", e sim **"quem é o dono de cada campo"**
— e que nem tudo o que parece divergência é drift.

O diretório de trabalho chega **completo e aplicável**: o namespace, um ConfigMap
`mirante-config` com a chave `mensagem` valendo `v1`, e o Deployment `mirante-web` com três
réplicas montando o ConfigMap como volume. O exercício deste Cenário não é escrever — é
diagnosticar. Abra o `main.tf` e leia o que veio pronto antes de aplicar.

## Aplique e crie o conflito

```powershell
terraform init
terraform apply
```

O `init` baixa o provider e o `apply` cria os três recursos. Confira pelos dois lados:

```powershell
kubectl --context docker-desktop -n learning-infra-iac-14 get deployment mirante-web
terraform state list
```

Namespace, ConfigMap e Deployment no cluster; namespace, ConfigMap e Deployment no state. O
triângulo dos Fundamentos em sincronia — por enquanto. Agora a segunda pessoa da tarde de
abertura entra em cena. Edite **por fora**, como quem resolve no braço:

```powershell
kubectl --context docker-desktop -n learning-infra-iac-14 scale deployment mirante-web --replicas=5
kubectl --context docker-desktop -n learning-infra-iac-14 get deployment mirante-web
```

O `scale` mudou o Deployment sem passar pelo código. O cluster obedeceu na hora: cinco
réplicas, o controller feliz. Agora pergunte ao Terraform se o mundo ainda combina com o
arquivo:

```powershell
terraform plan
```

O plano acusa. `mirante-web` tem cinco réplicas e o código declara três: `~ update in-place`
na contagem. Este é o **mesmo drift do Cenário 05**, em outro substrato. Naquele Cenário, a
mão de plantão parou um container do Docker e o state envelheceu em silêncio; aqui, a mão de
plantão escalou um Deployment e o resultado é idêntico — o mundo real mudou, o código não
sabia, e o `plan` é a régua que mede a distância. O triângulo não mudou de forma: mudou o
substrato onde o vértice "mundo real" vive.

## Onde Kubernetes é diferente

Este Cenário existe para ensinar uma distinção, e ela cabe num contraste com o Ato I. No
Docker, o mundo real só muda quando alguém o muda: um container não se reinicia sozinho, não
ganha réplica, não troca de imagem. A palavra "drift" descreve bem qualquer diferença, porque
toda diferença tem uma mão por trás — e essa mão foi proibida de tocar.

No Kubernetes, **um controller muda o mundo real o tempo todo — e legitimamente**. Olhe o
Deployment que você acabou de escalar: o campo que você escreveu foi `spec.replicas = 5`, mas
o objeto que o cluster guarda tem também `status.replicas`, `status.readyReplicas`,
`status.availableReplicas` — números que você nunca declarou. O controller do Deployment lê
o `spec`, cria Pods, espera ficarem prontos e escreve o `status` de volta. Cada um desses
campos muda sozinho, sem pedir licença, e a mudança é o **trabalho** do cluster, não uma
invasão.

É por isso que "diferença entre o objeto declarado e o objeto no cluster" não é sinônimo de
drift. Parte da diferença é o `status` — um campo que **nunca foi seu para declarar**, e que
o Terraform lê e guarda no state como leitura, não como intenção. A pergunta que separa drift
de trabalho é quem escreveu o campo: se a escrita veio do código, é reconciliação; se veio de
fora do código, é drift; se nunca coube no código, é `status`.

## Campos gerenciados

Como o cluster sabe quem escreveu o quê? Pergunte a ele. O objeto guarda um registro de
assinaturas — o `managedFields`:

```powershell
kubectl --context docker-desktop -n learning-infra-iac-14 get deployment mirante-web -o yaml | Select-String -Pattern "managedFields" -Context 0,12
```

Cada cliente que escreve num objeto deixa o nome do seu **field manager** — o Terraform, o
`kubectl`, o controller, o kubelet — junto do conjunto de campos que escreveu. Repare nos
nomes que aparecem e no que cada um reclama para si.

É isso que permite a coexistência que a tarde de abertura dramatiza. O servidor não aceita
qualquer escrita sobre qualquer campo: ele compara quem pediu contra quem já governa. Dois
clientes diferentes podem escrever no mesmo objeto sem se sobrescrever cegamente — cada um
fica dono dos campos que tocou. E quando um campo tem dois donos reivindicando, o conflito é
**explícito**, não silencioso: a escrita é recusada até alguém ceder ou forçar.

Repare no que o `terraform plan` leu da escala que você fez à mão. O Terraform reivindica
`spec.replicas` — é dele, o código o declara. A mão que escalou à mão tocou um campo que não
era dela. O `plan` não disse "o mundo está estranho": disse algo mais preciso — **o campo
`spec.replicas` que eu governo foi alterado para fora do meu código**. O conflito de
propriedade virou plano divergente.

## O ConfigMap que dispara rollout

Mudança de assunto, mesma pergunta. Abra o `main.tf` e troque o valor do ConfigMap: `mensagem`
de `v1` para `v2`. Depois aplique:

```powershell
terraform plan
terraform apply
```

**Antes de aplicar, preveja: os Pods do Deployment vão reiniciar?** Pense no que mudou no
template. Aplicou? Agora confira:

```powershell
kubectl --context docker-desktop -n learning-infra-iac-14 get pods -l app=mirante-web
```

A resposta honesta é: **depende de como o ConfigMap está referenciado** — e é por isso que a
pergunta vale um Cenário. O ConfigMap do workspace entra no Deployment como **volume**
montado no container. Volumes de ConfigMap atualizam o arquivo depois de um intervalo — o Pod
continua vivo, o arquivo muda por dentro. Por causa do ConfigMap, nenhum Pod novo nasce e
nenhum reinicia: o template não mudou, e o Deployment não tem motivo para trocar os Pods que
já existem.

Repare, porém, no que mais este `apply` carrega: a escala de cinco réplicas feita à mão
ainda está no mundo, e `spec.replicas` é um campo que o código governa. O plano mostra as
duas mudanças juntas — a do ConfigMap e a das réplicas — e é por isso que o número de Pods
muda neste apply. A mudança não é o ConfigMap disparando rollout: é o Terraform reconciliando
a réplica que a mão mexeu.

A técnica que garante o rollout existe, e ela muda o jogo de dono de campo. A ideia é amarrar
uma **anotação do template** ao conteúdo do ConfigMap: quando o dado muda, o valor da anotação
muda, o template muda, e o Deployment faz rollout de Pods novos — a mudança de configuração
vira mudança de template. O caminho que garante isso é conhecido na Trilha Kubernetes: o hash
do conteúdo no template. Num Deployment gerenciado por Terraform, a mesma costura existe — e
é você quem decide se muda o dado e torce, ou muda o dado e garante.

## Um recurso, dois donos

O ConfigMap acima tem um dono só: o `kubernetes_config_map_v1` que o declarou inteiro — nome,
namespace e o mapa de chaves. O que acontece quando dois times querem o mesmo ConfigMap, cada
um dono de uma fatia? Existe um recurso para isso, e o nome denuncia a diferença:
`kubernetes_config_map_v1_data`.

Enquanto `kubernetes_config_map_v1` governa o objeto inteiro, `kubernetes_config_map_v1_data`
governa **apenas as chaves que você declara** num ConfigMap que **já existe** — e o faz por
`server-side apply`, o mecanismo que o cluster usa para respeitar donos por campo. Você não
cria o ConfigMap com ele: adota uma fatia de um objeto que outra pessoa criou. É a ferramenta
para um time declarar "dessas dez chaves, esta é nossa" sem reivindicar as outras nove.

E é aí que a propriedade por campo cobra seu preço. Se outro field manager — outro
`kubernetes_config_map_v1_data`, ou um `kubectl apply` — já governa a mesma chave, o seu
`apply` **conflita**: o servidor recusa a escrita e aponta o dono atual. O Terraform responde
com o erro do conflito de propriedade, e o recurso expõe uma saída: `force = true`, que
sobrescreve o dono.

A saída é legítima — e é uma armadilha. `force = true` é a ferramenta certa quando o conflito
é de fronteira: seu time passou a ser dono daquela chave, o registro antigo precisa ceder, e
a decisão é sua. É armadilha quando vira atalho: `force = true` usado para **calar um
conflito real** não muda quem deveria ser o dono — apenas registra que o seu cliente venceu
por escrever por último. Na próxima reconciliação, o outro lado volta a reivindicar, e o
conflito vira um ping-pong que só o código para: ou um dos lados deixa de declarar a chave,
ou os dois combinam quem é dono. O `force` resolve a escrita; só a fronteira resolve a
disputa.

## Reconcilie

Volte ao terminal e pergunte de novo ao Terraform o que ele vê:

```powershell
terraform plan
```

Se o leitor seguiu o fio até aqui, o plano pode já sair limpo: o `apply` da seção anterior
reconciliou as réplicas que a mão escalou junto com a mudança do ConfigMap, porque os dois
campos pertencem ao código. O `apply` só tem uma coisa a fazer — e, se não sobrou nada,
ele não faz nada.

Para ver a reconciliação fazendo o trabalho dela, devolva o conflito ao mundo. A mão de
plantão voltou a mexer:

```powershell
kubectl --context docker-desktop -n learning-infra-iac-14 scale deployment mirante-web --replicas=5
terraform plan
```

O que sobrou de divergência — as réplicas que a mão escalou, qualquer edição feita por fora —
aparece aqui. Agora devolva o mundo ao código:

```powershell
terraform apply
```

O `apply` é a única escrita que reconcilia: escreve o mundo até ele coincidir com o arquivo.
Confira o Deployment e o plano final:

```powershell
kubectl --context docker-desktop -n learning-infra-iac-14 get deployment mirante-web
terraform plan
```

O plano limpo é o atestado: código, state e mundo real voltaram a coincidir. Os campos que o
código governa estão de volta ao código. O que sobrou de diferente no objeto — o `status` que
o controller escreve a cada segundo — é trabalho, não drift, e nunca foi seu para declarar.

## Verificação

Cinco Asserções conferem o desfecho. As três primeiras olham o mundo real: o Deployment
`mirante-web` disponível, com o número de réplicas **de volta ao que o código declara**, e o
ConfigMap já com a **configuração nova** (`mensagem = v2`) no cluster. A quarta confere a
origem: `kubernetes_config_map_v1.config` no state — o ConfigMap nasceu do Terraform, e não
de um `kubectl create` à mão. A quinta roda um `plan` e só aprova quando não sobrou
divergência entre o código e o cluster.

A prova de que estas Asserções enxergam drift é rápida de fazer e vale o cenário inteiro.
Com tudo verde, rode de novo o `scale` da mão de plantão:

```powershell
kubectl --context docker-desktop -n learning-infra-iac-14 scale deployment mirante-web --replicas=5
```

Verifique de novo. O Deployment continua disponível — o controller não deixaria outra coisa —
e o ConfigMap continua `v2`. Mas duas Asserções reprovam: a contagem de réplicas não é mais
`3`, e o plano deixa de ser limpo. A Verificação **vê** o drift que o campo de fora criou.
Depois, um `terraform apply` reconcilia e tudo volta a passar.
