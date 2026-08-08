import { useEffect, useRef, useState, type ReactNode } from 'react'

export function IconeCopiar() {
  return (
    <svg
      className="icone-copiar"
      viewBox="0 0 16 16"
      width="14"
      height="14"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <rect x="5.5" y="5.5" width="9" height="9" rx="2" />
      <path d="M10.5 3.5V3a2 2 0 0 0-2-2H3a2 2 0 0 0-2 2v5.5a2 2 0 0 0 2 2h.5" />
    </svg>
  )
}

export function IconeCheck() {
  return (
    <svg
      className="icone-check"
      viewBox="0 0 16 16"
      width="14"
      height="14"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M2.5 8.5l3.5 3.5 7.5-8" />
    </svg>
  )
}

export function BlocoDeCodigo({ children }: { children?: ReactNode }) {
  const referenciaDoPre = useRef<HTMLPreElement>(null)
  const temporizador = useRef<number | undefined>(undefined)
  const [copiado, setCopiado] = useState(false)

  useEffect(() => () => window.clearTimeout(temporizador.current), [])

  async function copiar() {
    const texto = referenciaDoPre.current?.textContent?.replace(/\n$/, '') ?? ''
    try {
      await navigator.clipboard.writeText(texto)
    } catch {
      return
    }
    setCopiado(true)
    window.clearTimeout(temporizador.current)
    temporizador.current = window.setTimeout(() => setCopiado(false), 1600)
  }

  return (
    <div className="bloco-codigo">
      <pre ref={referenciaDoPre}>{children}</pre>
      <button
        type="button"
        className={copiado ? 'copiar-codigo copiado' : 'copiar-codigo'}
        onClick={copiar}
        aria-label={copiado ? 'Comando copiado' : 'Copiar comando'}
      >
        <IconeCopiar />
        <IconeCheck />
      </button>
    </div>
  )
}
