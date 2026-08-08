# Learning Infra

Plataforma pessoal de laboratórios práticos para aprender infraestrutura — Docker,
Kubernetes, AWS e infraestrutura como código — lendo e executando no mesmo lugar.
Usuário único: o autor.

## Language

### Conteúdo

**Trilha**:
Roteiro sequencial sobre uma mesma tecnologia, formado por seus Fundamentos e por
uma sequência de Cenários. Exemplos: Docker, Kubernetes, AWS, IaC.
_Avoid_: Módulo, curso, track

**Fundamentos**:
A abertura conceitual de uma Trilha. Reúne o texto que constrói o modelo mental e
o Questionário que confirma a compreensão; não prepara nem altera ambiente local.
_Avoid_: Artigo, aula teórica, introdução

**Questionário**:
A checagem formativa dos Fundamentos. Avalia decisões e conceitos, devolve feedback
para revisão e pode ser refeita; não substitui uma Verificação prática.
_Avoid_: Prova, teste teórico, quiz

**Cenário**:
A unidade prática de uma Trilha. Contém o texto didático e os exercícios
verificáveis no mesmo documento, prepara um ambiente local e carrega uma
Dificuldade.
_Avoid_: Aula, aula-tutorial, lição, lab, laboratório, exercício, cenário-exercício

**Dificuldade**:
O grau de autonomia que um Cenário exige. Quanto maior, menos o texto entrega e
mais o leitor descobre. Quatro degraus: `Guiado` (todo comando dado, explicado e
justificado), `Assistido` (objetivo mais dicas, comandos parciais), `Autônomo`
(só o objetivo e o ambiente), `Mestre` (ambiente quebrado, a causa não é revelada).
_Avoid_: Nível, complexidade

### Execução

**Cenário Ativo**:
O único Cenário cujo ambiente está no ar em um dado momento. Iniciar um Cenário
derruba o anterior — a invariante que impede uma Verificação de passar por sobra
de ambiente.
_Avoid_: Sessão, ambiente atual

**Verificação**:
A checagem automatizada que decide se um Cenário foi concluído, executada contra o
ambiente real da máquina do autor. Composta de Asserções.
_Avoid_: Validação, correção, teste

**Asserção**:
Uma afirmação isolada e nomeada sobre o estado do ambiente, extraída de um
vocabulário pequeno e fixo. Falha individualmente e com mensagem própria — é a
unidade de feedback ao leitor.
_Avoid_: Check, regra, condição
