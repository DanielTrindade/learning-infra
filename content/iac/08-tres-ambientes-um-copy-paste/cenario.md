---
id: iac/08-tres-ambientes-um-copy-paste
titulo: Três ambientes, um copy-paste
dificuldade: assistido
terraform: true
containers: [mirante-homologacao, mirante-producao]
---
# Três ambientes, um copy-paste

O contrato do cliente grande pede um terceiro ambiente. Hoje o serviço da Mirante vive em
dois — homologação na porta 8077, produção na 8078 — e o arquivo que os descreve carrega
uma dívida: dois blocos quase idênticos, separados por um copy-paste. Com o arquivo como
está, o terceiro ambiente significa copiar mais um bloco. E a próxima mudança — a versão
da imagem, um atributo novo — vira três edições que precisam ser idênticas, ou um bug que
só aparece em um ambiente.

Este Cenário é `Assistido`: você recebe o objetivo, as pistas e a forma dos comandos —
parte do caminho você descobre sozinho.

O diretório de trabalho chega com a dívida na mão: o `main.tf` com os dois blocos
copiados, aplicável como está. Coloque os dois ambientes no ar antes de refatorar — a
refatoração só faz sentido se existir algo para ela desfazer e refazer:

```powershell
terraform init
terraform apply
```

Confira no navegador: <http://localhost:8077> e <http://localhost:8078>. Os dois
respondem. Abra o `main.tf` e olhe de perto: são oito linhas repetidas — a mesma imagem,
a mesma publicação de porta, a mesma estrutura, mudando só o nome do container e o número
da porta. Este Cenário paga essa dívida com as duas ferramentas que o Terraform oferece
para isso: o módulo para escrever uma vez, e o `for_each` para chamar em laço.

## O que é um módulo

Um módulo é um diretório com arquivos `.tf` dentro. O que acontece lá dentro é detalhe de
implementação; o que importa é o contorno, e o contorno tem três peças. A `variable` é a
**entrada** — o valor que quem chama entrega. O `output` é a **saída** — o valor que o
módulo devolve para quem chamou. E o que estiver no meio — recursos, `local`, `data` — é
o corpo da função: quem chama não precisa conhecer.

A analogia que segura o resto deste Cenário: um módulo é uma **função**. Ele recebe
entradas, calcula, devolve saídas. E o `for_each` é **chamar essa função em laço** — uma
vez para cada item de uma coleção. O Cenário 03 já usou `variable`, `local` e `output`
num único arquivo; aqui a função ganha um diretório próprio e é chamada duas vezes a
partir de uma linha só.

## Extraia

O objetivo é devolver o serviço à forma de função: um módulo escrito uma única vez e
chamado de um único lugar. Como dica de degrau `Assistido`, aqui está a estrutura de
diretórios e as assinaturas — o conteúdo é com você:

```
workspace/
  main.tf
  modulos/
    servico/
      main.tf
```

O módulo `servico` recebe duas entradas e devolve uma saída:

- `ambiente` — uma `string` com o nome do ambiente;
- `porta` — um `number` com a porta publicada;
- `nome_do_container` — o `output` com o nome do container criado.

Mova o bloco `docker_container` para dentro do módulo, substituindo o que varia por
`var.ambiente` e `var.porta`. Uma pista: o módulo não alcança a raiz — quem vive dentro
do módulo não enxerga `docker_image.web`. Tudo o que o módulo precisa usar chega pela
porta de entrada; se algo não está nas entradas acima, ele precisa receber também.

## A armadilha que todo mundo cai

Um aviso antes de você criar o módulo, porque esta armadilha foi medida e é real: um
módulo filho que usa `docker_container` **sem declarar o seu próprio bloco
`required_providers`** faz o `init` procurar `hashicorp/docker` e falhar com *provider
registry does not have a provider named*.

A razão não é mágica: a herança de provider não é automática por nome de recurso. A
configuração da raiz declara que `docker` é `kreuzwerker/docker`; dentro do módulo filho,
essa associação não chega sozinha. Sem uma declaração própria, o Terraform assume que o
`docker` do módulo é o `hashicorp/docker` — um provider que **não existe** no registry —
e o `init` para aí, com o módulo apontado pelo erro.

A correção é um bloco `terraform` dentro do módulo, declarando o próprio
`required_providers` apontando `docker` para `kreuzwerker/docker`. Crie o módulo, rode o
`terraform init`, leia o erro com calma quando ele aparecer, adicione o bloco e rode de
novo — o `init` passa. A armadilha deixa uma lição para a vida: módulo é código separado,
e código separado declara as próprias dependências.

## for_each em vez de dois blocos

