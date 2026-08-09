# Fundamentos da AWS

AWS não é “um datacenter acessado pelo navegador”. É uma coleção de serviços expostos
por APIs, distribuídos por contas, regiões e zonas de disponibilidade, em que cada
decisão deixa três efeitos diferentes: um recurso técnico, uma superfície de segurança
e uma consequência de custo. Antes de memorizar nomes de produtos, vale construir um
modelo mental que responda: onde o recurso vive, quem pode alterá-lo, qual plano está
sendo observado e o que aquela evidência realmente prova.

Esta leitura prepara os Cenários práticos. Ela não substitui o terminal: ao terminar,
faça o Questionário para descobrir o que revisar e siga para os laboratórios mesmo que
ainda não tenha atingido 80%. A recomendação não é um bloqueio.

## O problema: infraestrutura virou uma API

Num datacenter tradicional, obter um servidor envolve capacidade física, cabeamento,
instalação e uma sequência de chamados. Na nuvem, a mesma intenção começa com uma
requisição autenticada: criar um bucket, reservar uma rede, registrar uma task, gravar
uma rota. A velocidade muda o trabalho. Provisionar fica barato em tempo, mas também
fica fácil criar recursos no lugar errado, com acesso excessivo ou sem saber quem pagará
por eles.

Uma resposta `200`, `CREATE_COMPLETE` ou estado `running` confirma apenas que o plano de
controle aceitou e registrou uma operação. Ela não garante que a aplicação responde,
que a arquitetura é resiliente, que o dado está protegido nem que a conta chegará ao fim
do mês dentro do orçamento. Na AWS, operar bem é acumular evidências independentes em
vez de aceitar uma luz verde genérica.

## Responsabilidade compartilhada

A AWS protege a infraestrutura **da** nuvem: datacenters, hardware, rede física e a
camada que oferece os serviços. O cliente protege o que coloca **na** nuvem:
identidades, dados, configuração, exposição de rede, sistema operacional quando ele é
gerenciado pelo cliente e o código da aplicação.

```diagrama
tipo: comparacao
visual: responsabilidade-aws
titulo: Segurança da nuvem × na nuvem
colunas:
  - titulo: Segurança da nuvem · AWS
    itens:
      - datacenter, hardware e rede física
      - hipervisor e base dos serviços
      - disponibilidade contratada
  - titulo: Segurança na nuvem · cliente
    tom: destaque
    itens:
      - identidades e permissões
      - dados e criptografia
      - rede, sistema, código e logs
legenda: Serviços gerenciados movem a fronteira operacional, não a responsabilidade pelos dados.
```

O corte depende do serviço. Em EC2, o cliente atualiza o sistema operacional e o
runtime; em RDS, a AWS opera boa parte dessas camadas. Isso não torna o banco “seguro
por padrão”: grupos de segurança, credenciais, parâmetros, backups e uso da aplicação
continuam sob controle do cliente. Serviço gerenciado reduz trabalho indiferenciado;
não terceiriza arquitetura nem responsabilidade pelos dados.

## Conta, região, zona e endereço do recurso

A **conta AWS** é a principal fronteira administrativa e de cobrança. Dentro dela,
recursos são criados em uma **região**, como `us-east-1`. Cada região reúne múltiplas
**zonas de disponibilidade** (AZs), instalações fisicamente separadas e conectadas por
rede de baixa latência. Alguns serviços são globais; muitos são regionais; recursos
como uma subnet pertencem a uma única AZ.

```diagrama
tipo: camadas
visual: fronteiras-aws
titulo: Conta → região → AZ → recurso
camadas:
  - titulo: conta
    detalhe: identidade, políticas, quotas e cobrança
    tom: destaque
  - titulo: região
    detalhe: fronteira geográfica e de endpoints
  - titulo: zona de disponibilidade
    detalhe: domínio de falha; subnets vivem aqui
  - titulo: recurso
    detalhe: ARN, tags e estado
    tom: sucesso
legenda: Multi-AZ só existe quando dependências atravessam domínios de falha reais.
```

Duas subnets com nomes `public-a` e `public-b` na mesma AZ continuam expostas ao mesmo
domínio de falha. Da mesma forma, copiar dados para outra AZ não protege de uma falha
regional. O desenho começa pela falha que você precisa tolerar, pelo objetivo de tempo
de recuperação (RTO) e pela perda aceitável de dados (RPO); só então escolhe região,
AZs, replicação e backup.

