import { MarcaDaPlataforma } from './MarcaDaPlataforma'

type ContextoDoCabecalho = 'landing' | 'aprendizado' | 'conteudo'

function rolarAte(id: string) {
  document.getElementById(id)?.scrollIntoView({
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
  })
}

export function CabecalhoDaPlataforma({ contexto }: { contexto: ContextoDoCabecalho }) {
  const naLanding = contexto === 'landing'

  return (
    <div className="cabecalho-vidro">
      <header className={`barra-superior barra-superior-${contexto}`}>
        <a
          className="marca"
          href="#/"
          aria-label="Learning Infra, página inicial"
        >
          <MarcaDaPlataforma />
          <span>
            <strong>Learning Infra</strong>
            <small>laboratórios locais</small>
          </span>
        </a>

        {naLanding ? (
          <div className="barra-navegacao">
            <nav aria-label="Navegação principal">
              <button type="button" onClick={() => rolarAte('como-funciona')}>Como funciona</button>
              <button type="button" onClick={() => rolarAte('trilhas-disponiveis')}>Trilhas</button>
            </nav>
            <a className="acesso-aprendizado" href="#/aprender">Minha aprendizagem</a>
          </div>
        ) : (
          <div className="barra-acoes">
            <a className="atalho-inicio" href="#/">Página inicial</a>
            {contexto === 'conteudo' && (
              <a className="atalho-aprendizado" href="#/aprender">Minha aprendizagem</a>
            )}
            <span className="contexto-local">Execução local</span>
          </div>
        )}
      </header>
    </div>
  )
}
