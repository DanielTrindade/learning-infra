# Plano — Trilha Linux, Etapa 1: Plataforma

> Data: 2026-08-25
>
> Design de origem: [`docs/superpowers/specs/2026-08-21-trilha-linux-design.md`](../specs/2026-08-21-trilha-linux-design.md)
>
> Estado: em execução

A Etapa 1 entrega a plataforma que a Trilha Linux inteira vai usar, e prova o caminho de
ponta a ponta com o Cenário 08 — o que exercita os dois tipos de Asserção novos de uma
vez. As etapas seguintes (Fundamentos e os Atos I a IV) dependem dela.

## Decisões de implementação que divergem do spec

1. **O alvo `linux` do `scripts-checar-conteudo.mjs` e os três `visual` ficam para a
   Etapa 2.** O script lê `fundamentos.md` e `questionario.yaml` já na primeira linha e
   falharia sem eles; ele é genérico e passa a funcionar sozinho quando os Fundamentos
   existirem. Os três `visual` novos (`fronteiras-kernel`, `arvore-de-processos`,
   `permissao-octal`) pertencem aos diagramas dos Fundamentos. Mesmo desvio que a Etapa 1
   da Trilha IaC fez com o alvo `iac`.

2. **O `Dockerfile` faz mais do que o spec mostra.** O spec publica só a receita do
   `compose.yaml`, mas a imagem `ubuntu:26.04` não traz systemd — só `libsystemd0` — e
   não tem `/sbin/init`. Medido nesta máquina: a imagem base precisa de
   `apt-get install systemd`, e o processo 1 é `/usr/lib/systemd/systemd`. A imagem
   também instala o ferramental que a grade inteira usa (`procps`, `iproute2`, `sudo`,
   `curl`, `python3`) para o `Dockerfile` não mudar a cada etapa e invalidar o cache de
   camadas.

3. **O primeiro `up --build` leva mais que os 68 s do spec.** Os 68 s foram medidos com
   uma imagem mínima; com o ferramental completo acima o primeiro build mediu **~124 s**
   a frio e **~7 s** com cache. O README avisa "alguns minutos na primeira vez", sem
   prometer um número que não corresponde ao `Dockerfile` entregue.

4. **Códigos de saída do `systemctl`, medidos.** `is-active --quiet`: `0` ativo, `3`
   inativo, `4` unit inexistente. `is-enabled --quiet`: `0` habilitado, `1` desabilitado,
   `4` inexistente. A Asserção trata `0` como o único positivo — mais estrito que o
   `contem: active`, que aprovaria `inactive` por substring.

## Fatos medidos que fixam o substrato

| Peça | Valor medido em 2026-08-25 |
|---|---|
| Digest amd64 de `ubuntu:26.04` | `sha256:889d056d5c6c0bfb55789ff3710681d68e50713cb562d2196dc07110599c7a6f` |
| systemd na imagem | `259 (259.5-0ubuntu3.4)` |
| `is-system-running` com a receita | `running`, nenhuma unit falha |
| `stat -c "%a %U %G"` | `2770 root ana` |
| `docker exec -it` → sessão interativa | já aprovado em 2026-08-21 (ver ADR 0001) |
| `docker compose down -v` | remove container e rede |

---

### Task 1: Registrar a pinagem do substrato

Sem código: registra na pesquisa o digest e a receita exata do `Dockerfile`, que são
fatos medidos novos e não estão escritos em lugar nenhum.

**Files:**
- Modify: `docs/research/proximas-trilhas.md`

**Interfaces:**
- Consumes: nada.
- Produces: o digest e a receita `Dockerfile` como fonte de verdade para as Tasks 6 e 7.

- [ ] **Step 1: Acrescentar a subseção de pinagem**

Ao final da seção "Trilha 1 da construção — Linux", depois do bloco de portas:

```markdown
### Pinagem medida em 2026-08-25

A imagem base é `ubuntu:26.04` pinada por digest, mas a base **não traz systemd** — só
`libsystemd0` — e não tem `/sbin/init`. O `Dockerfile` precisa instalar o systemd e
apontar o processo 1 para `/usr/lib/systemd/systemd`:

| Peça | Valor medido |
|---|---|
| Digest amd64 de `ubuntu:26.04` | `sha256:889d056d5c6c0bfb55789ff3710681d68e50713cb562d2196dc07110599c7a6f` |
| Pacotes instalados | `systemd procps iproute2 sudo curl python3` |
| Processo 1 | `/usr/lib/systemd/systemd` (não existe `/sbin/init`) |
| `is-system-running` | `running`, nenhuma unit falha |
| Primeiro `up --build` a frio | ~124 s; seguintes com cache, ~7 s |
```

- [ ] **Step 2: Commitar**

