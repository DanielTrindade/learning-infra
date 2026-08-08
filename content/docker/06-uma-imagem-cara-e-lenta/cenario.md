---
id: docker/06-uma-imagem-cara-e-lenta
titulo: Uma imagem cara e lenta
dificuldade: assistido
containers: [lab-06-app]
---
# Uma imagem cara e lenta

Você já construiu uma imagem e viu as camadas serem cacheadas. Agora vem a parte em que
a imagem fica **cara** (grande) e **lenta** (demora para construir) — e você aprende a
consertar.

Este Cenário é `Assistido`: você recebe o objetivo e a forma dos comandos, não os
comandos prontos.

## O que você recebeu

Clique em **Iniciar cenário**. No seu diretório de trabalho há `app/` com uma aplicação
Node pura: nenhum `npm install`, nenhuma dependência de rede. O "build" é um script
local, `build.js`, que junta `vendor/lib.js` e `src/server.js` num único arquivo
`dist/server.js`.

Junto com o que importa, o diretório carrega entulho de um projeto real: `artefatos/`,
`logs/` e um `node_modules/` fabricado à mão — nenhum desses é usado pela aplicação.
Eles estão aí para provar um ponto.

O `Dockerfile` entregue é o pior caso, escrito para ser melhorado:

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY . .
RUN node build.js
EXPOSE 3000
CMD ["node", "dist/server.js"]
```

## O problema

O `COPY . .` copia o **diretório inteiro** para dentro da imagem. Entulho incluído.
E, como o `.` é tudo, **qualquer** mudança em qualquer arquivo invalida essa camada —
e todas as que vêm depois. Resultado: imagem grande e build que não aproveita cache.
Nada disso é um bug; é exatamente como o Docker funciona. Só é péssimo para o seu tempo
e o seu disco.

## Passo 1 — medir o problema

De dentro de `app/`, construa a imagem entregue como está:

```sh
docker build -t lab-06-app:antes .
```

Repare no `Sending build context` no começo da saída: é o tamanho do diretório inteiro
sendo enviado ao daemon. **Esse número é o seu ponto de partida.** Guarde-o.

## Passo 2 — parar de enviar o que ninguém usa

Crie um arquivo `.dockerignore` na **mesma pasta do Dockerfile** — ou seja, dentro de
`app/`. A regra é a do `.gitignore`: uma entrada por linha. Você sabe os nomes: o cache,
o diretório de build, os logs, os artefatos e o `.git` da aplicação.

```sh
# forma: dockerignore
<o que deve ficar de fora do contexto>
```

Recrie a imagem `lab-06-app:antes` e repare: o `Sending build context` caiu. O tamanho
da imagem **final** também pode cair, porque o `COPY . .` não vai mais mandar o que foi
ignorado. Esse é o ganho imediato.

## Passo 3 — ordenar as camadas para preservar cache

O `.dockerignore` resolve o tamanho, mas a ordem do Dockerfile ainda é frágil: o
`COPY . .` continua copiando tudo numa camada só, e continua invalidando tudo quando
qualquer arquivo muda.

A regra prática: **o que muda raramente vem antes, o que muda toda hora vem por último.**
Separe os `COPY` de um jeito que deixe `package.json` (e a pasta `vendor/`, que é a
"dependência" deste projeto) e o `build.js` antes do `src/`. Assim, quando você só
mexer no código em `src/`, só a última camada é refeita — as anteriores vêm do cache.

Construa de novo com a mesma tag e repare nos `CACHED` da saída. Depois mude qualquer
coisa em `src/server.js` e reconstrua: só a camada do `src/` é refeita.

## Passo 4 — multi-stage: a imagem final só com o resultado

O build ainda deixa dentro da imagem o `build.js`, o `src/` e tudo que a construção
precisou — e a aplicação só precisa de `dist/`. É aqui que entra o **multi-stage**: um
Dockerfile com dois `FROM`. O primeiro estágio prepara o `dist/` com `node build.js`. O
segundo começa de novo (de uma imagem Node enxuta), copia **só** `dist/` do primeiro
estágio e roda a aplicação.

```dockerfile
FROM node:22-alpine AS build
# ... copia o que precisa, roda build.js

FROM node:22-alpine
WORKDIR /app
# copia só dist/ do estágio de build
# cria a imagem final: EXPOSE, USER node, CMD
```

A imagem final **não carrega** o script de build nem o código-fonte — só o resultado
compilado. E o estágio final deve rodar como o usuário `node` (a imagem `node:22-alpine`
já o cria), não como `root`: é uma boa prática de segurança que esta Verificação exige.

## Passo 5 — subir a imagem final

Construa a solução com a tag que a Verificação procura:

```sh
docker build -t lab-06-app:final .
```

E rode com o nome e a porta que a Verificação procura:

```sh
docker run -d --name lab-06-app -p 8092:3000 lab-06-app:final
```

Abra `http://localhost:8092`. O corpo deve dizer **`Cenário 06 otimizado`**.

## Passo 6 — confirmar o ganho

Comparação lado a lado:

```sh
docker images
```

Procure `lab-06-app` e compare `antes` com `final`. E, se você reconstruir depois de
mexer só no código, veja as camadas de dependência virem do cache marcadas como
`CACHED` — construções mais rápidas, imagem menor.

## Verifique

Clique em **Verificar**. Seis Asserções: a imagem `lab-06-app:final` existe; o container
`lab-06-app` está rodando; a porta 8092 responde 200 com o texto esperado; o container
roda como usuário `node`; e a imagem final não carrega o script de build — esta última
é a prova da lição: no Dockerfile entregue o `build.js` fica na imagem, no multi-stage
não.

## O que você aprendeu

Que o contexto de build é o diretório inteiro e que o `COPY . .` o embute na imagem.
Que `.dockerignore` impede o envio do que não faz parte da imagem — e que todo projeto
tem entulho que não devia ir. Que a **ordem** das instruções decide o que o cache
aproveita: o que muda raramente primeiro, o código por último. E que o multi-stage deixa
na imagem final só o resultado — sem ferramenta de build, sem código-fonte, rodando
como um usuário sem privilégios.
