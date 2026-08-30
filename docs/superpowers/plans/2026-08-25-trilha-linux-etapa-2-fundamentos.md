# Trilha Linux — Etapa 2: Fundamentos

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publicar a abertura conceitual da Trilha Linux — artigo, três diagramas novos e
Questionário de doze situações — no mesmo contrato das outras quatro Trilhas, e tornar a
Trilha a primeira a oferecer Fundamentos na ordem recomendada.

**Architecture:** O artigo é `content/linux/fundamentos.md`, com blocos ```` ```diagrama ````
declarativos conforme a [ADR 0004](../../adr/0004-diagramas-declarativos-no-conteudo.md):
o conteúdo nunca carrega SVG, coordenada, classe CSS ou JSX. Três ids novos entram no
catálogo fechado `frontend/src/visuaisDoDiagrama.ts`, ganham desenho em
`IlustracaoDoDiagrama.tsx` e entram na lista do `scripts-checar-conteudo.mjs`. O
`trilha.yaml` passa a declarar o bloco `fundamentos`, o alvo `linux` do checador entra no
`ci.yml`, e o `CatalogoRealTest` deixa de tratar `linux` como a Trilha sem Fundamentos.

**Tech Stack:** React 19 · TypeScript · Vite · SVG inline · SnakeYAML · JUnit 5 · AssertJ

## Global Constraints

- **Todas as restrições da [Etapa 1](./2026-08-25-trilha-linux-etapa-1-plataforma.md)
  continuam valendo** — em especial a pinagem do substrato: a imagem é
  `ubuntu:26.04@sha256:889d…7a6f`, e todo Cenário da Trilha carrega o mesmo `Dockerfile`,
  byte a byte. Esta Etapa não cria Cenário nenhum, então não toca em `workspace/`.
- Todo texto em **português**, incluindo nomes de seção, ids de questão e ids de
  alternativa.
- **Personagens são referidos por papel, não por pronome** — regra da narrativa da
  Aurora fixada no [design](../specs/2026-08-21-trilha-linux-design.md).
- O artigo mira **aproximadamente 300 linhas**, como os outros quatro (`docker` 290,
  `kubernetes` 287, `aws` 319, `iac` 318).
- `aproveitamentoMinimo: 80`, **sem bloquear a prática** — contrato da
  [ADR 0003](../../adr/0003-fundamentos-pertencem-a-trilha.md).
- O catálogo de visuais é **fechado**: nenhum `visual` fora de `visuaisDosDiagramas`, e
  o `viewBox` de toda ilustração é **`0 0 160 96`**. Só classes CSS já existentes
  (`ilustracao-plano`, `ilustracao-no`, `ilustracao-linha`, `ilustracao-seta`,
  `ilustracao-ponto`, e variantes `-destaque`, `-alerta`, `-sucesso`, `-fantasma`,
  `ilustracao-base`).
- `revisar` de cada questão precisa ser o slug de um cabeçalho `##` **existente** no
  artigo. O `scripts-checar-conteudo.mjs` reprova o contrário, e o `LeitorDeTrilha`
  também (`seção de revisão desconhecida`).
- Nenhum Cenário é criado nesta Etapa. `CatalogoRealTest` continua contando 62 Cenários
  e 1 Cenário `linux`; o que muda é só a asserção sobre os Fundamentos.

## Estrutura de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `frontend/src/visuaisDoDiagrama.ts` | três ids novos no catálogo fechado |
| `frontend/src/IlustracaoDoDiagrama.tsx` | o desenho SVG de cada id novo |
| `frontend/scripts-checar-conteudo.mjs` | os três ids em `visuaisValidos` |
| `content/linux/fundamentos.md` | o artigo, com catorze cabeçalhos `##` |
| `content/linux/questionario.yaml` | as doze situações |
| `content/linux/trilha.yaml` | ganha o bloco `fundamentos` |
| `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java` | `linux` deixa de ser a Trilha sem Fundamentos |
| `.github/workflows/ci.yml` | o alvo `linux` entra na checagem de conteúdo |
| `README.md` | a Trilha Linux passa de "Fundamentos vêm nas próximas etapas" para publicados |

## Os catorze cabeçalhos e seus slugs

O `scripts-checar-conteudo.mjs` e o `LeitorDeTrilha` geram o slug removendo acentos,
baixando a caixa e trocando tudo que não é letra ou número por hífen. Os cabeçalhos abaixo
são **exatos** — o `revisar` das questões depende deles.