```bash
git add docs/research/proximas-trilhas.md
git commit -m "docs: registra a pinagem do substrato da Trilha Linux"
```

---

### Task 2: Campo `ordem` no `trilha.yaml`

Torna a posição declarada e obrigatória, como a
[ADR 0005](../../adr/0005-ordem-das-trilhas-e-recomendacao.md) decidiu. As quatro
`trilha.yaml` existentes mudam **no mesmo commit** — o catálogo inteiro deixa de carregar
sem isso.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/trilha/Trilha.java`
- Modify: `backend/src/main/java/dev/learninginfra/trilha/LeitorDeTrilha.java`
- Modify: `backend/src/main/java/dev/learninginfra/trilha/CatalogoDeTrilhas.java`
- Modify: `content/docker/trilha.yaml`, `content/kubernetes/trilha.yaml`,
  `content/aws/trilha.yaml`, `content/iac/trilha.yaml`
- Test: `backend/src/test/java/dev/learninginfra/trilha/CatalogoDeTrilhasTest.java`
- Test: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: nada.
- Produces: `Trilha.ordem()` (`int`); `LeitorDeTrilha` exige `ordem`; o comparator do
  `CatalogoDeTrilhas` vira `Comparator.comparingInt(Trilha::ordem).thenComparing(Trilha::id)`.

- [ ] **Step 1: Escrever os testes que falham**

Em `CatalogoDeTrilhasTest`, mude o helper e o primeiro teste para declarar ordem e
ordenar por ela, e acrescente o teste da obrigatoriedade:

```java
    @Test
    void listaTrilhasExplicitasEmOrdemECompoeSeusCenarios() throws Exception {
        escreverTrilha("kubernetes", "Kubernetes", 3);
        escreverTrilha("docker", "Docker", 2);
        escreverCenarioDocker();

        var catalogo = catalogo();

        assertThat(catalogo.listar()).extracting(Trilha::id)
                .containsExactly("docker", "kubernetes");
        ...
    }

    @Test
    void rejeitaManifestoSemOrdem() throws Exception {
        Path diretorio = raiz.resolve("content/docker");
        Files.createDirectories(diretorio);
        Files.writeString(diretorio.resolve("trilha.yaml"), "id: docker\ntitulo: Docker\n");

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> catalogo().listar()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ordem");
    }
```

E o helper passa a receber a ordem:

```java
    private void escreverTrilha(String id, String titulo, int ordem) throws Exception {
        Path diretorio = raiz.resolve("content").resolve(id);
        Files.createDirectories(diretorio);
        Files.writeString(diretorio.resolve("trilha.yaml"), """
                id: %s
                titulo: %s
                ordem: %d
                """.formatted(id, titulo, ordem));
    }
```

Os demais testes que usam `escreverTrilha` (só o primeiro usa) continuam com o helper.

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw test -Dtest=CatalogoDeTrilhasTest`
Expected: FAIL — o teste de ordem nova não compila (`escreverTrilha` com 3 argumentos).

- [ ] **Step 3: Acrescentar `ordem` a `Trilha` e `MetadadosDaTrilha`**

Em `Trilha.java`:

```java
public record Trilha(
        String id,
        String titulo,
        int ordem,
        Fundamentos fundamentos,
        List<Cenario> cenarios) {
```

Em `LeitorDeTrilha.java`, leia o campo e o exponha:

```java
            return new MetadadosDaTrilha(
                    id,
                    exigirTexto(dados, "titulo", arquivo),
                    exigirInteiro(dados, "ordem", arquivo),
                    lerFundamentos(dados, arquivo));
```

```java
    public record MetadadosDaTrilha(String id, String titulo, int ordem, Fundamentos fundamentos) {
    }
```

- [ ] **Step 4: Reordenar no `CatalogoDeTrilhas`**

Troque o comparator e passe `ordem` adiante em `montarTrilha`:

```java
                    .sorted(Comparator.comparingInt(Trilha::ordem).thenComparing(Trilha::id))
```

```java
        return new Trilha(
                metadados.id(), metadados.titulo(), metadados.ordem(),
                metadados.fundamentos(), cenarios);
```

- [ ] **Step 5: Declarar `ordem` nas quatro Trilhas existentes**

`docker` → `ordem: 2`, `kubernetes` → `ordem: 3`, `aws` → `ordem: 6`, `iac` → `ordem: 7`.

- [ ] **Step 6: Ajustar o `CatalogoRealTest`**

No segundo teste, a ordem alfabética deixa de valer e vira a ordem declarada:

```java
        assertThat(trilhas).extracting(trilha -> trilha.id())
                .containsExactly("docker", "kubernetes", "aws", "iac");
```

- [ ] **Step 7: Rodar a suíte e ver passar**

Run: `cd backend && ./mvnw test`
Expected: PASS.

