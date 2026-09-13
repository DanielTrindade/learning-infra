import { useEffect, useState, type CSSProperties, type ReactNode } from 'react'
import {
  listTracks,
  type ScenarioSummary,
  type FundamentalsSummary,
  type TrackSummary,
} from './api'
import { glifoDaTrilha } from './identidadeDaTrilha'
import { ConfirmacaoDeReset } from './ConfirmacaoDeReset'
import {
  loadCurrentTrack,
  scenarioState,
  clearCurrentTrack,
  scenarioNumber,
  difficultyLabels,
  stateLabels,
  saveCurrentTrack,
  type ScenarioState,
} from './progresso'

type Filtro = 'todos' | ScenarioState

type TrackSummaryView = {
  id: string
  name: string
  aulas: ScenarioSummary[]
  fundamentals: FundamentalsSummary | null
  completedCount: number
  inProgress: number
  notStarted: number
  total: number
  score: number
  allCompleted: boolean
  hasProgress: boolean
}

const CHAVE_TRILHAS_ABERTAS = 'learning-infra:trilhas-abertas:v2'
const CHAVE_TRILHAS_RESTAURADAS = 'learning-infra:trilhas-restauradas:v1'

const filtros: { id: Filtro; rotulo: string }[] = [
  { id: 'todos', rotulo: 'Todos' },
  { id: 'in-progress', rotulo: 'Em andamento' },
  { id: 'completed', rotulo: 'Concluídos' },
  { id: 'not-started', rotulo: 'Não iniciados' },
]

function carregarTrilhasAbertas(): Set<string> | null {
  try {
    const valor = localStorage.getItem(CHAVE_TRILHAS_ABERTAS)
    if (valor === null) return null

    const tracks = JSON.parse(valor)
    return Array.isArray(tracks) && tracks.every((track) => typeof track === 'string')
      ? new Set(tracks)
      : null
  } catch {
    return null
  }
}

function salvarTrilhasAbertas(tracks: Set<string>) {
  try {
    localStorage.setItem(CHAVE_TRILHAS_ABERTAS, JSON.stringify([...tracks]))
  } catch {
    // A navegação continua funcionando quando o browser bloqueia armazenamento local.
  }
}

function carregarTrilhasRestauradas(): Set<string> | null {
  try {
    const valor = localStorage.getItem(CHAVE_TRILHAS_RESTAURADAS)
    if (valor === null) return null

    const tracks = JSON.parse(valor)
    return Array.isArray(tracks) && tracks.every((track) => typeof track === 'string')
      ? new Set(tracks)
      : null
  } catch {
    return null
  }
}

function salvarTrilhasRestauradas(tracks: Set<string>) {
  try {
    localStorage.setItem(CHAVE_TRILHAS_RESTAURADAS, JSON.stringify([...tracks]))
  } catch {
    // A navegação continua funcionando quando o browser bloqueia armazenamento local.
  }
}

function fundamentalsState(fundamentals: FundamentalsSummary): ScenarioState {
  if (fundamentals.state === 'COMPLETED') return 'completed'
  if (fundamentals.state === 'IN_PROGRESS') return 'in-progress'
  return 'not-started'
}

function summarizeTrack(track: TrackSummary): TrackSummaryView {
  const emAndamentoNosCenarios = track.scenarios
    .filter((scenario) => scenarioState(scenario) === 'in-progress').length
  const fundamentosEmAndamento = track.fundamentals?.state === 'IN_PROGRESS' ? 1 : 0
  const inProgress = emAndamentoNosCenarios + fundamentosEmAndamento
  const notStarted = track.total - track.completed - inProgress

  return {
    id: track.id,
    name: track.title,
    aulas: track.scenarios,
    fundamentals: track.fundamentals,
    completedCount: track.completed,
    inProgress,
    notStarted,
    total: track.total,
    score: track.score,
    allCompleted: track.allCompleted,
    hasProgress: track.completed > 0 || inProgress > 0,
  }
}

function MarcadorDeEstado({ state }: { state: ScenarioState }) {
  return (
    <span className={`marcador-estado marcador-${state}`} aria-hidden="true">
      {state === 'completed' ? '✓' : state === 'in-progress' ? '●' : ''}
    </span>
  )
}

function EstadoDeCarregamento() {
  return (
    <div className="estado-pagina" role="status">
      <span className="spinner" aria-hidden="true" />
      <strong>Carregando seus Cenários…</strong>
      <span>Lendo o catálogo e o progresso salvo nesta máquina.</span>
    </div>
  )
}

