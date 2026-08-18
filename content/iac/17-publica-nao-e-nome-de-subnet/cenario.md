---
id: iac/17-publica-nao-e-nome-de-subnet
titulo: Pública não é nome de subnet
dificuldade: autonomo
terraform: true
ministack: true
---
# Pública não é nome de subnet

A auditoria da Mirante fez uma pergunta simples e importante: quais recursos da
Mirante são alcançáveis da internet? A resposta que veio de quem olhou os recursos foi
o nome das subnets. As `mirante-publica-a` e `mirante-publica-b` — públicas, diz o
nome. A `mirante-privada` — privada, diz o nome. Bonito e organizado. E, no entanto,
a resposta estava errada antes mesmo de ser digitada, porque **o nome não é a
resposta**: a pergunta era sobre roteamento, e roteamento não é um atributo de subnet.

Este Cenário é o espelho declarativo de um que a Trilha AWS já resolveu. Na Trilha
AWS, o Cenário 08 construiu a mesma topologia com um comando atrás do outro, numa
conta local que se descartava a cada Cenário. Aqui a Mirante tem a sua nuvem local no
MiniStack, e a topologia nasce de um arquivo versionado — para que a pergunta da
auditoria tenha uma resposta que não dependa da memória de ninguém.

Este Cenário é `Autônomo`: você recebe o objetivo e o ambiente, e descobre o caminho
entre eles. O diretório de trabalho chega com o `versions.tf` pronto — o provider do
Cenário 15, com o endpoint `ec2` acrescentado ao bloco `endpoints` — e com um
`main.tf` que já declara o começo da topologia: a VPC `mirante`, com o CIDR
`10.60.0.0/16`, e uma primeira subnet, `publica_a`, com o CIDR `10.60.1.0/24`, em
`us-east-1a`. Aplique o que está lá (`terraform init` e `terraform apply`) e a rede
local ganha a sua VPC. O resto é o exercício.

## O objetivo

A Mirante precisa de uma topologia completa, e cada peça tem um papel na pergunta da
auditoria:

- duas subnets **públicas** em **zonas de disponibilidade diferentes** — uma delas já
  nasceu (`publica_a`, em `us-east-1a`); a segunda precisa morar em outra zona;
- uma subnet **privada**;
- um **Internet Gateway** anexado à VPC;
- uma route table com uma rota **default (`0.0.0.0/0`) apontando para o Internet
  Gateway**, **associada apenas às duas subnets públicas** — e a nenhuma outra;
- um **security group** — o começo do vocabulário de acesso, mesmo que este Cenário
  não prove que ele filtra nada (o aviso de fidelidade, adiante).

Ao final, você precisa conseguir **provar** — para a auditoria e para a Verificação —
quais subnets são públicas. E a prova não pode ser o nome nem a tag: tem de ser a
associação de rota. Há um comando da AWS CLI que mostra exatamente isso, listando as
associações de uma route table — o mesmo comando que resolveu o Cenário 08 na Trilha
AWS. A route table pública precisa carregar a tag `Name = mirante-publica`; é por ela
que a Verificação encontra a route table e lê as associações. E dois endereços do
state são nomes exigidos: o gateway deve ser `aws_internet_gateway.mirante` e a
primeira associação, `aws_route_table_association.publica_a` — o `apply` só é
aprovado quando eles estão lá.

## A pista

Uma pista só. A diferença entre uma subnet pública e uma privada **não está em
nenhum atributo da subnet** — nem no CIDR, nem na zona, nem na tag. Está em outro
objeto, que aponta para ela. A subnet não carrega a sua natureza; ela é apontada.

## O que o Terraform muda aqui

Esta é a razão de o Cenário existir, e a comparação com a Trilha AWS é o contraste.
Na Trilha AWS, o Cenário 08 foi resolvido na CLI, e cada passo foi um comando
isolado: `create-vpc`, `create-subnet`, `create-internet-gateway`,
`attach-internet-gateway`, `create-route-table`, `create-route`,
`associate-route-table`. Nenhum desses comandos sabe o que o outro fez. O
`create-subnet` não disse que a subnet era pública; o `associate-route-table` não
gravou comentário nenhum sobre o que a associação significava. A relação entre os
recursos — qual subnet, qual route table, qual gateway — morava na cabeça de quem
digitou, e morria com ela. A resposta da auditoria dependia de a pessoa lembrar,
naquela tarde, o que tinha criado.

