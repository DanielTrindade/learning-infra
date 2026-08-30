# Separação da Landing e Área de Aprendizagem Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separar a apresentação pública da Learning Infra da experiência de estudo, retirar os principais sinais de UI gerada por IA e corrigir o fluxo responsivo.

**Architecture:** A raiz `#/` será uma landing sem estado pessoal. A nova rota `#/aprender` hospedará o catálogo e o progresso existentes, recompostos como área de aprendizagem. Fundamentos e Cenários mantêm seus slugs e passam a retornar para `#/aprender`. O design continuará em React + CSS nativo para evitar a mistura de sistemas.

**Tech Stack:** React 19, TypeScript 6, Vite 8, CSS nativo, API Spring existente e imagem raster gerada para o hero.

## Global Constraints

- Responder e escrever copy em português do Brasil com UTF-8.
- Preservar logo, nome da marca, rotas de Fundamentos e Cenários e contratos da API.
- Usar teal como único acento de marca. Cores de sucesso, atenção e erro são semânticas.
- Não usar em dash ou en dash na copy visível nova.
- Não usar malha global, glow neon, gradiente roxo, hero centralizado, três cards iguais ou mock de dashboard.
- Manter tema claro/escuro, foco visível, skip link e reduced motion.
- A landing deve caber sem overflow em 390 px e a navegação deve ficar em uma linha no desktop.

---

### Task 1: Separar rotas e cabeçalhos

**Files:**
- Create: `frontend/src/CabecalhoDaPlataforma.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/PaginaDeFundamentos.tsx`
- Modify: `frontend/src/PaginaDoCenario.tsx`
- Modify: `frontend/src/ChecklistDeVerificacao.tsx`

**Interfaces:**
- Consumes: `useRota(): string`, `BotaoDeTema`, `MarcaDaPlataforma`.
- Produces: `CabecalhoDaPlataforma({ contexto }: { contexto: 'landing' | 'aprendizado' | 'conteudo' })` e a rota `#/aprender`.

- [x] **Step 1: Criar o cabeçalho contextual**

```tsx
export function CabecalhoDaPlataforma({ contexto }: Props) {
  return contexto === 'landing'
    ? <header>{/* marca, Como funciona, Trilhas, Minha aprendizagem */}</header>
    : <header>{/* marca, Minha aprendizagem, ambiente local, tema */}</header>
}
```

- [x] **Step 2: Atualizar a decisão de rota**

```tsx
const naLanding = rota === ''
const naAreaDeAprendizado = rota === 'aprender'
```

- [x] **Step 3: Apontar retornos de conteúdo para `#/aprender`**

Substituir apenas links de retorno ao catálogo. Manter links profundos de Fundamentos e Cenários.

- [x] **Step 4: Rodar a verificação de tipos**

Run: `cd frontend && npm run build`

Expected: TypeScript e Vite concluem sem erro.

### Task 2: Criar uma landing pública com mídia real

**Files:**
- Create: `frontend/src/LandingPage.tsx`
- Create: `frontend/public/learning-infra-lab-hero.png`
- Modify: `frontend/src/estilos.css`

**Interfaces:**
- Consumes: `listarTrilhas(): Promise<TrilhaResumo[]>`.
- Produces: landing com hero, metodologia, trilhas e CTA final, incluindo loading, vazio e erro para a prévia de trilhas.

- [x] **Step 1: Copiar a imagem aprovada para `public`**

A imagem deve ter dimensão reservada no markup e `fetchPriority="high"` para evitar CLS e reduzir LCP.

- [x] **Step 2: Implementar o hero assimétrico**

```tsx
<section className="landing-hero">
  <div className="landing-hero-copy">
    <h1>Aprenda infraestrutura resolvendo problemas reais.</h1>
    <p>Laboratórios locais, fundamentos claros e verificação prática em uma trilha contínua.</p>
    <a href="#/aprender">Começar agora</a>
  </div>
  <img src="/learning-infra-lab-hero.png" alt="Pequeno laboratório local com mini servidor, cabos e notebook" />
</section>
```

- [x] **Step 3: Implementar metodologia sem três cards iguais**

Usar uma composição assimétrica com um bloco principal e dois blocos auxiliares. Os títulos serão “Entenda”, “Pratique” e “Verifique”.

- [x] **Step 4: Mostrar trilhas com dados reais**

