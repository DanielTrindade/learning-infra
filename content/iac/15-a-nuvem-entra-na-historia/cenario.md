---
id: iac/15-a-nuvem-entra-na-historia
titulo: A nuvem entra na história
dificuldade: assistido
terraform: true
ministack: true
---
# A nuvem entra na história

A Mirante cresceu, e o que cabia num container num host só não cabe mais. As notas
fiscais dos clientes precisam de um lugar durável, os pedidos das trinta lojas precisam
de uma fila para não se perderem entre sistemas, e o estoque precisa responder por
chave — o SKU de cada produto. Na Trilha AWS o leitor criou esses três recursos pela
CLI, um comando de cada vez, numa conta local que se descartava a cada Cenário. Aqui a
mesma trinca nasce de um arquivo versionado: bucket, fila e tabela declarados, planejados
e aplicados — e a Trilha IaC ganha o terceiro substrato, a nuvem local do MiniStack.

O diretório de trabalho chega com o `versions.tf` pronto e o `main.tf` vazio. Este
Cenário é `Assistido`: o objetivo, as pistas e o formato dos comandos vêm abaixo; parte
do caminho você descobre sozinho — e a documentação de cada recurso é parte do caminho.

## O provider que aponta para o seu computador

Abra o `versions.tf` e leia o bloco `provider "aws"` com calma. Ele é o tradutor novo
deste Ato — o mesmo HCL dos doze Cenários anteriores, agora falando com a API da AWS
através do MiniStack, o emulador que a Trilha AWS já usou. Como todo tradutor, ele tem
configuração, e cada linha existe por um motivo.

`region = "us-east-1"` é a região do emulador, a mesma de toda a Trilha AWS. As
credenciais `access_key` e `secret_key` são **sintéticas**: valem `local` e `local`, não
existem em conta nenhuma, e o MiniStack não as valida. Elas estão aqui porque o provider
precisa de um par para assinar as requisições — e porque a fronteira de segurança não
pode depender do esquecimento de ninguém.

Os três `skip_*` desligam validações que batem em serviços de identidade que o emulador
não implementa. O `skip_credentials_validation` pula a checagem das credenciais contra o
STS; o `skip_metadata_api_check` pula a consulta ao serviço de metadados de instância
EC2; o `skip_requesting_account_id` pula a chamada que descobriria o número da conta.
Numa conta real, esses três são proteção; aqui, são ruído — e o bloco fica explícito
para ninguém tentar "melhorar" removendo-os.

O bloco `endpoints` é a parte que merece mais atenção. Cada serviço da AWS tem o próprio
host e o próprio domínio; o emulador não espelha `amazonaws.com`, ele **é** um processo
no `127.0.0.1:4566`. Por isso o endpoint é declarado por serviço — `s3`, `sqs`,
`dynamodb` — e é por isso que só os três serviços usados aparecem aqui. Adicionar um
serviço novo é um ato deliberado: escrever o endpoint dele neste bloco. A omissão
acidental fica impossível por construção.

E este é o ponto que define a fronteira: **esse bloco é a fronteira de segurança da
Trilha**. Um `.tf` com `provider "aws"` sem `endpoints` para o `127.0.0.1:4566` e sem os
três `skip_*` é um defeito de conteúdo — significaria que um `apply` poderia alcançar a
AWS de verdade. O repositório tem um teste que reprova qualquer `.tf` assim antes de o
Cenário chegar ao leitor. A segurança não mora no cuidado de quem digita; mora no que
está versionado.

## Três recursos

O objetivo deste Cenário é declarar os três recursos que a Mirante pediu, na forma que
a Trilha IaC ensina:

- um bucket S3 **`mirante-notas`**, com versionamento — o histórico das notas fiscais;
- uma fila SQS **`mirante-pedidos`** — os pedidos das lojas esperando processo;
- uma tabela DynamoDB **`mirante-estoque`**, com chave de partição **`sku`** — consultar
  o estoque por produto sem varrer a tabela inteira.

Os tipos dos recursos que você vai declarar são `aws_s3_bucket`,
`aws_s3_bucket_versioning`, `aws_sqs_queue` e `aws_dynamodb_table`. Onde os argumentos
ficam é o exercício — e, como dica de degrau `Assistido`, aqui vão os pontos que um
primeiro plano costuma errar.

O bucket precisa ter exatamente o nome `mirante-notas` — é o nome que a Mirante e a
Verificação vão procurar. A fila, `mirante-pedidos`. A tabela, `mirante-estoque`, com
`sku` na chave. E uma tabela DynamoDB **não aceita só um nome**: o provider exige que
você declare o atributo da chave e aponte para ele como chave de partição — a mesma
`sku`, duas vezes, num `attribute` e no campo que escolhe a chave. Sem isso o plano nem
sai. O versionamento do bucket tem uma história própria, na seção seguinte.

