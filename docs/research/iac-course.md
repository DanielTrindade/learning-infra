# Pesquisa — ferramental de infraestrutura como código simulável localmente

> Estado: recomendação para implementação
>
> Data da pesquisa: 2026-08-09
>
> Escopo: Windows 11 + Docker Desktop, cluster kind local, MiniStack já instalado pela
> Trilha AWS

## Resumo executivo

A Trilha IaC não precisa ser uma segunda Trilha AWS. O achado que mais muda o desenho é
que **dois dos três substratos disponíveis nesta máquina têm fidelidade total**: o
provider `kreuzwerker/docker` cria containers, imagens, redes e volumes reais, e o
provider `hashicorp/kubernetes` cria objetos reais no cluster kind do Docker Desktop.
Só o terceiro substrato — AWS via MiniStack — carrega a matriz de fidelidade parcial
que já está registrada em [`aws-local-lab.md`](aws-local-lab.md).

Isso permite uma trilha que sobe as três camadas com o mesmo binário e o mesmo HCL,
reprovisionando declarativamente o que o aluno construiu à mão nas Trilhas Docker,
Kubernetes e AWS. A recomendação é **Terraform 1.15.8 pinado**, com OpenTofu citado e
demonstrado como drop-in, e **18 Cenários** organizados em quatro atos.

A recomendação também é conservadora no que fica de fora: nada de Ansible, Pulumi,
CDKTF, Conftest, Checkov ou Terragrunt instalados. `terraform test`,
`precondition`/`postcondition` e `check` cobrem teste e política sem acrescentar um
binário sequer.

## Método e fontes

A consulta começou pelo Context7, como exigido pelo projeto. O catálogo resolveu
`/websites/developer_hashicorp_terraform` (31.680 snippets, reputação alta) e
`/opentofu/opentofu`; a documentação do framework de testes veio de lá. As versões
foram lidas diretamente da API de releases do GitHub em 2026-08-09, e não das páginas
de documentação, que costumam antecipar versões ainda não publicadas — o mesmo cuidado
que a pesquisa do MiniStack exigiu.

O comportamento do provider Docker no Windows foi verificado no código-fonte, e não em
blog ou documentação, porque a documentação publicada **não** cobre o caso.

## Auditoria da máquina em 2026-08-09

| Ferramenta | Estado |
|---|---|
| Docker Engine | 29.6.1, presente |
| AWS CLI | 2.31.22, presente |
| `terraform` | **ausente do PATH** |
| `tofu` | **ausente do PATH** |
| `pulumi` | ausente do PATH |
| `ansible` | ausente do PATH |

Instalar e pinar o Terraform é pré-requisito da trilha e precisa entrar no README ao
lado de Docker Desktop, Java 25, Node 22+ e AWS CLI v2.

## Versões publicadas em 2026-08-09

| Peça | Versão | Publicada em |
|---|---|---|
| Terraform CLI | v1.15.8 | 2026-07-08 |
| OpenTofu | v1.12.5 | 2026-07-21 |
| `kreuzwerker/docker` | v4.5.0 | 2026-06-18 |
| `hashicorp/kubernetes` | v3.2.1 | 2026-07-01 |
| `hashicorp/aws` | v6.58.0 | 2026-08-05 |

Fonte: API de releases oficial de cada repositório.

O provider Kubernetes está na linha **3.x**, uma major recente. Qualquer exemplo de
Terraform + Kubernetes anterior a ela pode não colar sem ajuste, o que reforça pinar a
constraint no conteúdo em vez de deixar o `init` resolver livremente.

## Terraform ou OpenTofu

| Critério | Terraform 1.15.8 | OpenTofu 1.12.5 |
|---|---|---|
| Licença | BUSL desde agosto de 2023 | MPL, sob a Linux Foundation |
| Adoção | maior fatia do mercado, entre 33% e 62% conforme a medição | cerca de 12%, com 27% dos times avaliando |
| Linguagem | HCL | HCL idêntico |
| Testes | `terraform test` | `tofu test`, mesmo formato |
| Exclusivo | — | criptografia nativa de state |

A BUSL restringe construir um serviço gerenciado concorrente sobre o Terraform. Ela não
restringe uso pessoal, estudo nem uso comercial interno, então não é obstáculo para esta
plataforma.

**Recomendação: Terraform como o binário da trilha**, porque é o comando que aparece nas
vagas e na maior parte da documentação de mercado. O fork entra nos Fundamentos como
assunto — licença, governança e motivo — e ganha um passo prático no Cenário 10, em que
o mesmo código roda com `tofu` sem alterar uma linha. Ensinar a fungibilidade vale mais
do que escolher um lado.

