# Trilha IaC — Etapa 4: Ato II, Cenários 06 a 09

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fechar o Ato II — o código sobrevive às pessoas — com os Cenários 06 a 09:
state e segredos, adoção de infraestrutura órfã por `import`, extração de módulo com
`for_each` e refatoração com `moved` sem destruir dados.

**Architecture:** O Ato II exige **uma** mudança de plataforma, descoberta na validação de
2026-08-10: no Windows, o `ProcessBuilder` do Java entrega ao Terraform um endereço de
módulo com `for_each` sem as aspas, e o `terraform state show` responde
`Error parsing instance address`. Sem escapar as aspas, a evidência prevista para o
Cenário 08 não funciona. A correção é um helper puro no `MotorDeVerificacao`, testável nos
dois sistemas operacionais sem depender de qual está rodando. O resto do Ato é conteúdo.

**Tech Stack:** Java 25 · Spring Boot 4.0.0 · JUnit 5 · AssertJ · Terraform 1.15.8 ·
`kreuzwerker/docker` ~> 4.5

## Global Constraints

- **Todas as restrições das Etapas [1](./2026-08-09-trilha-iac-etapa-1-plataforma.md) a
  [3](./2026-08-10-trilha-iac-etapa-3-ato-i.md) continuam valendo.**
- **Portas:** 06 → **8076**; 07 → **nenhuma porta publicada**, por imposição técnica
  descrita abaixo; 08 → **8077** e **8078**; 09 → **8079**.
- Imagem única: **`nginx:1.27-alpine`**.
- **`import` de container só converge sem recriação quando o container não publica portas,
  não monta volumes e a configuração declara `env = []`.** Medido em 2026-08-10 e
  registrado em [`docs/research/iac-course.md`](../../research/iac-course.md): o `Read` do
  provider Docker não recupera `ports`, `volumes` nem `env`, e os três forçam recriação.
  O Cenário 07 é desenhado em cima dessa limitação, não contra ela.
- **`terraform_plano_limpo` sai com código 2 depois de um bloco `moved` e antes do
  `apply`**, mesmo com o plano vazio. O Cenário 09 precisa instruir o `apply` e o texto
  precisa explicar por quê.
- Nenhum segredo real entra no conteúdo. A senha do Cenário 06 é literalmente
  `senha-de-exemplo-nao-use`.
- A Verificação continua **estritamente observadora**.
- Todo texto em **português**; personagens por papel, nunca por pronome.

## Estrutura de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java` | escapa aspas do endereço no Windows |
| `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java` | os dois ramos do escape, sem depender do SO |
| `content/iac/06-o-arquivo-que-nao-pode-ir-para-o-git/` | Cenário 06 |
| `content/iac/07-a-infra-que-nasceu-a-mao/` | Cenário 07 |
| `content/iac/08-tres-ambientes-um-copy-paste/` | Cenário 08, com `workspace/modulos/servico/` |
| `content/iac/09-o-rename-que-derrubou-o-banco/` | Cenário 09 |
| `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java` | contagens |
| `README.md` | portas e estado da Trilha |

---

### Task 1: Endereço de módulo atravessa o Windows

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Test: `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java`

**Interfaces:**
- Consumes: `Assercao.TerraformEstado` e `diretorioDoTerraform(String)`, ambos da Etapa 1.
- Produces: o método estático de pacote
  `MotorDeVerificacao.enderecoParaLinhaDeComando(String endereco, boolean windows)`,
  que devolve o endereço com `"` trocado por `\"` quando `windows` é verdadeiro e
  inalterado caso contrário. A Task 4 depende dele para a evidência do Cenário 08.

- [ ] **Step 1: Escrever os testes que falham**

Acrescente a `MotorDeVerificacaoTest`. Os dois primeiros testam o helper puro nos dois
ramos, sem depender do sistema operacional em que a suíte roda; o terceiro prova que o
avaliador de fato usa o helper, capturando o comando que chega ao executor:

