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
- **Terraform 1.15.8.** Necessário apenas para a Trilha IaC; confirme com
  `terraform version`. A versão é fixa porque o conteúdo declara
  `required_version = "= 1.15.8"` e o plano exibido muda entre versões do provider.

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

### Preparando a Trilha IaC

Instale o Terraform 1.15.8 e confirme que ele está no PATH:

```powershell
terraform version
```

Nada mais precisa ser preparado. Os primeiros Cenários usam o provider
`kreuzwerker/docker`, que no Windows encontra sozinho o named pipe do Docker Desktop —
`provider "docker" {}` sem argumento nenhum funciona. Os Cenários 13, 14 e 18 exigem o
cluster `docker-desktop` da Trilha Kubernetes **no ar**, e os Cenários 15 a 18 exigem o
MiniStack da Trilha AWS — os mesmos pré-requisitos que você já preparou.

A Verificação nunca roda `apply`, `destroy` ou `init` por você: ela só observa o state e
pede um `plan`. Se uma Asserção de Terraform reclamar que não conseguiu planejar,
confirme que você rodou `terraform init` no diretório de trabalho.

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

### Preparando a Trilha Linux

Nada precisa ser baixado adiante: a imagem é construída pelo próprio Compose a partir do
`Dockerfile` de cada Cenário. O primeiro **Iniciar cenário** constrói a imagem e leva
alguns minutos; os seguintes aproveitam o cache de camadas e levam segundos.

Você entra na máquina pelo terminal dele, com:

```powershell
docker exec -it learning-infra-linux bash
```

Essa linha é uma incantação com prazo de validade — a Trilha Docker, logo em seguida,
explica cada pedaço dela. O container da Trilha Linux sobe o systemd com `cgroup: host`
no Compose: é o mesmo alcance da VM do Docker Desktop que a plataforma já usa no
MiniStack, e estritamente menos poder que montar o socket do Docker.

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

## Estudando uma Trilha

1. Escolha uma Trilha no catálogo. Quando houver **Fundamentos**, comece pelo artigo para
   construir o modelo mental ou vá direto à prática — a recomendação não bloqueia os
   Cenários.
2. Responda o Questionário sem consultar o texto. Com 80% de aproveitamento, os
   Fundamentos são concluídos; abaixo disso, o feedback leva às seções que vale revisar.
   As tentativas são ilimitadas e o melhor resultado fica salvo.
3. Escolha um Cenário. A etiqueta de Dificuldade diz o quanto ele entrega:
   `Guiado` dá todos os comandos, `Assistido` dá o objetivo e a forma, `Autônomo` dá só
   o objetivo, `Mestre` entrega um ambiente quebrado sem dizer o que quebrou.
4. Clique em **Iniciar cenário**. Isso derruba o ambiente do Cenário anterior e prepara
   o seu diretório de trabalho — o caminho absoluto aparece na tela, e é ele que você
   usa nos comandos com `-v`.
5. **Clique em Verificar antes de fazer qualquer coisa.** Todas as Asserções devem
   falhar. Se alguma passar de cara, ou o Cenário está mal escrito ou sobrou ambiente —
   nos dois casos é bug, não sucesso.
6. Faça o exercício no seu terminal.
7. Clique em **Verificar**. O checklist mostra cada Asserção separadamente, com o motivo
   ao lado das que falharam.

## Fundamentos e evolução das Trilhas

As cinco Trilhas publicadas oferecem **Fundamentos** e um Questionário com 12 situações,
aproveitamento recomendado de 80%, feedback por questão e links para revisar cada
conceito:

- **Linux:** kernel e espaço de usuário, tudo é arquivo, processo e sinal, identidade e
  permissão, shell, pacote, systemd, journald, rede no host e acesso mínimo;
- **Docker:** arquitetura, imagens, containers, isolamento, rede, persistência, Compose
  e distribuição;
- **Kubernetes:** estado desejado, control plane, reconciliação, workloads, rede,
  agendamento, persistência e mínimo privilégio;
- **AWS:** responsabilidade compartilhada, regiões e AZs, IAM, APIs, control plane e
  data plane, IaC, custo e guardrails do MiniStack;
- **Infraestrutura como Código:** imperativo e declarativo, o ciclo plan/apply/state,
  grafo de dependências, provider, módulos, drift e blast radius.

Fundamentos não transformam teoria em um Cenário artificial nem bloqueiam a prática. O
contrato está na [ADR 0003](docs/adr/0003-fundamentos-pertencem-a-trilha.md), os diagramas
de conteúdo na [ADR 0004](docs/adr/0004-diagramas-declarativos-no-conteudo.md), e a
expansão está registrada no
[plano de Fundamentos e Questionário por Trilha](docs/superpowers/plans/2026-08-07-fundamentos-questionario-por-trilha.md).

