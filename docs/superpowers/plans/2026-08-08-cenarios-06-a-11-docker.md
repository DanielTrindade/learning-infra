# Cenários Docker 06–11 — Expansão prática da Trilha

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Acrescentar os seis Cenários do núcleo planejado na [pesquisa curricular de Docker](../research/docker-course.md) — builds reais, redes, readiness, segurança e distribuição — com as quatro Asserções tipadas que eles exigem.

**Architecture:** O vocabulário de Asserções ganha `container_saudavel`, `container_em_rede`, `container_configuracao` e `imagem_no_registry`, todos avaliados pelo `MotorDeVerificacao` executando o CLI do Docker. Cada Cenário é conteúdo no disco: `workspace/` com o estado inicial (correto ou quebrado), `verificacao.yaml` e `cenario.md` didático. Nenhuma mudança de lifecycle além de registrar os containers/volumes que o teardown já sabe remover.

**Tech Stack:** Java 25, Spring Boot 4, SnakeYAML, Docker Engine 29.x via CLI. Node 22-alpine para as aplicações de exemplo, todas sem dependências externas (zero `npm install`). O `registry:2` do Cenário 10 é a única imagem nova baixada do Docker Hub.

## Contexto obrigatório

Leia [`CONTEXT.md`](../../../CONTEXT.md), as ADRs em [`docs/adr/`](../../adr/) e a
[pesquisa curricular de Docker](../research/docker-course.md). Os planos dos Cenários
[#1](./2026-08-04-esqueleto-cenario-docker.md), [#2](./2026-08-06-cenario-02-dockerfile.md),
[#3](./2026-08-06-cenario-03-compose.md), [#4](./2026-08-06-cenario-04-diagnostico.md) e
[#5](./2026-08-06-cenario-05-volumes.md) e do [Fundamentos e Questionário](./2026-08-07-fundamentos-questionario-por-trilha.md)
já estão executados. Os Cenários atuais usam as portas 8088–8091; o backend usa 8099.

## Global Constraints

- **Todas as restrições dos planos anteriores continuam valendo.**
- **Portas de host novas:** 06→8092, 07→8093, 08→8094 (api) e 8095 (web), 09→8096,
  10→8097, 11→8098. Nenhuma colide com as portas 8088–8091 nem com a 8099 do backend.
- **Nomes de containers Compose seguem `<projeto>-<serviço>-1`.** Projetos: `lab-06` a
  `lab-11`.
- **Aplicações sem dependência externa.** Todos os `server.js` usam só o módulo `http`
  do Node. Nada de `npm install` — a rede do npm não é pré-requisito.
- **Só o Cenário 10 baixa imagem nova** (`registry:2`). 07 usa só os serviços de Node
  entregues; 08 e 11 também. Postgres/Redis reais ficam de fora para não depender de
  pull e de cliente npm.
- **A Verificação é observadora:** nenhuma Asserção nova pode criar recurso. A lição do
  Cenário #5 sobre o `-v` criar volume por engano segue valendo.
- **O daemon do Docker precisa estar no ar** para todas as tarefas.
- **Asserções tipadas só quando um Cenário as exige.** `comando_produz` permanece como
  escape hatch e é usado onde um check tipado seria over-engineering.

## Decisões fechadas

- **Dificuldades:** 06 assistido, 07 autônomo, 08 assistido, 09 assistido, 10 assistido,
  11 mestre. 07 é o primeiro `autônomo` da plataforma (objetivo + ambiente, sem passos).
- **Cada Cenário novo declara o que o teardown precisa remover:** 06 e 10 declaram
  `containers`; 08, 09 e 11 usam `projetoCompose`; 11 declara também `volumes`.
- **`container_configuracao` tem campos opcionais** (`usuario`, `somenteLeitura`,
  `capabilitiesRemovidas`) e avalia apenas os presentes. A descrição deriva dos campos.
- **`imagem_no_registry` verifica com `docker manifest inspect --insecure`** contra o
  registry local; não usa a API HTTP do daemon. O `--insecure` é obrigatório porque o
  registry local é HTTP sem TLS (verificado na engine 29.6.1: sem a flag, `manifest
  inspect` sai com código 1 e *no such manifest*).
- **O degrau autônomo de 07 entrega `compose.yaml` quebrado no workspace** (todas as
  portas publicadas, uma rede só) e o correto é descoberto pelo leitor.
- **O Cenário 10 usa `containers: [lab-10-app, lab-10-registry]`** para o teardown
  derrubar o registry descartável.
- **O rollback por digest é demonstrado no texto; a Verificação confirma o efeito** (o
  app no ar servindo a versão boa), não a referência exata usada no `docker run`.

---

## Fase 1 — Asserções tipadas novas

Todas seguem o mesmo ciclo TDD dos planos anteriores: testes primeiro, o `switch`
exaustivo quebra a compilação, tratar os casos novos no motor e no leitor.

### Task 1: `container_saudavel` e `container_em_rede`

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/Assercao.java`
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Test: `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java`
- Test: `backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java`

**Interfaces:**
- Produces:
  - `record Assercao.ContainerSaudavel(String nome)`
  - `record Assercao.ContainerEmRede(String nome, String rede, boolean presente)`
  - Tipos YAML `container_saudavel` (campo `nome`) e `container_em_rede` (campos
    `nome`, `rede`, `presente`).

#### Semântica

- `container_saudavel`: roda `docker inspect -f '{{.State.Health.Status}}' <nome>` e
  aprova quando a saída é exatamente `healthy`. Container sem healthcheck devolve
  `<no value>` com exit 0 — falha com mensagem que mostra o status atual.
- `container_em_rede`: roda
  `docker inspect -f '{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}' <nome>`
  e confere se o nome da rede está na lista. O campo `presente: false` permite afirmar
  que um container **não** está numa rede — a prova de isolamento do Cenário 07.

- [x] **Step 1: Escrever os testes que falham**

Acrescente a `MotorDeVerificacaoTest`:

```java
    @Test
    void containerSaudavelPassaQuandoHealthDizHealthy() {
        var resultado = motorQueResponde("healthy\n", 0)
                .verificar(List.of(new Assercao.ContainerSaudavel("lab-08-db-1")));

        assertTrue(resultado.concluido());
    }

    @Test
    void containerSaudavelFalhaQuandoNaoEstaHealthy() {
        var resultado = motorQueResponde("starting\n", 0)
                .verificar(List.of(new Assercao.ContainerSaudavel("lab-08-db-1")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("starting"));
    }

    @Test
    void containerEmRedePassaQuandoOPertence() {
        var resultado = motorQueResponde("lab-07-borda lab-07-interna\n", 0)
                .verificar(List.of(new Assercao.ContainerEmRede("lab-07-api-1", "lab-07-borda", true)));

        assertTrue(resultado.concluido());
    }

    @Test
    void containerEmRedeFalhaQuandoNaoPertenceMasDeveria() {
        var resultado = motorQueResponde("lab-07-borda\n", 0)
                .verificar(List.of(new Assercao.ContainerEmRede("lab-07-db-1", "lab-07-interna", true)));

        assertFalse(resultado.concluido());
    }

    @Test
    void containerForaDaRedePassaComPresenteFalse() {
        var resultado = motorQueResponde("lab-07-interna\n", 0)
                .verificar(List.of(new Assercao.ContainerEmRede("lab-07-db-1", "lab-07-borda", false)));

        assertTrue(resultado.concluido());
    }

    @Test
    void containerEmRedeIgnoraNomeParcial() {
        var resultado = motorQueResponde("lab-07\n", 0)
                .verificar(List.of(new Assercao.ContainerEmRede("lab-07-api-1", "lab-07-borda", true)));

        assertFalse(resultado.concluido(), "nome parcial de rede não pode aprovar");
    }
```

E a `LeitorDeCenarioTest`:

```java
    @Test
    void leAsercoesDeSaudeEDeRede() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_saudavel
                    nome: lab-08-db-1
                  - tipo: container_em_rede
                    nome: lab-07-db-1
                    rede: lab-07-interna
                    presente: true
                """);

        List<Assercao> asercoes = new LeitorDeCenario().ler(diretorio).asercoes();

        assertEquals(new Assercao.ContainerSaudavel("lab-08-db-1"), asercoes.get(0));
        assertEquals(
                new Assercao.ContainerEmRede("lab-07-db-1", "lab-07-interna", true),
                asercoes.get(1));
    }
```

- [x] **Step 2: Rodar para ver falhar**

Run: `cd backend && ./mvnw -B test -Dtest=MotorDeVerificacaoTest`
Expected: FAIL na compilação — os records novos não existem.

- [x] **Step 3: Acrescentar os records**

Em `Assercao.java`, depois de `VolumeExiste`:

```java
    record ContainerSaudavel(String nome) implements Assercao {
        @Override
        public String descricao() {
            return "o container `" + nome + "` está saudável";
        }
    }

    record ContainerEmRede(String nome, String rede, boolean presente) implements Assercao {
        @Override
        public String descricao() {
            return presente
                    ? "o container `" + nome + "` está na rede `" + rede + "`"
                    : "o container `" + nome + "` não está na rede `" + rede + "`";
        }
    }
```

- [x] **Step 4: Recompilar e ver o switch quebrar**

Run: `cd backend && ./mvnw -B -q compile`
Expected: FAIL — *the switch expression does not cover all possible input values*. Não
adicione `default`.

- [x] **Step 5: Tratar os casos novos no motor**

Em `MotorDeVerificacao.avaliar`:

```java
            case Assercao.ContainerSaudavel a -> avaliarSaude(a);
            case Assercao.ContainerEmRede a -> avaliarEmRede(a);
```

E os métodos:

```java
    private ResultadoDeAsercao avaliarSaude(Assercao.ContainerSaudavel a) {
        SaidaDeComando saida = executor.executar(List.of(
                "docker", "inspect", "-f", "{{.State.Health.Status}}", a.nome()));
        if (!saida.sucesso()) {
            return ResultadoDeAsercao.reprovada(a, "o container `" + a.nome() + "` não existe");
        }
        String status = saida.stdout().strip();
        return status.equals("healthy")
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a, "não está saudável — status atual: `" + status + "`");
    }

    private ResultadoDeAsercao avaliarEmRede(Assercao.ContainerEmRede a) {
        SaidaDeComando saida = executor.executar(List.of(
                "docker", "inspect", "-f",
                "{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}", a.nome()));
        if (!saida.sucesso()) {
            return ResultadoDeAsercao.reprovada(a, "o container `" + a.nome() + "` não existe");
        }
        boolean pertence = java.util.Arrays.stream(saida.stdout().strip().split("\\s+"))
                .anyMatch(rede -> rede.equals(a.rede()));
        boolean ok = pertence == a.presente();
        return ok
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a,
                        pertence
                                ? "está na rede `" + a.rede() + "` e não deveria"
                                : "não está na rede `" + a.rede() + "`");
    }
```

- [x] **Step 6: Ler os tipos novos no YAML**

Em `LeitorDeCenario.montarAsercao`, antes do `default`:

```java
            case "container_saudavel" -> new Assercao.ContainerSaudavel(exigirTexto(item, "nome"));
            case "container_em_rede" -> new Assercao.ContainerEmRede(
                    exigirTexto(item, "nome"),
                    exigirTexto(item, "rede"),
                    exigirBooleano(item, "presente"));
```

- [x] **Step 7: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS.

- [x] **Step 8: Commit**

```bash
git add backend/src && git commit -m "feat: asserções container_saudavel e container_em_rede"
```

---

### Task 2: `container_configuracao`

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/Assercao.java`
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Test: `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java`
- Test: `backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java`

**Interfaces:**
- Produces: `record Assercao.ContainerConfiguracao(String nome, String usuario,
  Boolean somenteLeitura, List<String> capabilitiesRemovidas)`.
  Tipo YAML `container_configuracao`: `nome` obrigatório; `usuario`, `somenteLeitura` e
  `capabilitiesRemovidas` opcionais.

#### Semântica

Usada pelo Cenário 09 (usuário não-root, filesystem somente leitura, capabilities
removidas). Cada campo presente é verificado com um `docker inspect` próprio, e o
primeiro que falhar reprova com mensagem específica:

- `usuario` → `docker inspect -f '{{.Config.User}}'` deve ser exatamente o esperado;
- `somenteLeitura` → `docker inspect -f '{{.HostConfig.ReadonlyRootfs}}'` deve ser
  `true` ou `false` conforme o campo;
- `capabilitiesRemovidas` → `docker inspect -f '{{json .HostConfig.CapDrop}}'` deve
  conter cada capability da lista (comparação case-insensitive, já que o Docker
  normaliza para maiúsculas).

- [x] **Step 1: Escrever os testes que falham**

Acrescente a `MotorDeVerificacaoTest`:

```java
    @Test
    void containerConfiguracaoValidaUsuarioSomenteLeituraECapabilities() {
        // O motor consulta o daemon 3 vezes; responda na ordem pedida com um executor por campo.
        var executor = new ExecutorDeComando() {
            int chamadas = 0;

            @Override
            public SaidaDeComando executar(java.util.List<String> comando) {
                chamadas++;
                return new SaidaDeComando(0,
                        switch (chamadas) {
                            case 1 -> "node\n";
                            case 2 -> "true\n";
                            default -> "[\"ALL\"]\n";
                        }, "");
            }
        };

        var resultado = new MotorDeVerificacao(executor).verificar(List.of(
                new Assercao.ContainerConfiguracao(
                        "lab-09-app-1", "node", true, java.util.List.of("ALL"))));

        assertTrue(resultado.concluido());
    }

    @Test
    void containerConfiguracaoReprovaUsuarioErrado() {
        var executor = comando -> new SaidaDeComando(0, "root\n", "");
        var resultado = new MotorDeVerificacao(executor).verificar(List.of(
                new Assercao.ContainerConfiguracao("lab-09-app-1", "node", null, null)));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("node"));
    }

    @Test
    void containerConfiguracaoReprovaCapabilityNaoRemovida() {
        var executor = comando -> new SaidaDeComando(0, "[]\n", "");
        var resultado = new MotorDeVerificacao(executor).verificar(List.of(
                new Assercao.ContainerConfiguracao(
                        "lab-09-app-1", null, null, java.util.List.of("ALL"))));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("ALL"));
    }
```

E a `LeitorDeCenarioTest`:

```java
    @Test
    void leContainerConfiguracaoComCamposOpcionais() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_configuracao
                    nome: lab-09-app-1
                    usuario: node
                    somenteLeitura: true
                    capabilitiesRemovidas: [ALL]
                """);

        var asercao = new LeitorDeCenario().ler(diretorio).asercoes().getFirst();

        assertEquals(
                new Assercao.ContainerConfiguracao(
                        "lab-09-app-1", "node", true, java.util.List.of("ALL")),
                asercao);
    }
```

- [x] **Step 2: Rodar para ver falhar**

Run: `cd backend && ./mvnw -B test -Dtest=MotorDeVerificacaoTest`
Expected: FAIL na compilação.

- [x] **Step 3: Acrescentar o record**

Em `Assercao.java`:

```java
    /**
     * Verifica a configuração de execução de um container: usuário, filesystem somente
     * leitura e capabilities removidas. Campos nulos não são verificados. A descrição
     * deriva dos campos presentes.
     */
    record ContainerConfiguracao(
            String nome,
            String usuario,
            Boolean somenteLeitura,
            java.util.List<String> capabilitiesRemovidas) implements Assercao {

        @Override
        public String descricao() {
            java.util.List<String> partes = new java.util.ArrayList<>();
            if (usuario != null) {
                partes.add("roda como `" + usuario + "`");
            }
            if (somenteLeitura != null && somenteLeitura) {
                partes.add("com filesystem somente leitura");
            }
            if (capabilitiesRemovidas != null && !capabilitiesRemovidas.isEmpty()) {
                partes.add("sem as capabilities " + capabilitiesRemovidas);
            }
            return "o container `" + nome + "` " + String.join(", ", partes);
        }
    }
```

- [x] **Step 4: Tratar o caso novo no motor**

Em `MotorDeVerificacao.avaliar`:

```java
            case Assercao.ContainerConfiguracao a -> avaliarConfiguracao(a);
```

E o método — note que cada campo consulta o daemon separadamente:

```java
    private ResultadoDeAsercao avaliarConfiguracao(Assercao.ContainerConfiguracao a) {
        if (a.usuario() != null) {
            SaidaDeComando saida = executor.executar(List.of(
                    "docker", "inspect", "-f", "{{.Config.User}}", a.nome()));
            if (!saida.sucesso()) {
                return ResultadoDeAsercao.reprovada(a, "o container `" + a.nome() + "` não existe");
            }
            if (!saida.stdout().strip().equals(a.usuario())) {
                return ResultadoDeAsercao.reprovada(a,
                        "esperava o usuário `" + a.usuario() + "`, veio `"
                                + saida.stdout().strip() + "`");
            }
        }
        if (a.somenteLeitura() != null) {
            SaidaDeComando saida = executor.executar(List.of(
                    "docker", "inspect", "-f", "{{.HostConfig.ReadonlyRootfs}}", a.nome()));
            if (!saida.sucesso()) {
                return ResultadoDeAsercao.reprovada(a, "o container `" + a.nome() + "` não existe");
            }
            String esperado = String.valueOf(a.somenteLeitura());
            if (!saida.stdout().strip().equals(esperado)) {
                return ResultadoDeAsercao.reprovada(a,
                        "filesystem somente leitura era `" + esperado + "`, veio `"
                                + saida.stdout().strip() + "`");
            }
        }
        if (a.capabilitiesRemovidas() != null && !a.capabilitiesRemovidas().isEmpty()) {
            SaidaDeComando saida = executor.executar(List.of(
                    "docker", "inspect", "-f", "{{json .HostConfig.CapDrop}}", a.nome()));
            if (!saida.sucesso()) {
                return ResultadoDeAsercao.reprovada(a, "o container `" + a.nome() + "` não existe");
            }
            String drop = saida.stdout().toLowerCase();
            for (String capability : a.capabilitiesRemovidas()) {
                if (!drop.contains(capability.toLowerCase())) {
                    return ResultadoDeAsercao.reprovada(a,
                            "a capability `" + capability + "` não foi removida");
                }
            }
        }
        return ResultadoDeAsercao.aprovada(a);
    }
```

- [x] **Step 5: Ler o tipo no YAML**

Em `LeitorDeCenario.montarAsercao`:

```java
            case "container_configuracao" -> new Assercao.ContainerConfiguracao(
                    exigirTexto(item, "nome"),
                    textoOpcional(item, "usuario"),
                    (Boolean) item.getOrDefault("somenteLeitura", null),
                    lerListaOpcional(item, "capabilitiesRemovidas"));
```

`textoOpcional` já existe; `getOrDefault` devolve `null` quando ausente.

- [x] **Step 6: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS.

- [x] **Step 7: Commit**

```bash
git add backend/src && git commit -m "feat: asserção container_configuracao"
```

---

### Task 3: `imagem_no_registry`

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/Assercao.java`
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Test: `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java`
- Test: `backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java`

**Interfaces:**
- Produces: `record Assercao.ImagemNoRegistry(String referencia, String descricao)`.
  Tipo YAML `imagem_no_registry` (campos `referencia`, `descricao`). A descrição vem do
  Cenário porque a referência `localhost:5000/lab-10-app:1.0` entregaria a resposta do
  exercício.

#### Semântica

Roda `docker manifest inspect --insecure <referencia>`. O comando fala com o registry
referenciado (local, no Cenário 10) e sai com código 0 quando a imagem existe lá. Com o
registry fora do ar ou a imagem ausente, sai com código 1. O `--insecure` é necessário
porque o registry local é HTTP sem TLS (a flag permite a comunicação); sem ela o comando
sai com código 1 mesmo com a imagem publicada. Verificado na engine 29.6.1.

- [x] **Step 1: Escrever os testes que falham**

Acrescente a `MotorDeVerificacaoTest`:

```java
    @Test
    void imagemNoRegistryPassaQuandoManifestInspectDaCerto() {
        var resultado = motorQueResponde("{\"schemaVersion\":2}\n", 0)
                .verificar(List.of(new Assercao.ImagemNoRegistry(
                        "localhost:5000/lab-10-app:1.0", "a v1.0 está publicada no registry")));

        assertTrue(resultado.concluido());
    }

    @Test
    void imagemForaDoRegistryFalhaDizendoQueNaoFoiPublicada() {
        var resultado = motorQueResponde("", 1)
                .verificar(List.of(new Assercao.ImagemNoRegistry(
                        "localhost:5000/lab-10-app:1.0", "a v1.0 está publicada no registry")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("registry"));
    }
```

E a `LeitorDeCenarioTest`:

```java
    @Test
    void leImagemNoRegistryComDescricaoDoCenario() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: imagem_no_registry
                    referencia: localhost:5000/lab-10-app:1.0
                    descricao: a v1.0 está publicada no registry
                """);

        var asercao = new LeitorDeCenario().ler(diretorio).asercoes().getFirst();

        assertEquals(
                new Assercao.ImagemNoRegistry("localhost:5000/lab-10-app:1.0",
                        "a v1.0 está publicada no registry"),
                asercao);
    }
```

- [x] **Step 2: Rodar para ver falhar**

Run: `cd backend && ./mvnw -B test -Dtest=MotorDeVerificacaoTest`
Expected: FAIL na compilação.

- [x] **Step 3: Acrescentar o record**

Em `Assercao.java`:

```java
    /** A imagem está publicada no registry referenciado. A descrição vem do Cenário. */
    record ImagemNoRegistry(String referencia, String descricao) implements Assercao {
    }
```

- [x] **Step 4: Tratar o caso novo no motor**

Em `MotorDeVerificacao.avaliar`:

```java
            case Assercao.ImagemNoRegistry a -> avaliarImagemNoRegistry(a);
```

E o método:

```java
    private ResultadoDeAsercao avaliarImagemNoRegistry(Assercao.ImagemNoRegistry a) {
        SaidaDeComando saida = executor.executar(List.of(
                "docker", "manifest", "inspect", "--insecure", a.referencia()));
        return saida.sucesso()
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a,
                        "a imagem `" + a.referencia() + "` não está no registry — "
                        + "confirme que o registry está no ar e que a imagem foi publicada");
    }
```

- [x] **Step 5: Ler o tipo no YAML**

Em `LeitorDeCenario.montarAsercao`:

```java
            case "imagem_no_registry" -> new Assercao.ImagemNoRegistry(
                    exigirTexto(item, "referencia"),
                    exigirTexto(item, "descricao"));
```

- [x] **Step 6: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS.

- [x] **Step 7: Commit**

```bash
git add backend/src && git commit -m "feat: asserção imagem_no_registry"
```

---

## Fase 2 — Cenário 06: Uma imagem cara e lenta

**Files:**
- Create: `content/docker/06-uma-imagem-cara-e-lenta/workspace/app/Dockerfile`
- Create: `content/docker/06-uma-imagem-cara-e-lenta/workspace/app/package.json`
- Create: `content/docker/06-uma-imagem-cara-e-lenta/workspace/app/build.js`
- Create: `content/docker/06-uma-imagem-cara-e-lenta/workspace/app/src/server.js`
- Create: `content/docker/06-uma-imagem-cara-e-lenta/workspace/app/vendor/lib.js`
- Create: `content/docker/06-uma-imagem-cara-e-lenta/workspace/app/artefatos/notas.txt`
- Create: `content/docker/06-uma-imagem-cara-e-lenta/workspace/app/logs/app.log`
- Create: `content/docker/06-uma-imagem-cara-e-lenta/workspace/app/node_modules/.cache/pacote.txt`
- Create: `content/docker/06-uma-imagem-cara-e-lenta/verificacao.yaml`
- Create: `content/docker/06-uma-imagem-cara-e-lenta/cenario.md`

**Interfaces:**
- Consumes: `imagem_existe`, `container_rodando`, `http_responde`, `http_corpo_contem`,
  `container_configuracao`, `comando_produz`.
- Produces: frontmatter `containers: [lab-06-app]`.

O workspace entrega uma aplicação Node pura (zero `npm install`, zero rede do npm) cujo
build é um script local `build.js` que junta `vendor/` e `src/` num `dist/server.js`. O
Dockerfile entregue é o pior caso: copia o repositório inteiro com `COPY . .` (entulho
incluído, cache todo invalidado) e deixa o script de build na imagem final. O leitor
deve criar `.dockerignore`, ordenar as camadas para preservar cache e usar multi-stage —
tudo recomendação oficial
([boas práticas](https://docs.docker.com/build/building/best-practices/)).

- [x] **Step 1: O workspace — código-fonte**

`workspace/app/src/server.js`:

```js
const http = require('node:http')

http
  .createServer((_req, res) => {
    res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8' })
    res.end(titulo())
  })
  .listen(3000, () => console.log('app ouvindo na porta 3000'))
```

`workspace/app/vendor/lib.js` (a "dependência" que muda raramente):

```js
function titulo() {
  return 'Cenário 06 otimizado'
}
```

`workspace/app/build.js` (o "build": concatena vendor + src num bundle só):

```js
const fs = require('node:fs')

const vendor = fs.readFileSync('vendor/lib.js', 'utf8')
const src = fs.readFileSync('src/server.js', 'utf8')
fs.mkdirSync('dist', { recursive: true })
fs.writeFileSync('dist/server.js', vendor + '\n' + src)
```

`workspace/app/package.json` — sem dependências; `npm` não é usado em lugar nenhum:

```json
{
  "name": "lab-06-app",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "build": "node build.js",
    "start": "node dist/server.js"
  }
}
```

`workspace/app/Dockerfile` (o entregue, deliberadamente ruim):

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY . .
RUN node build.js
EXPOSE 3000
CMD ["node", "dist/server.js"]
```

Entulho para provar que o contexto está sendo enviado inteiro: `artefatos/notas.txt`,
`logs/app.log` e `node_modules/.cache/pacote.txt` com qualquer conteúdo. Estes arquivos
não são usados pela aplicação.

- [x] **Step 2: O exercício (resumo do que o `cenario.md` deve ensinar)**

1. Medir o tamanho do contexto: rodar `docker build -t lab-06-app:antes .` e observar o
   `Sending build context`, que conta todo o diretório.
2. Criar `.dockerignore` com `node_modules`, `dist`, `logs`, `artefatos`, `.git`.
3. Reordenar o Dockerfile: copiar primeiro o que muda raramente
   (`COPY package.json vendor/ .`) e o `build.js`, depois o `src/` — só a camada do
   `src/` invalida quando o código muda, as anteriores ficam em cache.
4. Multi-stage: o primeiro estágio roda `node build.js`; o estágio final copia só
   `dist/` — a imagem final não carrega `build.js` nem `src/`.
5. Exigir a tag final `lab-06-app:final` e rodar:
   `docker run -d --name lab-06-app -p 8092:3000 lab-06-app:final`.
6. Confirmar o ganho: `docker images` comparando as duas imagens e a saída do build
   mostrando `CACHED` nas camadas de dependência.

A Verificação exige a imagem `lab-06-app:final` e o container `lab-06-app`.

- [x] **Step 3: A Verificação**

`verificacao.yaml`:

```yaml
asercoes:
  - tipo: imagem_existe
    referencia: lab-06-app:final

  - tipo: container_rodando
    nome: lab-06-app

  - tipo: http_responde
    url: http://localhost:8092
    status: 200

  - tipo: http_corpo_contem
    url: http://localhost:8092
    texto: Cenário 06 otimizado

  - tipo: container_configuracao
    nome: lab-06-app
    usuario: node

  - tipo: comando_produz
    descricao: a imagem final não carrega o script de build
    comando: ["docker", "run", "--rm", "--entrypoint", "sh", "lab-06-app:final", "-c", "test ! -f build.js && echo sem-build-tools"]
    contem: sem-build-tools
```

A ordem dá o diagnóstico em degraus: sem a imagem correta, nada mais faz sentido; sem o
container, as três HTTP falham juntas. A `container_configuracao` exige que a imagem
final rode como `node` (usuário criado com `USER node` no estágio final — a imagem
`node:22-alpine` já tem esse usuário). A `comando_produz` é o check que entrega a lição:
`test ! -f build.js` verifica que o script de build **não** está na imagem final — com o
Dockerfile entregue (`COPY . .`) o `build.js` está lá e a Asserção falha; só o
multi-stage o deixa fora. Note que o `Dockerfile` do exercício **não pode** incluir
`USER node` como parte da solução entregue — o leitor o adiciona no estágio final do
multi-stage.

- [x] **Step 4: O frontmatter do `cenario.md`**

```markdown
---
id: docker/06-uma-imagem-cara-e-lenta
titulo: Uma imagem cara e lenta
dificuldade: assistido
containers: [lab-06-app]
---
```

O texto segue o formato dos Cenários anteriores (`Assistido`: objetivo e a forma dos
comandos, não os comandos prontos) e termina com "O que você aprendeu".

- [x] **Step 5: Percorrer o Cenário à mão**

1. **Iniciar cenário**; confirmar que o workspace materializou `app/` com o Dockerfile
   ruim.
2. **Verificar** sem fazer nada: as Asserções falham (imagem não existe).
3. Fazer o exercício. **Verificar**: as seis passam.
4. Confirmar o ganho real: `docker images` mostra `lab-06-app:antes` maior que
   `lab-06-app:final`.
5. Confirmar que o teardown remove o `lab-06-app` ao abrir outro Cenário.

- [x] **Step 6: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS (o `CatalogoRealTest` ainda não conhece o Cenário 06 — ver Fase 8).

- [x] **Step 7: Commit**

```bash
git add content/docker/06-uma-imagem-cara-e-lenta && git commit -m "feat: cenário docker/06 uma imagem cara e lenta"
```

---

## Fase 3 — Cenário 07: Só quem precisa se enxerga

**Files:**
- Create: `content/docker/07-so-quem-precisa-se-enxerga/workspace/compose.yaml`
- Create: `content/docker/07-so-quem-precisa-se-enxerga/workspace/db/server.js`
- Create: `content/docker/07-so-quem-precisa-se-enxerga/workspace/db/Dockerfile`
- Create: `content/docker/07-so-quem-precisa-se-enxerga/workspace/api/server.js`
- Create: `content/docker/07-so-quem-precisa-se-enxerga/workspace/api/Dockerfile`
- Create: `content/docker/07-so-quem-precisa-se-enxerga/workspace/web/server.js`
- Create: `content/docker/07-so-quem-precisa-se-enxerga/workspace/web/Dockerfile`
- Create: `content/docker/07-so-quem-precisa-se-enxerga/verificacao.yaml`
- Create: `content/docker/07-so-quem-precisa-se-enxerga/cenario.md`

**Interfaces:**
- Consumes: `container_rodando`, `http_responde`, `http_corpo_contem`,
  `container_em_rede`.
- Produces: frontmatter `projetoCompose: lab-07`. Primeiro Cenário `autônomo`.

O workspace entrega três serviços e um `compose.yaml` quebrado: todos na mesma rede
(padrão do Compose) e todas as portas publicadas. O objetivo do leitor é criar duas
redes — `lab-07-borda` (só o `web`) e `lab-07-interna` (`api` e `db`) — colocar o `api`
nas duas, remover a publicação de porta do `db` e usar o DNS por nome de serviço.

- [x] **Step 1: O workspace — aplicações**

`workspace/db/server.js` (o "banco" de mentira: sem dependência, um servidor que
responde um animal):

```js
const http = require('node:http')

http
  .createServer((_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
    resposta.end(JSON.stringify({ animal: 'pinguim-magalhaes' }))
  })
  .listen(3000, () => console.log('db ouvindo na porta 3000'))
```

`workspace/db/Dockerfile`:

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY server.js .
EXPOSE 3000
CMD ["node", "server.js"]
```

`workspace/api/server.js` — busca o `db` por nome de serviço e repassa:

```js
const http = require('node:http')

async function buscar() {
  const resposta = await fetch('http://db:3000')
  return resposta.json()
}

http
  .createServer(async (_req, resposta) => {
    const dados = await buscar()
    resposta.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
    resposta.end(JSON.stringify({ animal: dados.animal }))
  })
  .listen(3000, () => console.log('api ouvindo na porta 3000'))
```

`workspace/web/server.js` — busca o `api` e monta a página:

```js
const http = require('node:http')

http
  .createServer(async (_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
    try {
      const resultado = await fetch('http://api:3000')
      const dados = await resultado.json()
      resposta.end(`<!doctype html><title>web</title><h1>O api disse: ${dados.animal}</h1>`)
    } catch (erro) {
      resposta.end(
        `<!doctype html><title>web</title><h1>nao alcancei http://api:3000</h1><pre>${erro.message}</pre>`,
      )
    }
  })
  .listen(3000, () => console.log('web ouvindo na porta 3000'))
