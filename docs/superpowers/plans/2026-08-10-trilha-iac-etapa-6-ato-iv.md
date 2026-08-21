# Trilha IaC — Etapa 6: Ato IV, Cenários 13 a 18

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fechar a Trilha IaC — e a plataforma — com os Cenários 13 a 18: o mesmo HCL
atravessando Kubernetes e AWS, state remoto com lock, rede declarada, e um incidente final
que combina os três substratos.

**Architecture:** Nenhuma Asserção nova. O Ato IV combina o vocabulário que já existe —
`kubernetes_condicao`, `kubernetes_jsonpath`, `aws_consulta`, `comando_produz` — com as
duas Asserções de Terraform da Etapa 1. O que muda é o frontmatter: um Cenário passa a
declarar `terraform: true` junto de `contextoKubernetes`/`namespaceKubernetes` ou de
`ministack: true`, e o Cenário 18 declara os três ao mesmo tempo. Dois riscos técnicos
continuam abertos e são resolvidos na Task 1, **antes** de qualquer conteúdo: o
`use_lockfile` contra o S3 do MiniStack e a RAM do Cenário 18.

**Tech Stack:** Terraform 1.15.8 · `kreuzwerker/docker` ~> 4.5 · `hashicorp/kubernetes`
~> 3.2 · `hashicorp/aws` ~> 6.58 · MiniStack · cluster `docker-desktop`

## Global Constraints

- **Todas as restrições das Etapas [1](./2026-08-09-trilha-iac-etapa-1-plataforma.md) a
  [5](./2026-08-10-trilha-iac-etapa-5-ato-iii.md) continuam valendo.**
- **Guardrails executáveis, já cobrados pelo `GuardrailsDeTerraformTest`:** todo `.tf` com
  `provider "aws"` declara `endpoints` para `http://127.0.0.1:4566` e os três `skip_*`;
  nenhuma access key `AKIA`/`ASIA` no conteúdo; todo `provider "kubernetes"` fixa
  `config_context = "docker-desktop"`. Um `.tf` que viole isso **quebra a suíte**.
- **Nenhuma Asserção de AWS pode alegar isolamento de rede, IAM aplicado, custo ou
  disponibilidade.** Mesma regra da Trilha AWS, registrada em
  [`docs/research/aws-local-lab.md`](../../research/aws-local-lab.md).
- **Portas:** o bloco 8070–8079 continua reusado. 13 → **8070** via `NodePort` **30070**;
  15 e 16 não publicam porta; 17 não publica porta; 18 → **8071**.
- Namespaces Kubernetes: `learning-infra-iac-13`, `learning-infra-iac-14`,
  `learning-infra-iac-18`.
- O teardown **nunca** chama `terraform destroy`. Cada Cenário declara `containers`,
  `volumes`, `namespaceKubernetes` e `ministack`, e o `work/` recriado a cada Iniciar leva
  junto `.terraform/`, o lockfile e o state local.
- Todo texto em **português**; personagens por papel, nunca por pronome.

## Estrutura de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `docs/research/iac-course.md` | recebe o resultado das validações da Task 1 |
| `content/iac/13-a-mirante-migra-para-o-cluster/` | Cenário 13 |
| `content/iac/14-quem-e-o-dono-deste-recurso/` | Cenário 14 |
| `content/iac/15-a-nuvem-entra-na-historia/` | Cenário 15 |
| `content/iac/16-o-state-que-morava-num-notebook/` | Cenário 16 |
| `content/iac/17-publica-nao-e-nome-de-subnet/` | Cenário 17 |
| `content/iac/18-o-apply-verde-que-quebrou-a-mirante/` | Cenário 18 |
| `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java` | contagens finais |
| `README.md` | Trilha completa |

---

### Task 1: Fechar os riscos antes de escrever qualquer Cenário

Esta Task é bloqueante. Dois dos seis Cenários dependem de premissas que nunca foram
medidas, e escrever o conteúdo antes de medi-las é o caminho mais curto para reescrever
tudo. Nenhuma outra Task começa antes desta terminar.

**Files:**
- Modify: `docs/research/iac-course.md` (seção nova ao final)

**Interfaces:**
- Consumes: nada.
- Produces: as decisões que as Tasks 2 a 7 assumem — provider Kubernetes funcionando,
  veredito sobre `use_lockfile` e veredito sobre a RAM do Cenário 18.

- [ ] **Step 1: Subir o cluster e o MiniStack**

Em 2026-08-10, `kubectl config get-contexts` nesta máquina não listou contexto nenhum: o
cluster não estava no ar. Siga a seção "Preparando a Trilha Kubernetes" do README, e
depois:

```powershell
kubectl config get-contexts
kubectl --context docker-desktop get nodes
docker ps --filter name=ministack --format '{{.Names}}  {{.Status}}'
```

Esperado: o contexto `docker-desktop` presente, ao menos um Node `Ready`, e o MiniStack no
ar. Se o contexto tiver outro nome, **pare**: todo o Ato IV e as Asserções da Trilha
Kubernetes assumem `docker-desktop`, e mudar isso é decisão de plataforma, não desta Task.

- [ ] **Step 2: Provar o provider Kubernetes 3.2 contra o cluster**

```powershell
$lab = "$env:TEMP\lab-k8s"
if (Test-Path $lab) { Remove-Item -Recurse -Force $lab }
New-Item -ItemType Directory -Force $lab | Out-Null
@'
terraform {
  required_version = "= 1.15.8"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 3.2"
    }
  }
}

provider "kubernetes" {
  config_path    = "~/.kube/config"
  config_context = "docker-desktop"
}

resource "kubernetes_namespace" "lab" {
  metadata {
    name = "lab-iac-validacao"
  }
}
'@ | Out-File -Encoding utf8 "$lab\main.tf"
terraform -chdir="$lab" init
terraform -chdir="$lab" apply -auto-approve
kubectl --context docker-desktop get namespace lab-iac-validacao
terraform -chdir="$lab" plan -detailed-exitcode -input=false -no-color -lock=false
Write-Output "EXITCODE=$LASTEXITCODE"
terraform -chdir="$lab" destroy -auto-approve
Remove-Item -Recurse -Force $lab
```

Esperado: namespace criado, `EXITCODE=0`. Anote a versão exata do provider que o `init`
resolveu.

