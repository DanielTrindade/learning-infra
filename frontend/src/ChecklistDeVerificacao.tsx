import { useEffect, useRef } from 'react'
import type { VerificationResult } from './api'

export function ChecklistDeVerificacao({ result }: { result: VerificationResult }) {
  const referencia = useRef<HTMLElement>(null)
  const aprovadas = result.assertions.filter((assertion) => assertion.passed).length
  const score = Math.round((aprovadas / result.assertions.length) * 100)
  const firstFailure = result.assertions.find((assertion) => !assertion.passed)

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
  }, [result])

  return (
    <section
      ref={referencia}
      tabIndex={-1}
      className={`checklist ${result.completed ? 'checklist-concluido' : 'checklist-pendente'}`}
      aria-live="polite"
    >
      <header>
        <span className="checklist-icone" aria-hidden="true">
          {result.completed ? '✓' : '↻'}
        </span>
        <div>
          <p>{result.completed ? 'Cenário concluído' : 'Ainda falta ajustar'}</p>
          <span>
            {aprovadas} de {result.assertions.length} verificações passaram
            {!result.completed && firstFailure ? '. Comece pela primeira em vermelho.' : ''}
          </span>
        </div>
      </header>

      <div className="checklist-barra" aria-hidden="true">
        <span style={{ transform: `scaleX(${score / 100})` }} />
      </div>

      <ul>
        {result.assertions.map((assertion) => (
          <li key={assertion.description} className={assertion.passed ? 'passou' : 'falhou'}>
            <span className="check-asserção" aria-hidden="true">{assertion.passed ? '✓' : '×'}</span>
            <span>
              <strong>{assertion.description}</strong>
              {!assertion.passed && assertion.detail && <small>{assertion.detail}</small>}
            </span>
          </li>
        ))}
      </ul>

      {result.completed && (
        <a className="proxima-aula" href="#/aprender">Ver próximos Cenários <span aria-hidden="true">›</span></a>
      )}
    </section>
  )
}
