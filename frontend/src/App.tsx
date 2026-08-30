import { BotaoDeTema } from './BotaoDeTema'
import { Catalogo } from './Catalogo'
import { MarcaDaPlataforma } from './MarcaDaPlataforma'
import { PaginaDoCenario } from './PaginaDoCenario'
import { PaginaDeFundamentos } from './PaginaDeFundamentos'
import { useRota } from './useRota'
import './estilos.css'

export default function App() {
  const rota = useRota()
  const idDoCenario = rota.startsWith('cenarios/') ? rota.slice('cenarios/'.length) : null
  const rotaDeFundamentos = rota.match(/^trilhas\/([^/]+)\/fundamentos$/)
  const idDaTrilha = rotaDeFundamentos?.[1] ?? null
  const emConteudo = Boolean(idDoCenario || idDaTrilha)

  return (
    <div className="app-shell">
      <a className="pular-para-conteudo" href="#conteudo-principal">Pular para o conteúdo</a>

      <header className="barra-superior">
        <a className="marca" href="#/" aria-label="Learning Infra — início">
          <MarcaDaPlataforma />
          <span>
            <strong>Learning Infra</strong>
            <small>laboratórios locais</small>
          </span>
        </a>
        <div className="barra-acoes">
          <span className="contexto-local" title="Os Cenários rodam na sua máquina">
            <span aria-hidden="true" />
            ambiente local
          </span>
          <BotaoDeTema />
        </div>
      </header>

      {/* Progresso de leitura: só nas páginas longas, e sem uma linha de JS.
          Onde o browser não implementa scroll timelines, a barra fica em 0 e some. */}
      {emConteudo && <div className="progresso-leitura" aria-hidden="true"><span /></div>}

      <main id="conteudo-principal" className={emConteudo ? 'app-main app-main-aula' : 'app-main'}>
        {idDoCenario
          ? <PaginaDoCenario id={idDoCenario} />
          : idDaTrilha
            ? <PaginaDeFundamentos idDaTrilha={idDaTrilha} />
            : <Catalogo />}
      </main>
    </div>
  )
}
