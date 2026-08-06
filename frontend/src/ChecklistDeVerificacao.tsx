import type { ResultadoDaVerificacao } from './api'

export function ChecklistDeVerificacao({ resultado }: { resultado: ResultadoDaVerificacao }) {
  return (
    <section className="checklist">
      <h2>{resultado.concluido ? 'Cenário concluído' : 'Ainda não'}</h2>
      <ul>
        {resultado.asercoes.map((asercao) => (
          <li key={asercao.descricao} className={asercao.passou ? 'passou' : 'falhou'}>
            <span aria-hidden="true">{asercao.passou ? '✓' : '✗'}</span>
            <span>
              {asercao.descricao}
              {!asercao.passou && asercao.detalhe && <em> — {asercao.detalhe}</em>}
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}
