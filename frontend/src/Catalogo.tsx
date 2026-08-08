import { useEffect, useState, type CSSProperties } from 'react'
import {
  listarTrilhas,
  type CenarioResumo,
  type FundamentosResumo,
  type TrilhaResumo,
} from './api'
import hero from './assets/hero-infra.svg'
import {
  carregarTrilhaAtual,
  estadoDoCenario,
  numeroDoCenario,
  rotulosDificuldade,
  rotulosEstado,
  salvarTrilhaAtual,
  type EstadoDoCenario,
} from './progresso'

type Filtro = 'todos' | EstadoDoCenario

type ResumoDaTrilha = {
  id: string
  nome: string
  aulas: CenarioResumo[]
  fundamentos: FundamentosResumo | null
  concluidas: number
  emAndamento: number
  naoIniciadas: number
  total: number
  percentual: number
  concluida: boolean
  temProgresso: boolean
}

const CHAVE_TRILHAS_ABERTAS = 'learning-infra:trilhas-abertas:v2'

const filtros: { id: Filtro; rotulo: string }[] = [
  { id: 'todos', rotulo: 'Todos' },
  { id: 'andamento', rotulo: 'Em andamento' },
  { id: 'concluido', rotulo: 'Concluídos' },
  { id: 'nao-iniciado', rotulo: 'Não iniciados' },
]

function carregarTrilhasAbertas(): Set<string> | null {
  try {
    const valor = localStorage.getItem(CHAVE_TRILHAS_ABERTAS)
    if (valor === null) return null

    const trilhas = JSON.parse(valor)
    return Array.isArray(trilhas) && trilhas.every((trilha) => typeof trilha === 'string')
      ? new Set(trilhas)
      : null
  } catch {
    return null
  }
}

function salvarTrilhasAbertas(trilhas: Set<string>) {
  try {
    localStorage.setItem(CHAVE_TRILHAS_ABERTAS, JSON.stringify([...trilhas]))
  } catch {
    // A navegação continua funcionando quando o browser bloqueia armazenamento local.
  }
}

function estadoDosFundamentos(fundamentos: FundamentosResumo): EstadoDoCenario {
  if (fundamentos.estado === 'CONCLUIDO') return 'concluido'
  if (fundamentos.estado === 'EM_ANDAMENTO') return 'andamento'
  return 'nao-iniciado'
}

function resumirTrilha(trilha: TrilhaResumo): ResumoDaTrilha {
  const emAndamentoNosCenarios = trilha.cenarios
    .filter((cenario) => estadoDoCenario(cenario) === 'andamento').length
  const fundamentosEmAndamento = trilha.fundamentos?.estado === 'EM_ANDAMENTO' ? 1 : 0
  const emAndamento = emAndamentoNosCenarios + fundamentosEmAndamento
  const naoIniciadas = trilha.total - trilha.concluidos - emAndamento

  return {
    id: trilha.id,
    nome: trilha.titulo,
    aulas: trilha.cenarios,
    fundamentos: trilha.fundamentos,
    concluidas: trilha.concluidos,
    emAndamento,
    naoIniciadas,
    total: trilha.total,
    percentual: trilha.percentual,
    concluida: trilha.concluida,
    temProgresso: trilha.concluidos > 0 || emAndamento > 0,
  }
}

function MarcadorDeEstado({ estado }: { estado: EstadoDoCenario }) {
  return (
    <span className={`marcador-estado marcador-${estado}`} aria-hidden="true">
      {estado === 'concluido' ? '✓' : estado === 'andamento' ? '●' : ''}
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
  percentual,
  emAndamento,
  total,
  rotulo,
  grande,
}: {
  percentual: number
  emAndamento: number
  total: number
  rotulo: string
  grande?: boolean
}) {
  const percentualComAndamento = Math.min(100, percentual + (emAndamento / Math.max(total, 1)) * 100)
  return (
    <div className={grande ? 'barra-linear barra-linear-grande' : 'barra-linear'} role="img" aria-label={rotulo}>
      <span
        className="barra-linear-andamento"
        style={{ '--fator': percentualComAndamento / 100 } as CSSProperties}
      />
      <span
        className="barra-linear-concluido"
        style={{ '--fator': percentual / 100 } as CSSProperties}
      />
    </div>
  )
}