Fontes: [Encore](https://encore.dev/articles/opentofu-vs-terraform-2026),
[Scalr](https://scalr.com/learning-center/opentofu-vs-terraform),
[Infisical](https://infisical.com/blog/terraform-vs-opentofu).

## Matriz de substratos

| Substrato | Provider | O que a Verificação prova de verdade | Limites a declarar |
|---|---|---|---|
| **Docker** | `kreuzwerker/docker` ~> 4.5 | container, imagem, rede e volume reais; ciclo de vida real | nenhum relevante para a trilha |
| **Kubernetes** | `hashicorp/kubernetes` ~> 3.2 | objetos reais no cluster kind do Docker Desktop | um cluster local não prova HA, upgrade nem multi-tenancy |
| **AWS** | `hashicorp/aws` ~> 6.58 + MiniStack | a matriz de [`aws-local-lab.md`](aws-local-lab.md): S3, DynamoDB e SQS com data plane real; VPC, EC2 e Route 53 só control plane | nenhuma Asserção pode alegar isolamento de rede, IAM aplicado, custo ou disponibilidade |

O MiniStack documenta compatibilidade com o AWS Provider v5 e v6, endpoints por serviço,
os três `skip_*` e multi-conta por access key sintética distinta.
[IaC no MiniStack](https://ministack.org/docs/iac)

### Provider Docker no Windows

A documentação publicada do provider mostra exemplos de `host` para socket Unix, SSH e
TCP com TLS, e **não** documenta o caso Windows. O código-fonte resolve a dúvida: quando
`host` é omitido e `DOCKER_HOST` não está definido, o provider retorna
`npipe:////./pipe/docker_engine` em `runtime.GOOS == "windows"`.

Fonte: [`internal/provider/provider.go`, linhas 48–53](https://github.com/kreuzwerker/terraform-provider-docker/blob/master/internal/provider/provider.go).

Consequência prática: `provider "docker" {}` sem argumento algum funciona no Docker
Desktop desta máquina. O Cenário 01 começa sem cerimônia de configuração, o que importa
muito para a curva de entrada.

O provider é mantido pela kreuzwerker, não pela HashiCorp; o antigo `hashicorp/docker`
está descontinuado. A manutenção é essencialmente de uma pessoa, o que é um risco de
sustentação a registrar — porém a superfície usada pela trilha (imagem, container, rede,
volume) é estável há várias majors.
[Repositório](https://github.com/kreuzwerker/terraform-provider-docker)

## State remoto e lock

A partir do Terraform 1.11, o backend S3 aceita `use_lockfile = true` e faz o lock com
*conditional writes* do próprio S3 — um `PutObject` com `If-None-Match` que só cria o
`.tflock` se ele ainda não existir. Os argumentos `dynamodb_table` e `dynamodb_endpoint`
passaram a emitir aviso de descontinuação.

**Este é o único risco técnico material da trilha.** A documentação do S3 no MiniStack
lista 73 operações, versionamento, lifecycle, CORS, notificações e object lock, mas
**não menciona** requisições condicionais nem `If-None-Match`, nem entre os recursos
suportados nem entre as limitações conhecidas.
[S3 no MiniStack](https://ministack.org/docs/services/s3)

Plano de contingência, em ordem de preferência:

1. validar empiricamente antes de escrever o Cenário 16 — dois `apply` concorrentes
   contra o mesmo state devem produzir um erro de lock;
2. se falhar, ensinar o backend S3 sem lock nativo e tratar o lock como conceito, com o
   Cenário registrando explicitamente o que não pôde ser provado localmente;
3. não usar o lock por DynamoDB como saída, porque ensinaria um caminho que o próprio
   Terraform está descontinuando.

Fontes: [S3 native state locking](https://www.bschaatsbergen.com/s3-native-state-locking),
[AWS Specialists](https://medium.com/aws-specialists/dynamodb-not-needed-for-terraform-state-locking-in-s3-anymore-29a8054fc0e9).

## Teste e política sem instalar nada

O `terraform test` é nativo desde a 1.6. Arquivos `.tftest.hcl` declaram blocos `run`
com `command = plan` ou `command = apply`, `variables`, `assert` com `condition` e
`error_message`, `expect_failures` e `state_key`. Há `mock_provider`, `override_data` e
`override_resource` para testar sem tocar em infraestrutura.

```hcl
run "valid_string_concat" {
  command = plan

  assert {
    condition     = aws_s3_bucket.bucket.bucket == "test-bucket"
    error_message = "S3 bucket name did not match expected"
  }
}
```

Fonte: [Terraform tests](https://developer.hashicorp.com/terraform/language/tests) e
[mocking](https://developer.hashicorp.com/terraform/language/tests/mocking), via Context7.

Somados a `precondition`, `postcondition`, `check`, `validate` e `fmt -check`, isso cobre
o assunto sem Conftest, OPA, Checkov ou Terratest. Cada binário a mais é uma
dependência que o aluno precisa instalar antes de aprender qualquer coisa, e o retorno
didático aqui não paga esse custo.

## O que fica deliberadamente de fora

| Ferramenta | Por que não entra na prática |
|---|---|
| **Ansible** | Categoria diferente: configura máquina que já existe, não a provisiona. No Windows exige WSL. Entra nos Fundamentos como distinção conceitual. |
| **Pulumi, CDK, CDKTF** | Mesma ideia com outra sintaxe. Entram nos Fundamentos como mapa do território. |
| **CloudFormation** | Já exercitado nos Cenários 06, 17 e 18 da Trilha AWS. Aqui vira objeto de comparação, não de prática. |
| **Terragrunt** | Resolve escala organizacional que esta trilha não tem. |
| **Conftest, OPA, Checkov** | Substituídos por `check`, `precondition` e `postcondition` nativos. |

## Verificação automatizável

As Asserções atuais provam a infraestrutura resultante, mas nenhuma prova o que é
específico de IaC. Sem tipo novo, o aluno passa em todos os Cenários rodando
`docker run` na mão — falso positivo estrutural numa trilha de infraestrutura como
código.

Dois tipos novos bastam:

1. `terraform_estado` — expressão JMESPath sobre `terraform show -json`, com valor
   esperado. Prova que o recurso **nasceu do código** e está sob gestão do state.
2. `terraform_plano_limpo` — `terraform plan -detailed-exitcode`, exigindo código de
   saída 0. Prova idempotência e ausência de drift em uma única afirmação.

`terraform test` reusa o `comando_produz` existente: diferente do caso que motivou aquele
escape hatch, o comando aqui não revela a resposta do exercício.

### Regras contra falso positivo

- Toda Asserção de infraestrutura real (`container_rodando`, `http_responde`,
  `kubernetes_*`, `aws_consulta`) vem acompanhada de uma Asserção de state ou de plano.
  Infra certa por caminho errado não é Cenário concluído.
- `terraform_plano_limpo` roda depois das demais, porque um `plan` também atualiza o
  conhecimento do state sobre o mundo.
- Nenhuma Asserção AWS pode alegar isolamento de rede, IAM aplicado, custo ou
  disponibilidade — a mesma regra da Trilha AWS.
- A Verificação continua estritamente observadora: nunca executa `apply` nem `destroy`.

## Ciclo de vida e limpeza

O teardown **não** deve chamar `terraform destroy`. Vários Cenários existem justamente
porque o state está quebrado, órfão ou divergente; um teardown que depende do state
falharia exatamente quando é mais necessário.

Vale a regra que a plataforma já usa: o Cenário declara `containers`, `volumes`,
`namespaceKubernetes` e `ministack`, e o `work/` recriado a cada Iniciar leva junto
`.terraform/`, o lockfile e o state local. Isso preserva a invariante da
[ADR 0002](../adr/0002-um-cenario-ativo-por-vez.md) sem acoplar a limpeza a um artefato
que o próprio exercício corrompe.

## Guardrails

Herdados da Trilha AWS e aplicados agora ao código que o aluno escreve:

- todo `.tf` com o provider AWS declara `endpoints` para `http://127.0.0.1:4566` e os
  três `skip_*`;
- nenhuma access key `AKIA`/`ASIA` no conteúdo versionado;
- o provider Kubernetes fixa `config_context = "docker-desktop"`, como as Asserções da
  Trilha Kubernetes já fazem;
- um teste de conteúdo no estilo do `CatalogoRealTest` rejeita qualquer `.tf` da trilha
  que viole essas regras, para que o guardrail seja executável e não um parágrafo de
  documentação.

## O que precisa ser validado antes de publicar a trilha

1. Instalar o Terraform 1.15.8 e confirmar `provider "docker" {}` sem `host` no Windows.
2. Provar `apply` idempotente com o provider Docker: segundo `plan` com exit code 0.
3. Provar `terraform import` de container e volume criados fora do Terraform, sem
   recriação.
4. Provar bloco `moved` preservando um volume com dados.
5. Provar `terraform test` com `command = plan` e `command = apply` no provider Docker.
6. Provar o provider Kubernetes 3.2.1 contra o contexto `docker-desktop`.
7. **Provar `use_lockfile` contra o S3 do MiniStack, com dois `apply` concorrentes.**
   Se falhar, acionar a contingência registrada acima.
8. Medir RAM e CPU do Cenário 18 com Docker, Kubernetes e MiniStack simultâneos.
9. Confirmar que o bloco de portas 8070–8079 está livre nesta máquina.
10. Rodar o canário que detecta chamada para `amazonaws.com`; o resultado esperado é
    zero, como na Trilha AWS.

## Recomendação final

Adotar **Terraform 1.15.8 pinado**, três providers e nenhum binário adicional. A trilha
ganha valor didático em três frentes que nenhuma das anteriores cobriu:

1. o triângulo **código, state e mundo real** — o modelo mental que explica drift,
   import e refactor de uma vez só;
2. a comparação direta com a reconciliação do Kubernetes, que o aluno já viveu: o
   controller reconcilia sozinho e para sempre, o Terraform reconcilia quando mandam;
3. o mesmo HCL atravessando três substratos com fidelidades diferentes, terminando com
   o inventário explícito do que foi provado localmente e do que ainda exige uma conta
   sandbox AWS.

## Validação executada em 2026-08-09

| Premissa | Resultado |
|---|---|
| `terraform version` | v1.15.8 |
| `provider "docker" {}` sem `host` no Windows | funcionou |
| `apply` de imagem e container | 2 added |
| segundo `plan -detailed-exitcode` | exit code 0 |
| duração do `plan` | 1.9293977 s (`TotalSeconds`) |
| formato de `state show` | `    name                                        = "validacao-iac-web"` |
