import { useEffect, useState } from 'react'
import { aplicarTema, carregarTema, salvarTema, temaDoSistema, type Tema } from './tema'

function IconeSol() {
  return (
    <svg viewBox="0 0 20 20" width="16" height="16" fill="none" stroke="currentColor"
      strokeWidth="1.6" strokeLinecap="round" aria-hidden="true">
      <circle cx="10" cy="10" r="3.6" />
      <path d="M10 2.2v1.6M10 16.2v1.6M17.8 10h-1.6M3.8 10H2.2M15.5 4.5l-1.1 1.1M5.6 14.4l-1.1 1.1M15.5 15.5l-1.1-1.1M5.6 5.6L4.5 4.5" />
    </svg>
  )
}

function IconeLua() {
  return (
    <svg viewBox="0 0 20 20" width="16" height="16" fill="none" stroke="currentColor"
      strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M16.2 12.4A6.8 6.8 0 0 1 7.6 3.8a6.8 6.8 0 1 0 8.6 8.6Z" />
    </svg>
  )
}

export function BotaoDeTema() {
  const [tema, setTema] = useState<Tema | null>(carregarTema)

  useEffect(() => {
    aplicarTema(tema)
  }, [tema])

  // Sem escolha manual, acompanhar o sistema em tempo real.
  useEffect(() => {
    if (tema !== null) return
    const consulta = window.matchMedia('(prefers-color-scheme: dark)')
    const aoMudar = () => aplicarTema(null)
    consulta.addEventListener('change', aoMudar)
    return () => consulta.removeEventListener('change', aoMudar)
  }, [tema])

  const efetivo = tema ?? temaDoSistema()
  const proximo: Tema = efetivo === 'escuro' ? 'claro' : 'escuro'

  return (
    <button
      type="button"
      className="botao-tema"
      onClick={() => {
        salvarTema(proximo)
        setTema(proximo)
      }}
      aria-label={`Mudar para o tema ${proximo}`}
      title={`Tema ${efetivo} — clique para ${proximo}`}
    >
      <span className="botao-tema-icones" aria-hidden="true">
        <IconeSol />
        <IconeLua />
      </span>
    </button>
  )
}