- [ ] **Step 3: Decidir o Cenário 16 — `use_lockfile` contra o S3 do MiniStack**

Este é **o único risco técnico material da Trilha**, registrado desde a pesquisa
original: a documentação do MiniStack não menciona requisições condicionais nem
`If-None-Match`, que é o mecanismo do lock nativo do backend S3.

```powershell
$e = "http://127.0.0.1:4566"
aws --endpoint-url $e --region us-east-1 --no-sign-request s3api create-bucket --bucket mirante-tfstate
$lab = "$env:TEMP\lab-backend"
if (Test-Path $lab) { Remove-Item -Recurse -Force $lab }
New-Item -ItemType Directory -Force $lab | Out-Null
```

Escreva um `main.tf` com um `docker_container` simples e o bloco de backend:

```hcl
terraform {
  backend "s3" {
    bucket                      = "mirante-tfstate"
    key                         = "mirante/terraform.tfstate"
    region                      = "us-east-1"
    use_lockfile                = true
    use_path_style              = true
    skip_credentials_validation = true
    skip_metadata_api_check     = true
    skip_requesting_account_id  = true
    skip_region_validation      = true
    skip_s3_checksum            = true

    endpoints = {
      s3 = "http://127.0.0.1:4566"
    }
  }
}
```

Então:

```powershell
terraform -chdir="$lab" init
terraform -chdir="$lab" apply -auto-approve
aws --endpoint-url $e --region us-east-1 --no-sign-request s3api list-objects-v2 --bucket mirante-tfstate
```

Esperado: o objeto `mirante/terraform.tfstate` no bucket.

Agora o lock concorrente. Em **dois terminais**, quase ao mesmo tempo:

```powershell
terraform -chdir="$lab" apply -auto-approve
```

**Veredito:**

- Se o segundo falhar com erro de lock, o Cenário 16 é escrito como desenhado, com
  `use_lockfile` e a demonstração de lock concorrente.
- Se os dois passarem, o lock nativo **não** funciona contra este emulador. Acione a
  contingência já registrada: o Cenário 16 ensina o backend S3 **sem** lock nativo, trata
  o lock como conceito, e o texto declara explicitamente o que não pôde ser provado
  localmente. **Não** use lock por DynamoDB como saída — o próprio Terraform está
  descontinuando esse caminho.

Registre o veredito antes de seguir. Todo o texto do Cenário 16 depende dele.

- [ ] **Step 4: Medir a RAM do Cenário 18**

Com o cluster, o MiniStack e alguns containers Docker no ar ao mesmo tempo:

```powershell
docker stats --no-stream --format '{{.Name}}  {{.MemUsage}}'
Get-CimInstance Win32_OperatingSystem | Select-Object @{n='LivreMB';e={[math]::Round($_.FreePhysicalMemory/1024)}}, @{n='TotalMB';e={[math]::Round($_.TotalVisibleMemorySize/1024)}}
```

**Veredito:** se os três substratos simultâneos não couberem com folga, o Cenário 18
encolhe para **dois** substratos — Docker e Kubernetes — e a AWS entra como inventário
escrito, não como infraestrutura no ar. A Task 7 tem os dois desenhos.

- [ ] **Step 5: Rodar o canário de chamada para a AWS real**

O resultado esperado é **zero**, como na Trilha AWS. Use o mesmo procedimento que aquela
Trilha registrou — se ele não estiver documentado, uma checagem suficiente é confirmar que
nenhum `.tf` do conteúdo contém `amazonaws.com` fora de um ARN, e que o provider AWS de
todo Cenário deste Ato aponta para `http://127.0.0.1:4566`:

```powershell
Select-String -Path "content\iac\**\*.tf" -Pattern "amazonaws\.com" -ErrorAction SilentlyContinue
```

Esperado: nenhuma linha. O `GuardrailsDeTerraformTest` cobre o resto.

- [ ] **Step 6: Confirmar as portas**

```powershell
foreach ($p in 8070,8071,30070) {
  $ocupada = Test-NetConnection -ComputerName localhost -Port $p -InformationLevel Quiet -WarningAction SilentlyContinue
  Write-Output "$p ocupada=$ocupada"
}
```

Esperado: as três livres. Se a 30070 estiver ocupada, escolha outro `NodePort` na faixa
30000–32767 e propague para a Task 2.

- [ ] **Step 7: Limpar e registrar**

```powershell
terraform -chdir="$lab" destroy -auto-approve
Remove-Item -Recurse -Force $lab
aws --endpoint-url $e --region us-east-1 --no-sign-request s3 rb s3://mirante-tfstate --force
kubectl --context docker-desktop delete namespace lab-iac-validacao --ignore-not-found
```

Acrescente ao final de `docs/research/iac-course.md`:

```markdown
## Validação do Ato IV, executada em PREENCHER com a data

| Premissa | Resultado |
|---|---|
| contexto `docker-desktop` disponível | PREENCHER |
| provider `hashicorp/kubernetes` resolvido pelo init | PREENCHER com a versão |
| `kubernetes_namespace` criado e plano limpo em seguida | PREENCHER |
| state no backend S3 do MiniStack | PREENCHER |
| **`use_lockfile` com dois apply concorrentes** | PREENCHER com o veredito |
| RAM livre com os três substratos no ar | PREENCHER |
| chamadas para `amazonaws.com` no conteúdo | PREENCHER, esperado zero |
| portas 8070, 8071 e 30070 livres | PREENCHER |
```

Substitua **todos** os `PREENCHER`. Um plano com placeholder committado é o defeito que
esta Task existe para evitar.

```bash
git add docs/research/iac-course.md
git commit -m "docs: fecha os riscos do Ato IV da Trilha IaC"
```

---

### Task 2: Cenário 13 — A Mirante migra para o cluster

**Files:**
- Create: `content/iac/13-a-mirante-migra-para-o-cluster/cenario.md`
- Create: `content/iac/13-a-mirante-migra-para-o-cluster/verificacao.yaml`
- Create: `content/iac/13-a-mirante-migra-para-o-cluster/workspace/versions.tf`
- Create: `content/iac/13-a-mirante-migra-para-o-cluster/workspace/main.tf`

