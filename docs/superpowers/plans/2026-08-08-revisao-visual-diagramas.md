# Revisão Visual dos Diagramas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduzir o aspecto genérico dos diagramas, cortar copy redundante e dar a cada conceito uma mini-ilustração vetorial própria sem tirar apresentação do frontend.

**Architecture:** O Markdown continua declarando apenas significado. Um novo campo opcional `visual` escolhe uma miniatura do catálogo fechado tipado em `visuaisDoDiagrama.ts`; `IlustracaoDoDiagrama.tsx` desenha a imagem e `Diagrama.tsx` valida o campo e mantém a renderização semântica. O CSS adota a linguagem de placa topológica e reduz o movimento de entrada.

**Tech Stack:** React 19, TypeScript 6, CSS, `react-markdown`, `yaml`.

## Global Constraints

- Responder e preservar conteúdo em português do Brasil e UTF-8.
- Não inserir HTML, SVG, JSX, classes CSS ou coordenadas nos arquivos Markdown.
- Manter `fluxo`, `camadas`, `comparacao` e `ciclo` compatíveis com conteúdo sem `visual`.
- Animar somente `transform`, `opacity` e `clip-path`, com duração máxima de 260 ms.
- Respeitar `prefers-reduced-motion` e layouts a partir de 320 px.
- Usar a paleta existente: tinta `#19172b`, papel `#ffffff`, violeta `#6541dc`, verde `#147a65`, âmbar `#ad601c` e linha `#dfdeea`.
- Tipografia existente: Bahnschrift para títulos, Segoe UI Variable Text para leitura e Cascadia Code para utilidade.

## Direção visual

**Assunto:** modelos mentais de infraestrutura para pessoas desenvolvedoras.

**Trabalho único da página:** tornar relações invisíveis — fronteiras, fluxo, camadas e reconciliação — compreensíveis antes do laboratório.

**Layout:** uma placa topológica silenciosa; identificação e miniatura no cabeçalho, relação semântica abaixo e uma nota curta no rodapé.

```text
┌─────────────────────────────────────────────────────────┐
│ TIPO              Título relacional       [miniatura]  │
├─────────────────────────────────────────────────────────┤
│  porta ───── nó principal ───── destino                 │
│                    └ · · ramo lateral                   │
├─────────────────────────────────────────────────────────┤
│ consequência que não aparece nos nós                    │
└─────────────────────────────────────────────────────────┘
```

**Assinatura:** miniaturas topológicas compostas por trilhos, planos e nós; cada uma representa o conceito real e usa o mesmo vocabulário visual do diagrama.

**Autocrítica:** a grade técnica poderia virar decoração “cyber”. Para evitar isso, ela fica quase imperceptível e a expressividade se concentra nas relações específicas de cada miniatura; não haverá glow, glassmorphism, ícones genéricos nem gradientes de destaque.

---

### Task 1: Catálogo de miniaturas semânticas

**Files:**
- Create: `frontend/src/IlustracaoDoDiagrama.tsx`
- Create: `frontend/src/visuaisDoDiagrama.ts`
- Modify: `frontend/src/Diagrama.tsx`
- Modify: `frontend/scripts-checar-conteudo.mjs`

**Interfaces:**
- Consumes: `visual?: string` lido do bloco YAML.
- Produces: `VisualDoDiagrama`, `visuaisDosDiagramas` e `IlustracaoDoDiagrama({ visual })`.

- [x] **Step 1: Declarar os 12 ids visuais no conteúdo e executar o verificador**

Run: `cd frontend && node scripts-checar-conteudo.mjs docker`

Expected: o verificador ainda não garante que o id pertence ao catálogo.

- [x] **Step 2: Criar o catálogo fechado e as 12 miniaturas SVG**

Implementar os ids `fronteiras-runtime`, `motor-docker`, `filesystem-camadas`,
`rotas-container`, `reconciliacao`, `arquitetura-cluster`, `hierarquia-workload`,
`service-endpoints`, `responsabilidade-aws`, `fronteiras-aws`, `planos-aws` e
`fidelidade-local`.

- [x] **Step 3: Validar `visual` no parser do frontend e no script local**