| Cabeçalho `##` | Slug |
|---|---|
| `O problema: a máquina que você nunca viu` | `o-problema-a-maquina-que-voce-nunca-viu` |
| `Kernel e espaço de usuário` | `kernel-e-espaco-de-usuario` |
| `Tudo é arquivo` | `tudo-e-arquivo` |
| `Processo: quem está pedindo` | `processo-quem-esta-pedindo` |
| `Identidade e permissão` | `identidade-e-permissao` |
| `O shell` | `o-shell` |
| `Pacote e distribuição` | `pacote-e-distribuicao` |
| `Serviço e init` | `servico-e-init` |
| `Log` | `log` |
| `Rede no host` | `rede-no-host` |
| `Namespace e cgroup: o container` | `namespace-e-cgroup-o-container` |
| `Acesso mínimo` | `acesso-minimo` |
| `O ambiente desta Trilha` | `o-ambiente-desta-trilha` |
| `Da teoria aos Cenários` | `da-teoria-aos-cenarios` |

---

### Task 1: Os três visuais novos

Vêm primeiro porque o artigo referencia os ids: escrever o artigo antes deixaria o
`scripts-checar-conteudo.mjs` vermelho por um motivo que não é do artigo. Mesma ordem da
Etapa 2 da Trilha IaC.

**Files:**
- Modify: `frontend/src/visuaisDoDiagrama.ts`
- Modify: `frontend/src/IlustracaoDoDiagrama.tsx`
- Modify: `frontend/scripts-checar-conteudo.mjs`

**Interfaces:**
- Consumes: nada.
- Produces: os ids `fronteiras-kernel`, `arvore-de-processos` e `permissao-octal`,
  aceitos pelo tipo `VisualDoDiagrama` e pelo checador de conteúdo. A Task 2 os usa nos
  blocos ```` ```diagrama ````.

- [ ] **Step 1: Acrescentar os três ids ao catálogo fechado**

Em `frontend/src/visuaisDoDiagrama.ts`, acrescente ao final da lista, antes de `] as const`:

```ts
  'fronteiras-kernel',
  'arvore-de-processos',
  'permissao-octal',
```

- [ ] **Step 2: Rodar o type-check e ver falhar**

Run: `cd frontend && npx tsc --noEmit`
Expected: FAIL — `desenhos` em `IlustracaoDoDiagrama.tsx` é um
`Record<VisualDoDiagrama, ReactNode>` e passa a faltar três chaves. É o mesmo mecanismo
do `switch` exaustivo do backend: o tipo cobra a implementação.

- [ ] **Step 3: Desenhar `fronteiras-kernel`**

Em `IlustracaoDoDiagrama.tsx`, acrescente ao objeto `desenhos`, depois de
`'grafo-dependencias'`. Três faixas horizontais — espaço de usuário em cima, kernel
embaixo, e a chamada de sistema como a faixa estreita de alerta entre as duas, a única
porta:

```tsx
  'fronteiras-kernel': (
    <>
      <rect className="ilustracao-plano" x="8" y="8" width="144" height="28" rx="4" />
      <circle className="ilustracao-no" cx="30" cy="22" r="7" />
      <rect className="ilustracao-no" x="52" y="15" width="26" height="14" rx="3" />
      <path className="ilustracao-linha-fantasma" d="M58 19h14M58 24h9" />
      <rect className="ilustracao-plano ilustracao-plano-alerta" x="8" y="42" width="144" height="12" rx="3" />
      <path className="ilustracao-linha-fantasma" d="M16 48h128" />
      <rect className="ilustracao-plano" x="8" y="60" width="144" height="28" rx="4" />
      <rect className="ilustracao-no-destaque" x="74" y="67" width="28" height="14" rx="3" />
      <circle className="ilustracao-ponto" cx="30" cy="74" r="4" />
      <circle className="ilustracao-ponto" cx="52" cy="74" r="4" />
      <path className="ilustracao-linha-fantasma" d="M80 71h16M120 71h12" />
    </>
  ),
```

- [ ] **Step 4: Desenhar `arvore-de-processos`**

Um nó de destaque no topo — o PID 1 — e a descendência espalhando por baixo, com um neto
em tom de sucesso para marcar que toda linhagem termina em alguém:

```tsx
  'arvore-de-processos': (
    <>
      <rect className="ilustracao-no-destaque" x="66" y="8" width="28" height="16" rx="3" />
      <path className="ilustracao-linha" d="M80 24v12M40 36h80M40 36v12M80 36v12M120 36v12" />
      <rect className="ilustracao-no" x="26" y="48" width="28" height="16" rx="3" />
      <rect className="ilustracao-no" x="66" y="48" width="28" height="16" rx="3" />
      <rect className="ilustracao-no" x="106" y="48" width="28" height="16" rx="3" />
      <path className="ilustracao-linha" d="M40 64v8M54 64v8M80 64v8M120 64v8M134 64v8" />
      <rect className="ilustracao-no-sucesso" x="28" y="72" width="24" height="14" rx="3" />
      <rect className="ilustracao-no" x="96" y="72" width="24" height="14" rx="3" />
      <path className="ilustracao-linha-fantasma" d="M72 12h16M32 52h16M72 52h16M112 52h16M34 75h12M102 75h12" />
    </>
  ),
