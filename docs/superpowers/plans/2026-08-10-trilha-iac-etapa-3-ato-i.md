# Trilha IaC — Etapa 3: Ato I, Cenários 02 a 05

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fechar o Ato I — do imperativo ao declarativo, inteiramente sobre o provider
Docker — com os Cenários 02 a 05, que ensinam a ler o plano, parametrizar ambientes,
entender o grafo de dependências e reconhecer drift.

**Architecture:** Nenhuma mudança de plataforma. Os quatro Cenários são conteúdo no
disco: `cenario.md`, `verificacao.yaml` e `workspace/` com os `.tf` de partida. Cada um
reusa as Asserções que já existem — `container_rodando`, `http_responde`,
`container_em_rede`, `volume_existe`, `comando_produz` — sempre acompanhadas de
`terraform_estado` ou `terraform_plano_limpo`, conforme a regra da Etapa 1 de que
infraestrutura certa por caminho errado não conclui Cenário.

**Tech Stack:** Terraform 1.15.8 · `kreuzwerker/docker` ~> 4.5 · Docker Engine 29.x ·
`nginx:1.27-alpine` · SnakeYAML · JUnit 5

## Global Constraints

- **Todas as restrições das Etapas [1](./2026-08-09-trilha-iac-etapa-1-plataforma.md) e
  [2](./2026-08-10-trilha-iac-etapa-2-fundamentos.md) continuam valendo.**
- Terraform **1.15.8** pinado; provider `kreuzwerker/docker` **~> 4.5**. Nenhum outro
  provider entra no Ato I.
- Imagem única: **`nginx:1.27-alpine`**. Nenhuma imagem nova é baixada.
- **Portas:** 02 → **8071**; 03 → **8072** e **8073**; 04 → **8074**; 05 → **8075**. O
  bloco 8070–8079 é da Trilha IaC e a 8070 é do Cenário 01.
- Todo `.tf` entregue no `workspace/` declara `required_version = "= 1.15.8"` e a
  constraint do provider — o `GuardrailsDeTerraformTest` varre `content/iac/**/*.tf`.
- Comandos de exemplo em **PowerShell**, rodados **dentro** do diretório de trabalho. O
  conteúdo nunca usa `terraform -chdir=`: no PowerShell a forma sem aspas não expande
  variável, e a lição não é sobre isso.
- A Verificação é **estritamente observadora**: nunca roda `apply`, `destroy` ou `init`.
- Todo texto em **português**; personagens por papel, nunca por pronome.
- A narrativa é contínua: cada Cenário abre com a pressão concreta que justifica o
  assunto, e a Mirante do Cenário 05 é maior do que a do 02.

## Estrutura de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `docs/research/iac-course.md` | recebe a tabela de símbolos do plano medida na Task 1 |
| `content/iac/02-o-plano-e-o-contrato/` | `cenario.md`, `verificacao.yaml`, `workspace/versions.tf`, `workspace/main.tf` |
| `content/iac/03-o-mesmo-servico-dois-ambientes/` | idem, mais `workspace/variables.tf` e `workspace/homologacao.tfvars` |
| `content/iac/04-a-ordem-que-ninguem-escreveu/` | idem, com `main.tf` sem as referências que criam a ordem |
| `content/iac/05-mexeram-na-producao/` | idem, com `main.tf` completo e aplicável |
| `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java` | contagens do catálogo |

---

### Task 1: Medir os símbolos do plano antes de escrever sobre eles

O Cenário 02 ensina a ler `+`, `~`, `-` e `-/+`. Escrever esse texto sem medir qual
mudança produz qual símbolo no provider Docker 4.5 é inventar. A pesquisa já provou que
`ports` força recriação; falta saber o que muda **em lugar**.

**Files:**
- Modify: `docs/research/iac-course.md` (seção nova ao final)

**Interfaces:**
- Consumes: nada.
- Produces: a tabela de símbolos que a Task 2 cita no `cenario.md` do 02.

- [ ] **Step 1: Montar o laboratório**

