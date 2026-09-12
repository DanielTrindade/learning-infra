# Plano — Backend: nomenclatura em inglês (código + API)

> Data: 2026-09-10
>
> Origem: revisão do backend (dívida de nomenclatura) + decisão de 2026-09-10
>
> Depende de: [modularização e SOLID](2026-09-10-backend-modularizacao-solid.md)
>
> Estado: concluído em 2026-09-10 — 157 testes verdes + `npm run build`

Renomeia identificadores para inglês por coerência do código, seguindo a linguagem
ubíqua em DDD. O **conteúdo didático permanece intocado**: `cenario.md`,
`verificacao.yaml`, chaves do frontmatter e valores `tipo: container_rodando` ficam
como estão; o mapeamento vive só nos leitores. Textos exibidos ao usuário (descrições
de Asserção, mensagens de diagnóstico, UI) continuam em português.

---

### Task 1: Glossário DDD ✅

**Files:**
- Modify: `CONTEXT.md`

Termos canônicos: Track, Fundamentals, Questionnaire, Scenario, Difficulty
(Guided/Assisted/Autonomous/Master), Active Scenario, Verification, Assertion,
Progress.

- [x] **Step 1: atualizar o glossário com os termos em inglês e a correspondência atual**.

---

### Task 2: Java interno ✅

**Files:** todo `backend/src/main/java/dev/learninginfra` e os testes.

- Pacotes: `conteudo`→`content`, `trilha`→`track`, `progresso`→`progress`,
  `verificacao`→`verification`, `ciclodevida`→`lifecycle`, `configuracao`→`config`,
  `execucao`→`execution`.
- Classes: `Cenario`→`Scenario`, `Assercao`→`Assertion`, `Dificuldade`→`Difficulty`
  (`AUTONOMO`→`AUTONOMOUS`, etc.), `MotorDeVerificacao`→`VerificationEngine`,
  `GerenciadorDeCenarioAtivo`→`ActiveScenarioManager`, entre outras.
- Strings de UI e mensagens permanecem em português. O schema do conteúdo continua em
  português: `ScenarioReader` ainda lê `asercoes`, `tipo: container_rodando` e
  `dificuldade: autonomo`; `Difficulty.fromText` e `Comparison.fromText` aceitam os
  dois vocabulários.

- [x] **Step 1: renomear pacote a pacote, com a suíte verde a cada um** — feito em
  varredura única guiada por scanner que só troca código (strings e comentários
  intactos), seguida de compilação e suíte completa.
- [x] **Step 2: rodar** `.\mvnw.cmd test`.

---

### Task 3: API + frontend no mesmo commit ✅

**Files:**
- Modify: controllers e DTOs em `backend/src/main/java/dev/learninginfra/api`
- Modify: `frontend/src/api.ts` e componentes que usam os tipos
- Modify: `backend/src/test/java/dev/learninginfra/api/*`

- Rotas: `/api/tracks`, `/api/scenarios`, `/{id}/fundamentals`,
  `/{id}/questionnaire`, `/{slug}/start`, `/{slug}/verify`.
- Campos: `title`, `difficulty`, `assertions`, `active`, `completed`,
  `allCompleted`, `progress`, `bestScore`, `attempts`, `minimumScore`, `questions`,
  `options`, `statement`, `review`, `explanation`, `correct`, `correctOption`,
  `assertionCount`, `workingDirectory`, entre outros.
- Textos de UI inalterados.

- [x] **Step 1: renomear DTOs e rotas no backend e ajustar testes**.
- [x] **Step 2: atualizar `api.ts` e componentes no mesmo commit**.
- [x] **Step 3: rodar** `.\mvnw.cmd test` e `npm run build`.

---

### Task 4: Configuração e persistência ✅

**Files:**
- Modify: `backend/src/main/resources/application.yaml`
- Modify: `backend/src/main/java/dev/learninginfra/progress/ProgressRepository.java`
- Test: `backend/src/test/java/dev/learninginfra/progress/ProgressRepositoryTest.java`

- Propriedades: `learning-infra.content-directory`, `learning-infra.working-directory`,
  `learning-infra.progress-file`.
- `Progress` com campos em inglês (`activeScenario`, `completed`, `fundamentals`) e
  `@JsonAlias` para as chaves antigas; o mesmo no `FundamentalsProgress`.
- Migração automática na primeira carga: `data/progresso.json` → `data/progress.json`,
  preservando conclusões, fundamentos e tentativas; o arquivo antigo fica de backup.

- [x] **Step 1: testes que falham** — progresso antigo é migrado sem perda.
- [x] **Step 2: implementar propriedades, alias e migração**.
- [x] **Step 3: rodar** `.\mvnw.cmd test`.

---

### Task 5: README e docs ✅

**Files:**
- Modify: `README.md`
- Modify: `docs/adr/0005-ordem-das-trilhas-e-recomendacao.md`

- [x] **Step 1: atualizar referências a nomes de código; exemplos de conteúdo não mudam**.
