# Trilha IaC — Etapa 1: plataforma

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dar à plataforma o vocabulário mínimo para verificar Cenários de infraestrutura
como código, e provar o caminho inteiro com o Cenário 01 da Trilha IaC no ar.

**Architecture:** Duas Asserções tipadas novas (`terraform_estado` e
`terraform_plano_limpo`) entram no `Assercao` selado; o `switch` exaustivo do
`MotorDeVerificacao` quebra a compilação até tratá-las, como o README exige. O Cenário
declara `terraform: true` e um `diretorioTerraform` **relativo** ao diretório de
trabalho; o `MotorDeVerificacao` resolve esse caminho contra `learninginfra.diretorio-de-trabalho`,
confina o resultado dentro dele e chama o binário com `terraform -chdir=<absoluto>`. Usar
`-chdir` evita mexer no `ExecutorDeComando`, que hoje não tem diretório de trabalho.

**Tech Stack:** Java 25 · Spring Boot 4.0.0 · JUnit 5 · AssertJ · SnakeYAML ·
Terraform 1.15.8 · provider `kreuzwerker/docker` ~> 4.5

## Global Constraints

- Terraform CLI **1.15.8**, pinado. `tofu` não é usado nesta etapa.
- Provider `kreuzwerker/docker` **~> 4.5**; nenhum outro provider entra na Etapa 1.
- Imagem de container do conteúdo: **`nginx:1.27-alpine`**, a mesma que as outras 14
  ocorrências do `content/` já usam.
- Porta do Cenário 01: **8070**. O bloco 8070–8079 é reservado para a Trilha IaC.
- Container do Cenário 01: **`mirante-web`**.
- Todo texto de conteúdo, nome de classe, método, variável e mensagem de erro em
  **português**, seguindo o repositório.
- O vocabulário de Asserções é fechado e pequeno de propósito: **nenhum `default:`** no
  `switch` do `MotorDeVerificacao`.
- A Verificação é estritamente observadora: **nunca** roda `apply`, `destroy` ou `init`.
- Comandos de exemplo no conteúdo são PowerShell, como nas outras trilhas.
- Rode a suíte a partir de `backend/` com `./mvnw test`. Ela precisa do Docker no ar.

## Estrutura de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `backend/src/main/java/dev/learninginfra/conteudo/Cenario.java` | ganha `terraform` e `diretorioTerraform` |
| `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java` | lê e valida o frontmatter novo; monta as duas Asserções |
| `backend/src/main/java/dev/learninginfra/verificacao/Assercao.java` | os dois records novos |
| `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java` | executa `terraform -chdir=…`, resolve e confina o diretório |
| `backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java` | frontmatter e montagem das Asserções |
| `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java` | avaliação das duas Asserções |
| `backend/src/test/java/dev/learninginfra/conteudo/GuardrailsDeTerraformTest.java` | **novo**: guardrails executáveis sobre os `.tf` versionados |
| `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java` | contagens do catálogo com a quarta Trilha |
| `content/iac/trilha.yaml` | manifesto da Trilha, sem Fundamentos nesta etapa |
| `content/iac/01-o-sabado-em-que-a-mirante-subiu/` | Cenário 01: `cenario.md`, `verificacao.yaml`, `workspace/versions.tf` |
| `README.md` | pré-requisito, preparo da Trilha, portas, frontmatter e Asserções novas |

## Duas decisões de implementação que divergem do spec

1. **`terraform_estado` não usa JMESPath.** O spec previa uma expressão sobre
   `terraform show -json`, o que exigiria uma biblioteca JMESPath nova num backend cujas
   únicas dependências hoje são Spring Boot e SnakeYAML. A Asserção usa
   `terraform state show -no-color <endereço>` e compara um atributo nomeado. Fica
   tipada, sem dependência nova, e a mensagem de falha diz o valor observado.
2. **O alvo `iac` do `scripts-checar-conteudo.mjs` fica para a Etapa 2.** O spec listou-o
   nesta etapa, mas o script lê `fundamentos.md` e `questionario.yaml` na primeira linha
   e falharia sem eles. Ele já é genérico: `node frontend/scripts-checar-conteudo.mjs iac`
   passa a funcionar sozinho quando os Fundamentos existirem, e o que a Etapa 2 precisa
   acrescentar são apenas os três ids novos no `visuaisValidos`.
3. **A ordem das Trilhas no catálogo não muda.** `CatalogoDeTrilhas.listar()` ordena por
   id, então `iac` aparece entre `docker` e `kubernetes`. Hoje a ordem já não é
   pedagógica (`aws` vem primeiro); consertar isso mexeria nas três Trilhas existentes e
   está fora do escopo desta etapa.

---

### Task 1: Validar o ambiente Terraform

Sem esta tarefa nenhuma das outras é verificável. Ela não altera código: instala o
binário pinado e prova empiricamente as três premissas do estudo de ferramental.

**Files:**
- Modify: `docs/research/iac-course.md` (seção nova ao final, com o resultado medido)

**Interfaces:**
- Consumes: nada.
- Produces: `terraform` 1.15.8 no PATH; a certeza de que `provider "docker" {}` sem
  `host` funciona no Windows e de que `plan` cabe no timeout de 30s do
  `ExecutorDeComandoReal`.

- [ ] **Step 1: Instalar o Terraform 1.15.8**

```powershell
$versao = "1.15.8"
$destino = "$env:LOCALAPPDATA\terraform"
New-Item -ItemType Directory -Force $destino | Out-Null
Invoke-WebRequest "https://releases.hashicorp.com/terraform/$versao/terraform_${versao}_windows_amd64.zip" -OutFile "$env:TEMP\terraform.zip"
Expand-Archive "$env:TEMP\terraform.zip" -DestinationPath $destino -Force
$atual = [Environment]::GetEnvironmentVariable("Path", "User")
if ($atual -notlike "*$destino*") {
  [Environment]::SetEnvironmentVariable("Path", "$atual;$destino", "User")
}
$env:Path = "$env:Path;$destino"
terraform version
```

Esperado: `Terraform v1.15.8`.

- [ ] **Step 2: Provar o provider Docker sem `host` no Windows**

