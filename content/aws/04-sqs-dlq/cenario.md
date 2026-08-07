---
id: aws/04-sqs-dlq
titulo: Receber uma mensagem não significa processá-la
dificuldade: assistido
ministack: true
---
# Receber uma mensagem não significa processá-la

SQS entrega pelo menos uma vez. Depois de `ReceiveMessage`, a mensagem fica invisível;
ela só desaparece quando o consumidor confirma com `DeleteMessage`. Falhar entre essas
etapas produz reentrega.

## Objetivo

Crie:

- `learning-infra-processamento-dlq`;
- `learning-infra-processamento`, com visibility timeout de 30 segundos;
- redrive para a DLQ após três recebimentos.

Capture a URL e o ARN em variáveis, pois a API trabalha com ambos:

```powershell
$endpoint = "http://127.0.0.1:4566"
$dlqUrl = aws --endpoint-url $endpoint sqs create-queue `
  --queue-name learning-infra-processamento-dlq --query QueueUrl --output text
$dlqArn = aws --endpoint-url $endpoint sqs get-queue-attributes `
  --queue-url $dlqUrl --attribute-names QueueArn --query Attributes.QueueArn --output text
$filaUrl = aws --endpoint-url $endpoint sqs create-queue `
  --queue-name learning-infra-processamento `
  --attributes VisibilityTimeout=30 --query QueueUrl --output text
```

Monte `RedrivePolicy` com JSON válido e use `set-queue-attributes`. Envie uma mensagem,
receba sem apagar e observe o retorno depois do visibility timeout.

No PowerShell, evite escapar JSON aninhado manualmente. Construa o mapa e deixe o
serializador cuidar das aspas:

```powershell
$redrive = @{deadLetterTargetArn=$dlqArn; maxReceiveCount="3"} |
  ConvertTo-Json -Compress
$atributos = @{RedrivePolicy=$redrive} | ConvertTo-Json -Compress
[IO.File]::WriteAllText(
  (Join-Path $PWD "atributos-fila.json"),
  $atributos,
  [Text.UTF8Encoding]::new($false)
)
aws --endpoint-url $endpoint sqs set-queue-attributes `
  --queue-url $filaUrl --attributes file://atributos-fila.json
```

O encoder sem BOM é deliberado: o Windows PowerShell clássico pode prefixar o arquivo
com bytes que fazem a AWS CLI rejeitar o JSON antes mesmo de chamar a API.

## Limite do laboratório

O comportamento de fila, invisibilidade e redrive é exercitável. Não use o tempo exato
do scheduler local como benchmark da AWS, e trate métricas aproximadas como diagnóstico,
não como contador transacional.
