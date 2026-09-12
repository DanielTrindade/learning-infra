# Plano — Backend: estado confiável e erros visíveis

> Data: 2026-09-10
>
> Origem: revisão do backend (bugs M2–M4, otimização O4)
>
> Depende de: [correções críticas](2026-09-10-backend-correcoes-criticas.md)
>
> Estado: concluído em 2026-09-10 — 152 testes verdes + `npm run build`

Fecha as brechas de estado (progresso e Iniciar) e faz as mensagens de diagnóstico
chegarem ao leitor.

---

### Task 1: M2 — ProblemDetail e frontend lendo a mensagem ✅

Hoje toda exceção vira `500` sem corpo útil (`server.error.include-message=never`) e o
`api.ts` descarta o corpo, exibindo apenas o status. As mensagens construídas com
cuidado no `GerenciadorDeCenarioAtivo` nunca aparecem.

**Files:**
- Create: `backend/src/main/java/dev/learninginfra/api/TratadorDeErros.java`
- Test: `backend/src/test/java/dev/learninginfra/api/TratadorDeErrosTest.java`
- Modify: `frontend/src/api.ts`

**Comportamento:** `@RestControllerAdvice` devolve `ProblemDetail` com `detail`
legível para `IllegalStateException`, `UncheckedIOException` e
`IllegalArgumentException`; `ResponseStatusException` mantém o status original. O
frontend passa a usar `detail` (fallback para a mensagem atual).

- [x] **Step 1: teste que falha** — resposta de erro traz `detail`.
- [x] **Step 2: implementar o advice** — `TratadorDeErros` cobre `ResponseStatusException`,
  `IllegalStateException`, `IllegalArgumentException` e `UncheckedIOException`.
- [x] **Step 3: consumir `detail` no `api.ts`** sem quebrar os `catch` existentes.
- [x] **Step 4: rodar** `.\mvnw.cmd test` e `npm run build` — verdes.

---

### Task 2: M3 — Progresso atômico, com lock e recuperação ✅

`RepositorioDeProgresso.salvar` grava direto no arquivo; crash no meio deixa JSON
truncado e `carregar` passa a lançar em toda requisição. Sem lock, dois POSTs
concorrentes perdem atualização.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/progresso/RepositorioDeProgresso.java`
- Test: `backend/src/test/java/dev/learninginfra/progresso/RepositorioDeProgressoTest.java`

**Comportamento:**
- `salvar` escreve em `arquivo.tmp` e move com `ATOMIC_MOVE` (fallback
  `REPLACE_EXISTING`);
- métodos sincronizados no mesmo lock;
- `carregar` com arquivo corrompido renomeia para `progresso.corrompido-<timestamp>.json`,
  registra aviso e devolve `Progresso.vazio()` em vez de derrubar o app.

- [x] **Step 1: testes que falham** — corrompido devolve vazio e cria quarentena;
  salvar deixa JSON íntegro.
- [x] **Step 2: implementar escrita atômica, lock e quarentena** — mais um método
  `atualizar(UnaryOperator<Progresso>)` para leitura-modificação-escrita sob o mesmo
  lock, usado por `marcarConcluido` e pela correção do questionário.
- [x] **Step 3: rodar** `.\mvnw.cmd test`.

---

### Task 3: M4 — Iniciar transacional e teardown de órfão ✅

Falha no meio do Iniciar deixa `work/` do Cenário novo no disco e progresso apontando
para o anterior. Id ativo que sumiu do catálogo faz o teardown ser pulado em silêncio.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivo.java`
- Test: `backend/src/test/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivoTest.java`

**Comportamento:**
- id ativo sem Cenário no catálogo → exceção com instrução de limpeza (nunca pular em
  silêncio);
- falha depois do teardown → melhor esforço de limpeza do ambiente parcial e
  `cenarioAtivo` volta a `null` antes de propagar o erro;
- ativo só é gravado com o ambiente pronto.

- [x] **Step 1: testes que falham** — órfão lança com o id e limpa o registro; falha de
  compose up deixa ativo nulo e ambiente parcial derrubado.
- [x] **Step 2: implementar** — `derrubarAnterior` e `limparAmbienteParcial`.
- [x] **Step 3: rodar** `.\mvnw.cmd test`.

---

### Task 4: O4 — Uma leitura de progresso por Iniciar ✅

`iniciar` lê o progresso três vezes (`cenarioAtivo()` + `carregar()` no salvar, além de
possíveis releituras). Passa a carregar uma vez e reutilizar.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivo.java`

- [x] **Step 1: refatorar mantendo a suíte verde** — o `iniciar` carrega uma vez e usa
  `atualizar` no fim.
- [x] **Step 2: rodar** `.\mvnw.cmd test`.