**Interfaces:**
- Consumes: o provider Kubernetes provado na Task 1.
- Produces: o Cenário 13; o `versions.tf` deste Cenário é o modelo dos Cenários 14 e 18.

- [ ] **Step 1: Criar o workspace**

`workspace/versions.tf` — o `config_context` fixo é exigência do
`GuardrailsDeTerraformTest`:

```hcl
terraform {
  required_version = "= 1.15.8"

  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 3.2"
    }
  }
}

provider "kubernetes" {
  config_path    = "~/.kube/config"
  config_context = "docker-desktop"
}
```

`workspace/main.tf` — só o namespace vem pronto; Deployment e Service são o exercício:

```hcl
resource "kubernetes_namespace" "mirante" {
  metadata {
    name = "learning-infra-iac-13"
  }
}
```

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/13-a-mirante-migra-para-o-cluster
titulo: A Mirante migra para o cluster
dificuldade: guiado
terraform: true
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-iac-13
---
```

Estrutura obrigatória:

1. **Abertura.** A Mirante fechou com uma rede de trinta lojas. Um container num host só
   deixou de ser resposta. O cluster o leitor já conhece da Trilha Kubernetes — o que muda
   é quem escreve os objetos.
2. **`## O mesmo HCL, outro provider`.** A ideia central do Ato: nada na linguagem muda.
   Muda o tradutor. Um `resource` continua sendo um `resource`; o provider Kubernetes fala
   com o API server em vez de falar com o daemon do Docker.
3. **`## Por que o contexto é fixo`.** `config_context = "docker-desktop"` não é
   preferência. Um `apply` sem contexto fixo alcança **o cluster que estiver selecionado**,
   e essa é uma das formas mais rápidas de aplicar em produção sem querer. O texto liga
   isso ao que as Asserções da Trilha Kubernetes já fazem.
4. **`## Declare o Deployment`.** Sendo `Guiado`, o HCL vem inteiro, com o
   `kubernetes_deployment_v1` de três réplicas usando `nginx:1.27-alpine`, `selector` e
   `template` coerentes. O texto precisa apontar duas coisas: que o `metadata.namespace`
   referencia `kubernetes_namespace.mirante.metadata[0].name` — e que essa referência é a
   aresta do grafo que garante a ordem — e que `spec.template` aqui é o mesmo template de
   Pod que o leitor já escreveu em YAML.
5. **`## Declare o Service`.** `kubernetes_service_v1` do tipo `NodePort`, com
   `node_port = 30070`, expondo a porta 80. O texto explica por que `NodePort` e não
   `LoadBalancer` num cluster local.
6. **`## Aplique e confira dos dois lados`.**

   ```powershell
   terraform init
   terraform apply
   kubectl --context docker-desktop -n learning-infra-iac-13 get deployment,service
   ```

   O ponto pedagógico: o mesmo objeto aparece no `terraform state list` e no `kubectl get`.
   Duas visões do mesmo fato, uma pela intenção declarada, outra pela realidade do cluster.
7. **`## O que o Terraform não faz aqui`.** Ponte para o Cenário 14 e volta aos
   Fundamentos: o controller do Deployment reconcilia sozinho e para sempre; o Terraform
   reconcilia quando mandam. Os dois laços convivem no mesmo objeto, e é daí que nasce a
   pergunta de quem é o dono.
8. **`## Verificação`.**

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: kubernetes_condicao
    recurso: deployment
    nome: mirante-web
    condicao: Available
    timeout: 20
    descricao: as três réplicas da Mirante estão disponíveis no cluster
  - tipo: kubernetes_jsonpath
    recurso: service
    nome: mirante-web
    expressao: "{.spec.ports[0].nodePort}"
    contem: "30070"
    descricao: o Service publica a porta combinada
  - tipo: http_responde
    url: http://localhost:30070
    status: 200
  - tipo: terraform_estado
    endereco: kubernetes_deployment_v1.web
    atributo: name
    esperado: mirante-web
    descricao: o Deployment nasceu do Terraform, e não de um kubectl apply
  - tipo: terraform_plano_limpo
    descricao: o código descreve os objetos que estão no cluster
```

Os nomes de recurso `kubernetes_deployment_v1.web` e `kubernetes_service_v1.web` e o nome
de objeto `mirante-web` são **exigidos do leitor** no texto do Cenário.

**Confirme no Step 4** que `terraform state show` de um recurso Kubernetes imprime `name`
no formato `chave = valor` que a Asserção espera. Se o atributo estiver aninhado dentro do
bloco `metadata`, a Asserção lê a **primeira** ocorrência de `name` na saída — que pode ser
outra. Se for o caso, troque para `terraform_estado` sem `atributo`, que apenas exige que
o endereço exista no state.

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo. A prova de falso positivo: apague o Deployment com
`kubectl -n learning-infra-iac-13 delete deployment mirante-web` e recrie com
`kubectl create deployment`. As Asserções de Kubernetes podem passar; as duas de Terraform
precisam reprovar. Restaure com `terraform apply`.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/13-a-mirante-migra-para-o-cluster
git commit -m "feat: Cenário 13 da Trilha IaC — o mesmo HCL contra o cluster"
```

---

### Task 3: Cenário 14 — Quem é o dono deste recurso?

**Files:**
- Create: `content/iac/14-quem-e-o-dono-deste-recurso/cenario.md`
- Create: `content/iac/14-quem-e-o-dono-deste-recurso/verificacao.yaml`
- Create: `content/iac/14-quem-e-o-dono-deste-recurso/workspace/versions.tf`
- Create: `content/iac/14-quem-e-o-dono-deste-recurso/workspace/main.tf`

**Interfaces:**
- Consumes: o `versions.tf` do Cenário 13.
- Produces: o Cenário 14.

- [ ] **Step 1: Criar o workspace**

`workspace/versions.tf` é o do Cenário 13, com o namespace `learning-infra-iac-14`.

