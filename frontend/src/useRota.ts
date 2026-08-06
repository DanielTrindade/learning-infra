import { useEffect, useState } from 'react'

/** Devolve o caminho corrente depois do `#`, sem a cerquilha. Ex.: "cenarios/docker/01". */
export function useRota(): string {
  const ler = () => window.location.hash.replace(/^#\/?/, '')
  const [rota, setRota] = useState(ler)

  useEffect(() => {
    const aoMudar = () => setRota(ler())
    window.addEventListener('hashchange', aoMudar)
    return () => window.removeEventListener('hashchange', aoMudar)
  }, [])

  return rota
}
