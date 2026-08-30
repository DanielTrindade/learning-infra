import { useEffect, useState } from 'react'
import { rolarAteSecao, type SecaoDaAula } from './secoesDaAula'

/**
 * Marca a seção que está sendo lida. Usa IntersectionObserver em vez de ouvir a
 * rolagem: o browser faz a conta fora da thread principal e o React só
 * re-renderiza quando a seção realmente muda.
 */
function useSecaoAtiva(secoes: SecaoDaAula[]): string | null {
  const [ativa, setAtiva] = useState<string | null>(null)
  const chave = secoes.map((secao) => secao.id).join('|')

  useEffect(() => {
    const ids = chave ? chave.split('|') : []
    const alvos = ids
      .map((id) => document.getElementById(id))
      .filter((elemento): elemento is HTMLElement => elemento !== null)
    if (alvos.length === 0) return

    const visiveis = new Set<string>()
    const observador = new IntersectionObserver(
      (entradas) => {
        for (const entrada of entradas) {
          if (entrada.isIntersecting) visiveis.add(entrada.target.id)
          else visiveis.delete(entrada.target.id)
        }
        // A primeira seção visível na ordem do documento é a que está sendo lida.
        const primeira = ids.find((id) => visiveis.has(id))
        if (primeira) setAtiva(primeira)
      },
      // A faixa alta da viewport evita que a última seção curta nunca ative.
      { rootMargin: '-72px 0px -55% 0px', threshold: 0 },
    )

    for (const alvo of alvos) observador.observe(alvo)
    return () => observador.disconnect()
  }, [chave])

  return ativa
}

export function SumarioDaAula({
  secoes,
  rotulo,
}: {
  secoes: SecaoDaAula[]
  rotulo: string
}) {
  const ativa = useSecaoAtiva(secoes)
  if (secoes.length === 0) return null

  return (
    <nav className="sumario" aria-label={rotulo}>
      <p>{rotulo}</p>
      <ol>
        {secoes.map((secao, indice) => (
          <li key={secao.id}>
            <button
              type="button"
              className={secao.id === ativa ? 'sumario-ativo' : undefined}
              aria-current={secao.id === ativa ? 'true' : undefined}
              onClick={() => rolarAteSecao(secao.id)}
            >
              <span>{String(indice + 1).padStart(2, '0')}</span>
              {secao.titulo}
            </button>
          </li>
        ))}
      </ol>
    </nav>
  )
}
