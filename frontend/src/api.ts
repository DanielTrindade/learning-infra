export type Difficulty = 'GUIDED' | 'ASSISTED' | 'AUTONOMOUS' | 'MASTER'

export type ScenarioDetail = {
  id: string
  title: string
  difficulty: Difficulty
  markdown: string
  assertions: string[]
  containers: string[]
  active: boolean
  completed: boolean
}

export type ScenarioSummary = {
  id: string
  title: string
  difficulty: Difficulty
  assertionCount: number
  active: boolean
  completed: boolean
}

export type FundamentalsState = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'

export type FundamentalsSummary = {
  title: string
  state: FundamentalsState
  bestScore: number
  attempts: number
}

export type TrackSummary = {
  id: string
  title: string
  fundamentals: FundamentalsSummary | null
  scenarios: ScenarioSummary[]
  completed: number
  total: number
  score: number
  allCompleted: boolean
}

export type QuestionOption = {
  id: string
  text: string
}

export type Question = {
  id: string
  statement: string
  options: QuestionOption[]
}

export type FundamentalsDetail = {
  trackId: string
  title: string
  markdown: string
  minimumScore: number
  state: FundamentalsState
  bestScore: number
  attempts: number
  questions: Question[]
}

export type QuestionFeedback = {
  questionId: string
  correct: boolean
  correctOption: string
  explanation: string
  review: string
}

export type QuestionnaireResult = {
  score: number
  passed: boolean
  bestScore: number
  attempts: number
  feedback: QuestionFeedback[]
}

export type AssertionResult = {
  description: string
  passed: boolean
  detail: string
}

export type VerificationResult = {
  completed: boolean
  assertions: AssertionResult[]
}

async function request<T>(url: string, method: 'GET' | 'POST' = 'GET', body?: unknown): Promise<T> {
  const response = await fetch(url, {
    method,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    const detail = await extractDetail(response)
    throw new Error(detail ?? `${method} ${url} devolveu ${response.status}`)
  }
  return response.json() as Promise<T>
}

/**
 * O backend responde RFC 7807 quando algo falha; `detail` é a mensagem que explica o
 * problema — sem ele, o leitor só veria o código HTTP.
 */
async function extractDetail(response: Response): Promise<string | null> {
  try {
    const body = (await response.json()) as { detail?: string; message?: string }
    return body.detail ?? body.message ?? null
  } catch {
    return null
  }
}

export const listScenarios = () => request<ScenarioDetail[]>('/api/scenarios')

export const listTracks = () => request<TrackSummary[]>('/api/tracks')

export const resetTrackProgress = (trackId: string) =>
  request<TrackSummary>(`/api/tracks/${encodeURIComponent(trackId)}/reset-progress`, 'POST')

export const getFundamentals = (trackId: string) =>
  request<FundamentalsDetail>(`/api/tracks/${trackId}/fundamentals`)

export const submitQuestionnaire = (trackId: string, answers: Record<string, string>) =>
  request<QuestionnaireResult>(`/api/tracks/${trackId}/questionnaire`, 'POST', { answers })

export const getScenario = (id: string) => request<ScenarioDetail>(`/api/scenarios/${id}`)

export const startScenario = (id: string) =>
  request<{ workingDirectory: string }>(`/api/scenarios/${id}/start`, 'POST')

export const verifyScenario = (id: string) =>
  request<VerificationResult>(`/api/scenarios/${id}/verify`, 'POST')