- [ ] **Step 8: Commitar**

```bash
git add backend/src/main/java/dev/learninginfra/trilha content/docker/trilha.yaml content/kubernetes/trilha.yaml content/aws/trilha.yaml content/iac/trilha.yaml backend/src/test/java/dev/learninginfra/trilha/CatalogoDeTrilhasTest.java backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: Trilha declara ordem e o catálogo a respeita"
```

---

### Task 3: Frontmatter `containerLinux`

O `Cenario` passa a carregar o nome do container que hospeda a máquina Linux do Cenário.
A presença do campo habilita os dois tipos novos de Asserção; ele nunca é repetido no
`verificacao.yaml`, como contexto e namespace do Kubernetes, o endpoint do MiniStack e o
diretório do Terraform.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/Cenario.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Test: `backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java`

**Interfaces:**
- Consumes: nada.
- Produces: `Cenario.containerLinux()` (`String`, `null` quando ausente) e
  `Cenario.usaLinux()` (`boolean`).

- [ ] **Step 1: Escrever o teste que falha**

```java
    @Test
    void leOContainerLinuxDeclarado() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: linux/08-um-programa-vira-servico
                titulo: Um programa vira serviço
                dificuldade: guiado
                projetoCompose: linux-08
                containerLinux: learning-infra-linux
                ---
                # corpo
                """);

        Cenario cenario = new LeitorDeCenario().ler(diretorio);

        assertEquals("learning-infra-linux", cenario.containerLinux());
        assertTrue(cenario.usaLinux());
    }
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw test -Dtest=LeitorDeCenarioTest`
Expected: FAIL — `cenario.containerLinux()` não existe.

- [ ] **Step 3: Acrescentar o campo ao `Cenario`**

Ao final da lista de componentes do record (depois de `diretorioTerraform`):

```java
        boolean terraform,
        String diretorioTerraform,
        String containerLinux) {
```

Atualize os cinco construtores de compatibilidade para delegarem com `null` ao final, e
acrescente:

```java
    public boolean usaLinux() {
        return containerLinux != null && !containerLinux.isBlank();
    }
```

- [ ] **Step 4: Ler o frontmatter**

Em `LeitorDeCenario.ler(...)`, antes de `lerAsercoes`:

```java
        String containerLinux = textoOpcional(meta, "containerLinux");
```

E passe-o como último argumento do `new Cenario(...)`.

- [ ] **Step 5: Rodar e ver passar**

Run: `cd backend && ./mvnw test -Dtest=LeitorDeCenarioTest`
Expected: PASS.

- [ ] **Step 6: Commitar**

```bash
git add backend/src/main/java/dev/learninginfra/conteudo/Cenario.java backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java
git commit -m "feat: Cenário declara containerLinux"
```

---

### Task 4: Asserção `servico_systemd`

