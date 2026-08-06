import { useEffect, useState } from 'react'
import Markdown from 'react-markdown'
import {
  buscarCenario,
  iniciarCenario,
  verificarCenario,
  type CenarioDetalhado,
  type ResultadoDaVerificacao,
} from './api'
import { ChecklistDeVerificacao } from './ChecklistDeVerificacao'

const rotulos: Record<string, string> = {
  GUIADO: 'Guiado',
  ASSISTIDO: 'Assistido',
  AUTONOMO: 'Autônomo',
  MESTRE: 'Mestre',
}

export function PaginaDoCenario({ id }: { id: string }) {
  const [cenario, setCenario] = useState<CenarioDetalhado | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [diretorio, setDiretorio] = useState<string | null>(null)
  const [resultado, setResultado] = useState<ResultadoDaVerificacao | null>(null)
  const [ocupado, setOcupado] = useState(false)

  useEffect(() => {
    buscarCenario(id).then(setCenario).catch((e) => setErro(String(e)))
  }, [id])

  async function aoIniciar() {
    setOcupado(true)
    setErro(null)
    setResultado(null)
    try {
      const { diretorioDeTrabalho } = await iniciarCenario(id)
      setDiretorio(diretorioDeTrabalho)
    } catch (e) {
      setErro(String(e))
    } finally {
      setOcupado(false)
    }
  }

  async function aoVerificar() {
    setOcupado(true)
    setErro(null)
    try {
      setResultado(await verificarCenario(id))
    } catch (e) {
      setErro(String(e))
    } finally {
      setOcupado(false)
    }
  }

  if (erro && !cenario) return <p className="erro">{erro}</p>
  if (!cenario) return <p>Carregando…</p>

  return (
    <article>
      <header>
        <span className="dificuldade">{rotulos[cenario.dificuldade]}</span>
        <h1>{cenario.titulo}</h1>
      </header>

      <div className="acoes">
        <button onClick={aoIniciar} disabled={ocupado}>Iniciar cenário</button>
        <button onClick={aoVerificar} disabled={ocupado || !diretorio}>Verificar</button>
      </div>

      {diretorio && (
        <p className="diretorio">
          Seu diretório de trabalho: <code>{diretorio}</code>
        </p>
      )}

      {erro && <p className="erro">{erro}</p>}

      <Markdown>{cenario.markdown}</Markdown>

      {resultado && <ChecklistDeVerificacao resultado={resultado} />}
    </article>
  )
}