```

`api/Dockerfile` e `web/Dockerfile` iguais ao do `db`.

- [x] **Step 2: O `compose.yaml` entregue (quebrado)**

```yaml
services:
  web:
    build: ./web
    ports:
      - "8093:3000"
    depends_on:
      - api

  api:
    build: ./api
    ports:
      - "8094:3000"

  db:
    build: ./db
    ports:
      - "8095:3000"
```

Todas as portas publicadas e tudo na rede padrão. O correto (descoberto pelo leitor):

```yaml
services:
  web:
    build: ./web
    ports:
      - "8093:3000"
    networks:
      - lab-07-borda

  api:
    build: ./api
    networks:
      - lab-07-borda
      - lab-07-interna

  db:
    build: ./db
    networks:
      - lab-07-interna

networks:
  lab-07-borda:
    name: lab-07-borda
  lab-07-interna:
    name: lab-07-interna
```

O `web` exposto no host, o `db` invisível, o `api` fazendo a ponte entre as duas redes.

O `name:` explícito nas redes **não é opcional**: sem ele o Compose cria a rede com o
prefixo do projeto (`lab-07_lab-07-borda`), e a Asserção `container_em_rede` procura o
nome exato `lab-07-borda`. Verificado na engine 29.6.1.

- [x] **Step 3: A Verificação**

`verificacao.yaml`:

```yaml
asercoes:
  - tipo: container_rodando
    nome: lab-07-web-1
  - tipo: container_rodando
    nome: lab-07-api-1
  - tipo: container_rodando
    nome: lab-07-db-1

  - tipo: http_responde
    url: http://localhost:8093
    status: 200

  - tipo: http_corpo_contem
    url: http://localhost:8093
    texto: "O api disse: pinguim-magalhaes"

  - tipo: container_em_rede
    nome: lab-07-web-1
    rede: lab-07-borda
    presente: true

  - tipo: container_em_rede
    nome: lab-07-api-1
    rede: lab-07-borda
    presente: true

  - tipo: container_em_rede
    nome: lab-07-api-1
    rede: lab-07-interna
    presente: true

  - tipo: container_em_rede
    nome: lab-07-db-1
    rede: lab-07-interna
    presente: true

  - tipo: container_em_rede
    nome: lab-07-db-1
    rede: lab-07-borda
    presente: false
