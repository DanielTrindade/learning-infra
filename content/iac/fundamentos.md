# Fundamentos de Infraestrutura como Código

Docker, Kubernetes e AWS foram construídos à mão — comando a comando, por uma pessoa que
lembrava de cada passo. Esta Trilha reconstrói o mesmo mundo de outra forma: em vez de
executar passos, você declara um destino; em vez de uma memória que sai da empresa, um
arquivo versionado que conta a história. Antes de decorar sintaxe, vale construir o modelo
mental que a Trilha inteira cobra: o que separa descrever de executar, quem guarda a
memória do que foi criado e o que acontece quando as verdades do sistema divergem.

Esta leitura prepara os Cenários práticos. Ela não substitui o terminal: ao terminar,
faça o Questionário para descobrir o que revisar e siga para os laboratórios mesmo que
ainda não tenha atingido 80%. A recomendação não é um bloqueio.

## O problema: a infra que só existe na memória

A Mirante começou como uma pessoa e um container subido à mão num sábado. O serviço
funciona, o estoque responde, o cliente não desconfia. Até o dia em que alguém precisa
reproduzir aquele ambiente — numa máquina nova, num incidente, numa auditoria — e
descobre que ninguém sabe como ele foi montado.

O custo não é falta de capacidade técnica: é falta de registro executável. Documentação
em wiki não resolve, porque ela não é executável e não tem como ser conferida contra a
realidade — um passo desatualizado não falha, apenas engana. O problema se manifesta em
três sintomas que a Trilha inteira ataca: a infra **não é reprodutível** (ninguém
consegue refazê-la), **não é auditável** (ninguém sabe o que mudou, quando e por quem) e
**não é transferível** (o conhecimento sai junto com quem a subiu).

## Imperativo e declarativo

Um script que roda `docker run` descreve o caminho: executa a intenção na ordem em que
foi escrita. Na segunda execução, o script falha — o container já existe, e o caminho que
funcionou ontem não vale hoje. O script não pergunta o que já está de pé; ele apenas
repete passos.

Um arquivo `.tf` descreve o destino: o container com aquela imagem, aquelas portas,
aquela rede. Ao rodar, o Terraform compara o destino com o que já existe; se o container
já está do jeito descrito, ele não faz nada. Nenhuma instrução para repetir, nenhum passo
para dar duas vezes. O modo declarativo compra exatamente isso: rodar de novo sem medo.

## O ciclo: código, plan, apply, state

O ciclo tem quatro peças e a ordem entre elas. O **código** declara o destino. O **plan**
é leitura: lê o código, o state e o mundo real, e devolve a diferença — o que vai nascer,
o que vai mudar, o que vai morrer. O **apply** é a única escrita: é a etapa que muda o
mundo. E o **state** é o que o Terraform sabe depois; é dele que sai a resposta para "o
que eu criei?".

```diagrama
tipo: ciclo
visual: ciclo-iac
titulo: Código → plan → apply → state
passos:
  - titulo: código
    detalhe: o destino que você descreve
  - titulo: plan
    detalhe: a diferença entre o destino e o que o state conhece
  - titulo: apply
    detalhe: a única etapa que muda o mundo
  - titulo: state
    detalhe: o que o Terraform passa a saber
retorno: o próximo plan parte do state
```

Perder o state não apaga a infraestrutura — o container continua no ar. O que se perde é
a **correspondência** entre o código e a infraestrutura: o Terraform deixa de saber que
aquele container do mundo real é o `docker_container.web` declarado. Recuperar essa
correspondência é `import`, assunto do Cenário 07.

## O triângulo: código, state e mundo real

Três coisas podem divergir duas a duas: o **código**, o **state** e o **mundo real**.
Esta é a seção mais importante do artigo, porque o modelo mental que ela constrói sustenta
os dezoito Cenários — e cada Cenário exercita um par desses três fora de sincronia.