```powershell
$lab = "$env:TEMP\lab-simbolos"
if (Test-Path $lab) { Remove-Item -Recurse -Force $lab }
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
  name    = "lab-simbolos"
  image   = docker_image.web.image_id
  restart = "no"

  ports {
    internal = 80
    external = 8071
  }
}
'@ | Out-File -Encoding utf8 "$lab\main.tf"
terraform -chdir="$lab" init
terraform -chdir="$lab" apply -auto-approve
```

Esperado: `Apply complete! Resources: 2 added, 0 changed, 0 destroyed.`

- [ ] **Step 2: Medir `~` — mudança em lugar**

Troque `restart = "no"` por `restart = "unless-stopped"` e planeje:

```powershell
(Get-Content "$lab\main.tf" -Raw).Replace('restart = "no"', 'restart = "unless-stopped"') | Out-File -Encoding utf8 "$lab\main.tf"
terraform -chdir="$lab" plan -no-color | Select-String -Pattern "will be updated in-place|must be replaced|forces replacement|^Plan:"
```

Anote o resultado. **Se `restart` aparecer como `forces replacement`**, o Cenário 02 usa
`labels` como exemplo de `~` e este passo é repetido com um bloco
`labels { label = "dono", value = "plataforma" }`. Um dos dois é atualizável em lugar; o
Cenário precisa citar o que foi medido, não o que se espera.

- [ ] **Step 3: Medir `-/+` — recriação**

```powershell
(Get-Content "$lab\main.tf" -Raw).Replace('external = 8071', 'external = 8075') | Out-File -Encoding utf8 "$lab\main.tf"
terraform -chdir="$lab" plan -no-color | Select-String -Pattern "must be replaced|forces replacement|^Plan:"
```

Esperado, pela pesquisa de 2026-08-10: `# docker_container.web must be replaced` com
`# forces replacement` nas linhas de `ports`.

- [ ] **Step 4: Medir `-` — remoção**

Apague o recurso `docker_container.web` do arquivo e planeje:

```powershell
terraform -chdir="$lab" plan -no-color | Select-String -Pattern "will be destroyed|^Plan:"
```

Esperado: `# docker_container.web will be destroyed` e `Plan: 0 to add, 0 to change, 1 to destroy.`

- [ ] **Step 5: Medir `plan -out` e o apply do plano salvo**

Restaure o container no arquivo e:

```powershell
terraform -chdir="$lab" plan -out="$lab\plano.tfplan" -no-color
terraform -chdir="$lab" show -no-color "$lab\plano.tfplan" | Select-Object -First 5
terraform -chdir="$lab" apply -no-color "$lab\plano.tfplan"
```

Esperado: o `apply` de um plano salvo **não pede confirmação**. Anote a mensagem exata,
porque o Cenário 02 depende dela para explicar por que um pipeline usa plano salvo.

- [ ] **Step 6: Limpar e registrar**

```powershell
terraform -chdir="$lab" destroy -auto-approve
Remove-Item -Recurse -Force $lab
docker ps -a --filter name=lab-simbolos --format '{{.Names}}'
```

Acrescente ao final de `docs/research/iac-course.md`:

```markdown
### Símbolos do plano medidos no provider Docker 4.5

| Mudança | Símbolo | Linha observada |
|---|---|---|
| atributo atualizável em lugar | `~` | PREENCHER com o atributo que se confirmou atualizável |
| `ports.external` | `-/+` | `# forces replacement` |
| recurso removido do arquivo | `-` | `will be destroyed` |
| recurso novo no arquivo | `+` | `will be created` |

