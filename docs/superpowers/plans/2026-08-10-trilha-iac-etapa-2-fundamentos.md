# Trilha IaC — Etapa 2: Fundamentos

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publicar a abertura conceitual da Trilha IaC — artigo, três diagramas novos e
Questionário de doze situações — no mesmo contrato das outras três Trilhas.

**Architecture:** O artigo é `content/iac/fundamentos.md`, com blocos ```` ```diagrama ````
declarativos conforme a [ADR 0004](../../adr/0004-diagramas-declarativos-no-conteudo.md):
o conteúdo nunca carrega SVG, coordenada, classe CSS ou JSX. Três ids novos entram no
catálogo fechado `frontend/src/visuaisDoDiagrama.ts`, ganham desenho em
`IlustracaoDoDiagrama.tsx` e entram na lista do `scripts-checar-conteudo.mjs`. O
`trilha.yaml` passa a declarar o bloco `fundamentos`, e o `CatalogoRealTest` deixa de
tratar `iac` como exceção.

**Tech Stack:** React 19 · TypeScript · Vite · SVG inline · SnakeYAML · JUnit 5 · AssertJ

## Global Constraints

- **Todas as restrições da [Etapa 1](./2026-08-09-trilha-iac-etapa-1-plataforma.md)
  continuam valendo.**
- Terraform CLI **1.15.8**, pinado; providers `kreuzwerker/docker` **~> 4.5**,
  `hashicorp/kubernetes` **~> 3.2**, `hashicorp/aws` **~> 6.58**.
- Todo texto em **português**, incluindo nomes de seção, ids de questão e ids de
  alternativa.
- **Personagens são referidos por papel, não por pronome** — regra da narrativa da
  Mirante fixada no [design](../specs/2026-08-09-trilha-iac-design.md).
- O artigo mira **aproximadamente 300 linhas**, como os outros três (`docker` 290,
  `kubernetes` 287, `aws` 319).
- `aproveitamentoMinimo: 80`, **sem bloquear a prática** — contrato da
  [ADR 0003](../../adr/0003-fundamentos-pertencem-a-trilha.md).
- O catálogo de visuais é **fechado**: nenhum `visual` fora de `visuaisDosDiagramas`, e
  o `viewBox` de toda ilustração é **`0 0 160 96`**.
- `revisar` de cada questão precisa ser o slug de um cabeçalho `##` **existente** no
  artigo. O `scripts-checar-conteudo.mjs` reprova o contrário.
- Nenhum arquivo `.tf` é criado nesta Etapa. O `GuardrailsDeTerraformTest` continua verde
  com o `versions.tf` do Cenário 01.

## Estrutura de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `frontend/src/visuaisDoDiagrama.ts` | três ids novos no catálogo fechado |
| `frontend/src/IlustracaoDoDiagrama.tsx` | o desenho SVG de cada id novo |
| `frontend/scripts-checar-conteudo.mjs` | os três ids em `visuaisValidos` |
| `content/iac/fundamentos.md` | o artigo, com quinze cabeçalhos `##` |
| `content/iac/questionario.yaml` | as doze situações |
| `content/iac/trilha.yaml` | ganha o bloco `fundamentos` |
| `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java` | `iac` deixa de ser a Trilha sem Fundamentos |
| `README.md` | a Trilha IaC sai de "em construção" na seção de Fundamentos |

## Os quinze cabeçalhos e seus slugs

O `scripts-checar-conteudo.mjs` gera o slug removendo acentos, baixando a caixa e
trocando tudo que não é letra ou número por hífen. Os cabeçalhos abaixo são **exatos** —
o `revisar` das questões depende deles.

| Cabeçalho `##` | Slug |
|---|---|
| `O problema: a infra que só existe na memória` | `o-problema-a-infra-que-so-existe-na-memoria` |
| `Imperativo e declarativo` | `imperativo-e-declarativo` |
| `O ciclo: código, plan, apply, state` | `o-ciclo-codigo-plan-apply-state` |
| `O triângulo: código, state e mundo real` | `o-triangulo-codigo-state-e-mundo-real` |
| `Reconciliação, de novo` | `reconciliacao-de-novo` |
| `O grafo de dependências` | `o-grafo-de-dependencias` |
| `Provider como tradutor` | `provider-como-tradutor` |
| `Módulos e composição` | `modulos-e-composicao` |
| `Idempotência e drift` | `idempotencia-e-drift` |
| `Blast radius: ler o plano` | `blast-radius-ler-o-plano` |
| `Segredos e o state` | `segredos-e-o-state` |
| `Infra que se testa` | `infra-que-se-testa` |
| `O mapa do território` | `o-mapa-do-territorio` |
| `O ambiente desta Trilha` | `o-ambiente-desta-trilha` |
| `Da teoria aos Cenários` | `da-teoria-aos-cenarios` |

