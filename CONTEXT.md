# Learning Infra

Plataforma pessoal de laboratórios práticos para aprender infraestrutura — Docker,
Kubernetes, AWS e infraestrutura como código — lendo e executando no mesmo lugar.
Usuário único: o autor.

## Language

### Conteúdo

**Trilha**:
Agrupamento sequencial de Cenários sobre uma mesma tecnologia. Exemplos: Docker,
Kubernetes, AWS, IaC.
_Avoid_: Módulo, curso, track

**Cenário**:
A unidade atômica de conteúdo e a única coisa que aparece no catálogo. Contém o
texto didático e os exercícios verificáveis no mesmo documento, e carrega uma
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
