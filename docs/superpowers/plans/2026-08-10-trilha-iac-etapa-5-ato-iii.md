# Trilha IaC — Etapa 5: Ato III, Cenários 10 a 12

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fechar o Ato III — confiança — com os Cenários 10 a 12: reprodutibilidade por
lockfile e constraints, infraestrutura que se testa, e a sequência que um pipeline rodaria.

**Architecture:** Nenhuma mudança de plataforma. Os três Cenários são conteúdo, e todos
reusam `comando_produz` para as evidências que nenhuma Asserção tipada cobre — a presença
do lockfile, a saída do `terraform test` e o consumo do plano salvo. A decisão que molda o
Ato: a Verificação é **estritamente observadora**, então ela só executa a parte do
`terraform test` que roda em modo `plan`. O arquivo de teste que aplica infraestrutura de
verdade existe, é exercitado pelo leitor e **não** é executado pela Verificação.

**Tech Stack:** Terraform 1.15.8 · OpenTofu 1.12.5 (opcional) · `kreuzwerker/docker` ~> 4.5

## Global Constraints

- **Todas as restrições das Etapas [1](./2026-08-09-trilha-iac-etapa-1-plataforma.md) a
  [4](./2026-08-10-trilha-iac-etapa-4-ato-ii.md) continuam valendo.**
- **Portas:** 10 → **8070**, 11 → **8071**, 12 → **8072**. O bloco 8070–8079 é **reusado**
  a partir daqui: pela [ADR 0002](../../adr/0002-um-cenario-ativo-por-vez.md) só existe um
  Cenário Ativo por vez, e dezoito Cenários não cabem em dez portas. O README precisa
  registrar o reuso explicitamente.
- **A Verificação nunca roda `terraform test` em modo `apply`.** O `test` com
  `command = apply` cria e destrói infraestrutura real; usá-lo numa Asserção quebraria a
  invariante de Verificação observadora que vale desde a Etapa 1.
- **O `ExecutorDeComandoReal` tem timeout de 30 s.** As quatro execuções de `run` em modo
  `plan` medidas em 2026-08-10 ficaram na casa de segundos, mas o Cenário 11 precisa medir
  de novo antes de fechar a Verificação.
- OpenTofu é **opcional** e **nunca verificado**: se `tofu` não estiver no PATH, o passo é
  pulado sem prejuízo. Nenhuma Asserção depende dele.
- Todo texto em **português**; personagens por papel, nunca por pronome.

## Estrutura de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `content/iac/10-reproduzivel-na-maquina-de-todo-mundo/` | Cenário 10 |
| `content/iac/11-infra-que-se-testa/` | Cenário 11, com `workspace/tests/` |
| `content/iac/12-o-pipeline-que-voce-rodaria-no-ci/` | Cenário 12 |
| `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java` | contagens |
| `README.md` | reuso de portas e estado da Trilha |

---

### Task 1: Medir o `terraform test` no ambiente da plataforma

O Cenário 11 entrega uma Asserção que executa `terraform test`. Antes de escrevê-la é
preciso saber quanto tempo ela leva e qual é a linha exata que o `contem` vai procurar.

**Files:**
- Modify: `docs/research/iac-course.md` (acrescenta à seção de validação de 2026-08-10)

**Interfaces:**
- Consumes: nada.
- Produces: a duração medida e a linha de saída que a Task 3 usa no `contem`.

- [ ] **Step 1: Montar o laboratório**

Reproduza o laboratório da validação de 2026-08-10: um `main.tf` com uma `variable`
validada, um `docker_container` com `precondition` e `postcondition`, um bloco `check`, e
um `tests/unidade.tftest.hcl` com três `run` em `command = plan` — um assert normal, um
`expect_failures` sobre a `precondition` e um `expect_failures` sobre a `validation` da
variável. Os blocos exatos estão no
[estudo de ferramental](../../research/iac-course.md), seção de 2026-08-10.

- [ ] **Step 2: Medir**

```powershell
$lab = "$env:TEMP\lab-test"
terraform -chdir="$lab" init -no-color | Out-Null
Measure-Command { terraform -chdir="$lab" test -no-color -test-directory=tests }
terraform -chdir="$lab" test -no-color -test-directory=tests | Select-Object -Last 3
Write-Output "EXITCODE=$LASTEXITCODE"
```

