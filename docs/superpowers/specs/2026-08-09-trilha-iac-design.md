# Design — Trilha de Infraestrutura como Código

> Data: 2026-08-09
>
> Estudo de ferramental: [`docs/research/iac-course.md`](../../research/iac-course.md)
>
> Estado: aprovado para virar plano de implementação

## Objetivo

Criar a quarta Trilha da plataforma. Ela fecha o aprendizado de infraestrutura: em vez
de introduzir uma tecnologia nova e isolada, reprovisiona declarativamente o que o aluno
construiu à mão nas Trilhas Docker, Kubernetes e AWS, e ensina no caminho o que só
aparece quando infraestrutura vira código versionado — state, drift, módulos,
reprodutibilidade, blast radius e teste.

O critério de sucesso é duplo: ao terminar, o aluno consegue escrever e manter
Terraform em contexto profissional, **e** consegue diagnosticar situações de
infraestrutura que a trilha nunca mostrou, porque saiu com um modelo mental, não com
uma lista de comandos.

## Decisões

| Decisão | Escolha | Motivo |
|---|---|---|
| Substrato | Docker → Kubernetes → AWS | Dois dos três têm fidelidade total; a progressão amarra as trilhas anteriores |
| Binário | Terraform 1.15.8 | É o comando das vagas; OpenTofu entra como assunto e como um passo prático |
| Escopo prático | núcleo + módulos + state/import/refactor + teste + pipeline + segredos + lockfile + blast radius | Escolhido com o autor; cobre o que o mercado pede sem virar enciclopédia |
| Narrativa | arco contínuo de uma empresa fictícia | O progresso fica visível: a infra de um capítulo é o ponto de partida do seguinte |
| Verificação | duas Asserções novas + reuso das existentes | Sem elas, o aluno passa rodando `docker run` na mão |
| Teardown | declarativo, nunca `terraform destroy` | State quebrado é o exercício de vários Cenários |

## O modelo mental que a trilha inteira cobra

Três coisas podem divergir duas a duas: o **código**, o **state** e o **mundo real**.
Quase todo assunto difícil de IaC é um par desses três fora de sincronia.

| Divergência | Nome de mercado | Cenários |
|---|---|---|
| mundo ≠ state | drift | 05, 14 |
| mundo existe, state não | brownfield, infra órfã | 07 |
| state existe, mundo não | recurso apagado por fora | 05, 18 |
| código ≠ state para o mesmo recurso | refactor | 09 |

Esse triângulo é apresentado na quarta seção dos Fundamentos e referenciado
explicitamente em cada Cenário que o exercita.

## Ferramental

| Peça | Versão | Papel |
|---|---|---|
| Terraform CLI | 1.15.8 | binário da trilha; entra no README como pré-requisito |
| `kreuzwerker/docker` | ~> 4.5 | Atos I–III |
| `hashicorp/kubernetes` | ~> 3.2 | Ato IV, contexto `docker-desktop` |
| `hashicorp/aws` | ~> 6.58 | Ato IV, contra o MiniStack existente |
| OpenTofu | 1.12.5 | opcional; um passo do Cenário 10 |

No Windows, `provider "docker" {}` sem `host` resolve para
`npipe:////./pipe/docker_engine`. Nenhum binário além do Terraform é instalado: teste e
política usam `terraform test`, `precondition`, `postcondition` e `check`.

## Fundamentos

Mesmo contrato da [ADR 0003](../../adr/0003-fundamentos-pertencem-a-trilha.md): artigo
mais Questionário de 12 situações, 80% recomendado, sem bloquear a prática. Alvo de
aproximadamente 300 linhas, como as outras três trilhas.

### Seções do artigo

1. **O problema** — a infra que só existe na memória de quem a subiu; abre a narrativa.
2. **Imperativo e declarativo** — descrever o destino, não o caminho.
3. **O ciclo** — código → plan → apply → state.
4. **O triângulo** — código, state e mundo real.
5. **Reconciliação, de novo** — reusa o visual `reconciliacao` dos Fundamentos de
   Kubernetes e marca a diferença de cadência: o controller reconcilia sozinho e para
   sempre; o Terraform reconcilia quando mandam. É o que explica por que drift é um
   problema central de IaC e quase não é de Kubernetes.