---

### Task 1: Os três visuais novos

Vêm primeiro porque o artigo referencia os ids: escrever o artigo antes deixaria o
`scripts-checar-conteudo.mjs` vermelho por um motivo que não é do artigo.

**Files:**
- Modify: `frontend/src/visuaisDoDiagrama.ts`
- Modify: `frontend/src/IlustracaoDoDiagrama.tsx`
- Modify: `frontend/scripts-checar-conteudo.mjs`

**Interfaces:**
- Consumes: nada.
- Produces: os ids `ciclo-iac`, `triangulo-state` e `grafo-dependencias`, aceitos pelo
  tipo `VisualDoDiagrama` e pelo checador de conteúdo. A Task 2 os usa nos blocos
  ```` ```diagrama ````.

- [ ] **Step 1: Acrescentar os três ids ao catálogo fechado**

Em `frontend/src/visuaisDoDiagrama.ts`, acrescente ao final da lista, antes de `] as const`:

```ts
  'ciclo-iac',
  'triangulo-state',
  'grafo-dependencias',
```

- [ ] **Step 2: Rodar o type-check e ver falhar**

Run: `cd frontend && npx tsc --noEmit`
Expected: FAIL — `desenhos` em `IlustracaoDoDiagrama.tsx` é um
`Record<VisualDoDiagrama, ReactNode>` e passa a faltar três chaves. É o mesmo mecanismo
do `switch` exaustivo do backend: o tipo cobra a implementação.

- [ ] **Step 3: Desenhar `ciclo-iac`**

Em `IlustracaoDoDiagrama.tsx`, acrescente ao objeto `desenhos`, depois de
`'fidelidade-local'`. Quatro caixas em ciclo — código, plan, apply, state — com a seta de
retorno do state para o plan, que é o que distingue o ciclo do Terraform de uma esteira
linear:

```tsx
  'ciclo-iac': (
    <>
      <rect className="ilustracao-plano" x="10" y="18" width="32" height="24" rx="4" />
      <rect className="ilustracao-plano ilustracao-plano-destaque" x="64" y="18" width="32" height="24" rx="4" />
      <rect className="ilustracao-plano" x="118" y="18" width="32" height="24" rx="4" />
      <rect className="ilustracao-plano ilustracao-plano-alerta" x="64" y="62" width="32" height="24" rx="4" />
      <path className="ilustracao-linha-fantasma" d="M16 26h20M16 33h14M124 26h20M124 33h12" />
      <path className="ilustracao-linha" d="M42 30h16M96 30h16M134 42v20H102M62 74H26V42" />
      <path className="ilustracao-seta" d="m53 26 6 4-6 4M107 26l6 4-6 4M107 70l-6 4 6 4M31 47l-5-6 5-1" />
      <circle className="ilustracao-ponto" cx="80" cy="30" r="4" />
      <circle className="ilustracao-ponto-alerta" cx="80" cy="74" r="4" />
    </>
  ),
```

- [ ] **Step 4: Desenhar `triangulo-state`**

Três vértices — código, state e mundo real — e as três arestas que podem divergir. O
vértice do mundo real é o de tom alerta, porque é o único que muda sem passar por você:

```tsx
  'triangulo-state': (
    <>
      <path className="ilustracao-linha" d="M80 20 30 74h100z" />
      <circle className="ilustracao-no-destaque" cx="80" cy="20" r="13" />
      <circle className="ilustracao-no" cx="30" cy="74" r="13" />
      <circle className="ilustracao-no-alerta" cx="130" cy="74" r="13" />
      <path className="ilustracao-linha-fantasma" d="M74 16h12M24 70h12M124 70h12M74 24h12M24 78h12M124 78h12" />
      <circle className="ilustracao-ponto" cx="53" cy="48" r="3" />
      <circle className="ilustracao-ponto" cx="107" cy="48" r="3" />
      <circle className="ilustracao-ponto-alerta" cx="80" cy="74" r="3" />
    </>
  ),
