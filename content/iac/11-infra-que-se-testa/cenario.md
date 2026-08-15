---
id: iac/11-infra-que-se-testa
titulo: Infra que se testa
dificuldade: assistido
terraform: true
containers: [mirante-homologacao]
---
# Infra que se testa

O incidente começou com um dígito. Um `apply` em produção publicou o serviço da Mirante
na porta 8171 — a 8071 de sempre com um dígito trocado. Ninguém errou de digitação: o
valor veio de uma variável, atravessou `plan` e `apply` sem que ninguém checasse a
faixa, e o balanceador seguiu apontando para a 8071 enquanto o container subia saudável
numa porta onde ninguém o procurava. Para o monitor, o serviço tinha caído. Para o log,
estava tudo em ordem. O time levou quarenta minutos para entender que a infraestrutura
estava de pé — no endereço errado.

A pergunta que ficou não é "quem errou", porque ninguém errou. É por que um erro de um
dígito atravessou todas as barreiras sem ser barrado. Código de aplicação tem teste;
código de infraestrutura também pode ter — e este Cenário trata de escrever o dele.

Este Cenário é `Assistido`: você recebe o objetivo, as pistas e a forma dos comandos —
parte do caminho você descobre sozinho.

O diretório de trabalho chega com o serviço `mirante-homologacao`, na porta 8071 — e sem
nenhuma das checagens e dos testes que este Cenário pede: escrevê-los é o exercício
inteiro. Coloque no ar o que já existe:

```powershell
terraform init
terraform apply
```

Confira no navegador: <http://localhost:8071>.

## Quatro degraus, do barato ao caro

O **`terraform fmt -check -recursive`** confere formatação — e só. Ele lê o texto dos
arquivos: alinhamento de `=`, indentação, quebras de linha. É o degrau mais barato
porque não interpreta nada — e é por isso que ele não alcança os significados: nomes
trocados, valores absurdos e lógica errada passam alinhados.

O **`terraform validate`** sobe um degrau: confere sintaxe e coerência de tipos, **sem
falar com provider nenhum**. Um `external = "oitenta"` dentro de `ports` morre aqui.
Mas ele não alcança os valores: `porta = 8171` é um número perfeitamente tipado, e o
`validate` aprova o dígito do incidente sem piscar. E, como não fala com o mundo real,
uma imagem inexistente também passa.

As **checagens** — `precondition`, `postcondition`, `check` — sobem mais um degrau: são
regras que rodam junto com o plano e o apply, dentro da própria configuração. É a
primeira vez que valores são interrogados: "esta porta está na faixa?". O que elas não
alcançam é o comportamento de ponta a ponta: nenhuma delas sobe um container para
perguntar ao mundo se o serviço responde.

O **`terraform test`** é o único degrau que exercita o comportamento de ponta a ponta:
roda planos e applys de verdade contra o provider de verdade, e confere o que a
infraestrutura fez — não apenas o que ela declarou. É também o mais caro, pela mesma
razão. A escada existe porque cada degrau barato elimina uma classe de erro sem pagar o
preço do degrau caro.

## As três checagens que ficam no recurso

Acrescente ao `main.tf` as duas checagens que moram dentro do recurso, e a terceira, que
mora fora dele:

```hcl
resource "docker_container" "web" {
  name  = "mirante-${var.ambiente}"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = var.porta
  }

  lifecycle {
    precondition {
      condition     = var.porta >= 8070 && var.porta <= 8079
      error_message = "porta precisa ficar entre 8070 e 8079."
    }

    postcondition {
      condition     = self.name == "mirante-${var.ambiente}"
      error_message = "o container nasceu com nome diferente do declarado."
    }
  }
}

check "uma_porta_publicada" {
  assert {
    condition     = length(docker_container.web.ports) == 1
    error_message = "o serviço deveria publicar exatamente uma porta."
  }
}
```

As três diferem em **momento** e em **severidade** — e as duas diferenças são a lição.

A `precondition` é avaliada no plano, antes de qualquer chamada ao provider. É a porta
que tranca na entrada: com `porta = 8171`, o próprio `plan` reprova na tela, sem criar
nada. O erro do incidente morreria aqui, no degrau mais barato em que ele podia morrer.

A `postcondition` só é avaliada depois de o recurso existir, no `apply`: ela confirma
que o container nasceu com o nome declarado — e, se não nasceu, reprova um recurso que
já passou a existir. Repare na troca de custo: a `precondition` barrava antes de qualquer
chamada; a `postcondition` descobre o erro depois do nascimento. Uma reprova o plano; a
outra reprova o recurso.

O `check` tem o terceiro temperamento: roda com o plano e com o apply, mas **avisa sem
reprovar**. Um `check` vermelho aparece como aviso e o comando segue em frente. É por
isso que ele serve para o que é **desejável**, não para o que é obrigatório: "publicar
exatamente uma porta" é uma intenção de projeto — e, se um dia ela virar obrigatória,
muda de casa e vira `precondition`.

Depois de acrescentar as três, confirme que o código continua válido e sem mudança
pendente no ambiente real:

```powershell
terraform validate
terraform plan
```

## Dois arquivos de teste, dois propósitos