6. **O grafo de dependências** — implícito por referência, explícito por `depends_on`.
7. **Provider como tradutor** — o mesmo HCL em três substratos; reusa `fidelidade-local`.
8. **Módulos e composição** — entrada, saída, e por que copiar-colar ambiente é dívida.
9. **Idempotência e drift**.
10. **Blast radius** — ler o plano; `forces replacement`, `moved`, `prevent_destroy`, e
    `-target` como ferramenta de emergência.
11. **Segredos** — o state guarda em claro; `sensitive` esconde da saída, não do arquivo.
12. **Infra que se testa** — `validate`, `fmt`, `precondition`/`postcondition`/`check`,
    `terraform test`.
13. **O mapa do território** — Terraform e o fork OpenTofu; CloudFormation, já visto na
    Trilha AWS; Pulumi e CDK; e Ansible como categoria diferente, porque configurar uma
    máquina que já existe não é criá-la.
14. **O ambiente desta Trilha** e **Da teoria aos Cenários**, com referências.

### Diagramas

Seguem a [ADR 0004](../../adr/0004-diagramas-declarativos-no-conteudo.md): o conteúdo
nunca carrega SVG, coordenada, classe CSS ou JSX. Três ids novos entram no catálogo
fechado de `frontend/src/visuaisDoDiagrama.ts`:

- `ciclo-iac` — código → plan → apply → state;
- `triangulo-state` — código, state e mundo real, com as divergências nomeadas;
- `grafo-dependencias` — ordem derivada de referências.

Reusa `reconciliacao` e `fidelidade-local`.

### Questionário

Doze situações de decisão, nenhuma de decorar sintaxe, cada uma com `revisar` apontando
para a seção correspondente:

1. aplicar duas vezes: o que muda;
2. alguém corrigiu na mão em produção;
3. o `plan` diz *forces replacement* no banco;
4. o state ficou no notebook de quem saiu;
5. existe container em produção que o código não conhece;
6. renomear recurso sem destruir;
7. a senha que apareceu no state;
8. `-target` durante um incidente;
9. provider sem constraint quebrando na máquina do colega;
10. onde Ansible entra e onde não entra;
11. Terraform contra CloudFormation;
12. o que a fidelidade local não prova.

## A narrativa

A **Mirante** é um SaaS de gestão de estoque para pequenos varejistas. No Cenário 01 é
uma pessoa só e um container subido à mão num sábado. No Cenário 18 é um time, três
ambientes, um cluster e uma conta na nuvem. Cada Cenário abre com uma pressão concreta —
auditoria, incidente, alguém que saiu, um cliente grande — e a infraestrutura do
capítulo anterior é o ponto de partida do seguinte.

Personagens são referidos por papel, não por pronome.

## Grade de Cenários

### Ato I — Do imperativo ao declarativo (Docker)

| # | Cenário | Dif. | O que ensina | Evidência |
|---:|---|---|---|---|
| 01 | O sábado em que a Mirante subiu | Guiado | `provider "docker" {}`, `docker_image`, `docker_container`, init/plan/apply/destroy, o lockfile que nasce sozinho | container real, `http_responde`, `terraform_estado` |
| 02 | O plano é o contrato | Guiado | ler `+ ~ - -/+`, `plan -out`, apply de plano salvo | `terraform_plano_limpo`, `terraform_estado` |
| 03 | O mesmo serviço, dois ambientes | Guiado | variáveis, `.tfvars`, `locals`, `output` | dois containers derivados de variável |
| 04 | A ordem que ninguém escreveu | Assistido | grafo, dependência implícita, `depends_on`, `terraform graph` | `container_em_rede`, `volume_existe`, `terraform_plano_limpo` |
| 05 | Mexeram na produção | Assistido | drift: `plan` acusa, `apply` reconcilia, `refresh` | `terraform_plano_limpo`, `container_rodando` |

### Ato II — O código sobrevive às pessoas

| # | Cenário | Dif. | O que ensina | Evidência |
|---:|---|---|---|---|
| 06 | O arquivo que não pode ir para o Git | Assistido | `state list`, `state show`, senha em claro, `sensitive`, `.gitignore` | `terraform_estado`, `comando_produz` |
| 07 | A infra que nasceu à mão | Autônomo | bloco `import`, adotar sem recriar | container com o mesmo id de antes, `terraform_plano_limpo` |
| 08 | Três ambientes, um copy-paste | Assistido | extrair módulo, entradas e saídas, `for_each` | `terraform_estado` em endereços `module.*` |
| 09 | O rename que derrubou o banco | Autônomo | `forces replacement`, bloco `moved`, `prevent_destroy` | o volume original sobreviveu, `terraform_estado` no endereço novo |

