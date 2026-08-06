import { useEffect, useState } from 'react'
import { listarCenarios, type CenarioDetalhado } from './api'

const rotulos: Record<string, string> = {
  GUIADO: 'Guiado',
  ASSISTIDO: 'Assistido',
  AUTONOMO: 'Autônomo',
  MESTRE: 'Mestre',
}

export function Catalogo() {
  const [cenarios, setCenarios] = useState<CenarioDetalhado[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    listarCenarios().then(setCenarios).catch((e) => setErro(String(e)))
  }, [])

  if (erro) return <p className="erro">{erro}</p>
  if (!cenarios) return <p>Carregando…</p>
  if (cenarios.length === 0) return <p>Nenhum Cenário em content/ ainda.</p>

  const trilhas = [...new Set(cenarios.map((c) => c.id.split('/')[0]))]

  return (
    <>
      <h1>Laboratórios</h1>
      {trilhas.map((trilha) => (
        <section key={trilha} className="trilha">
          <h2>{trilha}</h2>
          <ul className="cenarios">
            {cenarios
              .filter((c) => c.id.startsWith(`${trilha}/`))
              .map((c) => (
                <li key={c.id}>
                  <a href={`#/cenarios/${c.id}`}>
                    <span className="dificuldade">{rotulos[c.dificuldade]}</span>
                    <span className="titulo">{c.titulo}</span>
                    {c.concluido && <span className="selo" title="concluído">✓</span>}
                    {c.ativo && <span className="selo ativo" title="ambiente no ar">●</span>}
                  </a>
                </li>
              ))}
          </ul>
        </section>
      ))}
    </>
  )
}