Esperado: `TotalSeconds` **bem abaixo de 30**, exit code 0, e a última linha no formato
`Success! N passed, 0 failed.`

Se passar de 30 s, o Cenário 11 não pode verificar com `comando_produz` direto: nesse
caso, a Asserção passa a conferir um arquivo de saída que o leitor gera com
`terraform test | Out-File resultado.txt`, e o Cenário instrui esse passo. Decida aqui,
não na Task 3.

- [ ] **Step 3: Limpar e registrar**

```powershell
Remove-Item -Recurse -Force $lab
```

Acrescente à seção de validação de 2026-08-10 em `docs/research/iac-course.md`:

```markdown
Duração de `terraform test` com três `run` em modo `plan`: PREENCHER com o TotalSeconds
medido. Linha final observada: PREENCHER com a linha `Success! …` exata.
```

Substitua os dois `PREENCHER` antes de commitar.

```bash
git add docs/research/iac-course.md
git commit -m "docs: mede a duração do terraform test para a Verificação"
```

---

### Task 2: Cenário 10 — Reprodutível na máquina de todo mundo

**Files:**
- Create: `content/iac/10-reproduzivel-na-maquina-de-todo-mundo/cenario.md`
- Create: `content/iac/10-reproduzivel-na-maquina-de-todo-mundo/verificacao.yaml`
- Create: `content/iac/10-reproduzivel-na-maquina-de-todo-mundo/workspace/versions.tf`
- Create: `content/iac/10-reproduzivel-na-maquina-de-todo-mundo/workspace/main.tf`

**Interfaces:**
- Consumes: nada.
- Produces: o Cenário 10.

- [ ] **Step 1: Criar o workspace com a constraint frouxa**

`workspace/versions.tf` — repare que aqui, e **só** aqui em toda a Trilha, a constraint do
provider é deliberadamente frouxa. É o defeito que o leitor vai consertar:

```hcl
terraform {
  required_version = ">= 1.0"

  required_providers {
    docker = {
      source = "kreuzwerker/docker"
    }
  }
}

provider "docker" {}
```

`workspace/main.tf`:

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

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/10-reproduzivel-na-maquina-de-todo-mundo
titulo: Reprodutível na máquina de todo mundo
dificuldade: assistido
terraform: true
containers: [mirante-web]
---
```

Estrutura obrigatória:

1. **Abertura.** O time cresceu para quatro pessoas. O mesmo `apply` produziu planos
   diferentes em duas máquinas na mesma tarde, e ninguém consegue explicar. O arquivo é o
   mesmo; o que não é o mesmo é o **binário** que o lê e o **provider** que ele resolve.
2. **`## Duas versões, dois planos`.** O `required_version = ">= 1.0"` aceita qualquer
   Terraform desta década; a constraint sem `version` aceita qualquer provider. O texto
   explica que "mais nova" é uma resposta que muda com o tempo — a mesma configuração
   resolve coisas diferentes em datas diferentes.
3. **`## Aperte as constraints`.** O leitor troca por `= 1.15.8` e `~> 4.5`. O texto
   precisa explicar o operador pessimista: `~> 4.5` aceita 4.6 e recusa 5.0, o que traduz
   a promessa de compatibilidade do versionamento semântico em regra executável.
4. **`## O arquivo que ninguém escreve à mão`.**

   ```powershell
   terraform init
   Get-Content .terraform.lock.hcl
   ```

   O lockfile registra a versão resolvida **e o digest** de cada plataforma. Constraint é
   a faixa aceitável; lockfile é a escolha já feita. O texto precisa dizer, com todas as
   letras, que **o lockfile vai para o Git** — é a única exceção à lista de ignorados que
   o Cenário 06 montou — e por quê: sem ele, cada `init` reabre a decisão.
5. **`## Quando você quer reabrir a decisão`.**

   ```powershell
   terraform init -upgrade
   ```

   Move o lockfile dentro do que a constraint permite. O texto marca a diferença: `init`
   respeita o lockfile, `init -upgrade` o reescreve — e reescrever lockfile é mudança que
   entra em pull request como qualquer outra.
