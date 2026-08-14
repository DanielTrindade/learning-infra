---
id: iac/09-o-rename-que-derrubou-o-banco
titulo: O rename que derrubou o banco
dificuldade: autonomo
terraform: true
containers: [mirante-banco]
volumes: [mirante-estoque]
---
# O rename que derrubou o banco

Noutra empresa — não na Mirante — uma faxina de nomes quase matou o negócio. Uma pessoa
renomeou `docker_volume.dados` para `docker_volume.estoque` no arquivo, porque o nome novo
dizia melhor o que o volume guardava. O plano que o Terraform imprimiu foi aprovado sem
que ninguém lesse o rodapé. O `apply` destruiu o volume antigo — e, com ele, o banco de
dados inteiro. A faxina terminou num restore apressado de madrugada e numa lição que o
time nunca esqueceu: renomear recurso no Terraform não é rebatizar. É anunciar a morte do
velho e o nascimento de outro.

Este Cenário é `Autônomo`: você recebe o objetivo e o ambiente, e descobre o caminho. Não
há passos, nem comandos dados, nem dicas de formato. Só o que deve ser provado — e uma
diferença em relação ao acidente: aqui o mesmo rename precisa acontecer **sem perder nada**.

## O ambiente

O diretório de trabalho chega com o `main.tf` aplicável como está, usando os endereços
antigos — os mesmos que a faxina lá da outra empresa tinha antes do estrago. Coloque a
infraestrutura no ar:

```powershell
terraform init
terraform apply
```

O banco da Mirante sobe na porta 8079, com o volume `mirante-estoque` montado em
`/var/lib/mirante`. Antes de tocar em qualquer nome, grave um dado dentro do volume — é ele
que torna a perda concreta e verificável daqui para a frente:

```powershell
docker exec mirante-banco sh -c "echo pedido-4711 > /var/lib/mirante/estoque.txt"
docker exec mirante-banco cat /var/lib/mirante/estoque.txt
```

A segunda linha precisa responder `pedido-4711`. Anote também o id do container — você vai
precisar dele para provar, no fim, que nada foi recriado:

```powershell
docker inspect -f '{{.Id}}' mirante-banco
```

## O objetivo

Ao final deste Cenário:

1. `docker_volume.dados` virou `docker_volume.estoque`;
2. `docker_container.banco` virou `docker_container.estoque`;
3. `estoque.txt` continua legível, com o mesmo conteúdo `pedido-4711`;
4. o container tem o **mesmo id** de antes do rename;
5. o state conhece os dois recursos nos endereços novos;
6. `terraform plan` sem mudança pendente.

Os endereços novos não são livres: a Verificação procura `docker_volume.estoque` (com o
`name` `mirante-estoque`) e `docker_container.estoque` (com o `name` `mirante-banco`), e
não adivinha o que você escolheria. Repare no que **não** muda: o nome do volume e o nome
do container continuam os mesmos de sempre. Só o endereço muda. Recriar qualquer um dos
dois é reprovar — um container novo com o mesmo nome não é o mesmo container.

## Renomeie e leia o plano — não aplique

Renomeie os dois recursos no `main.tf` — só os endereços, sem tocar em mais nada — e
planeje:

```powershell
terraform plan
```

Não aplique. Leia o rodapé: `Plan: 2 to add, 0 to change, 2 to destroy`, com o volume
marcado para destruição. É exatamente o plano que a pessoa da outra empresa aprovou sem
ler.

Pare no plano e responda a pergunta que ele faz a você: por que o Terraform acha que o
recurso **sumiu**? Nada mudou no mundo real — o container segue de pé, o volume segue
guardando `pedido-4711`. E ainda assim o Terraform propõe destruir um e criar dois. A
resposta estava na pergunta que o Cenário 08 deixou no ar: para o Terraform, o que é a
identidade de um recurso?

## A pista

Uma só, e ela é a resposta da pergunta acima. Para o Terraform, **o endereço é a
identidade**. Mudou o endereço, o Terraform vê um recurso morrer e outro nascer no lugar.

Existe um bloco que ensina o Terraform que um endereço virou outro. Ele **fica no código**,
não é um comando avulso — e por isso é revisável em pull request, como tudo o que dura. A
forma exata do bloco, onde ele mora e como apontar o velho para o novo é descoberta sua.

## O detalhe que confunde todo mundo

Isto foi medido, e precisa estar escrito antes de você seguir: quando o bloco está no
lugar, o plano fica `0 to add, 0 to change, 0 to destroy` — e, mesmo assim, **ainda há uma
mudança a aplicar**.

Não é contradição. Mover um endereço não muda o mundo real — por isso o plano não propõe
criar nem destruir nada. Mas é uma **escrita no state**: o Terraform precisa gravar que o
endereço `docker_volume.dados` agora responde por `docker_volume.estoque`, e o mesmo para o
container. Essa gravação só acontece no `apply`. Enquanto o `apply` não rodar, a
Verificação continua reprovando — e está certa em reprovar: o código já usa os nomes novos,
o state ainda não, e essa distância é exatamente o que a Verificação existe para medir. O
plano ficou limpo na tela, mas a tarefa não terminou. Rode o `apply` e termine.

## Ponha o cinto

Depois do rename — com o state já nos endereços novos — vale entender o acidente por
dentro. Acrescente `prevent_destroy` no volume, no endereço novo, e tente:

```powershell
terraform destroy
```

O Terraform se recusa: `Error: Instance cannot be destroyed`. O cinto funcionou — agora.
Anote o que foi medido, porque é a lição mais fina deste Cenário: o `prevent_destroy`
protege **um endereço, não um recurso**. Declará-lo no endereço novo não teria salvado o
volume antigo do plano do passo 4: naquele plano, o recurso que ia para o abate vivia no
endereço `docker_volume.dados`, onde ainda não havia cinto nenhum. O cinto só funciona se
estiver posto **antes** da curva — não depois.

O cinto pode ficar no arquivo: ele só fala em `destroy`, e a Verificação não destrói nada.

## Verificação

Seis Asserções, e cada uma prova uma camada deste Cenário.

As duas primeiras conferem o mundo real: o container `mirante-banco` de pé e o volume
`mirante-estoque` existindo. A terceira confere o dado: o `estoque.txt` ainda responde
`pedido-4711` — é ela que transforma a perda em algo concreto, e não em promessa de que
"não deve ter acontecido nada".

As duas seguintes conferem a origem, lendo o state. `docker_volume.estoque` com o `name`
`mirante-estoque`, e `docker_container.estoque` com o `name` `mirante-banco` — os dois
recursos moram nos endereços novos, com os mesmos nomes de sempre. A última roda um `plan`
e só aprova quando não há mudança pendente. É ela que cobra o `apply` da seção `O detalhe
que confunde todo mundo`: mover um endereço é uma escrita no state, e enquanto essa escrita
não acontecer a Verificação reprova — corretamente.

O Ato II termina aqui, tendo percorrido as quatro divergências que o triângulo dos
Fundamentos desenha: drift, brownfield, recurso apagado por fora e refactor. Este Cenário
é a quarta — `código ≠ state para o mesmo recurso`. Os Fundamentos a nomeiam como refactor,
e agora ela tem um remédio que não destrói: ensinar o Terraform, no código, que um
endereço virou outro. O código, o state e o mundo real se reconciliam — e o banco não cai.