ARNs e tags ajudam a localizar e governar recursos, mas não substituem essas
fronteiras. Uma tag `Ambiente=producao` é metadado; não muda isolamento, permissão nem
rota por conta própria.

## Identidade antes do recurso

Toda chamada à AWS é feita por uma identidade. **IAM** decide quem pode executar qual
ação, sobre qual recurso e sob quais condições. Usuários representam pessoas ou
integrações legadas; roles representam permissões assumidas temporariamente por pessoas,
workloads e serviços. Para aplicações, prefira credenciais temporárias obtidas por uma
role a access keys permanentes copiadas para arquivos ou variáveis.

Uma política é uma lista de declarações com efeito, ações, recursos e condições. A
avaliação começa com negação implícita, soma permissões aplicáveis e respeita qualquer
negação explícita. **Mínimo privilégio** significa conceder somente as ações e os
recursos necessários, de preferência com condições verificáveis — não começar por
`AdministratorAccess` e esperar reduzir depois.

Autenticação responde “quem é”; autorização responde “o que pode fazer”. Criptografia,
rede e IAM são controles complementares. Um security group não impede uma identidade
autorizada de apagar um bucket pela API, e uma policy IAM não corrige um serviço
publicado para toda a internet.

## APIs, requisições e estado

AWS CLI, SDKs, console e ferramentas de IaC chegam aos mesmos contratos de API. O
console é um cliente visual, não um plano de controle separado. Uma requisição carrega
serviço e operação, região, identidade, parâmetros e uma assinatura derivada das
credenciais. A resposta traz dados ou um erro específico; registrar request IDs e
códigos de erro é mais útil do que repetir o comando às cegas.

```diagrama
tipo: fluxo
visual: planos-aws
titulo: Da chamada ao comportamento
passos:
  - titulo: CLI · SDK · IaC · console
    detalhe: monta a operação
  - titulo: endpoint do serviço
    detalhe: define serviço e região
    lateral:
      titulo: IAM e guardrails
      detalhe: autoriza ação, recurso e condições
      tom: destaque
  - titulo: plano de controle
    detalhe: registra configuração e estado
  - titulo: data plane
    detalhe: processa bytes, mensagens, consultas e tráfego
    tom: sucesso
legenda: Estado aceito não é comportamento provado; teste os dois planos.
```

APIs distribuídas também exigem duas disciplinas. **Idempotência** permite repetir uma
operação sem criar efeitos adicionais — por token de cliente, chave natural, escrita
condicional ou desenho do consumidor. **Consistência eventual** significa que uma
mudança aceita pode levar tempo para aparecer em todas as leituras. O tratamento correto
é polling com timeout e backoff, não um `sleep` arbitrário nem criação duplicada.

## Plano de controle e data plane

O **plano de controle** cria, configura e descreve recursos. O **data plane** executa o
trabalho: S3 lê bytes, DynamoDB grava itens, SQS entrega mensagens, um banco responde
SQL, uma task atende HTTP. Diagnóstico começa identificando em qual plano a evidência
falhou.

Um bucket pode existir e negar leitura de objeto. Uma task definition pode estar
registrada e nenhuma task ficar saudável. Um listener pode existir e encaminhar para o
target group errado. Separar os planos evita correções supersticiosas: se a configuração
não existe, investigue API, identidade e dependências; se existe mas não funciona,
investigue logs, métricas, conectividade, saúde e o comportamento da aplicação.

Essa distinção é ainda mais importante no laboratório local. O MiniStack implementa
alguns data planes, substitui outros por infraestrutura local e, em certos serviços,
mantém apenas metadados do plano de controle. “Criar uma instância EC2” local não liga
uma VM; comprova somente que o modelo estrutural foi aceito.

## Rede: intenção, rotas e filtros

Uma **VPC** é a fronteira de rede regional. Cada subnet ocupa um CIDR e uma AZ. O que
torna uma subnet pública não é seu nome nem `MapPublicIpOnLaunch`: é uma tabela de rotas
com caminho para um Internet Gateway, combinada com um recurso que tenha endereço
público. Uma subnet privada pode usar NAT Gateway para iniciar conexões de saída sem
aceitar conexões iniciadas da internet.