```

- [ ] **Step 5: Desenhar `grafo-dependencias`**

Uma raiz que dois nós referenciam, e um terceiro nó que depende de um deles — a ordem que
ninguém escreveu, derivada de referência:

```tsx
  'grafo-dependencias': (
    <>
      <rect className="ilustracao-no-destaque" x="62" y="10" width="36" height="20" rx="3" />
      <path className="ilustracao-linha" d="M80 30v12H40v10M80 42h40v10" />
      <path className="ilustracao-seta" d="m36 47 4 6 4-6M116 47l4 6 4-6" />
      <rect className="ilustracao-no" x="22" y="52" width="36" height="20" rx="3" />
      <rect className="ilustracao-no" x="102" y="52" width="36" height="20" rx="3" />
      <path className="ilustracao-linha" d="M120 72v8H80" />
      <path className="ilustracao-seta" d="m85 76-5 4 5 4" />
      <rect className="ilustracao-no-sucesso" x="44" y="74" width="36" height="14" rx="3" />
      <path className="ilustracao-linha-fantasma" d="M28 60h24M108 60h24" />
    </>
  ),
```

- [ ] **Step 6: Acrescentar os três ids ao checador de conteúdo**

Em `frontend/scripts-checar-conteudo.mjs`, dentro do `Set` `visuaisValidos`, depois de
`'fidelidade-local',`:

```js
  'ciclo-iac',
  'triangulo-state',
  'grafo-dependencias',
```

- [ ] **Step 7: Rodar o type-check e o build e ver passar**

Run: `cd frontend && npx tsc --noEmit && npm run build`
Expected: PASS.

- [ ] **Step 8: Commitar**

```bash
git add frontend/src/visuaisDoDiagrama.ts frontend/src/IlustracaoDoDiagrama.tsx frontend/scripts-checar-conteudo.mjs
git commit -m "feat: três visuais novos para os Fundamentos de IaC"
```

---

### Task 2: O artigo

**Files:**
- Create: `content/iac/fundamentos.md`

**Interfaces:**
- Consumes: os três ids da Task 1.
- Produces: os quinze cabeçalhos `##` da tabela acima, que a Task 3 referencia em
  `revisar`.

- [ ] **Step 1: Escrever a abertura**

O arquivo começa com `# Fundamentos de Infraestrutura como Código` e dois parágrafos sem
cabeçalho, no molde dos outros três Fundamentos: o primeiro amarra a Trilha às anteriores
— Docker, Kubernetes e AWS foram construídos **à mão**, e esta Trilha reconstrói tudo
declarativamente; o segundo repete o contrato da ADR 0003, com estas palavras de
efeito: a leitura prepara os Cenários, não substitui o terminal, e os 80% do Questionário
são recomendação, não bloqueio.

- [ ] **Step 2: Escrever as seções 1 a 3**

`## O problema: a infra que só existe na memória`
Abre a narrativa da Mirante e nomeia o custo concreto: a infra existe, funciona e ninguém
sabe reconstruí-la. Precisa dizer que documentação em wiki não resolve, porque ela não é
executável e não tem como ser conferida contra a realidade. Encerra distinguindo os três
sintomas que a Trilha inteira ataca — não reprodutível, não auditável, não transferível.

`## Imperativo e declarativo`
Descrever o destino, não o caminho. O contraste obrigatório é entre um script que roda
`docker run` — e falha na segunda execução porque o container já existe — e um `.tf` que
descreve o container e não faz nada quando ele já está do jeito descrito. Encerra
nomeando o que o modo declarativo compra: rodar de novo sem medo.

`## O ciclo: código, plan, apply, state`
As quatro peças e a ordem entre elas. O `plan` é **leitura**; o `apply` é a única
escrita. O state é o que o Terraform sabe, e é dele que sai a resposta para "o que eu
criei?". Esta seção carrega o primeiro diagrama, exatamente assim:

````markdown
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
````

A seção também precisa responder à questão do state perdido: quem perde o state não perde
a infraestrutura, perde a **correspondência** entre código e infraestrutura — e recuperá-la
é `import`, assunto do Cenário 07.

- [ ] **Step 3: Escrever a seção 4, o triângulo**

`## O triângulo: código, state e mundo real`
É a seção mais importante do artigo: o modelo mental que a Trilha inteira cobra. Carrega
o diagrama e a tabela das divergências.

````markdown
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
````

Depois do diagrama, a tabela que nomeia cada divergência e aponta o Cenário que a
exercita:

```markdown
| Divergência | Nome de mercado | Onde você vai encontrar |
|---|---|---|
| mundo ≠ state | drift | Cenários 05 e 14 |
| mundo existe, state não | brownfield, infra órfã | Cenário 07 |
| state existe, mundo não | recurso apagado por fora | Cenários 05 e 18 |
| código ≠ state para o mesmo recurso | refactor | Cenário 09 |
```

Encerra com a frase que o resto do artigo reusa: quase todo assunto difícil de IaC é um
par desses três fora de sincronia.

- [ ] **Step 4: Escrever as seções 5 a 7**

`## Reconciliação, de novo`
Reusa o visual `reconciliacao` dos Fundamentos de Kubernetes e marca a **diferença de
cadência**: o controller reconcilia sozinho e para sempre; o Terraform reconcilia quando
mandam. É isso que explica por que drift é problema central de IaC e quase não é de
Kubernetes. O diagrama:

````markdown
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
````

`## O grafo de dependências`
A ordem é **derivada de referência**, não escrita. `image = docker_image.web.image_id` é
uma declaração de dependência disfarçada de reuso de valor. `depends_on` existe para a
dependência que não aparece em nenhuma referência, e é a exceção. Carrega o diagrama:

````markdown
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
````

`## Provider como tradutor`
O mesmo HCL sobre três substratos. Provider é quem traduz recurso declarado em chamada de
API. Precisa dizer que **a constraint de versão do provider é parte do código**: sem
`required_providers` pinado, a máquina do colega pode receber outra versão e ver outro
plano. Reusa `fidelidade-local`:

````markdown
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
````

- [ ] **Step 5: Escrever as seções 8 a 12**

`## Módulos e composição`
Entrada, saída e a razão de existir: copiar-colar ambiente é dívida que se paga na
próxima mudança, multiplicada pelo número de cópias. Precisa registrar a armadilha
medida na pesquisa: **um módulo filho declara o próprio `required_providers`**, senão o
`init` procura `hashicorp/docker` e falha.

`## Idempotência e drift`
Aplicar duas vezes não faz duas coisas. Drift é o mundo mudando sem passar pelo código, e
o `plan` é o detector. Nomeia o que o Cenário 05 vai medir: um `docker stop` feito à mão
já produz plano divergente.

`## Blast radius: ler o plano`
A seção mais densa, e a mais cobrada no Questionário. Precisa cobrir, nesta ordem:
ler `+ ~ - -/+`; o que `forces replacement` significa quando aparece embaixo de um banco
de dados; o bloco `moved` como a ferramenta de renomear sem destruir; `prevent_destroy`
como cinto de segurança — e a sutileza medida na pesquisa, de que ele protege **um
endereço, não um recurso**, de modo que declará-lo no endereço novo não salva o antigo; e
`-target` como ferramenta de emergência, que estreita o plano e por isso mesmo mente
sobre o resto.

`## Segredos e o state`
O state guarda valores em claro. `sensitive = true` esconde da **saída do terminal**, não
do arquivo. A consequência prática é a que o Cenário 06 cobra: `.gitignore` para o state
local, e state remoto com acesso restrito quando o time cresce.

`## Infra que se testa`
Quatro degraus, do mais barato ao mais caro: `fmt -check`, `validate`,
`precondition`/`postcondition`/`check`, e `terraform test` com `command = plan` e
`command = apply`. Precisa deixar claro o que cada um alcança: `validate` não fala com
provider nenhum; `precondition` roda durante o plano; `check` avisa sem reprovar o apply;
o `test` com `apply` cria e destrói infraestrutura de verdade.

- [ ] **Step 6: Escrever as seções 13 a 15**

`## O mapa do território`
Terraform e o fork OpenTofu — licença BUSL desde 2023, o fork sob a Linux Foundation, HCL
idêntico, e a razão de a Trilha usar Terraform: é o comando que aparece nas vagas.
CloudFormation, já exercitado nos Cenários 06, 17 e 18 da Trilha AWS, entra como
comparação. Pulumi e CDK, mesma ideia com outra sintaxe. E Ansible como **categoria
diferente**: configurar uma máquina que já existe não é criá-la.

`## O ambiente desta Trilha`
Terraform 1.15.8 pinado; os três providers e suas versões; o bloco de portas 8070–8079; e
o aviso de fidelidade que a Trilha AWS já estabeleceu — nenhuma Asserção desta Trilha
alega isolamento de rede, IAM aplicado, custo ou disponibilidade.

