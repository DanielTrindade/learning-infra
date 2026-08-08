const fs = require('node:fs')

const vendor = fs.readFileSync('vendor/lib.js', 'utf8')
const src = fs.readFileSync('src/server.js', 'utf8')
fs.mkdirSync('dist', { recursive: true })
fs.writeFileSync('dist/server.js', vendor + '\n' + src)
