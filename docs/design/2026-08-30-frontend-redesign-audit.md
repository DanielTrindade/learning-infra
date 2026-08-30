# Auditoria de UI/UX do frontend

Data: 30 de agosto de 2026

## Leitura de design

Redesign estrutural de uma plataforma de cursos para alunos e visitantes, preservando a marca e o conteúdo útil. A nova linguagem deve ser limpa, confiante e editorial-tech, com separação clara entre marketing e aprendizagem.

- `DESIGN_VARIANCE: 7`
- `MOTION_INTENSITY: 4`
- `VISUAL_DENSITY: 4`
- Modo: redesign com preservação da marca e reorganização da experiência
- Fundação: React + CSS nativo existente, sem introduzir outro design system

## Estado atual

### Tokens de marca

- Claro: canvas `#f4f6f7`, superfície `#ffffff`, acento `#0d6c80`.
- Escuro: canvas `#0c0f11`, superfície `#13181b`, acento `#45bcd6`.
- Tipografia: Segoe UI no texto, Bahnschrift no display e Cascadia no mono.
- Formas: raios entre 4 e 16 px, além de pills.
- Materialidade: bordas finas, sombras leves, superfícies translúcidas e muitos containers elevados.

### Arquitetura da informação

- `#/` mistura apresentação da proposta, progresso pessoal, escolha de trilha, filtros, catálogo completo e arquivo de trilhas concluídas.
- `#/trilhas/:id/fundamentos` entrega a etapa conceitual.
- `#/cenarios/:id` entrega o laboratório prático.
- A marca sempre retorna para `#/`, por isso visitante e aluno chegam à mesma superfície.
- Não existe navegação própria para marketing nem um ponto de entrada nomeado para a área de aprendizagem.

### Conteúdo que já funciona

- A frase central da proposta é concreta e alinhada ao produto.
- A progressão Fundamentos, Cenários e verificação diferencia a plataforma.
- A trilha atual e o CTA de continuidade dão um próximo passo útil.
- Estados de carregamento, vazio e erro já existem.
- Tema claro/escuro, skip link, foco visível e reduced motion já estão previstos.

## Padrões a preservar

- Símbolo e nome Learning Infra.
- Teal como cor de marca.
- Posicionamento de laboratórios executados no ambiente local.
- Progresso salvo na máquina e uma trilha atual por vez.
- Rotas dos Fundamentos e Cenários.
- Voz direta, técnica e em português do Brasil.
- Estados semânticos verde, âmbar e vermelho.

## Padrões a retirar

- Malha quadriculada aplicada ao site inteiro.
- Glow radial no hero.
- Ilustração isométrica feita à mão como mídia principal.
- Hero e dashboard pessoal na mesma página.
- Cards dentro de cards e bordas em quase todos os blocos.
- Excesso de labels mono em caixa alta e pontos decorativos.
- Spinner circular genérico na tela inicial.
- Catálogo longo como primeira experiência de um visitante.

## Problemas de fluxo

1. Um visitante vê dados pessoais e um catálogo denso antes de entender como a metodologia funciona.
2. Um aluno recorrente precisa atravessar o hero de marketing para continuar o estudo.
3. “Escolher uma trilha”, “trocar trilha”, “aderir” e “continuar” competem na mesma superfície.
4. O catálogo mostra até 75 cenários em uma estrutura vertical extensa.
5. No mobile, filtros, metadados, CTAs e o cabeçalho de Fundamentos causam overflow horizontal.

## Fluxo proposto

### Visitante

`Landing` -> entende a proposta -> conhece a metodologia -> vê as trilhas -> entra em `Minha aprendizagem`.

### Aluno

`Minha aprendizagem` -> continua a trilha atual -> abre Fundamentos ou próximo Cenário -> verifica o laboratório -> segue para o próximo conteúdo.

### Exploração

`Minha aprendizagem` -> explora trilhas disponíveis -> adere ou retoma -> a nova trilha vira o foco principal sem apagar progresso anterior.

## Direção visual

- Fundo sólido em cinza mineral no claro e grafite no escuro.
- Uma única lavagem tonal muito sutil apenas no hero, sem mesh, glow ou grade global.
- Teal preservado como acento de ação. Verde, âmbar e vermelho aparecem somente como estados.
- Superfícies de landing mais editoriais, com menos caixas e mais espaço negativo.
- Área de aprendizagem mais compacta, com hierarquia de produto e CTAs orientados ao próximo passo.
- Fotografia editorial real no hero, mostrando um pequeno laboratório local e materiais de estudo.
- Raios consistentes: controles de 10 px, superfícies de 16-18 px e pills apenas para filtros.
- Movimento limitado a entrada, hover, expansão e feedback de estado, sempre respeitando reduced motion.

## SEO e contratos

- O baseline atual possui `title`, description e favicon, mas não possui OG, canonical ou dados estruturados.
- A aplicação usa hash routing, portanto as páginas internas não têm URLs tradicionais indexáveis.
- `#/` passará a ser uma landing real, melhorando a mensagem de entrada sem remover as rotas de conteúdo.
- Eventos analíticos não foram encontrados no frontend.
- Logo, conteúdo legal e campos de formulário não serão alterados.

## Critérios de sucesso

- Landing e área de aprendizagem têm objetivos e navegação distintos.
- O aluno chega ao próximo conteúdo em uma ação.
- O visitante não vê progresso pessoal misturado ao marketing.
- Não há malha global, glow de IA, hero centralizado ou três cards iguais.
- Não há overflow em 390 px.
- Claro e escuro mantêm contraste e hierarquia equivalentes.
- Build e lint continuam passando.

