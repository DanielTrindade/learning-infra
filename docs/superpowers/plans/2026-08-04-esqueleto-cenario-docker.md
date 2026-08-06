# Esqueleto de Ponta a Ponta — Cenário Docker #1

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Um único Cenário Docker atravessando o sistema inteiro — Markdown no disco, página React, iniciar, verificar por Asserções nomeadas, marcar concluído.

**Architecture:** Backend Spring Boot lê Cenários de arquivos no disco, gerencia o Cenário Ativo (materializa um workspace e derruba containers do Cenário anterior), e avalia Asserções declarativas executando o CLI do Docker por `ProcessBuilder` e fazendo requisições HTTP. Frontend React renderiza o Markdown e mostra o resultado de cada Asserção individualmente. Não há terminal na plataforma — o leitor executa os comandos no shell nativo dele.

**Tech Stack:** Java 25, Spring Boot 4.0.x, Maven (via wrapper `mvnw`), SnakeYAML e Jackson (já vêm com Spring Boot), JUnit 5, Vite, React 19, TypeScript, `react-markdown`.

## Global Constraints

- **Java 25.** O JDK instalado é o 25.0.3 LTS. Spring Boot 4.0.x tem suporte de primeira classe a Java 25.
- **Maven só pelo wrapper.** Não há `mvn` instalado na máquina. Sempre `./mvnw` (ou `mvnw.cmd`).
- **Nomes de domínio em português, exatamente como em `CONTEXT.md`:** `Trilha`, `Cenário`, `Dificuldade`, `Cenário Ativo`, `Verificação`, `Asserção`. Classes, campos, endpoints e YAML usam esses nomes. Não traduza para inglês.
- **Sem terminal embutido** (ADR 0001). Nenhuma Asserção pode depender de observar comandos digitados — só estado do mundo.
- **No máximo um Cenário Ativo** (ADR 0002). Iniciar um Cenário derruba os containers declarados pelo Cenário anterior. Falha de teardown deve ser ruidosa, nunca silenciosa.
- **Sem Docker Compose neste esqueleto.** O Cenário #1 não tem ambiente para subir — o leitor cria o container com as próprias mãos, que é justamente o exercício. Setup significa materializar arquivos de workspace. Suporte a Compose entra quando um Cenário multi-serviço existir.
- **Sem banco, sem autenticação, sem catálogo.** Progresso é um arquivo JSON. Um usuário, uma máquina, `127.0.0.1`.
- **O daemon do Docker precisa estar no ar** para as tarefas 3, 4 e 8. `docker context ls` deve responder sem pendurar.
- **Plataforma Windows.** O endpoint do Docker é o named pipe `npipe:////./pipe/dockerDesktopLinuxEngine`. Nunca assuma `/var/run/docker.sock` no host. Toda conversa com o Docker é via CLI, nunca via API HTTP do daemon.

---

## Estrutura de arquivos

```
learning-infra/
├── CONTEXT.md                          (existe)
├── docs/adr/                           (existe)
├── .gitignore                          Task 1
├── content/                            conteúdo versionado
│   └── docker/01-servir-html-nginx/
│       ├── cenario.md                  Task 8 — frontmatter + texto didático
│       ├── verificacao.yaml            Task 8 — as Asserções
│       └── workspace/                  Task 8 — arquivos entregues ao leitor
├── data/progresso.json                 gerado, gitignored
├── work/                               workspace materializado, gitignored
├── backend/
│   ├── pom.xml                         Task 1
│   ├── mvnw, mvnw.cmd, .mvn/           Task 1
│   └── src/
│       ├── main/java/dev/learninginfra/
│       │   ├── LearningInfraApplication.java        Task 1
│       │   ├── execucao/
│       │   │   ├── SaidaDeComando.java              Task 3
│       │   │   ├── ExecutorDeComando.java           Task 3
│       │   │   └── ExecutorDeComandoReal.java       Task 3
│       │   ├── conteudo/
│       │   │   ├── Dificuldade.java                 Task 2
│       │   │   ├── Cenario.java                     Task 2
│       │   │   ├── LeitorDeCenario.java             Task 2
│       │   │   └── RepositorioDeCenarios.java       Task 2
│       │   ├── verificacao/
│       │   │   ├── Assercao.java                    Task 2 (sealed)
│       │   │   ├── ResultadoDeAsercao.java          Task 3
│       │   │   ├── ResultadoDaVerificacao.java      Task 3
│       │   │   └── MotorDeVerificacao.java          Task 3
│       │   ├── ciclodevida/
│       │   │   └── GerenciadorDeCenarioAtivo.java   Task 4
│       │   ├── progresso/
│       │   │   ├── Progresso.java                   Task 4
│       │   │   └── RepositorioDeProgresso.java      Task 4
│       │   └── api/
│       │       ├── CenarioController.java           Task 5
│       │       └── dto/CenarioDetalhado.java        Task 5
│       ├── main/resources/application.yaml          Task 1
│       └── test/java/dev/learninginfra/...          cada task
└── frontend/                                        Tasks 6, 7
    ├── package.json, vite.config.ts, tsconfig.json
    └── src/{main.tsx,App.tsx,api.ts,PaginaDoCenario.tsx,ChecklistDeVerificacao.tsx}
```

Responsabilidade por pacote: `execucao` só sabe rodar processos e não conhece domínio. `conteudo` só sabe ler arquivos do disco. `verificacao` só sabe avaliar Asserções contra o mundo. `ciclodevida` é o único que muda o estado da máquina. `api` só traduz HTTP. Nenhum pacote de baixo importa de `api`.

---

### Task 1: Esqueleto do backend e do repositório

> **CONCLUÍDA e validada em 2026-08-04** — commit `11fa3cc`. Spring Boot 4.0.0 sobre
> Java 25.0.3, `./mvnw test` com `BUILD SUCCESS`. Os passos abaixo já incorporam as
> correções que a execução real revelou. Não reexecute esta tarefa.

**Files:**
- Create: `.gitignore`
- Create: `backend/pom.xml`, `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties`
- Create: `backend/src/main/java/dev/learninginfra/LearningInfraApplication.java`
- Create: `backend/src/main/resources/application.yaml`
- Test: `backend/src/test/java/dev/learninginfra/LearningInfraApplicationTests.java`

**Interfaces:**
- Consumes: nada.
- Produces: a aplicação Spring Boot sobe em `127.0.0.1:8099`. Propriedade de configuração `learninginfra.diretorio-de-conteudo` (default `../content`), `learninginfra.diretorio-de-trabalho` (default `../work`), `learninginfra.arquivo-de-progresso` (default `../data/progresso.json`).

- [ ] **Step 1: Inicializar o repositório git**

O diretório ainda não é um repositório git.

```bash
cd learning-infra
git init
```

- [ ] **Step 2: Escrever o `.gitignore`**

Crie `.gitignore` na raiz:

```gitignore
# artefatos gerados em tempo de execução
/work/
/data/

# backend
backend/target/
backend/.mvn/wrapper/maven-wrapper.jar

# frontend
frontend/node_modules/
frontend/dist/

# IDE
.idea/
*.iml
.vscode/
```

- [ ] **Step 3: Gerar o projeto Spring Boot**

Gere pelo Spring Initializr, com o wrapper Maven incluído (não há `mvn` na máquina):

```bash
curl -sS -G https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=4.0.0 \
  -d javaVersion=25 \
  -d groupId=dev.learninginfra \
  -d artifactId=backend \
  -d name=learning-infra \
  -d packageName=dev.learninginfra \
  -d dependencies=web \
  -o backend.zip
unzip -q backend.zip -d backend && rm backend.zip
```

Dois detalhes verificados na execução real, ambos fáceis de errar:

- **O zip não tem diretório-base.** Os arquivos vêm na raiz do arquivo, então é
  `unzip -d backend`, e não descompactar num temporário para depois mover.
- **`name` define o nome da classe principal**, não o `artifactId`. Com
  `name=learning-infra` o Initializr gera `LearningInfraApplication.java` e
  `LearningInfraApplicationTests.java`. Com `name=backend` sairia `BackendApplication`,
  divergindo do resto deste plano.

Se `bootVersion=4.0.0` for recusado, omita o parâmetro `bootVersion` e deixe o Initializr escolher a versão estável mais recente — qualquer 4.0.x ou superior serve. Confirme depois que `pom.xml` tem `<java.version>25</java.version>`.

Note que o Spring Boot 4 renomeou os starters: pedir `dependencies=web` produz
`spring-boot-starter-webmvc` e `spring-boot-starter-webmvc-test` no `pom.xml` — e
**não** os antigos `spring-boot-starter-web` / `spring-boot-starter-test`. Isso é o
esperado; não "corrija" para os nomes antigos.

- [ ] **Step 4: Escrever a configuração**