Deixe os nomes locais dos recursos razoáveis — `notas`, `pedidos`, `estoque` — porque o
state guarda os endereços, e a Verificação, no fim, lê um endereço específico.

## Um recurso ou dois?

Quem abre o console da AWS vê o bucket e um interruptor de versionamento na mesma tela.
É natural declarar o versionamento como argumento do bucket — e é exatamente onde o
provider moderno diverge. **Versionamento é um recurso separado**: `aws_s3_bucket` cria
o bucket; `aws_s3_bucket_versioning` liga o versionamento nele, referenciando o bucket
que o outro recurso criou.

Por que essa divisão? Porque a modelagem do provider não é um espelho da tela do
console: cada recurso agrupa o que nasce, muda e morre junto, e o versionamento tem um
ciclo de vida próprio em relação ao bucket. A lição maior vale para o trabalho inteiro:
**a documentação do recurso é parte do trabalho**. O nome do tipo no provider é a
unidade que você planeja, aplica e remove — e só a documentação diz quais argumentos
cada recurso aceita e de quais depende. O texto deste Cenário entrega os tipos; a
pergunta "o que exatamente cada um pede" é resolvida na documentação, como seria na
frente de uma conta real.

## Confira pelos dois caminhos

Aplique o que escreveu:

```powershell
terraform init
terraform apply
```

Depois confira pela intenção declarada — o que o código diz que existe:

```powershell
terraform state list
```

E pela realidade do emulador — o que o MiniStack responde quando perguntado de verdade:

```powershell
$env:AWS_ACCESS_KEY_ID = "000000000000"
$env:AWS_SECRET_ACCESS_KEY = "test"
$endpoint = "http://127.0.0.1:4566"

aws --endpoint-url $endpoint s3api get-bucket-versioning --bucket mirante-notas
aws --endpoint-url $endpoint sqs get-queue-url --queue-name mirante-pedidos
aws --endpoint-url $endpoint dynamodb describe-table --table-name mirante-estoque
```

O mesmo recurso, duas visões: o `state list` responde o que o código governa; a AWS CLI
responde o que o emulador guarda. Uma confirma a outra — e, quando discordam, o `plan`
mede a distância. Confira também o endereço que a Verificação vai ler no final:

```powershell
terraform state show aws_dynamodb_table.estoque
```

## O que este Cenário não prova

O que este Cenário prova, e o que ele não prova, importa tanto quanto o próprio código.
Pela matriz de fidelidade do MiniStack, S3, SQS e DynamoDB têm **data plane real**: os
bytes de um objeto existem de verdade, uma mensagem enfileirada pode ser recebida de
verdade, um item gravado pode ser consultado de verdade. Gravar e ler funciona — e é
por isso que as Asserções deste Cenário têm valor.

O que nenhuma Asserção aqui prova é a nuvem por trás do emulador. Nada aqui prova **IAM
aplicado**: o MiniStack aceita as credenciais sintéticas sem verificar identidade
alguma — a política de quem pode o quê não é exercitada. Nada prova **isolamento de
rede**: tudo roda num processo só, no loopback da sua máquina, sem VPC, sem security
group, sem tráfego real a filtrar. Nada prova **custo**, porque nada é cobrado. Nada
prova **disponibilidade**: um processo local não tem zonas de disponibilidade nem
failover. Aplicar aqui é aprender a declarar e a conferir; é não aprender o que uma
conta de verdade faria com a declaração. O Cenário 17 chega num caso em que a fidelidade
cai bem mais — uma rede declarada cujas rotas não encaminham pacote nenhum.

## Verificação

Cinco Asserções conferem o desfecho. As três primeiras perguntam ao MiniStack pelo
mundo real: o bucket `mirante-notas` com versionamento `Enabled`, a fila
`mirante-pedidos` respondendo à busca de URL, e a tabela `mirante-estoque` com `sku` na
posição zero da chave. A quarta lê o state e confere que o endereço
`aws_dynamodb_table.estoque` está lá, com `name` igual a `mirante-estoque` — é por isso
que a sua tabela, no código, precisa se chamar `estoque`. A quinta roda um `plan` e só
aprova quando os três recursos convergiram.

A prova de que as Asserções medem origem, e não existência, é rápida e vale o Cenário.
Recrie os três recursos **pela AWS CLI** — `s3api create-bucket` e
`put-bucket-versioning`, `sqs create-queue`, `dynamodb create-table` — sem encostar no
Terraform, e verifique. As três consultas passam: o emulador tem bucket, fila e tabela,
e não sabe nem se importa de onde vieram. As duas de Terraform reprovam: o state não tem
o endereço `aws_dynamodb_table.estoque`, e o `plan` propõe criar o que já existe. O
recurso no ar é real; o recurso **sob gestão do código** é outra história. Para voltar
ao verde, `terraform import` adota o que a mão criou — tabela, bucket e fila — e um
`terraform apply` reconcilia o resto.
