---
id: iac/10-reproduzivel-na-maquina-de-todo-mundo
titulo: Reprodutível na máquina de todo mundo
dificuldade: assistido
terraform: true
containers: [mirante-web]
---
# Reprodutível na máquina de todo mundo

A Mirante cresceu: agora são quatro pessoas. E foi logo depois da quarta entrega que o
mistério apareceu. O mesmo commit, o mesmo `apply` — rodado em duas máquinas na mesma
tarde, produziu planos diferentes. O time comparou os arquivos linha por linha: iguais.
Ninguém consegue explicar. Este Cenário é sobre a explicação que faltou: o arquivo é o
mesmo; o que não é o mesmo é o **binário** que o lê e o **provider** que esse binário
resolve.

Este Cenário é `Assistido`: você recebe o objetivo, as pistas e a forma dos comandos —
parte do caminho você descobre sozinho.

O diretório de trabalho chega com a entrega da quarta pessoa: o serviço `mirante-web`,
na porta 8070, aplicável como está. Coloque no ar:

```powershell
terraform init
terraform apply
```

Confira no navegador: <http://localhost:8070>.

## Duas versões, dois planos

Abra o `versions.tf` e olhe o defeito com calma — ele tem duas metades.

A primeira: `required_version = ">= 1.0"`. Essa constraint aceita qualquer Terraform
desta década. A máquina que instalou o binário ano passado e a que instalou ontem rodam
o mesmo arquivo com binários diferentes — e o arquivo não diz uma palavra sobre isso.

A segunda: o bloco `required_providers` declara a origem do provider Docker, mas nenhum
`version`. Sem versão pedida, cada `terraform init` resolve **a mais recente disponível
no registry naquele momento**. E "mais nova" é uma resposta que muda com o tempo: o
provider publica uma release numa terça de manhã, e a configuração que não mudou uma
linha passa a resolver outra coisa na terça à tarde. Duas máquinas, dois `init` em
momentos diferentes, duas escolhas diferentes — e nenhuma delas registrada em lugar
nenhum que o time compartilhe.

Foi isso que as duas máquinas viveram: binários diferentes lendo o arquivo, providers
diferentes executando o plano. Quando o time finalmente comparou a saída de
`terraform version` nas duas máquinas, as linhas de provider não batiam — e o mistério
virou mecânica. Ninguém mudou o arquivo. O arquivo apenas delegava duas decisões — qual
binário, qual provider — para o acaso de cada máquina.

## Aperte as constraints

A correção cabe em duas linhas. No `versions.tf`, troque a constraint do binário e
aperte a do provider:

```hcl
terraform {
  required_version = "= 1.15.8"

  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 4.5"
    }
  }
}
```

O `= 1.15.8` é a mais simples das constraints: exatamente esta versão, o binário que
esta Trilha pinou desde os Fundamentos. Qualquer máquina com outro Terraform para na
porta — erro na cara, antes de qualquer plano. É o fracasso certo: alto, na entrada, no
lugar do fracasso errado: silencioso, no meio.

O `~> 4.5` é o **operador pessimista**. Ele promete uma faixa: aceita 4.5, aceita 4.6,
aceita 4.9 — e recusa 5.0. É o versionamento semântico traduzido em regra executável:
dentro da mesma versão major, o provider promete compatibilidade; entre majors, mora a
quebra. O `~>` deixa a porta aberta para correções e melhorias, e trancada para a
próxima quebra — e a próxima quebra só entra quando alguém editar esta linha. Decisão
de pessoa, não do calendário.

Depois de trocar, confirme que o que já está resolvido cabe na faixa nova:

```powershell
terraform init
```

O `init` respeita a escolha já feita — a próxima seção trata do arquivo onde ela mora.

## O arquivo que ninguém escreve à mão

```powershell
terraform init
Get-Content .terraform.lock.hcl
```

O lockfile que abre na tela não foi escrito por pessoa nenhuma: nasce do `init` e
ninguém o edita à mão. Para cada provider, ele registra duas coisas: a **versão
resolvida** e o **digest** — a soma criptográfica do pacote, uma por plataforma. O
digest é o que transforma "baixe o provider" em "baixe este provider exato, e prove
com o hash que é ele mesmo".