Roda `systemctl is-active --quiet` e `systemctl is-enabled --quiet` dentro do container e
compara **códigos de saída** — `0` é o único positivo. Existe por causa da armadilha
medida: `is-active` de uma unit parada imprime `inactive`, e
`"inactive".contains("active")` é verdadeiro.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/Assercao.java`
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Test: `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java`
- Test: `backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java`

**Interfaces:**
- Consumes: `Cenario.containerLinux()` (Task 3).
- Produces: `Assercao.ServicoSystemd(String container, String nome, Boolean ativo,
  Boolean habilitado, String descricao)`.

- [ ] **Step 1: Escrever os testes que falham**

Em `MotorDeVerificacaoTest`:

```java
    @Test
    void servicoSystemdPassaQuandoAtivoEHabilitado() {
        List<List<String>> comandos = new java.util.ArrayList<>();
        ExecutorDeComando executor = comando -> {
            comandos.add(comando);
            return new SaidaDeComando(0, "", "");
        };

        var resultado = new MotorDeVerificacao(executor).verificar(List.of(
                new Assercao.ServicoSystemd(
                        "learning-infra-linux", "catalogo.service", true, true,
                        "o catálogo vira serviço")));

        assertTrue(resultado.concluido());
        assertEquals(List.of("docker", "exec", "learning-infra-linux",
                "systemctl", "is-active", "--quiet", "catalogo.service"), comandos.get(0));
        assertEquals(List.of("docker", "exec", "learning-infra-linux",
                "systemctl", "is-enabled", "--quiet", "catalogo.service"), comandos.get(1));
    }

    @Test
    void servicoSystemdReprovaServicoParado() {
        ExecutorDeComando executor = comando -> new SaidaDeComando(3, "inactive", "");

        var resultado = new MotorDeVerificacao(executor).verificar(List.of(
                new Assercao.ServicoSystemd(
                        "learning-infra-linux", "catalogo.service", true, null,
                        "o catálogo vira serviço")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("não está ativo"));
    }

    @Test
    void servicoSystemdNaoDeveEstarAtivoQuandoExigidoFalse() {
        ExecutorDeComando executor = comando -> new SaidaDeComando(0, "active", "");

        var resultado = new MotorDeVerificacao(executor).verificar(List.of(
                new Assercao.ServicoSystemd(
                        "learning-infra-linux", "catalogo.service", false, null,
                        "o serviço não pode subir sozinho")));

        assertFalse(resultado.concluido());
    }
```

Em `LeitorDeCenarioTest`:

```java
    @Test
    void montaAsercaoDeServicoSystemdComOContainerDoFrontmatter() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: linux/08-um-programa-vira-servico
                titulo: Um programa vira serviço
                dificuldade: guiado
                containerLinux: learning-infra-linux
                ---
                # corpo
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: servico_systemd
                    nome: catalogo.service
                    ativo: true
                    habilitado: true
                    descricao: o catálogo vira serviço
                """);

        var asercao = (Assercao.ServicoSystemd) new LeitorDeCenario().ler(diretorio).asercoes().getFirst();

        assertEquals("learning-infra-linux", asercao.container());
        assertEquals("catalogo.service", asercao.nome());
        assertTrue(asercao.ativo());
        assertTrue(asercao.habilitado());
    }

    @Test
    void asercaoDeServicoSystemdExigeContainerLinux() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: servico_systemd
                    nome: catalogo.service
                    ativo: true
                    descricao: o catálogo vira serviço
                """);

        var erro = assertThrows(
                IllegalArgumentException.class, () -> new LeitorDeCenario().ler(diretorio));

        assertTrue(erro.getMessage().contains("containerLinux"));
    }
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw test -Dtest=MotorDeVerificacaoTest+LeitorDeCenarioTest`
Expected: FAIL — `Assercao.ServicoSystemd` não existe.

- [ ] **Step 3: Acrescentar o record ao `Assercao`**

```java
    /**
     * Um serviço systemd está ativo e habilitado dentro do container Linux do Cenário.
     * Compara códigos de saída — 0 é o único positivo — porque `is-active` de uma unit
     * parada imprime `inactive`, e uma checagem por substring aprovaria o serviço parado.
     */
    record ServicoSystemd(
            String container,
            String nome,
            Boolean ativo,
            Boolean habilitado,
            String descricao) implements Assercao {
    }
```

- [ ] **Step 4: Tratar o caso no `switch` e implementar o avaliador**

No `switch` de `avaliar`:

```java
            case Assercao.ServicoSystemd a -> avaliarServicoSystemd(a);
```

```java
    private ResultadoDeAsercao avaliarServicoSystemd(Assercao.ServicoSystemd a) {
        if (a.ativo() != null) {
            boolean ativo = executor.executar(List.of(
                    "docker", "exec", a.container(),
                    "systemctl", "is-active", "--quiet", a.nome())).sucesso();
            if (ativo != a.ativo()) {
                return ResultadoDeAsercao.reprovada(a,
                        ativo
                                ? "o serviço `" + a.nome() + "` está ativo e não deveria"
                                : "o serviço `" + a.nome() + "` não está ativo");
            }
        }
        if (a.habilitado() != null) {
            boolean habilitado = executor.executar(List.of(
                    "docker", "exec", a.container(),
                    "systemctl", "is-enabled", "--quiet", a.nome())).sucesso();
            if (habilitado != a.habilitado()) {
                return ResultadoDeAsercao.reprovada(a,
                        habilitado
                                ? "o serviço `" + a.nome() + "` está habilitado e não deveria"
                                : "o serviço `" + a.nome() + "` não está habilitado");
            }
        }
        return ResultadoDeAsercao.aprovada(a);
    }
```

- [ ] **Step 5: Montar a Asserção no `LeitorDeCenario`**

Passe `containerLinux` adiante na assinatura de `lerAsercoes` e de `montarAsercao` (como
o `diretorioTerraform` já é passado). No `switch` de `montarAsercao`, antes do `default`:

```java
            case "servico_systemd" -> {
                exigirContainerLinux(containerLinux, tipo, arquivo);
                yield new Assercao.ServicoSystemd(
                        containerLinux,
                        exigirTexto(item, "nome"),
                        booleanoOpcionalNulo(item, "ativo"),
                        booleanoOpcionalNulo(item, "habilitado"),
                        exigirTexto(item, "descricao"));
            }
```

E o guarda, ao lado de `exigirTerraform`:

```java
    private void exigirContainerLinux(String containerLinux, String tipo, Path arquivo) {
        if (containerLinux == null || containerLinux.isBlank()) {
            throw new IllegalArgumentException(
                    "a Asserção " + tipo + " exige containerLinux em " + arquivo);
        }
    }
