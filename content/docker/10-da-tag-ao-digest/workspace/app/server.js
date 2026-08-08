const http = require('node:http')

const versao = process.env.APP_VERSAO || 'indefinida'

http
  .createServer((_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
    resposta.end(JSON.stringify({ versao }))
  })
  .listen(3000, () => console.log('app v' + versao + ' ouvindo na porta 3000'))
