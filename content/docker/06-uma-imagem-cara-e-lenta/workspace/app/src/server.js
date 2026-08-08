const http = require('node:http')

http
  .createServer((_req, res) => {
    res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8' })
    res.end(titulo())
  })
  .listen(3000, () => console.log('app ouvindo na porta 3000'))
