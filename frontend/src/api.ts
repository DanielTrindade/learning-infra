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

export type CenarioResumo = {
  id: string
  titulo: string
  dificuldade: Dificuldade
  quantidadeDeAsercoes: number
  ativo: boolean
  concluido: boolean
}

export type EstadoDosFundamentos = 'NAO_INICIADO' | 'EM_ANDAMENTO' | 'CONCLUIDO'

export type FundamentosResumo = {
  titulo: string
  estado: EstadoDosFundamentos
  melhorPercentual: number
  tentativas: number
}

export type TrilhaResumo = {
  id: string
  titulo: string
  fundamentos: FundamentosResumo | null
  cenarios: CenarioResumo[]
  concluidos: number
  total: number
  percentual: number
  concluida: boolean
}

export type AlternativaDoQuestionario = {
  id: string
  texto: string
}

export type QuestaoDoQuestionario = {
  id: string
  enunciado: string
  alternativas: AlternativaDoQuestionario[]
}

export type FundamentosDetalhados = {
  idDaTrilha: string
  titulo: string
  markdown: string
  aproveitamentoMinimo: number
  estado: EstadoDosFundamentos
  melhorPercentual: number
  tentativas: number
  questoes: QuestaoDoQuestionario[]
}

export type FeedbackDoQuestionario = {
  questaoId: string
  acertou: boolean
  alternativaCorreta: string
  explicacao: string
  revisar: string
}

export type ResultadoDoQuestionario = {
  percentual: number
  aprovado: boolean
  melhorPercentual: number
  tentativas: number
  feedback: FeedbackDoQuestionario[]
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

async function pedir<T>(url: string, metodo: 'GET' | 'POST' = 'GET', corpo?: unknown): Promise<T> {
  const resposta = await fetch(url, {
    method: metodo,
    headers: corpo === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  })
  if (!resposta.ok) {
    throw new Error(`${metodo} ${url} devolveu ${resposta.status}`)
  }
  return resposta.json() as Promise<T>
}

export const listarCenarios = () => pedir<CenarioDetalhado[]>('/api/cenarios')

export const listarTrilhas = () => pedir<TrilhaResumo[]>('/api/trilhas')

export const buscarFundamentos = (idDaTrilha: string) =>
  pedir<FundamentosDetalhados>(`/api/trilhas/${idDaTrilha}/fundamentos`)

export const responderQuestionario = (idDaTrilha: string, respostas: Record<string, string>) =>
  pedir<ResultadoDoQuestionario>(
    `/api/trilhas/${idDaTrilha}/questionario`,
    'POST',
    { respostas },
  )

export const buscarCenario = (id: string) => pedir<CenarioDetalhado>(`/api/cenarios/${id}`)

export const iniciarCenario = (id: string) =>
  pedir<{ diretorioDeTrabalho: string }>(`/api/cenarios/${id}/iniciar`, 'POST')

export const verificarCenario = (id: string) =>
  pedir<ResultadoDaVerificacao>(`/api/cenarios/${id}/verificar`, 'POST')
