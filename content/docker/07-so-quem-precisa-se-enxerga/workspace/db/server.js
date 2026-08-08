const http = require('node:http')

http
  .createServer((_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
    resposta.end(JSON.stringify({ animal: 'pinguim-magalhaes' }))
  })
  .listen(3000, () => console.log('db ouvindo na porta 3000'))