```

A última Asserção é o coração do Cenário: o banco **não** está na rede de borda. O
corpo da página (`O api disse: pinguim-magalhaes`) só aparece se o DNS por nome de
serviço funcionou de `web` → `api` → `db`.

- [x] **Step 4: O frontmatter do `cenario.md`**

```markdown
---
id: docker/07-so-quem-precisa-se-enxerga
titulo: Só quem precisa se enxerga
dificuldade: autonomo
projetoCompose: lab-07
---
```

`autonomo` = objetivo e ambiente, sem passos. O texto explica o que o leitor deve
provar (o `web` alcança o host; o `db` não; o `api` conversa com os dois) e oferece as
ferramentas (`docker compose -p lab-07 ps`, `exec`, `network ls`), mas não a solução.
É o primeiro Cenário a usar esse degrau.

- [x] **Step 5: Percorrer o Cenário à mão**

1. **Iniciar cenário**: o `compose.yaml` quebrado sobe sozinho (o lifecycle já faz
   `up -d --build` quando há `compose.yaml` no workspace).
2. **Verificar** sem fazer nada: as HTTP passam (tudo publicado), mas as Asserções
   `container_em_rede` com `presente: true` falham — o `web` não está na `lab-07-borda`,
   o `api` não está na `lab-07-interna` e o `db` não está na `lab-07-interna`.
3. Fazer o exercício. **Verificar**: as dez Asserções passam.
4. Confirmar o isolamento de verdade: `docker compose -p lab-07 exec db wget -qO- http://localhost:3000`
   responde, enquanto `docker port lab-07-db-1` não mostra nada.