```

O leitor de booleano anulável, ao lado de `booleanoOpcional`:

```java
    private Boolean booleanoOpcionalNulo(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        return valor instanceof Boolean booleano ? booleano : null;
    }
```

- [ ] **Step 6: Rodar a suíte e ver passar**

Run: `cd backend && ./mvnw test`
Expected: PASS — o `switch` exaustivo exige o caso novo; não usar `default`.

- [ ] **Step 7: Commitar**

```bash
git add backend/src/main/java/dev/learninginfra/verificacao/Assercao.java backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java
git commit -m "feat: Asserção servico_systemd prova serviço ativo e habilitado"
```

---

### Task 5: Asserção `arquivo_linux`

Roda `stat -c "%a %U %G"` dentro do container e compara modo, dono e grupo campo a campo.
Campos nulos não são verificados, como `container_configuracao` já faz.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/Assercao.java`
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Test: `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java`
- Test: `backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java`

**Interfaces:**
- Consumes: `Cenario.containerLinux()` e o guarda `exigirContainerLinux` (Tasks 3 e 4).
- Produces: `Assercao.ArquivoLinux(String container, String caminho, String modo,
  String dono, String grupo, String descricao)`.

- [ ] **Step 1: Escrever os testes que falham**

Em `MotorDeVerificacaoTest`:

```java
    @Test
    void arquivoLinuxPassaQuandoOsCamposConferem() {
        List<List<String>> comandos = new java.util.ArrayList<>();
        ExecutorDeComando executor = comando -> {
            comandos.add(comando);
            return new SaidaDeComando(0, "2770 root ana\n", "");
        };

        var resultado = new MotorDeVerificacao(executor).verificar(List.of(
                new Assercao.ArquivoLinux(
                        "learning-infra-linux", "/srv/dados", "2770", "root", "ana",
                        "o diretório tem o dono e o modo certos")));

        assertTrue(resultado.concluido());
        assertEquals(List.of("docker", "exec", "learning-infra-linux",
                "stat", "-c", "%a %U %G", "/srv/dados"), comandos.getFirst());
    }

    @Test
    void arquivoLinuxReprovaModoErradoDizendoOObservado() {
        ExecutorDeComando executor = comando -> new SaidaDeComando(0, "755 root ana\n", "");

        var resultado = new MotorDeVerificacao(executor).verificar(List.of(
                new Assercao.ArquivoLinux(
                        "learning-infra-linux", "/srv/dados", "2770", null, null,
                        "o diretório tem o modo certo")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("755"));
    }

    @Test
    void arquivoLinuxReprovaCaminhoInexistente() {
        ExecutorDeComando executor = comando -> new SaidaDeComando(1, "", "No such file");

        var resultado = new MotorDeVerificacao(executor).verificar(List.of(
                new Assercao.ArquivoLinux(
                        "learning-infra-linux", "/nao/existe", "440", null, null,
                        "o arquivo existe")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("não existe"));
    }
```

Em `LeitorDeCenarioTest`:

```java
    @Test
    void montaAsercaoDeArquivoLinuxComCamposNulosNaoVerificados() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: linux/13-acesso-minimo
                titulo: Acesso mínimo
                dificuldade: autonomo
                containerLinux: learning-infra-linux
                ---
                # corpo
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: arquivo_linux
                    caminho: /etc/sudoers.d/plantao
                    modo: "440"
                    dono: root
                    descricao: a regra de sudo não é editável por quem ela beneficia
                """);

        var asercao = (Assercao.ArquivoLinux) new LeitorDeCenario().ler(diretorio).asercoes().getFirst();

        assertEquals("440", asercao.modo());
        assertEquals("root", asercao.dono());
        assertNull(asercao.grupo());
    }
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw test -Dtest=MotorDeVerificacaoTest+LeitorDeCenarioTest`
Expected: FAIL — `Assercao.ArquivoLinux` não existe.

- [ ] **Step 3: Acrescentar o record ao `Assercao`**

```java
    /**
     * Um arquivo ou diretório dentro do container Linux tem o modo, o dono e o grupo
     * esperados. Campos nulos não são verificados. A descrição vem do Cenário.
     */
    record ArquivoLinux(
            String container,
            String caminho,
            String modo,
            String dono,
            String grupo,
            String descricao) implements Assercao {
    }
```

- [ ] **Step 4: Tratar o caso no `switch` e implementar o avaliador**

```java
            case Assercao.ArquivoLinux a -> avaliarArquivoLinux(a);
```

