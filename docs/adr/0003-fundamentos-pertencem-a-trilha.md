# Fundamentos pertencem à Trilha, não são um Cenário

Cada Trilha terá uma abertura conceitual chamada Fundamentos, composta pelo texto e
por um Questionário formativo, seguida de seus Cenários práticos. Decidimos modelar
Fundamentos e Cenário como conteúdos distintos porque somente o Cenário prepara um
ambiente e pode ser concluído por Verificações contra o estado real da máquina. O
Questionário recomenda revisão e registra compreensão, mas não bloqueia o início dos
Cenários.

## Alternativas rejeitadas

- **Criar um “Cenário 00” teórico.** Exigiria uma Verificação artificial, misturaria
  compreensão com estado da máquina e enfraqueceria o significado de Cenário.
- **Colocar toda a teoria no primeiro Cenário.** Acoplaria a abertura conceitual ao
  lifecycle de um ambiente e dificultaria aplicar a mesma experiência a todas as
  Trilhas.
- **Criar um curso teórico separado da Trilha.** Separaria conteúdo que existe para
  preparar exatamente a prática daquela tecnologia e duplicaria navegação e progresso.

## Consequências

- A Trilha passa a ser conteúdo explícito, em vez de ser apenas inferida do prefixo dos
  ids dos Cenários.
- Fundamentos contam no progresso quando estiverem disponíveis, mas não alteram o
  Cenário Ativo.
- O contrato será único para todas as Trilhas. Docker será a primeira implementação de
  conteúdo; Kubernetes, AWS e futuras Trilhas adotarão a mesma estrutura depois.
- Durante a migração, uma Trilha pode existir sem Fundamentos publicados e continuar
  funcionando somente com seus Cenários.