```java
    @Test
    void enderecoDeModuloGanhaAspasEscapadasNoWindows() {
        String escapado = MotorDeVerificacao.enderecoParaLinhaDeComando(
                "module.ambiente[\"producao\"].docker_container.web", true);

        assertEquals("module.ambiente[\\\"producao\\\"].docker_container.web", escapado);
    }

    @Test
    void enderecoDeModuloFicaIntactoForaDoWindows() {
        String cru = "module.ambiente[\"producao\"].docker_container.web";

        assertEquals(cru, MotorDeVerificacao.enderecoParaLinhaDeComando(cru, false));
    }

    @Test
    void enderecoSemAspasNaoEAlterado() {
        assertEquals("docker_container.web",
                MotorDeVerificacao.enderecoParaLinhaDeComando("docker_container.web", true));
    }

    @Test
    void avaliadorDeEstadoEntregaOEnderecoAoExecutor() {
        List<List<String>> recebidos = new java.util.ArrayList<>();
        ExecutorDeComando espiao = comando -> {
            recebidos.add(comando);
            return new SaidaDeComando(0, ESTADO_DO_CONTAINER, "");
        };

        new MotorDeVerificacao(espiao, java.time.Duration.ofSeconds(1), "../work")
                .verificar(List.of(new Assercao.TerraformEstado(
                        ".", "module.ambiente[\"producao\"].docker_container.web",
                        null, null, "o container do módulo está no state")));

        String enderecoEnviado = recebidos.getFirst().getLast();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        assertEquals(
                MotorDeVerificacao.enderecoParaLinhaDeComando(
                        "module.ambiente[\"producao\"].docker_container.web", windows),
                enderecoEnviado);
    }
```

O `ESTADO_DO_CONTAINER` já existe na classe desde a Etapa 1.

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw test -Dtest=MotorDeVerificacaoTest`
Expected: FAIL — `enderecoParaLinhaDeComando` não existe, a compilação quebra.

- [ ] **Step 3: Implementar o helper**

Em `MotorDeVerificacao.java`, acrescente ao lado de `diretorioDoTerraform`:

```java
    /**
     * O `terraform state list` devolve endereços com aspas embutidas, como
     * {@code module.ambiente["producao"].docker_container.web}. No Windows o
     * {@link ProcessBuilder} só cita argumentos com espaço, então as aspas atravessam a
     * linha de comando cruas e o runtime do Go as desfaz antes de o Terraform ver o
     * argumento — que chega como {@code module.ambiente[producao]...} e é recusado.
     * Escapar com barra invertida é o que sobrevive à viagem. Fora do Windows não há
     * linha de comando intermediária e o endereço vai como está.
     */
    static String enderecoParaLinhaDeComando(String endereco, boolean windows) {
        return windows ? endereco.replace("\"", "\\\"") : endereco;
    }
```

- [ ] **Step 4: Usar o helper no avaliador**

Em `avaliarEstadoTerraform`, troque a montagem do comando:

```java
        SaidaDeComando saida = executor.executar(List.of(
                "terraform", "-chdir=" + diretorio, "state", "show", "-no-color",
                enderecoParaLinhaDeComando(a.endereco(), noWindows())));
```

E acrescente o predicado, ao lado do helper:

```java
    private static boolean noWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
