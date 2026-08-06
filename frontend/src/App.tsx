import { Catalogo } from './Catalogo'
import { PaginaDoCenario } from './PaginaDoCenario'
import { useRota } from './useRota'
import './estilos.css'

export default function App() {
  const rota = useRota()
  const id = rota.startsWith('cenarios/') ? rota.slice('cenarios/'.length) : null

  return (
    <main>
      {id ? (
        <>
          <a className="voltar" href="#/">← todos os laboratórios</a>
          <PaginaDoCenario id={id} />
        </>
      ) : (
        <Catalogo />
      )}
    </main>
  )
}
