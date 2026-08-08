const http = require('node:http')

const alvo = 'http://api-errado:3000'

http
  .createServer(async (_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
    try {
      const resultado = await fetch(alvo)
      const dados = await resultado.json()
      resposta.end(`<!doctype html><title>web</title><h1>O api disse: ${dados.animal}</h1>`)
    } catch (erro) {
      resposta.end(
        `<!doctype html><title>web</title><h1>nao alcancei ${alvo}</h1><pre>${erro.message}</pre>`,
      )
    }
  })
  .listen(3000, () => console.log('web ouvindo na porta 3000'))