`## Da teoria aos Cenários`
Fecha com o mapa dos quatro atos e uma frase por ato, e repete que a Verificação exige
`terraform_estado` ou `terraform_plano_limpo` junto de toda Asserção de infraestrutura
real: nesta Trilha, resultado certo pelo caminho errado não conta.

- [ ] **Step 7: Rodar o checador de conteúdo**

Run: `cd frontend && node scripts-checar-conteudo.mjs iac`
Expected: FAIL apenas em `questionário inválido: ENOENT` — os cinco blocos de diagrama
precisam sair `OK`. Se algum bloco der `ERRO`, corrija antes de seguir; o
`questionario.yaml` é a Task 3.

- [ ] **Step 8: Conferir o tamanho e commitar**

Run: `wc -l content/iac/fundamentos.md`
Expected: entre 280 e 330 linhas. Muito abaixo disso significa seção cortada; muito acima
significa que alguma seção virou Cenário.

```bash
git add content/iac/fundamentos.md
git commit -m "feat: artigo dos Fundamentos de Infraestrutura como Código"
```

---

### Task 3: O Questionário

**Files:**
- Create: `content/iac/questionario.yaml`

**Interfaces:**
- Consumes: os slugs dos cabeçalhos da Task 2.
- Produces: doze questões; a Task 4 as expõe pelo `trilha.yaml`.

- [ ] **Step 1: Escrever as seis primeiras questões**

```yaml
questoes:
  - id: aplicar-duas-vezes
    enunciado: Você roda terraform apply, ele cria um container, e você roda terraform apply de novo sem mudar uma linha. O que acontece na segunda vez?
    alternativas:
      - id: nada-muda
        texto: O Terraform compara o código com o que o state conhece, não encontra diferença e não faz nada.
      - id: segundo-container
        texto: Um segundo container é criado, porque cada apply executa as instruções do arquivo.
      - id: erro-de-conflito
        texto: O apply falha com erro de nome duplicado, como aconteceria num script.
    alternativaCorreta: nada-muda
    explicacao: Um script descreve o caminho e repete o caminho. O Terraform descreve o destino e só age sobre a diferença. É isso que se chama idempotência, e é o que permite rodar o mesmo código todo dia.
    revisar: idempotencia-e-drift

  - id: correcao-na-mao-em-producao
    enunciado: Durante um incidente, alguém corrigiu a configuração direto no console e não mexeu no código. O serviço voltou. O que o próximo terraform apply faz com essa correção?
    alternativas:
      - id: desfaz-a-correcao
        texto: Desfaz — o plano vai propor voltar o recurso ao que o código descreve.
      - id: incorpora-ao-codigo
        texto: Incorpora a correção ao código automaticamente, já que ela está no mundo real.
      - id: ignora-para-sempre
        texto: Ignora, porque o Terraform só age sobre recursos que ele mesmo alterou desde o último apply.
    alternativaCorreta: desfaz-a-correcao
    explicacao: O código é a fonte de intenção. Uma correção que não voltou para ele é drift, e a próxima reconciliação a apaga. Correção de incidente que precisa durar vira commit.
    revisar: idempotencia-e-drift

  - id: forces-replacement-no-banco
    enunciado: O plano de uma mudança pequena mostra, embaixo do recurso do banco de dados, a marca forces replacement. O que isso significa?
    alternativas:
      - id: destroi-e-recria
        texto: O atributo alterado não pode ser mudado em lugar, então o Terraform vai destruir o banco e criar outro no lugar.
      - id: reinicia-o-servico
        texto: O banco vai ser reiniciado para aplicar a nova configuração, sem perder dados.
      - id: apenas-um-aviso
        texto: É um aviso de que o provider tem uma versão nova disponível para aquele recurso.
    alternativaCorreta: destroi-e-recria
    explicacao: forces replacement é a marca mais cara do plano. Ela diz que o recurso vai ser destruído e recriado — e num banco isso é perda de dados. É o motivo de o plano ser lido antes de aprovado, e não depois.
    revisar: blast-radius-ler-o-plano

  - id: state-no-notebook-de-quem-saiu
    enunciado: A pessoa que criou a infraestrutura saiu da empresa, e o arquivo de state estava só no notebook dela. A infraestrutura continua no ar. O que o time perdeu?
    alternativas:
      - id: a-correspondencia
        texto: A correspondência entre cada endereço do código e o recurso real — a infraestrutura existe, mas o Terraform não sabe mais que é dela.
      - id: a-infraestrutura
        texto: A infraestrutura, que para de funcionar assim que o state some.
      - id: apenas-o-historico
        texto: Apenas o histórico de execuções, já que o state guarda um log dos applies anteriores.
    alternativaCorreta: a-correspondencia
    explicacao: O state não sustenta a infraestrutura, ele a mapeia. Sem ele, o próximo apply tenta criar tudo de novo em cima do que já existe. Recuperar essa correspondência é import, e é por isso que state remoto compartilhado deixa de ser luxo quando o time passa de uma pessoa.
    revisar: o-ciclo-codigo-plan-apply-state

  - id: recurso-que-o-codigo-nao-conhece
    enunciado: Existe em produção um container que ninguém declarou em nenhum arquivo .tf. Como ele aparece no terraform plan?
    alternativas:
      - id: nao-aparece
        texto: Não aparece — o Terraform só enxerga o que está no state, e ele não está.
      - id: aparece-para-destruir
        texto: Aparece marcado para destruição, porque não consta no código.
      - id: aparece-como-drift
        texto: Aparece como drift, com aviso de recurso não gerenciado.
    alternativaCorreta: nao-aparece
    explicacao: O Terraform não varre o ambiente atrás do que existe. Ele compara o código com o state e checa no mundo real apenas os recursos que o state conhece. Infra órfã é invisível até alguém trazê-la para dentro com import.
    revisar: o-triangulo-codigo-state-e-mundo-real

  - id: renomear-sem-destruir
    enunciado: Você quer renomear docker_volume.dados para docker_volume.estoque, mantendo o mesmo volume e os dados dentro dele. Qual é o caminho seguro?
    alternativas:
      - id: bloco-moved
        texto: Declarar um bloco moved do endereço antigo para o novo e aplicar — o Terraform move o registro no state sem tocar no recurso.
      - id: renomear-e-aplicar
        texto: Renomear no código e aplicar; o Terraform reconhece que é o mesmo volume pelo nome declarado.
      - id: destruir-e-recriar
        texto: Destruir e recriar, restaurando os dados de um backup depois.
    alternativaCorreta: bloco-moved
    explicacao: Para o Terraform, o endereço é a identidade. Renomear sem moved é dizer que um recurso sumiu e outro nasceu — e o plano vem com um destroy embaixo dos seus dados. O bloco moved reescreve o state e não encosta no recurso.
    revisar: blast-radius-ler-o-plano
```