`workspace/main.tf` entrega namespace, ConfigMap e Deployment completos e aplicáveis: o
exercício deste Cenário é **diagnosticar**, não escrever do zero. O Deployment monta o
ConfigMap como volume e tem três réplicas; o ConfigMap tem uma chave `mensagem` com valor
`v1`.

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/14-quem-e-o-dono-deste-recurso
titulo: Quem é o dono deste recurso?
dificuldade: assistido
terraform: true
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-iac-14
---
```

Estrutura obrigatória:

1. **Abertura.** Duas pessoas mexeram no mesmo Deployment na mesma tarde: uma por
   `terraform apply`, outra por `kubectl edit`. Os dois disseram que funcionou. Um dos
   dois estava errado e não sabia.
2. **`## Aplique e crie o conflito`.** Depois do `apply`, o leitor muda a réplica **por
   fora**:

   ```powershell
   kubectl --context docker-desktop -n learning-infra-iac-14 scale deployment mirante-web --replicas=5
   kubectl --context docker-desktop -n learning-infra-iac-14 get deployment mirante-web
   terraform plan
   ```

   O plano acusa a divergência. O texto nomeia: é o **mesmo drift** do Cenário 05, em outro
   substrato. O triângulo dos Fundamentos não mudou de forma.
3. **`## Onde Kubernetes é diferente`.** A distinção que o Cenário existe para ensinar. No
   Docker, o mundo real só muda quando alguém o muda. No Kubernetes, **um controller muda o
   mundo real o tempo todo** — e legitimamente. Nem toda diferença entre o objeto declarado
   e o objeto no cluster é drift: parte é o `status`, que nunca foi seu para declarar.
4. **`## Campos gerenciados`.**

   ```powershell
   kubectl --context docker-desktop -n learning-infra-iac-14 get deployment mirante-web -o yaml | Select-String -Pattern "managedFields" -Context 0,12
   ```

   Cada cliente que escreve num objeto deixa o nome do seu **field manager**. O texto
   explica que o servidor rastreia quem escreveu o quê, e que é isso que permite dois
   clientes coexistirem sem se sobrescrever cegamente.
5. **`## O ConfigMap que dispara rollout`.** O leitor muda `mensagem` de `v1` para `v2` e
   aplica. O texto pede que ele **preveja** se os Pods vão reiniciar. A resposta honesta
   depende de como o ConfigMap está referenciado — e a técnica que garante o rollout é
   amarrar uma anotação do template ao conteúdo do ConfigMap, para que mudar o dado mude o
   template. Como dica de degrau `Assistido`, o texto aponta a ideia sem entregar o HCL.
6. **`## Um recurso, dois donos`.** Apresenta `kubernetes_config_map_v1_data`, que gerencia
   **apenas as chaves declaradas** de um ConfigMap que já existe, por server-side apply, e
   que conflita quando outro field manager já governa a mesma chave — conflito que
   `force = true` sobrescreve. O texto precisa dizer quando isso é a ferramenta certa
   (adotar uma fatia de um objeto de outro time) e quando é armadilha (`force = true`
   usado para calar um conflito real).
7. **`## Reconcilie`.** `terraform apply` traz tudo de volta ao declarado.
8. **`## Verificação`.**

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: kubernetes_condicao
    recurso: deployment
    nome: mirante-web
    condicao: Available
    timeout: 20
    descricao: o Deployment está disponível depois da reconciliação
  - tipo: kubernetes_jsonpath
    recurso: deployment
    nome: mirante-web
    expressao: "{.spec.replicas}"
    contem: "3"
    descricao: o número de réplicas voltou ao que o código declara
  - tipo: kubernetes_jsonpath
    recurso: configmap
    nome: mirante-config
    expressao: "{.data.mensagem}"
    contem: v2
    descricao: a configuração nova chegou ao cluster
  - tipo: terraform_estado
    endereco: kubernetes_config_map_v1.config
    descricao: o ConfigMap está sob gestão do Terraform
  - tipo: terraform_plano_limpo
    descricao: não sobrou divergência entre o código e o cluster
```

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo. A prova específica: com tudo verde, rode o `kubectl scale --replicas=5` de
novo e verifique — `terraform_plano_limpo` reprova e a Asserção de `spec.replicas` também.
É a demonstração de que a Verificação enxerga drift em Kubernetes.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/14-quem-e-o-dono-deste-recurso
git commit -m "feat: Cenário 14 da Trilha IaC — drift, campos gerenciados e propriedade"
```

---

### Task 4: Cenário 15 — A nuvem entra na história

**Files:**
- Create: `content/iac/15-a-nuvem-entra-na-historia/cenario.md`
- Create: `content/iac/15-a-nuvem-entra-na-historia/verificacao.yaml`
- Create: `content/iac/15-a-nuvem-entra-na-historia/workspace/versions.tf`
- Create: `content/iac/15-a-nuvem-entra-na-historia/workspace/main.tf`

**Interfaces:**
- Consumes: o MiniStack confirmado na Task 1.
- Produces: o `versions.tf` com o provider AWS local, reusado pelos Cenários 16, 17 e 18.

- [ ] **Step 1: Criar o workspace**

`workspace/versions.tf` — este bloco é o que o `GuardrailsDeTerraformTest` exige de todo
`.tf` com provider AWS da Trilha:

```hcl
terraform {
  required_version = "= 1.15.8"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.58"
    }
  }
}

provider "aws" {
  region                      = "us-east-1"
  access_key                  = "local"
  secret_key                  = "local"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    s3       = "http://127.0.0.1:4566"
    sqs      = "http://127.0.0.1:4566"
    dynamodb = "http://127.0.0.1:4566"
  }
}
```

`workspace/main.tf` vem **vazio** — apenas um comentário dizendo que os três recursos são o
exercício.

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/15-a-nuvem-entra-na-historia
titulo: A nuvem entra na história
dificuldade: assistido
terraform: true
ministack: true
---
```

Estrutura obrigatória:

1. **Abertura.** A Mirante precisa guardar as notas fiscais dos clientes, processar
   pedidos em fila e consultar estoque por chave. Na Trilha AWS o leitor fez isso pela
   CLI; aqui os mesmos recursos nascem de arquivo versionado.
2. **`## O provider que aponta para o seu computador`.** Explica cada linha do
   `provider "aws"`: por que `endpoints` por serviço, por que os três `skip_*` — as
   validações batem em serviços de identidade que o emulador não implementa — e por que as
   credenciais são sintéticas. Precisa dizer que **esse bloco é a fronteira de segurança**
   desta Trilha e que o repositório tem um teste que reprova qualquer `.tf` sem ele.