```java
    private ResultadoDeAsercao avaliarArquivoLinux(Assercao.ArquivoLinux a) {
        SaidaDeComando saida = executor.executar(List.of(
                "docker", "exec", a.container(),
                "stat", "-c", "%a %U %G", a.caminho()));
        if (!saida.sucesso()) {
            return ResultadoDeAsercao.reprovada(a,
                    "o caminho `" + a.caminho() + "` não existe no container `"
                    + a.container() + "`");
        }
        String[] campos = saida.stdout().strip().split("\\s+");
        if (campos.length < 3) {
            return ResultadoDeAsercao.reprovada(a, "não consegui ler o `stat` de `" + a.caminho() + "`");
        }
        if (a.modo() != null && !campos[0].equals(a.modo())) {
            return ResultadoDeAsercao.reprovada(a, "o modo de `" + a.caminho() + "` é `" + campos[0] + "`");
        }
        if (a.dono() != null && !campos[1].equals(a.dono())) {
            return ResultadoDeAsercao.reprovada(a, "o dono de `" + a.caminho() + "` é `" + campos[1] + "`");
        }
        if (a.grupo() != null && !campos[2].equals(a.grupo())) {
            return ResultadoDeAsercao.reprovada(a, "o grupo de `" + a.caminho() + "` é `" + campos[2] + "`");
        }
        return ResultadoDeAsercao.aprovada(a);
    }
```

- [ ] **Step 5: Montar a Asserção no `LeitorDeCenario`**

```java
            case "arquivo_linux" -> {
                exigirContainerLinux(containerLinux, tipo, arquivo);
                yield new Assercao.ArquivoLinux(
                        containerLinux,
                        exigirTexto(item, "caminho"),
                        textoOpcional(item, "modo"),
                        textoOpcional(item, "dono"),
                        textoOpcional(item, "grupo"),
                        exigirTexto(item, "descricao"));
            }
```

- [ ] **Step 6: Rodar a suíte e ver passar**

Run: `cd backend && ./mvnw test`
Expected: PASS.

- [ ] **Step 7: Commitar**

```bash
git add backend/src/main/java/dev/learninginfra/verificacao/Assercao.java backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java
git commit -m "feat: Asserção arquivo_linux prova dono, grupo e modo"
```

---

### Task 6: Trilha `linux` e o Cenário 08

A prova de que o caminho inteiro funciona: a Trilha entra no catálogo na posição 1, e o
Cenário 08 sobe o systemd, vira um programa em serviço e passa nas duas Asserções novas
mais o `http_responde`.

**Files:**
- Create: `content/linux/trilha.yaml`
- Create: `content/linux/08-um-programa-vira-servico/cenario.md`
- Create: `content/linux/08-um-programa-vira-servico/verificacao.yaml`
- Create: `content/linux/08-um-programa-vira-servico/workspace/Dockerfile`
- Create: `content/linux/08-um-programa-vira-servico/workspace/compose.yaml`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: `containerLinux`, `servico_systemd`, `arquivo_linux` e `ordem` das Tasks
  2 a 5.
- Produces: a Trilha `linux` com um Cenário; o catálogo passa a começar por ela.

- [ ] **Step 1: Criar o manifesto da Trilha**

`content/linux/trilha.yaml` — sem `fundamentos`, que é a Etapa 2:

```yaml
id: linux
titulo: Linux
ordem: 1
```

- [ ] **Step 2: Criar o `Dockerfile` compartilhado**

`content/linux/08-um-programa-vira-servico/workspace/Dockerfile`:

```dockerfile
FROM ubuntu:26.04@sha256:889d056d5c6c0bfb55789ff3710681d68e50713cb562d2196dc07110599c7a6f

