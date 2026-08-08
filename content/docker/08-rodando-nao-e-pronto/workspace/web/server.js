const http = require('node:http')

http
  .createServer(async (_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
    try {
      const resultado = await fetch('http://api:3000')
      const dados = await resultado.json()
      resposta.end(`<!doctype html><title>web</title><h1>O api disse: ${dados.animal}</h1>`)
    } catch (erro) {
      resposta.end(
        `<!doctype html><title>web</title><h1>nao alcancei http://api:3000</h1><pre>${erro.message}</pre>`,
      )
    }
  })
  .listen(3000, () => console.log('web ouvindo na porta 3000'))