```powershell
$lab = "$env:TEMP\validacao-iac"
Remove-Item -Recurse -Force $lab -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $lab | Out-Null
@'
terraform {
  required_version = "= 1.15.8"
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 4.5"
    }
  }
}

provider "docker" {}

resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name  = "validacao-iac-web"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = 8070
  }
}
'@ | Out-File -Encoding utf8 "$lab\main.tf"
terraform -chdir=$lab init
terraform -chdir=$lab apply -auto-approve
```

Esperado: `Apply complete! Resources: 2 added, 0 changed, 0 destroyed.` e
`docker ps` mostrando `validacao-iac-web`.

- [ ] **Step 3: Provar idempotência e medir a duração do `plan`**

```powershell
Measure-Command { terraform -chdir=$lab plan -detailed-exitcode -input=false -no-color -lock=false }
$LASTEXITCODE
```

Esperado: `$LASTEXITCODE` igual a **0** (nenhuma mudança pendente) e `TotalSeconds` bem
abaixo de 30. Se passar de 30s, o `TIMEOUT_SEGUNDOS` do `ExecutorDeComandoReal` precisa
subir e isso vira uma tarefa nova antes da Task 4.

- [ ] **Step 4: Provar `state show` e o formato do atributo**

```powershell
terraform -chdir=$lab state show -no-color docker_container.web
```

Esperado: saída com linhas `chave = valor`, incluindo `name = "validacao-iac-web"`. Anote
o formato exato: a Task 3 depende de `name` vir entre aspas e indentado.

- [ ] **Step 5: Limpar**

```powershell
terraform -chdir=$lab destroy -auto-approve
Remove-Item -Recurse -Force $lab
docker ps -a --filter name=validacao-iac-web
```

Esperado: a última linha não lista nenhum container.

- [ ] **Step 6: Registrar o resultado e commitar**

Acrescente ao final de `docs/research/iac-course.md`:

```markdown
## Validação executada em 2026-08-09

| Premissa | Resultado |
|---|---|
| `terraform version` | v1.15.8 |
| `provider "docker" {}` sem `host` no Windows | funcionou |
| `apply` de imagem e container | 2 added |
| segundo `plan -detailed-exitcode` | exit code 0 |
| duração do `plan` | PREENCHER com o TotalSeconds medido |
| formato de `state show` | PREENCHER com a linha `name = …` observada |
```

Substitua os dois `PREENCHER` pelos valores reais antes de commitar — um plano com
placeholder committado é o defeito que esta etapa inteira existe para evitar.

```bash
git add docs/research/iac-course.md
git commit -m "docs: registra a validação do ambiente Terraform da Trilha IaC"
```

---

### Task 2: Frontmatter `terraform` e `diretorioTerraform`

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/Cenario.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Test: `backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java`

**Interfaces:**
- Consumes: nada das tarefas anteriores.
- Produces: `Cenario.terraform()` retorna `boolean`; `Cenario.diretorioTerraform()`
  retorna `String` — `"."` quando `terraform: true` e o campo foi omitido, `null` quando
  `terraform` é falso. As Tasks 3 e 4 usam esse `String` como caminho relativo.

- [ ] **Step 1: Escrever os testes que falham**

Acrescente ao final de `LeitorDeCenarioTest`, antes da última chave:

```java
    @Test
    void cenarioSemTerraformNaoDeclaraDiretorio() throws Exception {
        escreverCenarioCompleto();

        Cenario cenario = new LeitorDeCenario().ler(diretorio);

        assertFalse(cenario.terraform());
        assertNull(cenario.diretorioTerraform());
    }

    @Test
    void terraformSemDiretorioUsaARaizDoWorkspace() throws Exception {
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: iac/01-primeiro-apply
                titulo: Primeiro apply
                dificuldade: guiado
                terraform: true
                ---
                # Primeiro apply
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_rodando
                    nome: mirante-web
                """);

        Cenario cenario = new LeitorDeCenario().ler(diretorio);

        assertTrue(cenario.terraform());
        assertEquals(".", cenario.diretorioTerraform());
    }

    @Test
    void diretorioTerraformExigeTerraformLigado() throws Exception {
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: iac/01-primeiro-apply
                titulo: Primeiro apply
                dificuldade: guiado
                diretorioTerraform: infra
                ---
                # Primeiro apply
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_rodando
                    nome: mirante-web
                """);

        var erro = assertThrows(
                IllegalArgumentException.class, () -> new LeitorDeCenario().ler(diretorio));

        assertTrue(erro.getMessage().contains("terraform: true"));
    }

    @Test
    void diretorioTerraformNaoPodeEscaparDoWorkspace() throws Exception {
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: iac/01-primeiro-apply
                titulo: Primeiro apply
                dificuldade: guiado
                terraform: true
                diretorioTerraform: ../fora
                ---
                # Primeiro apply
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_rodando
                    nome: mirante-web
                """);

        var erro = assertThrows(
                IllegalArgumentException.class, () -> new LeitorDeCenario().ler(diretorio));

        assertTrue(erro.getMessage().contains("relativo"));
    }
```

- [ ] **Step 2: Rodar os testes e ver falhar**

Run: `cd backend && ./mvnw test -Dtest=LeitorDeCenarioTest`
Expected: FAIL — compilação quebra em `cenario.terraform()`, que ainda não existe.

- [ ] **Step 3: Acrescentar os dois componentes ao `Cenario`**

Em `Cenario.java`, acrescente os dois componentes ao final da lista do record:

```java
public record Cenario(
        String id,
        String titulo,
        Dificuldade dificuldade,
        List<String> containers,
        String markdown,
        Path diretorio,
        List<Assercao> asercoes,
        String projetoCompose,
        List<String> volumes,
        String contextoKubernetes,
        String namespaceKubernetes,
        String manifestosIniciais,
        boolean ministack,
        boolean infraestruturaRealAws,
        String inicializacaoAws,
        boolean terraform,
        String diretorioTerraform) {
```

Atualize os quatro construtores de compatibilidade existentes para delegarem com
`, false, null` no final, e acrescente um quinto para a assinatura anterior à Trilha IaC:

```java
    /** Compatibilidade para Cenários anteriores à Trilha IaC. */
    public Cenario(String id, String titulo, Dificuldade dificuldade, List<String> containers,
                   String markdown, Path diretorio, List<Assercao> asercoes, String projetoCompose,
                   List<String> volumes, String contextoKubernetes, String namespaceKubernetes,
                   String manifestosIniciais, boolean ministack, boolean infraestruturaRealAws,
                   String inicializacaoAws) {
        this(id, titulo, dificuldade, containers, markdown, diretorio, asercoes, projetoCompose,
                volumes, contextoKubernetes, namespaceKubernetes, manifestosIniciais,
                ministack, infraestruturaRealAws, inicializacaoAws, false, null);
    }
```

Os quatro construtores antigos passam a delegar para esse novo, mantendo `false, null`
apenas neste ponto. Exemplo do primeiro:

```java
    /** Cenário sem projeto Compose e sem volumes — a maioria. */
    public Cenario(String id, String titulo, Dificuldade dificuldade, List<String> containers,
                   String markdown, Path diretorio, List<Assercao> asercoes) {
        this(id, titulo, dificuldade, containers, markdown, diretorio, asercoes,
                null, List.of(), null, null, null, false, false, null);
    }
```

Esse já delega para o construtor de 15 argumentos, que agora é o de compatibilidade —
nenhuma outra mudança é necessária nos quatro.

- [ ] **Step 4: Ler e validar o frontmatter no `LeitorDeCenario`**

Em `ler(...)`, depois do bloco de validação AWS e antes de `lerAsercoes`:

```java
        boolean terraform = booleanoOpcional(meta, "terraform");
        String diretorioTerraform = textoOpcional(meta, "diretorioTerraform");
        validarMetadadosTerraform(terraform, diretorioTerraform, diretorioDoCenario);
        if (terraform && diretorioTerraform == null) {
            diretorioTerraform = ".";
        }
```

Acrescente os dois argumentos ao final da chamada `new Cenario(...)`:

```java
                inicializacaoAws,
                terraform,
                diretorioTerraform);
```

E o método de validação, ao lado de `validarMetadadosAws`:

```java
    /**
     * O diretório do Terraform é relativo ao diretório de trabalho e nunca escapa dele.
     * A checagem é textual porque o caminho vem do conteúdo, e um `Path` absoluto no
     * Windows pode não parecer absoluto para o Java quando começa só com barra.
     */
    private void validarMetadadosTerraform(
            boolean terraform, String diretorioTerraform, Path diretorio) {
        if (!terraform && diretorioTerraform != null) {
            throw new IllegalArgumentException(
                    "diretorioTerraform exige terraform: true em " + diretorio);
        }
        if (diretorioTerraform == null) {
            return;
        }
        boolean absoluto = diretorioTerraform.startsWith("/")
                || diretorioTerraform.startsWith("\\")
                || diretorioTerraform.matches("(?i)^[a-z]:.*");
        boolean escapa = Path.of(diretorioTerraform).normalize().startsWith("..");
        if (absoluto || escapa) {
            throw new IllegalArgumentException(
                    "diretorioTerraform deve ser relativo ao workspace e não pode escapar dele em "
                    + diretorio);
        }
    }
```

- [ ] **Step 5: Rodar os testes e ver passar**

Run: `cd backend && ./mvnw test -Dtest=LeitorDeCenarioTest`
Expected: PASS, incluindo os quatro testes novos.

- [ ] **Step 6: Commitar**

```bash
git add backend/src/main/java/dev/learninginfra/conteudo/Cenario.java backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java
git commit -m "feat: Cenário declara terraform e diretorioTerraform"
```

---