O `apply` de um plano salvo com `-out` não pede confirmação: PREENCHER com a mensagem
observada.
```

Substitua os dois `PREENCHER` pelos valores reais. Plano com placeholder committado é
exatamente o defeito que esta Task existe para evitar.

```bash
git add docs/research/iac-course.md
git commit -m "docs: mede os símbolos do plano no provider Docker"
```

---

### Task 2: Cenário 02 — O plano é o contrato

**Files:**
- Create: `content/iac/02-o-plano-e-o-contrato/cenario.md`
- Create: `content/iac/02-o-plano-e-o-contrato/verificacao.yaml`
- Create: `content/iac/02-o-plano-e-o-contrato/workspace/versions.tf`
- Create: `content/iac/02-o-plano-e-o-contrato/workspace/main.tf`

**Interfaces:**
- Consumes: a tabela de símbolos da Task 1.
- Produces: o Cenário 02 no catálogo. A Task 6 conta os Cenários.

- [ ] **Step 1: Criar o workspace**

`workspace/versions.tf` — idêntico em todos os Cenários deste Ato:

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

`workspace/main.tf` — o resultado do Cenário 01, já escrito, para que o assunto seja ler
o plano e não redigitar recursos:

```hcl
resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name    = "mirante-web"
  image   = docker_image.web.image_id
  restart = "no"

  ports {
    internal = 80
    external = 8071
  }
}
```

- [ ] **Step 2: Escrever o Cenário**

`content/iac/02-o-plano-e-o-contrato/cenario.md`, com este frontmatter exato:

```yaml
---
id: iac/02-o-plano-e-o-contrato
titulo: O plano é o contrato
dificuldade: guiado
terraform: true
containers: [mirante-web]
---
```

Estrutura obrigatória do texto, na ordem:

1. **Abertura narrativa.** A Mirante contratou uma segunda pessoa. O primeiro `apply` que
   ela rodou derrubou o serviço por quarenta minutos, porque ela aprovou um plano sem ler.
   A lição do Cenário: o plano não é ruído antes do `yes`, é o contrato do que vai
   acontecer.
2. **`## Aplique o que já está escrito`.** `terraform init`, `terraform apply`. O
   container sobe na 8071.
3. **`## Os quatro símbolos`.** A tabela medida na Task 1, com uma frase por símbolo. O
   `-/+` recebe o parágrafo mais longo, porque é o único que destrói.
4. **`## Faça uma mudança barata`.** O leitor altera o atributo que a Task 1 confirmou
   ser atualizável em lugar e roda `terraform plan`. Confirma o `~` e o rodapé
   `0 to add, 1 to change, 0 to destroy`. Aplica.
5. **`## Faça uma mudança cara — e não aplique`.** O leitor troca `external = 8071` por
   `external = 8075` e planeja. Aparece `must be replaced` e `# forces replacement` nas
   linhas de `ports`. O texto explica: a porta publicada não é ajustável num container em
   execução, então o provider destrói e cria outro. **Desfaça a mudança antes de seguir.**
   O texto diz explicitamente que aplicar aqui deixaria a Verificação vermelha, porque a
   Asserção espera a 8071.
6. **`## O plano salvo`.** `terraform plan -out=plano.tfplan`, `terraform show plano.tfplan`,
   `terraform apply plano.tfplan`. O ponto: o `apply` de um plano salvo não pergunta nada,
   porque a decisão já foi tomada quando o plano foi revisado. É por isso que um pipeline
   separa as duas etapas — e é o que o Cenário 12 vai montar por inteiro.
7. **`## Verificação`.** Explica que `terraform_plano_limpo` só aprova se o arquivo e a
   realidade coincidirem: quem deixou a porta em 8075 no arquivo e não aplicou reprova, e
   quem aplicou reprova na Asserção de HTTP. As duas juntas fecham a saída.

- [ ] **Step 3: Escrever a Verificação**

`content/iac/02-o-plano-e-o-contrato/verificacao.yaml`:

```yaml
asercoes:
  - tipo: container_rodando
    nome: mirante-web
  - tipo: http_responde
    url: http://localhost:8071
    status: 200
  - tipo: terraform_estado
    endereco: docker_container.web
    atributo: name
    esperado: mirante-web
    descricao: o container está sob gestão do Terraform
  - tipo: terraform_plano_limpo
    descricao: o arquivo e a infraestrutura no ar descrevem a mesma coisa
```

- [ ] **Step 4: Provar o Cenário na aplicação real**

Suba backend e frontend, inicie o Cenário 02 e:

1. **Verifique antes de fazer qualquer coisa.** As quatro Asserções devem falhar.
2. Faça o exercício até o fim.
3. Verifique. As quatro devem passar.
4. Deixe `external = 8075` no arquivo **sem aplicar** e verifique de novo:
   `terraform_plano_limpo` reprova e as outras três passam. Essa é a prova de que a
   Asserção mede divergência, e não apenas presença.
