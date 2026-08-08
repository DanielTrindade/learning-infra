const http = require('node:http')

async function principal() {
  const resposta = await fetch('http://db:3000')
  const dados = await resposta.json()

  http
    .createServer((_req, res) => {
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
      res.end(JSON.stringify({ animal: dados.animal }))
    })
    .listen(3000, () => console.log('api ouvindo na porta 3000'))
}

principal().catch((erro) => {
  console.error('banco não respondeu na primeira tentativa:', erro.message)
  process.exit(1)
})