### Task 3: Asserção `terraform_estado`

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/Assercao.java`
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Test: `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java`
- Test: `backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java`

**Interfaces:**
- Consumes: `Cenario.diretorioTerraform()` da Task 2.
- Produces: `Assercao.TerraformEstado(String diretorio, String endereco, String atributo,
  String esperado, String descricao)`; o método privado
  `MotorDeVerificacao.diretorioDoTerraform(String relativo)` que devolve um `Path`
  absoluto confinado ou `null`, reusado pela Task 4; o construtor
  `MotorDeVerificacao(ExecutorDeComando, Duration, String diretorioDeTrabalho)`.

- [ ] **Step 1: Escrever os testes que falham**

Em `MotorDeVerificacaoTest`, acrescente o helper e os testes. O helper existente
`motorQueResponde` não serve porque o Motor agora precisa de um diretório de trabalho:

```java
    private MotorDeVerificacao motorComTrabalho(String stdout, int codigo, String trabalho) {
        ExecutorDeComando falso = comando -> new SaidaDeComando(codigo, stdout, "");
        return new MotorDeVerificacao(falso, java.time.Duration.ofSeconds(1), trabalho);
    }

    private static final String ESTADO_DO_CONTAINER = """
            # docker_container.web:
            resource "docker_container" "web" {
                image = "sha256:abc"
                name  = "mirante-web"
                rm    = false
            }
            """;

    @Test
    void terraformEstadoPassaQuandoOAtributoConfere() {
        var resultado = motorComTrabalho(ESTADO_DO_CONTAINER, 0, "../work")
                .verificar(List.of(new Assercao.TerraformEstado(
                        ".", "docker_container.web", "name", "mirante-web",
                        "o container está sob gestão do Terraform")));

        assertTrue(resultado.concluido());
    }

    @Test
    void terraformEstadoFalhaDizendoOValorObservado() {
        var resultado = motorComTrabalho(ESTADO_DO_CONTAINER, 0, "../work")
                .verificar(List.of(new Assercao.TerraformEstado(
                        ".", "docker_container.web", "name", "outro-nome",
                        "o container está sob gestão do Terraform")));

        assertFalse(resultado.concluido());
        assertEquals("`name` no state é `mirante-web`",
                resultado.asercoes().getFirst().detalhe());
    }

    @Test
    void terraformEstadoFalhaQuandoORecursoNaoNasceuDoCodigo() {
        var resultado = motorComTrabalho("", 1, "../work")
                .verificar(List.of(new Assercao.TerraformEstado(
                        ".", "docker_container.web", null, null,
                        "o container está sob gestão do Terraform")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("não nasceu do código"));
    }

    @Test
    void terraformEstadoSemAtributoSoExigeQueOEnderecoExista() {
        var resultado = motorComTrabalho(ESTADO_DO_CONTAINER, 0, "../work")
                .verificar(List.of(new Assercao.TerraformEstado(
                        ".", "docker_container.web", null, null,
                        "o container está sob gestão do Terraform")));

        assertTrue(resultado.concluido());
    }

    @Test
    void terraformEstadoRecusaDiretorioQueEscapaDoTrabalho() {
        var resultado = motorComTrabalho(ESTADO_DO_CONTAINER, 0, "../work")
                .verificar(List.of(new Assercao.TerraformEstado(
                        "../..", "docker_container.web", null, null,
                        "o container está sob gestão do Terraform")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("fora do diretório"));
    }
```

Em `LeitorDeCenarioTest`, acrescente:

```java
    @Test
    void montaAsercaoDeEstadoDoTerraformComODiretorioDoFrontmatter() throws Exception {
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: iac/01-primeiro-apply
                titulo: Primeiro apply
                dificuldade: guiado
                terraform: true
                diretorioTerraform: infra
                ---
                # Primeiro apply
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: terraform_estado
                    endereco: docker_container.web
                    atributo: name
                    esperado: mirante-web
                    descricao: o container está sob gestão do Terraform
                """);

        Assercao asercao = new LeitorDeCenario().ler(diretorio).asercoes().getFirst();

        assertInstanceOf(Assercao.TerraformEstado.class, asercao);
        var estado = (Assercao.TerraformEstado) asercao;
        assertEquals("infra", estado.diretorio());
        assertEquals("docker_container.web", estado.endereco());
        assertEquals("name", estado.atributo());
        assertEquals("mirante-web", estado.esperado());
    }

    @Test
    void asercaoDeTerraformExigeTerraformLigado() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: terraform_estado
                    endereco: docker_container.web
                    descricao: o container está sob gestão do Terraform
                """);

        var erro = assertThrows(
                IllegalArgumentException.class, () -> new LeitorDeCenario().ler(diretorio));

        assertTrue(erro.getMessage().contains("terraform: true"));
    }
```

- [ ] **Step 2: Rodar os testes e ver falhar**

Run: `cd backend && ./mvnw test -Dtest=MotorDeVerificacaoTest+LeitorDeCenarioTest`
Expected: FAIL — `Assercao.TerraformEstado` não existe.

- [ ] **Step 3: Acrescentar o record ao `Assercao`**

Ao final de `Assercao.java`, depois de `AwsConsulta`:

```java
    /**
     * Um endereço está no state do Terraform e, quando {@code atributo} vem preenchido,
     * com o valor esperado. Prova que o recurso nasceu do código, e não de um comando
     * digitado à mão.
     *
     * <p>O {@code diretorio} é relativo ao diretório de trabalho e chega injetado pelo
     * frontmatter do Cenário; o {@link MotorDeVerificacao} resolve e confina o caminho.
     * O YAML do Cenário não escolhe onde o Terraform roda.
     */
    record TerraformEstado(
            String diretorio,
            String endereco,
            String atributo,
            String esperado,
            String descricao) implements Assercao {
    }
```

- [ ] **Step 4: Ensinar o `MotorDeVerificacao` a resolver o diretório**

Acrescente o campo, mude o construtor autowired e acrescente o de teste:

```java
    private final Path raizDeTrabalho;
```

```java
    @Autowired
    public MotorDeVerificacao(
            ExecutorDeComando executor,
            @Value("${learninginfra.diretorio-de-trabalho}") String diretorioDeTrabalho) {
        this(executor, ESPERA_PADRAO, diretorioDeTrabalho);
    }

    /** Só para teste: o dublê simples, sem interesse em espera nem em Terraform. */
    MotorDeVerificacao(ExecutorDeComando executor) {
        this(executor, ESPERA_PADRAO, "../work");
    }

    /** Só para teste: permite uma espera curta sem deixar a suíte lenta. */
    MotorDeVerificacao(ExecutorDeComando executor, Duration espera) {
        this(executor, espera, "../work");
    }

    /** Só para teste: espera curta e um diretório de trabalho controlado. */
    MotorDeVerificacao(ExecutorDeComando executor, Duration espera, String diretorioDeTrabalho) {
        this.executor = executor;
        this.espera = espera;
        this.http = HttpClient.newBuilder().connectTimeout(espera).build();
        this.raizDeTrabalho = Path.of(diretorioDeTrabalho);
    }
```

Acrescente os imports `org.springframework.beans.factory.annotation.Value` e
`java.nio.file.Path`.

**Os três construtores de teste precisam existir.** O de um argumento é usado por
`motorQueResponde` e por outras nove chamadas diretas no `MotorDeVerificacaoTest`; o de
dois é usado na linha 190, no teste do serviço lento. Remover qualquer um deles quebra a
compilação da suíte. Só o construtor público muda de assinatura, de
`(ExecutorDeComando)` para `(ExecutorDeComando, String)`.

Acrescente o helper de confinamento:

```java
    /**
     * Confina o Terraform ao diretório de trabalho. O Cenário declara um caminho
     * relativo; se ele escapar, a Verificação reprova em vez de rodar `terraform` num
     * diretório arbitrário da máquina do leitor.
     */
    private Path diretorioDoTerraform(String relativo) {
        Path raiz = raizDeTrabalho.toAbsolutePath().normalize();
        Path alvo = raiz.resolve(relativo).normalize();
        return alvo.startsWith(raiz) ? alvo : null;
    }
```

- [ ] **Step 5: Tratar o caso novo no `switch`**

Acrescente ao `switch` de `avaliar`, depois de `AwsConsulta`:

```java
            case Assercao.TerraformEstado a -> avaliarEstadoTerraform(a);
```

E o avaliador:

```java
    private ResultadoDeAsercao avaliarEstadoTerraform(Assercao.TerraformEstado a) {
        Path diretorio = diretorioDoTerraform(a.diretorio());
        if (diretorio == null) {
            return ResultadoDeAsercao.reprovada(a,
                    "o Cenário aponta para fora do diretório de trabalho");
        }
        SaidaDeComando saida = executor.executar(List.of(
                "terraform", "-chdir=" + diretorio, "state", "show", "-no-color", a.endereco()));
        if (!saida.sucesso()) {
            return ResultadoDeAsercao.reprovada(a,
                    "`" + a.endereco() + "` não está no state — o recurso não nasceu do código");
        }
        if (a.atributo() == null) {
            return ResultadoDeAsercao.aprovada(a);
        }
        String observado = valorNoEstado(saida.stdout(), a.atributo());
        if (observado == null) {
            return ResultadoDeAsercao.reprovada(a,
                    "o atributo `" + a.atributo() + "` não aparece no state de `"
                    + a.endereco() + "`");
        }
        return observado.equals(a.esperado())
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a,
                        "`" + a.atributo() + "` no state é `" + observado + "`");
    }

    /**
     * O `state show` imprime `chave = valor` indentado, com aspas em texto. Interessa a
     * primeira ocorrência: blocos aninhados repetem nomes comuns como `name`.
     */
    private String valorNoEstado(String saida, String atributo) {
        java.util.regex.Matcher achado = java.util.regex.Pattern.compile(
                        "^\\s*" + java.util.regex.Pattern.quote(atributo) + "\\s*=\\s*(.+?)\\s*$",
                        java.util.regex.Pattern.MULTILINE)
                .matcher(saida);
        if (!achado.find()) {
            return null;
        }
        String valor = achado.group(1);
        return valor.length() >= 2 && valor.startsWith("\"") && valor.endsWith("\"")
                ? valor.substring(1, valor.length() - 1)
                : valor;
    }
