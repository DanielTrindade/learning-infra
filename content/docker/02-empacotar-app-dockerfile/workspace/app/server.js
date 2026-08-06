const http = require('node:http')

const porta = process.env.PORT || 3000

http
  .createServer((_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
    resposta.end('<!doctype html><title>App empacotada</title><h1>Empacotei minha app</h1>')
  })
  .listen(porta, () => console.log(`ouvindo na porta ${porta}`))
