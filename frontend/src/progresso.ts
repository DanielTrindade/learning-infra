import type { Difficulty } from './api'

export type ScenarioState = 'completed' | 'in-progress' | 'not-started'

const CURRENT_TRACK_KEY = 'learning-infra:trilha-atual:v1'

export const difficultyLabels: Record<Difficulty, string> = {
  GUIDED: 'Guiado',
  ASSISTED: 'Assistido',
  AUTONOMOUS: 'Autônomo',
  MASTER: 'Mestre',
}

export const stateLabels: Record<ScenarioState, string> = {
  completed: 'Concluído',
  'in-progress': 'Em andamento',
  'not-started': 'Não iniciado',
}

export function scenarioState(scenario: { completed: boolean; active: boolean }): ScenarioState {
  if (scenario.completed) return 'completed'
  if (scenario.active) return 'in-progress'
  return 'not-started'
}

export function scenarioNumber(id: string): string {
  const slug = id.split('/').at(-1) ?? id
  return slug.match(/^\d+/)?.[0] ?? 's/n'
}

export function trackName(id: string): string {
  const name = id.split('/')[0] ?? id
  return name.charAt(0).toLocaleUpperCase('pt-BR') + name.slice(1)
}

export function trackId(scenarioId: string): string {
  return scenarioId.split('/')[0] ?? scenarioId
}

export function loadCurrentTrack(): string | null {
  try {
    return localStorage.getItem(CURRENT_TRACK_KEY)
  } catch {
    return null
  }
}

export function saveCurrentTrack(track: string) {
  try {
    localStorage.setItem(CURRENT_TRACK_KEY, track)
  } catch {
    // O laboratório continua utilizável quando o browser bloqueia armazenamento local.
  }
}

export function clearCurrentTrack() {
  try {
    localStorage.removeItem(CURRENT_TRACK_KEY)
  } catch {
    // O laboratório continua utilizável quando o browser bloqueia armazenamento local.
  }
}
