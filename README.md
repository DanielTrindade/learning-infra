# Learning Infra

Laboratórios práticos de infraestrutura, para uso pessoal. Você lê a aula no navegador,
executa os comandos **no seu próprio terminal**, e a plataforma verifica o estado real da
sua máquina para dizer se o Cenário foi concluído.

Não há terminal embutido, e isso é decisão de projeto — veja
[ADR 0001](docs/adr/0001-sem-terminal-embutido.md).

## Antes de começar

- **Docker Desktop rodando.** `docker ps` precisa responder sem pendurar. Quase tudo
  aqui depende dele.
- **Java 25.** O Maven vem pelo wrapper (`./mvnw`), não precisa instalar.
- **Node 22+.**
- **AWS CLI v2.** Necessária apenas para a Trilha AWS; confirme com `aws --version`.

### Preparando a Trilha Kubernetes

O `kubectl` já vem com o Docker Desktop usado neste projeto; não é necessário instalar
`kind`, `minikube` nem Helm. Antes do primeiro Cenário Kubernetes:

1. Abra **Docker Desktop → Kubernetes**.
2. Crie o cluster com o provisionador **kind**.
3. Configure pelo menos **um control plane e dois workers**. O Cenário de agendamento
   precisa de dois Nodes que aceitem workloads.
4. Espere o cluster ficar pronto e confirme:

```powershell
kubectl --context docker-desktop get nodes
```

Todos os comandos e Verificações da Trilha fixam explicitamente o contexto
`docker-desktop`. Isso impede que um exercício atinja outro cluster que esteja selecionado
no seu `kubectl`.

### Preparando a Trilha AWS

A Trilha AWS usa **MiniStack**, não uma conta AWS e não LocalStack. Baixe uma vez a imagem
exata auditada pelo curso — a versão e o digest são fixos para uma atualização do registry
não mudar os laboratórios silenciosamente:

```powershell
docker pull ministackorg/ministack:1.4.13-full@sha256:07c3bc4b0eeb8f68669f2352cd624ccb09b477ab78b8b21ba1461f2035f3f610
```

No terminal em que você fará os exercícios, use somente credenciais sintéticas:

```powershell
$env:AWS_ACCESS_KEY_ID = "000000000000"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"
$env:AWS_EC2_METADATA_DISABLED = "true"
$endpoint = "http://127.0.0.1:4566"
```

Mesmo com esse ambiente, todos os comandos da aula trazem
`--endpoint-url http://127.0.0.1:4566`. Não o remova: esse é o guardrail visível que
impede a AWS CLI de atingir uma conta real. As credenciais sintéticas mantêm as
requisições assinadas para o MiniStack distinguir serviços REST com rotas parecidas,
como ECS e EKS, sem reutilizar nenhuma identidade real da máquina.

O emulador oferece fidelidades diferentes, e a trilha as trata explicitamente:

| Fidelidade local | Serviços e conceitos usados | O que a conclusão realmente prova |
|---|---|---|
| Data plane local | S3, DynamoDB e SQS | objetos, itens, mensagens, retries e eventos reais no processo local |
| Infraestrutura substituta | RDS/PostgreSQL, Aurora compatível, ECR, ECS, ALB e EKS/k3s | SQL, containers, registry, HTTP e workloads reais, mas não o serviço gerenciado da AWS |
| Somente control plane | EC2, VPC, subnets, rotas, security groups e Route 53 | metadata, relações e IaC; não há VM, isolamento de pacotes nem propagação DNS real |

Os Cenários 11, 12 e 14–16 montam `/var/run/docker.sock` para criar sidecars reais. Isso
dá ao MiniStack controle sobre o Docker da máquina; execute somente o conteúdo versionado
do projeto. Ao reiniciar ou trocar de Cenário AWS, a plataforma remove o MiniStack e todos
os containers com o label global `ministack`. Portanto, **não rode outro projeto MiniStack
ao mesmo tempo**.

## Rodando

Dois processos, em dois terminais. Nenhum dos dois é o terminal onde você vai fazer os
exercícios — use um terceiro para isso.

```sh
# terminal 1 — backend em 127.0.0.1:8099
cd backend
./mvnw spring-boot:run
```

```sh
# terminal 2 — frontend em localhost:5180
cd frontend
npm install     # só na primeira vez
npm run dev
```

