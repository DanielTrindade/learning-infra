import { useEffect, useState } from 'react'
import {
  getFundamentals,
  listTracks,
  type FundamentalsDetail,
  type QuestionnaireResult,
} from './api'
import { ConteudoMarkdown } from './ConteudoMarkdown'
import { Questionario } from './Questionario'
import { saveCurrentTrack } from './progresso'
import { SumarioDaAula } from './SumarioDaAula'
import { rolarAteSecao, secoesDoMarkdown } from './secoesDaAula'

const stateLabels = {
  NOT_STARTED: 'Não iniciado',
  IN_PROGRESS: 'Em andamento',
  COMPLETED: 'Concluído',
} as const

export function PaginaDeFundamentos({ trackId }: { trackId: string }) {
  const [fundamentals, setFundamentos] = useState<FundamentalsDetail | null>(null)
  const [primeiroCenario, setPrimeiroCenario] = useState<string | null>(null)
  const [error, setErro] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    setFundamentos(null)
    setErro(null)
    Promise.all([getFundamentals(trackId), listTracks()])
      .then(([dados, tracks]) => {
        if (!active) return
        setFundamentos(dados)
        setPrimeiroCenario(
          tracks.find((track) => track.id === trackId)?.scenarios[0]?.id ?? null,
        )
        saveCurrentTrack(trackId)
      })
      .catch((e) => {
        if (active) setErro(String(e))
      })
    return () => {
      active = false
    }
  }, [trackId])

  function atualizarResultado(result: QuestionnaireResult) {
    setFundamentos((atual) => atual ? {
      ...atual,
      state: result.passed ? 'COMPLETED' : 'IN_PROGRESS',
      bestScore: result.bestScore,
      attempts: result.attempts,
    } : atual)
  }

  if (error) {
    return (
      <div className="estado-pagina estado-erro" role="alert">
        <span className="estado-icone" aria-hidden="true">!</span>
        <strong>Não foi possível abrir os Fundamentos.</strong>
        <span>Volte ao catálogo ou confira se o backend está rodando.</span>
        <code>{error}</code>
        <a href="#/aprender">Voltar às Trilhas</a>
      </div>
    )
  }

  if (!fundamentals) {
    return (
      <div className="estado-pagina" role="status">
        <span className="spinner" aria-hidden="true" />
        <strong>Preparando os Fundamentos…</strong>
      </div>
    )
  }

  const secoes = secoesDoMarkdown(fundamentals.markdown)
  const visualState = fundamentals.state === 'COMPLETED'
    ? 'completed'
    : fundamentals.state === 'IN_PROGRESS'
      ? 'in-progress'
      : 'not-started'

  return (
    <div className="pagina-aula pagina-fundamentos">
      <a className="voltar" href="#/aprender"><span aria-hidden="true">‹</span> Todas as Trilhas</a>

      <header className="cabecalho-aula cabecalho-fundamentos">
        <p className="terminal-label">
          <span>Fundamentos</span>
          <i aria-hidden="true">/</i>
          <span>{trackId}</span>
        </p>
        <h1>{fundamentals.title}</h1>
        <p className="introducao-fundamentos">
          Entenda as decisões por trás da ferramenta, confirme o modelo mental e leve-o
          para os Cenários práticos, sem bloqueio rígido.
        </p>
        <div className="metadados-aula">
          <span className={`status status-${visualState}`}><i aria-hidden="true" />{stateLabels[fundamentals.state]}</span>
          <span>Aproveitamento recomendado: {fundamentals.minimumScore}%</span>
          <span>{fundamentals.attempts} {fundamentals.attempts === 1 ? 'tentativa' : 'tentativas'}</span>
          {fundamentals.attempts > 0 && <span>Melhor result: {fundamentals.bestScore}%</span>}
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
              <button className="botao botao-primario" type="button" onClick={() => rolarAteSecao('inicio-questionario')}>
                Ir ao Questionário
              </button>
              {primeiroCenario && (
                <a className="botao botao-secundario" href={`#/cenarios/${primeiroCenario}`}>
                  Ir direto à prática
                </a>
              )}
            </div>
          </section>

          <SumarioDaAula secoes={secoes} rotulo="Neste artigo" />
        </aside>

        <div className="coluna-fundamentos">
          <article className="conteudo-aula conteudo-fundamentos">
            <ConteudoMarkdown markdown={fundamentals.markdown} />
          </article>

          <section className="bloco-questionario" id="inicio-questionario" aria-labelledby="titulo-questionario">
            <header className="cabecalho-questionario">
              <h2 id="titulo-questionario">Teste seu modelo mental</h2>
              <p>
                Responda as {fundamentals.questions.length} situações sem consultar o text.
                O feedback indicará exatamente o que review.
              </p>
            </header>
            <Questionario
              trackId={trackId}
              questions={fundamentals.questions}
              primeiroCenario={primeiroCenario}
              aoResultado={atualizarResultado}
              aoRevisar={rolarAteSecao}
            />
          </section>
        </div>
      </div>
    </div>
  )
}
