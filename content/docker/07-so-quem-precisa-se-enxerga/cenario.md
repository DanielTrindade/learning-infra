---
id: docker/07-so-quem-precisa-se-enxerga
titulo: Só quem precisa se enxerga
dificuldade: autonomo
projetoCompose: lab-07
---
# Só quem precisa se enxerga

Este Cenário é `Autônomo`: você recebe o objetivo e o ambiente, e descobre o caminho.
Não há passos, nem comandos dados, nem dicas de formato. Só o que deve ser provado.

## O ambiente

Clique em **Iniciar cenário**. O diretório de trabalho tem três serviços, projeto
`lab-07`:

- `db` — o "banco" de mentira: responde um JSON com um animal na porta 3000.
- `api` — busca esse JSON no endereço `http://db:3000` e repassa.
- `web` — busca o `api` em `http://api:3000` e monta a página.

O `compose.yaml` entregue sobe. Ele está, porém, **todo errado para o que o Cenário
pede**: tudo na mesma rede e todas as portas publicadas no host. Em aplicação real isso
é exatamente o que não se faz — e o que você vai consertar.

## O objetivo

Três fatos sobre a rede, e todos devem ser verdadeiros ao mesmo tempo:

1. **`http://localhost:8093` responde** no host, com o corpo **`O api disse:
   pinguim-magalhaes`**.
2. O **`db` não é alcançável a partir do host** — nenhuma porta dele publicada.
3. Ainda assim, o `web` conversa com o `api`, e o `api` conversa com o `db`.

Traduzindo: duas redes — uma de **borda**, onde só o `web` é visível de fora, e uma
**interna**, onde o `db` fica escondido — com o `api` fazendo a ponte entre as duas. O
endereço `http://localhost:8093` só abre se o DNS por nome de serviço tiver funcionado
de ponta a ponta: `web` → `api` → `db`.

## As ferramentas

Você já usou todas:

```sh
docker compose -p lab-07 ps
docker compose -p lab-07 exec <serviço> <comando>
docker compose -p lab-07 up -d --build
docker network ls
```

- `ps` mostra os containers e as portas publicadas de cada um. O `db` publicado é a
  sua prova de que o estado entregue está errado.
- `exec` roda um comando **dentro** de um container. Ele é como você prova o que uma
  rede alcança de verdade.
- `network ls` lista as redes do Docker. Repare que o Compose já criou **uma** rede
  para o projeto — e que ela não tem o nome que a Verificação procura.

Quando **Verificar** apontar uma Asserção, leia o detalhe: ele diz em qual rede um
container deveria estar — ou **não** estar.

## O que a Verificação vai cobrar

Oito coisas: os três containers de pé, a 8093 respondendo 200, o corpo com
**`O api disse: pinguim-magalhaes`**, o `web` na rede `lab-07-borda`, o `api` nas duas
redes, o `db` na `lab-07-interna` **e não** na `lab-07-borda`.

A última é o coração do Cenário. Uma rede de borda só serve se o que não deve ser visto
de fora **não estiver nela**.

Dois detalhes que vão morder você se não souber:

- A Verificação procura redes com o **nome exato** `lab-07-borda` e `lab-07-interna`.
  O Compose, sem instrução explícita, prefere o nome do diretório e prefixa — o que
  criaria `lab-07_lab-07-borda`. Se o nome bater errado, a Asserção falha mesmo com a
  topologia certa.
- A porta do host e a porta do container são coisas diferentes. Publicar **ou não**
  publicar decide quem alcança o serviço de fora; a rede decide quem alcança por DNS.

## Verifique

Clique em **Verificar**. Antes de fazer qualquer coisa, a 8093 já responde e o corpo já
vem certo — tudo está publicado, então tudo se enxerga. O que reprova são as Asserções
de rede: o `web` não está na borda, o `api` não está nas duas redes e o `db` está na
rede errada. É o estado "tudo se enxerga" que você precisa desfazer, sem quebrar a
conversa.

## O que você aprendeu

Que publicar porta no host e pertencer a uma rede são decisões independentes, e que a
boa prática é não publicar o que não precisa ser visto de fora. Que o DNS por nome de
serviço funciona **por rede**: dois serviços só se falam pelo nome se estiverem na mesma
rede. Que um container pode pertencer a mais de uma rede e virar a ponte entre elas —
é assim que o `api` conversa com as duas enquanto o `db` fica escondido. E que a prova
do isolamento não é o que responde, é o que **deixa de responder** quando se olha de
fora.
