# Plano — Backend: performance de leitura

> Data: 2026-09-10
>
> Origem: revisão do backend (otimizações O1–O3)
>
> Depende de: [estado confiável e erros visíveis](2026-09-10-backend-estado-e-erros.md)
>
> Estado: concluído em 2026-09-10 — 156 testes verdes

Sem mudar comportamento observável, corta trabalho repetido por requisição.

---

### Task 1: O1 — Progresso único por requisição ✅

`CenarioController.listar` chama `progressos.carregar()` dentro de `detalhar`, uma vez
por Cenário — 75 leituras e parses de JSON por chamada.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/api/CenarioController.java`
- Test: `backend/src/test/java/dev/learninginfra/api/CenarioControllerTest.java`

- [x] **Step 1: carregar o progresso uma vez e passar para `detalhar`**.
- [x] **Step 2: rodar** `.\mvnw.cmd test`.

---

### Task 2: O2 — Cache do catálogo por assinatura do conteúdo ✅

Cada requisição re-parseia os 75 Cenários (markdown incluso) e os manifestos das
Trilhas. Cache invalidado por assinatura do diretório de conteúdo (quantidade de
arquivos + soma dos mtimes), preservando o hot-reload documentado no README.

**Files:**
- Create: `backend/src/main/java/dev/learninginfra/conteudo/AssinaturaDeConteudo.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/RepositorioDeCenarios.java`
- Modify: `backend/src/main/java/dev/learninginfra/trilha/CatalogoDeTrilhas.java`
- Test: `backend/src/test/java/dev/learninginfra/conteudo/RepositorioDeCenariosTest.java`
- Test: `backend/src/test/java/dev/learninginfra/trilha/CatalogoDeTrilhasTest.java`

**Aceite:** criar um Cenário novo no diretório de conteúdo aparece na próxima chamada;
nenhum parse extra quando nada mudou.

- [x] **Step 1: testes que falham** — hot-reload continua funcionando.
- [x] **Step 2: implementar a assinatura e os caches**.
- [x] **Step 3: rodar** `.\mvnw.cmd test`.

---

### Task 3: O3 — Asserções em paralelo, resultado em ordem ✅

As Asserções são independentes; um HTTP travado (10s) ou `kubectl wait` (até 20s) hoje
soma em série. Avaliar em virtual threads preservando a ordem de saída.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Test: `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java`

**Aceite:** ordem do checklist idêntica à declarada; suíte verde; Verificação com
Asserção lenta não espera a soma das demais.

- [x] **Step 1: teste que falha** — ordem preservada com duas Asserções.
- [x] **Step 2: implementar o paralelismo com virtual threads**.
- [x] **Step 3: rodar** `.\mvnw.cmd test`.
