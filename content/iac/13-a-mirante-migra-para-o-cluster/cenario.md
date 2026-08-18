---
id: iac/13-a-mirante-migra-para-o-cluster
titulo: A Mirante migra para o cluster
dificuldade: guiado
terraform: true
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-iac-13
---
# A Mirante migra para o cluster

A Mirante fechou contrato com uma rede de trinta lojas. Na semana do cadastro, o sistema
de estoque — um container `mirante-web` num host só — passou a cair toda vez que a rede
cadastrava a promoção do dia. Um container num host só deixou de ser resposta: sem
réplicas para dividir a carga, sem o cluster para recriar o que cai, sem como tirar o
serviço daquela máquina para manutenção sem derrubar trinta lojas junto.

A resposta é o cluster — e o leitor já o conhece: a Trilha Kubernetes inteira rodou num
cluster de três Nodes no Docker Desktop. O que muda neste Cenário não é o destino, é quem
escreve os objetos. Até aqui, a Trilha IaC descreveu containers e o Terraform falava com
o daemon do Docker. A partir daqui, o mesmo HCL descreve objetos de Kubernetes —
Namespace, Deployment, Service — e o Terraform passa a falar com o API server.

O diretório de trabalho chega com o `versions.tf` — agora pedindo o provider
`hashicorp/kubernetes` — e um `main.tf` com um único recurso pronto: o namespace
`learning-infra-iac-13`. Deployment e Service são o exercício.

## O mesmo HCL, outro provider

A ideia central deste ato cabe numa frase: nada na linguagem muda. Muda o tradutor.

Um `resource` continua sendo um `resource`. Blocos, argumentos, referências entre
endereços, o plano antes do apply — a gramática que os doze Cenários anteriores
construíram atravessa intacta. Compare os dois lados:

```hcl
resource "docker_container" "web" {
  # Ato I: o provider falava com o daemon do Docker
}

resource "kubernetes_deployment_v1" "web" {
  # agora: o provider fala com o API server do cluster
}
```

A diferença mora no provider. O `kreuzwerker/docker` traduzia cada recurso em chamadas ao
daemon que gerencia os containers da máquina. O `hashicorp/kubernetes` traduz o mesmo
bloco em chamadas à API do cluster — registra o objeto declarado, lê de volta o que o
cluster respondeu e grava no state. O mundo do outro lado mudou de forma; o código do
lado de cá não muda nenhuma.

E os argumentos seguem a forma do objeto de Kubernetes: `metadata` para a identidade,
`spec` para o estado desejado — a estrutura que o leitor escreveu em YAML a Trilha
Kubernetes inteira. Aqui o leitor a escreve em HCL.

## Por que o contexto é fixo

Abra o `versions.tf` que veio no diretório de trabalho. O bloco do provider traz dois
argumentos:

```hcl
provider "kubernetes" {
  config_path    = "~/.kube/config"
  config_context = "docker-desktop"
}
```

O `config_path` diz onde está a configuração do `kubectl`. O `config_context` diz **qual
cluster** dessa configuração usar — e não é preferência, é fronteira.

Sem o contexto fixo, o Terraform aplica no cluster que estiver **selecionado** no
`kubectl` naquele momento. Um `kubectl config use-context` esquecido — talvez digitado
ontem, para olhar o cluster de outra conta — e o próximo `terraform apply` desta Trilha
bateria naquele cluster. Sem mensagem de erro, sem passo a mais: só o alvo errado. Essa é
uma das formas mais rápidas de aplicar em produção sem querer.

A Trilha Kubernetes estabeleceu a disciplina pelo outro lado: todo `kubectl` fixa
`--context docker-desktop`, e as Verificações daquela Trilha exigem o contexto explícito
pelo mesmo motivo. Aqui a disciplina muda de casa — o contexto deixa de ser um argumento
digitado a cada comando e passa a morar no código, versionado e revisado junto com o
resto. O Cenário 12 deixou a regra: o que está no código é o que a revisão vê.

## Declare o Deployment

Sendo `Guiado`, o HCL vem inteiro. Acrescente ao `main.tf`, abaixo do namespace:

```hcl
resource "kubernetes_deployment_v1" "web" {
  metadata {
    name      = "mirante-web"
    namespace = kubernetes_namespace.mirante.metadata[0].name
  }

  spec {
    replicas = 3

    selector {
      match_labels = {
        app = "mirante-web"
      }
    }

    template {
      metadata {
        labels = {
          app = "mirante-web"
        }
      }

      spec {
        container {
          name  = "web"
          image = "nginx:1.27-alpine"

          port {
            container_port = 80
          }
        }
      }
    }
  }
}
```

Duas coisas merecem atenção antes de aplicar.