function ResumoVisualDaTrilha({ resumo }: { resumo: ResumoDaTrilha }) {
  return (
    <div className="trilha-progresso">
      <div className="trilha-progresso-topo">
        <strong>{resumo.percentual}%</strong>
        <span>{resumo.concluidas} de {resumo.total} concluídos</span>
      </div>
      <BarraDeProgressoLinear
        percentual={resumo.percentual}
        emAndamento={resumo.emAndamento}
        total={resumo.total}
        rotulo={`${resumo.percentual}% da trilha concluída`}
      />
      <div className="trilha-estados" aria-hidden="true">
        <span className="trilha-estado-concluido"><b>{resumo.concluidas}</b> concluídos</span>
        <span className="trilha-estado-andamento"><b>{resumo.emAndamento}</b> em andamento</span>
        <span className="trilha-estado-nao-iniciado"><b>{resumo.naoIniciadas}</b> a iniciar</span>
      </div>
    </div>
  )
}

function ListaDeCenarios({
  aulas,
  fundamentos,
  aberta,
  id,
  idDaTrilha,
  podeIniciar,
}: {
  aulas: CenarioResumo[]
  fundamentos: FundamentosResumo | null
  aberta: boolean
  id: string
  idDaTrilha: string
  podeIniciar: boolean
}) {
  return (
    <div className={aberta ? 'cenarios-envelope' : 'cenarios-envelope cenarios-envelope-fechado'}>
      <ol className="cenarios" id={id}>
      {fundamentos && (
        <li className={`cenario fundamentos-item cenario-${estadoDosFundamentos(fundamentos)}`}>
          <span className="linha-progresso" aria-hidden="true" />
          <MarcadorDeEstado estado={estadoDosFundamentos(fundamentos)} />
          <a href={`#/trilhas/${idDaTrilha}/fundamentos`}>
            <span className="cenario-numero">Fundamentos</span>
            <span className="cenario-corpo">
              <strong>{fundamentos.titulo}</strong>
              <span className="cenario-meta">
                <span className={`estado-texto estado-${estadoDosFundamentos(fundamentos)}`}>
                  {rotulosEstado[estadoDosFundamentos(fundamentos)]}
                </span>
                <span aria-hidden="true">·</span>
                <span>artigo + Questionário</span>
                {fundamentos.tentativas > 0 && (
                  <><span aria-hidden="true">·</span><span>melhor resultado {fundamentos.melhorPercentual}%</span></>
                )}
              </span>
            </span>
            <span className="cenario-seta" aria-hidden="true">→</span>
          </a>
        </li>
      )}
      {aulas.map((cenario) => {
        const estado = estadoDoCenario(cenario)
        const conteudo = (
          <>
            <span className="cenario-numero">Cenário {numeroDoCenario(cenario.id)}</span>
            <span className="cenario-corpo">
              <strong>{cenario.titulo}</strong>
              <span className="cenario-meta">
                <span className={`estado-texto estado-${estado}`}>{rotulosEstado[estado]}</span>
                <span aria-hidden="true">·</span>
                <span>{rotulosDificuldade[cenario.dificuldade]}</span>
                <span aria-hidden="true">·</span>
                <span>
                  {cenario.quantidadeDeAsercoes} {cenario.quantidadeDeAsercoes === 1 ? 'verificação' : 'verificações'}
                </span>
                {!podeIniciar && (
                  <><span aria-hidden="true">·</span><span>adira para iniciar</span></>
                )}
              </span>
            </span>
            <span className="cenario-seta" aria-hidden="true">{podeIniciar ? '→' : '·'}</span>
          </>
        )

        return (
          <li key={cenario.id} className={`cenario cenario-${estado}`}>
            <span className="linha-progresso" aria-hidden="true" />
            <MarcadorDeEstado estado={estado} />
            {podeIniciar
              ? <a href={`#/cenarios/${cenario.id}`}>{conteudo}</a>
              : <div className="cenario-preview">{conteudo}</div>}
          </li>
        )
      })}
      </ol>
    </div>
  )
}

