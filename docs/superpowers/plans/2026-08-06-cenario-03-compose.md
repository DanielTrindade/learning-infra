# Cenário #3 — Dois containers conversando com Docker Compose

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Um Cenário sobre escrever o primeiro `compose.yaml`, e o teardown de projeto Compose sem o qual esse Cenário vazaria containers para o próximo.

**Architecture:** O `Cenário` ganha um campo opcional `projetoCompose`. Quando ele existe, o teardown roda `docker compose -p <projeto> down -v` antes de remover os containers declarados. O setup continua sendo apenas materializar workspace.

**Tech Stack:** A mesma dos planos anteriores. Nenhuma dependência nova.

## Contexto obrigatório

Leia [`CONTEXT.md`](../../../CONTEXT.md) e as ADRs em [`docs/adr/`](../../adr/). Os
planos [do esqueleto](./2026-08-04-esqueleto-cenario-docker.md) e
[do Cenário #2](./2026-08-06-cenario-02-dockerfile.md) já estão executados.

## Por que só o teardown, e não o setup

O `iniciar` **não** roda `docker compose up`, e isso é deliberado. Num Cenário `Guiado`
sobre Compose, o exercício *é* escrever o compose e subi-lo — um setup que já subisse o
stack não deixaria nada para o leitor fazer. É a mesma razão pela qual o Cenário #1 não
tem `compose.yaml`.

O teardown, esse, é necessidade imediata. Quando o leitor roda `docker compose up`,
nascem containers cujos nomes o campo `containers:` do Cenário não conhece, e o
`docker rm -f` não alcança. Sem teardown de projeto, o Cenário seguinte herdaria um
stack inteiro no ar — exatamente o vazamento que a [ADR 0002](../../adr/0002-um-cenario-ativo-por-vez.md)
existe para impedir.

Setup com Compose só se justifica num Cenário que precise de ambiente pronto ou
quebrado de propósito. Quando esse Cenário existir, o `iniciar` ganha o `up -d`.

## Global Constraints

- **Todas as restrições dos planos anteriores continuam valendo.**
- **A porta de host deste Cenário é 8090.** Verificada livre. 8080 é de outra aplicação
  da máquina, 8088 é do Cenário #1, 8089 é do Cenário #2.
- **O nome do projeto Compose deste Cenário é `lab-03`**, e o Cenário exige que o
  leitor use `-p lab-03`. Sem isso, o Compose derivaria o nome do diretório e o
  teardown não encontraria nada.
- **O daemon do Docker precisa estar no ar** para as duas tarefas.

### Comportamento do CLI verificado na engine 29.6.1

- `docker compose -p NOME up -d` nomeia containers como `<projeto>-<serviço>-<índice>`.
  Com `-p lab-03` e serviços `api` e `web`, saem `lab-03-api-1` e `lab-03-web-1`.
- `docker compose -p NOME down -v` **funciona de qualquer diretório, sem arquivo
  compose** — resolve os recursos pela label `com.docker.compose.project`. Remove
  também a rede do projeto.
- `docker compose -p INEXISTENTE down -v` sai com **código 0** e só emite
  `Warning: No resource found to remove for project`. É idempotente, como o
  `docker rm -f`.

---

### Task 1: Teardown de projeto Compose

> **CONCLUÍDA e validada em 2026-08-06** — 32 testes verdes.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/Cenario.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Modify: `backend/src/main/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivo.java`
- Test: `backend/src/test/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivoTest.java` — acrescentar
- Test: `backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java` — acrescentar

**Interfaces:**
- Consumes: `Cenario`, `GerenciadorDeCenarioAtivo`, `ExecutorDeComando` dos planos anteriores.
- Produces: campo `String projetoCompose` em `Cenario` (nulo quando ausente), lido do
  frontmatter pela chave `projetoCompose`, e um construtor de sete argumentos que o
  deixa nulo para não quebrar chamadas existentes.

- [ ] **Step 1: Escrever os testes que falham**

Acrescente a `GerenciadorDeCenarioAtivoTest`. Note o helper novo `cenarioCompose`, que
não substitui o `cenario` existente:

```java
    private Cenario cenarioCompose(String id, String projeto) throws Exception {
        Path diretorio = raiz.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(diretorio.resolve("workspace"));
        return new Cenario(id, "titulo", Dificuldade.GUIADO, List.of(), "# corpo",
                diretorio, List.of(), projeto);
    }

    @Test
    void iniciarDerrubaOProjetoComposeDoCenarioAnterior() throws Exception {
        Cenario primeiro = cenarioCompose("docker/03", "lab-03");
        Cenario segundo = cenario("docker/04", List.of());
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/03")).thenReturn(Optional.of(primeiro));

        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        gerenciador.iniciar(primeiro);
        comandosExecutados.clear();
        gerenciador.iniciar(segundo);

        assertEquals(
                List.of(List.of("docker", "compose", "-p", "lab-03", "down", "-v")),
                comandosExecutados);
    }

    @Test
    void cenarioSemProjetoComposeNaoChamaCompose() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        Cenario segundo = cenario("docker/02", List.of());
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/01")).thenReturn(Optional.of(primeiro));

        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        gerenciador.iniciar(primeiro);
        comandosExecutados.clear();
        gerenciador.iniciar(segundo);

        assertTrue(comandosExecutados.stream().noneMatch(c -> c.contains("compose")));
    }

    @Test
    void falhaDoComposeDownEhRuidosa() throws Exception {
        Cenario primeiro = cenarioCompose("docker/03", "lab-03");
        Cenario segundo = cenario("docker/04", List.of());
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/03")).thenReturn(Optional.of(primeiro));

        ExecutorDeComando executorQueFalha = comando ->
                comando.contains("compose") ? new SaidaDeComando(1, "", "daemon fora do ar")
                                            : new SaidaDeComando(0, "", "");
        var gerenciador = new GerenciadorDeCenarioAtivo(
                raiz.resolve("work").toString(),
                executorQueFalha,
                new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString()),
                repositorio);

        gerenciador.iniciar(primeiro);

        var erro = assertThrows(IllegalStateException.class, () -> gerenciador.iniciar(segundo));
        assertTrue(erro.getMessage().contains("lab-03"));
    }
```

E a `LeitorDeCenarioTest`:

```java
    @Test
    void leOProjetoComposeQuandoDeclarado() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: docker/03
                titulo: Com compose
                dificuldade: guiado
                projetoCompose: lab-03
                ---
                # corpo
                """);

        assertEquals("lab-03", new LeitorDeCenario().ler(diretorio).projetoCompose());
    }

    @Test
    void projetoComposeEhNuloQuandoAusente() throws Exception {
        escreverCenarioCompleto();

        assertNull(new LeitorDeCenario().ler(diretorio).projetoCompose());
    }
```

- [ ] **Step 2: Rodar para ver falhar**

Run: `cd backend && ./mvnw -B test`
Expected: FAIL na compilação — `Cenario` não tem oito componentes nem `projetoCompose()`.

- [ ] **Step 3: Acrescentar o campo ao `Cenario`**

Substitua `Cenario.java`:

```java
package dev.learninginfra.conteudo;

import dev.learninginfra.verificacao.Assercao;

import java.nio.file.Path;
import java.util.List;

public record Cenario(
        String id,
        String titulo,
        Dificuldade dificuldade,
        List<String> containers,
        String markdown,
        Path diretorio,
        List<Assercao> asercoes,
        String projetoCompose) {

    /** Cenário sem projeto Compose — a maioria. */
    public Cenario(String id, String titulo, Dificuldade dificuldade, List<String> containers,
                   String markdown, Path diretorio, List<Assercao> asercoes) {
        this(id, titulo, dificuldade, containers, markdown, diretorio, asercoes, null);
    }

    public boolean usaCompose() {
        return projetoCompose != null && !projetoCompose.isBlank();
    }

    public Path workspace() {
        return diretorio.resolve("workspace");
    }
}
```

O construtor de sete argumentos é o que impede esta mudança de quebrar todo teste e
toda chamada existente. Records não têm parâmetro com valor padrão; um construtor
secundário é a forma de conseguir o efeito.

- [ ] **Step 4: Ler o campo no `LeitorDeCenario`**

Em `LeitorDeCenario.ler`, troque a construção do `Cenario` para passar o oitavo
argumento:

```java
        return new Cenario(
                exigirTexto(meta, "id"),
                exigirTexto(meta, "titulo"),
                Dificuldade.deTexto(exigirTexto(meta, "dificuldade")),
                lerContainers(meta),
                corpo,
                diretorioDoCenario,
                asercoes,
                textoOpcional(meta, "projetoCompose"));
```

E acrescente o helper ao lado de `exigirTexto`:

```java
    private String textoOpcional(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        return valor == null ? null : valor.toString();
    }
```

- [ ] **Step 5: Derrubar o projeto no `GerenciadorDeCenarioAtivo`**

Substitua o método `derrubar`:

```java
    private void derrubar(Cenario anterior) {
        if (anterior.usaCompose()) {
            SaidaDeComando saida = executor.executar(
                    List.of("docker", "compose", "-p", anterior.projetoCompose(), "down", "-v"));
            if (!saida.sucesso()) {
                throw new IllegalStateException(
                        "não consegui derrubar o projeto Compose `" + anterior.projetoCompose()
                        + "` do Cenário " + anterior.id() + ": " + saida.stderr().strip()
                        + " — resolva isso antes de continuar, senão a próxima Verificação pode "
                        + "passar por sobra de ambiente");
            }
        }
        for (String container : anterior.containers()) {
            SaidaDeComando saida = executor.executar(List.of("docker", "rm", "-f", container));
            if (!saida.sucesso() && !pareceInexistente(saida)) {
                throw new IllegalStateException(
                        "não consegui remover o container `" + container + "` do Cenário "
                        + anterior.id() + ": " + saida.stderr().strip()
                        + " — resolva isso antes de continuar, senão a próxima Verificação pode "
                        + "passar por sobra de ambiente");
            }
        }
    }
