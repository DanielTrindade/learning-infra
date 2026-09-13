# Verificação ao final do cenário

**Objetivo:** permitir verificar o exercício e consultar seus resultados ao terminar a leitura, sem voltar ao topo.

**Direção:** preservar React e CSS nativo, tokens de tema, tipografia e raios atuais. Variação 4, movimento 3, densidade 5. Adicionar uma seção de fechamento no fluxo da página, sem sobrepor comandos.

## Implementação

- [x] PaginaDoCenario.tsx: adicionar seção após o artigo, com botão Verificar exercício e instrução contextual. Ambiente inativo oferece Iniciar ambiente, sem reiniciar silenciosamente o trabalho.
- [x] Compartilhar estado de envio e handlers entre controles; impedir requisições duplicadas.
- [x] Renderizar uma única checklist junto à origem do clique (lateral ou final). Manter erros no mesmo local e permitir nova tentativa.
- [x] estilos.css: coluna de conteúdo com fechamento responsivo, contraste por tokens e botões sem quebra de texto.
- [x] Validar build/lint e navegador com API simulada: mobile/desktop, claro/escuro, início, envio, pendência, sucesso, erro e nova tentativa, sem retorno ao topo.

## Resultado da validação

Build e lint passaram. Playwright com Edge e API simulada passou nas quatro combinações de 390/1366 px e claro/escuro: início com falha e nova tentativa, erro local de verificação, pendências e aprovação no final, foco na checklist, preservação da rolagem, nenhum reinício involuntário, botões sincronizados e verificação lateral preservada. Capturas de desktop claro e mobile escuro inspecionadas.