6. **`## O mesmo código, outro binário`** *(opcional)*. Se `tofu` estiver no PATH, o leitor
   roda `tofu init` e `tofu plan` no mesmo diretório, sem alterar uma linha. O texto conta
   a história curta: BUSL desde 2023, fork sob a Linux Foundation, HCL idêntico, e a razão
   de a Trilha usar Terraform — é o comando das vagas. Fecha com a lição que vale mais do
   que escolher um lado: a fungibilidade é a propriedade que protege o time.
   **Se `tofu` não estiver instalado, o passo é pulado** e nada na Verificação muda.
7. **`## Verificação`.**

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: container_rodando
    nome: mirante-web
  - tipo: http_responde
    url: http://localhost:8070
    status: 200
  - tipo: comando_produz
    comando: ["powershell.exe", "-NoProfile", "-Command", "Get-Content ../work/.terraform.lock.hcl -Raw"]
    contem: kreuzwerker/docker
    descricao: o lockfile existe e registra o provider resolvido
  - tipo: comando_produz
    comando: ["powershell.exe", "-NoProfile", "-Command", "Get-Content ../work/versions.tf -Raw"]
    contem: "~> 4.5"
    descricao: a constraint do provider deixou de aceitar qualquer versão
  - tipo: terraform_estado
    endereco: docker_container.web
    atributo: name
    esperado: mirante-web
    descricao: o container nasceu do código
  - tipo: terraform_plano_limpo
    descricao: o código descreve a infraestrutura que está no ar
```

O caminho `../work/` resolve porque o backend roda a partir de `backend/` e
`learninginfra.diretorio-de-trabalho` aponta para `../work`. Confirme no Step 4.

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo. A prova específica: antes de apertar as constraints, verifique — a
Asserção da constraint reprova enquanto as outras passam. É o que separa este Cenário de
um `apply` qualquer.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/10-reproduzivel-na-maquina-de-todo-mundo
git commit -m "feat: Cenário 10 da Trilha IaC — constraints, lockfile e OpenTofu"
```

---

### Task 3: Cenário 11 — Infra que se testa

**Files:**
- Create: `content/iac/11-infra-que-se-testa/cenario.md`
- Create: `content/iac/11-infra-que-se-testa/verificacao.yaml`
- Create: `content/iac/11-infra-que-se-testa/workspace/versions.tf`
- Create: `content/iac/11-infra-que-se-testa/workspace/variables.tf`
- Create: `content/iac/11-infra-que-se-testa/workspace/main.tf`

**Interfaces:**
- Consumes: a duração e a linha de saída medidas na Task 1.
- Produces: o Cenário 11.

- [ ] **Step 1: Criar o workspace**

`workspace/versions.tf` é o padrão da Trilha, com `= 1.15.8` e `~> 4.5`.

`workspace/variables.tf`:

```hcl
variable "ambiente" {
  type        = string
  description = "nome do ambiente"
  default     = "homologacao"

  validation {
    condition     = contains(["homologacao", "producao"], var.ambiente)
    error_message = "ambiente precisa ser homologacao ou producao."
  }
}

variable "porta" {
  type        = number
  description = "porta publicada no host"
  default     = 8071
}
```

`workspace/main.tf` — **sem** as três checagens. Escrevê-las é o exercício:

```hcl
resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name  = "mirante-${var.ambiente}"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = var.porta
  }
}

output "nome_do_container" {
  value = docker_container.web.name
}
```

O diretório `workspace/tests/` **não** é entregue: criá-lo é parte do exercício.

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/11-infra-que-se-testa
titulo: Infra que se testa
dificuldade: assistido
terraform: true
containers: [mirante-homologacao]
---
```

Estrutura obrigatória:

1. **Abertura.** Um `apply` publicou a porta errada em produção. Ninguém errou de
   digitação: o valor veio de uma variável e ninguém checou a faixa. Código de aplicação
   tem teste; código de infraestrutura também pode ter.
2. **`## Quatro degraus, do barato ao caro`.** Uma seção curta por degrau, nesta ordem, e
   cada uma dizendo **o que aquele degrau não alcança**:
   - `terraform fmt -check -recursive` — formatação, e só;
   - `terraform validate` — sintaxe e coerência de tipos, **sem falar com provider nenhum**;
   - `precondition`, `postcondition` e `check` — regras que rodam com o plano e o apply;
   - `terraform test` — o único que exercita o comportamento de ponta a ponta.
