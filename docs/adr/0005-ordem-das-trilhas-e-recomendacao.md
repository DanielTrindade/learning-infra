# A ordem das Trilhas é recomendação, não pré-requisito

O catálogo cresceu para além do ponto em que a ordem podia ser acidente. Hoje
`CatalogoDeTrilhas` ordena por `Comparator.comparing(Trilha::id)`, e o resultado é
alfabético: `aws, docker, iac, kubernetes`. A Trilha AWS abre o catálogo e a IaC, que é
a última do desenho e depende do cluster e do MiniStack das outras, aparece em terceiro.
Com a Trilha Linux o erro fica evidente, porque `linux` é o último id em ordem
alfabética e ela é justamente a que deve abrir o catálogo.

Decidimos que cada Trilha declara sua posição no `trilha.yaml`, em um campo `ordem`
obrigatório, e que essa posição afeta **somente apresentação e recomendação**. Nenhuma
Trilha é bloqueada, escondida ou marcada como indisponível por causa de outra.

## Alternativas rejeitadas

- **Ordem como pré-requisito avaliado pelo backend.** Um campo `requer:` que trava a
  Trilha seguinte até a anterior ser concluída. Rejeitada porque contradiz a decisão que
  a [ADR 0003](0003-fundamentos-pertencem-a-trilha.md) já tomou uma camada abaixo: o
  Questionário "recomenda revisão e registra compreensão, mas não bloqueia o início dos
  Cenários". Seria incoerente a plataforma não travar dentro de uma Trilha e travar
  entre elas. O usuário é único, é o autor, e sabe o que quer estudar.
- **Continuar ordenando por id.** É o estado atual, e ele já está errado antes mesmo de
  qualquer Trilha nova entrar.
- **Ordenar por data de criação do diretório.** Reflete a ordem em que o autor
  construiu, que não é a ordem em que se aprende — e depende do sistema de arquivos.
- **Deixar `ordem` opcional com um padrão.** Uma Trilha sem posição declarada volta a
  ser ordenada por acidente, que é exatamente o problema. O projeto já rejeita um
  diretório com Cenários e sem `trilha.yaml` pelo mesmo motivo.

## Consequências

- `Trilha` ganha o campo, `LeitorDeTrilha` passa a exigi-lo, e o comparator vira
  `Comparator.comparingInt(Trilha::ordem).thenComparing(Trilha::id)`. O desempate por id
  mantém o catálogo determinístico se duas Trilhas declararem o mesmo número, em vez de
  deixar a ordem depender do sistema de arquivos.
- As quatro `trilha.yaml` existentes precisam declarar `ordem` no mesmo commit em que o
  campo vira obrigatório, senão o catálogo inteiro deixa de carregar.
- O frontend não muda: `Catalogo.tsx` renderiza na ordem em que a API devolve e não
  reordena nada.
- **Nada de `requer:`, `preRequisitos:` ou equivalente no `trilha.yaml`.** Se um dia for
  útil sinalizar dependência entre Trilhas, que seja texto exibido ao leitor, nunca uma
  condição avaliada pelo backend. Uma Trilha nunca fica cinza, travada ou escondida.
- A ordem é conteúdo, não código: mudar a sequência recomendada é editar manifestos, sem
  recompilar nada.
- Uma Trilha ainda não escrita simplesmente não aparece. Como a ordem não trava nada, um
  buraco temporário na sequência não impede ninguém de seguir.
