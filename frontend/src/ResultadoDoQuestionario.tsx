import type { Question, QuestionnaireResult } from './api'

type Props = {
  result: QuestionnaireResult
  questions: Question[]
  primeiroCenario: string | null
  aoRevisar: (id: string) => void
  aoTentarNovamente: () => void
}

export function QuestionnaireResult({
  result,
  questions,
  primeiroCenario,
  aoRevisar,
  aoTentarNovamente,
}: Props) {
  return (
    <section
      className={`resultado-questionario ${result.passed ? 'resultado-aprovado' : 'resultado-revisar'}`}
      aria-labelledby="titulo-resultado-questionario"
      aria-live="polite"
    >
      <header>
        <span className="resultado-nota" aria-hidden="true">{result.score}%</span>
        <div>
          <p className="eyebrow">Tentativa {result.attempts}</p>
          <h3 id="titulo-resultado-questionario">
            {result.passed ? 'Fundamentos concluídos' : 'Vale revisar alguns conceitos'}
          </h3>
          <p>
            {result.passed
              ? `Você atingiu o aproveitamento. Seu melhor resultado é ${result.bestScore}%.`
              : `Seu melhor resultado é ${result.bestScore}%. Revise os pontos abaixo e tente novamente.`}
          </p>
        </div>
      </header>

      <ol className="feedback-questionario">
        {result.feedback.map((feedback, indice) => {
          const questao = questions.find((item) => item.id === feedback.questionId)
          const alternativa = questao?.options.find(
            (item) => item.id === feedback.correctOption,
          )
          return (
            <li key={feedback.questionId} className={feedback.correct ? 'feedback-acertou' : 'feedback-errou'}>
              <span className="feedback-icone" aria-hidden="true">{feedback.correct ? '✓' : '!'}</span>
              <div>
                <strong>{indice + 1}. {questao?.statement}</strong>
                <p>{feedback.explanation}</p>
                {!feedback.correct && (
                  <>
                    <small>Resposta correta: {alternativa?.text ?? feedback.correctOption}</small>
                    <button type="button" onClick={() => aoRevisar(feedback.review)}>
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
            Ir para o primeiro Cenário <span aria-hidden="true">›</span>
          </a>
        )}
      </div>
    </section>
  )
}
