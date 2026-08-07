import type { CenarioDetalhado, Dificuldade } from './api'

export type EstadoDoCenario = 'concluido' | 'andamento' | 'nao-iniciado'

const CHAVE_TRILHA_ATUAL = 'learning-infra:trilha-atual:v1'

export const rotulosDificuldade: Record<Dificuldade, string> = {
  GUIADO: 'Guiado',
  ASSISTIDO: 'Assistido',
  AUTONOMO: 'Autônomo',
  MESTRE: 'Mestre',
}

export const rotulosEstado: Record<EstadoDoCenario, string> = {
  concluido: 'Concluído',
  andamento: 'Em andamento',
  'nao-iniciado': 'Não iniciado',
}

export function estadoDoCenario(cenario: CenarioDetalhado): EstadoDoCenario {
  if (cenario.concluido) return 'concluido'
  if (cenario.ativo) return 'andamento'
  return 'nao-iniciado'
}

export function numeroDoCenario(id: string): string {
  const slug = id.split('/').at(-1) ?? id
  return slug.match(/^\d+/)?.[0] ?? '—'
}

export function nomeDaTrilha(id: string): string {
  const nome = id.split('/')[0] ?? id
  return nome.charAt(0).toLocaleUpperCase('pt-BR') + nome.slice(1)
}

export function idDaTrilha(idDoCenario: string): string {
  return idDoCenario.split('/')[0] ?? idDoCenario
}

export function carregarTrilhaAtual(): string | null {
  try {
    return localStorage.getItem(CHAVE_TRILHA_ATUAL)
  } catch {
    return null
  }
}

export function salvarTrilhaAtual(trilha: string) {
  try {
    localStorage.setItem(CHAVE_TRILHA_ATUAL, trilha)
  } catch {
    // O laboratório continua utilizável quando o browser bloqueia armazenamento local.
  }
}
