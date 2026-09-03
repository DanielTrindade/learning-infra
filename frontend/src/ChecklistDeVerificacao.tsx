import { useEffect, useRef } from 'react'
import type { ResultadoDaVerificacao } from './api'

export function ChecklistDeVerificacao({ resultado }: { resultado: ResultadoDaVerificacao }) {
  const referencia = useRef<HTMLElement>(null)
  const aprovadas = resultado.asercoes.filter((asercao) => asercao.passou).length
  const percentual = Math.round((aprovadas / resultado.asercoes.length) * 100)
  const primeiraFalha = resultado.asercoes.find((asercao) => !asercao.passou)

  // O resultado é a resposta a um clique: leva o foco e a rolagem até ele, em
  // vez de contar que a pessoa perceba um painel aparecendo fora da vista.
  useEffect(() => {
    const painel = referencia.current
    if (!painel) return
    painel.focus({ preventScroll: true })
    painel.scrollIntoView({
      block: 'nearest',
      behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
    })
  }, [resultado])

  return (
    <section
      ref={referencia}
      tabIndex={-1}
      className={`checklist ${resultado.concluido ? 'checklist-concluido' : 'checklist-pendente'}`}
      aria-live="polite"
    >
      <header>
        <span className="checklist-icone" aria-hidden="true">
          {resultado.concluido ? '✓' : '↻'}
        </span>
        <div>
          <p>{resultado.concluido ? 'Cenário concluído' : 'Ainda falta ajustar'}</p>
          <span>
            {aprovadas} de {resultado.asercoes.length} verificações passaram
            {!resultado.concluido && primeiraFalha ? '. Comece pela primeira em vermelho.' : ''}
          </span>
        </div>
      </header>

      <div className="checklist-barra" aria-hidden="true">
        <span style={{ transform: `scaleX(${percentual / 100})` }} />
      </div>

      <ul>
        {resultado.asercoes.map((asercao) => (
          <li key={asercao.descricao} className={asercao.passou ? 'passou' : 'falhou'}>
            <span className="check-asserção" aria-hidden="true">{asercao.passou ? '✓' : '×'}</span>
            <span>
              <strong>{asercao.descricao}</strong>
              {!asercao.passou && asercao.detalhe && <small>{asercao.detalhe}</small>}
            </span>
          </li>
        ))}
      </ul>

      {resultado.concluido && (
        <a className="proxima-aula" href="#/aprender">Ver próximos Cenários <span aria-hidden="true">›</span></a>
      )}
    </section>
  )
}