function BarraDeProgressoLinear({
  score,
  inProgress,
  total,
  rotulo,
  grande,
}: {
  score: number
  inProgress: number
  total: number
  rotulo: string
  grande?: boolean
}) {
  const percentualComAndamento = Math.min(100, score + (inProgress / Math.max(total, 1)) * 100)
  return (
    <div className={grande ? 'barra-linear barra-linear-grande' : 'barra-linear'} role="img" aria-label={rotulo}>
      <span
        className="barra-linear-andamento"
        style={{ '--fator': percentualComAndamento / 100 } as CSSProperties}
      />
      <span
        className="barra-linear-concluido"
        style={{ '--fator': score / 100 } as CSSProperties}
      />
    </div>
  )
}

function ResumoVisualDaTrilha({ resumo }: { resumo: TrackSummaryView }) {
  return (
    <div className="trilha-progresso">
      <div className="trilha-progresso-topo">
        <strong>{resumo.score}%</strong>
        <span>{resumo.completedCount} de {resumo.total} concluídos</span>
      </div>
      <BarraDeProgressoLinear
        score={resumo.score}
        inProgress={resumo.inProgress}
        total={resumo.total}
        rotulo={`${resumo.score}% da trilha concluída`}
      />
      <div className="trilha-estados" aria-hidden="true">
        <span className="trilha-estado-concluido"><b>{resumo.completedCount}</b> concluídos</span>
        <span className="trilha-estado-andamento"><b>{resumo.inProgress}</b> em andamento</span>
        <span className="trilha-estado-nao-iniciado"><b>{resumo.notStarted}</b> a iniciar</span>
      </div>
    </div>
  )
}

function ListaDeCenarios({
  aulas,
  fundamentals,
  aberta,
  id,
  trackId,
  podeIniciar,
}: {
  aulas: ScenarioSummary[]
  fundamentals: FundamentalsSummary | null
  aberta: boolean
  id: string
  trackId: string
  podeIniciar: boolean
}) {
  return (
    <div className={aberta ? 'cenarios-envelope' : 'cenarios-envelope cenarios-envelope-fechado'}>
      <ol className="cenarios" id={id}>
      {fundamentals && (
        <li className={`cenario fundamentos-item cenario-${fundamentalsState(fundamentals)}`}>
          <span className="linha-progresso" aria-hidden="true" />
          <MarcadorDeEstado state={fundamentalsState(fundamentals)} />
          <a href={`#/trilhas/${trackId}/fundamentos`}>
            <span className="cenario-numero">Fundamentos</span>
            <span className="cenario-corpo">
              <strong>{fundamentals.title}</strong>
              <span className="cenario-meta">
                <span className={`estado-texto estado-${fundamentalsState(fundamentals)}`}>
                  {stateLabels[fundamentalsState(fundamentals)]}
                </span>
                <span>artigo + Questionário</span>
                {fundamentals.attempts > 0 && (
                  <span>melhor result {fundamentals.bestScore}%</span>
                )}
              </span>
            </span>
            <span className="cenario-seta" aria-hidden="true">›</span>
          </a>
        </li>
      )}
      {aulas.map((scenario) => {
        const state = scenarioState(scenario)
        const conteudo = (
          <>
            <span className="cenario-numero">Cenário {scenarioNumber(scenario.id)}</span>
            <span className="cenario-corpo">
              <strong>{scenario.title}</strong>
              <span className="cenario-meta">
                <span className={`estado-texto estado-${state}`}>{stateLabels[state]}</span>
                <span>{difficultyLabels[scenario.difficulty]}</span>
                <span>
                  {scenario.assertionCount} {scenario.assertionCount === 1 ? 'verificação' : 'verificações'}
                </span>
                {!podeIniciar && <span>adira para iniciar</span>}
              </span>
            </span>
            <span className="cenario-seta" aria-hidden="true">{podeIniciar ? '›' : ''}</span>
          </>
        )

        return (
          <li key={scenario.id} className={`cenario cenario-${state}`}>
            <span className="linha-progresso" aria-hidden="true" />
            <MarcadorDeEstado state={state} />
            {podeIniciar
              ? <a href={`#/cenarios/${scenario.id}`}>{conteudo}</a>
              : <div className="cenario-preview">{conteudo}</div>}
          </li>
        )
      })}
      </ol>
    </div>
  )
}

