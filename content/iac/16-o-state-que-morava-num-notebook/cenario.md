---
id: iac/16-o-state-que-morava-num-notebook
titulo: O state que morava num notebook
dificuldade: autonomo
terraform: true
ministack: true
---
# O state que morava num notebook

A infraestrutura da Mirante está, enfim, inteira no código. O Cenário 15 levou o trio —
bucket, fila e tabela — para a nuvem local, e cada recurso nasce de um arquivo
versionado, cada mudança passa por um plano, cada divergência tem um dono. E, no meio
dessa arquitetura, mora um resto do passado: o state. O Cenário 06 cobrou a pergunta
certa — onde estão os segredos — e a resposta honesta daquela tarde foi o `.gitignore`:
proteger o repositório, porque o state não pode ir para o Git. Ele continuou onde sempre
morou: num arquivo `terraform.tfstate`, num diretório, numa máquina só. A pergunta que
o Questionário fez volta agora com o protagonista no palco: a pessoa que criou a
infraestrutura saiu, e o state estava só no notebook dela.

Isso nunca incomodou por um motivo simples: só uma pessoa aplica. O time da Mirante lê
o código, revisa o plano, discute o recurso — mas quem aperta o `apply` é uma pessoa,
numa máquina, e dois `apply` simultâneos de duas pessoas nunca aconteceram. E isso não é
uma arquitetura, é uma sorte. Este Cenário é sobre o que a sorte esconde: state numa
máquina só não tem dono coletivo, e `apply` sem trava é uma corrida esperando para
acontecer.

Este Cenário é `Autônomo`: você recebe o objetivo e o ambiente, e descobre o caminho
entre eles. Onde o texto entrega configuração pronta, é porque ela é configuração de
ambiente — não conhecimento.

## O ambiente

O diretório de trabalho chega com um `versions.tf` — o mesmo provider da nuvem local do
Cenário 15, **sem nenhum `backend`**, e isto é de propósito — um `main.tf` com uma fila
SQS `mirante-pedidos` já aplicável, e um `preparar.ps1`. Rode o preparador e depois
coloque a infra no ar:

```powershell
.\preparar.ps1
terraform init
terraform apply
```

O `preparar.ps1` cria o bucket `mirante-tfstate` e liga o versionamento dele. Repare no
nome da tarefa: esse é o bucket que vai guardar o state — e por isso ele **não pode**
nascer do Terraform que o usa. O bucket precisa existir antes do primeiro `init`, senão
o Terraform não tem para onde migrar; quem o cria é um comando à parte, e o motivo é a
própria lição deste Cenário. O recurso do `main.tf` é só um apoio: o assunto aqui é o
state, não a fila.

Aplique e olhe em volta. O `apply` criou a fila no emulador e gravou o retrato do mundo
num arquivo `terraform.tfstate` — neste diretório, nesta máquina, neste notebook. É
dele que este Cenário vai tratar.

## O objetivo

O state precisa terminar como objeto dentro do bucket `mirante-tfstate`, na chave
`mirante/terraform.tfstate` — sem que **nenhum recurso seja recriado** no caminho, e com
o `plan` limpo logo em seguida. A fila que o código declara tem de continuar sendo a
mesma fila: migrar o state move o retrato, não pinta o mundo de novo. Quando terminar,
`terraform state list` continua mostrando a mesma fila, o bucket tem o objeto na chave
exigida, e nenhum `apply` tem nada a fazer.

## A pista

Uma pista só. O bloco `backend` mora dentro de `terraform { }` — o mesmo bloco que já
abriga `required_version` e os provedores — e o `terraform init` tem uma opção que
oferece migrar o state existente em vez de começar do zero. O resto é com você.

E um aviso honesto: o `init` **vai perguntar** alguma coisa no meio do caminho. Não
responda por reflexo. A pergunta está lá porque há uma decisão de verdade por trás dela
— e ler a pergunta é parte do exercício.

## O que o endpoint local exige

O MiniStack não espelha o `amazonaws.com`: para o emulador, cada serviço da AWS vive no
`127.0.0.1:4566`. O bloco `endpoints` do provider já faz essa tradução para os recursos;
o backend S3 precisa da mesma tradução, no próprio bloco `backend`. Esta configuração é
entregue porque é configuração de ambiente, não conhecimento — copie o bloco para dentro
do `terraform { }`, ao lado do que já está lá:

```hcl
backend "s3" {
  bucket                      = "mirante-tfstate"
  key                         = "mirante/terraform.tfstate"
  region                      = "us-east-1"
  use_path_style              = true
  skip_region_validation      = true
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true
  skip_s3_checksum            = true

  endpoints = {
    s3 = "http://127.0.0.1:4566"
  }
}
```

Cada linha existe por um motivo, e todos são o mesmo motivo: o emulador é um servidor
local, no loopback, com regras próprias. O `use_path_style` faz o Terraform montar os
endereços no formato de caminho (`/bucket/objeto`) em vez do formato de subdomínio que a
AWS real atende — e que nenhum host local hospeda. Os `skip_region_validation`,
`skip_credentials_validation`, `skip_metadata_api_check` e `skip_requesting_account_id`
desligam validações que batem em serviços de identidade — catálogo de regiões, STS,
metadados de instância, descoberta de conta — que o emulador não implementa. E o
`skip_s3_checksum` existe justamente por causa de implementações S3-compatíveis: alguns
algoritmos de checksum que a AWS real calcula não existem no emulador, e sem essa linha
o `init` morre tentando assinar bytes com um algoritmo que ninguém responde.