```

A mensagem de falha continua citando `a.endereco()` — o leitor precisa ver o endereço que
ele escreveu, não a forma escapada.

- [ ] **Step 5: Rodar e ver passar**

Run: `cd backend && ./mvnw test -Dtest=MotorDeVerificacaoTest`
Expected: PASS.

- [ ] **Step 6: Provar contra o Terraform de verdade**

O teste unitário prova o escape; só um Terraform real prova que o escape é o certo.

```powershell
$lab = "$env:TEMP\lab-endereco"
if (Test-Path $lab) { Remove-Item -Recurse -Force $lab }
New-Item -ItemType Directory -Force "$lab\modulos\servico" | Out-Null
```

Monte um módulo mínimo com `for_each` — o `main.tf` da raiz com um `module` iterando
`{ producao = 8079 }` e o módulo filho criando um `docker_container`, **com o próprio
bloco `required_providers`**. Aplique, e então:

```powershell
terraform -chdir="$lab" state show -no-color 'module.ambiente[\"producao\"].docker_container.web' | Select-String 'name '
```

Esperado: a linha `name = "mirante-producao"` e código de saída 0. Depois
`terraform -chdir="$lab" destroy -auto-approve` e `Remove-Item -Recurse -Force $lab`.

- [ ] **Step 7: Commitar**

```bash
git add backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java
git commit -m "fix: terraform_estado alcança endereço de módulo no Windows"
```

---

### Task 2: Cenário 06 — O arquivo que não pode ir para o Git

**Files:**
- Create: `content/iac/06-o-arquivo-que-nao-pode-ir-para-o-git/cenario.md`
- Create: `content/iac/06-o-arquivo-que-nao-pode-ir-para-o-git/verificacao.yaml`
- Create: `content/iac/06-o-arquivo-que-nao-pode-ir-para-o-git/workspace/versions.tf`
- Create: `content/iac/06-o-arquivo-que-nao-pode-ir-para-o-git/workspace/main.tf`
- Create: `content/iac/06-o-arquivo-que-nao-pode-ir-para-o-git/workspace/variables.tf`

**Interfaces:**
- Consumes: nada da Task 1.
- Produces: o Cenário 06.

- [ ] **Step 1: Criar o workspace**

`workspace/versions.tf` é o mesmo dos Cenários do Ato I. `workspace/variables.tf`:

```hcl
variable "senha_do_painel" {
  type        = string
  description = "senha do painel administrativo — de exemplo, nunca uma senha real"
  sensitive   = true
  default     = "senha-de-exemplo-nao-use"
}
```

`workspace/main.tf`:

```hcl
resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name  = "mirante-painel"
  image = docker_image.web.image_id

  env = [
    "SENHA_DO_PAINEL=${var.senha_do_painel}",
  ]

  ports {
    internal = 80
    external = 8076
  }
}

output "senha_em_uso" {
  value     = var.senha_do_painel
  sensitive = true
}
```

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/06-o-arquivo-que-nao-pode-ir-para-o-git
titulo: O arquivo que não pode ir para o Git
dificuldade: assistido
terraform: true
containers: [mirante-painel]
---
```

Estrutura obrigatória:

1. **Abertura.** Auditoria de segurança marcada para a semana que vem. O time precisa
   responder onde estão os segredos da infraestrutura — e a resposta honesta, hoje, é
   "não sabemos".
2. **`## Aplique e olhe a saída`.** `terraform init`, `terraform apply`. A saída mostra
   `senha_em_uso = (sensitive value)`. O texto pergunta, antes de responder: isso
   significa que a senha está protegida?
3. **`## Abra o state`.**

   ```powershell
   terraform state list
   terraform state show docker_container.web
   ```

   A senha aparece **em claro** dentro de `env`. O texto nomeia o que acabou de acontecer:
   `sensitive` é uma proteção de **exibição**, e o state guarda o valor como recebeu.
4. **`## Encontre-a no arquivo`.** Como dica de degrau `Assistido`, o texto diz que o
   state é JSON e que uma busca por texto o atravessa — sem entregar o comando pronto.
   O leitor precisa provar para si mesmo que a senha está no arquivo.
5. **`## O que sensitive realmente compra`.** Ele impede o vazamento acidental em log de
   pipeline e em captura de tela, que é onde segredo vaza com mais frequência. Não é
   criptografia, e o texto precisa dizer isso sem suavizar.
6. **`## Proteja o que dá para proteger`.** O leitor cria um `.gitignore` no diretório de
   trabalho cobrindo `*.tfstate`, `*.tfstate.*`, `.terraform/` e `*.tfvars`. O texto
   explica por que `.terraform.lock.hcl` é a **exceção** que vai para o Git — assunto do
   Cenário 10 — e por que `*.tfvars` entra na lista mesmo quando não tem segredo hoje.
7. **`## O que isso não resolve`.** Ponte honesta para o Cenário 16: `.gitignore` protege
   o repositório, não o notebook. State com segredo em disco de máquina pessoal continua
   sendo problema, e a saída é state remoto com acesso restrito.
