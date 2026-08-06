---
id: docker/02-empacotar-app-dockerfile
titulo: Empacotar uma app num Dockerfile
dificuldade: guiado
containers: [lab-app]
---
# Empacotar uma app num Dockerfile

No Cenário anterior você rodou uma imagem que outra pessoa construiu. Agora você vai
construir a sua. É a diferença entre usar Docker e realmente trabalhar com Docker.

## O que você recebeu

Clique em **Iniciar cenário**. No seu diretório de trabalho há `app/server.js`: um
servidor HTTP em Node, sem nenhuma dependência, que responde uma página e escuta na
porta 3000.

Ele não tem Dockerfile. Esse é o exercício.

## Passo 1 — escrever o Dockerfile

Crie um arquivo chamado `Dockerfile`, sem extensão, dentro de `app/`:

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY server.js .
EXPOSE 3000
CMD ["node", "server.js"]
```

Linha por linha:

- `FROM node:22-alpine` — toda imagem começa de outra. `alpine` é uma distribuição
  Linux enxuta, então a variante `-alpine` é bem menor que a padrão. Não espere
  milagre: esta imagem sai por volta de 230MB, porque o Node em si é grande. Se quiser
  medir a diferença, construa uma segunda vez trocando para `FROM node:22` e compare
  com `docker images lab-app`.
- `WORKDIR /app` — cria e entra no diretório. Os comandos seguintes rodam a partir dele.
- `COPY server.js .` — copia do **seu disco** para **dentro da imagem**. Repare na
  diferença em relação ao `-v` do Cenário anterior: `COPY` grava o arquivo dentro da
  imagem para sempre, enquanto `-v` só empresta um diretório seu durante a execução.
- `EXPOSE 3000` — **documenta** que a aplicação escuta nessa porta. E é só isso: não
  publica nada. Muita gente perde tempo aqui achando que `EXPOSE` substitui o `-p`.
- `CMD ["node", "server.js"]` — o processo que roda quando o container sobe. Quando ele
  termina, o container termina.

## Passo 2 — construir a imagem

De dentro de `app/`:

```sh
docker build -t lab-app:1.0 .
```

- `-t lab-app:1.0` dá nome e versão à imagem. Sem isso ela nasce sem nome e você só a
  alcança pelo id. Este cenário **exige** a etiqueta `lab-app:1.0`.
- O `.` no fim é o **contexto de build**: o diretório enviado ao daemon. Não é a
  localização do Dockerfile — é o que o `COPY` consegue ver. Um `COPY` de arquivo fora
  do contexto falha, e é uma das confusões mais comuns de quem está começando.

Confirme:

```sh
docker images lab-app
```

## Passo 3 — rodar a sua imagem

```sh
docker run -d --name lab-app -p 8089:3000 lab-app:1.0
```

O `-p 8089:3000` é o mesmo mecanismo do Cenário anterior: porta do host à esquerda,
porta do container à direita. O `3000` tem que casar com a porta em que o `server.js`
escuta — o `EXPOSE` não fez esse trabalho.

Abra `http://localhost:8089`.

## Passo 4 — ver o cache de camadas trabalhando

Cada instrução do Dockerfile cria uma camada, e o Docker reaproveita as que não
mudaram. Construa de novo sem alterar nada:

```sh
docker build -t lab-app:1.0 .
```

Repare nos `CACHED` na saída, e em quanto mais rápido foi.

Agora edite o `<h1>` dentro de `server.js` e construa outra vez. Só as camadas a partir
do `COPY` são refeitas — o `FROM` e o `WORKDIR` seguem em cache. É por isso que, em
projetos reais, se copia o arquivo de dependências e se instala **antes** de copiar o
código: o código muda toda hora, as dependências quase nunca.

Para ver a sua alteração no ar, recrie o container:

```sh
docker rm -f lab-app
docker run -d --name lab-app -p 8089:3000 lab-app:1.0
```

Se você mudou o texto do `<h1>`, devolva-o para **Empacotei minha app** antes de
verificar — é esse texto que a Asserção procura.

## Verifique

Clique em **Verificar**. Quatro Asserções: a imagem `lab-app:1.0` existe, o container
`lab-app` está rodando, a porta 8089 responde 200, e o corpo tem o texto esperado.

## O que você aprendeu

`FROM` escolhe a base. `WORKDIR` define o diretório. `COPY` grava arquivos na imagem —
permanentemente, ao contrário de `-v`. `EXPOSE` documenta e não publica. `CMD` define o
processo. `docker build -t nome:versão .` constrói, e o `.` é o contexto, não o
Dockerfile. E camadas são cacheadas na ordem em que você as escreveu, o que faz da
ordem das instruções uma decisão de performance.