Substitua `backend/src/main/resources/application.properties` por `application.yaml` (apague o `.properties`):

```yaml
server:
  address: 127.0.0.1
  # 8080 é deliberadamente evitada: é a porta que os Cenários de Docker usam.
  port: 8099

learninginfra:
  diretorio-de-conteudo: ../content
  diretorio-de-trabalho: ../work
  arquivo-de-progresso: ../data/progresso.json
```

`server.address: 127.0.0.1` não é cosmético — o backend executa comandos e mexe em containers da máquina. Ele nunca deve escutar em `0.0.0.0`.

A porta **8099** também não é arbitrária. A 8080 é a porta canônica dos tutoriais de
Docker e é a que o Cenário #1 manda publicar (`docker run -p 8080:80`). Se o backend
ocupasse a 8080, o `docker run` do leitor falharia com *port is already allocated* —
ou pior, a Asserção `http_responde` bateria no próprio backend em vez do nginx e
passaria pelo motivo errado. Mantenha o backend fora de qualquer porta que os
Cenários usem.

- [ ] **Step 5: Ajustar o teste de contexto**

O Initializr **já gerou** `backend/src/test/java/dev/learninginfra/LearningInfraApplicationTests.java`
com um método `contextLoads()`. Não crie o arquivo — substitua o conteúdo dele, para
seguir a convenção de nomes em português deste projeto:

```java
package dev.learninginfra;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LearningInfraApplicationTests {

    @Test
    void oContextoSobe() {
    }
}
```

- [ ] **Step 6: Rodar o teste**

Run: `cd backend && ./mvnw test`
Expected: PASS. O wrapper vai baixar o Maven na primeira execução — isso é esperado e demora.

- [ ] **Step 7: Commit**

```bash
git add .gitignore backend/
git commit -m "chore: esqueleto do backend Spring Boot"
```

---

### Task 2: Ler um Cenário do disco

**Files:**
- Create: `backend/src/main/java/dev/learninginfra/conteudo/Dificuldade.java`
- Create: `backend/src/main/java/dev/learninginfra/conteudo/Cenario.java`
- Create: `backend/src/main/java/dev/learninginfra/verificacao/Assercao.java`
- Create: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Create: `backend/src/main/java/dev/learninginfra/conteudo/RepositorioDeCenarios.java`
- Test: `backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java`

**Interfaces:**
- Consumes: nada de tasks anteriores.
- Produces:
  - `enum Dificuldade { GUIADO, ASSISTIDO, AUTONOMO, MESTRE }` com `static Dificuldade deTexto(String)`.
  - `record Cenario(String id, String titulo, Dificuldade dificuldade, List<String> containers, String markdown, Path diretorio, List<Assercao> asercoes)`.
  - `sealed interface Assercao permits ContainerRodando, HttpResponde, HttpCorpoContem` com `String descricao()`.
  - `record Assercao.ContainerRodando(String nome)`, `record Assercao.HttpResponde(String url, int status)`, `record Assercao.HttpCorpoContem(String url, String texto)`.
  - `LeitorDeCenario.ler(Path diretorioDoCenario) -> Cenario`.
  - `RepositorioDeCenarios.buscar(String id) -> Optional<Cenario>` e `listar() -> List<Cenario>`.

- [ ] **Step 1: Escrever o teste que falha**

Crie `backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java`:

```java
package dev.learninginfra.conteudo;

import dev.learninginfra.verificacao.Assercao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeitorDeCenarioTest {

    @TempDir
    Path diretorio;

    private void escreverCenarioCompleto() throws Exception {
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: docker/01-servir-html-nginx
                titulo: Servir um HTML seu com nginx
                dificuldade: guiado
                containers: [lab-web]
                ---
                # Servir um HTML seu com nginx

                Texto didático aqui.
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_rodando
                    nome: lab-web
                  - tipo: http_responde
                    url: http://localhost:8080
                    status: 200
                  - tipo: http_corpo_contem
                    url: http://localhost:8080
                    texto: Meu primeiro container
                """);
    }

    @Test
    void leMetadadosDoFrontmatter() throws Exception {
        escreverCenarioCompleto();

        Cenario cenario = new LeitorDeCenario().ler(diretorio);

        assertEquals("docker/01-servir-html-nginx", cenario.id());
        assertEquals("Servir um HTML seu com nginx", cenario.titulo());
        assertEquals(Dificuldade.GUIADO, cenario.dificuldade());
        assertEquals(List.of("lab-web"), cenario.containers());
    }

    @Test
    void separaOCorpoDoFrontmatter() throws Exception {
        escreverCenarioCompleto();

        Cenario cenario = new LeitorDeCenario().ler(diretorio);

        assertTrue(cenario.markdown().startsWith("# Servir um HTML seu com nginx"));
        assertFalse(cenario.markdown().contains("dificuldade:"));
    }

    @Test
    void leAsTresAsercoes() throws Exception {
        escreverCenarioCompleto();

        List<Assercao> asercoes = new LeitorDeCenario().ler(diretorio).asercoes();

        assertEquals(3, asercoes.size());
        assertEquals(new Assercao.ContainerRodando("lab-web"), asercoes.get(0));
        assertEquals(new Assercao.HttpResponde("http://localhost:8080", 200), asercoes.get(1));
        assertEquals(new Assercao.HttpCorpoContem("http://localhost:8080", "Meu primeiro container"),
                asercoes.get(2));
    }

    @Test
    void recusaTipoDeAsercaoDesconhecido() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_falando_grego
                    nome: lab-web
                """);

        var erro = assertThrows(IllegalArgumentException.class, () -> new LeitorDeCenario().ler(diretorio));
        assertTrue(erro.getMessage().contains("container_falando_grego"));
    }
}
```

- [ ] **Step 2: Rodar o teste para ver falhar**

Run: `cd backend && ./mvnw test -Dtest=LeitorDeCenarioTest`
Expected: FAIL na compilação — `Cenario`, `Dificuldade`, `Assercao` e `LeitorDeCenario` não existem.

- [ ] **Step 3: Escrever `Dificuldade`**

```java
package dev.learninginfra.conteudo;

import java.util.Locale;

public enum Dificuldade {
    GUIADO, ASSISTIDO, AUTONOMO, MESTRE;

    public static Dificuldade deTexto(String texto) {
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("dificuldade desconhecida: " + texto);
        }
    }
}
```

- [ ] **Step 4: Escrever `Assercao`**

```java
package dev.learninginfra.verificacao;

public sealed interface Assercao {

    String descricao();

    record ContainerRodando(String nome) implements Assercao {
        @Override
        public String descricao() {
            return "o container `" + nome + "` está rodando";
        }
    }

    record HttpResponde(String url, int status) implements Assercao {
        @Override
        public String descricao() {
            return "GET " + url + " responde " + status;
        }
    }

    record HttpCorpoContem(String url, String texto) implements Assercao {
        @Override
        public String descricao() {
            return "o corpo de " + url + " contém \"" + texto + "\"";
        }
    }
}
```

Records aninhados dentro de uma `sealed interface` são implicitamente `permits` — não precisa da cláusula explícita.

- [ ] **Step 5: Escrever `Cenario`**

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
        List<Assercao> asercoes) {

    public Path workspace() {
        return diretorio.resolve("workspace");
    }
}
```

- [ ] **Step 6: Escrever `LeitorDeCenario`**

```java
package dev.learninginfra.conteudo;