```

- [ ] **Step 6: Montar a Asserção no `LeitorDeCenario`**

Passe `diretorioTerraform` adiante. Mude a assinatura de `lerAsercoes` e de
`montarAsercao` para receberem um `String diretorioTerraform` a mais, e a chamada em
`ler(...)`:

```java
        List<Assercao> asercoes = lerAsercoes(
                diretorioDoCenario.resolve("verificacao.yaml"),
                contextoKubernetes,
                namespaceKubernetes,
                ministack,
                diretorioTerraform);
```

No `switch` de `montarAsercao`, antes do `default`:

```java
            case "terraform_estado" -> {
                exigirTerraform(diretorioTerraform, tipo, arquivo);
                yield new Assercao.TerraformEstado(
                        diretorioTerraform,
                        exigirTexto(item, "endereco"),
                        textoOpcional(item, "atributo"),
                        textoOpcional(item, "esperado"),
                        exigirTexto(item, "descricao"));
            }
```

E o guarda, ao lado de `exigirMinistack`:

```java
    private void exigirTerraform(String diretorioTerraform, String tipo, Path arquivo) {
        if (diretorioTerraform == null) {
            throw new IllegalArgumentException(
                    "a Asserção " + tipo + " exige terraform: true em " + arquivo);
        }
    }
```

- [ ] **Step 7: Rodar os testes e ver passar**

Run: `cd backend && ./mvnw test -Dtest=MotorDeVerificacaoTest+LeitorDeCenarioTest`
Expected: PASS.

- [ ] **Step 8: Commitar**

```bash
git add backend/src/main/java/dev/learninginfra/verificacao/Assercao.java backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java
git commit -m "feat: Asserção terraform_estado prova que o recurso nasceu do código"
```

---

### Task 4: Asserção `terraform_plano_limpo`

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/Assercao.java`
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Test: `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java`

**Interfaces:**
- Consumes: `diretorioDoTerraform(String)` e o construtor de três argumentos, ambos da
  Task 3.
- Produces: `Assercao.TerraformPlanoLimpo(String diretorio, String descricao)`.

- [ ] **Step 1: Escrever os testes que falham**

Em `MotorDeVerificacaoTest`:

```java
    @Test
    void planoLimpoPassaQuandoOTerraformNaoTemMudancaPendente() {
        var resultado = motorComTrabalho("No changes.", 0, "../work")
                .verificar(List.of(new Assercao.TerraformPlanoLimpo(
                        ".", "o código descreve a infraestrutura que está no ar")));

        assertTrue(resultado.concluido());
    }

    @Test
    void planoComMudancaPendenteFalhaFalandoEmDivergencia() {
        var resultado = motorComTrabalho("Plan: 1 to add, 0 to change, 0 to destroy.", 2, "../work")
                .verificar(List.of(new Assercao.TerraformPlanoLimpo(
                        ".", "o código descreve a infraestrutura que está no ar")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("divergem"));
    }

    @Test
    void planoQueNemRodaOrientaARodarInit() {
        var resultado = motorComTrabalho("", 1, "../work")
                .verificar(List.of(new Assercao.TerraformPlanoLimpo(
                        ".", "o código descreve a infraestrutura que está no ar")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("terraform init"));
    }
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw test -Dtest=MotorDeVerificacaoTest`
Expected: FAIL — `Assercao.TerraformPlanoLimpo` não existe.

- [ ] **Step 3: Acrescentar o record**

Ao final de `Assercao.java`, depois de `TerraformEstado`:

```java
    /**
     * O `plan` não encontra nenhuma mudança pendente. É a Asserção que distingue uma
     * infraestrutura descrita por código de uma infraestrutura construída à mão: prova
     * idempotência e ausência de drift numa afirmação só.
     */
    record TerraformPlanoLimpo(String diretorio, String descricao) implements Assercao {
    }
```

- [ ] **Step 4: Tratar o caso no `switch` e implementar o avaliador**

No `switch` de `avaliar`, depois de `TerraformEstado`:

```java
            case Assercao.TerraformPlanoLimpo a -> avaliarPlanoTerraform(a);
```

