// Checagem local de conteúdo: blocos ```diagrama parseiam e `revisar` aponta para seção existente.
// Uso: node scripts-checar-conteudo.mjs <trilha>
import fs from 'node:fs'
import { parse } from 'yaml'

const trilha = process.argv[2]
const base = new URL(`../content/${trilha}/`, import.meta.url)
const md = fs.readFileSync(new URL('fundamentos.md', base), 'utf8')

let falhas = 0

const tiposValidos = new Set(['fluxo', 'camadas', 'comparacao', 'ciclo'])
const tonsValidos = new Set(['neutro', 'destaque', 'sucesso', 'alerta', 'perigo'])
const visuaisValidos = new Set([
  'fronteiras-runtime',
  'motor-docker',
  'filesystem-camadas',
  'rotas-container',
  'reconciliacao',
  'arquitetura-cluster',
  'hierarquia-workload',
  'service-endpoints',
  'responsabilidade-aws',
  'fronteiras-aws',
  'planos-aws',
  'fidelidade-local',
  'ciclo-iac',
  'triangulo-state',
  'grafo-dependencias',
])

function textoObrigatorio(valor, campo) {
  if (typeof valor !== 'string' || valor.trim() === '') {
    throw new Error(`o campo "${campo}" precisa ser texto`)
  }
}

function listaObrigatoria(valor, campo) {
  if (!Array.isArray(valor) || valor.length === 0) {
    throw new Error(`o campo "${campo}" precisa ser uma lista com pelo menos um item`)
  }
}

function validarTom(valor, campo) {
  if (valor !== undefined && !tonsValidos.has(valor)) {
    throw new Error(`o campo "${campo}" tem tom desconhecido: ${String(valor)}`)
  }
}

function validarNo(item, campo) {
  if (typeof item !== 'object' || item === null) {
    throw new Error(`o item de "${campo}" precisa ser um objeto`)
  }
  textoObrigatorio(item.titulo, `${campo}.titulo`)
  validarTom(item.tom, `${campo}.tom`)
  if (item.detalhe !== undefined) textoObrigatorio(item.detalhe, `${campo}.detalhe`)
}

function validarDiagrama(dados) {
  if (typeof dados !== 'object' || dados === null) {
    throw new Error('o bloco precisa ser um objeto YAML')
  }
  if (!tiposValidos.has(dados.tipo)) {
    throw new Error(`tipo desconhecido: ${String(dados.tipo)}`)
  }
  if (dados.visual !== undefined && !visuaisValidos.has(dados.visual)) {
    throw new Error(`visual desconhecido: ${String(dados.visual)}`)
  }

  const campo = dados.tipo === 'camadas' ? 'camadas' : dados.tipo === 'comparacao' ? 'colunas' : 'passos'
  listaObrigatoria(dados[campo], campo)
  dados[campo].forEach((item, indice) => {
    validarNo(item, `${campo}[${indice}]`)
    if (dados.tipo === 'fluxo' && item.lateral !== undefined) {
      validarNo(item.lateral, `${campo}[${indice}].lateral`)
    }
    if (dados.tipo === 'comparacao') {
      listaObrigatoria(item.itens, `${campo}[${indice}].itens`)
      item.itens.forEach((texto, indiceDoItem) =>
        textoObrigatorio(texto, `${campo}[${indice}].itens[${indiceDoItem}]`),
      )
    }
  })
}

const blocos = [...md.matchAll(/```diagrama\n([\s\S]*?)```/g)]
console.log(`blocos diagrama: ${blocos.length}`)
blocos.forEach((bloco, indice) => {
  try {
    const dados = parse(bloco[1])
    validarDiagrama(dados)
    console.log(`  ${indice + 1}. OK — ${dados.tipo} · ${dados.titulo}`)
  } catch (e) {
    falhas += 1
    console.log(`  ${indice + 1}. ERRO: ${e.message}`)
  }
})

const slug = (texto) =>
  texto
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '')

const slugs = new Set([...md.matchAll(/^## (.+)$/gm)].map((m) => slug(m[1])))
let questoes = []
try {
  const questionario = parse(fs.readFileSync(new URL('questionario.yaml', base), 'utf8'))
  if (!Array.isArray(questionario?.questoes)) {
    throw new Error('o campo "questoes" precisa ser uma lista')
  }
  questoes = questionario.questoes
  console.log(`questões: ${questoes.length}`)
} catch (e) {
  falhas += 1
  console.log(`questionário inválido: ${e instanceof Error ? e.message.split('\n')[0] : String(e)}`)
}

for (const questao of questoes) {
  if (!slugs.has(questao.revisar)) {
    falhas += 1
    console.log(`  REVISAR QUEBRADO: ${questao.id} -> ${questao.revisar}`)
  }
  const ids = questao.alternativas.map((a) => a.id)
  if (!ids.includes(questao.alternativaCorreta)) {
    falhas += 1
    console.log(`  CORRETA DESCONHECIDA: ${questao.id} -> ${questao.alternativaCorreta}`)
  }
}

console.log(falhas === 0 ? 'tudo certo' : `${falhas} falha(s)`)
process.exit(falhas === 0 ? 0 : 1)