8. **`## Verificação`.**

- [ ] **Step 3: Escrever a Verificação**

O `comando_produz` aqui não revela a resposta do exercício: ele confirma que o
`.gitignore` existe e cobre o state, que é o entregável, não o caminho.

```yaml
asercoes:
  - tipo: container_rodando
    nome: mirante-painel
  - tipo: http_responde
    url: http://localhost:8076
    status: 200
  - tipo: comando_produz
    comando: [git, check-ignore, -v, terraform.tfstate]
    contem: .gitignore
    descricao: o state local está ignorado pelo Git
  - tipo: terraform_estado
    endereco: docker_container.web
    atributo: name
    esperado: mirante-painel
    descricao: o painel nasceu do código
  - tipo: terraform_plano_limpo
    descricao: o código descreve a infraestrutura que está no ar
```

**Atenção:** `git check-ignore` roda no diretório do backend, não no diretório de
trabalho. Confirme no Step 4 que a Asserção passa de fato; se o caminho relativo não
resolver, troque o comando por
`[git, -C, ../work, check-ignore, -v, terraform.tfstate]` e ajuste o `contem` para o que
a saída real apresentar.

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo. A prova específica deste Cenário: apague o `.gitignore` e verifique de
novo — só a Asserção de `comando_produz` reprova, e a mensagem precisa deixar claro o que
falta.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/06-o-arquivo-que-nao-pode-ir-para-o-git
git commit -m "feat: Cenário 06 da Trilha IaC — state, sensitive e o que não vai para o Git"
```

---

### Task 3: Cenário 07 — A infra que nasceu à mão

**Files:**
- Create: `content/iac/07-a-infra-que-nasceu-a-mao/cenario.md`
- Create: `content/iac/07-a-infra-que-nasceu-a-mao/verificacao.yaml`
- Create: `content/iac/07-a-infra-que-nasceu-a-mao/workspace/versions.tf`
- Create: `content/iac/07-a-infra-que-nasceu-a-mao/workspace/preparar.ps1`

**Interfaces:**
- Consumes: nada.
- Produces: o Cenário 07, primeiro `Autônomo` da Trilha.

**A restrição que desenha este Cenário.** A validação de 2026-08-10 provou que o `Read`
do provider Docker não recupera `ports`, `volumes` nem `env` de um container importado.
Container com porta publicada **não** pode ser adotado sem recriação. Por isso a infra
órfã deste Cenário é um **volume**, uma **rede** e um **container sem porta publicada** —
e a impossibilidade de adotar um container com porta vira a lição, não um defeito
escondido.

- [ ] **Step 1: Criar o workspace**

`workspace/versions.tf` é o mesmo dos demais. **Não** há `main.tf`: escrevê-lo é o
exercício inteiro.

`workspace/preparar.ps1` — o script que cria a infraestrutura órfã, exatamente como a
pessoa que saiu teria feito:

```powershell
# Sobe a infraestrutura da Mirante do jeito que ela nasceu: à mão, sem código.
docker volume create mirante-arquivos | Out-Null
docker network create mirante-interna | Out-Null
docker run -d --name mirante-relatorios --restart unless-stopped --network mirante-interna nginx:1.27-alpine | Out-Null
Write-Output "infraestrutura no ar. Nenhuma linha de Terraform foi escrita."
docker ps --filter name=mirante-relatorios --format '{{.Names}}  {{.Status}}'
```

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/07-a-infra-que-nasceu-a-mao
titulo: A infra que nasceu à mão
dificuldade: autonomo
terraform: true
containers: [mirante-relatorios]
volumes: [mirante-arquivos]
---
```

Sendo `Autônomo`, o texto entrega **o objetivo e o ambiente, e nada mais**: nenhum bloco
`import` pronto, nenhum `main.tf` parcial, nenhuma sequência de comandos. Estrutura:

1. **Abertura.** A pessoa que subiu o serviço de relatórios saiu. A infraestrutura está no
   ar, funcionando, e não existe uma linha de código que a descreva. Recriá-la do zero
   significa derrubar o que funciona; a alternativa é adotá-la.