As checagens interrogam valores em pontos do plano. Os arquivos de teste dão o passo que
falta: eles rodam a configuração e conferem o resultado. Crie o diretório `tests/` com
dois arquivos — e repare que os dois não fazem a mesma pergunta.

`tests/unidade.tftest.hcl`:

```hcl
run "nome_derivado" {
  command = plan

  assert {
    condition     = output.nome_do_container == "mirante-homologacao"
    error_message = "o nome do container não derivou como devia."
  }
}

run "porta_fora_da_faixa" {
  command = plan

  variables {
    porta = 8171
  }

  expect_failures = [docker_container.web]
}

run "ambiente_invalido" {
  command = plan

  variables {
    ambiente = "staging"
  }

  expect_failures = [var.ambiente]
}
```

Os três `run` ficam em `command = plan`: nada é aplicado. O primeiro confere o nome
derivado. Os dois seguintes usam `expect_failures` para transformar erro em evidência:
com `porta = 8171`, o teste espera que a `precondition` reprove `docker_container.web`;
com `ambiente = "staging"`, espera que a `validation` da variável reprove
`var.ambiente`. O `run` passa quando a falha esperada acontece — e reprova se a proteção
que devia existir no código sumir.

`tests/integracao.tftest.hcl`:

```hcl
run "sobe_e_confere" {
  command = apply

  variables {
    ambiente = "producao"
    porta    = 8072
  }

  assert {
    condition     = output.nome_do_container == "mirante-producao"
    error_message = "o container de verdade nasceu com outro nome."
  }
}
```

Este `run` é `command = apply`: ele **cria e destrói infraestrutura de verdade**. Cada
arquivo de teste roda num state próprio, desfeito no fim — mas o Docker daemon é o mesmo
mundo em que o seu `mirante-homologacao` está no ar, e um apply com os valores de sempre
bateria no nome e na porta já ocupados. Por isso o teste declara os próprios valores —
`producao` e `8072`, dentro da faixa que a `precondition` exige. No fim do arquivo, o
Terraform desfaz tudo o que criou: a conta fecha, o ambiente real nem percebe.

A diferença de propósito é a decisão que este Cenário precisa tornar explícita. O teste
de unidade responde "a configuração se comporta como escrita?" em segundos, sem sair do
plano. O de integração responde "a infraestrutura nasce como esperado?" — e paga o
preço: precisa do Docker no ar, demora muito mais e toca o mundo real. É por isso que um
pipeline costuma rodar os dois em momentos diferentes: unidade em todo commit, porque é
quase grátis; integração antes de um merge ou numa agenda, porque custa — e quem paga o
preço decide quando comprá-lo.

## Rode

```powershell
terraform fmt -check -recursive
terraform validate
terraform test
```

Se o `fmt -check` reclamar, rode `terraform fmt` sem o `-check` para reescrever os
arquivos — ele alcança também os arquivos de `tests/`.

O `terraform test` roda os dois arquivos e termina em:

```
Success! 4 passed, 0 failed.
```

Três `run` do arquivo de unidade, um do de integração. Antes de encostar na Verificação,
quebre um teste de propósito: troque o nome esperado do primeiro `assert` do arquivo de
unidade e rode `terraform test` de novo. Leia a mensagem de falha com calma — ela diz o
arquivo, o `run` e a condição que não segurou. Essa mensagem é o que o seu futuro eu vai
receber de madrugada; vale conhecê-la de dia. Conserte antes de seguir.

## O que a Verificação vai rodar

Transparência antes de tudo: a Verificação roda apenas `tests/unidade.tftest.hcl` — o
comando dela vem com `-filter` para isso. O motivo é princípio, não economia: a
Verificação é observadora, e o teste de integração aplica infraestrutura de verdade;
aplicar por trás de uma checagem seria a Verificação deixando de observar para agir. Rodar
o arquivo de integração é responsabilidade de quem escreveu a infraestrutura — e este
Cenário diz isso em vez de esconder.

As outras Asserções continuam olhando o ambiente real: o container no ar, o state, o
plano limpo. O teste de integração não bagunça nada para elas — o que ele cria, ele
derruba.

## Verificação

Seis Asserções — e duas delas, pela primeira vez na Trilha, executam o seu trabalho em
vez de apenas olhá-lo.

As duas primeiras conferem o mundo real: o container `mirante-homologacao` de pé e
respondendo em <http://localhost:8071>.

As duas seguintes rodam comandos no workspace. A terceira executa `terraform test`
filtrado no arquivo de unidade e só aprova com `3 passed, 0 failed` — ela reprova
enquanto o diretório `tests/` não existir e enquanto qualquer teste estiver vermelho. A
quarta roda o `fmt -check -recursive` e só aprova quando tudo está formatado.

A quinta lê o state e confere que o container nasceu do código, no endereço
`docker_container.web`, com o `name` `mirante-homologacao`. A última roda um `plan` e só
aprova quando não há mudança pendente.

O que este Cenário fez à mão — fmt, validate, checagens, testes, conferência do mundo
real — o próximo Cenário amarra numa ordem só, sem ninguém digitando: o caminho que um
pipeline rodaria. A escada fica em pé; falta subi-la de mãos livres.
