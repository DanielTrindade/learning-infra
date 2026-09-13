# Reset de progresso por tema

**Objetivo:** permitir zerar conclusões e questionários de uma trilha e distinguir cenários concluídos na lista expandida.

**Arquitetura:** operação atômica em ProgressRepository.update, exposta por POST /api/tracks/{id}/reset-progress. Preservar activeScenario: ele identifica recursos reais que precisam continuar rastreados para teardown. O frontend recebe o resumo atualizado e recalcula o catálogo.

**Stack:** Spring MVC, Java 25, React 19, CSS nativo, sem novas dependências.

## Auditoria e restrições

- Preservar rotas, tipografia de sistema, acento teal, raios e tokens claro/escuro existentes.
- DESIGN_VARIANCE: 4; MOTION_INTENSITY: 3; VISUAL_DENSITY: 5. Interface de produto, evolução pontual.
- CSS usa nomes antigos de estados em português, enquanto React gera completed/in-progress/not-started. Corrigir essa divergência.
- Contrato do resumo usa score no backend, mas progress no frontend. Alinhar para atualizar percentuais corretamente.
- Reset exige confirmação no produto e explica que o ambiente ativo permanece em andamento. Erros ficam no diálogo e permitem nova tentativa.
- Preservar Unicode e progresso dos demais temas, inclusive IDs com prefixos semelhantes.

## Implementação

- [x] Adicionar Progress.withoutTrackProgress(String trackId), removendo completed com prefixo trackId + "/" e fundamentals[trackId], mantendo activeScenario.
- [x] Adicionar TrackController.resetProgress(String id), validando requireTrack antes de progressRepository.update e retornando TrackSummary.from.
- [x] Cobrir persistência, isolamento por tema, idempotência, tema inexistente e preservação do cenário ativo em TrackControllerTest.
- [x] Adicionar resetTrackProgress no cliente e corrigir TrackSummary.score.
- [x] Adicionar confirmação via dialog nativo, cancelamento, estado de envio, erro e feedback de sucesso ao catálogo; oferecer reset nas trilhas disponíveis e arquivadas.
- [x] Corrigir seletores de estado e destacar concluídos com marcador, selo e fundo sem depender apenas de cor.
- [x] Executar testes do backend, build/lint e verificar a interface em desktop/mobile e claro/escuro.

## Validação

- 12 testes do backend passaram (TrackControllerTest e ProgressRepositoryTest).
- Build e lint passaram.
- Playwright com Edge e API simulada: claro/escuro, 1366 px e 390 px, sem overflow; foco inicial, retorno do foco, Escape, cancelamento sem requisição, bloqueio durante envio, erro e nova tentativa, isolamento do reset, retorno de trilha arquivada e recarga.
- Capturas inspecionadas em desktop claro e mobile escuro, incluindo confirmação. Dados reais não foram resetados.
