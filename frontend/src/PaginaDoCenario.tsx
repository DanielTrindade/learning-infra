import { useEffect, useRef, useState } from 'react'
import {
  getScenario,
  startScenario,
  verifyScenario,
  type ScenarioDetail,
  type VerificationResult,
} from './api'
import { IconeCheck, IconeCopiar } from './BlocoDeCodigo'
import { ChecklistDeVerificacao } from './ChecklistDeVerificacao'
import { ConteudoMarkdown } from './ConteudoMarkdown'
import {
  scenarioState,
  trackId,
  trackName,
  scenarioNumber,
  difficultyLabels,
  stateLabels,
  saveCurrentTrack,
} from './progresso'
import { SumarioDaAula } from './SumarioDaAula'
import { secoesDoMarkdown } from './secoesDaAula'

type LocalDaAcao = 'lateral' | 'final'

export function PaginaDoCenario({ id }: { id: string }) {
  const [scenario, setCenario] = useState<ScenarioDetail | null>(null)
  const [error, setErro] = useState<string | null>(null)
  const [diretorio, setDiretorio] = useState<string | null>(null)
  const [result, setResultado] = useState<VerificationResult | null>(null)
  const [ocupado, setOcupado] = useState<'iniciando' | 'verificando' | null>(null)
  const [diretorioCopiado, setDiretorioCopiado] = useState(false)
  const [localDaAcao, setLocalDaAcao] = useState<LocalDaAcao>('lateral')
  const requisicaoEmCurso = useRef(false)

  useEffect(() => {
    let active = true
    setCenario(null)
    setErro(null)
    setDiretorio(null)
    setResultado(null)

    getScenario(id)
      .then((dados) => {
        if (active) setCenario(dados)
      })
      .catch((e) => {
        if (active) setErro(String(e))
      })

    return () => {
      active = false
    }
  }, [id])

  async function aoIniciar(local: LocalDaAcao) {
    if (requisicaoEmCurso.current) return
    requisicaoEmCurso.current = true
    setLocalDaAcao(local)
    setOcupado('iniciando')
    setErro(null)
    setResultado(null)
    try {
      const { workingDirectory } = await startScenario(id)
      saveCurrentTrack(trackId(id))
      setDiretorio(workingDirectory)
      setCenario((atual) => atual ? { ...atual, active: true } : atual)
    } catch (e) {
      setErro(String(e))
    } finally {
      requisicaoEmCurso.current = false
      setOcupado(null)
    }
  }

  async function aoVerificar(local: LocalDaAcao) {
    if (requisicaoEmCurso.current) return
    requisicaoEmCurso.current = true
    setLocalDaAcao(local)
    setOcupado('verificando')
    setErro(null)
    setResultado(null)
    try {
      const newResult = await verifyScenario(id)
      setResultado(newResult)
      if (newResult.completed) {
        setCenario((atual) => atual ? { ...atual, completed: true } : atual)
      }
    } catch (e) {
      setErro(String(e))
    } finally {
      requisicaoEmCurso.current = false
      setOcupado(null)
    }
  }

  async function copiarDiretorio() {
    if (!diretorio) return
    await navigator.clipboard.writeText(diretorio)
    setDiretorioCopiado(true)
    window.setTimeout(() => setDiretorioCopiado(false), 1800)
  }

  if (error && !scenario) {
    return (
      <div className="estado-pagina estado-erro" role="alert">
        <span className="estado-icone" aria-hidden="true">!</span>
        <strong>Não foi possível abrir esta aula.</strong>
        <span>Volte ao catálogo ou confira se o backend está rodando.</span>
        <code>{error}</code>
        <a href="#/aprender">Voltar aos Cenários</a>
      </div>
    )
  }

  if (!scenario) {
    return (
      <div className="estado-pagina" role="status">
        <span className="spinner" aria-hidden="true" />
        <strong>Preparando o Cenário…</strong>
      </div>
    )
  }

  const state = scenarioState(scenario)
  const secoes = secoesDoMarkdown(scenario.markdown)
  const podeVerificar = Boolean(diretorio || scenario.active)
  const statusText = state === 'completed'
    ? 'Todas as verificações já passaram. Você pode revisar ou refazer este laboratório.'
    : state === 'in-progress'
      ? 'O ambiente está ativo. Siga a aula no seu terminal e verifique quando quiser.'
      : 'Inicie o ambiente para preparar o diretório de trabalho desta aula.'

  return (
    <div className="pagina-aula">
      <a className="voltar" href="#/aprender"><span aria-hidden="true">‹</span> Todos os Cenários</a>

      <header className="cabecalho-aula">
        <p className="terminal-label">
          <span>Cenário {scenarioNumber(scenario.id)}</span>
          <i aria-hidden="true">/</i>
          <span>{trackName(scenario.id)}</span>
        </p>
        <h1>{scenario.title}</h1>
        <div className="metadados-aula">
          <span className={`status status-${state}`}><i aria-hidden="true" />{stateLabels[state]}</span>
          <span>{difficultyLabels[scenario.difficulty]}</span>
          <span>{scenario.assertions.length} {scenario.assertions.length === 1 ? 'verificação' : 'verificações'}</span>
        </div>
      </header>

      <div className="layout-aula">
        <aside className="lateral-aula" aria-label="Controles e navegação do Cenário">
          <section className="painel-execucao">
            <div className="painel-status">
              <span className={`status status-${state}`}><i aria-hidden="true" />{stateLabels[state]}</span>
              <h2>{state === 'completed' ? 'Laboratório aprovado' : state === 'in-progress' ? 'Ambiente preparado' : 'Pronto para começar?'}</h2>
              <p>{statusText}</p>
            </div>

            <div className="acoes">
              <button className="botao botao-primario" onClick={() => aoIniciar('lateral')} disabled={ocupado !== null}>
                {ocupado === 'iniciando' && <span className="spinner spinner-botao" aria-hidden="true" />}
                {ocupado === 'iniciando'
                  ? 'Preparando…'
                  : scenario.active
                    ? 'Reiniciar ambiente'
                    : scenario.completed
                      ? 'Refazer laboratório'
                      : 'Iniciar ambiente'}
              </button>
              <button className="botao botao-secundario" onClick={() => aoVerificar('lateral')} disabled={ocupado !== null || !podeVerificar}>
                {ocupado === 'verificando' && <span className="spinner spinner-botao" aria-hidden="true" />}
                {ocupado === 'verificando' ? 'Verificando…' : 'Verificar exercício'}
              </button>
            </div>

            {!podeVerificar && (
              <p className="acao-ajuda">A verificação será liberada depois que o ambiente iniciar.</p>
            )}

            {scenario.active && !diretorio && (
              <p className="acao-ajuda">Este ambiente já estava ativo. Reinicie apenas se precisar recriar <code>work/</code>.</p>
            )}

            {diretorio && (
              <div className="diretorio">
                <span>Diretório de trabalho</span>
                <code>{diretorio}</code>
                <button
                  type="button"
                  className={diretorioCopiado ? 'copiar-caminho copiado' : 'copiar-caminho'}
                  onClick={copiarDiretorio}
                >
                  <span className="copiar-caminho-icones" aria-hidden="true">
                    <IconeCopiar />
                    <IconeCheck />
                  </span>
                  {diretorioCopiado ? 'Copiado' : 'Copiar caminho'}
                </button>
              </div>
            )}

            {error && localDaAcao === 'lateral' && <p className="erro" role="alert">{error}</p>}
          </section>

          {result && localDaAcao === 'lateral' && <ChecklistDeVerificacao result={result} />}

          <SumarioDaAula secoes={secoes} rotulo="Neste Cenário" />
        </aside>

        <div className="coluna-cenario">
          <article className="conteudo-aula">
            <div className="nota-de-uso">
              <span aria-hidden="true">↗</span>
              <p><strong>Os comandos rodam no seu terminal.</strong> Esta página explica o exercício e verifica o resultado na sua máquina.</p>
            </div>

            <ConteudoMarkdown markdown={scenario.markdown} />
          </article>

          <section className="verificacao-final" aria-labelledby="titulo-verificacao-final">
            <h2 id="titulo-verificacao-final">Verifique seu exercício</h2>
            <p>
              {podeVerificar
                ? 'Terminou os passos no terminal? Confira o resultado e veja se falta algum ajuste.'
                : 'Inicie o ambiente, execute os passos no terminal e volte aqui para verificar o resultado.'}
            </p>
            <div className="verificacao-final-acoes" aria-busy={ocupado !== null}>
              {podeVerificar ? (
                <button type="button" className="botao botao-primario"
                  onClick={() => aoVerificar('final')} disabled={ocupado !== null}>
                  {ocupado === 'verificando' ? 'Verificando…' : 'Verificar exercício'}
                </button>
              ) : (
                <button type="button" className="botao botao-primario"
                  onClick={() => aoIniciar('final')} disabled={ocupado !== null}>
                  {ocupado === 'iniciando' ? 'Preparando…' : 'Iniciar ambiente'}
                </button>
              )}
              <span role="status">
                {ocupado === 'verificando'
                  ? 'Conferindo as verificações do cenário…'
                  : ocupado === 'iniciando'
                    ? 'Preparando seu diretório de trabalho…'
                    : 'Você pode verificar novamente após cada ajuste.'}
              </span>
            </div>
            {diretorio && localDaAcao === 'final' && (
              <div className="diretorio">
                <span>Ambiente preparado. Execute os comandos neste diretório:</span>
                <code>{diretorio}</code>
                <button type="button" className="copiar-caminho" onClick={copiarDiretorio}>
                  {diretorioCopiado ? 'Copiado' : 'Copiar caminho'}
                </button>
              </div>
            )}
            {error && localDaAcao === 'final' && <p className="erro" role="alert">{error}</p>}
          </section>
          {result && localDaAcao === 'final' && <ChecklistDeVerificacao result={result} />}
        </div>
      </div>
    </div>
  )
}