3. **`## Três recursos`.** Objetivo: um bucket S3 `mirante-notas` com versionamento, uma
   fila SQS `mirante-pedidos` e uma tabela DynamoDB `mirante-estoque` com chave de partição
   `sku`. Como dica de degrau `Assistido`, o texto dá os **nomes dos recursos**
   (`aws_s3_bucket`, `aws_s3_bucket_versioning`, `aws_sqs_queue`, `aws_dynamodb_table`) e
   deixa os argumentos por conta do leitor.
4. **`## Um recurso ou dois?`.** Ponto que confunde de verdade: versionamento de bucket é
   um **recurso separado** no provider AWS moderno, não um argumento do bucket. O texto usa
   isso para uma lição maior — a modelagem do provider nem sempre espelha a tela do console,
   e ler a documentação do recurso é parte do trabalho.
5. **`## Confira pelos dois caminhos`.** `terraform state list` e a AWS CLI apontada para o
   endpoint local. O mesmo recurso, duas visões.
6. **`## O que este Cenário não prova`.** Obrigatório, e sem suavizar. Pela matriz de
   fidelidade: S3, SQS e DynamoDB têm data plane real, então gravar e ler funciona de
   verdade. Nada aqui prova IAM aplicado, isolamento de rede, custo ou disponibilidade.
   O Cenário 17 vai chegar num caso em que a fidelidade cai bem mais.
7. **`## Verificação`.**

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: aws_consulta
    servico: s3api
    operacao: get-bucket-versioning
    argumentos: ["--bucket", "mirante-notas"]
    consulta: Status
    esperado: Enabled
    descricao: o bucket de notas mantém histórico de versões
  - tipo: aws_consulta
    servico: sqs
    operacao: get-queue-url
    argumentos: ["--queue-name", "mirante-pedidos"]
    consulta: QueueUrl
    esperado: mirante-pedidos
    descricao: a fila de pedidos existe
  - tipo: aws_consulta
    servico: dynamodb
    operacao: describe-table
    argumentos: ["--table-name", "mirante-estoque"]
    consulta: "Table.KeySchema[0].AttributeName"
    esperado: sku
    descricao: a tabela de estoque usa o SKU como chave de partição
  - tipo: terraform_estado
    endereco: aws_dynamodb_table.estoque
    atributo: name
    esperado: mirante-estoque
    descricao: a tabela nasceu do código
  - tipo: terraform_plano_limpo
    descricao: os três recursos convergiram
```

Confirme o formato de `esperado` do `aws_consulta` contra os exemplos da Trilha AWS —
`get-queue-url` devolve uma URL completa, e a Asserção precisa comparar do jeito que o
`MotorDeVerificacao` compara. Se ela exigir igualdade exata, troque a consulta por algo
determinístico ou use `list-queues` com `length(QueueUrls)`.

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo, mais a prova de caminho errado: crie os três recursos pela AWS CLI, sem
Terraform. As três Asserções de `aws_consulta` passam e as duas de Terraform reprovam.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/15-a-nuvem-entra-na-historia
git commit -m "feat: Cenário 15 da Trilha IaC — S3, SQS e DynamoDB declarados"
```

---

### Task 5: Cenário 16 — O state que morava num notebook

**Files:**
- Create: `content/iac/16-o-state-que-morava-num-notebook/cenario.md`
- Create: `content/iac/16-o-state-que-morava-num-notebook/verificacao.yaml`
- Create: `content/iac/16-o-state-que-morava-num-notebook/workspace/versions.tf`
- Create: `content/iac/16-o-state-que-morava-num-notebook/workspace/main.tf`
- Create: `content/iac/16-o-state-que-morava-num-notebook/workspace/preparar.ps1`

**Interfaces:**
- Consumes: **o veredito do Step 3 da Task 1.** O texto deste Cenário muda conforme o
  `use_lockfile` funcione ou não contra o MiniStack.
- Produces: o Cenário 16.

- [ ] **Step 1: Criar o workspace**

`workspace/versions.tf` é o do Cenário 15, sem bloco `backend` — migrar é o exercício.

`workspace/preparar.ps1` cria o bucket de state, porque o bucket que guarda o state não
pode nascer do mesmo Terraform que o usa:

```powershell
# O bucket que guarda o state não pode ser criado pelo Terraform que o usa.
$e = "http://127.0.0.1:4566"
aws --endpoint-url $e --region us-east-1 --no-sign-request s3api create-bucket --bucket mirante-tfstate | Out-Null
aws --endpoint-url $e --region us-east-1 --no-sign-request s3api put-bucket-versioning --bucket mirante-tfstate --versioning-configuration Status=Enabled
Write-Output "bucket de state pronto: mirante-tfstate"
```

`workspace/main.tf` traz um recurso simples já aplicável — uma fila SQS `mirante-pedidos`
basta. O assunto é o state, não o recurso.

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/16-o-state-que-morava-num-notebook
titulo: O state que morava num notebook
dificuldade: autonomo
terraform: true
ministack: true
---
```

Sendo `Autônomo`, entrega objetivo e ambiente. Estrutura:

1. **Abertura.** Fecha o arco aberto no Cenário 06 e cobrado no Questionário: a
   infraestrutura da Mirante está descrita em código versionado, e o state continua num
   diretório de uma máquina só. Dois `apply` simultâneos de duas pessoas nunca aconteceram
   porque só uma pessoa aplica — e isso não é uma arquitetura, é uma sorte.
2. **`## O ambiente`.** Rodar `.\preparar.ps1` e `terraform init && terraform apply`, para
   que exista state local a migrar.
3. **`## O objetivo`.** O state precisa terminar como objeto dentro do bucket
   `mirante-tfstate`, sem que nenhum recurso seja recriado, e o `plan` precisa ficar limpo
   em seguida.
4. **`## A pista`.** Uma só: o bloco `backend` mora dentro de `terraform { }`, e o `init`
   tem uma opção que oferece migrar o state existente em vez de começar do zero. O leitor
   descobre o resto. O texto avisa que o `init` **vai perguntar** e que ler a pergunta é
   parte do exercício.
