const http = require('node:http')
const fs = require('node:fs')

http
  .createServer((_req, resposta) => {
    fs.writeFileSync('/tmp/ping.txt', new Date().toISOString())
    resposta.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8' })
    resposta.end('Cenário 09 seguro')
  })
  .listen(3000, () => console.log('app ouvindo na porta 3000'))