Security groups são firewalls stateful ligados a interfaces; network ACLs são filtros
stateless no limite da subnet. Ambos aplicam regras de tráfego, não permissões de API.
Rotas dizem por onde o pacote tenta seguir; não garantem que o destino escute nem que a
aplicação esteja saudável. Por isso uma arquitetura de rede é provada em camadas: rotas
e associações, filtros, resolução de nomes, conexão e resposta da aplicação.

## Dados e processamento assíncrono

Serviços gerenciados mudam a unidade de raciocínio. No S3, pense em objetos, versões e
políticas de ciclo de vida; no DynamoDB, comece pelos padrões de acesso e desenhe a chave
para responder consultas sem `Scan`; no SQS, receber uma mensagem não a conclui — ela
fica invisível por um período e volta se não for apagada.

Entrega “pelo menos uma vez” implica duplicatas possíveis. Consumidores precisam ser
idempotentes, e uma **dead-letter queue** limita o ciclo de mensagens que falham
repetidamente sem apagá-las como evidência. Retentativa sem limite transforma uma falha
de negócio em custo, ruído e congestionamento.

Persistência também exige decidir retenção, backup e restauração. Versionamento protege
contra sobrescrita acidental, não contra toda forma de exclusão ou acesso indevido.
Backup que nunca foi restaurado é apenas uma expectativa.

## Infraestrutura como código e drift

Infraestrutura como código (IaC) transforma a arquitetura num artefato revisável. Um
template declara recursos e relações; a ferramenta calcula dependências, envia operações
e mantém estado suficiente para comparar intenção e realidade. O ganho principal não é
“criar tudo de uma vez”, mas tornar mudanças reproduzíveis, revisáveis e auditáveis.

**Drift** aparece quando alguém altera o recurso fora do template. Corrigir somente o
recurso ao vivo faz o próximo deploy desfazer a correção; atualizar somente o template
sem entender a realidade pode destruir uma mudança válida. O fluxo seguro é detectar,
classificar, reconciliar a fonte de verdade e revisar o plano antes de aplicar.

`CREATE_COMPLETE` significa que o provedor terminou as operações declaradas. Ainda é
necessário testar invariantes de arquitetura e o data plane: versionamento está ativo?
a fila redireciona após o número certo de tentativas? a aplicação responde pelo caminho
esperado? IaC organiza a intenção; não substitui verificação.

## Artefatos, containers e serviços gerenciados

ECR armazena imagens; ECS agenda tasks; um ALB recebe e roteia tráfego; EKS oferece uma
API Kubernetes gerenciada; RDS oferece bancos relacionais gerenciados. Cada serviço
remove parte do trabalho operacional, mas os contratos entre eles continuam seus:
imagem imutável, role adequada, porta coerente, health check útil, subnets corretas,
credencial rotacionável e observabilidade.

Tags são referências humanas mutáveis; **digests** identificam o conteúdo exato de uma
imagem. Um rollback confiável aponta para o artefato testado, não para a esperança de que
`latest` ainda signifique a mesma coisa. Da mesma forma, uma task `RUNNING` prova que o
processo iniciou, não que está pronto para receber tráfego.

## Custo, quotas e observabilidade

Na nuvem, arquitetura e economia são o mesmo sistema. Capacidade ociosa, retenção sem
prazo, transferência entre regiões, logs sem filtro e retentativas descontroladas viram
cobrança. Tags de alocação, budgets e alertas ajudam a detectar tendência; não substituem
limites de arquitetura nem desligam recursos automaticamente por padrão.

Quotas também fazem parte do desenho. Uma implantação pode falhar mesmo com código
correto porque atingiu limite regional de API ou capacidade. Métricas, logs e traces
precisam responder a perguntas operacionais — taxa de erro, latência, saturação e causa
— sem registrar segredos. O MiniStack não reproduz preço, quotas, toda a telemetria nem
o comportamento de disponibilidade da AWS; essas lacunas exigem testes posteriores em
uma conta sandbox com orçamento e guardrails reais.

## O laboratório MiniStack e seus guardrails

Esta Trilha usa **MiniStack 1.4.13-full** pinado por digest. Não é uma conta AWS e não é
LocalStack. Todos os Cenários usam a conta sintética `000000000000`, a região
`us-east-1` e o endpoint `http://127.0.0.1:4566`. O `--endpoint-url` explícito em cada
comando é uma barreira visível contra atingir `amazonaws.com`; nunca o remova.