5. **`## O que o endpoint local exige`.** Entregue, porque é configuração de ambiente e não
   conhecimento: `use_path_style`, `skip_region_validation`, `skip_credentials_validation`,
   `skip_metadata_api_check`, `skip_requesting_account_id`, `skip_s3_checksum` e o bloco
   `endpoints` com `s3`. O `skip_s3_checksum` existe justamente por causa de
   implementações S3-compatíveis.
6. **`## Lock`** — **esta seção depende do veredito da Task 1**:
   - **Se o lock nativo funcionou:** o leitor liga `use_lockfile = true`, roda dois `apply`
     concorrentes em dois terminais e vê o segundo ser recusado. O texto explica o
     mecanismo — um `PutObject` condicional que só cria o `.tflock` se ele ainda não
     existir — e registra que o lock por DynamoDB está descontinuado.
   - **Se o lock nativo não funcionou:** o texto ensina `use_lockfile` como o mecanismo
     correto, mostra a configuração, e declara **explicitamente** que este emulador não
     implementa a escrita condicional que o lock exige, então a demonstração de
     concorrência não pôde ser feita localmente. Registra o que aconteceria numa conta
     real. **Não** oferece DynamoDB como alternativa.

   Em qualquer dos dois casos a seção termina no mesmo lugar: state remoto sem lock é uma
   corrida esperando acontecer, e o dano de dois `apply` simultâneos é state corrompido —
   que é pior do que infraestrutura errada, porque você perde a capacidade de consertar.
7. **`## O que mudou para o time`.** State compartilhado, versionado no bucket, com acesso
   restrito. Fecha o assunto que o Cenário 06 abriu.
8. **`## Verificação`.**

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: aws_consulta
    servico: sqs
    operacao: list-queues
    argumentos: ["--queue-name-prefix", "mirante-pedidos"]
    consulta: "length(QueueUrls)"
    esperado: "1"
    descricao: a fila continua no ar depois da migração
  - tipo: aws_consulta
    servico: s3api
    operacao: list-objects-v2
    argumentos: ["--bucket", "mirante-tfstate", "--prefix", "mirante/"]
    consulta: "length(Contents)"
    esperado: "1"
    descricao: o state migrou para o bucket
  - tipo: comando_produz
    comando: ["powershell.exe", "-NoProfile", "-Command", "if (Test-Path ../work/terraform.tfstate) { 'state-local-ainda-existe' } else { 'sem-state-local' }"]
    contem: sem-state-local
    descricao: o state deixou de morar no diretório de trabalho
  - tipo: terraform_estado
    endereco: aws_sqs_queue.pedidos
    descricao: o Terraform continua reconhecendo a fila depois da migração
  - tipo: terraform_plano_limpo
    descricao: a migração não deixou mudança pendente
```

A chave `mirante/terraform.tfstate` é **exigida do leitor** na seção do objetivo.

**Atenção:** o `init -migrate-state` deixa um `terraform.tfstate.backup` para trás. Se a
Asserção do state local reprovar por causa dele, ajuste o `Test-Path` para o arquivo exato
`terraform.tfstate`, e não para um glob.

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo. A prova específica: depois da migração, apague o diretório `.terraform` e
rode `terraform init` de novo — o state vem do bucket, e o `plan` sai limpo sem nenhum
arquivo local. É a demonstração de que o state deixou de ser propriedade de uma máquina.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/16-o-state-que-morava-num-notebook
git commit -m "feat: Cenário 16 da Trilha IaC — backend S3, migração de state e lock"
```

---

### Task 6: Cenário 17 — Pública não é nome de subnet

**Files:**
- Create: `content/iac/17-publica-nao-e-nome-de-subnet/cenario.md`
- Create: `content/iac/17-publica-nao-e-nome-de-subnet/verificacao.yaml`
- Create: `content/iac/17-publica-nao-e-nome-de-subnet/workspace/versions.tf`
- Create: `content/iac/17-publica-nao-e-nome-de-subnet/workspace/main.tf`

**Interfaces:**
- Consumes: o `versions.tf` do Cenário 15, acrescido do endpoint `ec2`.
- Produces: o Cenário 17.

Este Cenário é o **espelho declarativo do Cenário 08 da Trilha AWS**, de propósito: o
leitor reencontra um problema que já resolveu na CLI e vê o que muda quando a fonte de
verdade é um artefato versionado.

- [ ] **Step 1: Criar o workspace**

`workspace/versions.tf` é o do Cenário 15 com `ec2 = "http://127.0.0.1:4566"` acrescentado
ao bloco `endpoints`. `workspace/main.tf` entrega apenas a VPC e uma subnet, e o leitor
constrói o resto:

```hcl
resource "aws_vpc" "mirante" {
  cidr_block           = "10.60.0.0/16"
  enable_dns_hostnames = true

  tags = {
    Name = "mirante"
  }
}

resource "aws_subnet" "publica_a" {
  vpc_id            = aws_vpc.mirante.id
  cidr_block        = "10.60.1.0/24"
  availability_zone = "us-east-1a"

  tags = {
    Name = "mirante-publica-a"
  }
}
```

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/17-publica-nao-e-nome-de-subnet
titulo: Pública não é nome de subnet
dificuldade: autonomo
terraform: true
ministack: true
---
```

Sendo `Autônomo`, entrega objetivo e ambiente. Estrutura:

1. **Abertura.** Uma auditoria perguntou quais recursos da Mirante são alcançáveis da
   internet. A resposta que veio foi o nome das subnets. O nome não é a resposta.
2. **`## O objetivo`.** Duas subnets públicas em zonas diferentes, uma privada, um Internet
   Gateway, uma route table com rota default para o IGW **associada apenas às públicas**, e
   um security group. Ao final, o leitor precisa conseguir provar pela associação de rota —
   não pela tag — quais subnets são públicas.
3. **`## A pista`.** Uma só: a diferença entre uma subnet pública e uma privada não está em
   nenhum atributo da subnet. Está em outro objeto, que aponta para ela.
4. **`## O que o Terraform muda aqui`.** A comparação com a Trilha AWS, que é a razão de o
   Cenário existir. Na CLI, cada `create-*` é um comando isolado e a relação entre eles
   mora na cabeça de quem digitou. No Terraform, `aws_route_table_association` **é** a
   relação, escrita e versionada. A pergunta da auditoria vira uma busca no repositório.