3. **`## As três checagens que ficam no recurso`.** O leitor acrescenta ao `main.tf`:
   uma `precondition` que exige `porta` entre 8070 e 8079; uma `postcondition` que confirma
   que o nome do container nasceu como declarado; e um bloco `check` sobre o número de
   portas publicadas. O texto explica a diferença de momento e de severidade: a
   `precondition` reprova o plano antes de qualquer chamada; a `postcondition` reprova
   depois de o recurso existir; o `check` **avisa sem reprovar** — e é por isso que ele
   serve para o que é desejável, não para o que é obrigatório.
4. **`## Dois arquivos de teste, dois propósitos`.** Aqui está a decisão que o Cenário
   precisa tornar explícita. O leitor cria:
   - `tests/unidade.tftest.hcl`, com `run` em `command = plan`: um assert sobre o nome
     derivado, um `expect_failures = [docker_container.web]` para a porta fora da faixa e
     um `expect_failures = [var.ambiente]` para o ambiente inválido;
   - `tests/integracao.tftest.hcl`, com um `run` em `command = apply` que confere o
     `output` — e que **cria e destrói infraestrutura de verdade**.

   O texto explica por que o segundo é mais caro e por que um pipeline costuma rodar os
   dois em momentos diferentes.
5. **`## Rode`.**

   ```powershell
   terraform fmt -check -recursive
   terraform validate
   terraform test
   ```

   A saída termina em `Success! N passed, 0 failed.` O texto pede que o leitor **quebre**
   um teste de propósito e leia a mensagem de falha antes de consertar.
6. **`## O que a Verificação vai rodar`.** Transparência com o leitor: a Verificação roda
   apenas `tests/unidade.tftest.hcl`, porque ela é observadora por princípio e o teste de
   integração aplica infraestrutura. Rodar o de integração é responsabilidade do leitor, e
   o Cenário diz isso em vez de esconder.
7. **`## Verificação`.**

- [ ] **Step 3: Escrever a Verificação**

O `contem` usa a contagem de `run` do arquivo de unidade — três, se o leitor seguir a
seção 4. Ajuste o número ao que a Task 1 mediu e ao que o Cenário pede.

```yaml
asercoes:
  - tipo: container_rodando
    nome: mirante-homologacao
  - tipo: http_responde
    url: http://localhost:8071
    status: 200
  - tipo: comando_produz
    comando: ["terraform", "-chdir=../work", "test", "-no-color", "-filter=tests/unidade.tftest.hcl"]
    contem: "3 passed, 0 failed"
    descricao: os testes de unidade da infraestrutura passam
  - tipo: comando_produz
    comando: ["terraform", "-chdir=../work", "fmt", "-check", "-recursive", "-no-color"]
    contem: ""
    descricao: os arquivos estão formatados
  - tipo: terraform_estado
    endereco: docker_container.web
    atributo: name
    esperado: mirante-homologacao
    descricao: o container nasceu do código
  - tipo: terraform_plano_limpo
    descricao: o código descreve a infraestrutura que está no ar
```

**Confirme dois pontos no Step 4 e ajuste se preciso:** que `contem: ""` é aceito pelo
`LeitorDeCenario` como "qualquer saída, desde que o comando tenha sucesso" — se não for,
troque a Asserção de `fmt` por um `contem` sobre uma saída real ou remova-a; e que
`-filter` aceita o caminho com barra normal no Windows.

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo. Duas provas específicas:

1. Antes de escrever as checagens, verifique: a Asserção do `test` reprova porque o
   diretório `tests/` não existe. A mensagem precisa ser compreensível.
2. Com tudo pronto, rode `terraform test` no terminal e confirme que o teste de integração
   sobe e derruba o container — e que, depois disso, a Verificação continua verde. Se o
   teste de integração deixar sobra de ambiente, o Cenário precisa instruir a limpeza.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/11-infra-que-se-testa