```

- [ ] **Step 5: Desenhar `permissao-octal`**

Três colunas sobre o mesmo arquivo — dono, grupo e outros — cada uma com as próprias
linhas de permissão, que vão encolhendo até "outros":

```tsx
  'permissao-octal': (
    <>
      <rect className="ilustracao-plano ilustracao-plano-destaque" x="8" y="16" width="40" height="64" rx="4" />
      <rect className="ilustracao-plano" x="60" y="16" width="40" height="64" rx="4" />
      <rect className="ilustracao-plano" x="112" y="16" width="40" height="64" rx="4" />
      <circle className="ilustracao-no-destaque" cx="28" cy="36" r="6" />
      <path className="ilustracao-linha" d="M14 52h28M14 60h28M14 68h28" />
      <circle className="ilustracao-no" cx="80" cy="36" r="6" />
      <path className="ilustracao-linha" d="M66 52h28M66 60h28" />
      <path className="ilustracao-linha-fantasma" d="M66 68h28" />
      <circle className="ilustracao-no" cx="132" cy="36" r="6" />
      <path className="ilustracao-linha" d="M118 52h28" />
      <path className="ilustracao-linha-fantasma" d="M118 60h28M118 68h28" />
    </>
  ),
```

- [ ] **Step 6: Acrescentar os três ids ao checador de conteúdo**

Em `frontend/scripts-checar-conteudo.mjs`, dentro do `Set` `visuaisValidos`, depois de
`'grafo-dependencias',`:

```js
  'fronteiras-kernel',
  'arvore-de-processos',
  'permissao-octal',
```

- [ ] **Step 7: Rodar o type-check e o build e ver passar**

Run: `cd frontend && npx tsc --noEmit && npm run build`
Expected: PASS.

- [ ] **Step 8: Commitar**

```bash
git add frontend/src/visuaisDoDiagrama.ts frontend/src/IlustracaoDoDiagrama.tsx frontend/scripts-checar-conteudo.mjs
git commit -m "feat: três visuais novos para os Fundamentos de Linux"
```

---

### Task 2: O artigo

**Files:**
- Create: `content/linux/fundamentos.md`

**Interfaces:**
- Consumes: os três ids da Task 1, mais o `fronteiras-runtime` já existente.
- Produces: os catorze cabeçalhos `##` da tabela acima, que a Task 3 referencia em
  `revisar`.

- [ ] **Step 1: Escrever a abertura**

O arquivo começa com `# Fundamentos do Linux` e dois parágrafos sem cabeçalho, no molde
dos outros quatro Fundamentos. O primeiro amarra a Trilha ao modelo mental do design:
uma máquina Linux é **um kernel e um monte de processos pedindo coisas a ele**, e todo
assunto difícil da Trilha é uma de quatro perguntas — quem está pedindo, com que
identidade, sobre qual recurso, e quem garante que continue pedindo. O segundo repete o
contrato da ADR 0003, com estas palavras de efeito: a leitura prepara os Cenários, não
substitui o terminal, e os 80% do Questionário são recomendação, não bloqueio.

- [ ] **Step 2: Escrever as seções 1 a 3**

`## O problema: a máquina que você nunca viu`
Abre a narrativa da Aurora: o leitor recebe o acesso ao servidor que alguém montou anos
atrás e saiu da empresa, e nenhuma documentação. Precisa dizer que operar sem enxergar
por dentro não é opção: o dia em que algo quebra, não há para onde correr. Encerra
anunciando o que o artigo vai construir — a máquina por dentro, das fronteiras ao
serviço.

`## Kernel e espaço de usuário`
A chamada de sistema como **única fronteira**: processo pede, o kernel atende — e o
processo nunca fala com o hardware diretamente. É a seção do primeiro diagrama:

````markdown
```diagrama
tipo: camadas
visual: fronteiras-kernel
titulo: Duas metades e uma porta
camadas:
  - titulo: espaço de usuário
    detalhe: os processos que você enxerga
  - titulo: chamada de sistema
    detalhe: a única porta entre as metades
    tom: alerta
  - titulo: kernel
    detalhe: quem de fato fala com o hardware
```
````

`## Tudo é arquivo`
FHS — `/etc`, `/var`, `/proc` —, descritor e inode. Precisa registrar a armadilha que o
Cenário 07 mede: o nome do arquivo é só um ponteiro para o inode, e apagar o nome não
libera o espaço enquanto um descritor continuar aberto. `df` conta o disco; `du` conta
nomes.

- [ ] **Step 3: Escrever as seções 4 e 5**

