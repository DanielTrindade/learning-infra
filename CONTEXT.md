# Learning Infra

Plataforma pessoal de laboratórios práticos para aprender infraestrutura — Docker,
Kubernetes, AWS e infraestrutura como código — lendo e executando no mesmo lugar.
Usuário único: o autor.

## Language

> Código, API e nomes de tipos usam os termos em inglês. O conteúdo didático, o schema
> dos arquivos de `content/` e as mensagens exibidas ao leitor permanecem em português.
> Este glossário é a ponte entre os dois.

### Content

**Track** (Trilha):
Roteiro sequencial sobre uma mesma tecnologia, formado por seus Fundamentals e por
uma sequência de Scenarios. Exemplos: Docker, Kubernetes, AWS, IaC.
_Avoid_: Module, course

**Fundamentals** (Fundamentos):
A abertura conceitual de uma Track. Reúne o texto que constrói o modelo mental e o
Questionnaire que confirma a compreensão; não prepara nem altera ambiente local.
_Avoid_: Article, lecture, introduction

**Questionnaire** (Questionário):
A checagem formativa dos Fundamentals. Avalia decisões e conceitos, devolve feedback
para revisão e pode ser refeita; não substitui uma Verificação prática.
_Avoid_: Exam, quiz

**Scenario** (Cenário):
A unidade prática de uma Track. Contém o texto didático e os exercícios verificáveis
no mesmo documento, prepara um ambiente local e carrega uma Difficulty.
_Avoid_: Lesson, lab, exercise

**Difficulty** (Dificuldade):
O grau de autonomia que um Scenario exige. Quanto maior, menos o texto entrega e mais
o leitor descobre. Quatro degraus: `GUIDED` (todo comando dado, explicado e
justificado), `ASSISTED` (objetivo mais dicas, comandos parciais), `AUTONOMOUS` (só o
objetivo e o ambiente), `MASTER` (ambiente quebrado, a causa não é revelada).
_Avoid_: Level, complexity

### Execution

**Active Scenario** (Cenário Ativo):
O único Scenario cujo ambiente está no ar em um dado momento. Iniciar um Scenario
derruba o anterior — a invariante que impede uma Verification de passar por sobra de
ambiente.
_Avoid_: Session

**Verification** (Verificação):
A checagem automatizada que decide se um Scenario foi concluído, executada contra o
ambiente real da máquina do autor. Composta de Assertions.
_Avoid_: Validation, test

**Assertion** (Asserção):
Uma afirmação isolada e nomeada sobre o estado do ambiente, extraída de um vocabulário
pequeno e fixo. Falha individualmente e com mensagem própria — é a unidade de feedback
ao leitor.
_Avoid_: Check, rule, condition