```

Não há guarda de "projeto inexistente" porque não é preciso: `docker compose down` de
um projeto que não existe sai com código 0. Só uma falha real — daemon fora do ar,
permissão — produz código diferente de zero, e nesse caso queremos o estouro.

- [ ] **Step 6: Rodar a suíte inteira**

Run: `cd backend && ./mvnw -B test`
Expected: PASS, 32 testes (27 anteriores mais 5 novos).

- [ ] **Step 7: Commit**

```bash
git add backend/src
git commit -m "feat: teardown de projeto Compose"
```

---

### Task 2: O conteúdo do Cenário #3

> **CONCLUÍDA e validada em 2026-08-06** — percorrido contra o Docker real. As três
> afirmações do Passo 3 foram conferidas uma a uma, e o teardown de Compose apagou os
> dois containers **e a rede** ao trocar de Cenário.

**Files:**
- Create: `content/docker/03-dois-containers-compose/workspace/api/server.js`
- Create: `content/docker/03-dois-containers-compose/workspace/api/Dockerfile`
- Create: `content/docker/03-dois-containers-compose/workspace/web/server.js`
- Create: `content/docker/03-dois-containers-compose/workspace/web/Dockerfile`
- Create: `content/docker/03-dois-containers-compose/verificacao.yaml`
- Create: `content/docker/03-dois-containers-compose/cenario.md`

**Interfaces:**
- Consumes: o campo `projetoCompose` da Task 1.
- Produces: o Cenário `docker/03-dois-containers-compose`.

- [ ] **Step 1: O serviço `api`**

`workspace/api/server.js`:

```js
const http = require('node:http')