Constraint e lockfile dividem o trabalho, e a divisão importa. A **constraint** é a
faixa aceitável — uma promessa sobre o futuro, escrita no código. O **lockfile** é a
escolha já feita — um fato sobre o presente, escrito pelo `init`. A constraint diz o
que pode; o lockfile diz o que é.

E aqui vai a regra com todas as letras: **o lockfile vai para o Git**. É a única
exceção à lista de ignorados que o Cenário 06 montou — state, backups, `.terraform/` e
`*.tfvars` ficam de fora; o `.terraform.lock.hcl` entra. O porquê é a lição deste
Cenário de ponta a ponta: sem o lockfile no repositório, cada `init` de cada máquina
reabre a decisão sozinho — resolve a mais nova daquele momento, e o mistério das duas
máquinas volta pela porta da frente. Com ele commitado, o `init` de qualquer máquina
instala exatamente a versão registrada, conferida pelo digest. A escolha feita uma vez
vale para todo mundo.

## Quando você quer reabrir a decisão

Se a escolha está congelada, como ela muda? De propósito, com um comando:

```powershell
terraform init -upgrade
```

O `-upgrade` procura a versão mais nova que cabe dentro da constraint — a mais nova
4.x, nunca uma 5.0 — e **reescreve o lockfile**. Aí está a diferença que decora:
`terraform init` respeita o lockfile; `terraform init -upgrade` o reescreve. Um congela
a decisão; o outro a revisa.

E reescrever lockfile é mudança de infraestrutura. O provider novo pode planejar
diferente do velho — foi exatamente isso que produziu planos distintos na abertura. Por
isso o diff do lockfile entra em pull request como qualquer outro: com revisão, com
justificativa, e com a autoria de quem reabriu a decisão registrada no histórico. A
constraint é a lei; o lockfile é a escolha vigente dentro da lei; o pull request é como
a escolha muda de dono.

## O mesmo código, outro binário

Este passo é opcional: se o binário `tofu` não estiver no PATH da sua máquina, pule a
seção — nada na Verificação muda.

Se estiver, rode no mesmo diretório, sem alterar uma linha do que já existe:

```powershell
tofu init
tofu plan
```

A história curta: em 2023 o Terraform trocou de licença para a BUSL, e a comunidade
respondeu com um fork — o **OpenTofu**, hoje sob a Linux Foundation. O HCL é idêntico:
os mesmos `main.tf` e `versions.tf` que o Terraform leu nesta tarde, o `tofu` lê,
planeja e aplica sem reclamar. A razão de esta Trilha usar Terraform é prática e não
doutrinária: é o comando que aparece nas vagas.

A lição que vale mais do que escolher um lado: **a fungibilidade é a propriedade que
protege o time**. O valor da infraestrutura da Mirante mora nos arquivos `.tf` —
código, constraints, lockfile — não no binário que os executa. Um time que aperta as
constraints e versiona o lockfile pode trocar de binário num dia ruim, sem reescrever
nada. Um time que deixa "a mais nova" decidir não pode trocar nem de máquina.

## Verificação

Seis Asserções, e cada uma prova uma camada deste Cenário.

As duas primeiras conferem o mundo real: o container `mirante-web` de pé e respondendo
em <http://localhost:8070>.

As duas seguintes conferem o entregável, lendo arquivos. A terceira lê o
`.terraform.lock.hcl` e só aprova quando ele existe e registra o provider resolvido. A
quarta lê o `versions.tf` e só aprova quando a constraint do provider deixou de aceitar
qualquer versão — é ela que carrega a lição deste Cenário. Se quiser vê-la reprovar,
verifique antes de apertar as constraints: as outras cinco continuam passando, e só
ela cai.

A quinta lê o state e confere que o container nasceu do código, no endereço
`docker_container.web`, com o `name` `mirante-web` de sempre. A última roda um `plan` e
só aprova quando não há mudança pendente — código, state e mundo real em sincronia.

O Ato III começa aqui, e o nome do ato é Confiança. O primeiro degrau é este Cenário:
mesma configuração, mesmo binário, mesmo provider — em qualquer máquina, em qualquer
data. Os próximos dois sobem a mesma escada: testar a infraestrutura antes de aplicar
e rodar, numa sequência só, o caminho que um pipeline rodaria.