5. **`## O aviso de fidelidade`.** Obrigatório e categórico, pela matriz do MiniStack: VPC
   é **somente control plane**. Nenhuma rota encaminha pacote, nenhum NACL ou SG filtra
   tráfego. Este Cenário prova que o leitor **declarou** a topologia certa. Ele não prova,
   e não pode alegar, que a rede isola coisa alguma. É o contraste mais forte da Trilha:
   fidelidade total nos Cenários de Docker, quase nenhuma aqui, e o mesmo HCL nos dois.
6. **`## Verificação`.**

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: aws_consulta
    servico: ec2
    operacao: describe-route-tables
    argumentos: ["--filters", "Name=tag:Name,Values=mirante-publica"]
    consulta: "RouteTables[0].Routes[?DestinationCidrBlock=='0.0.0.0/0'].GatewayId | [0]"
    esperado: igw-
    descricao: a rota default das subnets públicas aponta para o Internet Gateway
  - tipo: aws_consulta
    servico: ec2
    operacao: describe-route-tables
    argumentos: ["--filters", "Name=tag:Name,Values=mirante-publica"]
    consulta: "length(RouteTables[0].Associations)"
    esperado: "2"
    descricao: exatamente as duas subnets públicas estão associadas a essa route table
  - tipo: terraform_estado
    endereco: aws_internet_gateway.mirante
    descricao: o gateway nasceu do código
  - tipo: terraform_estado
    endereco: aws_route_table_association.publica_a
    descricao: a associação que torna a subnet pública está versionada
  - tipo: terraform_plano_limpo
    descricao: a topologia declarada é a que está no control plane
```

Confirme se o `esperado: igw-` funciona como comparação parcial no `MotorDeVerificacao`. Se
a comparação for por igualdade exata, troque a consulta por
`length(RouteTables[0].Routes[?DestinationCidrBlock=='0.0.0.0/0'])` com `esperado: "1"`.

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo. A prova específica deste Cenário: associe a subnet **privada** à route
table pública e verifique — a Asserção de contagem reprova. É a demonstração de que a
Verificação mede associação, e não nome.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/17-publica-nao-e-nome-de-subnet
git commit -m "feat: Cenário 17 da Trilha IaC — VPC, rotas e o que o nome não prova"
```

---

### Task 7: Cenário 18 — O apply verde que quebrou a Mirante

**Files:**
- Create: `content/iac/18-o-apply-verde-que-quebrou-a-mirante/cenario.md`
- Create: `content/iac/18-o-apply-verde-que-quebrou-a-mirante/verificacao.yaml`
- Create: `content/iac/18-o-apply-verde-que-quebrou-a-mirante/workspace/` com os `.tf` quebrados
- Create: `content/iac/18-o-apply-verde-que-quebrou-a-mirante/workspace/preparar.ps1`

**Interfaces:**
- Consumes: **o veredito de RAM do Step 4 da Task 1** e tudo que os dezessete Cenários
  anteriores ensinaram.
- Produces: o Cenário final da Trilha e da plataforma.

- [ ] **Step 1: Escolher o desenho conforme a RAM medida**

- **Se os três substratos couberem:** Docker, Kubernetes e AWS no ar ao mesmo tempo, com
  as quatro falhas combinadas do desenho original.
- **Se não couberem:** o Cenário encolhe para **Docker e Kubernetes**, com três falhas, e a
  parte de AWS vira **inventário escrito** — o leitor descreve o que investigaria e o que
  a fidelidade local não provaria, sem infraestrutura no ar. A Verificação perde as
  Asserções de `aws_consulta` e o texto declara a redução com franqueza.

Registre a escolha no topo do `cenario.md`, numa nota de uma linha. Não deixe o leitor
descobrir sozinho que um substrato sumiu.

- [ ] **Step 2: Montar o workspace quebrado**

Sendo `Mestre`, o ambiente vem quebrado e **a causa não é revelada**. As quatro falhas,
todas plantadas de forma que o `apply` termine em verde:

1. **`moved` faltando.** Um recurso Docker foi renomeado no código sem bloco `moved`. O
   plano propõe destruir e recriar um volume com dados. Se o leitor aprovar sem ler, perde
   o dado — e a Asserção que confere o conteúdo do volume reprova.
2. **Módulo com variável errada.** O módulo de ambiente recebe a porta de produção no
   ambiente de homologação, e vice-versa. Tudo sobe, tudo responde, e cada um responde no
   lugar do outro.
3. **Drift em Kubernetes.** O `preparar.ps1` aplica, por fora, uma mudança de réplicas com
   `kubectl scale`. O `terraform plan` acusa; o `apply` verde de quem não planejou, não.
4. **Recurso AWS órfão do state.** O `preparar.ps1` cria uma fila SQS pela CLI, com o nome
   que o código espera. O `apply` falha por conflito, ou pior, o código passa a apontar
   para um recurso que ele não gerencia. A saída é `import`.

`workspace/preparar.ps1` planta as falhas 3 e 4; as falhas 1 e 2 já estão nos `.tf`
entregues.

- [ ] **Step 3: Escrever o Cenário**

Frontmatter exato — é o único Cenário da plataforma que declara os três substratos:

```yaml
---
id: iac/18-o-apply-verde-que-quebrou-a-mirante
titulo: O apply verde que quebrou a Mirante
dificuldade: mestre
terraform: true
containers: [mirante-homologacao, mirante-producao]
volumes: [mirante-estoque]
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-iac-18
ministack: true
---
```

Estrutura obrigatória, e curta — degrau `Mestre` fala pouco:

1. **Abertura.** O `apply` da sexta-feira terminou em verde. Na segunda, a Mirante estava
   quebrada em quatro lugares. Nada falhou; tudo aplicou.
2. **`## O ambiente`.** Rodar `.\preparar.ps1`. Nada além disso.
3. **`## O objetivo`.** Os critérios de saída, sem dizer o que está errado: os dois
   ambientes respondem cada um na sua porta; o volume de estoque preserva o dado que já
   estava lá; o cluster tem o número de réplicas que o código declara; a fila da Mirante
   está sob gestão do Terraform; e `terraform plan` sai limpo.
4. **`## A única dica`.** O plano é o instrumento. Quatro problemas diferentes deixam
   quatro marcas diferentes no plano, e todas aparecem antes de qualquer `apply`.