`## Processo: quem está pedindo`
PID, PPID, `fork`/`exec`, a árvore, sinais — e por que o PID 1 é diferente de todos os
outros: é quem colhe os órfãos. Carrega o segundo diagrama:

````markdown
```diagrama
tipo: fluxo
visual: arvore-de-processos
titulo: A linhagem de um processo
passos:
  - titulo: PID 1
    detalhe: o init, pai de todos
    tom: destaque
  - titulo: o shell
    detalhe: filho do PID 1, a sua sessão
  - titulo: o comando que você roda
    detalhe: neto do PID 1, morre quando termina
```
````

Encerra com a distinção que o Cenário 05 mede: `SIGTERM` é um pedido que o processo pode
recusar; `SIGKILL` é aplicado pelo kernel e não pode ser recusado.

`## Identidade e permissão`
Usuário, grupo, o octal, `setuid`/`setgid`, e root como **ausência de checagem**, não como
usuário especial. Carrega o terceiro diagrama:

````markdown
```diagrama
tipo: comparacao
visual: permissao-octal
titulo: Três conjuntos de permissão sobre o mesmo arquivo
colunas:
  - titulo: dono
    detalhe: quem criou o arquivo
    itens:
      - ler, escrever e executar
      - o único que pode mudar o modo
  - titulo: grupo
    detalhe: os colegas do mesmo grupo
    itens:
      - só o que o grupo declara
  - titulo: outros
    detalhe: todo o resto do sistema
    itens:
      - o mais restrito de todos
```
````

Registra a sutileza que o Cenário 02 mede: a permissão é conferida ao **processo**, não à
pessoa — e o processo nasce com o conjunto de grupos da sua sessão (`id -G`).

- [ ] **Step 4: Escrever as seções 6 a 8**

`## O shell`
Pipe, redirecionamento, stdout contra stderr, código de saída, ambiente e `$PATH`. Precisa
dizer que "o comando funciona" é sempre "funciona no ambiente da minha sessão" — e o
`$PATH` é parte do comando. Isso prepara a questão do cron.

`## Pacote e distribuição`
Repositório, versão empacotada contra versão do upstream. A distribuição promete
estabilidade, não novidade: ela empacota, testa e congela. O `apt` fala com o repositório
da distro, não com o site do projeto.

`## Serviço e init`
systemd, unit, target, dependência, e a diferença que o Cenário 08 existe para ensinar:
`start` sobe agora, `enable` registra a intenção de subir no boot. Sem o `enable`, o
systemd nem olha para a unit no boot — o vínculo do `[Install]` é o que liga o serviço ao
`multi-user.target`.

- [ ] **Step 5: Escrever as seções 9 a 11**

`## Log`
journald, prioridade, o que persiste e o que não. O systemd captura a saída do serviço no
journal, e ele **sobrevive ao processo** — é o único registro de por que a unit saiu, com
timestamp e código de saída. Aplicação pode nem escrever em arquivo.

`## Rede no host`
Interface, rota, porta em escuta, resolução de nome. Porta em escuta é recurso
exclusivo: o segundo `bind` falha com "address already in use". Por isso diagnóstico de
"não sobe" começa em `ss -tlnp` — o erro do serviço novo é a evidência do antigo.

`## Namespace e cgroup: o container`
Os dois mecanismos do kernel que **são** o container — e a ponte explícita para a Trilha
Docker. Reusa o visual `fronteiras-runtime`:

````markdown
```diagrama
tipo: comparacao
visual: fronteiras-runtime
titulo: O que separa um container de uma máquina virtual
colunas:
  - titulo: máquina virtual
    itens:
      - virtualiza o hardware
      - inicia um kernel próprio
  - titulo: container
    tom: destaque
    itens:
      - isola processos
      - namespace e cgroup sobre o mesmo kernel
```
````

Precisa fechar com a frase que o resto da Trilha reusa: o container deixa de ser mágica
antes mesmo de a Trilha Docker começar.

- [ ] **Step 6: Escrever as seções 12 a 14**

`## Acesso mínimo`
`sudo`, `/etc/sudoers.d`, capabilities, e por que um processo não precisa ser root para
quase nada. `sudo` é delegação seletiva, não um botão de virar root. `chmod 777` é
conceder a todos o que um só precisava.

`## O ambiente desta Trilha`
A linha `docker exec -it learning-infra-linux bash` explicada como incantação com prazo
de validade — a Trilha Docker explica cada pedaço dela. E a tabela de fidelidade do
design, obrigatória e no mesmo espírito da que o README tem para o MiniStack:

```markdown
| Fidelidade | O que a Trilha usa | O que a conclusão prova |
|---|---|---|
| Real | processo, sinal, usuário, grupo, permissão, pacote, systemd, journald, rota, porta | tudo isso é o Linux de verdade, sem emulação |
| Compartilhado | kernel, `/proc/sys`, relógio | são da VM do Docker Desktop; o Cenário observa, não altera |
| Ausente | boot, initramfs, módulo, disco, `dmesg`, firewall do host | não são ensináveis aqui e entram só como mapa do território |
```

`## Da teoria aos Cenários`
Fecha com o mapa dos quatro atos e uma frase por ato, e repete que a Verificação exige
estado **habilitado** e não apenas rodando: nesta Trilha, resultado certo pelo caminho
errado não conta.

- [ ] **Step 7: Rodar o checador de conteúdo**

Run: `cd frontend && node scripts-checar-conteudo.mjs linux`
Expected: FAIL apenas em `questionário inválido: ENOENT` — os quatro blocos de diagrama
precisam sair `OK`. Se algum bloco der `ERRO`, corrija antes de seguir; o
`questionario.yaml` é a Task 3.

- [ ] **Step 8: Conferir o tamanho e commitar**

Run: `wc -l content/linux/fundamentos.md`
Expected: entre 280 e 330 linhas. Muito abaixo disso significa seção cortada; muito acima
significa que alguma seção virou Cenário.

```bash
git add content/linux/fundamentos.md
git commit -m "feat: artigo dos Fundamentos do Linux"
```

---

### Task 3: O Questionário

**Files:**
- Create: `content/linux/questionario.yaml`

**Interfaces:**
- Consumes: os slugs dos cabeçalhos da Task 2.
- Produces: doze questões; a Task 4 as expõe pelo `trilha.yaml`.

- [ ] **Step 1: Escrever as seis primeiras questões**

```yaml
questoes:
  - id: processo-que-ignora-o-kill
    enunciado: Você roda kill <pid> num processo e ele continua rodando. Qual é a explicação mais provável?
    alternativas:
      - id: sinal-terminavel
        texto: O processo pode estar ignorando ou bloqueando o SIGTERM — o kill padrão envia um sinal que o alvo pode recusar, enquanto o SIGKILL não pode.
      - id: pid-errado
        texto: O número do PID estava errado e o kill atingiu outro processo.
      - id: precisa-de-root
        texto: Só o root pode matar processos; o sinal foi silenciosamente descartado.
    alternativaCorreta: sinal-terminavel
    explicacao: kill envia sinal, não executa morte. O SIGTERM é um pedido que o processo pode ignorar ou tratar; o SIGKILL é o recurso que nem o alvo recusa, porque quem o aplica é o kernel. A diferença entre pedir e impor é a matéria do Cenário 04.
    revisar: processo-quem-esta-pedindo

  - id: grupo-certo-sem-escrita
    enunciado: O arquivo pertence ao grupo do seu colega e o grupo tem permissão de escrita, mas ele não consegue gravar. O que explica?
    alternativas:
      - id: processo-nao-tem-o-grupo
        texto: O processo do colega não está rodando com aquele grupo entre seus grupos ativos — a permissão de grupo vale para processos cuja identidade inclui o grupo.
      - id: dono-unico-escreve
        texto: Só o dono escreve; permissão de grupo é herança de outro sistema operacional.
      - id: precisa-de-root
        texto: A escrita exige root, e o colega não é.
    alternativaCorreta: processo-nao-tem-o-grupo
    explicacao: Permissão é conferida ao processo, não à pessoa. O processo do colega nasce com um conjunto de grupos (id -G); se o grupo do arquivo não está nesse conjunto, o sistema enxerga "outros". Ser do grupo no papel não basta — o processo precisa carregar essa identidade. Cenário 02.
    revisar: identidade-e-permissao

  - id: disco-cheio-que-o-du-nao-ve
    enunciado: df diz que o disco está 100% e du soma muito menos. Qual é a causa clássica?
    alternativas:
      - id: arquivo-apagado-com-descritor-aberto
        texto: Um processo ainda mantém aberto o descritor de um arquivo que você apagou — o nome sumiu do diretório, mas o espaço só é devolvido quando o último descritor fecha.
      - id: du-nao-conta-ocultos
        texto: O du não conta arquivos ocultos nem diretórios do sistema.
      - id: disco-fragmentado
        texto: O espaço existe, mas está fragmentado demais para o df reportar.
    alternativaCorreta: arquivo-apagado-com-descritor-aberto
    explicacao: No Linux o nome é só um ponteiro para o inode; o espaço pertence ao inode enquanto houver um descritor aberto. Apagar o nome não libera o disco — o processo que ainda escreve continua acumulando. É o Cenário 07 inteiro.
    revisar: tudo-e-arquivo

  - id: sobe-na-mao-nao-no-boot
    enunciado: O serviço sobe com systemctl start, mas depois de reiniciar a máquina ele não volta. Por quê?
    alternativas:
      - id: faltou-enable
        texto: Faltou systemctl enable: start sobe agora, mas só o enable grava a intenção de subir no boot.
      - id: unit-com-erro
        texto: A unit tem erro de sintaxe que só aparece no boot.
      - id: systemd-nao-persiste
        texto: O systemd não guarda estado entre reinicializações, por desenho.
    alternativaCorreta: faltou-enable
    explicacao: start responde "agora", enable responde "sempre". O vínculo de boot é criado pelo [Install] mais o enable, que cria o symlink no target. Sem isso, o systemd nem olha para a unit no boot. É a diferença que o Cenário 08 existe para ensinar.
    revisar: servico-e-init

  - id: a-morte-das-tres
    enunciado: Um serviço morreu às 3h e você precisa saber por quê. Onde procura primeiro?
    alternativas:
      - id: journalctl-da-unit
        texto: No journal da unit: journalctl -u traz as mensagens do serviço e o código de saída, mesmo que ele já não rode.
      - id: arquivo-de-log-do-app
        texto: No arquivo de log que a aplicação escreve em /var/log, que toda aplicação mantém por conta própria.
      - id: dmesg
        texto: No dmesg, que registra toda morte de processo do espaço de usuário.
    alternativaCorreta: journalctl-da-unit
    explicacao: O systemd captura a saída do serviço no journal, e ele sobrevive ao processo — é o único registro de por que a unit saiu, com prioridade e timestamp. A aplicação pode nem escrever em arquivo; o dmesg fala do kernel, não da sua unit. Cenário 09.
    revisar: log

  - id: funciona-no-seu-shell-nao-no-cron
    enunciado: O comando funciona no seu shell e falha quando o cron roda. Qual é a causa mais comum?
    alternativas:
      - id: ambiente-diferente
        texto: O cron roda com um ambiente mínimo — $PATH curto e sem as variáveis da sua sessão interativa.
      - id: cron-nao-pode-executar
        texto: O cron não tem permissão para executar aquele binário.
      - id: relogio-adiantado
        texto: O cron rodou num horário em que o comando ainda não estava instalado.
    alternativaCorreta: ambiente-diferente
    explicacao: O que "o comando funciona" significa é: funciona no ambiente da sua sessão, com seu $PATH, suas variáveis e seu diretório. O cron herda quase nada disso. A lição do Ato II: o ambiente é parte do comando, e confiar nele é assumir o que não está escrito. Cenário 10.
    revisar: o-shell
```

