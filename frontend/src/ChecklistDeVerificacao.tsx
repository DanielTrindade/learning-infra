import type { ResultadoDaVerificacao } from './api'

export function ChecklistDeVerificacao({ resultado }: { resultado: ResultadoDaVerificacao }) {
  const aprovadas = resultado.asercoes.filter((asercao) => asercao.passou).length
  const percentual = Math.round((aprovadas / resultado.asercoes.length) * 100)

  return (
    <section
      className={`checklist ${resultado.concluido ? 'checklist-concluido' : 'checklist-pendente'}`}
      aria-live="polite"
    >
      <header>
        <span className="checklist-icone" aria-hidden="true">
          {resultado.concluido ? '✓' : '↻'}
        </span>
        <div>
          <p>{resultado.concluido ? 'Cenário concluído' : 'Continue tentando'}</p>
          <span>{aprovadas} de {resultado.asercoes.length} verificações passaram</span>
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
        <a className="proxima-aula" href="#/">Ver próximos Cenários <span aria-hidden="true">→</span></a>
      )}
    </section>
  )
}
