export type Dificuldade = 'GUIADO' | 'ASSISTIDO' | 'AUTONOMO' | 'MESTRE'

export type CenarioDetalhado = {
  id: string
  titulo: string
  dificuldade: Dificuldade
  markdown: string
  asercoes: string[]
  containers: string[]
  ativo: boolean
  concluido: boolean
}

export type ResultadoDeAsercao = {
  descricao: string
  passou: boolean
  detalhe: string
}

export type ResultadoDaVerificacao = {
  concluido: boolean
  asercoes: ResultadoDeAsercao[]
}

async function pedir<T>(url: string, metodo: 'GET' | 'POST' = 'GET'): Promise<T> {
  const resposta = await fetch(url, { method: metodo })
  if (!resposta.ok) {
    throw new Error(`${metodo} ${url} devolveu ${resposta.status}`)
  }
  return resposta.json() as Promise<T>
}

export const listarCenarios = () => pedir<CenarioDetalhado[]>('/api/cenarios')

export const buscarCenario = (id: string) => pedir<CenarioDetalhado>(`/api/cenarios/${id}`)

export const iniciarCenario = (id: string) =>
  pedir<{ diretorioDeTrabalho: string }>(`/api/cenarios/${id}/iniciar`, 'POST')

export const verificarCenario = (id: string) =>
  pedir<ResultadoDaVerificacao>(`/api/cenarios/${id}/verificar`, 'POST')
