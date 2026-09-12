# Plano — Backend: correções críticas

> Data: 2026-09-10
>
> Origem: revisão do backend (bugs A1–A3 e M1)
>
> Estado: concluído em 2026-09-10 — 143 testes verdes

Elimina os bloqueios do fluxo Iniciar/Verificar e o falso positivo do `aws_consulta`,
sem mudar o schema do conteúdo além do campo aditivo `comparacao`. Nenhum contrato da
API muda, exceto o novo `409` documentado em M1.

---

### Task 1: A1 — Teardown tolera volume já ausente ✅

`docker volume rm -f` devolve `no such volume` quando o volume não existe, e o
`pareceInexistente` atual só reconhece `no such container`, `no such object` e
`not found`. Cenários com `volumes:` no frontmatter (docker/11, iac/04, 07, 09, 18)
bloqueiam o próximo Iniciar sempre que o volume não existir — nunca criado ou removido
à mão pelo procedimento do README.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivo.java`
- Test: `backend/src/test/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivoTest.java`

**Aceite:** derrubar um Cenário cujo volume já não existe conclui o teardown e o Iniciar
segue; qualquer outro erro de remoção continua ruidoso.

- [x] **Step 1: teste que falha** — executor que devolve erro `no such volume` apenas no
  `docker volume rm`; iniciar o segundo Cenário não pode lançar.
- [x] **Step 2: implementar** — `derrubar` passou a usar `pareceInexistente` no volume e
  `pareceInexistente` reconhece `no such volume`.
- [x] **Step 3: rodar** `.\mvnw.cmd test` — `GerenciadorDeCenarioAtivoTest` verde (22 test).

---

### Task 2: A2 — Timeout por chamada no executor ✅

`ExecutorDeComandoReal` mata todo processo em 30s. O primeiro Iniciar da Trilha Linux
roda `docker compose up -d --build` e o README documenta ~50s de build; `terraform plan`
também pode passar de 30s. O teto precisa ser escolhido por chamada.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/execucao/ExecutorDeComando.java`
- Modify: `backend/src/main/java/dev/learninginfra/execucao/ExecutorDeComandoReal.java`
- Modify: `backend/src/main/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivo.java`
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Test: `backend/src/test/java/dev/learninginfra/execucao/ExecutorDeComandoRealTest.java`

**Interfaces:**
- `ExecutorDeComando.executar(List, Duration)` e `executar(List, Map, Duration)` como
  `default` que delegam para os métodos atuais — dublês de teste seguem válidos.
- `ExecutorDeComandoReal` mantém 30s padrão e honra o limite recebido.
- `GerenciadorDeCenarioAtivo`: `compose up` com 10 minutos.
- `MotorDeVerificacao`: Terraform (`state show` e `plan`) com 2 minutos.

**Aceite:** timeout curto devolve código `-1` com "tempo esgotado"; compose up e
Terraform usam os limites maiores; suíte verde.

- [x] **Step 1: teste que falha** — comando que dorme além de um limite curto devolve
  `-1` e "tempo esgotado após Ns".
- [x] **Step 2: adicionar as sobrecargas com `Duration`**.
- [x] **Step 3: usar os limites maiores em compose e Terraform**.
- [x] **Step 4: rodar** `.\mvnw.cmd test`.

---

### Task 3: A3 — Comparação exata no `aws_consulta` ✅

`stdout.contains(esperado)` aprova `INACTIVE` para `ACTIVE` e `"10"` para `"1"`. O
conteúdo usa substring de propósito em seis Asserções (`igw-`, `nat-`, `COMPLETE`,
URL da fila, RedrivePolicy e evento S3), então a comparação vira explícita:

- novo campo opcional `comparacao` em `aws_consulta`, valores `exato` (padrão) e `contem`;
- `exato`: alguma linha do stdout, após `strip`, é igual ao esperado;
- `contem`: comportamento atual.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/Assercao.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Modify: `content/aws/04-sqs-dlq/verificacao.yaml` (QueueUrl e RedrivePolicy)
- Modify: `content/aws/05-pipeline-eventos/verificacao.yaml` (evento S3)
- Modify: `content/aws/06-cloudformation/verificacao.yaml` (StackStatus COMPLETE)
- Modify: `content/aws/08-subnets-e-rotas/verificacao.yaml` (igw- e nat-)
- Test: `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java`

**Aceite:** `ACCEPTED`/`INACTIVE` não aprovam `ACTIVE`; `"10"` não aprova `"1"`;
`CREATE_COMPLETE` continua aprovando `COMPLETE` via `comparacao: contem`; suíte verde.

- [x] **Step 1: testes que falham** — exato rejeita `INACTIVE`; exato aceita uma linha
  igual em saída multilinha; `contem` preserva `CREATE_COMPLETE`.
- [x] **Step 2: implementar o campo e as duas comparações** — enum `Comparacao`.
- [x] **Step 3: marcar as seis Asserções do conteúdo com `comparacao: contem`**.
- [x] **Step 4: rodar** `.\mvnw.cmd test`.

---

### Task 4: M1 — Verificação exige o Cenário Ativo ✅

`POST /api/cenarios/{trilha}/{slug}/verificar` avalia contra o ambiente corrente e
marca concluído sem conferir que o Cenário verificado é o ativo. A invariante do
ADR 0002 fica só na convenção do README.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/api/CenarioController.java`
- Test: `backend/src/test/java/dev/learninginfra/api/CenarioControllerTest.java`
- Modify: `README.md` (nota de que Verificar exige Cenário ativo)

**Comportamento:** se o id verificado difere do ativo (ou não há ativo), responde
`409 Conflict` com mensagem orientando a clicar em Iniciar; o progresso não muda.

- [x] **Step 1: testes que falham** — cenário não-ativo devolve 409; cenário ativo
  (mesmo com Asserções reprovadas) devolve 200.
- [x] **Step 2: implementar a guarda no controller**.
- [x] **Step 3: documento no README**.
- [x] **Step 4: rodar** `.\mvnw.cmd test`.