5. Desfaça e aplique.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/02-o-plano-e-o-contrato
git commit -m "feat: Cenário 02 da Trilha IaC — ler o plano antes de aprovar"
```

---

### Task 3: Cenário 03 — O mesmo serviço, dois ambientes

**Files:**
- Create: `content/iac/03-o-mesmo-servico-dois-ambientes/cenario.md`
- Create: `content/iac/03-o-mesmo-servico-dois-ambientes/verificacao.yaml`
- Create: `content/iac/03-o-mesmo-servico-dois-ambientes/workspace/versions.tf`
- Create: `content/iac/03-o-mesmo-servico-dois-ambientes/workspace/variables.tf`
- Create: `content/iac/03-o-mesmo-servico-dois-ambientes/workspace/main.tf`

**Interfaces:**
- Consumes: nada da Task 2.
- Produces: o Cenário 03. É o primeiro a usar `for_each` sobre `locals`, que o Cenário 08
  transforma em módulo.

- [ ] **Step 1: Criar o workspace**

`workspace/versions.tf` é idêntico ao da Task 2, Step 1.

`workspace/variables.tf`:

```hcl
variable "ambiente" {
  type        = string
  description = "nome do ambiente — aparece no nome do container"

  validation {
    condition     = contains(["homologacao", "producao"], var.ambiente)
    error_message = "ambiente precisa ser homologacao ou producao."
  }
}

variable "porta" {
  type        = number
  description = "porta publicada no host"

  validation {
    condition     = var.porta >= 8070 && var.porta <= 8079
    error_message = "a porta precisa estar no bloco 8070-8079 reservado para esta Trilha."
  }
}
```

`workspace/main.tf` — sem valor de variável embutido, para que o `apply` sem `-var-file`
falhe e a lição apareça:

```hcl
locals {
  nome_do_container = "mirante-${var.ambiente}"
}

resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name  = local.nome_do_container
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = var.porta
  }
}

output "endereco" {
  value       = "http://localhost:${var.porta}"
  description = "onde o serviço deste ambiente responde"
}
```

**Não** entregue o `.tfvars` pronto: escrevê-lo é o exercício.

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/03-o-mesmo-servico-dois-ambientes
titulo: O mesmo serviço, dois ambientes
dificuldade: guiado
terraform: true
containers: [mirante-homologacao, mirante-producao]
---
```

Estrutura obrigatória:

1. **Abertura.** Um cliente grande exigiu ambiente de homologação antes de assinar. A
   saída óbvia — copiar o diretório inteiro e trocar dois valores — é a dívida que o
   Cenário 08 vai cobrar. Aqui o passo é menor e correto: extrair o que varia.
2. **`## O que varia e o que não varia`.** Distingue os três recursos de parametrização, e
   isso é o coração do Cenário: `variable` é **entrada**, `local` é **apelido calculado** e
   `output` é **saída**. A regra prática: se vem de fora, é `variable`; se é derivado de
   outra coisa que já está no arquivo, é `local`.
3. **`## Rode sem dizer nada`.** `terraform apply` sem `-var-file`. O Terraform **pergunta**
   os valores no terminal. O texto explica por que isso é ruim num pipeline e por que a
   `validation` da variável é a primeira linha de defesa: peça `porta = 9090` e leia a
   mensagem de erro que o próprio arquivo escreveu.
4. **`## Escreva os dois arquivos de valores`.** O leitor cria `homologacao.tfvars` e
   `producao.tfvars`:

   ```hcl
   ambiente = "homologacao"
   porta    = 8072
   ```

   e

   ```hcl
   ambiente = "producao"
   porta    = 8073
   ```

5. **`## Um workspace por ambiente`.** Aqui entra a decisão que o Cenário precisa tornar
   explícita: um único diretório com um único state **não** consegue manter os dois
   ambientes no ar ao mesmo tempo, porque o segundo `apply` substituiria o primeiro. A
   ferramenta para isso é `terraform workspace`:

   ```powershell
   terraform workspace new homologacao
   terraform apply -var-file="homologacao.tfvars"
   terraform workspace new producao
   terraform apply -var-file="producao.tfvars"
   terraform workspace list
   ```

   O texto precisa dizer, sem rodeio, que workspace do Terraform separa **states**, não
   separa credencial nem conta, e que por isso não é o mecanismo de isolamento que
   ambientes de produção de verdade usam — lá se usa diretório e backend separados. É a
   ponte honesta para o Cenário 08.
