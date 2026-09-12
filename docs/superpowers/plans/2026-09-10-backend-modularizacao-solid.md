# Plano — Backend: modularização e SOLID

> Data: 2026-09-10
>
> Origem: revisão do backend (SRP, DIP, modelo, duplicação)
>
> Depende de: [performance de leitura](2026-09-10-backend-performance-leitura.md)
>
> Estado: concluído em 2026-09-10 — 156 testes verdes

Refatoração sem mudança de comportamento; toda a suíte de testes existente é o
contrato. Cada task é independente e pode parar com o app funcionando.

---

### Task 1: Quebrar o `MotorDeVerificacao` por domínio ✅

Hoje são 529 linhas: processo, HTTP, parsing e regras de seis trilhas no mesmo lugar. O
`switch` exaustivo continua sendo a composição (decisão do README), mas cada ramo passa
a delegar para um avaliador do seu domínio.

**Files:**
- Create: `backend/src/main/java/dev/learninginfra/verificacao/ContextoDeVerificacao.java`
- Create: `backend/src/main/java/dev/learninginfra/verificacao/AvaliadorDeContainer.java`
- Create: `backend/src/main/java/dev/learninginfra/verificacao/AvaliadorHttp.java`
- Create: `backend/src/main/java/dev/learninginfra/verificacao/AvaliadorKubernetes.java`
- Create: `backend/src/main/java/dev/learninginfra/verificacao/AvaliadorAws.java`
- Create: `backend/src/main/java/dev/learninginfra/verificacao/AvaliadorTerraform.java`
- Create: `backend/src/main/java/dev/learninginfra/verificacao/AvaliadorLinux.java`
- Create: `backend/src/main/java/dev/learninginfra/verificacao/AvaliadorDeComando.java`
- Create: `backend/src/main/java/dev/learninginfra/execucao/TextoDeComando.java`
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`

**Aceite:** suíte verde sem editar nenhum teste de comportamento; `MotorDeVerificacao`
só decide e delega.

- [x] **Step 1: extrair `ContextoDeVerificacao` e `TextoDeComando`**.
- [x] **Step 2: mover cada família de avaliação para o seu avaliador** — 570 → 158 linhas.
- [x] **Step 3: rodar** `.\mvnw.cmd test`.

---

### Task 2: Quebrar o `GerenciadorDeCenarioAtivo` ✅

364 linhas com cinco responsabilidades. O orquestrador mantém a ordem e o progresso;
cada peça vira uma classe.

**Files:**
- Create: `backend/src/main/java/dev/learninginfra/ciclodevida/MaterializadorDeWorkspace.java`
- Create: `backend/src/main/java/dev/learninginfra/ciclodevida/TeardownDeCenario.java`
- Create: `backend/src/main/java/dev/learninginfra/ciclodevida/PreparadorCompose.java`
- Create: `backend/src/main/java/dev/learninginfra/ciclodevida/PreparadorKubernetes.java`
- Create: `backend/src/main/java/dev/learninginfra/ciclodevida/PreparadorAws.java`
- Modify: `backend/src/main/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivo.java`

**Aceite:** os testes de comando (ordem exata dos `docker`/`kubectl`) continuam verdes
sem mudança; o construtor público de 4 argumentos permanece para os testes.

- [x] **Step 1: extrair o materializador de workspace**.
- [x] **Step 2: extrair o teardown** — mais `LimpezaAwsLocal` e `ErrosDeDocker` compartilhados.
- [x] **Step 3: extrair os três preparadores** — 414 → 127 linhas.
- [x] **Step 4: rodar** `.\mvnw.cmd test`.

---

### Task 3: Extrair o corretor de questionário do catálogo ✅

`CatalogoDeTrilhas` lê catálogo, corrige questionário e persiste progresso. O catálogo
fica só leitura; a correção vira caso de uso.

**Files:**
- Create: `backend/src/main/java/dev/learninginfra/trilha/CorretorDeQuestionario.java`
- Modify: `backend/src/main/java/dev/learninginfra/trilha/CatalogoDeTrilhas.java`
- Modify: `backend/src/main/java/dev/learninginfra/api/TrilhaController.java`
- Modify: `backend/src/test/java/dev/learninginfra/trilha/CatalogoDeTrilhasTest.java`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Aceite:** `CatalogoDeTrilhas` deixa de depender de `RepositorioDeProgresso` e `Clock`;
testes de correção migram para o novo serviço.

- [x] **Step 1: criar o corretor e mover a lógica**.
- [x] **Step 2: ajustar controller e testes**.
- [x] **Step 3: rodar** `.\mvnw.cmd test`.

---

### Task 4: Value objects no `Cenario` ✅

O record tem 18 componentes e cinco construtores telescópicos. Configuração por trilha
vira value object; resta um único construtor compacto para o caso simples.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/Cenario.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Modify: `backend/src/main/java/dev/learninginfra/ciclodevida/*`
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Modify: testes que constroem `Cenario`

**Interfaces:**
- `ConfiguracaoCompose(String projeto, List<String> volumes)`
- `ConfiguracaoKubernetes(String contexto, String namespace, String manifestosIniciais)`
- `ConfiguracaoAws(boolean ministack, boolean infraestruturaReal, String inicializacao)`
- `ConfiguracaoTerraform(boolean ativo, String diretorio)`
- `ConfiguracaoLinux(String container)`
- Acessores atuais (`usaCompose`, `usaKubernetes`, ...) delegam para os value objects.

- [x] **Step 1: criar os value objects e o construtor compacto** — `simples(...)` e um
  construtor canônico; os cinco construtores telescópicos sumiram.
- [x] **Step 2: migrar `LeitorDeCenario` e consumidores** — `terraform()` virou
  `usaTerraform()` para não colidir com o acessor do componente.
- [x] **Step 3: ajustar testes**.
- [x] **Step 4: rodar** `.\mvnw.cmd test`.

---

### Task 5: DIP e duplicações restantes ✅

- Injetar `LeitorDeTrilha` no `CatalogoDeTrilhas` (hoje `new` dentro do `@Component`).
- Unificar o cálculo de `EstadoDosFundamentos`, hoje duplicado em `TrilhaResumo` e
  `FundamentosDetalhados`, movendo o enum para `progresso` e derivando num só lugar.
- Substituir o `ultimoDetalhe` duplicado pelo `TextoDeComando`.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/trilha/CatalogoDeTrilhas.java`
- Create: `backend/src/main/java/dev/learninginfra/progresso/EstadoDosFundamentos.java`
- Modify: `backend/src/main/java/dev/learninginfra/api/dto/TrilhaResumo.java`
- Modify: `backend/src/main/java/dev/learninginfra/api/dto/FundamentosDetalhados.java`
- Modify: `backend/src/main/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivo.java`

- [x] **Step 1: injetar o leitor e remover o `ultimoDetalhe` duplicado**.
- [x] **Step 2: unificar o estado dos Fundamentos** — `EstadoDosFundamentos.de(...)` no
  pacote `progresso`; JSON idêntico.
- [x] **Step 3: rodar** `.\mvnw.cmd test`.
