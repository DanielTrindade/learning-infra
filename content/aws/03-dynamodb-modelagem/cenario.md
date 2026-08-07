---
id: aws/03-dynamodb-modelagem
titulo: A chave que evita um Scan caro
dificuldade: assistido
ministack: true
---
# A chave que evita um Scan caro

O acesso dominante é: “listar os pedidos de um cliente em ordem de data”. Crie uma
tabela `learning-infra-pedidos` cuja partition key seja `clienteId` e cuja sort key seja
`pedidoEm`. Não use `Scan` para resolver uma consulta que já é conhecida no desenho.

## Objetivo

1. Crie a tabela com chaves `S` e aguarde `ACTIVE`.
2. Grave dois pedidos do cliente `cli-42`.
3. O pedido `2026-08-07T10:00:00Z` deve ter `status=PAGO`.
4. Consulte com `Query` e `KeyConditionExpression`, em ordem decrescente.
5. Tente repetir o primeiro `PutItem` com uma condition que impeça sobrescrita.

Use sempre:

```powershell
$endpoint = "http://127.0.0.1:4566"
aws --endpoint-url $endpoint dynamodb create-table ...
aws --endpoint-url $endpoint dynamodb put-item ...
aws --endpoint-url $endpoint dynamodb query ...
```

Os arquivos de itens no workspace evitam uma guerra de escaping com JSON no
PowerShell. Passe-os com `--item file://pedido-01.json` e `pedido-02.json`.

## Diagnóstico

Compare `Query` e `Scan` pela intenção, não pelo tempo no notebook. O MiniStack não
reproduz partições físicas, capacidade provisionada nem custo. A modelagem continua
correta porque reduz o conjunto lógico lido na AWS real.

