import type { QuestaoDoQuestionario, ResultadoDoQuestionario } from './api'

type Props = {
  resultado: ResultadoDoQuestionario
  questoes: QuestaoDoQuestionario[]
  primeiroCenario: string | null
  aoRevisar: (id: string) => void
  aoTentarNovamente: () => void
}

export function ResultadoDoQuestionario({
  resultado,
  questoes,
  primeiroCenario,
  aoRevisar,
  aoTentarNovamente,
}: Props) {
  return (
    <section
      className={`resultado-questionario ${resultado.aprovado ? 'resultado-aprovado' : 'resultado-revisar'}`}
      aria-labelledby="titulo-resultado-questionario"
      aria-live="polite"
    >
      <header>
        <span className="resultado-nota" aria-hidden="true">{resultado.percentual}%</span>
        <div>
          <p className="eyebrow">Tentativa {resultado.tentativas}</p>
          <h3 id="titulo-resultado-questionario">
            {resultado.aprovado ? 'Fundamentos concluídos' : 'Vale revisar alguns conceitos'}
          </h3>
          <p>
            {resultado.aprovado
              ? `Você atingiu o aproveitamento. Seu melhor resultado é ${resultado.melhorPercentual}%.`
              : `Seu melhor resultado é ${resultado.melhorPercentual}%. Revise os pontos abaixo e tente novamente.`}
          </p>
        </div>
      </header>

      <ol className="feedback-questionario">
        {resultado.feedback.map((feedback, indice) => {
          const questao = questoes.find((item) => item.id === feedback.questaoId)
          const alternativa = questao?.alternativas.find(
            (item) => item.id === feedback.alternativaCorreta,
          )
          return (
            <li key={feedback.questaoId} className={feedback.acertou ? 'feedback-acertou' : 'feedback-errou'}>
              <span className="feedback-icone" aria-hidden="true">{feedback.acertou ? '✓' : '!'}</span>
              <div>
                <strong>{indice + 1}. {questao?.enunciado}</strong>
                <p>{feedback.explicacao}</p>
                {!feedback.acertou && (
                  <>
                    <small>Resposta correta: {alternativa?.texto ?? feedback.alternativaCorreta}</small>
                    <button type="button" onClick={() => aoRevisar(feedback.revisar)}>
                      Revisar esta seção ↑
                    </button>
                  </>
                )}
              </div>
            </li>
          )
        })}
      </ol>

      <div className="resultado-acoes">
        <button type="button" className="botao botao-secundario" onClick={aoTentarNovamente}>
          Tentar novamente
        </button>
        {primeiroCenario && (
          <a className="botao botao-primario" href={`#/cenarios/${primeiroCenario}`}>
            Ir para o primeiro Cenário <span aria-hidden="true">→</span>
          </a>
        )}
      </div>
    </section>
  )
}
