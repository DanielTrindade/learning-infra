---
id: docker/08-rodando-nao-e-pronto
titulo: Rodando ainda não é pronto
dificuldade: assistido
projetoCompose: lab-08
---
# Rodando ainda não é pronto

O Cenário #3 avisou: `depends_on` só ordena a partida, não espera prontidão. Aqui essa
diferença deixa de ser teoria e vira o erro que derruba o stack inteiro.

Este Cenário é `Assistido`: você recebe o objetivo e a forma das peças, não as peças
prontas.

## O ambiente

Clique em **Iniciar cenário**. O diretório de trabalho tem três serviços, projeto
`lab-08`:

- `db` — o "banco" de mentira, e desta vez é **lento**: demora 8 segundos para começar a
  escutar na porta 3000. O retardo está num `setTimeout` do `db/server.js`, e é de
  propósito — bancos de verdade demoram para aceitar conexão.
- `api` — busca o JSON em `http://db:3000` **uma única vez**, na subida, e repassa para
  o `web`.
- `web` — busca o `api` em `http://api:3000` e monta a página.

O `compose.yaml` entregue usa o `depends_on` simples que você já conhece — só isso.

## O primeiro sintoma

Abra o diretório e veja o que subiu:

```sh
docker compose -p lab-08 ps -a
```

O `db` está de pé. O `api` está **parado**, com exit code 1. O `web` nem chegou a sair do
lugar, porque depende de quem morreu.

Veja por quê:

```sh
docker compose -p lab-08 logs api
```

O `api` subiu junto com o `db`, tentou o banco na hora — e o banco ainda estava nos 8
segundos de `setTimeout`. A primeira e única tentativa falhou, e o `api` chamou
`process.exit(1)`. **O container rodou e morreu por conta própria.**

É aqui que mora a lição: o `depends_on` simples fez o `db` **começar** antes do `api`,
mas começar não é estar pronto. Um banco que iniciou ainda não aceita conexão — e a
aplicação que não sabe disso cai. Rodar ainda não é pronto.

## O objetivo

Três mudanças, e todas no lugar certo:

1. Um **`healthcheck`** no `db` — o Docker passa a medir se o banco responde de verdade,
   e não só se o processo existe.
2. A dependência do `api` passa a esperar **prontidão** — `condition: service_healthy`
   — em vez de só ordem de partida. O `web`, pela mesma razão, também passa a esperar o
   `api` saudável.
3. **Retries** no `api/server.js` — porque um serviço fica saudável, mas pode falhar
   depois. Sem retry, uma falha pós-ready derruba o `api` do mesmo jeito.

## As peças

**Healthcheck** — um bloco que o Compose adiciona ao serviço. Diz que comando o Docker
deve rodar dentro do container para decidir se ele está saudável:

```yaml
healthcheck:
  test: ["CMD", "wget", "-qO-", "http://localhost:3000"]
  interval: 1s
  timeout: 2s
  retries: 20
  start_period: 1s
```

- `test` — o comando que "pergunta" se o serviço responde. O `wget` silencioso existe na
  imagem `node:22-alpine`; se responder 200, o Docker conta como saudável.
- `interval` — de quanto em quanto tempo o teste roda.
- `retries` — quantas falhas seguidas o Docker tolera antes de marcar como *unhealthy*.
  O banco só fala aos 8 segundos: **as falhas dos primeiros segundos não podem esgotar
  a contagem antes dele acordar**.
- `start_period` — carência inicial: falhas nesse intervalo são registradas, mas não
  contam para o `retries`.

O `api` recebe um healthcheck igual, no mesmo endereço interno (a porta 3000 dele).

**Condição de dependência** — a forma antiga vira um mapa:

```yaml
depends_on:
  db:
    condition: service_healthy
```

O `api` só **começa** quando o `db` for considerado saudável pelo healthcheck. O `web`,
com a mesma forma, só começa quando o `api` for saudável.

**Retry** — a forma de um laço que tenta, falha, espera e tenta de novo, com limite:

```js
const tentativas = 10
for (let tentativa = 1; tentativa <= tentativas; tentativa++) {
  try {
    const resposta = await fetch('http://db:3000')
    return resposta.json()
  } catch (erro) {
    console.log(`tentativa ${tentativa} de ${tentativas} falhou`)
    await new Promise((resolve) => setTimeout(resolve, 1000))
  }
}
throw new Error('banco não respondeu depois de todas as tentativas')
```

Se o banco ainda estiver acordando ou piscar, o `api` espera e tenta de novo — em vez de
`process.exit(1)` na primeira falha. Adapte essa forma dentro da função `principal()` do
`api/server.js`.

## O caminho

1. Rode `docker compose -p lab-08 up -d --build` com o `compose.yaml` entregue e
   confirme o sintoma: `ps -a` mostra o `api` parado, `logs api` mostra o erro.
2. Edite `compose.yaml`: dê ao `db` o `healthcheck` com valores que aguentem os 8
   segundos de silêncio.
3. Troque a dependência do `api` para `condition: service_healthy`, e dê a ele o próprio
   `healthcheck` — o `web` vai exigir o mesmo do `api`.
4. Troque a dependência do `web` para `condition: service_healthy` também.
5. Edite `api/server.js` para tentar o banco mais de uma vez, com pausa entre as
   tentativas, antes de desistir.
6. Rode `docker compose -p lab-08 up -d --build` de novo e acompanhe: o `db` acorda,
   vira saudável, e **só então** o `api` sobe. Confirme com `docker compose -p lab-08
   ps` que agora os três estão de pé, e com `docker compose -p lab-08 logs api` que a
   mensagem de erro sumiu.
7. Abra `http://localhost:8095`. Deve aparecer **O api disse: pinguim-rei**.

## Verifique

Cinco Asserções: o `db` e o `api` **saudáveis**, o `web` rodando, a porta 8095
respondendo 200 e o corpo com a frase que só existe se a conversa inteira `web` → `api`
→ `db` funcionou.

Clique em **Verificar** antes de mexer em qualquer coisa: o `db` não está saudável
(healthcheck nenhum, status `<no value>`), o `api` morreu e o `web` não tem de onde
buscar. É o estado entregue — e é ele que você precisa consertar.

## O que você aprendeu

Que container rodando não é serviço pronto: o processo existe, mas a aplicação ainda
pode estar a 8 segundos de aceitar conexão. Que `healthcheck` transforma "processo vivo"
em "serviço saudável", com comando, frequência, carência e tolerância a falhas. Que
`depends_on` com `condition: service_healthy` espera prontidão de verdade, e não só
ordem de partida. E que prontidão não é imunidade: uma falha que acontece **depois** do
serviço ficar saudável ainda o derruba — e é para isso que servem os retries na
aplicação. Ordem de partida, saúde declarada e tolerância a falha: são três camadas
diferentes, e as três trabalham juntas.