```diagrama
tipo: comparacao
visual: triangulo-state
titulo: Três coisas que divergem duas a duas
colunas:
  - titulo: código
    detalhe: o que você declarou
    itens:
      - versionado no Git
      - a única fonte de intenção
      - mudá-lo não muda nada sozinho
  - titulo: state
    detalhe: o que o Terraform sabe
    tom: destaque
    itens:
      - correspondência entre endereço e recurso
      - guarda valores em claro
      - fica desatualizado em silêncio
  - titulo: mundo real
    detalhe: o que existe de fato
    tom: alerta
    itens:
      - muda sem pedir licença
      - console, colega, incidente
      - é a única realidade que atende usuário
```

O código é a única fonte de intenção e mudá-lo não muda nada sozinho: é preciso um
`apply` para que a intenção nova chegue ao mundo. O state guarda a correspondência entre
endereço e recurso e fica desatualizado em silêncio. O mundo real muda sem pedir licença
— console, colega, incidente — e é a única realidade que atende usuário. Cada divergência
tem nome de mercado e um Cenário que a exercita:

| Divergência | Nome de mercado | Onde você vai encontrar |
|---|---|---|
| mundo ≠ state | drift | Cenários 05 e 14 |
| mundo existe, state não | brownfield, infra órfã | Cenário 07 |
| state existe, mundo não | recurso apagado por fora | Cenários 05 e 18 |
| código ≠ state para o mesmo recurso | refactor | Cenário 09 |

Quase todo assunto difícil de IaC é um par desses três fora de sincronia.

## Reconciliação, de novo

O laço dos Fundamentos de Kubernetes volta aqui — observar, comparar, agir — com uma
diferença de cadência que muda tudo:

```diagrama
tipo: ciclo
visual: reconciliacao
titulo: O mesmo laço, cadências diferentes
passos:
  - titulo: observar
    detalhe: o Terraform lê o mundo durante o plan
  - titulo: comparar
    detalhe: código contra state contra mundo
  - titulo: agir
    detalhe: só no apply, e só quando mandam
retorno: nada acontece até o próximo comando
```

No Kubernetes, o controller reconcilia sozinho e para sempre: entre uma observação e
outra, o cluster trabalha continuamente para puxar o mundo de volta ao desejado. No
Terraform, a reconciliação acontece quando mandam — no comando. Entre um apply e o
próximo, ninguém corrige nada. É isso que explica por que drift é um problema central de
IaC e quase não é de Kubernetes: no cluster, o desvio é corrigido no mesmo instante; no
Terraform, ele dorme até o próximo plan acusar.

## O grafo de dependências

A ordem é **derivada de referência**, não escrita. No imperativo, a ordem é a ordem das
linhas: crie a imagem, depois a rede, depois o container. No declarativo, ninguém escreve
essa sequência — o Terraform a deriva das referências entre recursos.

```diagrama
tipo: fluxo
visual: grafo-dependencias
titulo: Ordem que ninguém escreveu
passos:
  - titulo: docker_image.web
    detalhe: nenhuma referência a outro recurso
  - titulo: docker_network.interna
    detalhe: independente da imagem — o Terraform cria as duas em paralelo
  - titulo: docker_container.web
    detalhe: referencia imagem e rede, então espera as duas
    tom: destaque
  - titulo: depends_on
    detalhe: a ordem que nenhuma referência revela
    tom: alerta
```

`image = docker_image.web.image_id` não é apenas reuso de um valor: é uma declaração de
dependência disfarçada. O **`depends_on`** existe para a dependência que não aparece em
nenhuma referência — uma ordem que o grafo não consegue descobrir — e é a exceção, não a
regra. Onde uma referência bastaria, usá-la é mais honesto: o grafo vê, o plano respeita
e o leitor não precisa confiar em memória.

## Provider como tradutor

Você vai escrever o mesmo HCL contra três substratos: o Docker da sua máquina, o cluster
local da Trilha Kubernetes e o MiniStack da Trilha AWS. Quem transforma recurso declarado
em chamada de API é o **provider** — o tradutor entre a sua intenção e o mundo. Trocar de
substrato não muda o que você escreve; muda o tradutor, e com ele a fidelidade:

```diagrama
tipo: camadas
visual: fidelidade-local
titulo: O mesmo HCL, três fidelidades
camadas:
  - titulo: provider docker
    detalhe: container, imagem, rede e volume reais — fidelidade total
    tom: sucesso
  - titulo: provider kubernetes
    detalhe: objetos reais num cluster local — não prova HA nem multi-tenancy
    tom: destaque
  - titulo: provider aws contra o MiniStack
    detalhe: control plane real, data plane parcial — nada sobre IAM, custo ou rede
    tom: alerta
```

E a constraint de versão do provider é parte do código: sem `required_providers` pinado,
a máquina do colega pode receber outra versão do tradutor e ver outro plano — a mesma
configuração, dois comportamentos. Pinada, toda máquina conversa com o mesmo mundo.

## Módulos e composição

Um **módulo** é uma caixa com entrada, saída e um corpo: variáveis na entrada, `outputs`
na saída, recursos no corpo. A razão de existir é não repetir. Copiar-colar um ambiente é
dívida que se paga na próxima mudança, multiplicada pelo número de cópias: corrigir um
problema em seis ambientes colados é seis correções idênticas, cada uma com a própria
chance de errar.

Uma armadilha medida na pesquisa: **um módulo filho declara o próprio
`required_providers`**. Se o módulo não declara, o `init` procura `hashicorp/docker` — o
provider padrão — e falha, mesmo com o módulo pai declarando a versão certa. O tradutor é
declarado por módulo, não herdado.

## Idempotência e drift

**Idempotência** é a propriedade de aplicar duas vezes não fazer duas coisas: o segundo
`apply` não encontra diferença e o Terraform não faz nada. É o que permite rodar o mesmo
código toda semana, todo dia, sem acumular efeitos.

**Drift** é o mundo mudando sem passar pelo código — alguém editou no console, um colega
parou o container à mão, um incidente deixou o recurso em outro estado. O `plan` é o
detector: compara o mundo real com o que o código pede e mostra a diferença. O Cenário 05
mede isso: um `docker stop` feito à mão já produz plano divergente.

## Blast radius: ler o plano

O plano é o contrato entre você e o mundo: tudo o que o `apply` vai fazer precisa caber
ali antes de acontecer. A leitura começa pelos quatro símbolos: `+` cria, `~` atualiza em
lugar, `-` destrói e `-/+` substitui — e substituir não é "atualizar de um jeito
melhor", é destruir e criar.

A marca mais cara do plano é **`forces replacement`**. Quando ela aparece embaixo do
recurso do banco de dados, uma mudança pequena no código vira um destroy seguido de um
create — e dados que não estão no código morrem no meio. É por isso que o plano é lido
antes de aprovado, e não depois.

O bloco **`moved`** é a ferramenta de renomear sem destruir. Para o Terraform, o endereço
é a identidade: renomear sem `moved` parece recurso sumido e recurso novo — um destroy
embaixo dos seus dados. O `moved` reescreve o state e não encosta no recurso.

**`prevent_destroy`** é o cinto de segurança, com uma sutileza que vale registrar: ele
protege **um endereço, não um recurso**. Declará-lo no endereço novo não salva o endereço
antigo — o cinto está preso ao endereço, e o Terraform não reconhece que o novo é o mesmo
recurso.

E **`-target`** é a ferramenta de emergência. Ela estreita o plano a um caminho e, por
isso mesmo, mente sobre o resto: mudanças pendentes fora do alvo ficam invisíveis até um
`apply` completo. Serve para apagar incêndio, não para o dia a dia.

## Segredos e o state

O state guarda tudo o que o Terraform precisa saber — inclusive os valores que entraram
como variável, **em claro**. `sensitive = true` esconde um valor da **saída do
terminal**, não do arquivo de state: protege a tela, não o disco. Confundir os dois é
imaginar que marcar `sensitive` guarda a senha; ela continua gravada como recebeu.