5. Confirmar que o teardown derruba as redes (`docker network ls` sem `lab-07-*`).

- [x] **Step 6: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS.

- [x] **Step 7: Commit**

```bash
git add content/docker/07-so-quem-precisa-se-enxerga && git commit -m "feat: cenário docker/07 só quem precisa se enxerga"
```

---

## Fase 4 — Cenário 08: Rodando ainda não é pronto

**Files:**
- Create: `content/docker/08-rodando-nao-e-pronto/workspace/compose.yaml`
- Create: `content/docker/08-rodando-nao-e-pronto/workspace/db/server.js`
- Create: `content/docker/08-rodando-nao-e-pronto/workspace/db/Dockerfile`
- Create: `content/docker/08-rodando-nao-e-pronto/workspace/api/server.js`
- Create: `content/docker/08-rodando-nao-e-pronto/workspace/api/Dockerfile`
- Create: `content/docker/08-rodando-nao-e-pronto/workspace/web/server.js`
- Create: `content/docker/08-rodando-nao-e-pronto/workspace/web/Dockerfile`
- Create: `content/docker/08-rodando-nao-e-pronto/verificacao.yaml`
- Create: `content/docker/08-rodando-nao-e-pronto/cenario.md`

**Interfaces:**
- Consumes: `container_saudavel`, `container_rodando`, `http_responde`,
  `http_corpo_contem`.
