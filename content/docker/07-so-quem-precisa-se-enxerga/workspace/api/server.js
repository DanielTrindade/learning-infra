const http = require('node:http')

async function buscar() {
  const resposta = await fetch('http://db:3000')
  return resposta.json()
}

http
  .createServer(async (_req, resposta) => {
    const dados = await buscar()
    resposta.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
    resposta.end(JSON.stringify({ animal: dados.animal }))
  })
  .listen(3000, () => console.log('api ouvindo na porta 3000'))
