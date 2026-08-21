---
id: iac/07-a-infra-que-nasceu-a-mao
titulo: A infra que nasceu à mão
dificuldade: autonomo
terraform: true
containers: [mirante-relatorios]
volumes: [mirante-arquivos]
---
# A infra que nasceu à mão

A pessoa que subiu o serviço de relatórios da Mirante saiu da equipe. Não houve
despedida com passagem de bastão: num dia ela estava ali, no outro o serviço continuava
funcionando e ninguém sabia explicar como. A infraestrutura dos relatórios está no ar,
respondendo, e não existe uma linha de código que a descreva. Recriá-la do zero significa
derrubar o que funciona — e este Cenário é sobre a alternativa a isso.

Este Cenário é `Autônomo`: você recebe o objetivo e o ambiente, e descobre o caminho. Não
há passos, nem comandos dados, nem dicas de formato. Só o que deve ser provado.

## O ambiente

O diretório de trabalho chega com um script que põe a infraestrutura da Mirante no ar do
jeito que ela nasceu — à mão. Rode-o:

```powershell
.\preparar.ps1
```

Confira o que existe de verdade com as três listagens:

```powershell
docker ps
docker volume ls
docker network ls
```

São três recursos, e nenhum deles tem dono:

- o container `mirante-relatorios` — o serviço de relatórios, de pé.
- o volume `mirante-arquivos` — o dado que o serviço usa.
- a rede `mirante-interna` — por onde o container conversa.

O volume e a rede foram criados à mão; o container foi subido à mão, sem porta
publicada, com reinício automático. Funciona — e não há código que o descreva.

## O objetivo

Ao final deste Cenário:

1. um `main.tf` que descreve os três recursos;
2. um state que conhece os três recursos;
3. os mesmos recursos de antes no ar — o container `mirante-relatorios` com o **mesmo
   id** que ele tinha quando você começou;
4. `terraform plan` sem mudança pendente.

Os três recursos no `main.tf` precisam ter os endereços `docker_volume.arquivos`,
`docker_network.interna` e `docker_container.relatorios` — a Verificação procura por esses
nomes, e não adivinha o que você escolheria.

Recriar qualquer um dos três é reprovar, mesmo que o resultado pareça idêntico. Um
container novo com o mesmo nome não é o mesmo container: é a infraestrutura refeita no
lugar da que estava funcionando. O objetivo é adotar o que existe — não reconstruí-lo.

## A pista

A documentação do Terraform chama o que você precisa fazer de **importar** — e, desde a
versão 1.5, existe uma forma **declarativa** de fazê-lo, que fica no código e é revisável
em pull request. O resto você descobre.

## O aviso que muda tudo

Um aviso, porque sem ele este exercício vira adivinhação: **importar não é o inverso de
aplicar**. Quando você aplica, o Terraform escreve o mundo real para coincidir com o
código. Quando você importa, ele escreve o state a partir do mundo real — mas só do que a
função de leitura do provider consegue recuperar. O provider Docker não recupera `ports`,
`volumes` nem `env` de um container importado.

A consequência prática você vai encontrar: um plano com `# forces replacement` embaixo do
container. Leia o plano. Ajuste o código. Repita até o plano ficar limpo — sem recriar
nada.

## A pergunta para levar embora

Se o container de relatórios publicasse uma porta, ele poderia ser adotado sem
recriação? Teste e descubra a resposta — ela muda o que você acha que `import` promete.

A lição maior: `import` não é uma promessa do Terraform, é uma capacidade de cada
provider. Quem decide se a adoção é segura é o plano — não o comando.

## Verificação

Sete Asserções, e cada uma prova uma camada deste Cenário.

As duas primeiras conferem o mundo real: o `mirante-relatorios` de pé e na rede
`mirante-interna`. A terceira confere que o volume `mirante-arquivos` existe. Sem os três
recursos no ar, não há o que adotar.

As três seguintes conferem a origem, lendo o state. `docker_volume.arquivos` com o `name`
`mirante-arquivos` — o volume que já existia entrou no state sem ser recriado.
`docker_container.relatorios` com o `name` `mirante-relatorios` — o container foi adotado,
e não refeito. E a Asserção que dá nome ao Cenário: o `id` do container no state é o
mesmo que estava no ar quando você começou — adotar não é reconstruir. A Asserção de
state não adivinha o nome que você escolheria para os recursos: por isso o Cenário
declara os endereços `docker_volume.arquivos`, `docker_network.interna` e
`docker_container.relatorios` no objetivo.

A última roda um `plan` e só aprova quando não há mudança pendente — o triângulo dos
Fundamentos inteiro em sincronia. E aqui mora a sutileza que dá sentido ao Cenário:
recriar o container deixa o plano **limpo** — o código, o state e o mundo real se
entendem, porque o mundo foi escrito de novo para coincidir com o código. É o `id` que
reprova: um container recriado ganha um id novo, e a Asserção do `id` exige o mesmo
container de antes. As outras seis passam; só o `id` reprova. O resultado certo pelo
caminho errado não conta nesta Trilha.