### Ato III — Confiança

| # | Cenário | Dif. | O que ensina | Evidência |
|---:|---|---|---|---|
| 10 | Reprodutível na máquina de todo mundo | Assistido | `required_version`, constraints, `.terraform.lock.hcl` versionado, `init -upgrade`, o mesmo código com `tofu` | lockfile presente, provider na versão pinada |
| 11 | Infra que se testa | Assistido | `terraform test`, `precondition`/`postcondition`/`check`, `validate`, `fmt -check` | `comando_produz` sobre a saída do `test` |
| 12 | O pipeline que você rodaria no CI | Autônomo | fmt → validate → test → `plan -out` → revisão → apply do plano salvo; `-target` como emergência | plano salvo consumido pelo apply, `terraform_plano_limpo` |

### Ato IV — A infra cresce

| # | Cenário | Dif. | O que ensina | Evidência |
|---:|---|---|---|---|
| 13 | A Mirante migra para o cluster | Guiado | provider `kubernetes` com contexto fixo, namespace, Deployment, Service | `kubernetes_condicao`, `terraform_estado` |
| 14 | Quem é o dono deste recurso? | Assistido | `kubectl apply` por fora, drift em k8s, ConfigMap que dispara rollout, campos gerenciados | `kubernetes_jsonpath`, `terraform_plano_limpo` |
| 15 | A nuvem entra na história | Assistido | provider `aws` com `endpoints` e `skip_*`; S3, SQS e DynamoDB, data plane real | `aws_consulta`, `terraform_estado` |
| 16 | O state que morava num notebook | Autônomo | backend S3, `use_lockfile`, `init -migrate-state`, lock concorrente | objeto de state no bucket, `terraform_plano_limpo` |
| 17 | Pública não é nome de subnet | Autônomo | VPC, subnets, rotas, IGW e SG em Terraform, declarando o que é só estrutura | `aws_consulta` sobre rotas e associações |
| 18 | O apply verde que quebrou a Mirante | Mestre | stack nos três substratos com falhas combinadas: `moved` faltando, módulo com variável errada, drift em k8s, recurso AWS órfão do state | as três famílias mais `terraform_plano_limpo` |

O Cenário 17 é o espelho declarativo do Cenário 08 da Trilha AWS, de propósito: o aluno
reencontra um problema que já resolveu na CLI e vê o que muda quando a fonte de verdade
é um artefato versionado. O Cenário 18 termina com o inventário de fidelidade — o que
foi provado localmente e o que ainda exige uma conta sandbox AWS.

Distribuição de Dificuldade: quatro `Guiado`, oito `Assistido`, cinco `Autônomo`, um
`Mestre`. Estimativa de 30 a 38 horas.

## Mudanças na plataforma

### Backend

1. **`Assercao.TerraformEstado(diretorio, expressao, esperado, descricao)`** — expressão
   JMESPath sobre `terraform show -json`, comparada a um valor esperado. Prova que o
   recurso nasceu do código.
2. **`Assercao.TerraformPlanoLimpo(diretorio, descricao)`** — `terraform plan
   -detailed-exitcode`, exigindo código de saída 0. Prova idempotência e ausência de
   drift.

   Nos dois casos o `diretorio` é **injetado pelo backend** a partir de
   `diretorioTerraform`, e o `verificacao.yaml` não o declara — mesma regra que o
   contexto e o namespace seguem nas Asserções Kubernetes e que o endpoint e a região
   seguem em `aws_consulta`. Um Cenário não pode apontar a Verificação para um diretório
   arbitrário da máquina.
3. O `switch` exaustivo do `MotorDeVerificacao` quebra a compilação nos dois casos novos.
   Isso é proposital e não deve ser resolvido com `default`.
4. **`LeitorDeCenario`** ganha o frontmatter `terraform: true` e `diretorioTerraform`
   (caminho relativo ao `work/`), e `Cenario` ganha os campos correspondentes mais
   `usaTerraform()`.
5. `terraform test` reusa `comando_produz`; não recebe tipo próprio.

### Ordem das Asserções

Toda Asserção de infraestrutura real vem acompanhada de uma Asserção de state ou de
plano — infra certa por caminho errado não é Cenário concluído. `terraform_plano_limpo`
roda por último, porque um `plan` também atualiza o conhecimento do state sobre o mundo.
A Verificação continua estritamente observadora: nunca executa `apply` nem `destroy`.

### Ciclo de vida