- [ ] **Step 2: Escrever as seis últimas questões**

Acrescente ao mesmo arquivo, mantendo a indentação:

```yaml
  - id: senha-no-state
    enunciado: Uma senha entrou como variável e você marcou a saída com sensitive = true. Onde essa senha está agora?
    alternativas:
      - id: em-claro-no-state
        texto: Em claro dentro do arquivo de state — sensitive esconde da saída do terminal, não do arquivo.
      - id: criptografada-no-state
        texto: Criptografada no state, que é o efeito de marcar o valor como sensitive.
      - id: nao-persistida
        texto: Em lugar nenhum: valores sensitive são usados na chamada da API e descartados.
    alternativaCorreta: em-claro-no-state
    explicacao: sensitive é uma proteção de exibição. O state guarda o valor como recebeu. Daí decorrem duas regras práticas: state local nunca vai para o Git, e state remoto compartilhado tem acesso restrito.
    revisar: segredos-e-o-state

  - id: target-durante-incidente
    enunciado: No meio de um incidente, alguém sugere terraform apply -target para aplicar só o recurso quebrado. Qual é o custo dessa escolha?
    alternativas:
      - id: plano-parcial
        texto: O plano deixa de representar o conjunto, então mudanças pendentes em outros recursos ficam invisíveis até o próximo apply completo.
      - id: nenhum-custo
        texto: Nenhum: -target é a forma recomendada de aplicar mudanças pontuais no dia a dia.
      - id: corrompe-o-state
        texto: O state é corrompido e precisa ser reconstruído com import depois.
    alternativaCorreta: plano-parcial
    explicacao: -target existe para emergência e cobra o preço de estreitar a visão. Ele não corrompe nada, mas o plano que você aprovou não é mais o retrato do sistema. Depois do incidente, um apply completo é o que reconcilia a conta.
    revisar: blast-radius-ler-o-plano

  - id: provider-sem-constraint
    enunciado: O código não declara versão de provider. Funciona na sua máquina e quebra na máquina de quem entrou no time ontem. Por quê?
    alternativas:
      - id: versao-diferente
        texto: O init de cada máquina resolveu uma versão diferente do provider, e o comportamento mudou entre elas.
      - id: cache-corrompido
        texto: O diretório .terraform da outra máquina está corrompido e precisa ser apagado.
      - id: state-incompativel
        texto: O state foi escrito por uma versão de Terraform mais nova do que a instalada lá.
    alternativaCorreta: versao-diferente
    explicacao: Sem constraint, o init pega a versão mais nova que satisfaz a configuração — e "mais nova" muda com o tempo. required_providers com constraint e o arquivo .terraform.lock.hcl versionado são o que faz duas máquinas receberem o mesmo binário.
    revisar: provider-como-tradutor

  - id: onde-ansible-entra
    enunciado: O time já provisiona servidores com Terraform e agora quer instalar e configurar pacotes dentro deles. Onde o Ansible se encaixa?
    alternativas:
      - id: configura-o-que-existe
        texto: Depois do provisionamento: ele configura máquinas que já existem, enquanto o Terraform cria a máquina.
      - id: substitui-o-terraform
        texto: No lugar do Terraform, já que também descreve o estado desejado em arquivos.
      - id: nao-se-encaixa
        texto: Em lugar nenhum: configuração de máquina é responsabilidade exclusiva da imagem.
    alternativaCorreta: configura-o-que-existe
    explicacao: São categorias diferentes. Provisionar é criar o recurso; configurar é ajustar o que já está de pé. Confundir as duas leva a usar provisioner para tudo, que é justamente o caminho que a documentação do Terraform recomenda evitar.
    revisar: o-mapa-do-territorio

  - id: terraform-contra-cloudformation
    enunciado: Comparado ao CloudFormation, que você já usou na Trilha AWS, qual é a diferença estrutural do Terraform?
    alternativas:
      - id: multiplos-providers
        texto: O Terraform fala com muitos providers pelo mesmo código e mantém o próprio state; o CloudFormation é da AWS e o estado da stack é gerido pelo serviço.
      - id: apenas-sintaxe
        texto: Apenas a sintaxe: HCL em vez de YAML, com o mesmo modelo de execução por baixo.
      - id: sem-state
        texto: O Terraform não tem state, e é por isso que ele consegue atravessar provedores.
    alternativaCorreta: multiplos-providers
    explicacao: A diferença que importa não é a sintaxe, é quem guarda o estado e quantos alvos o mesmo código alcança. Isso explica por que state, lock e backend são assunto no Terraform e quase não são no CloudFormation.
    revisar: o-mapa-do-territorio

  - id: o-que-a-fidelidade-local-nao-prova
    enunciado: Todos os Cenários de AWS desta Trilha passam contra o emulador local. O que esse resultado não autoriza você a afirmar?
    alternativas:
      - id: nada-sobre-iam-e-rede
        texto: Nada sobre IAM aplicado, isolamento de rede, custo ou disponibilidade — o emulador aceita a declaração sem executar o efeito.
      - id: nada-sobre-sintaxe
        texto: Nada sobre a sintaxe do HCL, que só é validada contra a API real.
      - id: nada-sobre-o-state
        texto: Nada sobre o state, que tem formato diferente quando o alvo é um emulador.
    alternativaCorreta: nada-sobre-iam-e-rede
    explicacao: O emulador tem fidelidade alta no control plane e parcial no data plane. Ele prova que o código descreve os recursos certos e que as relações entre eles fecham. Não prova que uma regra de segurança bloqueia tráfego, porque ali não passa tráfego.
    revisar: o-ambiente-desta-trilha
```