2. **`## O ambiente`.** Instrui rodar `.\preparar.ps1` e conferir com `docker ps`,
   `docker volume ls` e `docker network ls` o que existe. Nomeia os três recursos.
3. **`## O objetivo`.** Ao final: um `main.tf` que descreve os três recursos, um state que
   os conhece, **os mesmos recursos de antes — o container com o mesmo id** — e
   `terraform plan` sem mudança pendente. O texto diz explicitamente que recriar qualquer
   um dos três é reprovar, mesmo que o resultado pareça idêntico.
4. **`## A pista`.** Uma só, porque o degrau é autônomo: a documentação do Terraform chama
   isso de importar, e a partir da versão 1.5 existe uma forma **declarativa** de fazê-lo,
   que fica no código e é revisável em pull request. O leitor descobre o resto.
5. **`## O aviso que muda tudo`.** Este é o único parágrafo em que o Cenário entrega
   conhecimento de graça, porque sem ele o exercício vira adivinhação frustrante:
   importar não é o inverso de aplicar. O provider só recupera do mundo real aquilo que a
   função de leitura dele implementa, e o provider Docker **não** recupera `ports`,
   `volumes` nem `env`. A consequência prática, que o leitor vai encontrar: um plano com
   `# forces replacement` embaixo do container. O texto manda ler o plano e ajustar o
   código até o plano ficar limpo — sem dizer qual é o ajuste.
6. **`## A pergunta para levar embora`.** Se o container de relatórios publicasse uma
   porta, ele poderia ser adotado sem recriação? O leitor testa e descobre que não. E aí
   vem a lição maior: `import` não é uma promessa do Terraform, é uma capacidade de cada
   provider — e quem decide se a adoção é segura é o plano.
7. **`## Verificação`.**

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: container_rodando
    nome: mirante-relatorios
  - tipo: container_em_rede
    nome: mirante-relatorios
    rede: mirante-interna
    presente: true
  - tipo: volume_existe
    nome: mirante-arquivos
  - tipo: terraform_estado
    endereco: docker_volume.arquivos
    atributo: name
    esperado: mirante-arquivos
    descricao: o volume que já existia entrou no state sem ser recriado
  - tipo: terraform_estado
    endereco: docker_container.relatorios
    atributo: name
    esperado: mirante-relatorios
    descricao: o container foi adotado, e não refeito
  - tipo: terraform_plano_limpo
    descricao: a adoção convergiu — não há mudança pendente
```

Os endereços `docker_volume.arquivos`, `docker_network.interna` e
`docker_container.relatorios` são **exigidos do leitor**: o Cenário precisa declará-los na
seção do objetivo, porque a Asserção não adivinha o nome que ele escolheria.

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo, e além dele a prova que dá sentido ao Cenário: anote o id do container com
`docker inspect -f '{{.Id}}' mirante-relatorios` **antes** do exercício e confira depois.
Se o id mudou, o leitor recriou em vez de adotar — e a Verificação precisa reprovar nesse
caso. Se ela aprovar com id diferente, acrescente uma Asserção
`terraform_estado` sobre o atributo `id` do container com o valor observado; sem isso o
Cenário aprova o caminho errado.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/07-a-infra-que-nasceu-a-mao
git commit -m "feat: Cenário 07 da Trilha IaC — adotar infra órfã com import"
```

---

### Task 4: Cenário 08 — Três ambientes, um copy-paste

**Files:**
- Create: `content/iac/08-tres-ambientes-um-copy-paste/cenario.md`
- Create: `content/iac/08-tres-ambientes-um-copy-paste/verificacao.yaml`
- Create: `content/iac/08-tres-ambientes-um-copy-paste/workspace/versions.tf`
- Create: `content/iac/08-tres-ambientes-um-copy-paste/workspace/main.tf`

**Interfaces:**
- Consumes: `enderecoParaLinhaDeComando` da Task 1 — sem ela a Verificação deste Cenário
  não passa no Windows.
- Produces: o Cenário 08.

- [ ] **Step 1: Criar o workspace com o copy-paste**