- Produces: frontmatter `projetoCompose: lab-08`.

O workspace entrega uma API que tenta o banco uma única vez na subida e **sai** se ele
não responder. O `compose.yaml` entregue usa `depends_on` simples — que só ordena a
partida, não espera prontidão. O leitor deve criar `healthcheck` no `db`, trocar a
dependência do `api` para `condition: service_healthy`, e adicionar retries no `api`
porque readiness não elimina falhas posteriores.

- [x] **Step 1: O workspace — aplicações**

`workspace/db/server.js` — demora 8s para começar a escutar (simula banco lento):

```js
const http = require('node:http')

setTimeout(() => {
  http
    .createServer((_req, resposta) => {
      resposta.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
      resposta.end(JSON.stringify({ animal: 'pinguim-rei' }))
    })
    .listen(3000, () => console.log('db ouvindo na porta 3000'))
}, 8000)
```

`workspace/db/Dockerfile`:

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY server.js .
EXPOSE 3000
CMD ["node", "server.js"]
```

`workspace/api/server.js` — versão entregue: tenta o banco **uma vez** e morre se não
conseguir. A versão corrigida adiciona retries (a forma fica no `cenario.md`):

```js
const http = require('node:http')

async function principal() {
  const resposta = await fetch('http://db:3000')
  const dados = await resposta.json()

  http
    .createServer((_req, res) => {
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
      res.end(JSON.stringify({ animal: dados.animal }))
    })
    .listen(3000, () => console.log('api ouvindo na porta 3000'))
}