```java
    /**
     * O `-detailed-exitcode` separa três desfechos que um booleano confundiria: 0 é
     * convergido, 2 é divergente e qualquer outro é falha de execução. O `-lock=false`
     * evita reprovar por causa de um lock esquecido: planejar aqui é leitura, não
     * mutação.
     */
    private ResultadoDeAsercao avaliarPlanoTerraform(Assercao.TerraformPlanoLimpo a) {
        Path diretorio = diretorioDoTerraform(a.diretorio());
        if (diretorio == null) {
            return ResultadoDeAsercao.reprovada(a,
                    "o Cenário aponta para fora do diretório de trabalho");
        }
        SaidaDeComando saida = executor.executar(List.of(
                "terraform", "-chdir=" + diretorio, "plan",
                "-detailed-exitcode", "-input=false", "-no-color", "-lock=false"));
        return switch (saida.codigoDeSaida()) {
            case 0 -> ResultadoDeAsercao.aprovada(a);
            case 2 -> ResultadoDeAsercao.reprovada(a,
                    "ainda há mudanças pendentes — código e realidade divergem");
            default -> ResultadoDeAsercao.reprovada(a,
                    "não consegui planejar — confirme que você rodou `terraform init` neste "
                    + "diretório: " + ultimoDetalhe(saida));
        };
    }
```

- [ ] **Step 5: Montar a Asserção no `LeitorDeCenario`**

No `switch` de `montarAsercao`, depois de `terraform_estado`:

```java
            case "terraform_plano_limpo" -> {
                exigirTerraform(diretorioTerraform, tipo, arquivo);
                yield new Assercao.TerraformPlanoLimpo(
                        diretorioTerraform,
                        exigirTexto(item, "descricao"));
            }
```

- [ ] **Step 6: Rodar a suíte inteira e ver passar**

Run: `cd backend && ./mvnw test`
Expected: PASS. Se o `switch` reclamar de não exaustivo, o caso novo não foi acrescentado
— é o erro proposital descrito no README, e não deve ser resolvido com `default`.

- [ ] **Step 7: Commitar**

```bash
git add backend/src/main/java/dev/learninginfra/verificacao/Assercao.java backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java
git commit -m "feat: Asserção terraform_plano_limpo prova idempotência e ausência de drift"
```

---

### Task 5: Guardrails executáveis sobre os `.tf` versionados

O guardrail da Trilha AWS hoje é um parágrafo de documentação. Aqui ele vira teste, para
que um `.tf` do conteúdo não possa apontar para uma conta AWS real nem para um cluster
que não seja o do Docker Desktop.

**Files:**
- Create: `backend/src/test/java/dev/learninginfra/conteudo/GuardrailsDeTerraformTest.java`

**Interfaces:**
- Consumes: nada do código de produção; lê `content/iac/**/*.tf` do disco.
- Produces: nada consumido por outras tarefas.

- [ ] **Step 1: Escrever o teste**

```java
package dev.learninginfra.conteudo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os guardrails da plataforma valem também para o código que o Cenário entrega pronto.
 * Um `.tf` versionado que alcance uma conta AWS real ou um cluster que não seja o do
 * Docker Desktop é defeito de conteúdo, não descuido do leitor.
 */
class GuardrailsDeTerraformTest {

    @Test
    void nenhumArquivoTerraformDoConteudoEscapaDoAmbienteLocal() {
        List<Path> arquivos = arquivosTerraform();

        assertThat(arquivos)
                .describedAs("o teste precisa de pelo menos um .tf para não passar em silêncio")
                .isNotEmpty();

        for (Path arquivo : arquivos) {
            String texto = ler(arquivo);

            assertThat(texto)
                    .describedAs("credencial de AWS real em %s", arquivo)
                    .doesNotContainPattern("(AKIA|ASIA)[0-9A-Z]{16}");

            if (texto.contains("provider \"aws\"")) {
                assertThat(texto)
                        .describedAs("provider AWS sem endpoint local em %s", arquivo)
                        .contains("endpoints")
                        .contains("http://127.0.0.1:4566")
                        .contains("skip_credentials_validation")
                        .contains("skip_metadata_api_check")
                        .contains("skip_requesting_account_id");
            }

            if (texto.contains("provider \"kubernetes\"")) {
                assertThat(texto)
                        .describedAs("provider Kubernetes sem contexto fixo em %s", arquivo)
                        .contains("config_context")
                        .contains("docker-desktop");
            }
        }
    }

    private List<Path> arquivosTerraform() {
        Path raiz = localizarConteudo().resolve("iac");
        if (!Files.isDirectory(raiz)) {
            return List.of();
        }
        try (Stream<Path> caminhos = Files.walk(raiz)) {
            return caminhos
                    .filter(Files::isRegularFile)
                    .filter(caminho -> caminho.getFileName().toString().endsWith(".tf"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui varrer " + raiz, e);
        }
    }

    private String ler(Path caminho) {
        try {
            return Files.readString(caminho);
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui ler " + caminho, e);
        }
    }

    private Path localizarConteudo() {
        Path naRaiz = Path.of("content");
        return Files.isDirectory(naRaiz) ? naRaiz : Path.of("..", "content");
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw test -Dtest=GuardrailsDeTerraformTest`
Expected: FAIL com "o teste precisa de pelo menos um .tf para não passar em silêncio" —
`content/iac/` ainda não existe. É a falha correta: ela prova que o teste não aprova por
vacuidade.

- [ ] **Step 3: Deixar o teste vermelho e seguir**

Não crie conteúdo aqui. A Task 6 cria o `content/iac/` e é ela quem deixa este teste
verde — é assim que se prova que o guardrail está de fato olhando para o conteúdo real.
Commite só o teste.

```bash
git add backend/src/test/java/dev/learninginfra/conteudo/GuardrailsDeTerraformTest.java
git commit -m "test: guardrail executável para os .tf da Trilha IaC"
```

---

### Task 6: Trilha `iac` e o Cenário 01

**Files:**
- Create: `content/iac/trilha.yaml`
- Create: `content/iac/01-o-sabado-em-que-a-mirante-subiu/cenario.md`
- Create: `content/iac/01-o-sabado-em-que-a-mirante-subiu/verificacao.yaml`
- Create: `content/iac/01-o-sabado-em-que-a-mirante-subiu/workspace/versions.tf`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: o frontmatter da Task 2 e as duas Asserções das Tasks 3 e 4.
- Produces: a Trilha `iac` no catálogo com um Cenário; deixa verde o teste da Task 5.

- [ ] **Step 1: Criar o manifesto da Trilha**