function CartaoDeTrilha({
  resumo,
  aulas,
  fundamentals,
  aberta,
  ehAtual,
  podeIniciar,
  legenda,
  acoes,
  onAlternar,
  onReset,
}: {
  resumo: TrackSummaryView
  aulas: ScenarioSummary[]
  fundamentals: FundamentalsSummary | null
  aberta: boolean
  ehAtual: boolean
  podeIniciar: boolean
  legenda: string
  acoes: ReactNode
  onAlternar: () => void
  onReset: () => void
}) {
  const idDosCenarios = `cenarios-trilha-${resumo.id}`
  const idDoTitulo = `titulo-trilha-${resumo.id}`

  return (
    <section
      className={`trilha ${aberta ? '' : 'trilha-colapsada'} ${ehAtual ? 'trilha-atual' : ''}`}
      aria-labelledby={idDoTitulo}
    >
      <header
        className="trilha-cabecalho"
        onClick={(evento) => {
          if ((evento.target as HTMLElement).closest('button, a')) return
          onAlternar()
        }}
      >
        <div className="trilha-identidade">
          <span className="trilha-icone" aria-hidden="true">{glifoDaTrilha(resumo.id)}</span>
          <div>
            <h3 className="trilha-titulo" id={idDoTitulo}>{resumo.name}</h3>
            <span className="trilha-legenda">{legenda}</span>
          </div>
        </div>

        <ResumoVisualDaTrilha resumo={resumo} />

        <div className="trilha-acoes">
          {acoes}
          <button
            type="button"
            className="trilha-recolher"
            aria-expanded={aberta}
            aria-controls={idDosCenarios}
            aria-label={`${aberta ? 'Recolher' : 'Mostrar'} aulas da trilha ${resumo.name}`}
            title={aberta ? 'Recolher aulas' : 'Mostrar aulas'}
            onClick={onAlternar}
          >
            <svg
              className="trilha-chevron"
              viewBox="0 0 16 16"
              width="16"
              height="16"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <path d="M4 6.25l4 4 4-4" />
            </svg>
          </button>
        </div>
      </header>

      <ListaDeCenarios
        aulas={aulas}
        fundamentals={fundamentals}
        aberta={aberta}
        id={idDosCenarios}
        trackId={resumo.id}
        podeIniciar={podeIniciar}
      />
      {resumo.hasProgress && (
        <footer className="trilha-rodape">
          <button type="button" className="botao trilha-reset" onClick={onReset}
            aria-label={`Resetar progresso de ${resumo.name}`}>
            Resetar progresso
          </button>
        </footer>
      )}
    </section>
  )
}

