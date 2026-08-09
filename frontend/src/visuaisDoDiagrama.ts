export const visuaisDosDiagramas = [
  'fronteiras-runtime',
  'motor-docker',
  'filesystem-camadas',
  'rotas-container',
  'reconciliacao',
  'arquitetura-cluster',
  'hierarquia-workload',
  'service-endpoints',
  'responsabilidade-aws',
  'fronteiras-aws',
  'planos-aws',
  'fidelidade-local',
] as const

export type VisualDoDiagrama = (typeof visuaisDosDiagramas)[number]
