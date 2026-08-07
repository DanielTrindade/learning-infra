# Pesquisa — laboratório AWS local com MiniStack

> Estado: recomendação para implementação
>
> Data da pesquisa: 2026-08-07
>
> Escopo: Windows + Docker Desktop, AWS CLI e MiniStack; Terraform recomendado para a trilha

## Resumo executivo

O nome correto é **MiniStack**: um projeto próprio, recente, diferente do LocalStack,
mantido em [`ministackorg/ministack`](https://github.com/ministackorg/ministack). Ele
é MIT, não exige conta, token ou plano pago e expõe mais de 60 superfícies AWS pelo
endpoint padrão `http://localhost:4566`. A documentação, entretanto, registra apenas
10 serviços como plenamente funcionais e mais de 45 como parciais ou com diferenças
deliberadas. Portanto, “suportado” não pode ser tratado como “reproduz fielmente a
AWS”. [Catálogo oficial](https://ministack.org/docs/services/),
[limitações oficiais](https://ministack.org/docs/limitations)

Todos os serviços pedidos podem participar da trilha, mas em três categorias:

1. **data plane útil para exercícios:** S3, DynamoDB e SQS;
2. **control plane emulado com infraestrutura local real:** RDS/Aurora, ECS, EKS,
   ECR e parte do ALB;
3. **modelo estrutural/IaC, sem o comportamento físico da AWS:** EC2, VPC, subnets,
   route tables, NAT/IGW, security groups e Route 53.

A recomendação é uma trilha de **18 Cenários**, combinando AWS CLI e Terraform. Ela
deve ensinar explicitamente a distinguir a intenção declarada no control plane do
comportamento observável no data plane. VPC, subnet pública/privada, security group,
EC2 e Route 53 continuam úteis para modelagem e diagnóstico de IaC, mas não podem ter
Asserções que aleguem isolamento de rede, uma VM real ou propagação DNS.

## Método e fontes

A consulta começou pelo Context7, como exigido. O catálogo resolveu a documentação
oficial do LocalStack em `/localstack/localstack-docs`; não foi encontrada uma
biblioteca própria do MiniStack. O Context7 serviu para confirmar a diferença de
produto e os tiers atuais do LocalStack. A compatibilidade do MiniStack foi então
validada somente em fontes do próprio projeto: documentação por serviço, lista de
limitações, repositório, changelog e releases oficiais.

Há uma discrepância importante entre documentação e artefato publicado: algumas
páginas já dizem “MiniStack 1.4.14”, mas a release oficial mais recente em 2026-08-07
é **v1.4.13**, publicada em 2026-08-06. A implementação deve fixar o artefato
publicado, não antecipar a versão indicada pelas páginas geradas.
[Releases oficiais](https://github.com/ministackorg/ministack/releases)

Inspeção não destrutiva do registry em 2026-08-07:

| Imagem | Digest OCI multiarch |
|---|---|
| `ministackorg/ministack:1.4.13` | `sha256:c861d75d1499019a4891790899a0b2001f8c36bd788d48dc0610a1189f117c0c` |
| `ministackorg/ministack:1.4.13-full` | `sha256:07c3bc4b0eeb8f68669f2352cd624ccb09b477ab78b8b21ba1461f2035f3f610` |

A edição `full` inclui drivers nativos PostgreSQL/MySQL usados por recursos como RDS
Data API; a edição padrão é menor. A documentação explica a diferença entre as
imagens. [Configuração oficial](https://ministack.org/docs/configuration)

## Matriz de compatibilidade

| Serviço ou conceito | Nível no MiniStack 1.4.13 | O que é exercitável | Limites que o curso deve declarar |
|---|---|---|---|
| **S3** | Data plane funcional | bucket, objeto, multipart, versionamento, lifecycle, CORS, tags, Object Lock, range e notificações rastreadas | SSE-KMS não cifra os bytes; configuração de replicação é aceita, mas não replica. [S3](https://ministack.org/docs/services/s3) |
| **DynamoDB** | Data plane funcional | tabelas, CRUD, Query/Scan, GSI, TTL, transações, PartiQL e Streams | um shard sintético; Global Tables não replicam entre regiões. [DynamoDB](https://ministack.org/docs/services/dynamodb) |
| **SQS** | Data plane funcional | standard/FIFO, visibility timeout, lotes, DLQ/redrive e tags | métricas aproximadas não são emitidas; tempo exato de atraso depende do scheduler local. [SQS](https://ministack.org/docs/services/sqs) |
| **RDS** | Híbrido | API de controle emulada; Postgres, MySQL e MariaDB executados em containers e acessíveis por cliente SQL | snapshots não são dumps reais; event subscription não publica em SNS; subnet/security group não filtra pacotes. [RDS](https://ministack.org/docs/services/rds) |
| **Aurora** | Substituto funcional, não Aurora real | DB Cluster, membros compartilhando um banco compatível e RDS Data API | não há storage distribuído Aurora, quorum, AZs reais, failover operacional nem performance Aurora. Serve para API e SQL, não para provar HA. [RDS](https://ministack.org/docs/services/rds), [changelog v1.4.4](https://ministack.org/blog/changelog-v1-4-4) |
| **ECS** | Híbrido | cluster, task definition, service e `RunTask`; tasks são containers Docker reais | não é Fargate nem host EC2; `awslogs` não entrega logs ao CloudWatch; mudança de task não dispara EventBridge. [ECS](https://ministack.org/docs/services/ecs) |
| **EC2** | Somente control plane/metadados | instâncias, estados, launch templates, key pairs, volumes, ENIs e endereços | não inicia VM, não oferece SSH/cloud-init e não executa rede nem autorização IAM. [EC2](https://ministack.org/docs/services/ec2) |
| **EKS** | Híbrido | API EKS cria um sidecar k3s real; `kubectl` e workloads Kubernetes funcionam | é k3s, não o control plane EKS; Fargate Profile não é exposto; addons/access entries não reproduzem integração AWS completa. [EKS](https://ministack.org/docs/services/eks) |
| **VPC** | Somente control plane/metadados | VPC, IGW, NAT Gateway, route tables, NACL, endpoint, peering, ENI e SG | nenhuma rota encaminha pacotes; NACL e SG não filtram tráfego. [EC2](https://ministack.org/docs/services/ec2), [limitações](https://ministack.org/docs/limitations) |
| **Subnets públicas e privadas** | Modelo de IaC | CIDRs, AZs, associação com route table, rota para IGW/NAT e `MapPublicIpOnLaunch` | “pública” e “privada” são conclusões estruturais sobre rotas; não existe isolamento real. |
| **ALB / ELBv2** | Control plane + data plane parcial | load balancer, target group, listener, rules e roteamento local por host/path para targets suportados | health checks são armazenados, mas não executados; cross-zone não tem efeito; subnet/SG não controla o fluxo. [ALB/ELBv2](https://ministack.org/docs/services/elbv2) |
| **Route 53** | Control plane/metadados | hosted zones públicas/privadas, records, aliases, traffic policies e health-check CRUD | não altera o resolver do host; health checks não mudam de estado; DNSSEC é metadado. [Route 53](https://ministack.org/docs/services/route53) |
| **ECR** | API + registry local | repositories, layers/manifests, autenticação, push/pull e lifecycle policy; EKS pode consumir imagem local | token é fixo e não tem valor AWS; image scanning retorna achados vazios. [ECR](https://ministack.org/docs/services/ecr), [integração EKS/ECR](https://ministack.org/blog/changelog-v1-4-1) |

### Resposta direta: o que vale usar

**Núcleo comportamental confiável para o laboratório:** S3, DynamoDB e SQS.

**Bom para exercícios realmente executáveis, com ressalvas:** RDS/Postgres,
Aurora-compatible, ECR, ECS, ALB e EKS/k3s.

**Bom somente para ensinar IaC, relações, inventário e diagnóstico do control plane:**
EC2, VPC, subnet pública/privada, route tables, NAT Gateway, Internet Gateway,
security groups, NACL e Route 53.

Essa separação precisa aparecer na própria interface da plataforma, por exemplo com
badges “Data plane local”, “Infraestrutura substituta” e “Somente control plane”.

## MiniStack não é LocalStack

O MiniStack declara licença MIT, ausência de cadastro/chave e nenhum tier
Community/Pro. Todos os serviços acima pertencem ao mesmo projeto gratuito.
[Repositório e licença](https://github.com/ministackorg/ministack)

Se “MiniStack” tiver sido um engano e a intenção for **LocalStack**, a decisão muda.
Desde 23 de março de 2026, os planos comerciais são Base, Ultimate e Enterprise; o
Hobby permanece uma faixa separada. A matriz oficial atual põe os serviços em tiers
diferentes e avisa que presença na tabela não significa cobertura completa de APIs:

| LocalStack | Menor faixa indicada na matriz atual |
|---|---|
| S3, DynamoDB, SQS, EC2/VPC e Route 53 | Hobby |
| ECR, ECS, RDS/Aurora e ELB/ELBv2 | Base |
| EKS | Ultimate |

Fonte: [planos oficiais do LocalStack](https://docs.localstack.cloud/aws/licensing/).

## Arquitetura local recomendada para este repositório

### 1. Uma conta efêmera e um endpoint que nunca podem apontar para AWS real

Todo comando da plataforma deve injetar, e nunca aceitar do conteúdo:

```text
endpoint: http://127.0.0.1:4566
region: us-east-1
access key: uma conta sintética de 12 dígitos exclusiva do Cenário
secret: test
```

O MiniStack deriva a conta de uma access key numérica de 12 dígitos. Isso permite
nomes iguais sem colisão entre Cenários. Mesmo assim, os comandos apresentados ao
aluno devem passar `--endpoint-url` explicitamente ou usar um wrapper do repositório,
pois executar AWS CLI/CDK/Terraform sem override pode alcançar uma conta AWS real.
[Multi-tenancy e AWS CLI](https://github.com/ministackorg/ministack#using-with-aws-cli)

Guardrails obrigatórios:

- nunca ler `~/.aws/credentials` para o lab;
- nunca permitir access keys começando com `AKIA`/`ASIA`;
- o Verificador constrói endpoint, conta e região; o YAML do Cenário não os sobrescreve;
- antes de qualquer mutação, `GET /_ministack/health` deve identificar o emulador;
- Terraform deve declarar endpoint por serviço e os três `skip_*` de validação;
- nenhuma Asserção de laboratório pode omitir o endpoint explícito.

### 2. Container pinado e estado efêmero

Usar, para a trilha completa:

```text
ministackorg/ministack:1.4.13-full@sha256:07c3bc4b0eeb8f68669f2352cd624ccb09b477ab78b8b21ba1461f2035f3f610
```

Não usar `latest` nem `full` sem versão. Começar com:

```text
PERSIST_STATE=0
S3_PERSIST=0
RDS_PERSIST=0
```

Persistência é o assunto de um Cenário, não uma propriedade escondida do ambiente.
No modo efêmero, cada execução é determinística. A documentação separa persistência
de metadados, bytes S3 e volumes RDS. [Configuração](https://ministack.org/docs/configuration)

O ciclo **Iniciar** precisa limpar também quando o mesmo Cenário é iniciado de novo.
O gerenciador atual só derruba Compose ao trocar de Cenário; `docker compose up` no
mesmo projeto não zera o estado. Para AWS, o lifecycle deve:

1. apagar, via APIs, RDS/ECS/EKS criados pelo Cenário anterior;
2. aguardar os sidecars terminarem;
3. chamar `POST /_ministack/reset?init=1` ou recriar o Compose;
4. aguardar `/_ministack/ready` antes de liberar a UI.

### 3. Docker socket somente quando necessário

S3, DynamoDB, SQS, EC2/VPC, Route 53 e ECR não precisam receber o socket para seus
exercícios básicos. RDS, ECS e EKS precisam dele para criar sidecars/containers reais.
Separar dois perfis Compose:

- `aws-control-plane`: sem socket;
- `aws-real-infra`: socket montado, rede Docker exclusiva e orçamento de recursos.

Montar `/var/run/docker.sock` dá ao processo dentro do MiniStack controle equivalente
ao administrador do Docker host. EKS ainda inicia k3s privilegiado. Esses Cenários
devem executar apenas código versionado e confiável, nunca uploads arbitrários, e não
devem rodar num runner compartilhado.
[Testcontainers Java](https://ministack.org/docs/testcontainers-java),
[EKS no README oficial](https://github.com/ministackorg/ministack#eks-with-real-kubernetes-k3s)

O Testcontainers oficial para Java oferece reaping de sidecars ao chamar `stop()`, mas
o modelo atual da aplicação mantém um Cenário vivo entre requisições. Há duas opções:

- integrar `MiniStackContainer` ao `GerenciadorDeCenarioAtivo` e guardar sua instância;
- continuar com Compose e implementar inventário/teardown explícito dos recursos
  RDS, ECS e EKS antes do `compose down`.

Para esta base, a segunda opção altera menos o desenho existente, mas só é aceitável
se o teste de lifecycle provar que nenhum container ou volume `ministack-*` sobra após
Iniciar novamente e após trocar de Cenário.

### 4. IaC como interface principal, CLI como instrumento de diagnóstico

A máquina já possui Docker Engine 29.6.1, Docker Compose 5.2.0 e AWS CLI 2.31.22.
Terraform não foi encontrado na auditoria de 2026-08-07 e precisa ser instalado e
pinado antes da implementação da fase IaC.

Progressão recomendada:

- Cenários 01–05: AWS CLI para enxergar APIs, estados e erros;
- Cenários 06–18: Terraform para intenção, dependências, plan/apply/state e drift;
- AWS CLI continua como ferramenta de inspeção, nunca como substituto silencioso da
  correção declarativa.

O MiniStack documenta compatibilidade com AWS Provider v5/v6 e endpoint override por
serviço. [IaC no MiniStack](https://ministack.org/docs/iac)

## Trilha recomendada — 18 Cenários

| # | Cenário | Dificuldade | Problema prático dominante | Evidência real |
|---:|---|---|---|---|
| 01 | Conta, região e endpoint sem sustos | Guiado | provar que todos os comandos atingem o emulador, não AWS | STS/health e conta sintética |
| 02 | S3: versões que salvam e policies que enganam | Guiado | recuperar objeto sobrescrito e corrigir lifecycle/public access | bytes e versões reais |
| 03 | DynamoDB: chave ruim custa caro | Assistido | redesenhar partition/sort key e GSI para evitar Scan | CRUD/Query/condição reais |
| 04 | SQS: recebeu não significa processou | Assistido | visibility timeout causa duplicata; configurar retry e DLQ | mensagens e redrive reais |
| 05 | Pipeline S3 → SQS → DynamoDB | Assistido | evento duplicado exige consumidor idempotente | fluxo entre três data planes |
| 06 | Terraform sem atalhos locais | Assistido | provider aponta parcialmente para AWS ou drift não é entendido | `plan`, `apply` e state locais |
| 07 | VPC em duas AZs | Assistido | CIDRs sobrepostos e associações de route table incorretas | somente estrutura EC2/IaC |
| 08 | Pública não é nome de subnet | Autônomo | identificar IGW, NAT e rotas que definem pública/privada | relações estruturais, sem tráfego |
| 09 | EC2, launch template e security groups | Autônomo | regra excessiva, perfil e user data incoerentes | control plane; nenhuma VM/SSH |
| 10 | Route 53 pública, privada e alias | Autônomo | zona/record/alias errados e falsa expectativa de DNS local | record sets; não resolver host |
| 11 | RDS PostgreSQL realmente aceita SQL | Assistido | DB subnet group incorreto, credencial sintética e migração falhando | container DB + consulta SQL |
| 12 | Aurora: API compatível, arquitetura diferente | Autônomo | writer/reader e Data API compartilham dados; discutir falha não simulada | SQL real, HA apenas conceitual |
| 13 | ECR: do build ao registry | Assistido | tag mutável, lifecycle e pull de imagem inexistente | push/pull de imagem local |
| 14 | ECS: task definition não garante task saudável | Autônomo | command/env/porta errados em container real | container Docker executado |
| 15 | ALB → ECS: listener existe, rota não funciona | Autônomo | regra host/path ou target group incorretos | HTTP local via data plane ALB |
| 16 | EKS local consumindo ECR | Mestre | cluster k3s, kubeconfig isolado e ImagePull corrigido | workload Kubernetes real |
| 17 | Aplicação três camadas degradada | Mestre | ALB/ECS/RDS/SQS parecem culpados; diagnóstico por camadas | HTTP, container, SQL e fila |
| 18 | Incidente final e plano de migração para AWS | Mestre | corrigir stack Terraform multi-serviço e listar tudo que o emulador não provou | contratos locais + checklist cloud |

Estimativa: **28–36 horas**. EKS aparece perto do fim porque consome mais recursos e
a trilha Kubernetes já ensina os objetos do cluster; aqui o foco é o control plane
EKS, registry, kubeconfig, entrega e limites de fidelidade.

### Conceitos transversais obrigatórios

- account/region/ARN, tags e eventual consistency;
- control plane versus data plane;
- idempotência e operações condicionais;
- IaC, dependências, state e drift;
- responsabilidade compartilhada e menor privilégio — sem fingir enforcement IAM;
- disponibilidade, AZ e failover como conceitos que exigem validação posterior na AWS;
- custo, quotas e observabilidade como diferenças não reproduzidas localmente;
- contrato de portabilidade: código não pode espalhar `localhost:4566`; endpoint vem
  de configuração exclusiva do ambiente local.

## Verificação automatizável

O motor deve ganhar Asserções tipadas, não apenas procurar substrings na tabela da
AWS CLI:

1. `aws_api`
   - serviço, operação, argumentos permitidos, JMESPath, operador tipado e valor;
   - endpoint, conta e região injetados pela plataforma.
2. `aws_aguarda`
   - mesmo contrato de `aws_api`, com polling e timeout para RDS/ECS/EKS.
3. `aws_s3_objeto`
   - bucket/key, existência, version count, hash ou conteúdo sintético esperado.
4. `aws_dynamodb_item`
   - tabela, chave, condição/atributos esperados; sem Scan indiscriminado.
5. `aws_sqs_fila`
   - atributos, redrive policy e mensagem observável sem apagar a evidência.
6. `sql_consulta`
   - driver/endereço fornecidos pela plataforma, query somente leitura e resultado.
7. `http_responde`
   - status/header/body, host/path e timeout; útil no ALB/ECS.
8. `container_estado`
   - nome/labels/imagem/exit code do container criado pelo ECS, sem aceitar container
     alheio como evidência.
9. Reutilizar `kubernetes_condicao` e `kubernetes_jsonpath` no Cenário EKS, mas com
   kubeconfig exclusivo extraído do sidecar k3s.
10. `terraform_plano`
    - `validate`, `show -json`, ausência de mudanças após solução e recursos esperados.

### Regras contra falso positivo

- Separar Asserções de **estrutura**, **data plane real** e **não comprovável localmente**.
- Nunca aprovar “subnet privada” por nome/tag; verificar route table e ausência de rota
  direta para IGW, declarando que isso não testou pacotes.
- Nunca aprovar security group, NACL ou IAM por um `curl` que MiniStack não filtra.
- Nunca aprovar EC2 por container Docker; EC2 é apenas metadado no MiniStack.
- Aurora não recebe prova de Multi-AZ/failover; o Cenário exige que o aluno registre a
  lacuna e o teste que faria em AWS.
- ALB health check não é evidência, pois não roda. Verificar a rota HTTP diretamente.
- Route 53 é verificado com `ListResourceRecordSets`, não por `nslookup` do host.
- Usar polling com timeout para DB instance, ECS task e EKS cluster; nenhum `sleep`
  fixo.
- A Verificação deve falhar antes da solução e ser estritamente observadora.
- Reiniciar o mesmo Cenário deve zerar conta, recursos, sidecars e volumes efêmeros.

## O que precisa ser validado antes de publicar a trilha

1. Subir a imagem pinada cinco vezes do zero no Docker Desktop.
2. Provar limpeza dos sidecars RDS, ECS e EKS em reinício e troca de Cenário.
3. Provar S3, DynamoDB e SQS com AWS CLI 2.31.22.
4. Fixar uma versão do Terraform e do AWS Provider que passe todos os Cenários.
5. Testar push/pull ECR real e consumo pela task ECS e pelo k3s.
6. Testar ALB host/path routing para uma task ECS local.
7. Testar RDS PostgreSQL e Aurora-compatible com SQL e, se usado, Data API na imagem
   `full`.
8. Medir RAM/CPU/disco de RDS + ECS e de EKS isoladamente; não rodar todos os sidecars
   no capstone se a máquina não tiver margem.
9. Rodar um canário que detecta qualquer chamada para `amazonaws.com`; o resultado
   esperado é zero.
10. Registrar no Cenário final quais testes ainda devem rodar numa conta sandbox AWS.

## Recomendação final

Adotar **MiniStack 1.4.13 pinado** é uma escolha viável para esta plataforma, desde
que a trilha trate o emulador como um conjunto de implementações com fidelidades
diferentes, não como “uma AWS completa no Docker”.

O maior valor pedagógico virá de três coisas:

1. exercitar S3, DynamoDB, SQS, SQL, containers, registry e Kubernetes de verdade;
2. aprender a modelar VPC/EC2/Route 53 declarativamente sem confundir resource CRUD
   com infraestrutura funcionando;
3. terminar cada módulo dizendo explicitamente **o que foi provado localmente e o que
   ainda exige uma conta sandbox AWS**.