Abra **<http://localhost:5180>**.

> A porta é **5180**, não a 5173 padrão do Vite. A 5173 costuma já estar ocupada por
> outro projeto, e o `strictPort` está ligado justamente para o Vite falhar em vez de
> deslizar em silêncio para outra porta e te fazer abrir o app errado.

## Fazendo uma lição

1. Escolha um Cenário no catálogo. A etiqueta de dificuldade diz o quanto ele entrega:
   `Guiado` dá todos os comandos, `Assistido` dá o objetivo e a forma, `Autônomo` dá só
   o objetivo, `Mestre` entrega um ambiente quebrado sem dizer o que quebrou.
2. Clique em **Iniciar cenário**. Isso derruba o ambiente do Cenário anterior e prepara
   o seu diretório de trabalho — o caminho absoluto aparece na tela, e é ele que você
   usa nos comandos com `-v`.
3. **Clique em Verificar antes de fazer qualquer coisa.** Todas as Asserções devem
   falhar. Se alguma passar de cara, ou o Cenário está mal escrito ou sobrou ambiente —
   nos dois casos é bug, não sucesso.
4. Faça o exercício no seu terminal.
5. Clique em **Verificar**. O checklist mostra cada Asserção separadamente, com o motivo
   ao lado das que falharam.

## O que a plataforma mexe na sua máquina

| Caminho | O que é |
|---|---|
| `work/` | Diretório de trabalho. **Apagado e recriado a cada Iniciar** — não guarde nada seu aqui. |
| `data/progresso.json` | Qual Cenário está ativo e quais você concluiu. |
| `content/` | Os Cenários. É aqui que você edita ou escreve conteúdo. |
| namespace `learning-infra-k8s-*` | Ambiente descartável de um Cenário Kubernetes. |
| container `learning-infra-ministack` | Endpoint AWS local e efêmero do Cenário ativo. |
| containers `ministack-*` | PostgreSQL, tasks ECS ou k3s criados nos Cenários com infraestrutura substituta. |

Iniciar um Cenário **remove** os containers, projetos Compose e volumes declarados pelo
Cenário anterior. Isso é intencional: sem essa limpeza, sobra de ambiente faria a
Verificação aprovar você por engano. Veja
[ADR 0002](docs/adr/0002-um-cenario-ativo-por-vez.md).

Para Kubernetes, o isolamento é por namespace. Iniciar recria o namespace do Cenário;
trocar de Cenário apaga o namespace anterior. Recursos de sistema e outros namespaces do
cluster não são alterados. Alguns Cenários `Mestre` também aplicam automaticamente o
ambiente quebrado que você deve diagnosticar.

Para AWS, Iniciar recria o container do MiniStack com persistência desligada. Recursos,
mensagens e objetos do Cenário anterior somem. A plataforma também remove sidecars e
volumes anônimos; isso evita que uma Verificação aprove por causa de um RDS, ECS ou EKS
esquecido da tentativa anterior.

Só um Cenário fica ativo por vez. Trocar de Cenário perde o que você não tiver salvo
fora do `work/`.

### Portas usadas pelos Cenários