A primeira é o `metadata.namespace`. O Deployment não declara o namespace em texto solto:
referencia `kubernetes_namespace.mirante.metadata[0].name` — o recurso que já veio no
`main.tf`. É a mesma construção de `image = docker_image.web.image_id` do Cenário 01, e o
efeito é o mesmo: o Terraform lê a referência, conclui que o namespace precisa existir
antes do Deployment e desenha sozinho a aresta do grafo de dependências. Ninguém escreveu
a ordem — a ordem nasceu da referência.

A segunda é o `spec.template`. O leitor já escreveu esse bloco, em YAML: é o **template
de Pod** — labels no `metadata`, containers na `spec` — o mesmo que animou a reconciliação
da Trilha Kubernetes. O `selector` precisa casar com as labels do template, e é por isso
que os dois dizem `app = "mirante-web"`: o Deployment só gerencia os Pods que seleciona.

## Declare o Service

O Deployment cria as réplicas; falta publicá-las. Acrescente ao `main.tf`:

```hcl
resource "kubernetes_service_v1" "web" {
  metadata {
    name      = "mirante-web"
    namespace = kubernetes_namespace.mirante.metadata[0].name
  }

  spec {
    type = "NodePort"

    selector = {
      app = "mirante-web"
    }

    port {
      port        = 80
      target_port = 80
      node_port   = 30070
    }
  }
}
```

O `selector` entrega ao Service os Pods com a label `app = "mirante-web"` — os mesmos do
template do Deployment. A porta 80 do Service entrega ao `target_port` 80 de cada Pod, e o
`node_port = 30070` abre essa porta em **todos os Nodes** do cluster: qualquer Node,
perguntado na 30070, alcança o serviço.

Por que `NodePort` e não `LoadBalancer`? Porque `LoadBalancer` depende de um controlador
de nuvem que provisione o balanceador de verdade — coisa que um cluster local não tem.
Num cluster de laboratório, um Service `LoadBalancer` fica pendente para sempre. O
`NodePort` é a publicação que o cluster local sustenta sozinho — e, no cluster do Docker
Desktop, a porta do Node chega ao `localhost`. O leitor vai conferir no navegador.

## Aplique e confira dos dois lados

```powershell
terraform init
terraform apply
kubectl --context docker-desktop -n learning-infra-iac-13 get deployment,service
```

O `init` baixa o provider `hashicorp/kubernetes` e registra o digest no lockfile. O
`apply` mostra o plano — namespace, Deployment e Service — e pede confirmação. Digite
`yes`.

O `kubectl get` mostra os objetos que o Terraform acabou de criar. Agora olhe pelo outro
lado:

```powershell
terraform state list
```

O mesmo objeto aparece nas duas listas: `kubernetes_deployment_v1.web` no state,
Deployment `mirante-web` no cluster. São duas visões do mesmo fato — uma pela intenção
declarada, outra pela realidade do cluster. E o leitor não escolheu entre Terraform e
Kubernetes: usou os dois, cada um no seu papel. O HCL declarou, o API server registrou, e
os controllers do cluster fizeram o resto — três réplicas, distribuídas nos Nodes,
reconciliadas para sempre.

Confirme no navegador: <http://localhost:30070>.

## O que o Terraform não faz aqui

Os Fundamentos chamaram isso de o mesmo laço em cadências diferentes, e agora o leitor
está com os dois na mesma sala. O controller do Deployment reconcilia sozinho e para
sempre: um Pod apagado à mão volta sozinho, sem que ninguém peça. O Terraform reconcilia
quando mandam — `plan`, `apply`, e nada entre um comando e o próximo.

Os dois laços convivem no mesmo objeto. E a convivência tem uma rachadura: se a próxima
pessoa de plantão editar o Deployment com `kubectl`, o cluster aceita, o controller
obedece na hora — e o state do Terraform envelhece em silêncio, sem reclamar de nada. O
que os Fundamentos prometeram como diferença de cadência vira, aqui, uma pergunta
concreta: com dois sistemas prontos para agir no mesmo objeto, **quem é o dono dele?**
É a pergunta do próximo Cenário.

## Verificação

Cinco Asserções. As três primeiras conferem o mundo real: o Deployment `mirante-web`
disponível com as três réplicas, o Service publicando a porta 30070 e
<http://localhost:30070> respondendo. A quarta confere a origem:
`kubernetes_deployment_v1.web` no state, com o `name` `mirante-web` — o Deployment nasceu
do Terraform, e não de um `kubectl apply` à mão. A quinta roda um `plan` e só aprova
quando o código descreve os objetos que estão no cluster.

O caminho errado não conta. Crie os mesmos objetos com `kubectl`, sem rodar o Terraform,
e as três primeiras passam — mas a quarta reprova, porque o state continua vazio, e a
quinta também. E há um caso mais sutil, que o próximo Cenário desmonta por inteiro:
apagar o Deployment e recriá-lo com `kubectl` depois de tudo verde não toca no state — a
quarta continua passando — e é o plano limpo, sozinho, que acusa a troca.