ENV LANG=C.UTF-8

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        systemd procps iproute2 sudo curl python3 \
    && systemctl mask tmp.mount \
    && rm -rf /var/lib/apt/lists/*

CMD ["/usr/lib/systemd/systemd"]
```

- [ ] **Step 3: Criar o `compose.yaml`**

`content/linux/08-um-programa-vira-servico/workspace/compose.yaml`:

```yaml
services:
  servidor:
    image: learning-infra-linux:1
    build: .
    container_name: learning-infra-linux
    hostname: aurora
    cgroup: host
    tmpfs:
      - /run
      - /run/lock
    volumes:
      - /sys/fs/cgroup:/sys/fs/cgroup:rw
    ports:
      - "8040:8040"
```

- [ ] **Step 4: Escrever o Cenário**

`content/linux/08-um-programa-vira-servico/cenario.md` — Guiado: entra no ambiente,
cria o conteúdo do catálogo, escreve a unit, `daemon-reload`, `enable --now`, e distingue
`enable` de `start`:

````markdown
---
id: linux/08-um-programa-vira-servico
titulo: Um programa vira serviço
dificuldade: guiado
projetoCompose: linux-08
containerLinux: learning-infra-linux
---
# Um programa vira serviço

Até aqui a Aurora tocou seus programas na mão: para servir o catálogo, alguém abria uma
sessão e deixava um processo rodando. Na primeira reinicialização, o catálogo sumia e
ninguém lembrava como subia de novo.

Você vai acabar com isso: transformar o programa em um **serviço** que o systemd mantém
no ar e sobe sozinho a cada boot.

## Entre na máquina

Clique em **Iniciar cenário**. Depois, no seu terminal:

```powershell
docker exec -it learning-infra-linux bash
```

Confirme que o systemd está saudável:

```bash
systemctl is-system-running
```

## O programa

O catálogo da Aurora é um site estático servido pelo Python, que já está instalado. Crie
o conteúdo e experimente o programa rodando na mão:

```bash
mkdir -p /srv/catalogo
echo '<h1>Aurora — catálogo</h1>' > /srv/catalogo/index.html
python3 -m http.server 8040 --directory /srv/catalogo
```

Abra <http://localhost:8040> no navegador para ver o catálogo. Agora volte ao shell e
pressione `Ctrl-C`: o programa parou. É exatamente isso que o serviço resolve — sem o
systemd, o programa só vive enquanto a sessão que o iniciou estiver viva.

## Escreva a unit

Um serviço systemd é descrito por um arquivo **unit**. Crie o do catálogo:

```bash
sudo tee /etc/systemd/system/catalogo.service > /dev/null <<'EOF'
[Unit]
Description=Catálogo da Aurora

[Service]
ExecStart=/usr/bin/python3 -m http.server 8040 --directory /srv/catalogo

[Install]
WantedBy=multi-user.target
EOF
```

Três seções, cada uma com um papel:

- `[Unit]` descreve o serviço para humanos e para o próprio systemd.
- `[Service]` diz o que executar — o `ExecStart` é o mesmo comando que você rodou na mão.
- `[Install]` diz **quando** o serviço deve subir: `WantedBy=multi-user.target` o liga ao
  boot normal da máquina.

O arquivo ainda não faz nada. O systemd não lê um arquivo novo até você mandar:

```bash
sudo systemctl daemon-reload
```

## A diferença entre `start` e `enable`

`start` e `enable` respondem a perguntas diferentes, e o Cenário inteiro existe por causa
dessa diferença:

```bash
sudo systemctl start catalogo.service
sudo systemctl status catalogo.service
```

O `start` sobe o serviço **agora**. Mas se a máquina reiniciar, o systemd não o subirá de
novo — `start` não deixa registro nenhum de intenção.

O `enable` é o que registra a intenção:

```bash
sudo systemctl enable catalogo.service
```

Repare que `enable` sozinho não sobe o serviço; ele só cria o vínculo de boot. O atalho
para os dois ao mesmo tempo:

```bash
sudo systemctl enable --now catalogo.service
```

Confirme o estado:

```bash
systemctl is-active catalogo.service
systemctl is-enabled catalogo.service
```

O `is-active` responde `active` e o `is-enabled` responde `enabled`. O catálogo está no
ar em <http://localhost:8040>.

## Verificação

A Verificação cobra os dois estados: o serviço precisa estar **ativo** (rodando) **e**
**habilitado** (sobe sozinho no boot). Um serviço iniciado com `start` na mão passa na
primeira checagem e falha na segunda — subir o catálogo certo pelo caminho errado não
conta.
````

- [ ] **Step 5: Escrever a Verificação**

`content/linux/08-um-programa-vira-servico/verificacao.yaml`:

```yaml
asercoes:
  - tipo: servico_systemd
    nome: catalogo.service
    ativo: true
    habilitado: true
    descricao: o catálogo é um serviço ativo e habilitado
  - tipo: http_responde
    url: http://localhost:8040
    status: 200
```

- [ ] **Step 6: Atualizar as contagens do catálogo**

Em `CatalogoRealTest`, no primeiro teste, acrescente o grupo e ajuste o total:

```java
        var linux = cenarios.stream()
                .filter(cenario -> cenario.id().startsWith("linux/"))
                .toList();
```

```java
        assertThat(cenarios).hasSize(62);
```

```java
        assertThat(linux).hasSize(1);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(2);
```

No segundo teste, renomeie e ajuste a ordem e o Linux sem Fundamentos:

```java
    @Test
    void carregaCincoTrilhasComFundamentosPublicadosNasQuatroAnteriores() {
```

```java
        assertThat(trilhas).extracting(trilha -> trilha.id())
                .containsExactly("linux", "docker", "kubernetes", "aws", "iac");
        assertThat(catalogo.buscar("linux").orElseThrow().fundamentos()).isNull();
```

- [ ] **Step 7: Rodar a suíte**

Run: `cd backend && ./mvnw test`
Expected: PASS.

- [ ] **Step 8: Provar o Cenário de ponta a ponta na aplicação real**

Suba o backend e o frontend, abra <http://localhost:5180>, entre na Trilha Linux e no
Cenário 08. Então:

1. Clique em **Iniciar cenário** (a primeira vez constrói a imagem — alguns minutos).
2. Clique em **Verificar antes de fazer qualquer coisa**. As três Asserções devem falhar.
3. Siga o Cenário no seu terminal, dentro do container.
4. Clique em **Verificar**. As três devem passar.

**Esta é a prova de que a Etapa 1 cumpriu o objetivo.**

- [ ] **Step 9: Commitar**

```bash
git add content/linux backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: Trilha Linux com o Cenário 08 provando o caminho de verificação"
```

---

### Task 7: README

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Acrescentar a seção de preparo**

Depois de "### Preparando a Trilha IaC":

```markdown
### Preparando a Trilha Linux

Nada precisa ser baixado adiante: a imagem é construída pelo próprio Compose a partir do
`Dockerfile` de cada Cenário. O primeiro **Iniciar cenário** constrói a imagem e leva
alguns minutos; os seguintes aproveitam o cache e levam segundos.

Você entra na máquina pelo terminal dele, com:

```powershell
docker exec -it learning-infra-linux bash
```

Essa linha é uma incantação com prazo de validade — a Trilha Docker, logo em seguida,
explica cada pedaço dela. O container da Trilha Linux sobe o systemd com `cgroup: host`
no Compose: é o mesmo alcance da VM do Docker Desktop que a plataforma já usa no
MiniStack, e estritamente menos poder que montar o socket do Docker.
```

- [ ] **Step 2: Atualizar a seção de portas**

No bloco de portas, acrescente o bloco da Trilha Linux:

```markdown
A Trilha Linux usa o bloco **8040–8049**, começando pela **8040** no Cenário 08.
```

- [ ] **Step 3: Documentar o frontmatter e as Asserções**

Na seção "## Escrevendo um Cenário", depois do bloco de IaC:

````markdown
Um Cenário da Trilha Linux declara `containerLinux`, o nome do container que hospeda a
máquina:

```yaml
projetoCompose: linux-08
containerLinux: learning-infra-linux
```

As duas Asserções tipadas recebem esse nome do frontmatter e nunca o repetem:

```yaml
- tipo: servico_systemd
  nome: catalogo.service
  ativo: true
  habilitado: true
  descricao: o catálogo é um serviço ativo e habilitado
- tipo: arquivo_linux
  caminho: /etc/sudoers.d/plantao
  modo: "440"
  dono: root
  descricao: a regra de sudo não é editável por quem ela beneficia
```

`servico_systemd` roda `systemctl is-active --quiet` e `is-enabled --quiet` dentro do
container e compara **códigos de saída** — `0` é o único positivo, porque `is-active` de
uma unit parada imprime `inactive` e uma checagem por substring aprovaria um serviço
parado. `arquivo_linux` roda `stat -c` e compara modo, dono e grupo campo a campo; campos
nulos não são verificados.
````

Acrescente `servico_systemd` e `arquivo_linux` à lista de tipos de Asserção no parágrafo
que hoje termina em `terraform_plano_limpo`.

- [ ] **Step 4: Registrar a Trilha na seção de Fundamentos**

Na seção "## Fundamentos e evolução das Trilhas", troque "As quatro Trilhas" por
"As quatro Trilhas publicadas" e acrescente ao final:

```markdown
A quinta Trilha, **Linux**, abre o catálogo — ela é a primeira da ordem recomendada de
estudo, e a Etapa 1 entregou a plataforma de verificação e o Cenário 08. Fundamentos e os
demais Cenários vêm nas próximas etapas. O desenho completo está em
[`docs/superpowers/specs/2026-08-21-trilha-linux-design.md`](docs/superpowers/specs/2026-08-21-trilha-linux-design.md).
```

- [ ] **Step 5: Verificar e commitar**

Run: `cd backend && ./mvnw test` e `cd frontend && npm run build`
Expected: PASS.

```bash
git add README.md
git commit -m "docs: README cobre a Trilha Linux, o frontmatter containerLinux e as duas Asserções"
```

---

## Definição de pronto

- `cd backend && ./mvnw test` passa, com `CatalogoRealTest` contando 62 Cenários e o
  catálogo abrindo por `linux`.
- `cd frontend && npm run build` passa.
- O Cenário 08 aparece no catálogo, inicia (systemd `running`), reprova antes do
  exercício e aprova depois — ativo e habilitado, respondendo na 8040.
- As quatro `trilha.yaml` existentes declararam `ordem` no mesmo commit do campo
  obrigatório; `CatalogoDeTrilhasTest` e `CatalogoRealTest` cobrem.