Reserve estas: **8088** (#1), **8089** (#2), **8090** (#3), **8091** (#4). O #5 não usa
porta. A Trilha AWS usa **4566** (MiniStack), **18080–18081** (tasks ECS), **15432+**
(RDS) e **16443+** (EKS/k3s). O backend fica na **8099** e o frontend na **5180**.

Se for escrever um Cenário novo, escolha a porta conferindo o que já roda na sua
máquina. A 8080 parece a escolha óbvia e é justamente a mais arriscada — quando ela está
ocupada por outra aplicação, a Asserção recebe um `404` do app errado e reporta algo que
parece defeito do seu exercício.

## Quando algo der errado

**A porta está ocupada mas nada parece estar rodando.** Matar `./mvnw` ou `npm run dev`
mata só o wrapper; o processo filho (a JVM ou o `vite`) sobrevive e continua segurando a
porta. Ache o dono e confira antes de matar:

```powershell
Get-NetTCPConnection -LocalPort 8099 -State Listen |
  ForEach-Object { Get-CimInstance Win32_Process -Filter "ProcessId=$($_.OwningProcess)" } |
  Select-Object ProcessId, CommandLine
```

**A Verificação diz que nada respondeu, mas o `curl` funciona.** Confira se o backend em
execução é mesmo o que você acabou de compilar — provavelmente há uma JVM antiga na
8099, pelo motivo acima.

**Sobrou ambiente de um Cenário anterior.** O teardown roda ao iniciar outro Cenário. Na
mão:

```sh
docker rm -f <container>
docker compose -p <projeto> down -v
docker volume rm -f <volume>
```

## Escrevendo um Cenário

Cada Cenário é um diretório em `content/<trilha>/<slug>/`:

- `cenario.md` — frontmatter com `id`, `titulo`, `dificuldade` e, se precisar,
  `containers`, `projetoCompose` e `volumes` (o que o teardown vai remover). O corpo é o
  conteúdo do Cenário em Markdown.
- `verificacao.yaml` — as Asserções. Tipos disponíveis: `container_rodando`,
  `http_responde`, `http_corpo_contem`, `imagem_existe`, `volume_existe` e
  `comando_produz`. Para Kubernetes há também `kubernetes_condicao`,
  `kubernetes_jsonpath` e `kubernetes_rbac`; contexto e namespace vêm do frontmatter,
  não se repetem em cada Asserção. Conditions e JSONPath esperam convergência por até
  dez segundos por padrão, em vez de depender de um `sleep` fixo. Para AWS há
  `aws_consulta`: o endpoint local, região e credenciais sintéticas são injetados pelo
  backend e não podem ser substituídos no conteúdo.
- `workspace/` — opcional. Copiado para `work/` no Iniciar. Se contiver um
  `compose.yaml` **e** o Cenário declarar `projetoCompose`, o stack sobe sozinho; é
  assim que um Cenário entrega ambiente pronto ou quebrado de propósito.

Um Cenário Kubernetes declara `contextoKubernetes` e `namespaceKubernetes` juntos. Para
aplicar um ambiente inicial ao Iniciar, declare também `manifestosIniciais`, relativo ao
`workspace/`:

```yaml
contextoKubernetes: docker-desktop
namespaceKubernetes: learning-infra-k8s-14
manifestosIniciais: setup
```

Mantenha os recursos do Cenário dentro desse namespace. A plataforma não limpa recursos
cluster-scoped, justamente para não remover configuração compartilhada por acidente.

Um Cenário AWS declara `ministack: true`. Use `infraestruturaRealAws: true` somente para
RDS, ECS, EKS ou outro exercício que realmente precise criar containers pelo socket. Um
diretório de scripts de inicialização pode ser montado no hook `ready.d` com caminho
relativo ao `workspace/`:

```yaml
ministack: true
infraestruturaRealAws: true
inicializacaoAws: init
```

Uma consulta tipada ao estado local fica em `verificacao.yaml`:

```yaml
- tipo: aws_consulta
  servico: s3api
  operacao: get-bucket-versioning
  argumentos: ["--bucket", "learning-infra-arquivos"]
  consulta: Status
  esperado: Enabled
  descricao: o bucket mantém histórico de versões
```

Não há cadastro em banco nem passo de build: criar o diretório basta, e o catálogo o
encontra na próxima chamada.

Para acrescentar um tipo de Asserção, comece pelo record em `Assercao.java`. O `switch`
do `MotorDeVerificacao` é exaustivo e **vai quebrar a compilação** até você tratar o caso
novo. Esse erro é proposital; não o resolva com um `default`.

## Testes

```sh
cd backend && ./mvnw test      # precisa do Docker no ar
cd frontend && npm run build   # typecheck e bundle
```

## Onde está o resto da documentação

- [`CONTEXT.md`](CONTEXT.md) — o vocabulário do domínio. Leia antes de escrever Cenário.
- [`docs/adr/`](docs/adr/) — decisões de arquitetura e o porquê delas.
- [`docs/research/kubernetes-course.md`](docs/research/kubernetes-course.md) — pesquisa e
  decisões curriculares da Trilha Kubernetes, com fontes primárias.
- [`docs/research/aws-local-lab.md`](docs/research/aws-local-lab.md) — matriz de
  compatibilidade do MiniStack, riscos e decisões curriculares da Trilha AWS.
- [`docs/superpowers/plans/`](docs/superpowers/plans/) — os planos de implementação, com
  o comportamento do CLI do Docker verificado em execução real. São documentos de
  construção, não de uso.