6. **`## Leia a saída`.** `terraform output endereco` em cada workspace. O `output` é a
   interface do módulo com quem o consome, e o Cenário 08 vai depender disso.
7. **`## Verificação`.** Os dois containers precisam estar no ar simultaneamente, e o
   `terraform_plano_limpo` roda no workspace **selecionado** — o texto instrui a terminar
   com `terraform workspace select producao`, que é onde a Asserção de state vai olhar.

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: container_rodando
    nome: mirante-homologacao
  - tipo: container_rodando
    nome: mirante-producao
  - tipo: http_responde
    url: http://localhost:8072
    status: 200
  - tipo: http_responde
    url: http://localhost:8073
    status: 200
  - tipo: terraform_estado
    endereco: docker_container.web
    atributo: name
    esperado: mirante-producao
    descricao: o workspace selecionado é o de produção e o container nasceu do código
  - tipo: terraform_plano_limpo
    descricao: o ambiente selecionado não tem mudança pendente
```

- [ ] **Step 4: Provar na aplicação real**

Além do ciclo de sempre — verificar antes, fazer, verificar depois — faça o teste que
prova o valor pedagógico: rode `terraform workspace select homologacao` e verifique de
novo. A Asserção de `terraform_estado` reprova dizendo que o nome no state é
`mirante-homologacao`. Confirme que a mensagem de falha é compreensível para quem não
sabe o que aconteceu; se não for, ajuste o texto do Cenário, não a Asserção.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/03-o-mesmo-servico-dois-ambientes
git commit -m "feat: Cenário 03 da Trilha IaC — variáveis, tfvars, locals e output"
```

---

### Task 4: Cenário 04 — A ordem que ninguém escreveu

**Files:**
- Create: `content/iac/04-a-ordem-que-ninguem-escreveu/cenario.md`
- Create: `content/iac/04-a-ordem-que-ninguem-escreveu/verificacao.yaml`
- Create: `content/iac/04-a-ordem-que-ninguem-escreveu/workspace/versions.tf`
- Create: `content/iac/04-a-ordem-que-ninguem-escreveu/workspace/main.tf`

**Interfaces:**
- Consumes: nada.
- Produces: o Cenário 04, primeiro `Assistido` da Trilha.

- [ ] **Step 1: Criar o workspace com o defeito**

`workspace/versions.tf` é idêntico ao da Task 2.

`workspace/main.tf` — repare que **nada aqui referencia nada**: os nomes são repetidos
como texto, e é isso que o leitor precisa consertar:

```hcl
resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_network" "interna" {
  name = "mirante-interna"
}

resource "docker_volume" "conteudo" {
  name = "mirante-conteudo"
}

resource "docker_container" "web" {
  name  = "mirante-web"
  image = "nginx:1.27-alpine"

  ports {
    internal = 80
    external = 8074
  }
}
```

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/04-a-ordem-que-ninguem-escreveu
titulo: A ordem que ninguém escreveu
dificuldade: assistido
terraform: true
containers: [mirante-web]
volumes: [mirante-conteudo]
---
```

Sendo `Assistido`, o texto entrega **objetivo, dicas e comandos parciais** — nunca o
`main.tf` corrigido. Estrutura:

1. **Abertura.** O arquivo entregue aplica sem erro, e mesmo assim está errado: o
   container não entra na rede, não monta o volume e não tem relação nenhuma com a imagem
   que o arquivo declara. Está tudo no mesmo arquivo e nada está conectado.
2. **`## O objetivo`.** Ao final, `mirante-web` precisa estar na rede `mirante-interna`,
   montar o volume `mirante-conteudo` em `/usr/share/nginx/html` e usar a imagem
   declarada por `docker_image.web` — e o leitor precisa conseguir **provar** que a ordem
   de criação foi derivada, não escrita.
3. **`## A dica que importa`.** Referenciar um atributo de outro recurso cria uma aresta
   no grafo. `image = docker_image.web.image_id` não é economia de digitação: é a
   declaração de que a imagem precede o container. Os blocos que o leitor precisa
   descobrir são `networks_advanced` e `volumes` dentro de `docker_container` — os nomes
   são dados, a forma não.