import dev.learninginfra.verificacao.Assercao;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class LeitorDeCenario {

    private static final String DELIMITADOR = "---";

    public Cenario ler(Path diretorioDoCenario) {
        String bruto = lerArquivo(diretorioDoCenario.resolve("cenario.md"));
        String[] partes = separarFrontmatter(bruto, diretorioDoCenario);

        Map<String, Object> meta = new Yaml().load(partes[0]);
        String corpo = partes[1];

        List<Assercao> asercoes = lerAsercoes(diretorioDoCenario.resolve("verificacao.yaml"));

        return new Cenario(
                exigirTexto(meta, "id"),
                exigirTexto(meta, "titulo"),
                Dificuldade.deTexto(exigirTexto(meta, "dificuldade")),
                lerContainers(meta),
                corpo,
                diretorioDoCenario,
                asercoes);
    }

    private String[] separarFrontmatter(String bruto, Path diretorio) {
        String normalizado = bruto.replace("\r\n", "\n").stripLeading();
        if (!normalizado.startsWith(DELIMITADOR + "\n")) {
            throw new IllegalArgumentException("cenario.md sem frontmatter em " + diretorio);
        }
        int fim = normalizado.indexOf("\n" + DELIMITADOR, DELIMITADOR.length());
        if (fim < 0) {
            throw new IllegalArgumentException("frontmatter não fechado em " + diretorio);
        }
        String frontmatter = normalizado.substring(DELIMITADOR.length() + 1, fim);
        String corpo = normalizado.substring(fim + 1 + DELIMITADOR.length()).stripLeading();
        return new String[]{frontmatter, corpo};
    }

    @SuppressWarnings("unchecked")
    private List<Assercao> lerAsercoes(Path arquivo) {
        Map<String, Object> raiz = new Yaml().load(lerArquivo(arquivo));
        List<Map<String, Object>> itens = (List<Map<String, Object>>) raiz.get("asercoes");
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("verificacao.yaml sem asserções: " + arquivo);
        }
        List<Assercao> asercoes = new ArrayList<>();
        for (Map<String, Object> item : itens) {
            asercoes.add(montarAsercao(item, arquivo));
        }
        return List.copyOf(asercoes);
    }

    private Assercao montarAsercao(Map<String, Object> item, Path arquivo) {
        String tipo = exigirTexto(item, "tipo");
        return switch (tipo) {
            case "container_rodando" -> new Assercao.ContainerRodando(exigirTexto(item, "nome"));
            case "http_responde" -> new Assercao.HttpResponde(
                    exigirTexto(item, "url"), (Integer) item.getOrDefault("status", 200));
            case "http_corpo_contem" -> new Assercao.HttpCorpoContem(
                    exigirTexto(item, "url"), exigirTexto(item, "texto"));
            default -> throw new IllegalArgumentException(
                    "tipo de asserção desconhecido: " + tipo + " em " + arquivo);
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> lerContainers(Map<String, Object> meta) {
        Object valor = meta.get("containers");
        return valor == null ? List.of() : List.copyOf((List<String>) valor);
    }

    private String exigirTexto(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        if (valor == null) {
            throw new IllegalArgumentException("campo obrigatório ausente: " + chave);
        }
        return valor.toString();
    }

    private String lerArquivo(Path caminho) {
        try {
            return Files.readString(caminho);
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui ler " + caminho, e);
        }
    }
}
```

- [ ] **Step 7: Rodar os testes do leitor**

Run: `cd backend && ./mvnw test -Dtest=LeitorDeCenarioTest`
Expected: PASS, 4 testes.

- [ ] **Step 8: Escrever `RepositorioDeCenarios`**

```java
package dev.learninginfra.conteudo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class RepositorioDeCenarios {

    private final Path diretorioDeConteudo;
    private final LeitorDeCenario leitor;

    public RepositorioDeCenarios(
            @Value("${learninginfra.diretorio-de-conteudo}") String diretorioDeConteudo,
            LeitorDeCenario leitor) {
        this.diretorioDeConteudo = Path.of(diretorioDeConteudo);
        this.leitor = leitor;
    }

    public List<Cenario> listar() {
        if (!Files.isDirectory(diretorioDeConteudo)) {
            return List.of();
        }
        try (Stream<Path> trilhas = Files.list(diretorioDeConteudo)) {
            return trilhas
                    .filter(Files::isDirectory)
                    .flatMap(this::cenariosDaTrilha)
                    .map(leitor::ler)
                    .sorted(Comparator.comparing(Cenario::id))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui listar " + diretorioDeConteudo, e);
        }
    }

    public Optional<Cenario> buscar(String id) {
        return listar().stream().filter(c -> c.id().equals(id)).findFirst();
    }

    private Stream<Path> cenariosDaTrilha(Path trilha) {
        try (Stream<Path> filhos = Files.list(trilha)) {
            return filhos
                    .filter(p -> Files.isRegularFile(p.resolve("cenario.md")))
                    .toList()
                    .stream();
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui listar " + trilha, e);
        }
    }
}
```

O `.toList().stream()` dentro de `cenariosDaTrilha` é deliberado: o `try-with-resources` fecha o `Stream` ao sair do método, então ele precisa ser materializado antes.

- [ ] **Step 9: Rodar a suíte inteira**

Run: `cd backend && ./mvnw test`
Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add backend/src
git commit -m "feat: leitura de Cenário e Asserções do disco"
```

---

### Task 3: Executar comandos e avaliar Asserções

**Files:**
- Create: `backend/src/main/java/dev/learninginfra/execucao/SaidaDeComando.java`
- Create: `backend/src/main/java/dev/learninginfra/execucao/ExecutorDeComando.java`
- Create: `backend/src/main/java/dev/learninginfra/execucao/ExecutorDeComandoReal.java`
- Create: `backend/src/main/java/dev/learninginfra/verificacao/ResultadoDeAsercao.java`
- Create: `backend/src/main/java/dev/learninginfra/verificacao/ResultadoDaVerificacao.java`
- Create: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Test: `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java`
- Test: `backend/src/test/java/dev/learninginfra/execucao/ExecutorDeComandoRealTest.java`

**Interfaces:**
- Consumes: `Assercao` e seus três records (Task 2).
- Produces:
  - `record SaidaDeComando(int codigoDeSaida, String stdout, String stderr)` com `boolean sucesso()`.
  - `interface ExecutorDeComando { SaidaDeComando executar(List<String> comando); }`.
  - `record ResultadoDeAsercao(String descricao, boolean passou, String detalhe)`.
  - `record ResultadoDaVerificacao(boolean concluido, List<ResultadoDeAsercao> asercoes)`.
  - `MotorDeVerificacao.verificar(List<Assercao>) -> ResultadoDaVerificacao`.

- [ ] **Step 1: Escrever o teste do motor que falha**

Crie `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java`. Note que ele sobe um servidor HTTP de verdade numa porta efêmera — sem dependência externa, `com.sun.net.httpserver` vem no JDK.

```java
package dev.learninginfra.verificacao;

import com.sun.net.httpserver.HttpServer;
import dev.learninginfra.execucao.ExecutorDeComando;
import dev.learninginfra.execucao.SaidaDeComando;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MotorDeVerificacaoTest {

    private HttpServer servidor;
    private String base;

    @BeforeEach
    void subirServidor() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            byte[] corpo = "<h1>Meu primeiro container</h1>".getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(200, corpo.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(corpo);
            }
        });
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @AfterEach
    void derrubarServidor() {
        servidor.stop(0);
    }

    private MotorDeVerificacao motorQueResponde(String stdout, int codigo) {
        ExecutorDeComando falso = comando -> new SaidaDeComando(codigo, stdout, "");
        return new MotorDeVerificacao(falso);
    }

    @Test
    void containerRodandoPassaQuandoDockerDizTrue() {
        var resultado = motorQueResponde("true\n", 0)
                .verificar(List.of(new Assercao.ContainerRodando("lab-web")));

        assertTrue(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().passou());
    }

    @Test
    void containerParadoFalhaComDetalheUtil() {
        var resultado = motorQueResponde("false\n", 0)
                .verificar(List.of(new Assercao.ContainerRodando("lab-web")));

        assertFalse(resultado.concluido());
        assertEquals("existe, mas está parado", resultado.asercoes().getFirst().detalhe());
    }

    @Test
    void containerInexistenteFalhaComDetalheDiferenteDeParado() {
        var resultado = motorQueResponde("", 1)
                .verificar(List.of(new Assercao.ContainerRodando("lab-web")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("não existe"));
    }

    @Test
    void httpRespondePassaContraServidorDeVerdade() {
        var resultado = motorQueResponde("true\n", 0)
                .verificar(List.of(new Assercao.HttpResponde(base, 200)));

        assertTrue(resultado.concluido());
    }

    @Test
    void httpRespondeFalhaQuandoNadaEscuta() {
        var resultado = motorQueResponde("true\n", 0)
                .verificar(List.of(new Assercao.HttpResponde("http://127.0.0.1:1", 200)));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("nada respondeu"));
    }

    @Test
    void corpoContemPassaEFalhaConformeOTexto() {
        var passou = motorQueResponde("true\n", 0)
                .verificar(List.of(new Assercao.HttpCorpoContem(base, "primeiro container")));
        var falhou = motorQueResponde("true\n", 0)
                .verificar(List.of(new Assercao.HttpCorpoContem(base, "texto ausente")));

        assertTrue(passou.concluido());
        assertFalse(falhou.concluido());
    }

    @Test
    void todasAsAsercoesSaoAvaliadasMesmoQuandoAPrimeiraFalha() {
        var resultado = motorQueResponde("false\n", 0).verificar(List.of(
                new Assercao.ContainerRodando("lab-web"),
                new Assercao.HttpResponde(base, 200)));

        assertEquals(2, resultado.asercoes().size());
        assertFalse(resultado.asercoes().get(0).passou());
        assertTrue(resultado.asercoes().get(1).passou());
    }
}
```

O último teste é o que protege o valor do design: o leitor precisa ver *todas* as Asserções que falharam de uma vez, não parar na primeira.

- [ ] **Step 2: Rodar para ver falhar**

Run: `cd backend && ./mvnw test -Dtest=MotorDeVerificacaoTest`
Expected: FAIL na compilação — as classes não existem.

- [ ] **Step 3: Escrever os tipos de execução**

`SaidaDeComando.java`:

```java
package dev.learninginfra.execucao;

public record SaidaDeComando(int codigoDeSaida, String stdout, String stderr) {

    public boolean sucesso() {
        return codigoDeSaida == 0;
    }
}
```

`ExecutorDeComando.java`:

```java
package dev.learninginfra.execucao;

import java.util.List;

public interface ExecutorDeComando {

    SaidaDeComando executar(List<String> comando);
}
```

- [ ] **Step 4: Escrever os tipos de resultado**

`ResultadoDeAsercao.java`:

```java
package dev.learninginfra.verificacao;

public record ResultadoDeAsercao(String descricao, boolean passou, String detalhe) {

    public static ResultadoDeAsercao aprovada(Assercao asercao) {
        return new ResultadoDeAsercao(asercao.descricao(), true, "");
    }

    public static ResultadoDeAsercao reprovada(Assercao asercao, String detalhe) {
        return new ResultadoDeAsercao(asercao.descricao(), false, detalhe);
    }
}
```

`ResultadoDaVerificacao.java`:

```java
package dev.learninginfra.verificacao;

import java.util.List;

public record ResultadoDaVerificacao(boolean concluido, List<ResultadoDeAsercao> asercoes) {
}
```

- [ ] **Step 5: Escrever `MotorDeVerificacao`**

```java
package dev.learninginfra.verificacao;

import dev.learninginfra.execucao.ExecutorDeComando;
import dev.learninginfra.execucao.SaidaDeComando;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class MotorDeVerificacao {

    private final ExecutorDeComando executor;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public MotorDeVerificacao(ExecutorDeComando executor) {
        this.executor = executor;
    }

    public ResultadoDaVerificacao verificar(List<Assercao> asercoes) {
        List<ResultadoDeAsercao> resultados = asercoes.stream().map(this::avaliar).toList();
        boolean concluido = resultados.stream().allMatch(ResultadoDeAsercao::passou);
        return new ResultadoDaVerificacao(concluido, resultados);
    }

    private ResultadoDeAsercao avaliar(Assercao asercao) {
        return switch (asercao) {
            case Assercao.ContainerRodando a -> avaliarContainer(a);
            case Assercao.HttpResponde a -> avaliarStatus(a);
            case Assercao.HttpCorpoContem a -> avaliarCorpo(a);
        };
    }

    private ResultadoDeAsercao avaliarContainer(Assercao.ContainerRodando a) {
        SaidaDeComando saida = executor.executar(
                List.of("docker", "inspect", "-f", "{{.State.Running}}", a.nome()));
        if (!saida.sucesso()) {
            return ResultadoDeAsercao.reprovada(a, "o container `" + a.nome() + "` não existe");
        }
        boolean rodando = saida.stdout().trim().equals("true");
        return rodando
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a, "existe, mas está parado");
    }

    private ResultadoDeAsercao avaliarStatus(Assercao.HttpResponde a) {
        return buscar(a.url())
                .map(resposta -> resposta.statusCode() == a.status()
                        ? ResultadoDeAsercao.aprovada(a)
                        : ResultadoDeAsercao.reprovada(a, "respondeu " + resposta.statusCode()))
                .orElseGet(() -> ResultadoDeAsercao.reprovada(a, "nada respondeu em " + a.url()));
    }

    private ResultadoDeAsercao avaliarCorpo(Assercao.HttpCorpoContem a) {
        return buscar(a.url())
                .map(resposta -> resposta.body().contains(a.texto())
                        ? ResultadoDeAsercao.aprovada(a)
                        : ResultadoDeAsercao.reprovada(a, "respondeu, mas sem o texto esperado"))
                .orElseGet(() -> ResultadoDeAsercao.reprovada(a, "nada respondeu em " + a.url()));
    }

    private java.util.Optional<HttpResponse<String>> buscar(String url) {
        try {
            HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            return java.util.Optional.of(http.send(requisicao, HttpResponse.BodyHandlers.ofString()));
        } catch (IOException e) {
            return java.util.Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return java.util.Optional.empty();
        }
    }
}
```

O `switch` sobre a `sealed interface` é exaustivo — não há `default`. Quando uma quarta Asserção for adicionada, isto **não compila** até que ela seja tratada. Esse erro de compilação é a rede de segurança do vocabulário; não a remova adicionando um `default`.

- [ ] **Step 6: Rodar os testes do motor**

Run: `cd backend && ./mvnw test -Dtest=MotorDeVerificacaoTest`
Expected: PASS, 7 testes.

- [ ] **Step 7: Escrever `ExecutorDeComandoReal`**

```java
package dev.learninginfra.execucao;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class ExecutorDeComandoReal implements ExecutorDeComando {

    private static final int TIMEOUT_SEGUNDOS = 30;

    @Override
    public SaidaDeComando executar(List<String> comando) {
        try {
            Process processo = new ProcessBuilder(comando).start();
            try (ExecutorService leitores = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<String> stdout = leitores.submit(() -> ler(processo.getInputStream()));
                Future<String> stderr = leitores.submit(() -> ler(processo.getErrorStream()));

                if (!processo.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)) {
                    processo.destroyForcibly();
                    return new SaidaDeComando(-1, "", "tempo esgotado após " + TIMEOUT_SEGUNDOS + "s");
                }
                return new SaidaDeComando(processo.exitValue(), stdout.get(), stderr.get());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui executar " + comando, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new SaidaDeComando(-1, "", "interrompido");
        } catch (java.util.concurrent.ExecutionException e) {
            return new SaidaDeComando(-1, "", "falha lendo a saída: " + e.getCause());
        }
    }

    private String ler(java.io.InputStream entrada) throws IOException {
        return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
    }
}
```

Ler stdout e stderr em threads separadas não é preciosismo: se o processo enche o buffer de um dos canais enquanto você lê o outro sequencialmente, ele trava para sempre. Threads virtuais (Java 21+) tornam isso barato.

- [ ] **Step 8: Escrever o teste do executor real**

Crie `backend/src/test/java/dev/learninginfra/execucao/ExecutorDeComandoRealTest.java`:

```java
package dev.learninginfra.execucao;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorDeComandoRealTest {

    @Test
    void capturaSaidaEcodigoDeComandoQueExiste() {
        SaidaDeComando saida = new ExecutorDeComandoReal().executar(List.of("docker", "--version"));

        assertTrue(saida.sucesso(), "esperava sucesso, veio: " + saida);
        assertTrue(saida.stdout().toLowerCase().contains("docker"));
    }

    @Test
    void devolveCodigoDeErroSemLancarQuandoOComandoFalha() {
        SaidaDeComando saida = new ExecutorDeComandoReal()
                .executar(List.of("docker", "inspect", "container-que-nao-existe-jamais"));

        assertFalse(saida.sucesso());
    }
}
```

`docker --version` é do cliente e não precisa do daemon. O segundo teste precisa do daemon no ar.

- [ ] **Step 9: Rodar a suíte inteira**

Run: `cd backend && ./mvnw test`
Expected: PASS. Se `ExecutorDeComandoRealTest` falhar, confirme que o Docker Desktop está rodando: `docker context ls` precisa responder sem pendurar.

- [ ] **Step 10: Commit**

```bash
git add backend/src
git commit -m "feat: motor de verificação por asserções"
```

---

### Task 4: Cenário Ativo e progresso

**Files:**
- Create: `backend/src/main/java/dev/learninginfra/progresso/Progresso.java`
- Create: `backend/src/main/java/dev/learninginfra/progresso/RepositorioDeProgresso.java`
- Create: `backend/src/main/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivo.java`
- Test: `backend/src/test/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivoTest.java`

**Interfaces:**
- Consumes: `Cenario` (Task 2), `ExecutorDeComando` e `SaidaDeComando` (Task 3), `RepositorioDeCenarios` (Task 2).
- Produces:
  - `record Progresso(String cenarioAtivo, Map<String, String> concluidos)` com `static Progresso vazio()`.
  - `RepositorioDeProgresso.carregar() -> Progresso` e `salvar(Progresso)`.
  - `GerenciadorDeCenarioAtivo.iniciar(Cenario) -> Path` (devolve o diretório de trabalho absoluto), `marcarConcluido(Cenario)`, `cenarioAtivo() -> Optional<String>`.

- [ ] **Step 1: Escrever o teste que falha**

```java
package dev.learninginfra.ciclodevida;

import dev.learninginfra.conteudo.Cenario;
import dev.learninginfra.conteudo.Dificuldade;
import dev.learninginfra.conteudo.RepositorioDeCenarios;
import dev.learninginfra.execucao.ExecutorDeComando;
import dev.learninginfra.execucao.SaidaDeComando;
import dev.learninginfra.progresso.RepositorioDeProgresso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GerenciadorDeCenarioAtivoTest {

    @TempDir
    Path raiz;

    private final List<List<String>> comandosExecutados = new ArrayList<>();

    private final ExecutorDeComando executorEspiao = comando -> {
        comandosExecutados.add(comando);
        return new SaidaDeComando(0, "", "");
    };

    private Cenario cenario(String id, List<String> containers) throws Exception {
        Path diretorio = raiz.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(diretorio.resolve("workspace").resolve("site"));
        Files.writeString(diretorio.resolve("workspace").resolve("site").resolve("index.html"), "<h1>ola</h1>");
        return new Cenario(id, "titulo", Dificuldade.GUIADO, containers, "# corpo", diretorio, List.of());
    }

    private GerenciadorDeCenarioAtivo gerenciador(RepositorioDeCenarios repositorio) {
        return new GerenciadorDeCenarioAtivo(
                raiz.resolve("work").toString(),
                executorEspiao,
                new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString()),
                repositorio);
    }

    @Test
    void materializaOWorkspaceNoDiretorioDeTrabalho() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        Path trabalho = gerenciador(repositorio).iniciar(primeiro);

        assertTrue(trabalho.isAbsolute());
        assertTrue(Files.exists(trabalho.resolve("site/index.html")));
        assertEquals("<h1>ola</h1>", Files.readString(trabalho.resolve("site/index.html")));
    }

    @Test
    void iniciarDerrubaOsContainersDoCenarioAnterior() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        Cenario segundo = cenario("docker/02", List.of("lab-api"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/01")).thenReturn(Optional.of(primeiro));

        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        gerenciador.iniciar(primeiro);
        comandosExecutados.clear();
        gerenciador.iniciar(segundo);

        assertEquals(List.of(List.of("docker", "rm", "-f", "lab-web")), comandosExecutados);
    }

    @Test
    void iniciarLimpaSobrasDoWorkspaceAnterior() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        Cenario segundo = cenario("docker/02", List.of("lab-api"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/01")).thenReturn(Optional.of(primeiro));

        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        Path trabalho = gerenciador.iniciar(primeiro);
        Files.writeString(trabalho.resolve("lixo.txt"), "sobra");

        gerenciador.iniciar(segundo);

        assertFalse(Files.exists(trabalho.resolve("lixo.txt")));
    }

    @Test
    void oCenarioAtivoSobreviveAUmNovoGerenciador() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        gerenciador(repositorio).iniciar(primeiro);

        assertEquals(Optional.of("docker/01"), gerenciador(repositorio).cenarioAtivo());
    }

    @Test
    void marcarConcluidoRegistraNoProgresso() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);

        gerenciador.iniciar(primeiro);
        gerenciador.marcarConcluido(primeiro);

        assertTrue(new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString())
                .carregar().concluidos().containsKey("docker/01"));
    }

    @Test
    void falhaDeTeardownEhRuidosa() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        Cenario segundo = cenario("docker/02", List.of("lab-api"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/01")).thenReturn(Optional.of(primeiro));

        ExecutorDeComando executorQueFalha = comando ->
                comando.contains("rm") ? new SaidaDeComando(1, "", "daemon fora do ar")
                                       : new SaidaDeComando(0, "", "");
        var gerenciador = new GerenciadorDeCenarioAtivo(
                raiz.resolve("work").toString(),
                executorQueFalha,
                new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString()), repositorio);

        gerenciador.iniciar(primeiro);

        var erro = assertThrows(IllegalStateException.class, () -> gerenciador.iniciar(segundo));
        assertTrue(erro.getMessage().contains("lab-web"));
    }
}
```

O último teste implementa a consequência escrita na ADR 0002: teardown que falha em silêncio reintroduz exatamente o bug que a invariante existe para eliminar.

- [ ] **Step 2: Confirmar que Mockito está disponível**

No Spring Boot 4 o starter de teste chama-se `spring-boot-starter-webmvc-test`, e ele
já traz Mockito transitivamente — verificado em execução real. Confirme que existe no
`pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

Não adicione `spring-boot-starter-test` — esse é o nome anterior ao Boot 4.

Ao rodar, o Mockito emite este aviso no Java 25:

> Mockito is currently self-attaching to enable the inline-mock-maker. This will no
> longer work in future releases of the JDK.

É só aviso, os testes passam. Se um JDK futuro transformar isso em erro, a correção é
declarar o agente do Byte Buddy no `maven-surefire-plugin` via `argLine`, em vez de
deixar o Mockito se auto-anexar.

- [ ] **Step 3: Rodar para ver falhar**

Run: `cd backend && ./mvnw test -Dtest=GerenciadorDeCenarioAtivoTest`
Expected: FAIL na compilação.

- [ ] **Step 4: Escrever `Progresso`**

```java
package dev.learninginfra.progresso;

import java.util.Map;

public record Progresso(String cenarioAtivo, Map<String, String> concluidos) {

    public static Progresso vazio() {
        return new Progresso(null, Map.of());
    }

    public Progresso comAtivo(String id) {
        return new Progresso(id, concluidos);
    }

    public Progresso comConcluido(String id, String instante) {
        var novos = new java.util.LinkedHashMap<>(concluidos);
        novos.put(id, instante);
        return new Progresso(cenarioAtivo, Map.copyOf(novos));
    }
}
```

- [ ] **Step 5: Escrever `RepositorioDeProgresso`**

```java
package dev.learninginfra.progresso;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class RepositorioDeProgresso {

    private final Path arquivo;
    private final ObjectMapper json = new ObjectMapper();

    public RepositorioDeProgresso(@Value("${learninginfra.arquivo-de-progresso}") String arquivo) {
        this.arquivo = Path.of(arquivo);
    }

    public Progresso carregar() {
        if (!Files.isRegularFile(arquivo)) {
            return Progresso.vazio();
        }
        try {
            return json.readValue(arquivo.toFile(), Progresso.class);
        } catch (IOException e) {
            throw new UncheckedIOException("progresso ilegível em " + arquivo, e);
        }
    }

    public void salvar(Progresso progresso) {
        try {
            Files.createDirectories(arquivo.toAbsolutePath().getParent());
            json.writerWithDefaultPrettyPrinter().writeValue(arquivo.toFile(), progresso);
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui gravar " + arquivo, e);
        }
    }
}
```

Nota sobre o import do Jackson: o Spring Boot 4 pode trazer Jackson 3, cujo pacote
raiz é `tools.jackson` em vez de `com.fasterxml.jackson`. Se
`com.fasterxml.jackson.databind.ObjectMapper` não resolver, troque por
`tools.jackson.databind.ObjectMapper` — os métodos usados aqui (`readValue`,
`writerWithDefaultPrettyPrinter`) têm o mesmo nome nas duas versões. Records são
desserializados nativamente desde o Jackson 2.12, sem módulo extra.

- [ ] **Step 6: Escrever `GerenciadorDeCenarioAtivo`**

```java
package dev.learninginfra.ciclodevida;

import dev.learninginfra.conteudo.Cenario;
import dev.learninginfra.conteudo.RepositorioDeCenarios;
import dev.learninginfra.execucao.ExecutorDeComando;
import dev.learninginfra.execucao.SaidaDeComando;
import dev.learninginfra.progresso.Progresso;
import dev.learninginfra.progresso.RepositorioDeProgresso;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class GerenciadorDeCenarioAtivo {

    private final Path diretorioDeTrabalho;
    private final ExecutorDeComando executor;
    private final RepositorioDeProgresso progressos;
    private final RepositorioDeCenarios cenarios;

    public GerenciadorDeCenarioAtivo(
            @Value("${learninginfra.diretorio-de-trabalho}") String diretorioDeTrabalho,
            ExecutorDeComando executor,
            RepositorioDeProgresso progressos,
            RepositorioDeCenarios cenarios) {
        this.diretorioDeTrabalho = Path.of(diretorioDeTrabalho);
        this.executor = executor;
        this.progressos = progressos;
        this.cenarios = cenarios;
    }

    public Optional<String> cenarioAtivo() {
        return Optional.ofNullable(progressos.carregar().cenarioAtivo());
    }

    /** Derruba o Cenário anterior, materializa o workspace deste e o torna o Cenário Ativo. */
    public Path iniciar(Cenario cenario) {
        cenarioAtivo()
                .filter(id -> !id.equals(cenario.id()))
                .flatMap(cenarios::buscar)
                .ifPresent(this::derrubar);

        Path trabalho = diretorioDeTrabalho.toAbsolutePath().normalize();
        apagarRecursivamente(trabalho);
        copiarWorkspace(cenario, trabalho);

        progressos.salvar(progressos.carregar().comAtivo(cenario.id()));
        return trabalho;
    }

    public void marcarConcluido(Cenario cenario) {
        progressos.salvar(progressos.carregar()
                .comConcluido(cenario.id(), Instant.now().toString()));
    }

    private void derrubar(Cenario anterior) {
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

    private boolean pareceInexistente(SaidaDeComando saida) {
        String erro = saida.stderr().toLowerCase();
        return erro.contains("no such container") || erro.contains("not found");
    }

    private void copiarWorkspace(Cenario cenario, Path destino) {
        Path origem = cenario.workspace();
        try {
            Files.createDirectories(destino);
            if (!Files.isDirectory(origem)) {
                return;
            }
            try (var caminhos = Files.walk(origem)) {
                for (Path caminho : caminhos.toList()) {
                    Path alvo = destino.resolve(origem.relativize(caminho).toString());
                    if (Files.isDirectory(caminho)) {
                        Files.createDirectories(alvo);
                    } else {
                        Files.createDirectories(alvo.getParent());
                        Files.copy(caminho, alvo);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui materializar o workspace de " + cenario.id(), e);
        }
    }

    private void apagarRecursivamente(Path raiz) {
        if (!Files.exists(raiz)) {
            return;
        }
        try {
            Files.walkFileTree(raiz, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path arquivo, BasicFileAttributes atributos) throws IOException {
                    Files.delete(arquivo);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path diretorio, IOException erro) throws IOException {
                    Files.delete(diretorio);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui limpar " + raiz, e);
        }
    }
}
```

`iniciar` devolve o caminho **absoluto e normalizado** de propósito: o leitor precisa colar esse caminho num `docker run -v`, e caminho relativo não serve para o daemon.

- [ ] **Step 7: Rodar os testes**

Run: `cd backend && ./mvnw test -Dtest=GerenciadorDeCenarioAtivoTest`
Expected: PASS, 6 testes.

- [ ] **Step 8: Rodar a suíte inteira e commitar**

```bash
cd backend && ./mvnw test
cd .. && git add backend/src && git commit -m "feat: cenário ativo, teardown e progresso"
```

---

### Task 5: API REST

**Files:**
- Create: `backend/src/main/java/dev/learninginfra/api/dto/CenarioDetalhado.java`
- Create: `backend/src/main/java/dev/learninginfra/api/CenarioController.java`
- Test: `backend/src/test/java/dev/learninginfra/api/CenarioControllerTest.java`

**Interfaces:**
- Consumes: `RepositorioDeCenarios` (Task 2), `MotorDeVerificacao` e `ResultadoDaVerificacao` (Task 3), `GerenciadorDeCenarioAtivo` (Task 4).
- Produces os endpoints, todos sob `/api`:
  - `GET /api/cenarios` → `[{id, titulo, dificuldade, concluido}]`
  - `GET /api/cenarios/{trilha}/{slug}` → `CenarioDetalhado`
  - `POST /api/cenarios/{trilha}/{slug}/iniciar` → `{diretorioDeTrabalho}`
  - `POST /api/cenarios/{trilha}/{slug}/verificar` → `ResultadoDaVerificacao`

O id do Cenário contém uma barra (`docker/01-servir-html-nginx`), por isso a rota usa **duas** variáveis de caminho em vez de uma. Não tente passar o id inteiro numa `@PathVariable` única.

- [ ] **Step 1: Escrever o teste que falha**

```java
package dev.learninginfra.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = "learninginfra.diretorio-de-conteudo=../content")
class CenarioControllerTest {

    @Autowired
    WebApplicationContext contexto;

    MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void listaOsCenariosDisponiveis() throws Exception {
        mvc().perform(get("/api/cenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("docker/01-servir-html-nginx"))
                .andExpect(jsonPath("$[0].dificuldade").value("GUIADO"));
    }

    @Test
    void devolveODetalheComOMarkdown() throws Exception {
        mvc().perform(get("/api/cenarios/docker/01-servir-html-nginx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Servir um HTML seu com nginx"))
                .andExpect(jsonPath("$.markdown").isNotEmpty())
                .andExpect(jsonPath("$.asercoes.length()").value(3));
    }

    @Test
    void devolve404ParaCenarioInexistente() throws Exception {
        mvc().perform(get("/api/cenarios/docker/nao-existe"))
                .andExpect(status().isNotFound());
    }
}
```

Este teste depende do conteúdo da Task 8. Ele vai falhar até lá — é esperado e correto: escreva o controller agora, confirme que compila e que o 404 passa, e reveja este teste ao fim da Task 8.

- [ ] **Step 2: Escrever o DTO**

```java
package dev.learninginfra.api.dto;

import dev.learninginfra.conteudo.Cenario;
import dev.learninginfra.conteudo.Dificuldade;
import dev.learninginfra.verificacao.Assercao;

import java.util.List;

public record CenarioDetalhado(
        String id,
        String titulo,
        Dificuldade dificuldade,
        String markdown,
        List<String> asercoes,
        List<String> containers,
        boolean ativo,
        boolean concluido) {

    public static CenarioDetalhado de(Cenario cenario, boolean ativo, boolean concluido) {
        return new CenarioDetalhado(
                cenario.id(),
                cenario.titulo(),
                cenario.dificuldade(),
                cenario.markdown(),
                cenario.asercoes().stream().map(Assercao::descricao).toList(),
                cenario.containers(),
                ativo,
                concluido);
    }
}
```

O DTO expõe só a `descricao()` de cada Asserção, nunca a Asserção crua — o front não precisa saber que a verificação roda `docker inspect`, e o leitor não deve ver a resposta antes de tentar.

- [ ] **Step 3: Escrever o controller**

```java
package dev.learninginfra.api;

import dev.learninginfra.api.dto.CenarioDetalhado;
import dev.learninginfra.ciclodevida.GerenciadorDeCenarioAtivo;
import dev.learninginfra.conteudo.Cenario;
import dev.learninginfra.conteudo.RepositorioDeCenarios;
import dev.learninginfra.progresso.RepositorioDeProgresso;
import dev.learninginfra.verificacao.MotorDeVerificacao;
import dev.learninginfra.verificacao.ResultadoDaVerificacao;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CenarioController {

    private final RepositorioDeCenarios cenarios;
    private final RepositorioDeProgresso progressos;
    private final GerenciadorDeCenarioAtivo gerenciador;
    private final MotorDeVerificacao motor;

    public CenarioController(RepositorioDeCenarios cenarios, RepositorioDeProgresso progressos,
                             GerenciadorDeCenarioAtivo gerenciador, MotorDeVerificacao motor) {
        this.cenarios = cenarios;
        this.progressos = progressos;
        this.gerenciador = gerenciador;
        this.motor = motor;
    }

    @GetMapping("/cenarios")
    public List<CenarioDetalhado> listar() {
        return cenarios.listar().stream().map(this::detalhar).toList();
    }

    @GetMapping("/cenarios/{trilha}/{slug}")
    public CenarioDetalhado buscar(@PathVariable String trilha, @PathVariable String slug) {
        return detalhar(exigir(trilha, slug));
    }

    @PostMapping("/cenarios/{trilha}/{slug}/iniciar")
    public Map<String, String> iniciar(@PathVariable String trilha, @PathVariable String slug) {
        Cenario cenario = exigir(trilha, slug);
        return Map.of("diretorioDeTrabalho", gerenciador.iniciar(cenario).toString());
    }

    @PostMapping("/cenarios/{trilha}/{slug}/verificar")
    public ResultadoDaVerificacao verificar(@PathVariable String trilha, @PathVariable String slug) {
        Cenario cenario = exigir(trilha, slug);
        ResultadoDaVerificacao resultado = motor.verificar(cenario.asercoes());
        if (resultado.concluido()) {
            gerenciador.marcarConcluido(cenario);
        }
        return resultado;
    }

    private Cenario exigir(String trilha, String slug) {
        return cenarios.buscar(trilha + "/" + slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "cenário não encontrado: " + trilha + "/" + slug));
    }

    private CenarioDetalhado detalhar(Cenario cenario) {
        var progresso = progressos.carregar();
        return CenarioDetalhado.de(
                cenario,
                cenario.id().equals(progresso.cenarioAtivo()),
                progresso.concluidos().containsKey(cenario.id()));
    }
}
```

- [ ] **Step 4: Rodar o teste de 404**

Run: `cd backend && ./mvnw test -Dtest=CenarioControllerTest#devolve404ParaCenarioInexistente`
Expected: PASS. Os outros dois só passam depois da Task 8.

- [ ] **Step 5: Commit**

```bash
git add backend/src
git commit -m "feat: API REST de cenários"
```

---

### Task 6: Frontend — ler o Cenário

**Files:**
- Create: `frontend/package.json`, `frontend/vite.config.ts`, `frontend/tsconfig.json`, `frontend/index.html`
- Create: `frontend/src/main.tsx`, `frontend/src/api.ts`, `frontend/src/App.tsx`, `frontend/src/PaginaDoCenario.tsx`, `frontend/src/estilos.css`

**Interfaces:**
- Consumes: os endpoints da Task 5.
- Produces: `type CenarioDetalhado`, `type ResultadoDaVerificacao`, `type ResultadoDeAsercao` e as funções `buscarCenario`, `iniciarCenario`, `verificarCenario` em `api.ts`, usadas pela Task 7.

- [ ] **Step 1: Criar o projeto**

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend && npm install && npm install react-markdown
```

- [ ] **Step 2: Configurar o proxy para o backend**

`frontend/vite.config.ts`:

```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://127.0.0.1:8099',
    },
  },
})
```

- [ ] **Step 3: Escrever o cliente da API**

`frontend/src/api.ts`:

```ts
export type Dificuldade = 'GUIADO' | 'ASSISTIDO' | 'AUTONOMO' | 'MESTRE'

export type CenarioDetalhado = {
  id: string
  titulo: string
  dificuldade: Dificuldade
  markdown: string
  asercoes: string[]
  containers: string[]
  ativo: boolean
  concluido: boolean
}

export type ResultadoDeAsercao = {
  descricao: string
  passou: boolean
  detalhe: string
}

export type ResultadoDaVerificacao = {
  concluido: boolean
  asercoes: ResultadoDeAsercao[]
}

async function pedir<T>(url: string, metodo: 'GET' | 'POST' = 'GET'): Promise<T> {
  const resposta = await fetch(url, { method: metodo })
  if (!resposta.ok) {
    throw new Error(`${metodo} ${url} devolveu ${resposta.status}`)
  }
  return resposta.json() as Promise<T>
}

export const buscarCenario = (id: string) => pedir<CenarioDetalhado>(`/api/cenarios/${id}`)

export const iniciarCenario = (id: string) =>
  pedir<{ diretorioDeTrabalho: string }>(`/api/cenarios/${id}/iniciar`, 'POST')

export const verificarCenario = (id: string) =>
  pedir<ResultadoDaVerificacao>(`/api/cenarios/${id}/verificar`, 'POST')
```

`id` já contém a barra (`docker/01-servir-html-nginx`), então interpolar direto na URL produz o caminho de duas variáveis que o controller espera.

- [ ] **Step 4: Escrever `PaginaDoCenario`**

`frontend/src/PaginaDoCenario.tsx`:

```tsx
import { useEffect, useState } from 'react'
import Markdown from 'react-markdown'
import { buscarCenario, type CenarioDetalhado } from './api'

const rotulos: Record<string, string> = {
  GUIADO: 'Guiado',
  ASSISTIDO: 'Assistido',
  AUTONOMO: 'Autônomo',
  MESTRE: 'Mestre',
}

export function PaginaDoCenario({ id }: { id: string }) {
  const [cenario, setCenario] = useState<CenarioDetalhado | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    buscarCenario(id).then(setCenario).catch((e) => setErro(String(e)))
  }, [id])

  if (erro) return <p className="erro">{erro}</p>
  if (!cenario) return <p>Carregando…</p>

  return (
    <article>
      <header>
        <span className="dificuldade">{rotulos[cenario.dificuldade]}</span>
        <h1>{cenario.titulo}</h1>
      </header>
      <Markdown>{cenario.markdown}</Markdown>
    </article>
  )
}
```

- [ ] **Step 5: Escrever `App` e `main`**

`frontend/src/App.tsx`:

```tsx
import { PaginaDoCenario } from './PaginaDoCenario'
import './estilos.css'