5. **`## O inventário de fidelidade`.** A seção que fecha a Trilha e a plataforma. O leitor
   escreve, com as próprias palavras, o que foi provado localmente e o que ainda exige uma
   conta sandbox: ciclo de vida real de container e objeto Kubernetes de um lado; do outro,
   IAM aplicado, isolamento de rede, custo, disponibilidade e comportamento sob carga.
   Fecha com a frase que resume as quatro Trilhas: saber o que o seu laboratório não prova é
   parte de saber operar infraestrutura.

- [ ] **Step 4: Escrever a Verificação**

Uma Asserção por falha plantada, mais as duas de Terraform. Se o desenho encolheu no Step
1, remova as de AWS.

```yaml
asercoes:
  - tipo: http_responde
    url: http://localhost:8071
    status: 200
  - tipo: comando_produz
    comando: ["docker", "exec", "mirante-producao", "cat", "/var/lib/mirante/estoque.txt"]
    contem: pedido-4711
    descricao: o dado sobreviveu ao refactor
  - tipo: kubernetes_jsonpath
    recurso: deployment
    nome: mirante-web
    expressao: "{.spec.replicas}"
    contem: "3"
    descricao: o cluster voltou ao que o código declara
  - tipo: aws_consulta
    servico: sqs
    operacao: list-queues
    argumentos: ["--queue-name-prefix", "mirante-pedidos"]
    consulta: "length(QueueUrls)"
    esperado: "1"
    descricao: existe exatamente uma fila, e não uma duplicata
  - tipo: terraform_estado
    endereco: aws_sqs_queue.pedidos
    descricao: a fila criada à mão foi adotada, e não recriada
  - tipo: terraform_estado
    endereco: docker_volume.estoque
    descricao: o volume está no endereço novo do state
  - tipo: terraform_plano_limpo
    descricao: nenhuma das quatro divergências sobrou
```

- [ ] **Step 5: Provar na aplicação real**

Ciclo completo, com atenção especial ao tempo: com os três substratos no ar, confirme que
**cada** Asserção responde dentro do timeout de 30 s do executor. Se alguma estourar,
aumente o `timeout` das Asserções Kubernetes antes de mexer no executor.

Faça também o Cenário do começo ao fim **sem consultar os planos**, como um leitor faria.
Se alguma das quatro falhas for indecifrável sem conhecimento que a Trilha não deu, ela
está mal plantada — ajuste a falha, não o texto.

- [ ] **Step 6: Commitar**

```bash
git add content/iac/18-o-apply-verde-que-quebrou-a-mirante
git commit -m "feat: Cenário 18 da Trilha IaC — o incidente final da plataforma"
```

---

### Task 8: Fechar a Trilha

**Files:**
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-08-09-trilha-iac-design.md`

**Interfaces:**
- Consumes: os seis Cenários das Tasks 2 a 7.
- Produces: nada consumido por código.

- [ ] **Step 1: Atualizar as contagens**

Total: **61**. Grupo `iac`: **18**. Some as Asserções dos seis Cenários novos ao total de
67 do fim do Ato III e escreva o número **contado**, não estimado:

```java
        assertThat(cenarios).hasSize(61);
```

```java
        assertThat(iac).hasSize(18);
        assertThat(iac).allMatch(Cenario::terraform);
```

Acrescente também a distribuição de Dificuldade, que o design fixou — quatro `Guiado`,
oito `Assistido`, cinco `Autônomo` e um `Mestre`:

```java
        assertThat(iac).filteredOn(cenario -> cenario.dificuldade() == Dificuldade.MESTRE)
                .hasSize(1);
```

- [ ] **Step 2: Rodar tudo**

Run: `cd backend && ./mvnw test`
Run: `cd frontend && npm run build && node scripts-checar-conteudo.mjs iac`
Expected: PASS nos três.

- [ ] **Step 3: Atualizar o README**

Na seção de portas, feche a frase da Trilha IaC com o `NodePort` 30070 do Cenário 13 e a
8071 do Cenário 18.

Na seção "### Preparando a Trilha IaC", acrescente que os Cenários 13, 14 e 18 exigem o
cluster `docker-desktop` no ar e que os Cenários 15 a 18 exigem o MiniStack — os mesmos
pré-requisitos das Trilhas Kubernetes e AWS, que o leitor já preparou.

Na seção "## Fundamentos e evolução das Trilhas", a Trilha IaC deixa de ser "em
construção": as quatro Trilhas estão completas.

- [ ] **Step 4: Marcar o design como implementado**

No topo de `docs/superpowers/specs/2026-08-09-trilha-iac-design.md`, troque a linha de
estado por:

```markdown
> Estado: implementado nas Etapas 1 a 6
```

Se alguma decisão do design mudou durante a implementação — o desenho do Cenário 07 por
causa do `import`, o do 16 por causa do `use_lockfile`, o do 18 por causa da RAM —
acrescente uma seção curta ao final do spec registrando o que mudou e por quê. O design
não pode ficar descrevendo uma Trilha que não foi construída.

- [ ] **Step 5: Commitar**

```bash
git add backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java README.md docs/superpowers/specs/2026-08-09-trilha-iac-design.md
git commit -m "docs: Trilha IaC completa — 18 Cenários em quatro atos"
```

---

## Definição de pronto

- `cd backend && ./mvnw test` passa, com o catálogo em 61 Cenários e 18 na Trilha `iac`.
- `cd frontend && npm run build` e `node frontend/scripts-checar-conteudo.mjs iac` passam.
- A Task 1 foi executada por inteiro e `docs/research/iac-course.md` tem a tabela do Ato IV
  sem nenhum `PREENCHER` — incluindo o veredito do `use_lockfile` e o da RAM.
- Os seis Cenários iniciam, reprovam antes e aprovam depois.
- O Cenário 13 tem a prova de falso positivo por `kubectl create deployment`; o 14, por
  `kubectl scale`; o 15, por criação via CLI; o 17, por associação de rota errada.
- O Cenário 18 foi resolvido uma vez sem consultar os planos.
- Nenhum `.tf` do conteúdo alcança AWS real, e o `GuardrailsDeTerraformTest` está verde
  sobre todos eles.
- O spec de design está marcado como implementado, com as divergências registradas.