principal().catch((erro) => {
  console.error('banco não respondeu na primeira tentativa:', erro.message)
  process.exit(1)
})
```

`workspace/web/server.js` e `web/Dockerfile` iguais aos do Cenário 07 (buscam
`http://api:3000` e montam a página `O api disse: ...`).

- [x] **Step 2: O `compose.yaml` entregue (quebrado)**

```yaml
services:
  db:
    build: ./db

  api:
    build: ./api
    ports:
      - "8094:3000"
    depends_on:
      - db

  web:
    build: ./web
    ports:
      - "8095:3000"
    depends_on:
      - api
```

Com o `depends_on` simples, o `api` sobe junto com o `db`, o banco ainda está no
`setTimeout`, o `fetch` falha e o `api` sai. O Compose não espera prontidão — o que o
Cenário #3 já avisou e aqui vira o problema.

O correto (descoberto pelo leitor) acrescenta healthchecks e condições:

```yaml
services:
  db:
    build: ./db
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:3000"]
      interval: 1s
      timeout: 2s
      retries: 20
      start_period: 1s

  api:
    build: ./api
    ports:
      - "8094:3000"
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:3000"]
      interval: 2s
      timeout: 2s
      retries: 10
      start_period: 2s

  web:
    build: ./web
    ports:
      - "8095:3000"
    depends_on:
      api:
        condition: service_healthy
```

- [x] **Step 3: A Verificação**

`verificacao.yaml`:

```yaml
asercoes:
  - tipo: container_saudavel
    nome: lab-08-db-1

  - tipo: container_saudavel
    nome: lab-08-api-1

  - tipo: container_rodando
    nome: lab-08-web-1

  - tipo: http_responde
    url: http://localhost:8095
    status: 200

  - tipo: http_corpo_contem
    url: http://localhost:8095
    texto: "O api disse: pinguim-rei"
```

Sem o `healthcheck` no `db`, a `container_saudavel` do `db` falha (`<no value>`). Sem a
condição `service_healthy`, o `api` morre e o `web` não tem de onde buscar.

- [x] **Step 4: O frontmatter do `cenario.md`**

```markdown
---
id: docker/08-rodando-nao-e-pronto
titulo: Rodando ainda não é pronto
dificuldade: assistido
projetoCompose: lab-08
---
```

O texto conduz: observar o `api` sair (running ≠ ready), criar o `healthcheck`,
substituir a dependência por `condition: service_healthy`, e depois introduzir retries
na aplicação — porque uma falha pós-ready ainda derruba um serviço sem retry.

- [x] **Step 5: Percorrer o Cenário à mão**

1. **Iniciar cenário**: `docker compose -p lab-08 ps -a` mostra o `api` parado com exit
   code 1 — o primeiro sintoma.
2. **Verificar** sem fazer nada: as `container_saudavel` falham e o `web` não responde.
3. Fazer o exercício. **Verificar**: as cinco passam.
4. Confirmar que o `api` só subiu depois do `db` saudável: `docker compose -p lab-08
   logs api` não mostra a mensagem de erro.

- [x] **Step 6: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS.

- [x] **Step 7: Commit**

```bash
git add content/docker/08-rodando-nao-e-pronto && git commit -m "feat: cenário docker/08 rodando ainda não é pronto"
```

---

## Fase 5 — Cenário 09: O container com privilégios demais

**Files:**
- Create: `content/docker/09-container-privilegios-demais/workspace/compose.yaml`
- Create: `content/docker/09-container-privilegios-demais/workspace/app/Dockerfile`
- Create: `content/docker/09-container-privilegios-demais/workspace/app/server.js`
- Create: `content/docker/09-container-privilegios-demais/verificacao.yaml`
- Create: `content/docker/09-container-privilegios-demais/cenario.md`

**Interfaces:**
- Consumes: `container_rodando`, `http_responde`, `http_corpo_contem`,
  `container_configuracao`.
- Produces: frontmatter `projetoCompose: lab-09`.