O teardown não chama `terraform destroy`. Cada Cenário declara `containers`, `volumes`,
`namespaceKubernetes` e `ministack`, como os das outras trilhas, e o `work/` recriado a
cada Iniciar leva junto `.terraform/`, o lockfile e o state local. Isso preserva a
invariante da [ADR 0002](../../adr/0002-um-cenario-ativo-por-vez.md) sem acoplar a
limpeza a um artefato que o próprio exercício corrompe.

### Guardrails executáveis

Um teste de conteúdo no estilo do `CatalogoRealTest` rejeita qualquer `.tf` da trilha
que:

- use o provider AWS sem bloco `endpoints` apontando para `http://127.0.0.1:4566` e sem
  os três `skip_*`;
- contenha access key `AKIA` ou `ASIA`;
- use o provider Kubernetes sem `config_context = "docker-desktop"`.

### Frontend

- três `visual` novos em `visuaisDoDiagrama.ts`;
- `node frontend/scripts-checar-conteudo.mjs iac` passa a validar os diagramas e os
  links `revisar` do Questionário da trilha.

### Conteúdo

- `content/iac/trilha.yaml`, `fundamentos.md` e `questionario.yaml`;
- dezoito diretórios `content/iac/NN-slug/` com `cenario.md`, `verificacao.yaml` e
  `workspace/`.

### Documentação

- README: Terraform 1.15.8 como pré-requisito, seção de preparo da Trilha IaC, o bloco
  de portas 8070–8079, os dois tipos de Asserção novos e o frontmatter `terraform`;
- CONTEXT.md não muda: o vocabulário já contempla a Trilha IaC.

## Decomposição da implementação

Dezoito Cenários, dois tipos de Asserção, três diagramas e a documentação não cabem em
um plano só — a Trilha Docker já foi construída em etapas, e esta é maior. A ordem
proposta, cada etapa com seu próprio plano e cada uma entregando algo utilizável:

1. **Plataforma** — as duas Asserções, o frontmatter `terraform`, os guardrails
   executáveis, o alvo `iac` do `scripts-checar-conteudo.mjs` e as validações
   bloqueantes do estudo de ferramental. Termina com o Cenário 01 no ar como prova de
   que o caminho inteiro funciona.
2. **Fundamentos** — artigo, três diagramas novos e Questionário.
3. **Ato I** — Cenários 02 a 05.
4. **Ato II** — Cenários 06 a 09.
5. **Ato III** — Cenários 10 a 12.
6. **Ato IV** — Cenários 13 a 18, com a validação de RAM antes do 18.

A etapa 1 é pré-requisito de todas as outras. As etapas 3 a 6 são sequenciais por causa
da narrativa: cada Cenário parte da infraestrutura do anterior.

## Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| `use_lockfile` não funcionar contra o S3 do MiniStack | Cenário 16 perde a demonstração de lock concorrente | Validar empiricamente antes de escrever o Cenário. Fallback: backend S3 sem lock nativo, com o lock tratado como conceito e a lacuna registrada no texto. Não usar lock por DynamoDB, que o Terraform está descontinuando |
| Cenário 18 não caber em RAM com os três substratos simultâneos | Cenário final inviável na máquina | Medir antes. Se não couber, encolhe para dois substratos e o terceiro entra como inventário escrito |
| Provider Docker mantido por uma pessoa | Sustentação a longo prazo | Constraint pinada; a superfície usada é estável há várias majors |
| Bloco de portas 8070–8079 ocupado | Asserção reporta erro do app errado | Conferir na implementação, como o README já exige de qualquer Cenário novo |

## Fora de escopo

Ansible, Pulumi, CDK, CDKTF, Terragrunt, Conftest, OPA, Checkov e Terratest não são
instalados nem exercitados. Ansible, Pulumi, CDK e CloudFormation aparecem nos
Fundamentos como mapa do território. CI/CD hospedado também fica de fora: o Cenário 12
roda localmente a mesma sequência que um pipeline rodaria, o que ensina o conteúdo sem
exigir um runner.

## Validação antes de publicar

A lista completa está no [estudo de ferramental](../../research/iac-course.md). Os itens
bloqueantes são: `apply` idempotente com o provider Docker, `import` sem recriação,
`moved` preservando volume com dados, `terraform test` nos dois modos, provider
Kubernetes 3.2.1 contra `docker-desktop`, `use_lockfile` contra o MiniStack, e o canário
que confirma zero chamadas para `amazonaws.com`.