`content/iac/trilha.yaml` — sem `fundamentos`, que é a Etapa 2:

```yaml
id: iac
titulo: Infraestrutura como Código
```

- [ ] **Step 2: Criar o workspace do Cenário**

`content/iac/01-o-sabado-em-que-a-mirante-subiu/workspace/versions.tf`:

```hcl
terraform {
  required_version = "= 1.15.8"

  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 4.5"
    }
  }
}

provider "docker" {}
```

O `main.tf` não vem pronto: escrevê-lo é o exercício.

- [ ] **Step 3: Escrever o Cenário**

`content/iac/01-o-sabado-em-que-a-mirante-subiu/cenario.md`:

````markdown
---
id: iac/01-o-sabado-em-que-a-mirante-subiu
titulo: O sábado em que a Mirante subiu
dificuldade: guiado
terraform: true
containers: [mirante-web]
---
# O sábado em que a Mirante subiu

A Mirante vende um sistema de estoque para pequenos varejistas. O serviço está no ar
desde um sábado em que uma pessoa sozinha digitou uma sequência de comandos que funcionou.
Ninguém anotou quais foram. Meses depois, a empresa tem clientes, tem uma segunda pessoa
no time — e continua com uma infraestrutura que existe, mas que ninguém sabe reconstruir.

Você vai reconstruí-la. Não digitando os comandos de novo: **descrevendo o resultado** e
deixando o Terraform descobrir o caminho.

## O que já está no seu diretório

O `versions.tf` fixa a versão do Terraform e do provider. Isso não é burocracia: sem essa
fixação, a próxima pessoa que rodar `init` pode receber outra versão do provider e ver um
plano diferente do seu. Repare que `provider "docker" {}` não recebe nenhum argumento —
no Windows, o provider encontra sozinho o named pipe do Docker Desktop.

## Escreva o main.tf

Crie o arquivo `main.tf` ao lado do `versions.tf`, com exatamente estes dois recursos:

```hcl
resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name  = "mirante-web"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = 8070
  }
}
```

Duas coisas merecem atenção antes de você aplicar.

A primeira é `image = docker_image.web.image_id`. Você poderia ter escrito
`"nginx:1.27-alpine"` de novo ali. Ao referenciar o outro recurso, você não está evitando
repetição: está **declarando uma dependência**. O Terraform lê essa referência e conclui
sozinho que a imagem precisa existir antes do container. Ninguém escreveu a ordem.

A segunda é `keep_locally = true`. Sem isso, um `destroy` removeria a imagem do seu
disco, e o próximo `apply` faria download de novo.

## Aplique

```powershell
terraform init
terraform plan
terraform apply
```

O `init` baixa o provider e cria o `.terraform.lock.hcl` — o arquivo que registra o
digest exato do que foi baixado, para que a máquina do colega receba o mesmo binário.

O `plan` mostra o que vai acontecer. Leia a saída inteira antes de continuar: o `+` na
frente de cada recurso significa criação, e o número no rodapé é o contrato que o `apply`
vai cumprir.

O `apply` pede confirmação. Digite `yes`.

Confirme no navegador: <http://localhost:8070>.

## Aplique de novo

```powershell
terraform apply
```

Desta vez a resposta é `No changes. Your infrastructure matches the configuration.`

Esse é o comportamento que separa infraestrutura como código de um script. Um script que
cria container falha na segunda execução, porque o container já existe. O Terraform
compara o que você descreveu com o que ele sabe que existe, e não faz nada quando os dois
já coincidem. Isso se chama **idempotência**, e é o que permite rodar o mesmo código
todo dia sem medo.

## Olhe o que o Terraform passou a saber

```powershell
terraform state list
terraform state show docker_container.web
```

Esse é o **state**: o registro do que o Terraform criou e de qual recurso do mundo real
corresponde a cada endereço do seu código. Ele é a terceira peça da história — código,
state e mundo real — e vai ser o assunto de vários Cenários adiante.

## Verificação

A Verificação confere quatro coisas, e uma delas é sutil: além do container estar de pé e
respondendo, ela exige que `docker_container.web` esteja no state e que um `plan` não
encontre mudança pendente. Subir o mesmo container com `docker run` faria as duas
primeiras Asserções passarem e reprovaria nas outras duas — porque nesta Trilha o
resultado certo pelo caminho errado não conta.
````

- [ ] **Step 4: Escrever a Verificação**

`content/iac/01-o-sabado-em-que-a-mirante-subiu/verificacao.yaml`:

```yaml
asercoes:
  - tipo: container_rodando
    nome: mirante-web
  - tipo: http_responde
    url: http://localhost:8070
    status: 200
  - tipo: terraform_estado
    endereco: docker_container.web
    atributo: name
    esperado: mirante-web
    descricao: o container está sob gestão do Terraform, e não foi criado à mão
  - tipo: terraform_plano_limpo
    descricao: o código descreve exatamente a infraestrutura que está no ar
```

- [ ] **Step 5: Atualizar as contagens do catálogo**

Em `CatalogoRealTest`, no primeiro teste, acrescente o grupo e ajuste o total:

```java
        var iac = cenarios.stream()
                .filter(cenario -> cenario.id().startsWith("iac/"))
                .toList();
```

```java
        assertThat(cenarios).hasSize(44);
```

E, ao final do mesmo teste:

```java
        assertThat(iac).hasSize(1);
        assertThat(iac).allMatch(Cenario::terraform);
        assertThat(iac.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(4);
```

No segundo teste, a Trilha nova entra na lista ordenada por id e ainda não tem
Fundamentos. Renomeie o teste e ajuste a asserção:

```java
    @Test
    void carregaQuatroTrilhasComFundamentosPublicadosNasTresPrimeiras() {
```

```java
        assertThat(trilhas).extracting(trilha -> trilha.id())
                .containsExactly("aws", "docker", "iac", "kubernetes");
        assertThat(catalogo.buscar("iac").orElseThrow().fundamentos()).isNull();
```

O resto do segundo teste continua igual.

- [ ] **Step 6: Rodar a suíte inteira**

