import { useState, type FormEvent } from 'react'
import {
  responderQuestionario,
  type QuestaoDoQuestionario,
  type ResultadoDoQuestionario,
} from './api'
import { ResultadoDoQuestionario as Resultado } from './ResultadoDoQuestionario'

type Props = {
  idDaTrilha: string
  questoes: QuestaoDoQuestionario[]
  primeiroCenario: string | null
  aoResultado: (resultado: ResultadoDoQuestionario) => void
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

function prepararQuestoes(questoes: QuestaoDoQuestionario[]) {
  return embaralhar(questoes).map((questao) => ({
    ...questao,
    alternativas: embaralhar(questao.alternativas),
  }))
}

export function Questionario({
  idDaTrilha,
  questoes,
  primeiroCenario,
  aoResultado,
  aoRevisar,
}: Props) {
  const [respostas, setRespostas] = useState<Record<string, string>>({})
  const [questoesOrdenadas, setQuestoesOrdenadas] = useState(() => prepararQuestoes(questoes))
  const [resultado, setResultado] = useState<ResultadoDoQuestionario | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const primeiraSemResposta = questoesOrdenadas.find((questao) => !respostas[questao.id])
    if (primeiraSemResposta) {
      setErro('Responda todas as questões antes de conferir o resultado.')
      document.getElementById(`questao-${primeiraSemResposta.id}`)?.focus()
      return
    }

    setEnviando(true)
    setErro(null)
    try {
      const novoResultado = await responderQuestionario(idDaTrilha, respostas)
      setResultado(novoResultado)
      aoResultado(novoResultado)
    } catch (e) {
      setErro(String(e))
    } finally {
      setEnviando(false)
    }
  }

  function tentarNovamente() {
    setRespostas({})
    setResultado(null)
    setQuestoesOrdenadas(prepararQuestoes(questoes))
    setErro(null)
    document.getElementById('inicio-questionario')?.scrollIntoView({ behavior: 'auto' })
  }

  if (resultado) {
    return (
      <Resultado
        resultado={resultado}
        questoes={questoes}
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
            {questao.enunciado}
          </legend>
          <div className="alternativas">
            {questao.alternativas.map((alternativa) => (
              <label key={alternativa.id}>
                <input
                  type="radio"
                  name={questao.id}
                  value={alternativa.id}
                  checked={respostas[questao.id] === alternativa.id}
                  onChange={() => setRespostas((atuais) => ({
                    ...atuais,
                    [questao.id]: alternativa.id,
                  }))}
                />
                <span aria-hidden="true" />
                {alternativa.texto}
              </label>
            ))}
          </div>
        </fieldset>
      ))}

      {erro && <p className="erro-questionario" role="alert">{erro}</p>}
      <button className="botao botao-primario" type="submit" disabled={enviando}>
        {enviando && <span className="spinner spinner-botao" aria-hidden="true" />}
        {enviando ? 'Corrigindo…' : 'Conferir respostas'}
      </button>
    </form>
  )
}