A expansão prática de Docker foi implementada e está no catálogo, nos Cenários 06 a 11:

| Cenário | Dificuldade | O que ensina |
|---|---|---|
| 06 — Uma imagem cara e lenta | Assistido | `.dockerignore`, ordenação de camadas e multi-stage |
| 07 — Só quem precisa se enxerga | Autônomo | redes de borda e interna, DNS por nome de serviço |
| 08 — Rodando ainda não é pronto | Assistido | healthcheck e `depends_on.condition: service_healthy` |
| 09 — O container com privilégios demais | Assistido | usuário não-root, read-only, `tmpfs` e `cap_drop` |
| 10 — Da tag ao digest | Assistido | registry local, push, pull e rollback por digest |
| 11 — Incidente final de Docker | Mestre | diagnóstico de stack com falhas combinadas |

A segunda rodada de conteúdo avançado — CI/CD, scan, SBOM e cadeia de fornecimento —
permanece planejada, mas não bloqueia a expansão atual.

A quarta Trilha, **Infraestrutura como Código**, tem Fundamentos publicados e os Atos I a IV completos: os 18 Cenários do desenho estão no catálogo. O estudo de ferramental está em
[`docs/research/iac-course.md`](docs/research/iac-course.md) e o desenho completo —
Fundamentos e 18 Cenários em quatro atos — em
[`docs/superpowers/specs/2026-08-09-trilha-iac-design.md`](docs/superpowers/specs/2026-08-09-trilha-iac-design.md).

A quinta Trilha, **Linux**, abre o catálogo — é a primeira da ordem recomendada de estudo.
Fundamentos publicados e os Cenários 01 a 14 em construção, com o Cenário 08 já no ar como
prova da plataforma. O desenho completo — Fundamentos e 14 Cenários em quatro atos — está em
[`docs/superpowers/specs/2026-08-21-trilha-linux-design.md`](docs/superpowers/specs/2026-08-21-trilha-linux-design.md).

## O que a plataforma mexe na sua máquina

| Caminho | O que é |
|---|---|
| `work/` | Diretório de trabalho. **Apagado e recriado a cada Iniciar** — não guarde nada seu aqui. |
| `data/progresso.json` | Qual Cenário está ativo, conclusões práticas e tentativas dos Fundamentos. |
| `content/` | Manifestos das Trilhas, Fundamentos, Questionários e Cenários. |
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