`workspace/versions.tf` é o mesmo dos demais. `workspace/main.tf` entrega a dívida
literal — dois blocos quase idênticos, que o leitor vai extrair:

```hcl
resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "homologacao" {
  name  = "mirante-homologacao"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = 8077
  }
}

resource "docker_container" "producao" {
  name  = "mirante-producao"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = 8078
  }
}
```

**Não** entregue o diretório `modulos/servico/` pronto: criá-lo é o exercício.

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/08-tres-ambientes-um-copy-paste
titulo: Três ambientes, um copy-paste
dificuldade: assistido
terraform: true
containers: [mirante-homologacao, mirante-producao]
---
```

Estrutura obrigatória:

1. **Abertura.** O contrato do cliente grande pede um terceiro ambiente. Com o arquivo de
   hoje, isso é copiar mais um bloco — e a próxima mudança vira três edições que precisam
   ser idênticas, ou um bug que só aparece em um ambiente.
2. **`## O que é um módulo`.** Um diretório com `.tf` dentro. `variable` é a entrada,
   `output` é a saída, e o que estiver no meio é detalhe que quem chama não precisa
   conhecer. A analogia útil: um módulo é uma função, e `for_each` é chamá-la em laço.
3. **`## Extraia`.** Como dica de degrau `Assistido`, o texto entrega a **estrutura de
   diretórios** e as **assinaturas**, nunca o conteúdo:

   ```
   workspace/
     main.tf
     modulos/
       servico/
         main.tf
   ```

   O módulo recebe `ambiente` (string) e `porta` (number) e devolve `nome_do_container`.
4. **`## A armadilha que todo mundo cai`.** Este parágrafo é obrigatório e vem do medido
   em 2026-08-10: um módulo filho que usa `docker_container` **sem declarar o próprio
   bloco `required_providers`** faz o `init` procurar `hashicorp/docker` e falhar com
   *provider registry does not have a provider named*. O texto explica a razão — a
   herança de provider não é automática por nome de recurso — e deixa o leitor corrigir.
5. **`## for_each em vez de dois blocos`.** O `main.tf` da raiz passa a ter **um** bloco
   `module` com `for_each` sobre um `local` que mapeia ambiente para porta:

   ```hcl
   locals {
     ambientes = {
       homologacao = 8077
       producao    = 8078
     }
   }
   ```

   O texto pede que o leitor confirme que acrescentar o terceiro ambiente passou a ser
   **uma linha**.
6. **`## Os endereços mudaram`.**

   ```powershell
   terraform state list
   ```

   Os recursos agora vivem em `module.ambiente["producao"].docker_container.web`. O texto
   precisa avisar que, ao extrair o módulo, o `apply` vai propor **destruir e recriar** os
   dois containers, porque para o Terraform o endereço é a identidade — e que aceitar isso
   aqui é aceitável porque não há dado dentro. Quando houver, a ferramenta é o bloco
   `moved`, assunto do próximo Cenário. Essa frase é a ponte narrativa do Ato.
7. **`## Ler um endereço indexado no PowerShell`.** Detalhe prático medido: as aspas do
   endereço precisam ser escapadas para sobreviver ao PowerShell.

   ```powershell
   terraform state show 'module.ambiente[\"producao\"].docker_container.web'
   ```

8. **`## Verificação`.**

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: container_rodando
    nome: mirante-homologacao
  - tipo: container_rodando
    nome: mirante-producao
  - tipo: http_responde
    url: http://localhost:8077
    status: 200
  - tipo: http_responde
    url: http://localhost:8078
    status: 200
  - tipo: terraform_estado
    endereco: module.ambiente["producao"].docker_container.web
    atributo: name
    esperado: mirante-producao
    descricao: produção nasceu do módulo, e não de um bloco copiado
  - tipo: terraform_estado
    endereco: module.ambiente["homologacao"].docker_container.web
    atributo: name
    esperado: mirante-homologacao
    descricao: homologação nasceu do mesmo módulo
  - tipo: terraform_plano_limpo
    descricao: os dois ambientes convergiram