export function Catalogo() {
  const [trilhas, setTrilhas] = useState<TrilhaResumo[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [filtro, setFiltro] = useState<Filtro>('todos')
  const [trilhaAtual, setTrilhaAtual] = useState<string | null>(carregarTrilhaAtual)
  const [trilhasAbertas, setTrilhasAbertas] = useState<Set<string> | null>(carregarTrilhasAbertas)
  const [trilhasRecolhidasNoFiltro, setTrilhasRecolhidasNoFiltro] = useState<Set<string>>(
    () => new Set(),
  )

  useEffect(() => {
    listarTrilhas().then(setTrilhas).catch((e) => setErro(String(e)))
  }, [])

  if (erro) {
    return (
      <div className="estado-pagina estado-erro" role="alert">
        <span className="estado-icone" aria-hidden="true">!</span>
        <strong>Não foi possível carregar o catálogo.</strong>
        <span>Confira se o backend está rodando na porta 8099 e atualize a página.</span>
        <code>{erro}</code>
      </div>
    )
  }

  if (!trilhas) return <EstadoDeCarregamento />

  if (trilhas.length === 0) {
    return (
      <div className="estado-pagina">
        <span className="estado-icone" aria-hidden="true">＋</span>
        <strong>Seu catálogo ainda está vazio.</strong>
        <span>Crie uma Trilha com <code>trilha.yaml</code> para começar.</span>
      </div>
    )
  }

  const resumos = trilhas.map(resumirTrilha)
  const trilhasConcluidas = resumos.filter((resumo) => resumo.concluida)
  const trilhasDisponiveis = resumos.filter((resumo) => !resumo.concluida)
  const trilhaDoAmbienteAtivo = trilhasDisponiveis.find((resumo) =>
    resumo.aulas.some((cenario) => cenario.ativo),
  )
  const resumoDaTrilhaAtual = trilhasDisponiveis.find((resumo) => resumo.id === trilhaAtual)
    ?? trilhaDoAmbienteAtivo
    ?? null
  const idDaTrilhaAtual = resumoDaTrilhaAtual?.id ?? null
  const trilhasAbertasEfetivas = trilhasAbertas
    ?? new Set(idDaTrilhaAtual ? [idDaTrilhaAtual] : [])

  const aulasDisponiveis = trilhasDisponiveis.flatMap((resumo) => resumo.aulas)
  const cenariosVisiveis = aulasDisponiveis.filter(
    (cenario) => filtro === 'todos' || estadoDoCenario(cenario) === filtro,
  )
  const contagens: Record<Filtro, number> = {
    todos: aulasDisponiveis.length,
    concluido: aulasDisponiveis.filter((cenario) => estadoDoCenario(cenario) === 'concluido').length,
    andamento: aulasDisponiveis.filter((cenario) => estadoDoCenario(cenario) === 'andamento').length,
    'nao-iniciado': aulasDisponiveis.filter((cenario) => estadoDoCenario(cenario) === 'nao-iniciado').length,
  }

  const focoDaTrilhaAtual = resumoDaTrilhaAtual?.aulas.find(
    (cenario) => estadoDoCenario(cenario) === 'andamento',
  ) ?? resumoDaTrilhaAtual?.aulas.find(
    (cenario) => estadoDoCenario(cenario) === 'nao-iniciado',
  ) ?? resumoDaTrilhaAtual?.aulas[0]
  const fundamentosPendentes = resumoDaTrilhaAtual?.fundamentos
    && resumoDaTrilhaAtual.fundamentos.estado !== 'CONCLUIDO'
  const hrefDoFoco = fundamentosPendentes
    ? `#/trilhas/${resumoDaTrilhaAtual?.id}/fundamentos`
    : focoDaTrilhaAtual
      ? `#/cenarios/${focoDaTrilhaAtual.id}`
      : null

  function alterarTrilhasAbertas(transformar: (atuais: Set<string>) => Set<string>) {
    setTrilhasAbertas((atuais) => {
      const base = atuais ?? new Set(idDaTrilhaAtual ? [idDaTrilhaAtual] : [])
      const proximas = transformar(new Set(base))
      salvarTrilhasAbertas(proximas)
      return proximas
    })
  }

  function aderirATrilha(trilha: string) {
    setTrilhaAtual(trilha)
    salvarTrilhaAtual(trilha)
    setFiltro('todos')
    setTrilhasRecolhidasNoFiltro(new Set())
    alterarTrilhasAbertas(() => new Set([trilha]))
  }

  function alternarTrilha(trilha: string) {
    if (filtro !== 'todos') {
      setTrilhasRecolhidasNoFiltro((atuais) => {
        const proximas = new Set(atuais)
        if (proximas.has(trilha)) proximas.delete(trilha)
        else proximas.add(trilha)
        return proximas
      })
      return
    }

    alterarTrilhasAbertas((abertas) => {
      if (abertas.has(trilha)) abertas.delete(trilha)
      else abertas.add(trilha)
      return abertas
    })
  }

  function selecionarFiltro(proximoFiltro: Filtro) {
    setFiltro(proximoFiltro)
    setTrilhasRecolhidasNoFiltro(new Set())
  }

  function rolarAte(id: string) {
    document.getElementById(id)?.scrollIntoView({
      behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
    })
  }

  return (
    <>
      <section className="hero-catalogo" aria-labelledby="titulo-catalogo">
        <div className="hero-texto">
          <h1 id="titulo-catalogo">Infraestrutura se aprende com a mão no terminal.</h1>
          <p className="hero-descricao">
            Escolha uma trilha, avance no seu ritmo e prove cada conceito no ambiente local.
          </p>
          {hrefDoFoco ? (
            <a className="botao botao-primario" href={hrefDoFoco}>
              {fundamentosPendentes
                ? 'Começar pelos Fundamentos'
                : focoDaTrilhaAtual && estadoDoCenario(focoDaTrilhaAtual) === 'andamento'
                  ? 'Continuar Cenário'
                  : 'Continuar trilha'}
              <span aria-hidden="true">→</span>
            </a>
          ) : (
            <button
              type="button"
              className="botao botao-primario"
              onClick={() => rolarAte(trilhasDisponiveis.length > 0 ? 'titulo-trilhas' : 'titulo-concluidas')}
            >
              {trilhasDisponiveis.length > 0 ? 'Escolher uma trilha' : 'Ver trilhas concluídas'}
              <span aria-hidden="true">↓</span>
            </button>
          )}
        </div>

        <div className="hero-visual" aria-hidden="true">
          <img src={hero} alt="" />
        </div>
      </section>

      {resumoDaTrilhaAtual ? (
        <section className="painel-progresso" aria-labelledby="titulo-progresso">
          <div className="progresso-cabecalho">
            <div>
              <p className="eyebrow">Trilha atual</p>
              <h2 id="titulo-progresso">{resumoDaTrilhaAtual.nome}</h2>
            </div>
            <div className="progresso-acoes">
              <button type="button" onClick={() => rolarAte('titulo-trilhas')}>Trocar trilha</button>
              <p className="progresso-resumo">
                <strong>{resumoDaTrilhaAtual.percentual}%</strong>
                <span>{resumoDaTrilhaAtual.concluidas} de {resumoDaTrilhaAtual.total} concluídos</span>
              </p>
            </div>
          </div>

          <BarraDeProgressoLinear
            grande
            percentual={resumoDaTrilhaAtual.percentual}
            emAndamento={resumoDaTrilhaAtual.emAndamento}
            total={resumoDaTrilhaAtual.total}
            rotulo={`${resumoDaTrilhaAtual.percentual}% da trilha ${resumoDaTrilhaAtual.nome} concluída`}
          />

          <dl className="resumo-estados">
            <div className="resumo-concluido">
              <dt>Concluídos</dt>
              <dd>{resumoDaTrilhaAtual.concluidas}</dd>
            </div>
            <div className="resumo-andamento">
              <dt>Em andamento</dt>
              <dd>{resumoDaTrilhaAtual.emAndamento}</dd>
            </div>
            <div className="resumo-nao-iniciado">
              <dt>Não iniciados</dt>
              <dd>{resumoDaTrilhaAtual.naoIniciadas}</dd>
            </div>
          </dl>
        </section>
      ) : (
        <section className="painel-progresso painel-sem-trilha" aria-labelledby="titulo-progresso">
          <div>
            <p className="eyebrow">Seu roteiro</p>
            <h2 id="titulo-progresso">Escolha uma trilha para começar</h2>
            <p>O progresso geral aparecerá aqui somente depois que você aderir a uma trilha.</p>
          </div>
          {trilhasDisponiveis.length > 0 && (
            <button type="button" className="botao botao-secundario" onClick={() => rolarAte('titulo-trilhas')}>
              Ver trilhas disponíveis
            </button>
          )}
        </section>
      )}

      {trilhasDisponiveis.length > 0 && (
        <section className="catalogo-lista" aria-labelledby="titulo-trilhas">
          <div className="lista-cabecalho">
            <div>
              <p className="eyebrow">Roteiro de estudo</p>
              <h2 id="titulo-trilhas">Trilhas para estudar</h2>
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
              const aulas = resumo.aulas.filter((cenario) =>
                cenariosVisiveis.some((visivel) => visivel.id === cenario.id),
              )
              if (aulas.length === 0) return null

              const ehAtual = resumo.id === idDaTrilhaAtual
              const aberta = filtro === 'todos'
                ? trilhasAbertasEfetivas.has(resumo.id)
                : !trilhasRecolhidasNoFiltro.has(resumo.id)
              const idDosCenarios = `cenarios-trilha-${resumo.id}`
              const idDoTitulo = `titulo-trilha-${resumo.id}`

              return (
                <section
                  key={resumo.id}
                  className={`trilha ${aberta ? '' : 'trilha-colapsada'} ${ehAtual ? 'trilha-atual' : ''}`}
                  aria-labelledby={idDoTitulo}
                >
                  <header
                    className="trilha-cabecalho"
                    onClick={(evento) => {
                      if ((evento.target as HTMLElement).closest('button, a')) return
                      alternarTrilha(resumo.id)
                    }}
                  >
                    <div className="trilha-identidade">
                      <span className="trilha-icone" aria-hidden="true">▣</span>
                      <div>
                        <h3 className="trilha-titulo" id={idDoTitulo}>{resumo.nome}</h3>
                        <span className="trilha-legenda">
                          {filtro === 'todos'
                            ? `${resumo.aulas.length} ${resumo.aulas.length === 1 ? 'Cenário' : 'Cenários'}`
                            : `${aulas.length} ${aulas.length === 1 ? 'resultado' : 'resultados'} neste filtro`}
                        </span>
                      </div>
                    </div>

                    <ResumoVisualDaTrilha resumo={resumo} />

                    <div className="trilha-acoes">
                      {ehAtual ? (
                        <span className="selo-trilha-atual"><i aria-hidden="true" /> Trilha atual</span>
                      ) : (
                        <button
                          type="button"
                          className="botao trilha-aderir"
                          onClick={() => aderirATrilha(resumo.id)}
                        >
                          {resumo.temProgresso ? 'Retomar trilha' : 'Aderir à trilha'}
                        </button>
                      )}
                      <button
                        type="button"
                        className="trilha-recolher"
                        aria-expanded={aberta}
                        aria-controls={idDosCenarios}
                        aria-label={`${aberta ? 'Recolher' : 'Mostrar'} aulas da trilha ${resumo.nome}`}
                        title={aberta ? 'Recolher aulas' : 'Mostrar aulas'}
                        onClick={() => alternarTrilha(resumo.id)}
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
                    fundamentos={filtro === 'todos' ? resumo.fundamentos : null}
                    aberta={aberta}
                    id={idDosCenarios}
                    idDaTrilha={resumo.id}
                    podeIniciar={ehAtual}
                  />
                </section>
              )
            })
          )}
        </section>
      )}

      {trilhasConcluidas.length > 0 && (
        <section className="arquivo-trilhas" aria-labelledby="titulo-concluidas">
          <div className="arquivo-cabecalho">
            <div>
              <p className="eyebrow">Arquivo de conquistas</p>
              <h2 id="titulo-concluidas">Trilhas concluídas</h2>
            </div>
            <span>{trilhasConcluidas.length} {trilhasConcluidas.length === 1 ? 'trilha' : 'trilhas'}</span>
          </div>
          <div className="trilhas-concluidas-grade">
            {trilhasConcluidas.map((resumo) => (
              <article className="trilha-concluida" key={resumo.id}>
                <span className="trilha-concluida-status"><i aria-hidden="true" /> Trilha concluída</span>
                <h3>{resumo.nome}</h3>
                <p>{resumo.total} etapas concluídas</p>
                <a href={resumo.fundamentos
                  ? `#/trilhas/${resumo.id}/fundamentos`
                  : `#/cenarios/${resumo.aulas[0].id}`}>Revisar conteúdo <span aria-hidden="true">→</span></a>
              </article>
            ))}
          </div>
        </section>
      )}
    </>
  )
}