Agora a raiz. O `main.tf` perde os dois blocos copiados e passa a ter **um** bloco
`module` com `for_each` — um laço sobre um `local` que mapeia o nome do ambiente para a
porta:

```hcl
locals {
  ambientes = {
    homologacao = 8077
    producao    = 8078
  }
}
```

O módulo é chamado uma vez por item do mapa: `each.key` como nome do ambiente, `each.value`
como porta. O bloco `module` precisa se chamar **`ambiente`** — a Verificação deste Cenário
procura por esse nome, e não adivinha o que você escolheria. As chaves do `local` também
não são livres: `homologacao` e `producao`, exatamente como estão aqui. O `docker_image.web`
continua na raiz — a imagem é a mesma para os dois ambientes, e o módulo a recebe pronta.

Agora a pergunta que fecha a seção. Para o contrato que pediu o terceiro ambiente, quanto
custa acrescentá-lo hoje? Com o copy-paste, eram três edições que precisavam ser idênticas.
Com o `local`, é **uma linha**: `sandbox = 8079`. O terceiro ambiente passou a ser uma
linha. É o tamanho da dívida que este Cenário paga: o mesmo serviço, escrito uma vez,
chamado quantas vezes o contrato pedir.

## Os endereços mudaram

Depois de extrair o módulo, planeje antes de aplicar — e leia o plano sem se assustar:

```powershell
terraform plan
```

Extrair o módulo muda o **endereço** dos recursos, e para o Terraform o endereço é a
identidade. Os containers que viviam em `docker_container.homologacao` e
`docker_container.producao` agora vivem em `module.ambiente["homologacao"].docker_container.web`
e `module.ambiente["producao"].docker_container.web`. Endereço novo é recurso novo — e o
`apply` vai propor **destruir e recriar** os dois containers, mesmo que nada tenha mudado
no mundo real e os dois serviços continuem de pé.

Aceite o plano. Aqui a recriação é aceitável porque não há dado dentro dos containers: o
nginx é descartável, e o mundo real depois do apply é o mesmo que antes. O aviso fica para
quando houver dado: aí a ferramenta certa é o bloco `moved`, que ensina o Terraform a
reconhecer um recurso sob o endereço novo **sem** destruí-lo — o assunto do próximo
Cenário. Por ora, destruir e recriar é o preço justo de aprender o endereço novo.

Depois do apply, confira o state:

```powershell
terraform state list
```

## Ler um endereço indexado no PowerShell

A lista mostra os endereços novos com aspas embutidas:
`module.ambiente["homologacao"].docker_container.web` e
`module.ambiente["producao"].docker_container.web`. Para ler um deles por inteiro, as
aspas precisam ser escapadas para sobreviver ao PowerShell:

```powershell
terraform state show 'module.ambiente[\"producao\"].docker_container.web'
```

O `\"` dentro das aspas simples é a grafia que atravessa a linha de comando intacta —
sem o escape, o PowerShell desfaz as aspas e o Terraform responde que não conhece o
endereço. Confira os dois containers:

```powershell
terraform state show 'module.ambiente[\"homologacao\"].docker_container.web'
```

O `name` de cada um nasceu do módulo: `mirante-producao` e `mirante-homologacao`,
exatamente como o `local` manda. Antes de seguir, nomeie o que acabou de ler: o endereço
indexado por `["producao"]` é a assinatura do laço — quem era chamado em dois blocos
agora é chamado duas vezes.

## Verificação

Sete Asserções, e cada uma prova uma camada deste Cenário.

As quatro primeiras conferem o mundo real: os dois containers de pé, `mirante-homologacao`
e `mirante-producao`, respondendo em <http://localhost:8077> e <http://localhost:8078>.
Sem os dois ambientes no ar, não há o que verificar.

As duas seguintes conferem a origem, lendo o state. São as Asserções que dão nome ao
Cenário: no endereço `module.ambiente["producao"].docker_container.web` o `name` precisa
ser `mirante-producao`, e no endereço `module.ambiente["homologacao"].docker_container.web`
precisa ser `mirante-homologacao`. A descrição da primeira diz a lição: produção nasceu do
**módulo**, e não de um bloco copiado. Repare no que essas duas exigem de você: o bloco
`module` se chama `ambiente`, e as chaves do `local` se chamam `homologacao` e `producao` —
a Verificação não adivinha os nomes que você escolheria.

A última roda um `plan` e só aprova quando não há mudança pendente — o triângulo dos
Fundamentos inteiro em sincronia. É ela que cobra o `apply` da seção `Os endereços
mudaram`: se a recriação ficou só no plano, o código e o state divergem, e a Verificação
reprova até o `apply` acontecer.
