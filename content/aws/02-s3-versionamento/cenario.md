---
id: aws/02-s3-versionamento
titulo: A sobrescrita que não apagou a versão anterior
dificuldade: guiado
ministack: true
---
# A sobrescrita que não apagou a versão anterior

S3 não é um filesystem remoto. Objetos são identificados por bucket e key; diretórios
são apenas prefixos. Neste exercício você habilita versionamento antes de substituir um
objeto importante.

## Construa a evidência

Os arquivos `perfil-v1.json` e `perfil-v2.json` estão no seu diretório de trabalho.

```powershell
$endpoint = "http://127.0.0.1:4566"
aws --endpoint-url $endpoint s3api create-bucket --bucket learning-infra-arquivos
aws --endpoint-url $endpoint s3api put-bucket-versioning `
  --bucket learning-infra-arquivos `
  --versioning-configuration Status=Enabled

aws --endpoint-url $endpoint s3api put-object `
  --bucket learning-infra-arquivos --key clientes/perfil.json --body perfil-v1.json `
  --checksum-algorithm SHA256
aws --endpoint-url $endpoint s3api put-object `
  --bucket learning-infra-arquivos --key clientes/perfil.json --body perfil-v2.json `
  --checksum-algorithm SHA256
```

Observe `VersionId`, `IsLatest` e ETag:

```powershell
aws --endpoint-url $endpoint s3api list-object-versions `
  --bucket learning-infra-arquivos --prefix clientes/perfil.json
```

ETag não deve ser tratado como checksum universal: multipart e criptografia mudam sua
semântica. O exercício verifica versões, não interpreta ETag como contrato.

O `--checksum-algorithm SHA256` também é intencional: versões recentes da AWS CLI podem
escolher CRC64NVME, que esta imagem do MiniStack não inclui. Essa diferença é do
emulador, não uma recomendação para desabilitar integridade na AWS.

## Limite do emulador

Os bytes e versões são reais no MiniStack. SSE-KMS, porém, não cifra os bytes localmente;
uma configuração aceita pela API não prova criptografia.
