import { lazy, Suspense } from 'react'
import { CabecalhoDaPlataforma } from './CabecalhoDaPlataforma'
import { LandingPage } from './LandingPage'
import { useRota } from './useRota'
import './estilos.css'

const Catalogo = lazy(() => import('./Catalogo').then((modulo) => ({ default: modulo.Catalogo })))
const PaginaDoCenario = lazy(() => import('./PaginaDoCenario').then((modulo) => ({ default: modulo.PaginaDoCenario })))
const PaginaDeFundamentos = lazy(() => import('./PaginaDeFundamentos').then((modulo) => ({ default: modulo.PaginaDeFundamentos })))

function CarregamentoDaRota() {
  return (
    <div className="carregamento-rota" role="status" aria-label="Carregando conteúdo">
      <span />
      <span />
      <span />
    </div>
  )
}

export default function App() {
  const rota = useRota()
  const scenarioId = rota.startsWith('cenarios/') ? rota.slice('cenarios/'.length) : null
  const rotaDeFundamentos = rota.match(/^trilhas\/([^/]+)\/fundamentos$/)
  const trackId = rotaDeFundamentos?.[1] ?? null
  const emConteudo = Boolean(scenarioId || trackId)
  const naAreaDeAprendizado = rota === 'aprender'
  const naLanding = !emConteudo && !naAreaDeAprendizado
  const contextoDoCabecalho = emConteudo
    ? 'conteudo'
    : naAreaDeAprendizado
      ? 'aprendizado'
      : 'landing'

  return (
    <div className="app-shell">
      <a className="pular-para-conteudo" href="#conteudo-principal">Pular para o conteúdo</a>

      <CabecalhoDaPlataforma contexto={contextoDoCabecalho} />

      {/* Progresso de leitura: só nas páginas longas, e sem uma linha de JS.
          Onde o browser não implementa scroll timelines, a barra fica em 0 e some. */}
      {emConteudo && <div className="progresso-leitura" aria-hidden="true"><span /></div>}

      <main
        id="conteudo-principal"
        className={
          emConteudo
            ? 'app-main app-main-aula'
            : naLanding
              ? 'app-main app-main-landing'
              : 'app-main app-main-aprendizado'
        }
      >
        <Suspense fallback={<CarregamentoDaRota />}>
          {scenarioId
            ? <PaginaDoCenario id={scenarioId} />
            : trackId
              ? <PaginaDeFundamentos trackId={trackId} />
              : naAreaDeAprendizado
                ? <Catalogo />
                : <LandingPage />}
        </Suspense>
      </main>
    </div>
  )
}
