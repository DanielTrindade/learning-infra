# Diagramas de conteúdo usam um DSL YAML declarativo

Os artigos de Fundamentos precisam explicar relações que ficam frágeis em prosa e
ilegíveis em diagramas ASCII: sequências, pilhas, comparações e ciclos. Decidimos que o
conteúdo continuará sendo Markdown e poderá declarar essas relações em blocos cercados
`diagrama`, cujo corpo é um DSL YAML pequeno e validado pelo frontend.

O contrato inicial oferece quatro tipos — `fluxo`, `camadas`, `comparacao` e `ciclo` —
e cinco tons semânticos — `neutro`, `destaque`, `sucesso`, `alerta` e `perigo`. O
componente `ConteudoMarkdown` reconhece o bloco, `Diagrama` valida cada campo e renderiza
HTML semântico e responsivo. YAML inválido ou um campo incompatível produz um quadro de
erro com a fonte para o autor, em vez de quebrar a página inteira.

O conteúdo declara significado, não coordenadas, SVG, classes CSS nem animações. Assim,
o mesmo arquivo continua revisável em texto e a interface preserva controle sobre
acessibilidade, movimento reduzido, responsividade e identidade visual.

Um campo opcional `visual` pode selecionar uma miniatura semântica de um catálogo
fechado do frontend. O id expressa o conceito — por exemplo, `reconciliacao` ou
`service-endpoints` — enquanto formas, coordenadas e cores continuam fora do conteúdo.
O parser no navegador e o verificador local rejeitam ids desconhecidos.

## Alternativas rejeitadas

- **Manter diagramas ASCII.** São úteis no terminal, mas quebram em telas estreitas,
  dependem de alinhamento monoespaçado e oferecem pouca hierarquia visual.
- **Permitir HTML, SVG ou JSX no Markdown.** Misturaria conteúdo e apresentação,
  ampliaria a superfície de execução e deixaria cada artigo responsável por
  responsividade e acessibilidade.
- **Criar um componente React por diagrama.** Entregaria controle visual, mas faria uma
  mudança curricular depender de código e build mesmo quando a relação cabe no contrato
  existente.
- **Adotar Mermaid como runtime.** Oferece uma linguagem muito maior do que a necessidade
  atual, adiciona peso e uma estética paralela. O domínio inicial precisa somente de
  quatro relações com o mesmo vocabulário visual do produto.

## Consequências

- `react-markdown` continua sendo a única porta de renderização do conteúdo; blocos de
  código comuns mantêm o comportamento anterior.
- O frontend passa a depender de `yaml` para interpretar o DSL no navegador e no
  verificador local.
- Novos tipos ou campos exigem evolução compatível do parser, da renderização, dos
  estilos, desta ADR e da documentação de autoria.
- Novas miniaturas exigem atualizar o catálogo tipado, a renderização SVG e o catálogo
  equivalente do verificador local; conteúdo sem `visual` permanece compatível.
- `frontend/scripts-checar-conteudo.mjs` valida todos os blocos e confirma que cada
  campo `revisar` do Questionário aponta para uma seção existente.
- Autores escrevem significado e texto alternativo implícito na estrutura sem controlar
  pixels; o frontend pode refinar o desenho sem reescrever os artigos.
