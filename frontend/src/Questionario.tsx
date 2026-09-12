import { useState, type FormEvent } from 'react'
import {
  submitQuestionnaire,
  type Question,
  type QuestionnaireResult,
} from './api'
import { QuestionnaireResult as Resultado } from './ResultadoDoQuestionario'

type Props = {
  trackId: string
  questions: Question[]
  primeiroCenario: string | null
  aoResultado: (result: QuestionnaireResult) => void
  aoRevisar: (id: string) => void
}

function embaralhar<T>(itens: T[]): T[] {
  const copia = [...itens]
  for (let indice = copia.length - 1; indice > 0; indice -= 1) {
    const destino = Math.floor(Math.random() * (indice + 1))
    const atual = copia[indice]
    copia[indice] = copia[destino]
    copia[destino] = atual
  }
  return copia
}

function prepararQuestoes(questions: Question[]) {
  return embaralhar(questions).map((questao) => ({
    ...questao,
    options: embaralhar(questao.options),
  }))
}

export function Questionario({
  trackId,
  questions,
  primeiroCenario,
  aoResultado,
  aoRevisar,
}: Props) {
  const [answers, setRespostas] = useState<Record<string, string>>({})
  const [questoesOrdenadas, setQuestoesOrdenadas] = useState(() => prepararQuestoes(questions))
  const [result, setResultado] = useState<QuestionnaireResult | null>(null)
  const [error, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const primeiraSemResposta = questoesOrdenadas.find((questao) => !answers[questao.id])
    if (primeiraSemResposta) {
      setErro('Responda todas as questões antes de conferir o resultado.')
      document.getElementById(`questao-${primeiraSemResposta.id}`)?.focus()
      return
    }

    setEnviando(true)
    setErro(null)
    try {
      const newResult = await submitQuestionnaire(trackId, answers)
      setResultado(newResult)
      aoResultado(newResult)
    } catch (e) {
      setErro(String(e))
    } finally {
      setEnviando(false)
    }
  }

  function tentarNovamente() {
    setRespostas({})
    setResultado(null)
    setQuestoesOrdenadas(prepararQuestoes(questions))
    setErro(null)
    document.getElementById('inicio-questionario')?.scrollIntoView({ behavior: 'auto' })
  }

  if (result) {
    return (
      <Resultado
        result={result}
        questions={questions}
        primeiroCenario={primeiroCenario}
        aoRevisar={aoRevisar}
        aoTentarNovamente={tentarNovamente}
      />
    )
  }

  return (
    <form className="questionario" onSubmit={enviar} noValidate>
      {questoesOrdenadas.map((questao, indice) => (
        <fieldset key={questao.id} id={`questao-${questao.id}`} tabIndex={-1}>
          <legend>
            <span>{String(indice + 1).padStart(2, '0')}</span>
            {questao.statement}
          </legend>
          <div className="alternativas">
            {questao.options.map((alternativa) => (
              <label key={alternativa.id}>
                <input
                  type="radio"
                  name={questao.id}
                  value={alternativa.id}
                  checked={answers[questao.id] === alternativa.id}
                  onChange={() => setRespostas((atuais) => ({
                    ...atuais,
                    [questao.id]: alternativa.id,
                  }))}
                />
                <span aria-hidden="true" />
                {alternativa.text}
              </label>
            ))}
          </div>
        </fieldset>
      ))}

      {error && <p className="erro-questionario" role="alert">{error}</p>}
      <button className="botao botao-primario" type="submit" disabled={enviando}>
        {enviando && <span className="spinner spinner-botao" aria-hidden="true" />}
        {enviando ? 'Corrigindo…' : 'Conferir respostas'}
      </button>
    </form>
  )
}