Exibir nome e quantidade de conteúdos em uma lista editorial de até quatro itens. Não mostrar progresso pessoal na landing.

- [x] **Step 5: Implementar estados**

Loading usa skeleton com a mesma forma das linhas finais. Erro oferece acesso direto à área de aprendizagem. Vazio explica que o catálogo ainda não foi publicado.

### Task 3: Reorganizar a área de aprendizagem

**Files:**
- Modify: `frontend/src/Catalogo.tsx`
- Modify: `frontend/src/estilos.css`

**Interfaces:**
- Consumes: progresso local, trilhas da API e contratos existentes de adesão, restauração e arquivamento.
- Produces: cabeçalho “Minha aprendizagem”, bloco de continuidade e seção “Explorar trilhas”.

- [x] **Step 1: Retirar o hero de marketing do catálogo**

Remover `IlustracaoDoHero` e toda copy de venda do componente `Catalogo`.

- [x] **Step 2: Tornar a trilha atual a primeira ação**

O bloco principal deve mostrar nome, progresso e um CTA único para Fundamentos ou próximo Cenário.

- [x] **Step 3: Reagrupar filtros e trilhas disponíveis**

Os filtros permanecem como pills de produto. A lista usa linhas compactas e expande somente a trilha atual ou uma trilha escolhida pelo aluno.

- [x] **Step 4: Preservar estados completos**

Manter carregamento, erro, vazio, filtro vazio, trilhas concluídas e restauração.

### Task 4: Recalibrar tokens, fundo e responsividade

**Files:**
- Modify: `frontend/src/estilos.css`
- Modify: `frontend/index.html`

**Interfaces:**
- Produces: tokens de cor e forma coerentes nos dois temas, landing responsiva e área de conteúdo sem overflow.

- [x] **Step 1: Remover a malha global**

```css
body {
  background: var(--canvas);
}
```

- [x] **Step 2: Consolidar tipografia e forma**

Usar sans de sistema para texto e display, mono apenas em código e metadados técnicos. Controles usam 10 px, superfícies usam 16-18 px e pills ficam restritas a filtros.

- [x] **Step 3: Corrigir 960, 760 e 430 px**

Landing vira coluna única abaixo de 760 px. Cabeçalhos, metadados, filtros e CTAs podem quebrar ou rolar somente dentro do próprio componente, nunca no documento.

- [x] **Step 4: Atualizar metadata sem mudar o posicionamento da marca**

Manter title e description em português, acrescentando OG básico apenas se houver asset adequado.

### Task 5: Verificar experiência e pre-flight

**Files:**
- Verify: `frontend/src/**/*.tsx`
- Verify: `frontend/src/estilos.css`

- [x] **Step 1: Rodar lint**

Run: `cd frontend && npm run lint`

Expected: zero erros.

- [x] **Step 2: Rodar build**

Run: `cd frontend && npm run build`

Expected: zero erros e assets gerados em `dist`.

- [x] **Step 3: Auditar copy visível**

Run: `rg -n "—|–" frontend/src frontend/index.html`

Expected: nenhuma ocorrência introduzida nas superfícies refatoradas.

- [x] **Step 4: Auditar padrões proibidos**

Run: `rg -n "radial-gradient|h-screen|window\.addEventListener\(['\"]scroll|uppercase" frontend/src`

Expected: nenhuma malha/glow na landing, nenhum `h-screen`, nenhum listener manual de scroll e labels em caixa alta dentro do limite.

- [x] **Step 5: Capturar e revisar visualmente**

Verificar `1440x1200`, `390x844`, tema claro e tema escuro. Confirmar CTA visível, zero overflow horizontal, foco visível e hierarquia equivalente.

Resultado: claro, escuro e 390 px revisados. O documento manteve `scrollWidth` igual ao viewport em 390 px. A tentativa de Lighthouse via `npx` travou antes de iniciar o relatório e foi encerrada sem deixar processos órfãos. Como verificação complementar, o Chrome DevTools Protocol mediu FCP de 332 ms no servidor local, 53 KB transferidos após cache, zero controles sem nome e zero imagens sem `alt`.

- [ ] **Step 6: Commit sugerido**

```bash
git add frontend docs/design docs/superpowers/plans
git commit -m "refactor: separa landing da area de aprendizado"
```
