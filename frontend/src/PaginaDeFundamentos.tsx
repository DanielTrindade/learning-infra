import { useEffect, useState } from 'react'
import {
  buscarFundamentos,
  listarTrilhas,
  type FundamentosDetalhados,
  type ResultadoDoQuestionario,
} from './api'
import { ConteudoMarkdown } from './ConteudoMarkdown'
import { Questionario } from './Questionario'
import { salvarTrilhaAtual } from './progresso'
import { SumarioDaAula } from './SumarioDaAula'
import { rolarAteSecao, secoesDoMarkdown } from './secoesDaAula'

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
        <a href="#/aprender">Voltar às Trilhas</a>
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

  const secoes = secoesDoMarkdown(fundamentos.markdown)
  const estadoVisual = fundamentos.estado === 'CONCLUIDO'
    ? 'concluido'
    : fundamentos.estado === 'EM_ANDAMENTO'
      ? 'andamento'
      : 'nao-iniciado'

  return (
    <div className="pagina-aula pagina-fundamentos">
      <a className="voltar" href="#/aprender"><span aria-hidden="true">←</span> Todas as Trilhas</a>

      <header className="cabecalho-aula cabecalho-fundamentos">
        <p className="terminal-label">
          <span>Fundamentos</span>
          <i aria-hidden="true">/</i>
          <span>{idDaTrilha}</span>
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
              <button className="botao botao-primario" type="button" onClick={() => rolarAteSecao('inicio-questionario')}>
                Ir ao Questionário <span aria-hidden="true">↓</span>
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
            <ConteudoMarkdown markdown={fundamentos.markdown} />
          </article>

          <section className="bloco-questionario" id="inicio-questionario" aria-labelledby="titulo-questionario">
            <header className="cabecalho-questionario">
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
              aoRevisar={rolarAteSecao}
            />
          </section>
        </div>
      </div>
    </div>
  )
}