O workspace entrega uma imagem que roda como `root`, com filesystem gravável e todas as
capabilities. O leitor deve adotar usuário não-root, filesystem somente leitura, `tmpfs`
para a escrita transitória e remoção de capabilities — o mínimo do que a
[segurança do Engine](https://docs.docker.com/engine/security/) recomenda.

- [x] **Step 1: O workspace**

`workspace/app/server.js` — escreve em `/tmp` para provar que a escrita transitória
continua funcionando com `read_only: true` + `tmpfs`:

```js
const http = require('node:http')
const fs = require('node:fs')

http
  .createServer((_req, resposta) => {
    fs.writeFileSync('/tmp/ping.txt', new Date().toISOString())
    resposta.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8' })
    resposta.end('Cenário 09 seguro')
  })
  .listen(3000, () => console.log('app ouvindo na porta 3000'))
```

`workspace/app/Dockerfile`:

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY server.js .
EXPOSE 3000
CMD ["node", "server.js"]
```

`workspace/compose.yaml` (entregue, inseguro):

```yaml
services:
  app:
    build: ./app
    ports:
      - "8096:3000"
```

O correto (descoberto pelo leitor):

```yaml
services:
  app:
    build: ./app
    ports:
      - "8096:3000"
    user: node
    read_only: true
    tmpfs:
      - /tmp
    cap_drop:
      - ALL
```

- [x] **Step 2: A Verificação**

`verificacao.yaml`:

```yaml
asercoes:
  - tipo: container_rodando
    nome: lab-09-app-1

  - tipo: http_responde
    url: http://localhost:8096
    status: 200

  - tipo: http_corpo_contem
    url: http://localhost:8096
    texto: Cenário 09 seguro

  - tipo: container_configuracao
    nome: lab-09-app-1
    usuario: node
    somenteLeitura: true
    capabilitiesRemovidas: [ALL]
```

A `container_configuracao` é o núcleo: confirma usuário, filesystem e capabilities de
uma vez. Se o `read_only` quebrasse a escrita em `/tmp`, a HTTP não responderia o texto
— o `tmpfs` é parte da solução, não enfeite.

- [x] **Step 3: O frontmatter do `cenario.md`**

```markdown
---
id: docker/09-container-privilegios-demais
titulo: O container com privilégios demais
dificuldade: assistido
projetoCompose: lab-09
---
```

O texto percorre as quatro camadas: usuário não-root (e por que root dentro do
container não é isolamento), filesystem somente leitura, `tmpfs` para escrita
transitória e `cap_drop`. Termina com o alerta sobre `--privileged` e o socket do
Docker, que este Cenário não toca de propósito.

- [x] **Step 4: Percorrer o Cenário à mão**

1. **Iniciar cenário**; confirmar que a app sobe como `root`
   (`docker compose -p lab-09 exec app whoami`).
2. **Verificar** sem fazer nada: as HTTP passam, mas a `container_configuracao` falha.
3. Fazer o exercício. **Verificar**: as quatro passam.
4. Confirmar que a escrita em `/tmp` continua: `docker compose -p lab-09 exec app ls /tmp/ping.txt`.

- [x] **Step 5: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add content/docker/09-container-privilegios-demais && git commit -m "feat: cenário docker/09 o container com privilégios demais"
```

---

## Fase 6 — Cenário 10: Da tag ao digest

**Files:**
- Create: `content/docker/10-da-tag-ao-digest/workspace/app/Dockerfile`
- Create: `content/docker/10-da-tag-ao-digest/workspace/app/server.js`
- Create: `content/docker/10-da-tag-ao-digest/verificacao.yaml`
- Create: `content/docker/10-da-tag-ao-digest/cenario.md`

**Interfaces:**
- Consumes: `container_rodando`, `http_responde`, `http_corpo_contem`,
  `imagem_no_registry`.
- Produces: frontmatter `containers: [lab-10-app, lab-10-registry]`.

O Cenário ensina distribuição sem conta em registry externo: um registry local
descartável, tags semântica e de commit, push, pull e rollback por digest. O teardown
remove tanto a app quanto o registry (`lab-10-registry`).

- [x] **Step 1: O workspace**

`workspace/app/server.js` — serve a versão da imagem, para que a Verificação distinga
1.0 de 1.1 pelo corpo:

```js
const http = require('node:http')

const versao = process.env.APP_VERSAO || 'indefinida'

http
  .createServer((_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
    resposta.end(JSON.stringify({ versao }))
  })
  .listen(3000, () => console.log('app v' + versao + ' ouvindo na porta 3000'))
```

`workspace/app/Dockerfile`:

```dockerfile
FROM node:22-alpine
WORKDIR /app
ENV APP_VERSAO=1.0
COPY server.js .
EXPOSE 3000
CMD ["node", "server.js"]
```

- [x] **Step 2: O exercício (resumo do que o `cenario.md` deve ensinar)**

1. Subir o registry descartável:
   `docker run -d --name lab-10-registry -p 5000:5000 registry:2`.
2. Construir a app e marcar a versão boa:
   `docker build -t lab-10-app:1.0 app/`.
3. Renomear para o registry local e publicar:
   `docker tag lab-10-app:1.0 localhost:5000/lab-10-app:1.0`
   e `docker push localhost:5000/lab-10-app:1.0`. Anotar o digest:
   `docker inspect --format='{{index .RepoDigests 0}}' lab-10-app:1.0`.
4. Introduzir o bug (mudar `APP_VERSAO=1.1` no Dockerfile), reconstruir, publicar por
   cima do `:1.0`, e confirmar que `localhost:5000/lab-10-app:1.0` agora é a 1.1.
5. Remover as imagens locais e puxar de volta do registry. Rodar a 1.0 **pelo digest**
   anotado no passo 3, provando rollback sem mudar a tag:
   `docker run -d --name lab-10-app -p 8097:3000 localhost:5000/lab-10-app@sha256:...`.
6. Abrir `http://localhost:8097` e ver `"versao": "1.0"`.

A Verificação exige o registry no ar com a imagem e o container `lab-10-app`.

- [x] **Step 3: A Verificação**

`verificacao.yaml`:

```yaml
asercoes:
  - tipo: container_rodando
    nome: lab-10-app

  - tipo: http_responde
    url: http://localhost:8097
    status: 200

  - tipo: http_corpo_contem
    url: http://localhost:8097
    texto: '"versao":"1.0"'
```

> **Correção de plano (validada na execução):** o texto original tinha
> `'"versao": "1.0"'` (com espaço após os dois-pontos), mas o `JSON.stringify` do Node
> emite `{"versao":"1.0"}` **sem** espaço e o motor compara com `contains` literal — a
> versão espaçada nunca casaria. A `http_corpo_contem` usa o texto sem espaço.

  - tipo: imagem_no_registry
    referencia: localhost:5000/lab-10-app:1.0
    descricao: a imagem v1.0 está publicada no registry
```

A `imagem_no_registry` prova a publicação; o corpo da HTTP prova que o que está rodando
é a 1.0 — o rollback por digest.

- [x] **Step 4: O frontmatter do `cenario.md`**

```markdown
---
id: docker/10-da-tag-ao-digest
titulo: Da tag ao digest
dificuldade: assistido
containers: [lab-10-app, lab-10-registry]
---
```

O texto fecha o ciclo tag mutável versus digest (que os Fundamentos já introduziram) com
a prática: por que `latest` é traiçoeiro e por que deploys sérios referenciam digest.

- [x] **Step 5: Percorrer o Cenário à mão**

1. **Iniciar cenário**; confirmar que o workspace materializou `app/`.
2. **Verificar** sem fazer nada: falha tudo (sem registry, sem imagem, sem container).
3. Fazer o exercício. **Verificar**: as quatro passam.
4. Confirmar o rollback: abrir a página e ver `"versao": "1.0"` enquanto o registry
   ainda guarda a `:1.0` = 1.1.
5. Confirmar que o teardown remove o `lab-10-registry` e o `lab-10-app` ao abrir outro
   Cenário.

- [x] **Step 6: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS.

- [x] **Step 7: Commit**

```bash
git add content/docker/10-da-tag-ao-digest && git commit -m "feat: cenário docker/10 da tag ao digest"
```

---

## Fase 7 — Cenário 11: Incidente final de Docker

**Files:**
- Create: `content/docker/11-incidente-final/workspace/compose.yaml`
- Create: `content/docker/11-incidente-final/workspace/web/server.js`
- Create: `content/docker/11-incidente-final/workspace/web/Dockerfile`
- Create: `content/docker/11-incidente-final/workspace/api/server.js`
- Create: `content/docker/11-incidente-final/workspace/api/Dockerfile`
- Create: `content/docker/11-incidente-final/workspace/db/server.js`
- Create: `content/docker/11-incidente-final/workspace/db/Dockerfile`
- Create: `content/docker/11-incidente-final/verificacao.yaml`
- Create: `content/docker/11-incidente-final/cenario.md`

**Interfaces:**
- Consumes: `container_rodando`, `container_saudavel`, `http_responde`,
  `http_corpo_contem`, `container_em_rede`, `volume_existe`, `comando_produz`.
- Produces: frontmatter `projetoCompose: lab-11` e `volumes: [lab-11-dados]`. Degrau
  `mestre`.

A stack combina cinco falhas, cada uma de um Cenário anterior, sem causa revelada. O
leitor recebe sintomas e critérios de sucesso:

1. **cache que preservou artefato errado** — o `api/Dockerfile` copia o `src/` inteiro
   com `COPY . .` e o `compose.yaml` é subido sem `--build` depois da correção; o
   `api` continua servindo a versão antiga;
2. **serviço unhealthy** — o `db` não tem `healthcheck`, e o `api` depende dele com
   `condition: service_healthy`, então o `api` nunca fica pronto;
3. **DNS incorreto** — o `web` busca `http://api-errado:3000` em vez de `http://api:3000`;
4. **porta exposta indevidamente** — o `db` publica `8098:3000` no host, mas deveria
   ficar só na rede interna;
5. **volume com dado necessário** — o `db` precisa do volume `lab-11-dados` para
   guardar o valor que o `web` mostra; sem o volume, o valor some.

- [x] **Step 1: O workspace — aplicações**

`workspace/db/server.js` — escreve o animal num volume (`/dados`) e o serve; sem o
volume, o arquivo morre com o container:

```js
const http = require('node:http')
const fs = require('node:fs')

const ARQUIVO = '/dados/animal.txt'
const ANIMAL = process.env.ANIMAL || 'tatu-bola'

try {
  fs.writeFileSync(ARQUIVO, ANIMAL)
} catch (erro) {
  console.error('não consegui gravar em /dados:', erro.message)
}

http
  .createServer((_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
    resposta.end(JSON.stringify({ animal: ANIMAL }))
  })
  .listen(3000, () => console.log('db ouvindo na porta 3000'))
```

`workspace/api/server.js` — **incorreto**: depende do `db` com `service_healthy` que
nunca fica healthy; o texto do exercício não revela isso. Busca `http://db:3000`:

```js
const http = require('node:http')

async function buscar() {
  const resposta = await fetch('http://db:3000')
  return resposta.json()
}

http
  .createServer(async (_req, resposta) => {
    try {
      const dados = await buscar()
      resposta.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
      resposta.end(JSON.stringify({ animal: dados.animal }))
    } catch (erro) {
      resposta.writeHead(502, { 'Content-Type': 'text/plain; charset=utf-8' })
      resposta.end('api sem banco: ' + erro.message)
    }
  })
  .listen(3000, () => console.log('api ouvindo na porta 3000'))
```

`workspace/web/server.js` — **incorreto**: aponta para `http://api-errado:3000`:

```js
const http = require('node:http')

const alvo = 'http://api-errado:3000'

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

Os três `Dockerfile` são idênticos (`COPY . .` + `CMD ["node", "server.js"]`). O do
`api` deliberadamente usa `COPY . .` em vez de `COPY server.js .`: o leitor corrige o
`server.js` no workspace, mas a correção só chega à imagem com `docker compose -p
lab-11 up -d --build` — sem `--build`, o Compose reutiliza a imagem da vez anterior e o
sintoma persiste: é o "cache que preservou artefato errado" (mesma lição do Cenário 04).

`workspace/api/Dockerfile`:

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY . .
EXPOSE 3000
CMD ["node", "server.js"]
```

- [x] **Step 2: O `compose.yaml` entregue (quebrado)**

```yaml
services:
  web:
    build: ./web
    ports:
      - "8098:3000"
    depends_on:
      - api

  api:
    build: ./api
    depends_on:
      - db

  db:
    build: ./db
    ports:
      - "9098:3000"
    environment:
      ANIMAL: tatu-bola
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080"]
      interval: 2s
      timeout: 2s
      retries: 3
```

Falhas: `web` busca o nome errado (`http://api-errado:3000`); o `healthcheck` do `db`
aponta para a porta **8080** enquanto o servidor escuta na 3000, então o `db` fica
`unhealthy` para sempre; `db` publica porta de host indevidamente (a 9098); nenhum
volume montado em `/dados`. O `api` usa `depends_on` simples de propósito — se usasse
`condition: service_healthy` sobre um `db` sem healthcheck, o `up` falharia com
*no healthcheck configured* e o lifecycle nem iniciaria o Cenário. O correto (descoberto
pelo leitor):

```yaml
services:
  web:
    build: ./web
    ports:
      - "8098:3000"
    networks:
      - lab-11-borda
    depends_on:
      api:
        condition: service_healthy

  api:
    build: ./api
    networks:
      - lab-11-borda
      - lab-11-interna
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:3000"]
      interval: 2s
      timeout: 2s
      retries: 10
      start_period: 2s

  db:
    build: ./db
    networks:
      - lab-11-interna
    volumes:
      - lab-11-dados:/dados
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:3000"]
      interval: 2s
      timeout: 2s
      retries: 10
      start_period: 2s

volumes:
  lab-11-dados:
    name: lab-11-dados

networks:
  lab-11-borda:
    name: lab-11-borda
  lab-11-interna:
    name: lab-11-interna
```

O `name: lab-11-dados` no volume **não é opcional**: sem ele o Compose cria o volume com
o prefixo do projeto (`lab-11_lab-11-dados`), e as Asserções `volume_existe` e
`comando_produz` (que montam `-v lab-11-dados:/dados`) falhariam mesmo na solução
correta. Verificado na engine 29.6.1 — mesmo comportamento do prefixo das redes.

- [x] **Step 3: A Verificação**

`verificacao.yaml`:

```yaml
asercoes:
  - tipo: container_rodando
    nome: lab-11-web-1
  - tipo: container_rodando
    nome: lab-11-api-1
  - tipo: container_saudavel
    nome: lab-11-db-1

  - tipo: http_responde
    url: http://localhost:8098
    status: 200

  - tipo: http_corpo_contem
    url: http://localhost:8098
    texto: "O api disse: tatu-bola"

  - tipo: container_em_rede
    nome: lab-11-db-1
    rede: lab-11-borda
    presente: false

  - tipo: volume_existe
    nome: lab-11-dados

  - tipo: comando_produz
    descricao: o dado do banco sobreviveu no volume
    comando: ["docker", "run", "--rm", "-v", "lab-11-dados:/dados", "alpine", "cat", "/dados/animal.txt"]
    contem: tatu-bola
```

Todas as Asserções usam tipos já existentes ou criados nas Fases anteriores — o degrau
`mestre` combina vocabulário conhecido, não inventa. O `comando_produz` final é a prova
de que o dado está no volume, não só na camada gravável do container.

- [x] **Step 4: O frontmatter do `cenario.md`**

```markdown
---
id: docker/11-incidente-final
titulo: Incidente final de Docker
dificuldade: mestre
projetoCompose: lab-11
volumes: [lab-11-dados]
---
```

Seguindo o padrão do Cenário #4: sem passo a passo, sintomas e critérios de sucesso,
ferramentas listadas, método de diagnóstico sugerido — não o defeito.

- [x] **Step 5: Percorrer o Cenário à mão**

1. **Iniciar cenário**; a stack quebrada sobe sozinha — o `web` mostra "nao alcancei
   http://api-errado:3000" e o `api` nunca fica healthy (o `db` não tem healthcheck).
2. **Verificar** sem fazer nada: tudo falha, e a ordem das Asserções sugere o roteiro.
3. Diagnosticar e corrigir as cinco falhas. **Verificar**: as oito passam.
4. Confirmar que o `docker compose -p lab-11 down -v` (do teardown) remove o volume
   `lab-11-dados`.
5. O teste que importa: abrir outro Cenário, voltar ao #11 e verificar sem fazer nada —
   nada deve passar por sobra de ambiente.

- [x] **Step 6: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS.

- [x] **Step 7: Commit**

```bash
git add content/docker/11-incidente-final && git commit -m "feat: cenário docker/11 incidente final"
```

---

## Fase 8 — Fechamento

### Task 4: Atualizar o catálogo real e a documentação

**Files:**
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`
- Modify: `README.md`
- Modify: este plano, marcando as Fases concluídas.

O `CatalogoRealTest` conta 37 Cenários. Com os seis novos, passa a 43; Kubernetes segue
com 14 e AWS com 18. As somas de Asserções de Kubernetes e AWS não mudam. Docker passa
de 5 para 11 Cenários e de 17 para 56 Asserções (somas atuais: 01=3, 02=4, 03=4, 04=4,
05=2 → 17; novas: 06=6, 07=11, 08=5, 09=4, 10=4, 11=9 → 39; total 56).

> A contagem final (56) reflete o fix de portas aplicado no review final: os Cenários
> 07 e 11 ganharam uma Asserção `comando_produz` (`.HostConfig.PortBindings` = `{}`)
> para verificar que o `db` não publica porta no host.

- [x] **Step 1: Atualizar o teste**

Em `CatalogoRealTest`, troque `hasSize(37)` por `hasSize(43)` e acrescente:

```java
        var docker = cenarios.stream()
                .filter(cenario -> cenario.id().startsWith("docker/"))
                .toList();
        assertThat(docker).hasSize(11);
        assertThat(docker.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(56);
```

- [x] **Step 2: Rodar a suíte inteira**

Run: `cd backend && ./mvnw -B test`
Expected: PASS, com o Docker Desktop ativo.

- [x] **Step 3: Atualizar o README**

O README lista os Cenários da Trilha Docker. Acrescentar os seis novos com suas
dificuldades e atualizar qualquer contagem. Não inventar roadmap novo — o avançado de
CI/CD e cadeia de fornecimento fica para uma segunda rodada, como a pesquisa decidiu.

- [x] **Step 4: Percorrer os seis Cenários à mão**

A pergunta que valida cada Cenário: **abrir outro Cenário e voltar — a Verificação
deve reprovar sem refazer o exercício** (ADR 0002). O Cenário 11 em especial.

- [x] **Step 5: Commit**

```bash
git add backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java README.md
git commit -m "feat: catálogo e documentação com os cenários 06 a 11"
```

---

## Critérios de conclusão

- As quatro Asserções novas (`container_saudavel`, `container_em_rede`,
  `container_configuracao`, `imagem_no_registry`) têm testes verdes e são lidas pelo
  leitor de YAML.
- Os seis Cenários estão no catálogo (43 no total, 11 de Docker), cada um com
  workspace, `verificacao.yaml` e `cenario.md`.
- Nenhum Cenário novo depende de conta externa, credenciais ou `npm install`.
- O teardown remove containers, Compose projects, redes e o volume `lab-11-dados`
  declarados.
- Os degraus `autonomo` e `mestre` estão exercitados (07 e 11) além de `assistido`.
- Backend, lint do frontend e percurso manual dos seis Cenários estão verdes.
- A roadmap de Docker no README referencia a segunda rodada (CI/CD e cadeia de
  fornecimento) sem bloqueá-la.