Run: `cd frontend && node scripts-checar-conteudo.mjs docker && node scripts-checar-conteudo.mjs kubernetes && node scripts-checar-conteudo.mjs aws`

Expected: quatro blocos e zero falhas em cada Trilha.

- [x] **Step 4: Rodar typecheck e lint**

Run: `cd frontend && npm run lint && npm run build`

Expected: ambos passam.

### Task 2: Placa topológica e movimento contido

**Files:**
- Modify: `frontend/src/Diagrama.tsx`
- Modify: `frontend/src/estilos.css`

**Interfaces:**
- Consumes: especificações existentes e a miniatura opcional.
- Produces: figuras semânticas responsivas com a mesma API pública `Diagrama({ fonte })`.

- [x] **Step 1: Reorganizar o cabeçalho para identificação e miniatura**

Usar `.diagrama-identificacao` e `.diagrama-ilustracao`; ocultar a miniatura apenas se o
conteúdo legado não declarar `visual`.

- [x] **Step 2: Reduzir superfícies decorativas**

Remover gradiente radial, sombra de elevação e raios excessivos; substituir cartões
internos por planos, divisórias e trilhos com tons usados como sinal.

- [x] **Step 3: Encurtar a entrada**

Usar `240ms var(--ease-out)` nos elementos, `260ms var(--ease-in-out)` nos conectores e
`40ms` de stagger. Em movimento reduzido, remover transformações e transições.

- [x] **Step 4: Validar desktop e mobile**

Run: `cd frontend && npm run build`

Expected: comparação vira uma coluna e ciclo vira vertical abaixo de 760 px, sem overflow.

### Task 3: Copy complementar nos 12 diagramas

**Files:**
- Modify: `content/docker/fundamentos.md`
- Modify: `content/kubernetes/fundamentos.md`
- Modify: `content/aws/fundamentos.md`

**Interfaces:**
- Consumes: catálogo de `visual` da Task 1.
- Produces: 12 blocos com títulos relacionais, detalhes curtos e legendas que acrescentam consequência.

- [x] **Step 1: Cortar redundâncias de Docker**

Trocar explicações como “o cliente — traduz sua intenção” por rótulos diretos como
“traduz o comando em chamada à API” e usar a legenda somente para a consequência.

- [x] **Step 2: Cortar redundâncias de Kubernetes**

Manter nomes técnicos nos nós e remover frases que repetem a definição imediatamente
anterior no artigo.

- [x] **Step 3: Cortar redundâncias de AWS**

Transformar títulos discursivos em relações: “Segurança da nuvem × na nuvem”,
“Conta → região → AZ → recurso”, “Da chamada ao comportamento” e “Fidelidade local”.

- [x] **Step 4: Verificar conteúdo**

Run: `cd frontend && node scripts-checar-conteudo.mjs docker && node scripts-checar-conteudo.mjs kubernetes && node scripts-checar-conteudo.mjs aws`

Expected: 12 diagramas válidos e todos os links `revisar` íntegros.

### Task 4: Documentação e validação final

**Files:**
- Modify: `README.md`
- Modify: `docs/adr/0004-diagramas-declarativos-no-conteudo.md`
- Modify: `docs/superpowers/plans/2026-08-08-revisao-visual-diagramas.md`

**Interfaces:**
- Consumes: contrato final do DSL.
- Produces: instrução de autoria com o campo `visual` e registro da decisão.

- [x] **Step 1: Documentar o catálogo fechado de miniaturas**

Explicar que `visual` escolhe uma imagem mantida no frontend e não aceita SVG no conteúdo.

- [x] **Step 2: Rodar a validação completa**

Run: `cd frontend && npm run lint && npm run build`

Run: `cd backend && .\mvnw.cmd test`

Expected: lint, build e 97 testes aprovados.

- [x] **Step 3: Auditar o diff e encoding**

Run: `git diff --check`

Expected: nenhuma falha de whitespace ou caractere corrompido.

- [x] **Step 4: Marcar este plano como concluído**

Trocar todos os checkboxes executados para `[x]` depois que as validações passarem.