export function Catalogo() {
  const [trilhaParaReset, setTrilhaParaReset] = useState<TrackSummaryView | null>(null)
  const [aviso, setAviso] = useState('')
  const [tracks, setTrilhas] = useState<TrackSummary[] | null>(null)
  const [error, setErro] = useState<string | null>(null)
  const [filtro, setFiltro] = useState<Filtro>('todos')
  const [trilhaAtual, setTrilhaAtual] = useState<string | null>(loadCurrentTrack)
  const [trilhasAbertas, setTrilhasAbertas] = useState<Set<string> | null>(carregarTrilhasAbertas)
  const [trilhasRestauradas, setTrilhasRestauradas] = useState<Set<string> | null>(
    carregarTrilhasRestauradas,
  )
  const [trilhasRecolhidasNoFiltro, setTrilhasRecolhidasNoFiltro] = useState<Set<string>>(
    () => new Set(),
  )

  useEffect(() => {
    listTracks().then(setTrilhas).catch((e) => setErro(String(e)))
  }, [])

  if (error) {
    return (
      <div className="estado-pagina estado-erro" role="alert">
        <span className="estado-icone" aria-hidden="true">!</span>
        <strong>Não foi possível carregar o catálogo.</strong>
        <span>Confira se o backend está rodando na porta 8099 e atualize a página.</span>
        <code>{error}</code>
      </div>
    )
  }

  if (!tracks) return <EstadoDeCarregamento />

  if (tracks.length === 0) {
    return (
      <div className="estado-pagina">
        <span className="estado-icone" aria-hidden="true">＋</span>
        <strong>Seu catálogo ainda está vazio.</strong>
        <span>Crie uma Trilha com <code>trilha.yaml</code> para começar.</span>
      </div>
    )
  }

  const resumos = tracks.map(summarizeTrack)
  const trilhasRestauradasEfetivas = trilhasRestauradas ?? new Set<string>()
  const trilhasConcluidas = resumos.filter(
    (resumo) => resumo.allCompleted && !trilhasRestauradasEfetivas.has(resumo.id),
  )
  const trilhasDisponiveis = resumos.filter(
    (resumo) => !resumo.allCompleted || trilhasRestauradasEfetivas.has(resumo.id),
  )
  const trilhaDoAmbienteAtivo = trilhasDisponiveis.find((resumo) =>
    resumo.aulas.some((scenario) => scenario.active),
  )
  const currentTrackView = trilhasDisponiveis.find((resumo) => resumo.id === trilhaAtual)
    ?? trilhaDoAmbienteAtivo
    ?? null
  const currentTrackId = currentTrackView?.id ?? null
  const trilhasAbertasEfetivas = trilhasAbertas
    ?? new Set(currentTrackId ? [currentTrackId] : [])

  const aulasDisponiveis = trilhasDisponiveis.flatMap((resumo) => resumo.aulas)
  const cenariosVisiveis = aulasDisponiveis.filter(
    (scenario) => filtro === 'todos' || scenarioState(scenario) === filtro,
  )
  const contagens: Record<Filtro, number> = {
    todos: aulasDisponiveis.length,
    completed: aulasDisponiveis.filter((scenario) => scenarioState(scenario) === 'completed').length,
    'in-progress': aulasDisponiveis.filter((scenario) => scenarioState(scenario) === 'in-progress').length,
    'not-started': aulasDisponiveis.filter((scenario) => scenarioState(scenario) === 'not-started').length,
  }

  const focoDaTrilhaAtual = currentTrackView?.aulas.find(
    (scenario) => scenarioState(scenario) === 'in-progress',
  ) ?? currentTrackView?.aulas.find(
    (scenario) => scenarioState(scenario) === 'not-started',
  ) ?? currentTrackView?.aulas[0]
  const fundamentosPendentes = currentTrackView?.fundamentals
    && currentTrackView.fundamentals.state !== 'COMPLETED'
  const hrefDoFoco = fundamentosPendentes
    ? `#/trilhas/${currentTrackView?.id}/fundamentos`
    : focoDaTrilhaAtual
      ? `#/cenarios/${focoDaTrilhaAtual.id}`
      : null

  function alterarTrilhasAbertas(transformar: (atuais: Set<string>) => Set<string>) {
    setTrilhasAbertas((atuais) => {
      const base = atuais ?? new Set(currentTrackId ? [currentTrackId] : [])
      const proximas = transformar(new Set(base))
      salvarTrilhasAbertas(proximas)
      return proximas
    })
  }

  function aderirATrilha(track: string) {
    setTrilhaAtual(track)
    saveCurrentTrack(track)
    setFiltro('todos')
    setTrilhasRecolhidasNoFiltro(new Set())
    alterarTrilhasAbertas(() => new Set([track]))
  }

  function restaurarTrilha(track: string) {
    setTrilhasRestauradas((atuais) => {
      const proximas = new Set(atuais ?? [])
      proximas.add(track)
      salvarTrilhasRestauradas(proximas)
      return proximas
    })
    aderirATrilha(track)
    window.requestAnimationFrame(() => rolarAte(`titulo-trilha-${track}`))
  }

  function arquivarTrilha(track: string) {
    setTrilhasRestauradas((atuais) => {
      const proximas = new Set(atuais ?? [])
      proximas.delete(track)
      salvarTrilhasRestauradas(proximas)
      return proximas
    })
    if (trilhaAtual === track) {
      setTrilhaAtual(null)
      clearCurrentTrack()
    }
  }

  function alternarTrilha(track: string) {
    if (filtro !== 'todos') {
      setTrilhasRecolhidasNoFiltro((atuais) => {
        const proximas = new Set(atuais)
        if (proximas.has(track)) proximas.delete(track)
        else proximas.add(track)
        return proximas
      })
      return
    }

    alterarTrilhasAbertas((abertas) => {
      if (abertas.has(track)) abertas.delete(track)
      else abertas.add(track)
      return abertas
    })
  }

  function selecionarFiltro(proximoFiltro: Filtro) {
    setFiltro(proximoFiltro)
    setTrilhasRecolhidasNoFiltro(new Set())
  }

  function alternarAberturaArquivada(track: string) {
    alterarTrilhasAbertas((abertas) => {
      if (abertas.has(track)) abertas.delete(track)
      else abertas.add(track)
      return abertas
    })
  }

  function rolarAte(id: string) {
    document.getElementById(id)?.scrollIntoView({
      behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
    })
  }

  return (
    <>
      {trilhaParaReset && (
        <ConfirmacaoDeReset
          trilha={trilhaParaReset}
          onCancelar={() => setTrilhaParaReset(null)}
          onReset={(atualizada) => {
            setTrilhas((atuais) => atuais?.map((track) => track.id === atualizada.id ? atualizada : track) ?? null)
            setTrilhaParaReset(null)
            setTrilhasRestauradas((atuais) => {
              const proximas = new Set(atuais ?? [])
              proximas.delete(atualizada.id)
              salvarTrilhasRestauradas(proximas)
              return proximas
            })
            setFiltro('todos')
            setTrilhasRecolhidasNoFiltro(new Set())
            alterarTrilhasAbertas((abertas) => abertas.add(atualizada.id))
            setAviso(`Progresso de ${atualizada.title} resetado.`)
            window.requestAnimationFrame(() => document.getElementById('aviso-reset')?.focus())
          }}
        />
      )}
      <div id="aviso-reset" className={aviso ? 'reset-sucesso' : undefined} role="status" tabIndex={-1}>{aviso}</div>
      <header className="area-aprendizado-cabecalho">
        <div>
          <h1>Minha aprendizagem</h1>
          <p>Continue a trilha atual ou escolha um novo tema para estudar no seu ritmo.</p>
        </div>
        <span>{resumos.length} {resumos.length === 1 ? 'trilha disponível' : 'trilhas disponíveis'}</span>
      </header>

      {currentTrackView ? (
        <section className="painel-progresso painel-progresso-atual" aria-labelledby="titulo-progresso">
          <div className="progresso-cabecalho">
            <div>
              <p className="eyebrow">Em foco</p>
              <h2 id="titulo-progresso">{currentTrackView.name}</h2>
              <p className="progresso-proximo-passo">
                {fundamentosPendentes
                  ? 'Comece pelos Fundamentos para preparar o modelo mental da trilha.'
                  : focoDaTrilhaAtual && scenarioState(focoDaTrilhaAtual) === 'in-progress'
                    ? 'Retome o Cenário que já está em andamento.'
                    : 'Seu próximo Cenário está pronto para começar.'}
              </p>
            </div>
            <div className="progresso-acoes">
              <p className="progresso-resumo">
                <strong>{currentTrackView.score}%</strong>
                <span>{currentTrackView.completedCount} de {currentTrackView.total} concluídos</span>
              </p>
              {hrefDoFoco && (
                <a className="botao botao-primario" href={hrefDoFoco}>
                  {fundamentosPendentes ? 'Abrir Fundamentos' : 'Continuar Cenário'}
                </a>
              )}
              <button type="button" onClick={() => rolarAte('titulo-trilhas')}>Trocar trilha</button>
            </div>
          </div>

          <BarraDeProgressoLinear
            grande
            score={currentTrackView.score}
            inProgress={currentTrackView.inProgress}
            total={currentTrackView.total}
            rotulo={`${currentTrackView.score}% da trilha ${currentTrackView.name} concluída`}
          />

          <dl className="resumo-estados">
            <div className="resumo-concluido">
              <dt>Concluídos</dt>
              <dd>{currentTrackView.completedCount}</dd>
            </div>
            <div className="resumo-andamento">
              <dt>Em andamento</dt>
              <dd>{currentTrackView.inProgress}</dd>
            </div>
            <div className="resumo-nao-iniciado">
              <dt>Não iniciados</dt>
              <dd>{currentTrackView.notStarted}</dd>
            </div>
          </dl>
        </section>
      ) : (
        <section className="painel-progresso painel-sem-trilha" aria-labelledby="titulo-progresso">
          <div>
            <h2 id="titulo-progresso">Escolha sua primeira trilha</h2>
            <p>Depois da escolha, o próximo conteúdo e o progresso ficam em destaque aqui.</p>
          </div>
          {trilhasDisponiveis.length > 0 && (
            <button type="button" className="botao botao-secundario" onClick={() => rolarAte('titulo-trilhas')}>
              Explorar trilhas
            </button>
          )}
        </section>
      )}

      {trilhasDisponiveis.length > 0 && (
        <section className="catalogo-lista" aria-labelledby="titulo-trilhas">
          <div className="lista-cabecalho">
            <div>
              <h2 id="titulo-trilhas">Explorar trilhas</h2>
              <p className="lista-descricao">Mantenha uma trilha atual por vez. Trocar não apaga o que você já concluiu.</p>
            </div>
            <div className="filtros" aria-label="Filtrar Cenários">
              {filtros.map((item) => (
                <button
                  type="button"
                  key={item.id}
                  className={filtro === item.id ? 'filtro-ativo' : undefined}
                  aria-pressed={filtro === item.id}
                  onClick={() => selecionarFiltro(item.id)}
                >
                  {item.rotulo}
                  <span>{contagens[item.id]}</span>
                </button>
              ))}
            </div>
          </div>

          {cenariosVisiveis.length === 0 ? (
            <div className="filtro-vazio">
              <strong>Nenhuma aula neste estado.</strong>
              <button type="button" onClick={() => selecionarFiltro('todos')}>Mostrar todas</button>
            </div>
          ) : (
            trilhasDisponiveis.map((resumo) => {
              const aulas = resumo.aulas.filter((scenario) =>
                cenariosVisiveis.some((visivel) => visivel.id === scenario.id),
              )
              if (aulas.length === 0) return null

              const ehAtual = resumo.id === currentTrackId
              const aberta = filtro === 'todos'
                ? trilhasAbertasEfetivas.has(resumo.id)
                : !trilhasRecolhidasNoFiltro.has(resumo.id)
              const restaurada = resumo.allCompleted && trilhasRestauradasEfetivas.has(resumo.id)

              return (
                <CartaoDeTrilha
                  key={resumo.id}
                  resumo={resumo}
                  aulas={aulas}
                  fundamentals={filtro === 'todos' ? resumo.fundamentals : null}
                  aberta={aberta}
                  ehAtual={ehAtual}
                  podeIniciar={ehAtual}
                  legenda={
                    filtro === 'todos'
                      ? `${resumo.aulas.length} ${resumo.aulas.length === 1 ? 'Cenário' : 'Cenários'}`
                      : `${aulas.length} ${aulas.length === 1 ? 'resultado' : 'resultados'} neste filtro`
                  }
                  onAlternar={() => alternarTrilha(resumo.id)}
                  onReset={() => { setAviso(''); setTrilhaParaReset(resumo) }}
                  acoes={
                    <>
                      {ehAtual ? (
                        <span className="selo-trilha-atual"><i aria-hidden="true" /> Trilha atual</span>
                      ) : (
                        <button
                          type="button"
                          className="botao trilha-aderir"
                          onClick={() => aderirATrilha(resumo.id)}
                        >
                          {resumo.hasProgress ? 'Retomar trilha' : 'Aderir à trilha'}
                        </button>
                      )}
                      {restaurada && (
                        <button
                          type="button"
                          className="botao trilha-arquivar"
                          onClick={() => arquivarTrilha(resumo.id)}
                        >
                          Arquivar
                        </button>
                      )}
                    </>
                  }
                />
              )
            })
          )}
        </section>
      )}

      {trilhasConcluidas.length > 0 && (
        <section className="arquivo-trilhas" aria-labelledby="titulo-concluidas">
          <div className="arquivo-cabecalho">
            <div>
              <h2 id="titulo-concluidas">Trilhas concluídas</h2>
              <p className="arquivo-descricao">Restaurar traz a Trilha de volta à lista para refazer ou revisitar.</p>
            </div>
            <span>{trilhasConcluidas.length} {trilhasConcluidas.length === 1 ? 'trilha' : 'trilhas'}</span>
          </div>
          <div className="arquivo-lista">
            {trilhasConcluidas.map((resumo) => (
              <CartaoDeTrilha
                key={resumo.id}
                resumo={resumo}
                aulas={resumo.aulas}
                fundamentals={resumo.fundamentals}
                aberta={trilhasAbertasEfetivas.has(resumo.id)}
                ehAtual={false}
                podeIniciar
                legenda={`${resumo.total} ${resumo.total === 1 ? 'etapa' : 'etapas'} concluídas`}
                onAlternar={() => alternarAberturaArquivada(resumo.id)}
                onReset={() => { setAviso(''); setTrilhaParaReset(resumo) }}
                acoes={
                  <button
                    type="button"
                    className="botao trilha-restaurar"
                    onClick={() => restaurarTrilha(resumo.id)}
                  >
                    Restaurar track
                  </button>
                }
              />
            ))}
          </div>
        </section>
      )}
    </>
  )
}