Reserve estas: **8088** (#1), **8089** (#2), **8090** (#3), **8091** (#4), **8092**
(#6), **8093** (#7), **8094** e **8095** (#8, api e web), **8096** (#9), **8097** (#10)
e **8098** (#11). O #5 não usa porta. O Cenário 10 também usa a **5000** (registry local
descartável) e o Cenário 11 usa a **9098** para a porta que o `db` expõe indevidamente
no estado quebrado. A Trilha AWS usa **4566** (MiniStack), **18080–18081** (tasks ECS),
**15432+** (RDS) e **16443+** (EKS/k3s). O backend fica na **8099** e o frontend na
**5180**.
A Trilha IaC usa o bloco **8070–8079**: 8070 no Cenário 01, 8071 no 02, 8072 e 8073 no 03, 8074 no 04, 8075 no 05, 8076 no 06, 8077 e 8078 no 08 e 8079 no 09. O Cenário 07 não publica porta nenhuma, de propósito.
A partir do Cenário 10 a Trilha IaC **reusa** o bloco 8070–8079: 8070 no 10, 8071 no 11 e
8072 no 12. O Ato IV fecha a trilha reusando o bloco mais uma vez: 8071 no 18, e o 13
publica o cluster pelo **NodePort 30070** — alcançado no navegador pela mesma 30070, via
`kubectl port-forward`. Como só existe um Cenário Ativo por vez, dois Cenários podem
declarar a mesma porta sem colidir.
A Trilha Linux usa o bloco **8040–8049**, começando pela **8040** no Cenário 08.

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

Cada diretório `content/<trilha>/` precisa de um `trilha.yaml`. Um diretório com Cenários
e sem manifesto é rejeitado para que a Trilha nunca volte a ser inferida implicitamente:

```yaml
id: docker
titulo: Docker
fundamentos:
  titulo: Fundamentos do Docker
  artigo: fundamentos.md
  questionario: questionario.yaml
  aproveitamentoMinimo: 80
```

As referências de Fundamentos são opcionais durante a migração de uma Trilha. Cada
Cenário continua sendo um diretório em `content/<trilha>/<slug>/`:

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
  backend e não podem ser substituídos no conteúdo. Para IaC há também
  `terraform_estado` e `terraform_plano_limpo`. Para Linux há também
  `servico_systemd` e `arquivo_linux`: o container vem do frontmatter `containerLinux` e
  não se repete em cada Asserção.
- `workspace/` — opcional. Copiado para `work/` no Iniciar. Se contiver um
  `compose.yaml` **e** o Cenário declarar `projetoCompose`, o stack sobe sozinho; é
  assim que um Cenário entrega ambiente pronto ou quebrado de propósito.

Artigos e Cenários podem incluir diagramas responsivos com um bloco cercado
`diagrama`. O corpo é um DSL YAML deliberadamente pequeno: `fluxo` e `ciclo` recebem
`passos`, `camadas` recebe `camadas`, e `comparacao` recebe `colunas`. Nós aceitam
`titulo`, `detalhe` e o `tom` opcional `neutro`, `destaque`, `sucesso`, `alerta` ou
`perigo`:

````markdown
```diagrama
tipo: fluxo
titulo: Da intenção à execução
visual: planos-aws
passos:
  - titulo: cliente
    detalhe: envia a intenção pela API
  - titulo: reconciliador
    detalhe: aproxima realidade e estado desejado
    tom: destaque
legenda: O conteúdo declara a relação; a interface cuida do desenho.
```
````

O campo opcional `visual` escolhe uma miniatura SVG mantida pelo frontend. Ele aceita
somente ids do catálogo fechado: `fronteiras-runtime`, `motor-docker`,
`filesystem-camadas`, `rotas-container`, `reconciliacao`, `arquitetura-cluster`,
`hierarquia-workload`, `service-endpoints`, `responsabilidade-aws`, `fronteiras-aws`,
`planos-aws` e `fidelidade-local`. O conteúdo continua legível como Markdown e nunca
carrega coordenadas, SVG, classes CSS, JSX, HTML ou JavaScript.

Antes de publicar, valide os diagramas e os links `revisar` do Questionário:

```sh
node frontend/scripts-checar-conteudo.mjs docker
node frontend/scripts-checar-conteudo.mjs kubernetes
node frontend/scripts-checar-conteudo.mjs aws
```

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

Um Cenário de IaC declara `terraform: true`. O campo opcional `diretorioTerraform`
aponta, **relativo ao `work/`**, onde ficam os arquivos `.tf`; omitido, vale a raiz:

```yaml
terraform: true
diretorioTerraform: infra
```

As duas Asserções tipadas recebem esse diretório do frontmatter e nunca o repetem:

```yaml
- tipo: terraform_estado
  endereco: docker_container.web
  atributo: name
  esperado: mirante-web
  descricao: o container está sob gestão do Terraform
- tipo: terraform_plano_limpo
  descricao: o código descreve a infraestrutura que está no ar
```

`terraform_estado` roda `terraform state show` e prova que o recurso nasceu do código;
sem `atributo` e `esperado`, ela apenas exige que o endereço exista no state.
`terraform_plano_limpo` roda `terraform plan -detailed-exitcode` e prova idempotência e
ausência de drift. As duas juntas impedem que a Verificação aprove uma infraestrutura
certa construída pelo caminho errado.

Um Cenário da Trilha Linux declara `containerLinux`, o nome do container que hospeda a
máquina:

```yaml
projetoCompose: linux-08
containerLinux: learning-infra-linux
```

As duas Asserções tipadas recebem esse nome do frontmatter e nunca o repetem:

```yaml
- tipo: servico_systemd
  nome: catalogo.service
  ativo: true
  habilitado: true
  descricao: o catálogo é um serviço ativo e habilitado
- tipo: arquivo_linux
  caminho: /etc/sudoers.d/plantao
  modo: "440"
  dono: root
  descricao: a regra de sudo não é editável por quem ela beneficia
```

`servico_systemd` roda `systemctl is-active --quiet` e `is-enabled --quiet` dentro do
container e compara **códigos de saída** — `0` é o único positivo, porque `is-active` de
uma unit parada imprime `inactive` e uma checagem por substring aprovaria um serviço
parado. `arquivo_linux` roda `stat -c` e compara modo, dono e grupo campo a campo; campos
nulos não são verificados. O `modo` precisa ser o valor canônico que `stat -c %a` imprime,
sempre entre aspas — um `0440` sem aspas é lido como octal pelo parser YAML e vira `288`.

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
- [`docs/research/docker-course.md`](docs/research/docker-course.md) — pesquisa curricular
  da Trilha Docker e base para Fundamentos, Questionário e novos Cenários.
- [`docs/superpowers/plans/`](docs/superpowers/plans/) — os planos de implementação, com
  o comportamento do CLI do Docker verificado em execução real. São documentos de
  construção, não de uso.