```

O nome do bloco `module` — `ambiente` — e as chaves `homologacao` e `producao` são
**exigidos do leitor** na seção do objetivo, pelo mesmo motivo do Cenário 07.

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo. Esta é a prova que valida a Task 1: se as duas Asserções de
`terraform_estado` reprovarem com `não está no state` mesmo com os containers no ar e o
`terraform state list` mostrando os endereços, o escape do Windows não está sendo
aplicado. Volte à Task 1 antes de seguir.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/08-tres-ambientes-um-copy-paste
git commit -m "feat: Cenário 08 da Trilha IaC — extrair módulo e iterar com for_each"
```

---

### Task 5: Cenário 09 — O rename que derrubou o banco

**Files:**
- Create: `content/iac/09-o-rename-que-derrubou-o-banco/cenario.md`
- Create: `content/iac/09-o-rename-que-derrubou-o-banco/verificacao.yaml`
- Create: `content/iac/09-o-rename-que-derrubou-o-banco/workspace/versions.tf`
- Create: `content/iac/09-o-rename-que-derrubou-o-banco/workspace/main.tf`

**Interfaces:**
- Consumes: nada da Task 4.
- Produces: o Cenário 09, que fecha o Ato II.

- [ ] **Step 1: Criar o workspace**

`workspace/versions.tf` é o mesmo dos demais. `workspace/main.tf` — endereços com os nomes
antigos, que o leitor vai renomear:

```hcl
resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_volume" "dados" {
  name = "mirante-estoque"
}

resource "docker_container" "banco" {
  name  = "mirante-banco"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = 8079
  }

  volumes {
    volume_name    = docker_volume.dados.name
    container_path = "/var/lib/mirante"
  }
}
```

- [ ] **Step 2: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: iac/09-o-rename-que-derrubou-o-banco
titulo: O rename que derrubou o banco
dificuldade: autonomo
terraform: true
containers: [mirante-banco]
volumes: [mirante-estoque]
---
```

Sendo `Autônomo`, entrega objetivo e ambiente. Estrutura:

1. **Abertura.** Numa outra empresa, uma pessoa renomeou `docker_volume.dados` para
   `docker_volume.estoque` numa faxina de nomes, aprovou o plano sem ler o rodapé e perdeu
   o banco. Aqui o mesmo rename precisa acontecer — sem perder nada.
2. **`## O ambiente`.** `terraform init`, `terraform apply`, e então gravar um dado dentro
   do volume, para que a perda seja concreta e verificável:

   ```powershell
   docker exec mirante-banco sh -c "echo pedido-4711 > /var/lib/mirante/estoque.txt"
   docker exec mirante-banco cat /var/lib/mirante/estoque.txt
   ```

3. **`## O objetivo`.** `docker_volume.dados` vira `docker_volume.estoque` e
   `docker_container.banco` vira `docker_container.estoque`; ao final, `estoque.txt`
   continua legível com o mesmo conteúdo, o container tem o mesmo id de antes, o state
   está nos endereços novos e o plano está limpo.
4. **`## Renomeie e leia o plano — não aplique`.** O leitor renomeia, planeja e encontra
   `Plan: 2 to add, 0 to change, 2 to destroy` com o volume marcado para destruição. O
   texto pede que ele **não** aplique e pergunte por que o Terraform acha que o recurso
   sumiu.
5. **`## A pista`.** Uma só: para o Terraform, o endereço é a identidade. Existe um bloco
   que ensina o Terraform que um endereço virou outro, e ele fica no código, não é um
   comando avulso.
6. **`## O detalhe que confunde todo mundo`.** Parágrafo obrigatório, do medido em
   2026-08-10: com o bloco correto, o plano fica `0 to add, 0 to change, 0 to destroy` —
   e mesmo assim **há uma mudança a aplicar**. Mover um endereço é uma escrita no state.
   Enquanto o `apply` não rodar, a Verificação continua reprovando, e está certa. Rode o
   `apply`.
7. **`## Ponha o cinto`.** Depois do rename, o leitor acrescenta `prevent_destroy` no
   volume e tenta um `terraform destroy` para ver a recusa:
   `Error: Instance cannot be destroyed`. O texto precisa registrar o que foi medido: o
   `prevent_destroy` protege **um endereço, não um recurso**. Declará-lo no endereço novo
   não teria salvado o antigo do plano do passo 4 — o cinto só funciona se estiver posto
   antes da curva.