http
  .createServer((_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
    resposta.end(JSON.stringify({ animal: 'pinguim-imperador' }))
  })
  .listen(3000, () => console.log('api ouvindo na porta 3000'))
```

`workspace/api/Dockerfile`:

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY server.js .
EXPOSE 3000
CMD ["node", "server.js"]
```

- [ ] **Step 2: O serviço `web`**

`workspace/web/server.js`. Repare que ele responde **200 mesmo quando não alcança o
`api`** — isso é deliberado: separa "o web subiu" de "o web falou com o api" em duas
Asserções com diagnósticos diferentes.

```js
const http = require('node:http')

const alvo = process.env.API_URL || 'http://api:3000'

http
  .createServer(async (_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
    try {
      const resultado = await fetch(alvo)
      const dados = await resultado.json()
      resposta.end(`<!doctype html><title>web</title><h1>O api disse: ${dados.animal}</h1>`)
    } catch (erro) {
      resposta.end(
        `<!doctype html><title>web</title><h1>nao alcancei ${alvo}</h1><pre>${erro.message}</pre>`,
      )
    }
  })
  .listen(3000, () => console.log('web ouvindo na porta 3000'))
```

`workspace/web/Dockerfile` — idêntico ao do `api`:

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY server.js .
EXPOSE 3000
CMD ["node", "server.js"]
```

- [ ] **Step 3: A Verificação**

`verificacao.yaml`:

```yaml
asercoes:
  - tipo: container_rodando
    nome: lab-03-api-1
  - tipo: container_rodando
    nome: lab-03-web-1
  - tipo: http_responde
    url: http://localhost:8090
    status: 200
  - tipo: http_corpo_contem
    url: http://localhost:8090
    texto: "O api disse: pinguim-imperador"
```

A última Asserção é a única que prova o salto pela rede interna. As três primeiras
passariam com o `api` no ar mas inalcançável pelo `web`.

- [ ] **Step 4: O Cenário**

`cenario.md`:

````markdown
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
````

- [ ] **Step 5: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS. O conteúdo não afeta os testes, que usam fixture próprio.

- [ ] **Step 6: Percorrer o Cenário à mão**

1. Suba backend e frontend. O catálogo deve listar **três** Cenários.
2. Abra o #3 e clique em **Iniciar cenário**.
3. **Verificar** sem fazer nada: as quatro falham.
4. Escreva o `compose.yaml`, rode `docker compose -p lab-03 up -d --build`.
5. **Verificar**: as quatro passam.
6. **O teste que importa:** volte ao catálogo e inicie o Cenário #1. Depois rode
   `docker ps --filter "label=com.docker.compose.project=lab-03"` — precisa vir vazio.
   Se sobrar container, o teardown de Compose não funcionou e a ADR 0002 está furada.

- [ ] **Step 7: Commit**

```bash
git add content
git commit -m "feat: cenário docker/03 dois containers com compose"
```

---

## Depois deste Cenário

1. **Setup com Compose** — quando existir um Cenário que precise de ambiente pronto ou
   quebrado de propósito. O `iniciar` ganha `docker compose -p <projeto> up -d`.
2. **Asserção que distinga "nada respondeu" de "respondeu, mas não é seu container"**.
3. **Pré-requisitos entre Cenários** — a ordem já importa de fato: este Cenário assume
   o Dockerfile do #2.