Run: `cd backend && ./mvnw test`
Expected: PASS, incluindo o `GuardrailsDeTerraformTest` que ficou vermelho na Task 5 —
agora ele encontra o `versions.tf` e não acha nenhuma violação.

- [ ] **Step 7: Provar o Cenário de ponta a ponta na aplicação real**

Suba o backend e o frontend, abra <http://localhost:5180>, entre na Trilha
Infraestrutura como Código e no Cenário 01. Então:

1. Clique em **Iniciar cenário**.
2. Clique em **Verificar antes de fazer qualquer coisa**. As quatro Asserções devem
   falhar. Se alguma passar, é bug — sobra de ambiente ou Cenário mal escrito.
3. Faça o exercício no seu terminal, no diretório de trabalho que a tela mostra.
4. Clique em **Verificar**. As quatro devem passar.
5. Rode `terraform destroy -auto-approve` no workspace Terraform para remover os recursos
   gerenciados e limpar o state. Em seguida, rode `docker run -d --name mirante-web -p
   8070:80 nginx:1.27-alpine` e clique em **Verificar** de novo. As duas primeiras
   Asserções passam e as duas de Terraform reprovam: como o state está vazio, esta prova
   demonstra uma infraestrutura construída manualmente, sem reaproveitar o registro de um
   recurso gerenciado. **Esta é a prova de que a Etapa 1 cumpriu seu objetivo.**
6. Restaure com `docker rm -f mirante-web && terraform apply`.

- [ ] **Step 8: Commitar**

```bash
git add content/iac backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: Trilha IaC com o Cenário 01 provando o caminho de verificação"
```

---

### Task 7: README

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: tudo das tarefas anteriores.
- Produces: nada consumido por código.

- [ ] **Step 1: Acrescentar o pré-requisito**

Na lista de "Antes de começar", depois da linha da AWS CLI:

```markdown
- **Terraform 1.15.8.** Necessário apenas para a Trilha IaC; confirme com
  `terraform version`. A versão é fixa porque o conteúdo declara
  `required_version = "= 1.15.8"` e o plano exibido muda entre versões do provider.
```

- [ ] **Step 2: Acrescentar a seção de preparo**

Depois de "### Preparando a Trilha AWS":

```markdown
### Preparando a Trilha IaC

Instale o Terraform 1.15.8 e confirme que ele está no PATH:

```powershell
terraform version
```

Nada mais precisa ser preparado. Os primeiros Cenários usam o provider
`kreuzwerker/docker`, que no Windows encontra sozinho o named pipe do Docker Desktop —
`provider "docker" {}` sem argumento nenhum funciona. Os Cenários finais reusam o cluster
`docker-desktop` da Trilha Kubernetes e o MiniStack da Trilha AWS, que você já preparou.

A Verificação nunca roda `apply`, `destroy` ou `init` por você: ela só observa o state e
pede um `plan`. Se uma Asserção de Terraform reclamar que não conseguiu planejar,
confirme que você rodou `terraform init` no diretório de trabalho.
```

- [ ] **Step 3: Atualizar a seção de portas**

Na seção "### Portas usadas pelos Cenários", acrescente ao final do primeiro parágrafo:

```markdown
A Trilha IaC usa o bloco **8070–8079**, começando pela **8070** no Cenário 01.
```

- [ ] **Step 4: Documentar o frontmatter e as Asserções**

Na seção "## Escrevendo um Cenário", depois do bloco que descreve `inicializacaoAws`:

````markdown
Um Cenário de IaC declara `terraform: true`. O campo opcional `diretorioTerraform`
aponta, **relativo ao `work/`**, onde ficam os arquivos `.tf`; omitido, vale a raiz:

```yaml
terraform: true
diretorioTerraform: infra
```

As duas Asserções tipadas recebem esse diretório do frontmatter e nunca o repetem:

```yaml
- tipo: terraform_estado
  endereco: docker_container.web
  atributo: name
  esperado: mirante-web
  descricao: o container está sob gestão do Terraform
- tipo: terraform_plano_limpo
  descricao: o código descreve a infraestrutura que está no ar
```

`terraform_estado` roda `terraform state show` e prova que o recurso nasceu do código;
sem `atributo` e `esperado`, ela apenas exige que o endereço exista no state.
`terraform_plano_limpo` roda `terraform plan -detailed-exitcode` e prova idempotência e
ausência de drift. As duas juntas impedem que a Verificação aprove uma infraestrutura
certa construída pelo caminho errado.
````

Acrescente também os dois tipos à lista de Asserções disponíveis, no parágrafo que hoje
termina em `aws_consulta`.

- [ ] **Step 5: Registrar a Trilha na seção de Fundamentos**

Na seção "## Fundamentos e evolução das Trilhas", troque "As três Trilhas oferecem" por
"Três das quatro Trilhas oferecem" e acrescente ao final da seção:

```markdown
A quarta Trilha, **Infraestrutura como Código**, está em construção. O estudo de
ferramental está em [`docs/research/iac-course.md`](docs/research/iac-course.md) e o
desenho completo — Fundamentos e 18 Cenários em quatro atos — em
[`docs/superpowers/specs/2026-08-09-trilha-iac-design.md`](docs/superpowers/specs/2026-08-09-trilha-iac-design.md).
Esta etapa entregou a plataforma de verificação e o Cenário 01.
```

- [ ] **Step 6: Verificar e commitar**

Run: `cd backend && ./mvnw test`
Expected: PASS — o README não é testado, mas a suíte confirma que nada quebrou.

```bash
git add README.md
git commit -m "docs: README cobre a Trilha IaC, o frontmatter terraform e as duas Asserções"
```

---

## Definição de pronto

- `cd backend && ./mvnw test` passa, com `GuardrailsDeTerraformTest` encontrando pelo
  menos um `.tf`.
- `cd frontend && npm run build` passa.
- O Cenário 01 aparece no catálogo, inicia, reprova antes do exercício e aprova depois.
- O passo 7.5 da Task 6 foi executado com `terraform destroy -auto-approve` antes do
  `docker run`: partindo de state vazio, o container equivalente reprova nas duas
  Asserções de Terraform.
- `docs/research/iac-course.md` tem a seção de validação com os números medidos, sem
  nenhum `PREENCHER`.
