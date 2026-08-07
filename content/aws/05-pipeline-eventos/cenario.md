---
id: aws/05-pipeline-eventos
titulo: Um upload, uma fila e eventos duplicáveis
dificuldade: assistido
ministack: true
---
# Um upload, uma fila e eventos duplicáveis

Construa um fluxo S3 → SQS. Cada objeto criado em `learning-infra-entrada` deve publicar
uma mensagem em `learning-infra-eventos`. O objetivo não é só ligar APIs: é perceber
que notificação não é transação distribuída e que o consumidor precisa ser idempotente.

## Passos

1. Crie a fila e capture seu ARN.
2. Crie o bucket.
3. Configure `put-bucket-notification-configuration` para `s3:ObjectCreated:*`.
4. Envie `evento.json` para `entradas/evento-001.json`.
5. Inspecione a mensagem sem apagá-la antes de Verificar.

```powershell
$endpoint = "http://127.0.0.1:4566"
aws --endpoint-url $endpoint sqs create-queue --queue-name learning-infra-eventos
aws --endpoint-url $endpoint s3api create-bucket --bucket learning-infra-entrada
aws --endpoint-url $endpoint s3api put-bucket-notification-configuration `
  --bucket learning-infra-entrada `
  --notification-configuration file://notificacao.json
aws --endpoint-url $endpoint s3api put-object `
  --bucket learning-infra-entrada --key entradas/evento-001.json `
  --body evento.json --content-type application/json --checksum-algorithm SHA256
```

O arquivo `notificacao.json` é um esqueleto; substitua o ARN da fila. Em produção, uma
bucket policy/SQS policy também participa da autorização. O MiniStack é mais permissivo,
portanto sucesso local não prova a policy real.

## Idempotência

Use `eventName`, bucket, key e sequencer para imaginar uma chave de deduplicação. SQS
Standard e notificações S3 podem duplicar; o MiniStack inclusive pode entregar mais de
uma mensagem para este único upload. “Recebi uma vez no notebook” não altera esse
contrato.