- [ ] **Step 2: Escrever as seis últimas questões**

Acrescente ao mesmo arquivo, mantendo a indentação:

```yaml
  - id: a-porta-ocupada
    enunciado: Dois serviços querem escutar na mesma porta 8080. O que acontece?
    alternativas:
      - id: o-segundo-falha
        texto: O segundo não consegue vincular: uma porta é um recurso exclusivo, e o primeiro que faz bind fica com ela.
      - id: dividem-a-porta
        texto: Os dois dividem a porta e o kernel reparte as conexões entre eles.
      - id: o-segundo-toma
        texto: O segundo assume a porta e o primeiro é silenciosamente desconectado.
    alternativaCorreta: o-segundo-falha
    explicacao: Porta em escuta é recurso exclusivo: o segundo bind falha com "address already in use". É por isso que diagnóstico de "não sobe" quase sempre começa em ss -tlnp para ver quem já está com a porta — o erro do serviço novo é a evidência do antigo. Cenário 12.
    revisar: rede-no-host

  - id: o-777-que-resolve-errado
    enunciado: Um processo de serviço falha por permissão, e alguém sugere chmod 777. O que há de errado?
    alternativas:
      - id: da-mais-que-o-necessario
        texto: 777 dá leitura e escrita para todo o sistema, quando o processo precisa de muito menos — o mínimo necessário resolve sem abrir a porta para qualquer um.
      - id: nao-funciona
        texto: O 777 não resolve permissão de execução em serviço.
      - id: sempre-perigoso-mas-necessario
        texto: É perigoso mas é a única forma de um serviço gravar em disco.
    alternativaCorreta: da-mais-que-o-necessario
    explicacao: O problema quase nunca é "permissão de menos", é identidade errada: o processo deveria rodar como um usuário que já pode o que precisa. 777 é conceder a todos o que um só precisava — e o princípio do acesso mínimo é exatamente o inverso. Cenário 13.
    revisar: acesso-minimo

  - id: versao-empacotada-mais-antiga
    enunciado: O site do projeto anuncia a versão 3.2, mas apt install entregou a 3.0. Por quê?
    alternativas:
      - id: repositorio-da-distribuicao
        texto: A versão no repositório da distribuição é a que foi empacotada e congelada para aquela versão da distro, não a mais nova do upstream.
      - id: apt-desatualizado
        texto: O apt está desatualizado e precisa de upgrade antes de ver versões novas.
      - id: arquitetura-diferente
        texto: A 3.2 só existe para outra arquitetura de processador.
    alternativaCorreta: repositorio-da-distribuicao
    explicacao: A distribuição promete estabilidade, não novidade: ela empacota, testa e congela versões. O upstream pode lançar todo mês; o repositório só move quando a distro decide. Daí a diferença entre versão empacotada e versão do upstream. Cenário 06.
    revisar: pacote-e-distribuicao

  - id: o-que-o-sudo-concede
    enunciado: O que o sudo realmente concede a quem usa?
    alternativas:
      - id: executa-como-outro-usuario
        texto: Executa um comando como outro usuário (por padrão root), conforme as regras de /etc/sudoers — não é um privilégio em bloco.
      - id: vira-root-permanente
        texto: Transforma a sessão inteira em root até o logout.
      - id: desativa-permissoes
        texto: Desativa as checagens de permissão da máquina enquanto o comando roda.
    alternativaCorreta: executa-como-outro-usuario
    explicacao: sudo é delegação seletiva: o sudoers descreve quem pode executar o quê como quem. Uma regra bem feita dá a um usuário apenas o comando de que ele precisa — é acesso mínimo em ação, não um botão de "virar root". Cenário 13.
    revisar: acesso-minimo

  - id: o-pid-1-do-container
    enunciado: Por que o PID 1 de um container não reaproveita processos órfãos como o init de uma máquina normal?
    alternativas:
      - id: init-de-uso-geral
        texto: O PID 1 de um container costuma ser um programa comum que não foi escrito para colher processos órfãos — um init de verdade faz essa tarefa.
      - id: kernel-diferente
        texto: O kernel dentro do container é diferente e não reaproveita órfãos.
      - id: nao-ha-orfaos
        texto: Não existem processos órfãos dentro de container, então não há o que reaproveitar.
    alternativaCorreta: init-de-uso-geral
    explicacao: A tarefa de reaproveitar órfãos (chamar wait nos filhos que perderam o pai) é do PID 1, e nem todo processo que vira PID 1 num container foi escrito para isso — órfãos se acumulam como zumbis. É por isso que o container desta Trilha sobe um systemd como PID 1. Cenário 05.
    revisar: processo-quem-esta-pedindo

  - id: o-que-o-container-nao-prova
    enunciado: Tudo nesta Trilha roda num container Ubuntu. O que esse resultado não prova sobre uma máquina Linux de verdade?
    alternativas:
      - id: kernel-boot-disco
        texto: Nada sobre kernel próprio, boot, módulos, discos e firewall do host — o container compartilha o kernel da VM e não os expõe.
      - id: nada-sobre-processos
        texto: Nada sobre processos, que se comportam diferente dentro de container.
      - id: nada-sobre-permissoes
        texto: Nada sobre permissões e donos, que o container ignora.
    alternativaCorreta: kernel-boot-disco
    explicacao: Processo, sinal, permissão, serviço e log são reais e transferíveis. Kernel, boot, initramfs, módulo, disco e firewall do host são da VM do Docker Desktop — o Cenário observa, não altera. A tabela de fidelidade existe para separar os dois.
    revisar: o-ambiente-desta-trilha
```