- [ ] **Step 3: Rodar o checador e ver passar**

Run: `cd frontend && node scripts-checar-conteudo.mjs iac`
Expected: `blocos diagrama: 5`, todos `OK`, `questões: 12`, nenhum `REVISAR QUEBRADO`,
nenhuma `CORRETA DESCONHECIDA`, e a última linha `tudo certo`.

- [ ] **Step 4: Commitar**

```bash
git add content/iac/questionario.yaml
git commit -m "feat: Questionário dos Fundamentos de IaC com doze situações"
```

---

### Task 4: Publicar os Fundamentos na Trilha

**Files:**
- Modify: `content/iac/trilha.yaml`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: `fundamentos.md` e `questionario.yaml` das Tasks 2 e 3.
- Produces: `catalogo.buscar("iac").orElseThrow().fundamentos()` deixa de ser `null`.

- [ ] **Step 1: Escrever o teste que falha**

Em `CatalogoRealTest`, o segundo teste hoje afirma que `iac` não tem Fundamentos.
Renomeie-o e troque a asserção:

```java
    @Test
    void carregaQuatroTrilhasComFundamentosPublicados() {
```

Troque a linha `assertThat(catalogo.buscar("iac").orElseThrow().fundamentos()).isNull();`
por:

```java
        assertThat(catalogo.buscar("iac").orElseThrow().fundamentos()).isNotNull();
        assertThat(catalogo.buscar("iac").orElseThrow().fundamentos().questionario())
                .isNotNull();
```

