import { Catalogo } from './Catalogo'
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
      <header className="barra-superior">
        <a className="marca" href="#/" aria-label="Learning Infra — início">
          <img className="marca-simbolo" src="/favicon.svg" alt="" />
          <span>
            <strong>Learning Infra</strong>
            <small>Cenários locais</small>
          </span>
        </a>
        <span className="contexto-local">
          <span aria-hidden="true" />
          ambiente local
        </span>
      </header>

      <main className={emConteudo ? 'app-main app-main-aula' : 'app-main'}>
        {idDoCenario
          ? <PaginaDoCenario id={idDoCenario} />
          : idDaTrilha
            ? <PaginaDeFundamentos idDaTrilha={idDaTrilha} />
            : <Catalogo />}
      </main>
    </div>
  )
}
