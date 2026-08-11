---
id: iac/04-a-ordem-que-ninguem-escreveu
titulo: A ordem que ninguém escreveu
dificuldade: assistido
terraform: true
containers: [mirante-web]
volumes: [mirante-conteudo]
---
# A ordem que ninguém escreveu

Uma consultoria entregou o diretório deste Cenário com o `main.tf` "pronto". A pessoa
que revisou rodou o `apply` e tudo subiu sem erro — mas quem olhou com cuidado notou o
problema: o `mirante-web` não está na rede, não monta o volume e nem usa a imagem que o
mesmo arquivo declara. Está tudo no mesmo arquivo, e nada está conectado. Rode para ver
por você mesmo:

```powershell
terraform init
terraform apply
```

Tudo sobe, `yes` na confirmação, e o `mirante-web` responde na <http://localhost:8074>.
O apply verde não era a infraestrutura certa: era um conjunto de recursos que se ignoram.
O que falta não é mais um recurso no arquivo — é a relação entre os recursos que já
estão nele.

## O objetivo

Ao final, o `mirante-web` precisa estar na rede `mirante-interna`, montar o volume
`mirante-conteudo` em `/usr/share/nginx/html` e usar a imagem declarada por
`docker_image.web`. E você precisa conseguir **provar** que a ordem de criação veio
derivada, não escrita à mão.

## A dica que importa

Referenciar um atributo de outro recurso cria uma aresta no grafo do Terraform. Trocar a
string pela referência não é economia de digitação:

```hcl
image = docker_image.web.image_id
```

É a declaração de que a imagem precede o container — é o Terraform quem conclui que a
imagem precisa existir antes. Ninguém escreveu essa ordem; ela passou a existir no
instante em que um recurso apontou para o outro.

Agora olhe o `main.tf` entregue: nada aponta para nada. A imagem, a rede e o volume são
três recursos soltos, e o container repete os valores deles como texto. Os blocos que
faltam são `networks_advanced` e `volumes`, dentro do `docker_container`. Os **nomes**
dos blocos você já tem — a **forma** não, e é isso que você descobre. Quando precisar,
leia o dicionário da sua própria instalação:

```powershell
terraform providers schema -json
```

Ele lista os argumentos de cada recurso do provider Docker. A resposta está na sua
máquina, não neste texto.

## Veja o grafo

O Terraform guarda um desenho das dependências do seu código:

```powershell
terraform graph
```

A saída é DOT, a linguagem de grafos — e não é preciso renderizar para ler. Cada recurso
vira um nó; cada referência vira uma seta `->` entre dois nós. Rode agora e conte as
setas. Depois da correção, rode de novo e conte de novo: as setas que apareceram são as
relações que você declarou ao fazer os recursos se referenciarem.

## Quando a referência não existe

Existe uma situação em que não há atributo para referenciar: uma ordem real que nenhum
valor revela — por exemplo, um recurso que precisa existir antes de outro, sem que
nenhum deles use o valor do outro. Para esse caso o Terraform oferece `depends_on`.

Repare no que ele faz: declara a ordem **sem** criar a aresta de valor. E é aí que mora
a regra. `depends_on` é a **exceção**, não o caminho. Onde uma referência resolve — onde
você pode simplesmente usar o atributo do outro recurso — usar `depends_on` é esconder a
relação de quem lê o código depois: o leitor precisa de um palpite para entender por que
a sequência acontece. A referência diz a ordem e mostra o porquê na mesma linha.

## Prove a ordem

Com o `main.tf` corrigido, derrube tudo e suba de novo do zero:

```powershell
terraform destroy -auto-approve
terraform apply
```

O `destroy` limpa o que o apply anterior deixou — a imagem, a rede, o volume e o
container. O `apply` seguinte reconstrói tudo, e desta vez leia a sequência de criação:
a imagem, a rede e o volume primeiro, o container por último. O Terraform não leu a sua
vontade: leu as referências. A ordem que ninguém escreveu é a ordem que você declarou.

## Verificação

A Verificação deste Cenário tem cinco Asserções. As três primeiras conferem o mundo real:
o `mirante-web` rodando, o `mirante-web` presente na rede `mirante-interna` e o volume
`mirante-conteudo` existindo. As duas últimas conferem a origem: `docker_network.interna`
no state com o `name` `mirante-interna` — a rede nasceu do código, não de um `docker
network create` solto — e o plano limpo, provando que o grafo aplicado é o que o arquivo
descreve.

Não se engane com o apply verde: aplicar o `main.tf` entregue sobe o container e cria a
rede e o volume, mas o `mirante-web` não está na rede — e basta uma Asserção vermelha
para a Verificação inteira não concluir. O resultado certo é o container conectado ao
que o mesmo arquivo declara.
