---
id: docker/10-da-tag-ao-digest
titulo: Da tag ao digest
dificuldade: assistido
containers: [lab-10-app, lab-10-registry]
---
# Da tag ao digest

Até aqui as imagens foram nomes locais: `lab-06-app:final`, `lab-08-app:2.0`. Num
projeto real a imagem precisa ir de uma máquina a outra — e aí o nome ganha um
endereço e o controle de versão deixa de ser um rótulo à toa.

Este Cenário é `Assistido`: você recebe o objetivo e a forma dos comandos, não os
comandos prontos.

## O problema

Uma **tag** é um apelido mutável. `latest` não significa "a mais nova": significa "o que
quem publicou apontou para ela por último". Quem usa `latest` em produção está se
entregando a um controle de versão que muda sozinho, sem o seu conhecimento — e a
recuperação de um desastre deixa de ser um comando para virar uma busca.

Existe, porém, um identificador que a tag não pode trair: o **digest**. Ele é a
impressão digital exata do conteúdo da imagem. Duas imagens diferentes nunca têm o mesmo
digest; a mesma imagem, com qualquer tag que receba, tem sempre o mesmo digest. Este
Cenário é sobre usar um registry, publicar, errar — e voltar pelo digest.

## O objetivo

Um registry local descartável rodando em `localhost:5000`, com a imagem
`localhost:5000/lab-10-app:1.0` publicada nele. A imagem **publicada** é a 1.0, mas a
**tag** `:1.0` já foi sobrescrita por uma versão com bug (a 1.1). O container
`lab-10-app` rodando na porta 8097 deve servir a **1.0, puxada pelo digest** — o
rollback sem mudar a tag.

## As peças

Primeiro, o registry. É uma imagem oficial do Docker, rodando como qualquer outra:

```sh
docker run -d --name lab-10-registry -p 5000:5000 registry:2
```

Repare: **não existe conta, não existe login, não existe cloud.** É o mesmo mecanismo que
um registry externo, só que na sua máquina — e ele some quando você o remove. Por isso é
descartável: perfeito para aprender o fluxo inteiro sem pedir credencial a ninguém.

Depois, o endereço de uma imagem num registry tem três partes, e a ordem importa:

```text
<registry>/<nome>:<tag>        localhost:5000/lab-10-app:1.0
```

O registry vem **antes** do nome. Sem ele, a tag é local; com ele, o Docker sabe para
onde publicar.

## Passo 1 — a versão boa

Dentro de `app/` você recebeu um Dockerfile que declara `APP_VERSAO=1.0` e um
`server.js` que serve essa versão em JSON. Construa a imagem boa:

```sh
docker build -t lab-10-app:1.0 app/
```

Confira o corpo antes de qualquer coisa: a 1.0 precisa responder com
**`{"versao":"1.0"}`** — é isso que a Verificação procura no final.

## Passo 2 — publicar

Dê à imagem o endereço do registry e publique:

```sh
docker tag lab-10-app:1.0 localhost:5000/lab-10-app:1.0
docker push localhost:5000/lab-10-app:1.0
```

Agora a parte que o resto do Cenário depende: **anote o digest**. O digest não é
mostrado pelo push — você o lê da imagem local:

```sh
docker inspect --format='{{index .RepoDigests 0}}' lab-10-app:1.0
```

O resultado tem a forma `localhost:5000/lab-10-app@sha256:<44 caracteres hexadecimais>`.
Guarde esse valor inteiro: é o seu bilhete de volta para a 1.0.

## Passo 3 — o bug

Introduza um bug silencioso: no Dockerfile, mude `APP_VERSAO=1.0` para `APP_VERSAO=1.1`.
Reconstrua com a mesma tag local, dê o mesmo endereço do registry e publique **por cima**:

```sh
docker build -t lab-10-app:1.0 app/
docker tag lab-10-app:1.0 localhost:5000/lab-10-app:1.0
docker push localhost:5000/lab-10-app:1.0
```

O que aconteceu é o coração do Cenário: **a tag `:1.0` agora aponta para uma imagem
diferente** no registry. Nenhum dado foi perdido — a imagem antiga continua lá, órfã da
tag, mas acessível pelo digest que você anotou. Confirme com uma olhada no conteúdo:

```sh
docker run --rm localhost:5000/lab-10-app:1.0
```

O log de boot da app diz `app v1.1 ouvindo na porta 3000` — o mesmo nome de tag, outro
conteúdo. A tag mentiu.

## Passo 4 — o rollback

Em produção, neste ponto alguém rodaria a 1.1, veria o bug, e quereria a 1.0 de volta
sem reescrever nada. O caminho do Cenário: comece **sem imagens locais** e puxe de volta.

Apague as imagens locais:

```sh
docker rmi localhost:5000/lab-10-app:1.0 lab-10-app:1.0
```

Agora a sua máquina só conhece o registry. Puxe a 1.0 **pelo digest que você anotou** no
Passo 2 — não pela tag:

```sh
docker pull localhost:5000/lab-10-app@sha256:<o digest anotado>
```

Depois rode a app. A forma de endereçar a imagem é idêntica à do pull: `nome@sha256:...`.
Não é uma tag, então não há o que a tag sobrescrever:

```sh
docker run -d --name lab-10-app -p 8097:3000 localhost:5000/lab-10-app@sha256:<o digest anotado>
```

Abra `http://localhost:8097`. O corpo deve ser **`{"versao":"1.0"}`** — você voltou a
rodar a versão boa sem mudar a tag, sem reconstruir, sem tocar no registry. O registry
ainda guarda a 1.1 sob a tag `:1.0`; você só parou de usá-la.

## Por que `latest` é traiçoeiro

Pense no Passo 3 com `latest` no lugar de `:1.0`: ninguém notaria a troca. `latest` não
é uma versão, é um alvo móvel — cada `docker push` o move para a imagem nova, e qualquer
`docker pull` que não diga o quê busca o alvo atual. Numa equipe, dois dias depois
ninguém sabe **o que** está rodando, porque "a mais nova" depende de quando você puxou.
As tags que de fato amarram uma versão a um conteúdo são as semânticas (`1.0`, `1.1`) e,
principalmente, **o digest** — que identifica a imagem exata, para sempre.

## Verifique

Quatro Asserções: o container `lab-10-app` está rodando; a porta 8097 responde 200; o
corpo contém `"versao":"1.0"`; e a imagem `localhost:5000/lab-10-app:1.0` existe no
registry. A última prova a publicação; a do corpo prova o **rollback por digest** — é a
1.0 rodando enquanto a tag diz 1.1.

## O que você aprendeu

Que tag é apelido mutável: o `docker push` move a tag para a imagem nova e a antiga
continua viva, acessível pelo digest. Que o digest é a impressão digital exata do
conteúdo — `nome@sha256:...` endereça uma imagem específica e não muda quando a tag muda.
Que `latest` é o pior dos dois mundos: tag que muda sem você decidir. E que deploy sério
referencia digest, porque é o único endereço que devolve exatamente o que você testou.