A consequência prática é a que o Cenário 06 cobra: o state local vai para o `.gitignore`,
porque commit com state é senha no histórico. E quando o time cresce, state remoto com
acesso restrito deixa de ser luxo — a memória da infraestrutura vira um ativo com dono e
com lista de quem pode lê-lo.

## Infra que se testa

A infra declarada se testa, e o teste tem quatro degraus, do mais barato ao mais caro.
**`fmt -check`** confere formatação: barato, raso, e evita diff de convenção.
**`validate`** confere sintaxe e semântica do HCL — e não fala com provider nenhum: uma
configuração válida pode apontar para um recurso que não existe.

O terceiro degrau é o mais próximo do comportamento. **`precondition`** e
**`postcondition`** verificam hipóteses durante o plano — um `precondition` que falha
impede o apply. Já **`check`** avisa sem reprovar o apply: a regra quebrou, o recurso
nasceu assim mesmo, e o aviso fica registrado no estado.

O degrau mais caro é o **`terraform test`**: `command = plan` valida sem criar nada, e
`command = apply` cria e destrói infraestrutura de verdade. É a única forma de provar que
um recurso nasce de fato do jeito descrito — e o preço é mexer no mundo real para provar.

## O mapa do território

O Terraform é o que esta Trilha ensina, mas o território é maior. O **OpenTofu** é o fork
nascido da mudança de licença do Terraform para BUSL em 2023; ficou sob a Linux
Foundation, fala HCL idêntico e roda o mesmo código. A Trilha usa Terraform por uma razão
prática: é o comando que aparece nas vagas.

O **CloudFormation**, que você já exercitou nos Cenários 06, 17 e 18 da Trilha AWS, entra
como comparação: a mesma ideia, presa a um provedor só e com o estado gerenciado pelo
serviço. **Pulumi** e **CDK** são a mesma ideia com outra sintaxe — linguagens de
programação no lugar de HCL. E **Ansible** é categoria diferente: configurar uma máquina
que já existe não é criá-la. Provisionar é do Terraform; configurar é de outra família de
ferramentas.

## O ambiente desta Trilha

Tudo começa com o binário: **Terraform 1.15.8** pinado. Os três providers acompanham —
`kreuzwerker/docker` ~> 4.5, `hashicorp/kubernetes` ~> 3.2 e `hashicorp/aws` ~> 6.58 —
sempre com a constraint no código. As aplicações da Mirante vivem no bloco de portas
**8070–8079** da sua máquina, uma por serviço, para que nenhuma Verificação confunda um
recurso com outro.

E o aviso de fidelidade que a Trilha AWS já estabeleceu vale aqui: nenhuma Asserção desta
Trilha alega isolamento de rede, IAM aplicado, custo ou disponibilidade. O emulador prova
que o código descreve os recursos certos; não prova o que a nuvem real faria com eles.

## Da teoria aos Cenários

Os dezoito Cenários desta Trilha caminham da primeira linha de HCL ao diagnóstico de uma
Mirante em escala, organizados em quatro atos:

- **Ato I — Do imperativo ao declarativo (Cenários 01 a 05):** a Mirante nasce na mão e
  aprende a ser descrita — provider, ciclo, plano, grafo e drift, tudo sobre Docker;
- **Ato II — O código sobrevive às pessoas (Cenários 06 a 09):** state, import, módulos e
  o rename que não pode derrubar o banco;
- **Ato III — Confiança (Cenários 10 a 12):** reprodutibilidade em outra máquina, teste e
  a sequência que um pipeline rodaria;
- **Ato IV — A infra cresce (Cenários 13 a 18):** a Mirante migra para o cluster e para a
  nuvem, até o incidente em que as três famílias divergem de uma vez.

E um contrato atravessa todos os atos: a Verificação exige `terraform_estado` ou
`terraform_plano_limpo` junto de toda Asserção de infraestrutura real. Nesta Trilha,
resultado certo pelo caminho errado não conta.