git commit -m "feat: Cenário 11 da Trilha IaC — fmt, validate, checagens e terraform test"
```

---

### Task 4: Cenário 12 — O pipeline que você rodaria no CI

**Files:**
- Create: `content/iac/12-o-pipeline-que-voce-rodaria-no-ci/cenario.md`
- Create: `content/iac/12-o-pipeline-que-voce-rodaria-no-ci/verificacao.yaml`
- Create: `content/iac/12-o-pipeline-que-voce-rodaria-no-ci/workspace/versions.tf`
- Create: `content/iac/12-o-pipeline-que-voce-rodaria-no-ci/workspace/main.tf`
- Create: `content/iac/12-o-pipeline-que-voce-rodaria-no-ci/workspace/tests/unidade.tftest.hcl`

**Interfaces:**
- Consumes: as checagens e o formato de teste do Cenário 11.
- Produces: o Cenário 12, que fecha o Ato III.

- [ ] **Step 1: Criar o workspace**

`workspace/versions.tf` é o padrão da Trilha. `workspace/main.tf`:

```hcl
variable "porta" {
  type    = number
  default = 8072
}

resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name  = "mirante-web"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = var.porta
  }

  lifecycle {
    precondition {
      condition     = var.porta >= 8070 && var.porta <= 8079
      error_message = "a porta precisa estar no bloco 8070-8079 reservado para esta Trilha."
    }
  }
}
```

`workspace/tests/unidade.tftest.hcl` — entregue pronto, porque o assunto deste Cenário é a
**sequência**, não escrever teste de novo:

```hcl
run "porta_padrao_esta_na_faixa" {
  command = plan

  assert {
    condition     = docker_container.web.name == "mirante-web"
    error_message = "o container deveria se chamar mirante-web"
  }
}

run "porta_fora_da_faixa_reprova" {
  command = plan

  variables {
    porta = 9090
  }

  expect_failures = [
    docker_container.web,
  ]
}
```

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/12-o-pipeline-que-voce-rodaria-no-ci
titulo: O pipeline que você rodaria no CI
dificuldade: autonomo
terraform: true
containers: [mirante-web]
---
```

Sendo `Autônomo`, entrega objetivo e ambiente. Estrutura:

1. **Abertura.** A Mirante decidiu que ninguém mais aplica da própria máquina. O que vai
   para o CI precisa ser uma sequência que qualquer pessoa consiga auditar depois — e o
   passo mais importante dela é o único que não é um comando.
2. **`## O objetivo`.** Montar e rodar, na ordem, a sequência que um runner rodaria, e
   terminar com um `apply` que consumiu **um plano salvo**, não um plano recalculado. Ao
   final, `plano.tfplan` existe no diretório de trabalho e a infraestrutura está no ar.
3. **`## Os degraus`.** A lista é dada, porque a lição é a ordem e o motivo, não adivinhar
   os comandos: `fmt -check` → `validate` → `test` → `plan -out` → **revisão humana** →
   `apply` do plano salvo. O texto pede que o leitor justifique, para si mesmo, por que
   cada degrau vem antes do seguinte — e por que os três primeiros são baratos de propósito.
4. **`## O degrau que não é um comando`.** A revisão. O `plan -out` produz um artefato
   binário; `terraform show plano.tfplan` o torna legível, e `terraform show -json` o torna
   automatizável. O texto precisa afirmar que o valor do plano salvo é **remover a janela
   entre o que foi aprovado e o que foi aplicado**: sem ele, o `apply` recalcula, e o que
   entra em produção pode não ser o que passou na revisão.
5. **`## Quebre de propósito`.** O leitor roda a sequência com `-var="porta=9090"` e vê a
   `precondition` reprovar no `plan`, antes de qualquer chamada ao Docker. É a
   demonstração de que o pipeline falha barato.
6. **`## -target é para incêndio`.** Uma seção curta e categórica. `-target` estreita o
   plano, e um plano estreito não é o retrato do sistema: mudanças pendentes em outros
   recursos ficam invisíveis. Serve durante um incidente e cobra um `apply` completo
   depois. O texto liga isso à seção `Blast radius` dos Fundamentos.
