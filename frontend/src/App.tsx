import { Catalogo } from './Catalogo'
import { PaginaDoCenario } from './PaginaDoCenario'
import { useRota } from './useRota'
import './estilos.css'

export default function App() {
  const rota = useRota()
  const id = rota.startsWith('cenarios/') ? rota.slice('cenarios/'.length) : null

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

      <main className={id ? 'app-main app-main-aula' : 'app-main'}>
        {id ? <PaginaDoCenario id={id} /> : <Catalogo />}
      </main>
    </div>
  )
}