- [ ] **Step 3: Rodar o checador e ver passar**

Run: `cd frontend && node scripts-checar-conteudo.mjs linux`
Expected: `blocos diagrama: 4`, todos `OK`, `questões: 12`, nenhum `REVISAR QUEBRADO`,
nenhuma `CORRETA DESCONHECIDA`, e a última linha `tudo certo`.

- [ ] **Step 4: Commitar**

```bash
git add content/linux/questionario.yaml
git commit -m "feat: Questionário dos Fundamentos do Linux com doze situações"
```

---

### Task 4: Publicar os Fundamentos na Trilha

**Files:**
- Modify: `content/linux/trilha.yaml`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`
- Modify: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: `fundamentos.md` e `questionario.yaml` das Tasks 2 e 3.
- Produces: `catalogo.buscar("linux").orElseThrow().fundamentos()` deixa de ser `null`, e
  o alvo `linux` do checador entra no CI.

- [ ] **Step 1: Escrever o teste que falha**

Em `CatalogoRealTest`, o segundo teste hoje afirma que `linux` não tem Fundamentos.
Renomeie-o e troque a asserção. A linha
`assertThat(catalogo.buscar("linux").orElseThrow().fundamentos()).isNull();` vira um bloco
`satisfies` no mesmo molde de `docker`:

```java
    @Test
    void carregaCincoTrilhasComFundamentosPublicados() {
```

```java
        assertThat(catalogo.buscar("linux").orElseThrow().fundamentos())
                .satisfies(fundamentos -> {
                    assertThat(fundamentos.questionario().questoes()).hasSize(12);
                    assertThat(fundamentos.markdown()).contains("## Kernel e espaço de usuário");
                });
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw test -Dtest=CatalogoRealTest`
Expected: FAIL — `fundamentos()` ainda é `null`, porque o `trilha.yaml` não declara o bloco.

- [ ] **Step 3: Declarar os Fundamentos no manifesto**

`content/linux/trilha.yaml` passa a ser:

```yaml
id: linux
titulo: Linux
ordem: 1
fundamentos:
  titulo: Fundamentos do Linux
  artigo: fundamentos.md
  questionario: questionario.yaml
  aproveitamentoMinimo: 80
```

- [ ] **Step 4: Acrescentar o alvo `linux` ao CI**

Em `.github/workflows/ci.yml`, no passo `Checar conteúdo (diagramas e questionários)`,
acrescente o alvo novo como primeira linha do `run`, para refletir a ordem de estudo:

```yaml
        run: |
          node scripts-checar-conteudo.mjs linux
          node scripts-checar-conteudo.mjs docker
          node scripts-checar-conteudo.mjs kubernetes
          node scripts-checar-conteudo.mjs aws
          node scripts-checar-conteudo.mjs iac
```

- [ ] **Step 5: Rodar a suíte inteira e ver passar**

Run: `cd backend && ./mvnw test`
Expected: PASS.

- [ ] **Step 6: Commitar**

```bash
git add content/linux/trilha.yaml backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java .github/workflows/ci.yml
git commit -m "feat: publica os Fundamentos da Trilha Linux no catálogo e no CI"
```

---

### Task 5: Conferência visual e README

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: tudo das tarefas anteriores.
- Produces: nada consumido por código.

- [ ] **Step 1: Conferir os quatro diagramas na tela**

Suba backend e frontend, abra <http://localhost:5180>, entre na Trilha Linux e nos
Fundamentos. Confira, para cada um dos quatro diagramas:

1. a ilustração aparece e não estoura a moldura;
2. no tema claro e no escuro as formas continuam legíveis;
3. `fronteiras-kernel`, `arvore-de-processos` e `permissao-octal` são visualmente
   distintos entre si e dos já existentes — se dois parecerem o mesmo desenho, ajuste as
   coordenadas antes de seguir;
4. `fronteiras-runtime` reusado na seção 11 aparece com as colunas do container.

Se algum desenho estiver desequilibrado, corrija em `IlustracaoDoDiagrama.tsx` e commite
o ajuste antes do próximo passo.

- [ ] **Step 2: Fazer o Questionário de ponta a ponta**

Responda as doze questões deliberadamente errado, confirme que cada explicação aparece e
que cada link `revisar` leva à seção certa do artigo. Depois refaça acertando e confirme
o aproveitamento de 100%.

- [ ] **Step 3: Atualizar o README**

Na seção "## Fundamentos e evolução das Trilhas", troque "As quatro Trilhas publicadas"
por "As cinco Trilhas publicadas" e acrescente o item da Trilha Linux à lista. Substitua
o parágrafo que descreve a Trilha Linux como "Fundamentos e os demais Cenários vêm nas
próximas etapas" por:

```markdown
A quinta Trilha, **Linux**, abre o catálogo — é a primeira da ordem recomendada de estudo.
Fundamentos publicados e os Cenários 01 a 14 em construção, com o Cenário 08 já no ar como
prova da plataforma. O desenho completo — Fundamentos e 14 Cenários em quatro atos — está em
[`docs/superpowers/specs/2026-08-21-trilha-linux-design.md`](docs/superpowers/specs/2026-08-21-trilha-linux-design.md).
```

- [ ] **Step 4: Rodar tudo e commitar**

Run: `cd backend && ./mvnw test`
Run: `cd frontend && npm run build && node scripts-checar-conteudo.mjs linux`
Expected: PASS nos três.

```bash
git add README.md
git commit -m "docs: README registra os Fundamentos de Linux publicados"
```

---

## Definição de pronto

- `cd backend && ./mvnw test` passa, com `CatalogoRealTest` abrindo por `linux` e
  afirmando que os Fundamentos de Linux existem e têm 12 questões.
- `cd frontend && npm run build` e `npx tsc --noEmit` passam.
- `node frontend/scripts-checar-conteudo.mjs linux` imprime `blocos diagrama: 4`,
  `questões: 12` e `tudo certo`.
- Os catorze cabeçalhos `##` do artigo batem exatamente com a tabela de slugs deste plano.
- Os três diagramas novos foram conferidos na tela, nos dois temas.
- O Questionário foi respondido de ponta a ponta e os doze links `revisar` funcionam.
