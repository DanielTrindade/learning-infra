# Cenário #4 — Um stack que não sobe (Mestre)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** O primeiro Cenário `Mestre` — ambiente quebrado de propósito, causa não revelada — e o setup com Compose que ele exige, fechando a última metade não validada do ciclo de vida.

**Architecture:** O `iniciar` passa a subir o stack quando o workspace materializado traz um `compose.yaml` e o Cenário declara `projetoCompose`. Nenhum campo novo: a presença do arquivo é o sinal. O conteúdo é um stack de dois serviços em que um deles morre ao subir.

**Tech Stack:** A mesma dos planos anteriores. Nenhuma dependência nova.

## Contexto obrigatório

Leia [`CONTEXT.md`](../../../CONTEXT.md) e as ADRs em [`docs/adr/`](../../adr/). Os
planos [do esqueleto](./2026-08-04-esqueleto-cenario-docker.md), [do Cenário #2](./2026-08-06-cenario-02-dockerfile.md)
e [do Cenário #3](./2026-08-06-cenario-03-compose.md) já estão executados.

## Por que agora, e não antes

Os planos anteriores adiaram o setup com Compose três vezes, sempre pela mesma razão:
num Cenário `Guiado` sobre Compose, subir o stack no `iniciar` roubaria o exercício.

Este Cenário inverte isso. O leitor **precisa** receber um ambiente já no ar e já
quebrado — não há como diagnosticar o que não subiu. É o primeiro Cenário em que o
setup tem trabalho real, e por isso é ele que o justifica.

É também o primeiro Cenário fora do degrau `Guiado`. Segundo o `CONTEXT.md`, `Mestre`
significa *ambiente quebrado, a causa não é revelada* — então o texto descreve o
sintoma e o objetivo, lista as ferramentas disponíveis, e **não** diz onde olhar.

## Como o setup sabe se deve subir

Sem campo novo no frontmatter. A regra é a conjunção de dois fatos já declarados:

| Cenário | `projetoCompose` | `workspace/compose.yaml` | `iniciar` sobe? |
|---|---|---|---|
| #3 — escrever o compose | `lab-03` | ausente (o leitor escreve) | não |
| #4 — diagnosticar | `lab-04` | presente (já quebrado) | **sim** |

A distinção sai de graça e é honesta: se o arquivo veio pronto no workspace, ele é
parte do ambiente entregue; se não veio, produzi-lo é o exercício.

## Global Constraints

- **Todas as restrições dos planos anteriores continuam valendo.**
- **A porta de host deste Cenário é 8091.** Verificada livre. 8080 é de outra aplicação
  da máquina, 8088/8089/8090 são dos Cenários #1/#2/#3.
- **O nome do projeto Compose é `lab-04`.**
- **O `up` usa `-f <caminho absoluto>`, não `cd`.** Verificado na engine 29.6.1:
  `docker compose -f /caminho/compose.yaml up -d --build` resolve `build: ./api`
  relativo ao **diretório do arquivo**, mesmo executado de outro lugar. Por isso o
  `ExecutorDeComando` não precisa ganhar conceito de diretório de trabalho.
- **O daemon do Docker precisa estar no ar** para as duas tarefas.

---

### Task 1: Setup com Compose

> **CONCLUÍDA e validada em 2026-08-06** — 36 testes verdes. O `iniciar` subiu o stack
> do Cenário #4 em 6s. Exigiu `@Autowired` no construtor de injeção do
> `MotorDeVerificacao`: com dois construtores, o Spring não elege nenhum.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivo.java`
- Test: `backend/src/test/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivoTest.java` — acrescentar

**Interfaces:**
- Consumes: `Cenario.usaCompose()` e `Cenario.projetoCompose()` do plano do Cenário #3.
- Produces: `iniciar` executa
  `docker compose -p <projeto> -f <trabalho>/compose.yaml up -d --build` quando o
  arquivo existe no diretório de trabalho após a materialização.

- [ ] **Step 1: Escrever os testes que falham**

Acrescente a `GerenciadorDeCenarioAtivoTest`:

```java
    private Cenario cenarioComposeComArquivo(String id, String projeto) throws Exception {
        Path diretorio = raiz.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(diretorio.resolve("workspace"));
        Files.writeString(diretorio.resolve("workspace").resolve("compose.yaml"),
                "services:\n  alfa:\n    image: alpine\n");
        return new Cenario(id, "titulo", Dificuldade.GUIADO, List.of(), "# corpo",
                diretorio, List.of(), projeto);
    }

    @Test
    void iniciarSobeOComposeQuandoOWorkspaceTrazUm() throws Exception {
        Cenario cenario = cenarioComposeComArquivo("docker/04", "lab-04");
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        Path trabalho = gerenciador(repositorio).iniciar(cenario);

        assertEquals(
                List.of(List.of("docker", "compose", "-p", "lab-04",
                        "-f", trabalho.resolve("compose.yaml").toString(),
                        "up", "-d", "--build")),
                comandosExecutados);
    }

    @Test
    void iniciarNaoSobeComposeQuandoOWorkspaceNaoTrazArquivo() throws Exception {
        Cenario cenario = cenarioCompose("docker/03", "lab-03");
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        gerenciador(repositorio).iniciar(cenario);

        assertTrue(comandosExecutados.stream().noneMatch(c -> c.contains("up")));
    }

    @Test
    void falhaDoComposeUpEhRuidosa() throws Exception {
        Cenario cenario = cenarioComposeComArquivo("docker/04", "lab-04");
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        ExecutorDeComando executorQueFalha = comando ->
                comando.contains("up") ? new SaidaDeComando(1, "", "imagem nao encontrada")
                                       : new SaidaDeComando(0, "", "");
        var gerenciador = new GerenciadorDeCenarioAtivo(
                raiz.resolve("work").toString(),
                executorQueFalha,
                new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString()),
                repositorio);

        var erro = assertThrows(IllegalStateException.class, () -> gerenciador.iniciar(cenario));
        assertTrue(erro.getMessage().contains("lab-04"));
    }
```

- [ ] **Step 2: Rodar para ver falhar**

Run: `cd backend && ./mvnw -B test -Dtest=GerenciadorDeCenarioAtivoTest`
Expected: FAIL — nenhum comando `up` é executado hoje.

- [ ] **Step 3: Subir o stack no `iniciar`**

Em `GerenciadorDeCenarioAtivo.iniciar`, entre a materialização do workspace e a
gravação do progresso:

```java
    public Path iniciar(Cenario cenario) {
        cenarioAtivo()
                .filter(id -> !id.equals(cenario.id()))
                .flatMap(cenarios::buscar)
                .ifPresent(this::derrubar);

        Path trabalho = diretorioDeTrabalho.toAbsolutePath().normalize();
        apagarRecursivamente(trabalho);
        copiarWorkspace(cenario, trabalho);
        subirCompose(cenario, trabalho);

        progressos.salvar(progressos.carregar().comAtivo(cenario.id()));
        return trabalho;
    }
```

E o método novo, ao lado de `derrubar`:

```java
    /** Sobe o stack quando o workspace materializado traz um compose.yaml. */
    private void subirCompose(Cenario cenario, Path trabalho) {
        Path arquivo = trabalho.resolve("compose.yaml");
        if (!cenario.usaCompose() || !Files.isRegularFile(arquivo)) {
            return;
        }
        SaidaDeComando saida = executor.executar(List.of(
                "docker", "compose", "-p", cenario.projetoCompose(),
                "-f", arquivo.toString(), "up", "-d", "--build"));
        if (!saida.sucesso()) {
            throw new IllegalStateException(
                    "não consegui subir o projeto Compose `" + cenario.projetoCompose()
                    + "` do Cenário " + cenario.id() + ": " + saida.stderr().strip()
                    + " — o ambiente deste Cenário não está pronto");
        }
    }
```

Repare que a falha aqui é ruidosa pela razão oposta à do teardown: o teardown estoura
para não deixar sobra envenenar a Verificação seguinte; o setup estoura porque um
ambiente que não subiu faria o leitor diagnosticar um defeito que não é o do exercício.

**Um container que sobe e morre não é falha de `up`.** O `docker compose up -d` retorna
0 assim que os containers foram iniciados; se um deles termina logo depois, o comando já
saiu com sucesso. É exatamente por isso que o Cenário #4 consegue entregar um ambiente
"no ar e quebrado".

- [ ] **Step 4: Rodar a suíte inteira**

Run: `cd backend && ./mvnw -B test`
Expected: PASS, 35 testes (32 anteriores mais 3 novos).

- [ ] **Step 5: Commit**

```bash
git add backend/src
git commit -m "feat: setup com Compose no iniciar"
```

---

### Task 2: O conteúdo do Cenário #4

> **CONCLUÍDA e validada em 2026-08-06** — ambiente sobe quebrado, Verificação dá 2 de
> 4, o defeito é diagnosticável por `ps -a` e `logs`, e depois do conserto as quatro
> passam. Expôs um defeito real no motor de verificação; veja abaixo.

**Files:**
- Create: `content/docker/04-stack-que-nao-sobe/workspace/compose.yaml`
- Create: `content/docker/04-stack-que-nao-sobe/workspace/api/server.js`
- Create: `content/docker/04-stack-que-nao-sobe/workspace/api/Dockerfile`
- Create: `content/docker/04-stack-que-nao-sobe/workspace/web/server.js`
- Create: `content/docker/04-stack-que-nao-sobe/workspace/web/Dockerfile`
- Create: `content/docker/04-stack-que-nao-sobe/verificacao.yaml`
- Create: `content/docker/04-stack-que-nao-sobe/cenario.md`

**Interfaces:**
- Consumes: o setup da Task 1.
- Produces: o Cenário `docker/04-stack-que-nao-sobe`, dificuldade `mestre`.

#### O defeito plantado, e por que este

O `api/Dockerfile` termina com `CMD ["node", "servidor.js"]`, mas o arquivo se chama
`server.js`. O container do `api` sobe e morre imediatamente com `MODULE_NOT_FOUND`.

Isso foi escolhido porque **o sintoma aponta para o lugar errado**. O que o leitor vê
primeiro é o `web` no ar dizendo que não alcançou `http://api:3000` — uma mensagem de
rede. O instinto, logo depois do Cenário #3, é conferir nome de serviço e rede, onde
não há nada errado. A disciplina que o Cenário treina é olhar o estado dos containers
antes de acreditar na mensagem de erro.

- [ ] **Step 1: Os dois serviços**

`workspace/api/server.js`:

```js
const http = require('node:http')

http
  .createServer((_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
    resposta.end(JSON.stringify({ animal: 'tatu-bola' }))
  })
  .listen(3000, () => console.log('api ouvindo na porta 3000'))
```

`workspace/api/Dockerfile` — **com o defeito**, o nome do arquivo no `CMD` está errado:

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY server.js .
EXPOSE 3000
CMD ["node", "servidor.js"]
```

`workspace/web/server.js`:

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

`workspace/web/Dockerfile` — correto:

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY server.js .
EXPOSE 3000
CMD ["node", "server.js"]
```

- [ ] **Step 2: O compose entregue pronto**

`workspace/compose.yaml` — é a presença deste arquivo que faz o `iniciar` subir o stack:

```yaml
services:
  api:
    build: ./api

  web:
    build: ./web
    ports:
      - "8091:3000"
    depends_on:
      - api
```

- [ ] **Step 3: A Verificação**

`verificacao.yaml`:

```yaml
asercoes:
  - tipo: container_rodando
    nome: lab-04-api-1
  - tipo: container_rodando
    nome: lab-04-web-1
  - tipo: http_responde
    url: http://localhost:8091
    status: 200
  - tipo: http_corpo_contem
    url: http://localhost:8091
    texto: "O api disse: tatu-bola"
```

Na primeira Verificação, duas passam e duas falham. Isso é intencional: o `web` está
mesmo no ar e responde 200, e é justamente por isso que o problema parece ser de rede.

- [ ] **Step 4: O Cenário**

`cenario.md`:

````markdown
---
id: docker/04-stack-que-nao-sobe
titulo: Um stack que não sobe
dificuldade: mestre
projetoCompose: lab-04
---
# Um stack que não sobe

Este Cenário é diferente dos anteriores. Não há passo a passo.

O ambiente já subiu quando você clicou em **Iniciar cenário**, e ele está quebrado. Sua
tarefa é descobrir por quê e consertar. Não vou dizer onde olhar.

## O objetivo

`http://localhost:8091` deve mostrar **O api disse: tatu-bola**.

Abra agora. Você vai ver outra coisa.

## O que existe no ambiente

Dois serviços em `compose.yaml`, projeto `lab-04`:

- `api` — deveria responder um JSON na porta 3000, alcançável só de dentro da rede.
- `web` — busca esse JSON e monta a página, publicado na 8091.

Os arquivos estão no seu diretório de trabalho e você pode alterar qualquer um deles.

## As ferramentas

Você já usou todas nos Cenários anteriores, exceto as duas últimas:

```sh
docker compose -p lab-04 ps
docker compose -p lab-04 ps -a
docker compose -p lab-04 logs <serviço>
docker compose -p lab-04 exec <serviço> <comando>
docker compose -p lab-04 up -d --build
```

- `ps` lista **só os containers de pé**. Compare o que ele mostra com os serviços
  declarados no `compose.yaml`: uma ausência nessa lista já é informação.
- `ps -a` inclui os que saíram, com o estado de cada um. A diferença entre os dois
  comandos é a primeira coisa útil deste Cenário.
- `logs` mostra o que o processo escreveu antes de morrer. Um container que sai deixa o
  log para trás; ele não desaparece junto.

Depois de corrigir alguma coisa, é o `up -d --build` que aplica: sem `--build` o Compose
reaproveita a imagem antiga e você vai achar que a correção não funcionou.

## Uma dica sobre método, não sobre o defeito

A primeira mensagem de erro que você encontrar vai sugerir um problema de rede. Antes de
investigar rede, confirme que todos os containers que deveriam estar de pé realmente
estão. Mensagem de conexão recusada tem duas explicações possíveis — o caminho até o
destino está errado, ou não há ninguém no destino — e a segunda é muito mais comum.

Quando **Verificar** apontar uma Asserção falhando, leia o detalhe ao lado com atenção:
ele diz *o que* está errado. Descobrir *por quê* é com você.

## Verifique

Quatro Asserções: os dois containers de pé, a 8091 respondendo 200, e o corpo com a
frase esperada. Duas delas já passam agora — e é exatamente isso que torna o problema
enganoso.

## O que você aprendeu

Que `up` bem-sucedido não significa ambiente saudável: o Compose retorna assim que os
containers foram iniciados, e um deles pode morrer logo depois. Que o log de um
container morto sobrevive a ele. Que `--build` é necessário para que uma correção no
Dockerfile chegue à imagem. E que a mensagem de erro mais visível costuma ser a
consequência, não a causa.
````

- [ ] **Step 5: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS.

- [ ] **Step 6: Percorrer o Cenário à mão**

1. Suba backend e frontend. O catálogo deve listar **quatro** Cenários, e este com o
   rótulo `Mestre` — o primeiro fora do `Guiado`.
2. **Iniciar cenário.** Isto agora demora: o setup constrói duas imagens.
3. Confirme que o stack subiu **quebrado**: `docker compose -p lab-04 ps` deve mostrar o
   `api` fora do ar e o `web` de pé.
4. `http://localhost:8091` deve mostrar a mensagem de erro do `web`.
5. **Verificar**: duas passam, duas falham. A do `api` deve dizer *existe, mas está
   parado*.
6. Corrija o `CMD` do `api/Dockerfile` e rode `docker compose -p lab-04 up -d --build`.
7. **Verificar**: as quatro passam.
8. Abra outro Cenário e confirme que `lab-04` foi derrubado.

- [ ] **Step 7: Commit**

```bash
git add content
git commit -m "feat: cenário docker/04 diagnóstico de stack quebrado"
```

---

## Defeito no motor que este Cenário expôs

O `web` deste Cenário leva **4 a 5 segundos** para responder enquanto quebrado — o
`fetch` dele para um serviço morto só desiste depois do timeout interno do Node. O
`MotorDeVerificacao` esperava **3 segundos** e reportava *"nada respondeu"* para um
serviço vivo que devolvia 200.

Dois erros num só, ambos corrigidos junto com este plano:

- A espera subiu para **10 segundos**. Um Cenário realista tem serviços que dependem de
  serviços, e o tempo de falha se acumula.
- *Timeout* e *conexão recusada* agora dão mensagens diferentes: `HttpTimeoutException`
  vira "não respondeu em Ns — está lento ou travado", enquanto `IOException` segue como
  "nada respondeu em <url>". São diagnósticos opostos, e confundi-los mandava o leitor
  procurar um serviço morto que estava vivo.

A espera é injetável por um construtor de pacote, para que o teste do caso lento rode em
um segundo em vez de dez.

## Depois deste Cenário

Com este plano, todo o ciclo de vida está exercitado: setup com e sem Compose, teardown
com e sem Compose, e os degraus `Guiado` e `Mestre`.

1. **Asserção que distinga "nada respondeu" de "respondeu, mas não é seu container"** —
   segue pendente. O caso do timeout foi resolvido; o do app alheio na porta, não.
2. **Pré-requisitos entre Cenários** — a ordem importa de fato: o #3 assume o #2, e o
   #4 assume o #3.
3. **Os degraus `Assistido` e `Autônomo`** nunca foram usados. A escada tem quatro
   degraus e só dois são exercidos.
