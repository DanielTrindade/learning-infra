# O assunto deste Cenário é o state, não o recurso: uma fila já aplicável
# basta para existir algo a migrar.
resource "aws_sqs_queue" "pedidos" {
  name = "mirante-pedidos"
}