O emulador tem três níveis de fidelidade:

```diagrama
tipo: comparacao
visual: fidelidade-local
titulo: Fidelidade local
colunas:
  - titulo: Data plane local
    tom: sucesso
    itens:
      - S3, DynamoDB e SQS
      - dados e mensagens reais no processo local
  - titulo: Infraestrutura substituta
    tom: destaque
    itens:
      - SQL, containers, registry, HTTP e k3s
      - comportamento útil; operação diferente da AWS
  - titulo: Somente control plane
    tom: alerta
    itens:
      - metadados de VPC, EC2 e Route 53
      - sem VM, rede, DNS ou failover reais
legenda: A aprovação deve nomear o que foi provado e o que ainda exige sandbox.
```

Alguns Cenários montam o socket do Docker para iniciar bancos, tasks e k3s. Esse acesso
equivale a controle administrativo do Docker host: execute somente o conteúdo
versionado do projeto e não rode outro MiniStack ao mesmo tempo. Ao Iniciar, a
plataforma recria o ambiente efêmero; nada ali deve ser tratado como backup.

IAM, custo, quotas, isolamento de VPC, disponibilidade entre AZs e propagação DNS não
são provados localmente. O hábito profissional é registrar duas listas ao terminar:
“evidências obtidas” e “validações ainda necessárias na AWS”. Não promova uma conclusão
da primeira lista para a segunda por semelhança de API.

## Da teoria aos Cenários

Os Cenários seguintes transformam o modelo mental em evidência local:

- **01 — Uma conta local que não pode virar produção:** identidade sintética, região e endpoint seguro;
- **02 — A sobrescrita que não apagou a versão anterior:** objetos, versões e retenção no S3;
- **03 — A chave que evita um Scan caro:** padrões de acesso e modelagem no DynamoDB;
- **04 — Receber uma mensagem não significa processá-la:** visibility timeout, retry e DLQ no SQS;
- **05 — Um upload, uma fila e eventos duplicáveis:** pipeline assíncrono e idempotência;
- **06 — Infraestrutura declarada, não uma sequência de cliques:** CloudFormation, dependências e eventos;
- **07 — Uma VPC organizada em duas zonas de disponibilidade:** CIDRs, AZs e estrutura regional;
- **08 — Pública não é um nome de subnet:** route tables, Internet Gateway e NAT;
- **09 — Uma instância registrada não é uma máquina virtual:** EC2 e security group no plano de controle;
- **10 — O registro existe, mas seu computador não o resolve:** Route 53 e o limite do DNS local;
- **11 — RDS no plano de controle, PostgreSQL no data plane:** API gerenciada e SQL real;
- **12 — API Aurora sem fingir storage distribuído:** compatibilidade local e lacunas de alta disponibilidade;
- **13 — Uma imagem imutável do build ao registry:** ECR, tag, digest e pull verificável;
- **14 — Task definition não garante uma task saudável:** ECS, configuração e processo real;
- **15 — O listener existe, mas a rota não chega à task:** cadeia ALB, target group e ECS;
- **16 — Um EKS local que é k3s — e isso importa:** EKS, ECR, kubeconfig e fidelidade;
- **17 — O recurso mudou fora do template:** drift e reconciliação da fonte de verdade;
- **18 — O stack verde com uma arquitetura errada:** diagnóstico integrado e inventário de lacunas.

Use o Questionário como recuperação ativa: responda sem procurar no texto, veja o
feedback e volte apenas às seções indicadas. Depois, abra o terminal. Na AWS, toda
conclusão forte precisa dizer qual evidência a sustenta — e qual evidência ainda falta.

### Referências para continuar

- [Modelo de responsabilidade compartilhada](https://aws.amazon.com/compliance/shared-responsibility-model/)
- [Infraestrutura global da AWS](https://aws.amazon.com/about-aws/global-infrastructure/)
- [Práticas recomendadas de segurança no IAM](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)
- [Pilares do AWS Well-Architected Framework](https://docs.aws.amazon.com/wellarchitected/latest/framework/the-pillars-of-the-framework.html)
- [Documentação do AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html)
- [Limitações do MiniStack](https://ministack.org/docs/limitations)