Com o bloco no lugar, rode o `terraform init` de novo — e desta vez ele vai encontrar o
state local. É aqui que a pista e o aviso se encontram: a opção que migra o state
existe, a pergunta vai aparecer, e a decisão é sua.

## Lock

A migração deixou o state no bucket. Agora responda, com o mundo na frente: e se duas
pessoas apertarem `apply` ao mesmo tempo? Cada uma lê o mesmo state, cada uma planeja o
futuro da mesma fila, e as duas escrevem — a última a escrever apaga o retrato da
primeira, e o mundo real ganha um recurso que o state não conhece. O backend S3 tem a
resposta para isso, e ela se liga com uma linha no bloco que você acabou de escrever:

```hcl
use_lockfile = true
```

É o lock nativo do backend S3. Adicione a linha ao bloco, rode `terraform init` mais
uma vez — o Terraform percebe a mudança de configuração e reconfigura o backend, sem
precisar mover state nenhum — e abra **dois terminais no mesmo diretório**. Em cada um,
quase ao mesmo tempo:

```powershell
terraform apply
```

O segundo `apply` é recusado. O erro se chama `Error acquiring the state lock`, e vem
acompanhado do **Lock Info** — quem está segurando o lock, desde quando, rodando qual
operação. Não é uma mensagem de rede: é a trava funcionando, e dizendo quem é o dono da
vez.

Por baixo, o mecanismo é um detalhe de protocolo que vale ser visto. O lock é um objeto
— `.tflock` — que fica ao lado do state, no bucket. Criá-lo é um `PutObject`
**condicional**: o Terraform pede "crie este objeto se ele ainda não existir", numa
requisição com a condição `If-None-Match`. Os dois `apply` tentam criar o `.tflock` ao
mesmo tempo; o servidor deixa só o primeiro passar. O segundo recebe a recusa de volta
— o objeto já existe — e desiste sem tocar no state. Quando o vencedor termina, apaga o
lock, e o próximo `apply` pode começar.

Registre o que este Cenário deliberadamente não oferece: **não há lock por DynamoDB
aqui**. O Terraform está descontinuando esse caminho — o mecanismo antigo de travamento
por tabela está sendo aposentado — e oferecê-lo seria ensinar uma migração para um beco
sem saída. O lock nativo do S3 é o mecanismo; o `use_lockfile` é a linha que o liga.

E feche o raciocínio no ponto em que ele importa. State remoto sem lock é uma corrida
esperando para acontecer: a janela existe mesmo quando ninguém caiu nela. E o dano de
dois `apply` simultâneos não é infraestrutura errada — é **state corrompido**. Errado
você consegue consertar: o plano mostra a divergência e o `apply` reconcilia. Corrompido,
você perde a régua que media o conserto — o próprio retrato do mundo deixou de ser
confiável. Perder a capacidade de consertar é pior do que precisar de um conserto.

## O que mudou para o time

Pare e veja o que a migração mudou de verdade. O state não mora mais num notebook: mora
no bucket, e o bucket pertence à Mirante. Três consequências, cada uma valendo um
parágrafo.

O state ficou **compartilhado**. A pessoa que criou a infra pode sair amanhã — a
pergunta do Questionário deixa de ter motivo — e a máquina que aplica pode ser trocada,
porque o retrato do mundo não é mais propriedade dela. A prova é rápida e vale o
Cenário: apague o diretório `.terraform` do diretório de trabalho e rode `terraform
init` de novo, sem migrar nada. O state vem do bucket — e o `plan` sai limpo, sem
arquivo local no caminho. O retrato sobrevive a qualquer máquina.

O state ficou **versionado**. O `preparar.ps1` ligou o versionamento do bucket logo no
começo; cada `apply` grava uma versão nova do objeto. Um `apply` que deu errado — ou um
lock que quebrou o state no meio — tem versões anteriores para voltar. É o mesmo
argumento do Cenário 15, aplicado ao arquivo que governa todos os outros.

E o state ficou **restrito**. O que o Cenário 06 prometeu com o `.gitignore` — manter o
state fora do alcance acidental — o bucket entrega de outra forma: em vez de um arquivo
num disco pessoal legível por qualquer processo, um objeto num serviço com acesso
controlado. O arquivo não toca mais o notebook, porque o notebook deixa de ser o
guardião.

## Verificação

Cinco Asserções conferem o desfecho. As duas primeiras perguntam ao MiniStack pelo mundo
real: a fila `mirante-pedidos` continua no ar, e o bucket `mirante-tfstate` tem
exatamente um objeto sob o prefixo `mirante/` — o state migrado. A terceira roda uma
checagem no diretório de trabalho e só aprova quando o `terraform.tfstate` **não**
existe mais ali — o state deixou de morar na máquina. A quarta lê o state remoto e
confere que o endereço `aws_sqs_queue.pedidos` continua lá, reconhecido depois da
migração. A quinta roda um `plan` e só aprova quando não sobrou mudança nenhuma.

Dois detalhes da terceira Asserção merecem nota. Primeiro, o que a migração deixa
para trás: um `terraform.tfstate.backup`, a cópia local que o `init` guarda antes de
mover o state, e um `terraform.tfstate` vazio, a casca de zero bytes que o próprio
`init` deixa no lugar do arquivo que saiu. Nenhum dos dois é o state — o primeiro é a
cópia de segurança, o segundo é uma casca — e os dois podem ficar. A Asserção não se
conforma com um nome: ela confere que o state de verdade, o arquivo **com conteúdo**,
deixou de morar no diretório de trabalho. Segundo, o que a Asserção mede: é essa
ausência que prova que a máquina deixou de ser o guardião.
