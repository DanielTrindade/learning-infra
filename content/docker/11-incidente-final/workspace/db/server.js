const http = require('node:http')
const fs = require('node:fs')

const ARQUIVO = '/dados/animal.txt'
const ANIMAL = process.env.ANIMAL || 'tatu-bola'

try {
  fs.writeFileSync(ARQUIVO, ANIMAL)
} catch (erro) {
  console.error('não consegui gravar em /dados:', erro.message)
}

http
  .createServer((_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
    resposta.end(JSON.stringify({ animal: ANIMAL }))
  })
  .listen(3000, () => console.log('db ouvindo na porta 3000'))