8. **`## Verificação`.**

O texto deve fechar o Ato II lembrando que os quatro Cenários resolveram as quatro
divergências do triângulo dos Fundamentos.

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: container_rodando
    nome: mirante-banco
  - tipo: volume_existe
    nome: mirante-estoque
  - tipo: comando_produz
    comando: [docker, exec, mirante-banco, cat, /var/lib/mirante/estoque.txt]
    contem: pedido-4711
    descricao: o dado gravado antes do rename sobreviveu
  - tipo: terraform_estado
    endereco: docker_volume.estoque
    atributo: name
    esperado: mirante-estoque
    descricao: o volume está no endereço novo do state
  - tipo: terraform_estado
    endereco: docker_container.estoque
    atributo: name
    esperado: mirante-banco
    descricao: o container está no endereço novo, com o mesmo nome de sempre
  - tipo: terraform_plano_limpo
    descricao: o bloco moved foi aplicado e não sobrou mudança pendente
```

- [ ] **Step 4: Provar na aplicação real**

Ciclo completo, mais as duas provas específicas:

1. Com o bloco `moved` escrito e **sem** `apply`, verifique: `terraform_plano_limpo`
   reprova. É o comportamento medido, e o Cenário precisa tê-lo explicado.
2. Depois do `apply`, confirme que `estoque.txt` ainda tem `pedido-4711` e que o id do
   container não mudou.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/09-o-rename-que-derrubou-o-banco
git commit -m "feat: Cenário 09 da Trilha IaC — moved e prevent_destroy sem perder dados"
```

---

### Task 6: Fechar o Ato II no catálogo

**Files:**
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`
- Modify: `README.md`

**Interfaces:**
- Consumes: os quatro Cenários das Tasks 2 a 5.
- Produces: nada consumido por código.

- [ ] **Step 1: Atualizar as contagens**

Total de Cenários: **52**. Grupo `iac`: **9**. Soma de Asserções do grupo `iac`: 25 do Ato
I mais 5 no Cenário 06, 6 no 07, 7 no 08 e 6 no 09 — **49**.

```java
        assertThat(cenarios).hasSize(52);
```

```java
        assertThat(iac).hasSize(9);
        assertThat(iac).allMatch(Cenario::terraform);
        assertThat(iac.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(49);
```

Conte os `verificacao.yaml` antes de mudar qualquer número.

- [ ] **Step 2: Rodar a suíte inteira**

Run: `cd backend && ./mvnw test`
Expected: PASS.

- [ ] **Step 3: Atualizar o README**

Na seção de portas, estenda a frase da Trilha IaC:

```markdown
A Trilha IaC usa o bloco **8070–8079**: 8070 no Cenário 01, 8071 no 02, 8072 e 8073 no
03, 8074 no 04, 8075 no 05, 8076 no 06, 8077 e 8078 no 08 e 8079 no 09. O Cenário 07 não
publica porta nenhuma, de propósito.
```

Na seção "## Fundamentos e evolução das Trilhas", troque para "os Atos I e II completos e
os Atos III e IV em construção".

- [ ] **Step 4: Commitar**

```bash
git add backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java README.md
git commit -m "docs: Ato II da Trilha IaC completo no catálogo e no README"
```

---

## Definição de pronto

- `cd backend && ./mvnw test` passa, com o catálogo em 52 Cenários e 9 na Trilha `iac`.
- `cd frontend && npm run build` passa.
- O `enderecoParaLinhaDeComando` foi provado contra um Terraform real, e não só no teste
  unitário.
- Os quatro Cenários iniciam, reprovam antes e aprovam depois.
- No Cenário 07, o id do container antes e depois do exercício é o mesmo.
- No Cenário 09, `terraform_plano_limpo` reprova com o `moved` escrito e sem `apply`, e
  aprova depois.
- Os Cenários 07 e 09 não entregam a solução no texto — são `Autônomo`.