export default function App() {
  return (
    <main>
      <PaginaDoCenario id="docker/01-servir-html-nginx" />
    </main>
  )
}
```

Um id fixo é deliberado: o esqueleto não tem catálogo nem roteamento. Isso entra quando existir um segundo Cenário.

`frontend/src/main.tsx`:

```tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
```

- [ ] **Step 6: Escrever o CSS mínimo**

`frontend/src/estilos.css`:

```css
:root { color-scheme: light dark; }

body {
  margin: 0;
  font-family: system-ui, sans-serif;
  line-height: 1.6;
}

main { max-width: 46rem; margin: 0 auto; padding: 2rem 1.5rem 6rem; }

.dificuldade {
  display: inline-block;
  font-size: 0.75rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  padding: 0.2rem 0.6rem;
  border: 1px solid currentColor;
  border-radius: 999px;
  opacity: 0.7;
}

pre {
  padding: 0.9rem 1rem;
  overflow-x: auto;
  border-radius: 6px;
  background: rgba(127, 127, 127, 0.14);
}

code { font-family: ui-monospace, monospace; font-size: 0.9em; }

.erro { color: #b00020; }
```

- [ ] **Step 7: Verificar no navegador**

Rode os dois processos em terminais separados:

```bash
cd backend && ./mvnw spring-boot:run
cd frontend && npm run dev
```

Abra `http://localhost:5173`. Até a Task 8 existir, a página mostra o erro de 404 — isso confirma que o front está falando com o backend.

- [ ] **Step 8: Commit**

```bash
git add frontend
git commit -m "feat: front-end lendo e renderizando o cenário"
```

---

### Task 7: Frontend — iniciar, verificar e o checklist

**Files:**
- Create: `frontend/src/ChecklistDeVerificacao.tsx`
- Modify: `frontend/src/PaginaDoCenario.tsx`
- Modify: `frontend/src/estilos.css`

**Interfaces:**
- Consumes: `iniciarCenario`, `verificarCenario`, `ResultadoDaVerificacao` (Task 6).
- Produces: nada consumido por tasks posteriores.

- [ ] **Step 1: Escrever `ChecklistDeVerificacao`**

```tsx
import type { ResultadoDaVerificacao } from './api'

export function ChecklistDeVerificacao({ resultado }: { resultado: ResultadoDaVerificacao }) {
  return (
    <section className="checklist">
      <h2>{resultado.concluido ? 'Cenário concluído' : 'Ainda não'}</h2>
      <ul>
        {resultado.asercoes.map((asercao) => (
          <li key={asercao.descricao} className={asercao.passou ? 'passou' : 'falhou'}>
            <span aria-hidden="true">{asercao.passou ? '✓' : '✗'}</span>
            <span>
              {asercao.descricao}
              {!asercao.passou && asercao.detalhe && <em> — {asercao.detalhe}</em>}
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}
```

Renderizar **todas** as Asserções, passando ou falhando, é o ponto do design: o leitor precisa ver o que já está certo, não só o que falta.

- [ ] **Step 2: Ligar os botões em `PaginaDoCenario`**

Substitua o conteúdo de `frontend/src/PaginaDoCenario.tsx`:

```tsx
import { useEffect, useState } from 'react'
import Markdown from 'react-markdown'
import {
  buscarCenario,
  iniciarCenario,
  verificarCenario,
  type CenarioDetalhado,
  type ResultadoDaVerificacao,
} from './api'
import { ChecklistDeVerificacao } from './ChecklistDeVerificacao'

const rotulos: Record<string, string> = {
  GUIADO: 'Guiado',
  ASSISTIDO: 'Assistido',
  AUTONOMO: 'Autônomo',
  MESTRE: 'Mestre',
}

export function PaginaDoCenario({ id }: { id: string }) {
  const [cenario, setCenario] = useState<CenarioDetalhado | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [diretorio, setDiretorio] = useState<string | null>(null)
  const [resultado, setResultado] = useState<ResultadoDaVerificacao | null>(null)
  const [ocupado, setOcupado] = useState(false)

  useEffect(() => {
    buscarCenario(id).then(setCenario).catch((e) => setErro(String(e)))
  }, [id])

  async function aoIniciar() {
    setOcupado(true)
    setErro(null)
    setResultado(null)
    try {
      const { diretorioDeTrabalho } = await iniciarCenario(id)
      setDiretorio(diretorioDeTrabalho)
    } catch (e) {
      setErro(String(e))
    } finally {
      setOcupado(false)
    }
  }

  async function aoVerificar() {
    setOcupado(true)
    setErro(null)
    try {
      setResultado(await verificarCenario(id))
    } catch (e) {
      setErro(String(e))
    } finally {
      setOcupado(false)
    }
  }

  if (erro && !cenario) return <p className="erro">{erro}</p>
  if (!cenario) return <p>Carregando…</p>

  return (
    <article>
      <header>
        <span className="dificuldade">{rotulos[cenario.dificuldade]}</span>
        <h1>{cenario.titulo}</h1>
      </header>

      <div className="acoes">
        <button onClick={aoIniciar} disabled={ocupado}>Iniciar cenário</button>
        <button onClick={aoVerificar} disabled={ocupado || !diretorio}>Verificar</button>
      </div>

      {diretorio && (
        <p className="diretorio">
          Seu diretório de trabalho: <code>{diretorio}</code>
        </p>
      )}

      {erro && <p className="erro">{erro}</p>}

      <Markdown>{cenario.markdown}</Markdown>

      {resultado && <ChecklistDeVerificacao resultado={resultado} />}
    </article>
  )
}
```

O botão **Verificar** fica desabilitado até você iniciar o Cenário. Sem isso, o leitor verifica contra sobras do ambiente anterior — a mesma classe de bug que a ADR 0002 existe para eliminar, só que na camada de UI.

- [ ] **Step 3: Estilos do checklist**

Acrescente ao fim de `frontend/src/estilos.css`:

```css
.acoes { display: flex; gap: 0.75rem; margin: 1.5rem 0; }

.acoes button {
  padding: 0.5rem 1rem;
  font: inherit;
  border-radius: 6px;
  border: 1px solid currentColor;
  background: transparent;
  cursor: pointer;
}

.acoes button:disabled { opacity: 0.4; cursor: not-allowed; }

.diretorio { font-size: 0.9rem; opacity: 0.85; }

.checklist { margin-top: 2.5rem; border-top: 1px solid rgba(127,127,127,0.3); padding-top: 1rem; }
.checklist ul { list-style: none; padding: 0; }
.checklist li { display: flex; gap: 0.6rem; padding: 0.35rem 0; align-items: baseline; }
.checklist .passou { color: #1a7f37; }
.checklist .falhou { color: #b00020; }
.checklist em { opacity: 0.85; }
```

- [ ] **Step 4: Commit**

```bash
git add frontend
git commit -m "feat: iniciar, verificar e checklist de asserções"
```

---

### Task 8: O conteúdo do Cenário #1

**Files:**
- Create: `content/docker/01-servir-html-nginx/cenario.md`
- Create: `content/docker/01-servir-html-nginx/verificacao.yaml`
- Create: `content/docker/01-servir-html-nginx/workspace/site/index.html`

**Interfaces:**
- Consumes: o formato lido pelo `LeitorDeCenario` (Task 2).
- Produces: o Cenário `docker/01-servir-html-nginx`, que faz os testes da Task 5 passarem.

- [ ] **Step 1: Escrever o arquivo entregue ao leitor**

`content/docker/01-servir-html-nginx/workspace/site/index.html`:

```html
<!doctype html>
<meta charset="utf-8">
<title>Meu primeiro container</title>
<h1>Meu primeiro container</h1>
<p>Se você está lendo isto pela porta 8080, o nginx está servindo o seu diretório.</p>
```

- [ ] **Step 2: Escrever a Verificação**

`content/docker/01-servir-html-nginx/verificacao.yaml`:

```yaml
asercoes:
  - tipo: container_rodando
    nome: lab-web
  - tipo: http_responde
    url: http://localhost:8080
    status: 200
  - tipo: http_corpo_contem
    url: http://localhost:8080
    texto: Meu primeiro container
```

- [ ] **Step 3: Escrever o Cenário**

`content/docker/01-servir-html-nginx/cenario.md`:

````markdown
---
id: docker/01-servir-html-nginx
titulo: Servir um HTML seu com nginx
dificuldade: guiado
containers: [lab-web]
---
# Servir um HTML seu com nginx

Você vai pegar um arquivo HTML que está no seu disco e fazer o nginx servi-lo, sem
instalar nginx nenhum. Ao final você terá usado as três coisas que sustentam todo o
resto de Docker: rodar um container, mapear uma porta e montar um volume.

## O que você recebeu

Clique em **Iniciar cenário**. Isso limpa o ambiente do cenário anterior e copia os
arquivos deste para o seu diretório de trabalho — o caminho aparece logo acima. Lá
dentro há um `site/index.html`.

Esse diretório existe no **seu disco**, e é isso que torna o exercício possível: o
Docker vai enxergá-lo direto.

## Passo 1 — rodar um container

O comando mais simples possível:

```sh
docker run --rm hello-world
```

- `docker run` cria um container a partir de uma imagem e o executa.
- `--rm` apaga o container quando ele termina. Sem isso, ele fica parado ocupando
  espaço — rode `docker ps -a` depois para ver os cadáveres que você já acumulou.
- `hello-world` é a imagem. Como você não tem ela localmente, o Docker baixa do
  registro público antes de rodar.

Container não é máquina virtual: é um processo do seu sistema, isolado. Quando o
processo termina, o container termina.

## Passo 2 — rodar algo que não termina

O nginx é um servidor: ele fica no ar esperando requisições.

```sh
docker run -d --name lab-web nginx
```

- `-d` (*detached*) devolve o seu terminal em vez de prender no log do processo.
- `--name lab-web` dá um nome fixo ao container. Sem isso o Docker inventa um nome
  aleatório e você precisa do id para tudo. Este cenário **exige** o nome `lab-web`.

Confirme:

```sh
docker ps
```

Agora tente abrir `http://localhost:8080` no navegador. **Não vai funcionar** — e a
razão é o próximo passo.

## Passo 3 — mapear a porta

O nginx está escutando na porta 80 *dentro* do container. Essa porta não é a sua: o
container tem a própria pilha de rede. Para alcançá-la, você publica a porta no host.

Remova o container anterior e recrie com o mapeamento:

```sh
docker rm -f lab-web
docker run -d --name lab-web -p 8080:80 nginx
```

- `-p 8080:80` significa **porta 8080 do seu host** → **porta 80 do container**. A
  ordem é sempre `host:container`. Inverter é o erro mais comum aqui.

Abra `http://localhost:8080`. Você deve ver a página padrão do nginx.

## Passo 4 — montar o seu diretório

Falta servir o *seu* HTML. O nginx serve o que estiver em
`/usr/share/nginx/html` dentro do container. Você vai apontar essa pasta para a sua.

Use o caminho de trabalho que apareceu ao iniciar o cenário, acrescido de `\site`:

```sh
docker rm -f lab-web
docker run -d --name lab-web -p 8080:80 -v C:\caminho\completo\ate\work\site:/usr/share/nginx/html:ro nginx
```

- `-v origem:destino` monta um diretório do host dentro do container. A ordem é
  `host:container`, igual ao `-p`.
- A origem precisa ser um caminho **absoluto**. Caminho relativo o daemon não resolve.
- `:ro` monta somente-leitura. O nginx não precisa escrever ali, e restringir o que
  um container pode fazer é um hábito que vale desde o primeiro dia.

Recarregue `http://localhost:8080`. Agora é o seu HTML.

## Verifique

Clique em **Verificar**. Três Asserções serão checadas: o container `lab-web` está
rodando, a porta 8080 responde 200, e o corpo contém o título do seu HTML.

Se alguma falhar, o detalhe ao lado diz exatamente qual das três quebrou — o que
quase sempre aponta o passo que faltou.

## O que você aprendeu

`docker run` cria e executa. `-d` solta o terminal. `--name` te dá um identificador
estável. `-p host:container` publica uma porta. `-v host:container` monta um
diretório. Praticamente todo comando `docker run` que você vai ver na vida é uma
combinação disso com mais opções.
````

- [ ] **Step 4: Rodar os testes do controller que dependiam do conteúdo**

Run: `cd backend && ./mvnw test -Dtest=CenarioControllerTest`
Expected: PASS, 3 testes.

- [ ] **Step 5: Rodar a suíte inteira**

Run: `cd backend && ./mvnw test`
Expected: PASS.

- [ ] **Step 6: Percorrer o cenário à mão**

Este é o teste que importa — o esqueleto existe para isto:

1. Suba backend e frontend.
2. Abra `http://localhost:5173`, clique em **Iniciar cenário**.
3. Confirme que `work/site/index.html` foi criado no disco.
4. Clique em **Verificar** sem fazer nada — as três Asserções devem **falhar**, cada
   uma com o seu detalhe. Uma verificação que passa aqui é um bug.
5. Siga o cenário até o passo 4.
6. Clique em **Verificar** — as três devem passar.
7. Confirme que `data/progresso.json` registrou a conclusão.

- [ ] **Step 7: Commit**

```bash
git add content backend/src
git commit -m "feat: cenário docker/01 servir html com nginx"
```

---

## Depois do esqueleto

Fora de escopo aqui, na ordem em que provavelmente vão doer:

1. **Catálogo e roteamento** — no segundo Cenário, quando o id fixo em `App.tsx` virar mentira.
2. **Suporte a Compose** — no primeiro Cenário multi-serviço. `iniciar` ganha `docker compose -p <id> up -d`, `derrubar` ganha `down -v`.
3. **Empacotar num `java -jar` só** — quando cansar de subir dois processos.
4. **Novas Asserções** — só quando um Cenário real precisar. O `switch` exaustivo do `MotorDeVerificacao` vai te avisar em tempo de compilação onde tocar.