4. **`## Veja o grafo`.**

   ```powershell
   terraform graph
   ```

   O texto explica que a saída é DOT, que não é preciso renderizar para ler, e pede que o
   leitor conte as arestas antes e depois da correção.
5. **`## Quando a referência não existe`.** Introduz `depends_on` pelo caso legítimo: uma
   ordem real que nenhum atributo revela. Precisa dizer com todas as letras que
   `depends_on` é a **exceção**, e que usá-lo onde uma referência resolveria é esconder a
   relação de quem lê o código depois.
6. **`## Prove a ordem`.**

   ```powershell
   terraform destroy -auto-approve
   terraform apply
   ```

   Ler a sequência de criação na saída do `apply`: imagem, rede e volume primeiro, o
   container por último. Ninguém escreveu essa ordem.
7. **`## Verificação`.**

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: container_rodando
    nome: mirante-web
  - tipo: container_em_rede
    nome: mirante-web
    rede: mirante-interna
    presente: true
  - tipo: volume_existe
    nome: mirante-conteudo
  - tipo: terraform_estado
    endereco: docker_network.interna
    atributo: name
    esperado: mirante-interna
    descricao: a rede nasceu do código
  - tipo: terraform_plano_limpo
    descricao: o grafo aplicado é o que o arquivo descreve
```

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo. Além dele, o teste que prova o degrau `Assistido`: confirme que o
`cenario.md` **não** contém um bloco `networks_advanced` nem `volumes` pronto para copiar.
Se contiver, o Cenário virou `Guiado` e o texto precisa ser reduzido.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/04-a-ordem-que-ninguem-escreveu
git commit -m "feat: Cenário 04 da Trilha IaC — grafo de dependências e depends_on"
```

---

### Task 5: Cenário 05 — Mexeram na produção

**Files:**
- Create: `content/iac/05-mexeram-na-producao/cenario.md`
- Create: `content/iac/05-mexeram-na-producao/verificacao.yaml`
- Create: `content/iac/05-mexeram-na-producao/workspace/versions.tf`
- Create: `content/iac/05-mexeram-na-producao/workspace/main.tf`

**Interfaces:**
- Consumes: nada.
- Produces: o Cenário 05, que fecha o Ato I. É o primeiro a exercitar o triângulo dos
  Fundamentos com as mãos.

- [ ] **Step 1: Criar o workspace**

`workspace/versions.tf` idêntico ao da Task 2. `workspace/main.tf`:

```hcl
resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name    = "mirante-web"
  image   = docker_image.web.image_id
  restart = "unless-stopped"

  ports {
    internal = 80
    external = 8075
  }
}
```

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/05-mexeram-na-producao
titulo: Mexeram na produção
dificuldade: assistido
terraform: true
containers: [mirante-web]
---
```

Estrutura:

1. **Abertura.** Madrugada de sábado, alerta, e alguém resolveu no braço. Segunda-feira o
   serviço está de pé e ninguém sabe o que foi feito. Este Cenário é sobre descobrir e
   sobre decidir para onde a correção vai.
2. **`## Aplique e confira`.** `terraform init`, `terraform apply`, serviço na 8075.
3. **`## Crie o drift você mesmo`.** O leitor produz duas divergências, com as mãos, e o
   texto pede que ele **preveja** o plano antes de rodar:

   ```powershell
   docker stop mirante-web
   terraform plan
   docker start mirante-web
   docker rm -f mirante-web
   terraform plan
   ```

   Pela medição de 2026-08-10: o container parado sai como `must be replaced`; o container
   removido sai como `has been deleted` seguido de `will be created`. Os dois casos são
   divergências entre **state e mundo real**, os dois lados opostos do triângulo, e o
   texto precisa nomeá-los assim, ligando à seção `O triângulo` dos Fundamentos.
4. **`## O plan é leitura, o apply é escrita`.** Reforça que nenhum dos `plan` acima mudou
   nada. É a razão de a Verificação da plataforma poder rodar `plan` sem risco.
