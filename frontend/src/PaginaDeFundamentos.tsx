import { useEffect, useState, type ReactNode } from 'react'
import Markdown from 'react-markdown'
import {
  buscarFundamentos,
  listarTrilhas,
  type FundamentosDetalhados,
  type ResultadoDoQuestionario,
} from './api'
import { BlocoDeCodigo } from './BlocoDeCodigo'
import { Questionario } from './Questionario'
import { salvarTrilhaAtual } from './progresso'

function slugificar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('pt-BR')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '')
}

function textoDoNo(no: ReactNode): string {
  if (typeof no === 'string' || typeof no === 'number') return String(no)
  if (Array.isArray(no)) return no.map(textoDoNo).join('')
  return ''
}

function secoesDoArtigo(markdown: string) {
  return markdown
    .split('\n')
    .filter((linha) => /^##\s+/.test(linha))
    .map((linha) => linha.replace(/^##\s+/, '').trim())
    .map((titulo) => ({ titulo, id: slugificar(titulo) }))
}

const rotulosDeEstado = {
  NAO_INICIADO: 'Não iniciado',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDO: 'Concluído',
} as const

export function PaginaDeFundamentos({ idDaTrilha }: { idDaTrilha: string }) {
  const [fundamentos, setFundamentos] = useState<FundamentosDetalhados | null>(null)
  const [primeiroCenario, setPrimeiroCenario] = useState<string | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    let ativo = true
    setFundamentos(null)
    setErro(null)
    Promise.all([buscarFundamentos(idDaTrilha), listarTrilhas()])
      .then(([dados, trilhas]) => {
        if (!ativo) return
        setFundamentos(dados)
        setPrimeiroCenario(
          trilhas.find((trilha) => trilha.id === idDaTrilha)?.cenarios[0]?.id ?? null,
        )
        salvarTrilhaAtual(idDaTrilha)
      })
      .catch((e) => {
        if (ativo) setErro(String(e))
      })
    return () => {
      ativo = false
    }
  }, [idDaTrilha])

  function rolarAte(id: string) {
    document.getElementById(id)?.scrollIntoView({
      behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
    })
  }

  function atualizarResultado(resultado: ResultadoDoQuestionario) {
    setFundamentos((atual) => atual ? {
      ...atual,
      estado: resultado.aprovado ? 'CONCLUIDO' : 'EM_ANDAMENTO',
      melhorPercentual: resultado.melhorPercentual,
      tentativas: resultado.tentativas,
    } : atual)
  }

  if (erro) {
    return (
      <div className="estado-pagina estado-erro" role="alert">
        <span className="estado-icone" aria-hidden="true">!</span>
        <strong>Não foi possível abrir os Fundamentos.</strong>
        <span>Volte ao catálogo ou confira se o backend está rodando.</span>
        <code>{erro}</code>
        <a href="#/">Voltar às Trilhas</a>
      </div>
    )
  }

  if (!fundamentos) {
    return (
      <div className="estado-pagina" role="status">
        <span className="spinner" aria-hidden="true" />
        <strong>Preparando os Fundamentos…</strong>
      </div>
    )
  }

  const secoes = secoesDoArtigo(fundamentos.markdown)
  const estadoVisual = fundamentos.estado === 'CONCLUIDO'
    ? 'concluido'
    : fundamentos.estado === 'EM_ANDAMENTO'
      ? 'andamento'
      : 'nao-iniciado'

  return (
    <div className="pagina-aula pagina-fundamentos">
      <a className="voltar" href="#/"><span aria-hidden="true">←</span> Todas as Trilhas</a>

      <header className="cabecalho-aula cabecalho-fundamentos">
        <p className="terminal-label">
          <span>{idDaTrilha}</span>
          <i aria-hidden="true">/</i>
          <span>modelo mental → checagem → terminal</span>
        </p>
        <h1>{fundamentos.titulo}</h1>
        <p className="introducao-fundamentos">
          Entenda as decisões por trás da ferramenta, confirme o modelo mental e leve-o
          para os Cenários práticos — sem bloqueio rígido.
        </p>
        <div className="metadados-aula">
          <span className={`status status-${estadoVisual}`}><i aria-hidden="true" />{rotulosDeEstado[fundamentos.estado]}</span>
          <span>Aproveitamento recomendado: {fundamentos.aproveitamentoMinimo}%</span>
          <span>{fundamentos.tentativas} {fundamentos.tentativas === 1 ? 'tentativa' : 'tentativas'}</span>
          {fundamentos.tentativas > 0 && <span>Melhor resultado: {fundamentos.melhorPercentual}%</span>}
        </div>
      </header>

      <div className="layout-aula">
        <aside className="lateral-aula" aria-label="Navegação dos Fundamentos">
          <section className="painel-execucao painel-modelo-mental">
            <span className="status status-nao-iniciado"><i aria-hidden="true" /> Etapa conceitual</span>
            <h2>Leia, recupere, pratique</h2>
            <p>O Questionário orienta revisões. Você pode abrir os Cenários a qualquer momento.</p>
            <div className="fluxo-aprendizado" aria-label="Fluxo da Trilha">
              <strong>modelo mental</strong><span>→</span><strong>checagem</strong><span>→</span><strong>terminal</strong>
            </div>
            <div className="acoes">
              <button className="botao botao-primario" type="button" onClick={() => rolarAte('inicio-questionario')}>
                Ir ao Questionário <span aria-hidden="true">↓</span>
              </button>
              {primeiroCenario && (
                <a className="botao botao-secundario" href={`#/cenarios/${primeiroCenario}`}>
                  Ir direto à prática
                </a>
              )}
            </div>
          </section>

          <nav className="sumario" aria-label="Neste artigo">
            <p>Neste artigo</p>
            <ol>
              {secoes.map((secao, indice) => (
                <li key={secao.id}>
                  <button type="button" onClick={() => rolarAte(secao.id)}>
                    <span>{String(indice + 1).padStart(2, '0')}</span>
                    {secao.titulo}
                  </button>
                </li>
              ))}
            </ol>
          </nav>
        </aside>

        <div className="coluna-fundamentos">
          <article className="conteudo-aula conteudo-fundamentos">
            <Markdown
              components={{
                h1: () => null,
                h2: ({ children }) => <h2 id={slugificar(textoDoNo(children))}>{children}</h2>,
                pre: ({ children }) => <BlocoDeCodigo>{children}</BlocoDeCodigo>,
              }}
            >
              {fundamentos.markdown}
            </Markdown>
          </article>

          <section className="bloco-questionario" id="inicio-questionario" aria-labelledby="titulo-questionario">
            <header className="cabecalho-questionario">
              <p className="terminal-label"><span aria-hidden="true">?</span> checagem formativa</p>
              <h2 id="titulo-questionario">Teste seu modelo mental</h2>
              <p>
                Responda as {fundamentos.questoes.length} situações sem consultar o texto.
                O feedback indicará exatamente o que revisar.
              </p>
            </header>
            <Questionario
              idDaTrilha={idDaTrilha}
              questoes={fundamentos.questoes}
              primeiroCenario={primeiroCenario}
              aoResultado={atualizarResultado}
              aoRevisar={rolarAte}
            />
          </section>
        </div>
      </div>
    </div>
  )
}
