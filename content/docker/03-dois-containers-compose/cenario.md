---
id: docker/03-dois-containers-compose
titulo: Dois containers conversando com Compose
dificuldade: guiado
projetoCompose: lab-03
---
# Dois containers conversando com Compose

Até aqui você rodou um container por vez, à mão. Aplicações reais são vários processos
que precisam se achar. Digitar quatro `docker run` na ordem certa, toda vez, não escala
— e é esse o problema que o Compose resolve.

## O que você recebeu

Clique em **Iniciar cenário**. No diretório de trabalho há dois serviços prontos, cada
um com o seu `Dockerfile` — você já aprendeu a escrever esses no Cenário anterior, e
aqui eles não são o assunto:

- `api/` — responde um JSON com o nome de um animal, na porta 3000.
- `web/` — busca esse JSON e mostra numa página, também na porta 3000.

O `web` procura o `api` no endereço `http://api:3000`. Esse endereço ainda não existe.
Fazê-lo existir é o exercício.

## Passo 1 — escrever o compose.yaml

Crie `compose.yaml` na **raiz** do diretório de trabalho, ao lado de `api/` e `web/`:

```yaml
services:
  api:
    build: ./api

  web:
    build: ./web
    ports:
      - "8090:3000"
    depends_on:
      - api
```

O que cada parte faz:

- `services:` — cada entrada vira um container. Os nomes `api` e `web` são escolha sua,
  e daqui a pouco você vai ver que eles não são só rótulos.
- `build: ./api` — em vez de uma imagem pronta, o Compose constrói a partir daquele
  diretório. É o `docker build` do Cenário anterior, embutido.
- `ports:` só no `web` — e essa ausência no `api` é o ponto principal deste Cenário.
- `depends_on:` — controla a **ordem de partida**, e só isso. Ele não espera o `api`
  ficar pronto para atender; espera apenas o container começar. Confiar nisso como se
  fosse garantia de prontidão é uma das causas mais comuns de bug intermitente em
  Compose.

## Passo 2 — subir o stack

Da raiz do diretório de trabalho:

```sh
docker compose -p lab-03 up -d --build
```

- `-p lab-03` nomeia o projeto. Sem isso, o Compose usa o nome do diretório — e este
  Cenário **exige** `lab-03`, porque é por esse nome que a plataforma vai derrubar o
  stack quando você abrir outro Cenário.
- `--build` força a construção das imagens. Sem ele, o Compose reaproveita o que já
  existir.

Veja o que subiu:

```sh
docker compose -p lab-03 ps
```

Repare nos nomes: `lab-03-api-1` e `lab-03-web-1`. O padrão é
`<projeto>-<serviço>-<índice>` — o índice existe porque um serviço pode ter várias
réplicas.

Abra `http://localhost:8090`. Deve aparecer **O api disse: pinguim-imperador**.

## Passo 3 — entender por que funcionou

Foi o Compose que criou uma rede e colocou os dois containers nela. Dentro dessa rede,
**o nome do serviço é o nome da máquina**. Por isso `http://api:3000` funciona: não há
IP escrito em lugar nenhum, e o `web` acha o `api` por DNS.

Agora prove o outro lado. Tente alcançar o `api` a partir da sua máquina:

```sh
curl http://localhost:3000
```

Não funciona, e não é erro. O `api` não tem `ports:`, então a porta dele existe apenas
dentro da rede do Compose. É assim que se expõe só o que precisa ser exposto — um banco
de dados de verdade fica exatamente nessa situação.

Confirme que de dentro da rede o endereço existe:

```sh
docker compose -p lab-03 exec web wget -qO- http://api:3000
```

## Passo 4 — ver os logs e derrubar

```sh
docker compose -p lab-03 logs web
docker compose -p lab-03 logs api
```

Para derrubar tudo — containers e a rede que o Compose criou:

```sh
docker compose -p lab-03 down -v
```

Você não precisa fazer isso agora: abrir outro Cenário na plataforma roda esse mesmo
comando por você. É justamente por isso que o `-p lab-03` era obrigatório.

## Verifique

Clique em **Verificar**. Quatro Asserções: os dois containers no ar, a porta 8090
respondendo, e o corpo com a frase que só existe se o `web` tiver realmente falado com
o `api`.

Se as três primeiras passarem e a última falhar, o significado é preciso: os dois
containers subiram, mas o `web` não alcançou o `api`. Olhe o nome do serviço no
`compose.yaml` e o endereço em `web/server.js`.

## O que você aprendeu

`services:` declara containers. `build:` constrói em vez de baixar. O **nome do serviço
vira hostname** dentro da rede do projeto. Serviço sem `ports:` só é alcançável de
dentro. `depends_on` ordena a partida e não garante prontidão. E `-p` nomeia o projeto,
que é como você endereça o stack inteiro depois — inclusive para derrubá-lo.
