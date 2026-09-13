import { useEffect, useRef, useState } from 'react'
import { resetTrackProgress, type TrackSummary } from './api'

export function ConfirmacaoDeReset({
  trilha,
  onCancelar,
  onReset,
}: {
  trilha: { id: string; name: string }
  onCancelar: () => void
  onReset: (track: TrackSummary) => void
}) {
  const dialog = useRef<HTMLDialogElement>(null)
  const enviando = useRef(false)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    const elemento = dialog.current
    const focoAnterior = document.activeElement
    elemento?.showModal()
    return () => {
      elemento?.close()
      if (focoAnterior instanceof HTMLElement && focoAnterior.isConnected) focoAnterior.focus()
    }
  }, [])

  async function confirmar() {
    if (enviando.current) return
    enviando.current = true
    setSalvando(true)
    setErro(null)
    try {
      onReset(await resetTrackProgress(trilha.id))
    } catch (error) {
      setErro(error instanceof Error ? error.message : 'Tente novamente.')
    } finally {
      enviando.current = false
      setSalvando(false)
    }
  }

  return (
    <dialog
      ref={dialog}
      className="confirmacao-reset"
      aria-labelledby="reset-titulo"
      aria-describedby="reset-descricao"
      onCancel={(evento) => {
        evento.preventDefault()
        if (!enviando.current) onCancelar()
      }}
    >
      <h2 id="reset-titulo">Resetar progresso de {trilha.name}?</h2>
      <div id="reset-descricao">
        <p>As conclusões dos cenários e os resultados do questionário deste tema serão apagados. Esta ação não pode ser desfeita.</p>
        <p>Os outros temas não serão alterados. Se houver um laboratório ativo, ele continuará em andamento, com seus arquivos preservados.</p>
      </div>
      {erro && <p className="reset-erro" role="alert">Não foi possível resetar o progresso. {erro}</p>}
      <div className="reset-acoes" aria-busy={salvando}>
        <button type="button" className="botao" autoFocus disabled={salvando} onClick={onCancelar}>Cancelar</button>
        <button type="button" className="botao botao-reset-confirmar" disabled={salvando} onClick={confirmar}>
          {salvando ? 'Resetando…' : 'Resetar progresso'}
        </button>
      </div>
    </dialog>
  )
}