7. **`## O que ficou de fora`.** Honestidade sobre o escopo: não há runner aqui. A
   sequência é a mesma; o que muda no CI é quem a executa e onde os segredos moram. É a
   ponte para o Cenário 16.
8. **`## Verificação`.**

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: container_rodando
    nome: mirante-web
  - tipo: http_responde
    url: http://localhost:8072
    status: 200
  - tipo: comando_produz
    comando: ["powershell.exe", "-NoProfile", "-Command", "if (Test-Path ../work/plano.tfplan) { 'plano-salvo-presente' } else { 'ausente' }"]
    contem: plano-salvo-presente
    descricao: o apply consumiu um plano salvo, e não um plano recalculado
  - tipo: comando_produz
    comando: ["terraform", "-chdir=../work", "test", "-no-color"]
    contem: "2 passed, 0 failed"
    descricao: a etapa de teste do pipeline passa
  - tipo: terraform_estado
    endereco: docker_container.web
    atributo: name
    esperado: mirante-web
    descricao: o container nasceu do código
  - tipo: terraform_plano_limpo
    descricao: depois do apply do plano salvo, não sobrou mudança pendente
```

A Asserção do plano salvo prova **presença**, não consumo — o Terraform não deixa marca de
que aplicou aquele arquivo. O Cenário precisa ser honesto sobre isso no texto, em vez de
sugerir uma garantia que a Asserção não dá.

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo. A prova específica: apague `plano.tfplan` e verifique — só essa Asserção
reprova.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/12-o-pipeline-que-voce-rodaria-no-ci
git commit -m "feat: Cenário 12 da Trilha IaC — a sequência que um pipeline rodaria"
```

---

### Task 5: Fechar o Ato III no catálogo

**Files:**
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`
- Modify: `README.md`

**Interfaces:**
- Consumes: os três Cenários das Tasks 2 a 4.
- Produces: nada consumido por código.

- [ ] **Step 1: Atualizar as contagens**

Total: **55**. Grupo `iac`: **12**. Soma de Asserções do grupo `iac`: 49 do fim do Ato II
mais 6 no Cenário 10, 6 no 11 e 6 no 12 — **67**.

```java
        assertThat(cenarios).hasSize(55);
```

```java
        assertThat(iac).hasSize(12);
        assertThat(iac).allMatch(Cenario::terraform);
        assertThat(iac.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(67);
```

Se você removeu a Asserção de `fmt` na Task 3, o total cai para 66. Conte os
`verificacao.yaml` antes de escrever o número.

- [ ] **Step 2: Rodar a suíte inteira**

Run: `cd backend && ./mvnw test`
Expected: PASS.

- [ ] **Step 3: Atualizar o README**

Na seção de portas, acrescente o parágrafo do reuso:

```markdown
A partir do Cenário 10 a Trilha IaC **reusa** o bloco 8070–8079: 8070 no 10, 8071 no 11 e
8072 no 12. Como só existe um Cenário Ativo por vez, dois Cenários podem declarar a mesma
porta sem colidir.
```

Na seção "## Fundamentos e evolução das Trilhas", troque para "os Atos I a III completos e
o Ato IV em construção".

- [ ] **Step 4: Commitar**

```bash
git add backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java README.md
git commit -m "docs: Ato III da Trilha IaC completo no catálogo e no README"
```

---

## Definição de pronto

- `cd backend && ./mvnw test` passa, com o catálogo em 55 Cenários e 12 na Trilha `iac`.
- `cd frontend && npm run build` passa.
- Os três Cenários iniciam, reprovam antes e aprovam depois.
- A duração do `terraform test` foi medida e cabe no timeout de 30 s do executor — ou a
  Task 3 adotou a alternativa do arquivo de saída, e o Cenário 11 instrui esse passo.
- Nenhuma Asserção da Trilha executa `terraform test` em modo `apply`.
- O Cenário 12 não entrega a sequência de comandos pronta para copiar — é `Autônomo`.
- O passo de OpenTofu está marcado como opcional e nenhuma Asserção depende dele.