No Terraform a relação deixa de ser memória e vira artefato. A subnet, o gateway e a
route table são recursos; e a relação entre eles é **outro recurso** —
`aws_route_table_association` **é** a relação, escrita e versionada. "Esta subnet é
pública porque a associamos à route table que aponta para o gateway" deixa de ser uma
conclusão que só quem criou conseguia sustentar. Vira uma busca no repositório: quem
abrir o `main.tf` vê a associação, vê a rota, vê o gateway — e responde à auditoria
sem precisar perguntar a ninguém. O que era memória virou código, e código tem
versão, dono e revisão.

Repare também no que a pergunta da auditoria passou a custar. Na Trilha AWS, provar
que uma subnet era pública exigia descrever a route table por um filtro de tag — o
mesmo `describe-route-tables` que você vai usar aqui. A diferença é a origem da
resposta: lá, a pergunta ia contra o estado de um emulador que ninguém sabia como
havia chegado àquele estado; aqui, o estado é a sombra do código, e o código diz,
linha por linha, como aquele estado nasceu. A resposta continua uma consulta — mas a
consulta, agora, só confirma o que o repositório já declara.

## O aviso de fidelidade

Este aviso é categórico, e não há como suavizá-lo: **a VPC do MiniStack é somente
control plane**. O que este Cenário cria — VPC, subnets, Internet Gateway, route
tables, associações, security group — são objetos que o emulador guarda e devolve
quando perguntados, nada mais. Nenhuma rota encaminha pacote nenhum: um pacote não
cruza o Internet Gateway, um `0.0.0.0/0` não leva tráfego a lugar algum, uma subnet
"pública" e uma "privada" são igualmente indisponíveis para qualquer coisa que não
seja uma consulta de API. Nenhum NACL ou security group filtra tráfego: o emulador
aceita as regras, mas não tem um único byte atravessando a rede para ser filtrado.

Este Cenário prova que você **declarou** a topologia certa — que o repositório
descreve duas subnets públicas associadas a uma route table que aponta para um
Internet Gateway, e uma privada que não tem associação nenhuma com ela. É isso. Ele
não prova, e não pode alegar, que a rede isola coisa alguma: não há isolamento para
ser testado num emulador que não encaminha nada. É o contraste mais forte de toda a
Trilha: nos Cenários de Docker a fidelidade é total — container roda de verdade,
processo executa de verdade, porta responde de verdade — e aqui ela quase não existe,
com o mesmo tipo de HCL nos dois lugares. Leve as duas coisas juntas: o mesmo arquivo
que descreve um serviço com comportamento real descreve, aqui, uma topologia que só
existe no papel. Saber qual das duas você está declarando — e o que cada uma prova —
é parte do que este Cenário entrega.

## Verificação

Cinco Asserções conferem o desfecho, e todas apontam para o mesmo lugar: a topologia
declarada é a que está no control plane. As duas primeiras fazem a pergunta da
auditoria da forma certa. A primeira consulta a route table `mirante-publica` e só é
aprovada quando a rota `0.0.0.0/0` dela aponta para um Internet Gateway — a leitura
de "alcançável da internet" no vocabulário de roteamento. A segunda conta as
associações da mesma route table e só aprova quando são **exatamente duas**: as duas
subnets públicas, e nenhuma outra. As duas seguintes leem o state — o gateway
`aws_internet_gateway.mirante` e a associação `aws_route_table_association.publica_a`
precisam existir, nascidos do código. A quinta roda um `plan` e só aprova quando não
sobrou mudança nenhuma.

E repare no que a segunda Asserção está armada para pegar. Ela não lê nome de subnet
nem tag: ela conta associações. Se uma subnet privada ganhar uma associação a essa
route table — por um comando solto, por uma associação extra no código, por qualquer
coisa — a contagem deixa de ser duas, e a Verificação reprova. O nome continua
dizendo "privada", a tag continua dizendo "privada", e a Verificação continua
reprovando, porque ela mede o que torna uma subnet pública de verdade: a associação.
É a mesma regra que você aplica para provar o resultado — sem nome, sem tag, só
associação.