5. **`## refresh-only: atualizar o que se sabe sem mudar o que existe`.**

   ```powershell
   terraform plan -refresh-only
   terraform apply -refresh-only
   ```

   O texto explica a diferença de intenção: `apply` normal move o **mundo** até o código;
   `apply -refresh-only` move o **state** até o mundo. Um serve para reconciliar, o outro
   para registrar que a realidade mudou sem alterá-la — útil quando a mudança feita à mão
   foi correta e ainda não deu tempo de virar código.
6. **`## Reconcilie`.** `terraform apply` de verdade, o serviço volta na 8075.
7. **`## Onde a correção deveria ter ido`.** Fecha o arco: uma correção que precisa durar
   vira commit no código. Uma correção feita só no mundo real tem prazo de validade — o
   próximo `apply`.
8. **`## Verificação`.**

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: container_rodando
    nome: mirante-web
  - tipo: http_responde
    url: http://localhost:8075
    status: 200
  - tipo: terraform_estado
    endereco: docker_container.web
    atributo: name
    esperado: mirante-web
    descricao: o container voltou pelo código, e não à mão
  - tipo: terraform_plano_limpo
    descricao: não há mais divergência entre o código, o state e o que está no ar
```

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo, mais a prova que dá sentido ao Cenário: com o exercício concluído, rode
`docker rm -f mirante-web` e depois `docker run -d --name mirante-web -p 8075:80
nginx:1.27-alpine`. `container_rodando` e `http_responde` passam; `terraform_plano_limpo`
reprova, porque o state aponta para um container que não existe mais. Restaure com
`docker rm -f mirante-web` e `terraform apply`.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/05-mexeram-na-producao
git commit -m "feat: Cenário 05 da Trilha IaC — drift, refresh-only e reconciliação"
```

---

### Task 6: Fechar o Ato I no catálogo

**Files:**
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`
- Modify: `README.md`

**Interfaces:**
- Consumes: os quatro Cenários das Tasks 2 a 5.
- Produces: nada consumido por código.

- [ ] **Step 1: Atualizar as contagens**

Em `CatalogoRealTest`, no primeiro teste, o total sobe de 44 para **48** e o grupo `iac`
de 1 para **5**:

```java
        assertThat(cenarios).hasSize(48);
```

```java
        assertThat(iac).hasSize(5);
        assertThat(iac).allMatch(Cenario::terraform);
```

A soma de Asserções do grupo `iac` passa de 4 para **25**: 4 no Cenário 01, 4 no 02, 6 no
03, 5 no 04 e 6 no 05. Ajuste a linha correspondente:

```java
        assertThat(iac.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(25);
```

Se a sua contagem der diferente, conte de novo os `verificacao.yaml` antes de mudar o
número — a divergência costuma ser Asserção esquecida, não teste errado.

- [ ] **Step 2: Rodar a suíte inteira**

Run: `cd backend && ./mvnw test`
Expected: PASS, incluindo `GuardrailsDeTerraformTest`, que agora varre nove arquivos `.tf`.

- [ ] **Step 3: Atualizar o README**

Na seção "### Portas usadas pelos Cenários", substitua a frase da Trilha IaC por:

```markdown
A Trilha IaC usa o bloco **8070–8079**: 8070 no Cenário 01, 8071 no 02, 8072 e 8073 no
03, 8074 no 04 e 8075 no 05.
```

Na seção "## Fundamentos e evolução das Trilhas", troque "Cenários em construção" por
"o Ato I completo e os Atos II a IV em construção".

- [ ] **Step 4: Commitar**

```bash
git add backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java README.md
git commit -m "docs: Ato I da Trilha IaC completo no catálogo e no README"
```

---

## Definição de pronto

- `cd backend && ./mvnw test` passa, com o catálogo em 48 Cenários e 5 na Trilha `iac`.
- `cd frontend && npm run build` passa.
- Os quatro Cenários iniciam, reprovam antes do exercício e aprovam depois.
- A prova de falso positivo foi executada em cada um: 02 com a porta divergente no
  arquivo, 03 com o workspace errado selecionado, 05 com o container recriado à mão. Nos
  três, uma Asserção de Terraform reprova enquanto as de infraestrutura passam.
- `docs/research/iac-course.md` tem a tabela de símbolos medida, sem nenhum `PREENCHER`.
- O Cenário 04 não entrega o `main.tf` corrigido no texto.