Se o restante do teste já percorre as Trilhas exigindo Fundamentos e o `iac` estava
excluído dessa varredura, remova a exclusão — a Trilha nova entra na regra geral.

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw test -Dtest=CatalogoRealTest`
Expected: FAIL — `fundamentos()` ainda é `null`, porque o `trilha.yaml` não declara o bloco.

- [ ] **Step 3: Declarar os Fundamentos no manifesto**

`content/iac/trilha.yaml` passa a ser:

```yaml
id: iac
titulo: Infraestrutura como Código
fundamentos:
  titulo: Fundamentos de Infraestrutura como Código
  artigo: fundamentos.md
  questionario: questionario.yaml
  aproveitamentoMinimo: 80
```

- [ ] **Step 4: Rodar a suíte inteira e ver passar**

Run: `cd backend && ./mvnw test`
Expected: PASS.

- [ ] **Step 5: Commitar**

```bash
git add content/iac/trilha.yaml backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: publica os Fundamentos da Trilha IaC no catálogo"
```

---

### Task 5: Conferência visual e README

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: tudo das tarefas anteriores.
- Produces: nada consumido por código.

- [ ] **Step 1: Conferir os três diagramas na tela**

Suba backend e frontend, abra <http://localhost:5180>, entre na Trilha Infraestrutura
como Código e nos Fundamentos. Confira, para cada um dos cinco diagramas:

1. a ilustração aparece e não estoura a moldura;
2. no tema claro e no escuro as formas continuam legíveis;
3. `ciclo-iac`, `triangulo-state` e `grafo-dependencias` são visualmente distintos entre
   si — se dois parecerem o mesmo desenho, ajuste as coordenadas antes de seguir.

Se algum desenho estiver desequilibrado, corrija em `IlustracaoDoDiagrama.tsx` e commite
o ajuste antes do próximo passo.

- [ ] **Step 2: Fazer o Questionário de ponta a ponta**

Responda as doze questões deliberadamente errado, confirme que cada explicação aparece e
que cada link `revisar` leva à seção certa do artigo. Depois refaça acertando e confirme
o aproveitamento de 100%.

- [ ] **Step 3: Atualizar o README**

Na seção "## Fundamentos e evolução das Trilhas", troque "Três das quatro Trilhas
oferecem" por "As quatro Trilhas oferecem" e substitua o parágrafo que descreve a Trilha
IaC como em construção por:

```markdown
A quarta Trilha, **Infraestrutura como Código**, tem Fundamentos publicados e Cenários em
construção. O estudo de ferramental está em
[`docs/research/iac-course.md`](docs/research/iac-course.md) e o desenho completo —
Fundamentos e 18 Cenários em quatro atos — em
[`docs/superpowers/specs/2026-08-09-trilha-iac-design.md`](docs/superpowers/specs/2026-08-09-trilha-iac-design.md).
```

- [ ] **Step 4: Rodar tudo e commitar**

Run: `cd backend && ./mvnw test`
Run: `cd frontend && npm run build && node scripts-checar-conteudo.mjs iac`
Expected: PASS nos três.

```bash
git add README.md
git commit -m "docs: README registra os Fundamentos de IaC publicados"
```

---

## Definição de pronto

- `cd backend && ./mvnw test` passa.
- `cd frontend && npm run build` e `npx tsc --noEmit` passam.
- `node frontend/scripts-checar-conteudo.mjs iac` imprime `blocos diagrama: 5`,
  `questões: 12` e `tudo certo`.
- Os quinze cabeçalhos `##` do artigo batem exatamente com a tabela de slugs deste plano.
- Os três diagramas novos foram conferidos na tela, nos dois temas.
- O Questionário foi respondido de ponta a ponta e os doze links `revisar` funcionam.
