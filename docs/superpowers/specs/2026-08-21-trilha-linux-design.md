# Design — Trilha de Linux

> Data: 2026-08-21
>
> Estudo de tema e viabilidade: [`docs/research/proximas-trilhas.md`](../../research/proximas-trilhas.md)
>
> Estado: desenho aprovado, implementação não iniciada

## Objetivo

Criar a quinta Trilha da plataforma e torná-la a **primeira da ordem recomendada de
estudo**. Ela não introduz uma tecnologia a mais: entrega o vocabulário que as outras
seis usam o tempo todo e nunca definem. Processo, dono de arquivo, permissão, pacote,
serviço, log, rota e porta são pré-requisito silencioso de Docker, Kubernetes, AWS,
Observabilidade, CI/CD e IaC — e hoje o leitor os encontra pela primeira vez já
embrulhados em outra abstração.

O critério de sucesso é o mesmo das anteriores, com um acréscimo: ao terminar, o leitor
diagnostica uma máquina Linux que a Trilha nunca mostrou, **e** chega ao Cenário 09 da
Trilha Docker entendendo `USER 10001` e `cap_drop` em vez de copiá-los.

## Decisões

| Decisão | Escolha | Motivo |
|---|---|---|
| Posição na ordem de estudo | 1ª de 7 | É o vocabulário das outras seis; ver [ADR 0005](../../adr/0005-ordem-das-trilhas-e-recomendacao.md) |
| Posição na ordem de construção | 1ª das três novas | Maior sinal de mercado das três: `Bash/Shell` em 48,7% |
| Substrato | container Ubuntu com systemd, subido por Compose | Medido; entrega serviço, log, timer, usuário e permissão reais sem WSL e sem `--privileged` |
| Ciclo de vida | `projetoCompose`, o que já existe | Medido: `up -d --build` e `down -v` cobrem o Cenário inteiro; zero código novo de ciclo de vida |
| Verificação | duas Asserções novas mais reuso de `comando_produz` | `contem` sozinho aprova serviço parado; ver a armadilha medida abaixo |
| Narrativa | arco contínuo, empresa própria | Não reusa a Mirante da Trilha IaC para não criar conflito de linha do tempo |
| Entrada no ambiente | `docker exec -it` assumido e explicado | Ver a nota de 2026-08-21 na [ADR 0001](../../adr/0001-sem-terminal-embutido.md) |

## O modelo mental que a trilha inteira cobra

Uma máquina Linux é **um kernel e um monte de processos pedindo coisas a ele**. Todo
assunto difícil desta Trilha é uma das quatro perguntas que decorrem disso:

| Pergunta | Nome de mercado | Cenários |
|---|---|---|
| Quem está pedindo? | processo, PID, PID 1, sinal | 04, 05, 14 |
| Com que identidade? | usuário, grupo, permissão, capability | 02, 13, 14 |
| Sobre qual recurso? | tudo é arquivo; FHS, descritor, inode | 01, 03, 07 |
| E quem garante que continue pedindo? | init, unit, dependência, log | 08, 09, 10, 11, 14 |

O fecho da Trilha é a quinta pergunta, que é a ponte para a seguinte: **e se dois
conjuntos de processos não pudessem se enxergar?** Aí aparecem namespace e cgroup, e o
container deixa de ser mágica antes mesmo de a Trilha Docker começar.

## Substrato e ferramental

| Peça | Versão | Papel |
|---|---|---|
| Imagem base | `ubuntu:26.04`, pinada por digest | LTS de 2026-04-23, suporte até 2031 |
| systemd | 259 (259.5-0ubuntu3.4) | medido dentro do container |
| Docker Compose | o que vem no Docker Desktop | sobe e derruba o Cenário |

Nenhum binário novo no PATH do leitor. A imagem é construída pelo próprio Compose a
partir de um `Dockerfile` no `workspace/`.

### A receita medida

```yaml
services:
  servidor:
    image: learning-infra-linux:1
    build: .
    container_name: learning-infra-linux
    hostname: aurora
    cgroup: host
    tmpfs:
      - /run
      - /run/lock
    volumes:
      - /sys/fs/cgroup:/sys/fs/cgroup:rw
```

Cinco achados medidos em 2026-08-21 que **fixam** essa receita:

1. `cgroup: host` é obrigatório. Com `--cgroupns=private`, com ou sem montar
   `/sys/fs/cgroup`, o container **morre**. Não existe caminho por ali.
2. A imagem precisa de `systemctl mask tmp.mount`. Sem isso, o Ubuntu 26.04 sobe
   `degraded` com exatamente uma unit falha — e um Cenário que ensina a ler
   `systemctl is-system-running` começaria mentindo.
3. Com a receita completa, `systemctl is-system-running` responde `running` e a lista de
   unidades falhas vem **vazia**.
4. `docker compose up -d --build` leva **68 s** na primeira vez e **6 s** nas seguintes,
   porque o cache de camadas sobrevive ao `work/` recriado. Daí a regra: **todos os
   Cenários da Trilha carregam o mesmo `Dockerfile`, byte a byte**, e declaram
   `image: learning-infra-linux:1` junto do `build:`. Um `Dockerfile` divergente por
   Cenário devolveria os 68 s a cada Iniciar.
5. **`ENV LANG=C.UTF-8` é obrigatório no `Dockerfile`.** Sem ele a imagem sobe com
   `LANG` vazio e `locale charmap` igual a `ANSI_X3.4-1968` — ASCII. Numa Trilha em
   português, onde o leitor cria arquivo com acento e edita texto acentuado, isso
   apareceria como sujeira no editor e contagem errada em ferramentas de texto. Medido:
   `C.UTF-8` resolve **sem instalar o pacote `locales`**, ou seja, custo zero de imagem.

   A escolha de `C.UTF-8` em vez de `pt_BR.UTF-8` é deliberada: acento funciona nos dois,
   mas o `C.UTF-8` mantém as mensagens do sistema em inglês — que é exatamente o texto
   que o leitor vai colar no buscador quando algo quebrar.

### Fidelidade do substrato

Tabela obrigatória nos Fundamentos, no mesmo espírito da que o README já tem para o
MiniStack:

| Fidelidade | O que a Trilha usa | O que a conclusão prova |
|---|---|---|
| Real | processo, sinal, usuário, grupo, permissão, pacote, systemd, journald, rota, porta | tudo isso é o Linux de verdade, sem emulação |
| Compartilhado | kernel, `/proc/sys`, relógio | são da VM do Docker Desktop; o Cenário observa, não altera |
| Ausente | boot, initramfs, módulo, disco, `dmesg`, firewall do host | não são ensináveis aqui e entram só como mapa do território |

## Fundamentos

Mesmo contrato da [ADR 0003](../../adr/0003-fundamentos-pertencem-a-trilha.md): artigo
mais Questionário de 12 situações, 80% recomendado, sem bloquear a prática. Alvo de
aproximadamente 300 linhas, como as outras quatro.

### Seções do artigo

1. **O problema** — o computador que você usa o dia inteiro e nunca viu por dentro; abre
   a narrativa.
2. **Kernel e espaço de usuário** — a chamada de sistema como única fronteira.
3. **Tudo é arquivo** — FHS, `/etc`, `/var`, `/proc`, descritor, inode.
4. **Processo** — PID, PPID, `fork`/`exec`, a árvore, sinais, e por que o PID 1 é
   diferente de todos os outros.
5. **Identidade e permissão** — usuário, grupo, o octal, `setuid`/`setgid`, e root como
   ausência de checagem, não como usuário especial.
6. **O shell** — pipe, redirecionamento, stdout contra stderr, código de saída, ambiente
   e `$PATH`.
7. **Pacote e distribuição** — repositório, versão empacotada contra versão do upstream.
8. **Serviço e init** — systemd, unit, target, dependência, `enable` contra `start`.
9. **Log** — journald, prioridade, o que persiste e o que não.
10. **Rede no host** — interface, rota, porta em escuta, resolução de nome.
11. **Namespace e cgroup** — os dois mecanismos do kernel que **são** o container. Reusa
    o visual `fronteiras-runtime` e é a ponte explícita para a Trilha Docker.
12. **Acesso mínimo** — `sudo`, `/etc/sudoers.d`, capabilities, e por que um processo não
    precisa ser root para quase nada.
13. **O ambiente desta Trilha** — a tabela de fidelidade acima, e a linha
    `docker exec -it` explicada como incantação com prazo de validade.
14. **Da teoria aos Cenários**.

### Diagramas

Seguem a [ADR 0004](../../adr/0004-diagramas-declarativos-no-conteudo.md): o conteúdo
nunca carrega SVG, coordenada, classe CSS ou JSX. Três ids novos entram no catálogo
fechado de `frontend/src/visuaisDoDiagrama.ts`:

- `fronteiras-kernel` — espaço de usuário, chamada de sistema, kernel;
- `arvore-de-processos` — PID 1 e a descendência;
- `permissao-octal` — dono, grupo e outros sobre o mesmo arquivo.

Reusa `fronteiras-runtime` na seção 11.

### Questionário

Doze situações de decisão, nenhuma de decorar sintaxe, cada uma com `revisar` apontando
para a seção correspondente:

1. o processo não morre com `kill`;
2. o arquivo é do grupo certo e mesmo assim o colega não escreve;
3. `df` diz cheio e `du` diz vazio;
4. o serviço sobe na mão e não sobe no boot;
5. onde procurar a razão de um serviço ter morrido às 3h;
6. o comando funciona para você e não para o cron;
7. dois programas querem a mesma porta;
8. quando `chmod 777` é a resposta errada para a pergunta certa;
9. o pacote instalado é mais antigo que o do site do projeto;
10. o que `sudo` realmente concede;
11. por que o PID 1 de um container não reaproveita processos órfãos;
12. o que este ambiente **não** prova sobre uma máquina de verdade.

## A narrativa

A **Aurora** é uma editora pequena. Todo o funcionamento dela — o site do catálogo, a
entrada de pedidos e o relatório que roda de madrugada — vive em **um servidor Linux
só**, montado anos atrás por alguém que não trabalha mais lá. No Cenário 01 o leitor
recebe o acesso e nenhuma documentação. No Cenário 14 ele resolve um plantão sozinho.

O arco tem um final deliberado: a Aurora chega ao limite do que um servidor operado à mão
aguenta, e a última seção da Trilha nomeia o problema que a Trilha Docker vai resolver. A
Trilha Linux é o mundo **antes** do container, e é por isso que ela vem primeiro.

Não reusa a Mirante da Trilha IaC, de propósito: a Mirante já nasce com container no
Cenário 01 daquela Trilha, e forçar as duas na mesma linha do tempo criaria uma
contradição que o leitor perceberia.

Personagens são referidos por papel, não por pronome.

## Grade de Cenários

### Ato I — O chão

| # | Cenário | Dif. | O que ensina | Evidência |
|---:|---|---|---|---|
| 01 | Você herdou um servidor | Guiado | entrar no ambiente, `pwd`/`ls`/`cd`, FHS, ler `/etc/os-release`, `stat`, onde as coisas moram | `arquivo_linux` sobre o inventário que o leitor escreve |
| 02 | Todo arquivo tem dono | Guiado | usuário, grupo, octal, `chown`, `chmod`, `umask`, setgid em diretório compartilhado | `arquivo_linux` com modo, dono e grupo |
| 03 | O texto é a interface | Guiado | pipe, redirecionamento, stdout contra stderr, `grep`/`cut`/`sort`/`uniq`/`wc`, código de saída | `comando_produz` sobre o relatório derivado do log |

### Ato II — Processos e pacotes

| # | Cenário | Dif. | O que ensina | Evidência |
|---:|---|---|---|---|
| 04 | Quem está consumindo a máquina | Assistido | `ps`, árvore, PID e PPID, `kill`, `SIGTERM` contra `SIGKILL` | `comando_produz` sobre a ausência do processo e a presença do substituto |
| 05 | O programa que não morre | Assistido | órfão e zumbi, reaproveitamento pelo PID 1, background e `nohup` | `comando_produz` sobre o PPID do processo sobrevivente |
| 06 | De onde vem um programa | Assistido | `apt`, repositório, `dpkg -l`, `$PATH`, `which`, versão empacotada | `comando_produz` sobre a versão instalada |
| 07 | O disco encheu | Assistido | `df` contra `du`, inode, arquivo apagado com descritor aberto, rotação | `comando_produz` sobre o espaço liberado e o serviço de volta |

### Ato III — Serviços e log

| # | Cenário | Dif. | O que ensina | Evidência |
|---:|---|---|---|---|
| 08 | Um programa vira serviço | Guiado | unit file, `ExecStart`, `daemon-reload`, `enable` contra `start` | `servico_systemd` ativo e habilitado, `http_responde` na 8040 |
| 09 | O serviço que não sobe | Autônomo | `systemctl status`, `journalctl -u`, código de saída, `Restart=`, `After=`/`Requires=` | `servico_systemd`, `http_responde` |
| 10 | O trabalho das três da manhã | Assistido | timer do systemd contra cron, `OnCalendar`, `list-timers`, o timer é uma unit como outra | `servico_systemd` sobre o timer, `arquivo_linux` sobre o artefato gerado |
| 11 | Log não é arquivo de texto | Assistido | journald, `-u`, `-p`, `--since`, prioridade, persistência | `comando_produz` sobre a consulta ao journal |

### Ato IV — Rede, acesso e plantão

| # | Cenário | Dif. | O que ensina | Evidência |
|---:|---|---|---|---|
| 12 | Onde este pacote vai dar | Assistido | `ip addr`, `ip route`, `ss -tlnp`, porta em escuta, resolução de nome | `comando_produz` sobre a porta e o dono dela, `http_responde` |
| 13 | Acesso mínimo | Autônomo | `sudo` e `/etc/sudoers.d`, por que 440, capabilities, processo não-root servindo | `arquivo_linux` no modo 440, `comando_produz` sobre a capability e o usuário do processo |
| 14 | O plantão | Mestre | falhas combinadas: unit mascarada, permissão errada, disco tomado por log não rotacionado e porta ocupada pelo processo errado | as três famílias juntas, sem dica no texto |

Distribuição de Dificuldade: quatro `Guiado`, sete `Assistido`, dois `Autônomo`, um
`Mestre`. O peso em `Guiado` no Ato I é deliberado — esta passa a ser a porta de entrada
da plataforma inteira, e a primeira hora de um leitor novo não é lugar para autonomia.
Estimativa de 18 a 24 horas.

## Mudanças na plataforma

### Backend

1. **`Assercao.ServicoSystemd(container, nome, ativo, habilitado, descricao)`** — roda
   `systemctl is-active --quiet` e `systemctl is-enabled --quiet` dentro do container e
   compara **códigos de saída**, medidos como `0` para ativo e `3` para parado.

   Este tipo existe por uma razão medida, não por elegância: `systemctl is-active` de uma
   unit parada imprime `inactive`, e `"inactive".contains("active")` é verdadeiro. Uma
   Asserção `comando_produz` com `contem: active` **aprovaria um serviço parado** — falso
   positivo estrutural, exatamente o que a
   [ADR 0002](../../adr/0002-um-cenario-ativo-por-vez.md) existe para impedir.

2. **`Assercao.ArquivoLinux(container, caminho, modo, dono, grupo, descricao)`** — roda
   `stat -c "%a %U %G"` e compara campo a campo. Campos nulos não são verificados, como
   `ContainerConfiguracao` já faz.

   Nos dois casos o `container` é **injetado pelo backend** a partir do frontmatter
   `containerLinux`, e o `verificacao.yaml` não o declara — mesma regra que o contexto e o
   namespace seguem nas Asserções Kubernetes, que o endpoint e a região seguem em
   `aws_consulta` e que o diretório segue nas de Terraform. Um Cenário não pode apontar a
   Verificação para um container arbitrário da máquina.

3. O `switch` exaustivo do `MotorDeVerificacao` quebra a compilação nos dois casos novos.
   Isso é proposital e não deve ser resolvido com `default`.

4. **`LeitorDeCenario`** ganha o frontmatter `containerLinux`, e `Cenario` ganha o campo
   mais `usaLinux()`. A presença do campo é o que habilita os dois tipos novos, com um
   `exigirContainerLinux` no mesmo molde de `exigirTerraform` e `exigirMinistack`.

5. **`ordem` no `trilha.yaml`**, conforme a
   [ADR 0005](../../adr/0005-ordem-das-trilhas-e-recomendacao.md): `LeitorDeTrilha` passa
   a exigir o campo, `Trilha` ganha o campo, o comparator vira
   `Comparator.comparingInt(Trilha::ordem).thenComparing(Trilha::id)`, e as quatro
   `trilha.yaml` existentes declaram `ordem` no mesmo commit.

6. **Nada muda no `GerenciadorDeCenarioAtivo`.** Foi medido: `docker compose up -d
   --build` sobe o systemd e `docker compose down -v` derruba tudo. A Trilha reusa
   `projetoCompose` exatamente como os Cenários 07 a 11 da Trilha Docker.

### Ordem das Asserções

Uma Asserção de efeito visível — `http_responde` na porta do serviço — vem sempre
acompanhada de uma Asserção de configuração: `servico_systemd` provando que o serviço
está **habilitado**, e não apenas rodando porque o leitor o iniciou à mão. Serviço certo
por caminho errado não é Cenário concluído, e é a diferença entre `start` e `enable` que
o Cenário 08 existe para ensinar.

### Frontend

- três `visual` novos em `visuaisDoDiagrama.ts`;
- `node frontend/scripts-checar-conteudo.mjs linux` passa a validar os diagramas e os
  links `revisar` do Questionário, e entra no `ci.yml` ao lado das quatro Trilhas que já
  são checadas lá.

### Conteúdo

- `content/linux/trilha.yaml` com `ordem: 1`, `fundamentos.md` e `questionario.yaml`;
- catorze diretórios `content/linux/NN-slug/` com `cenario.md`, `verificacao.yaml` e
  `workspace/`, cada um carregando o mesmo `Dockerfile` e um `compose.yaml`.

### Documentação

- README: seção de preparo da Trilha Linux, o bloco de portas **8040–8049**, os dois
  tipos de Asserção novos, o frontmatter `containerLinux`, o campo `ordem` e o aviso de
  que o primeiro Iniciar leva cerca de 68 s construindo a imagem;
- CONTEXT.md não muda: o vocabulário já contempla a Trilha Linux.

## Decomposição da implementação

Cada etapa com seu próprio plano, cada uma entregando algo utilizável:

1. **Plataforma** — as duas Asserções, o frontmatter `containerLinux`, o campo `ordem` e
   a reordenação das quatro Trilhas existentes, o alvo `linux` do
   `scripts-checar-conteudo.mjs` e a imagem base. Termina com o Cenário 08 no ar como
   prova de que o caminho inteiro funciona, porque ele é o que exercita os dois tipos
   novos de uma vez.
2. **Fundamentos** — artigo, três diagramas novos e Questionário.
3. **Ato I** — Cenários 01 a 03.
4. **Ato II** — Cenários 04 a 07.
5. **Ato III** — Cenários 09 a 11, já que o 08 sai na etapa 1.
6. **Ato IV** — Cenários 12 a 14.

A etapa 1 é pré-requisito de todas as outras. As etapas 3 a 6 são sequenciais por causa
da narrativa: cada Cenário parte do servidor que o anterior deixou.

## Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| `cgroup: host` sumir numa atualização do Docker Desktop | Trilha inteira para de subir | Um teste no estilo do `CatalogoRealTest` que sobe a imagem e exige `is-system-running` igual a `running` |
| Dockerfile divergir entre Cenários | Cada Iniciar volta a custar 68 s | Um teste de conteúdo que compara o hash do `Dockerfile` de todos os Cenários da Trilha |
| Escopo virar enciclopédia | Trilha não termina | A grade de 14 está fechada neste documento; assunto novo entra como Fundamentos ou não entra |
| Tornar `ordem` obrigatório | Catálogo inteiro deixa de carregar | As quatro `trilha.yaml` existentes mudam no mesmo commit; `CatalogoDeTrilhasTest` e `CatalogoRealTest` cobrem |

## Fora de escopo

Kernel, boot, initramfs, módulo, disco, LVM, `dmesg` e firewall do host não são
exercitados: um container compartilha o kernel da VM e não os expõe honestamente. Entram
nos Fundamentos como mapa do território, com a tabela de fidelidade.

SSH também fica de fora. O leitor já está na máquina pelo `docker exec`, e um segundo
caminho de acesso acrescentaria um daemon, um par de chaves e uma porta sem ensinar nada
que os Cenários de serviço e de acesso mínimo não ensinem melhor.

Shell scripting avançado — funções, `trap`, `set -euo pipefail` — fica para uma segunda
rodada. O Ato I ensina o suficiente para operar, e transformar a Trilha em curso de
programação em Bash desviaria do objetivo.

## Validação antes de publicar

Todos os itens bloqueantes foram medidos em 2026-08-21. **Nenhum bloqueio permanece
aberto.**

1. **`docker exec -it` como sessão interativa de verdade no PowerShell** — medido no
   terminal do autor, com aprovação nos seis passos: TTY em `/dev/pts/0`, `TERM=xterm`,
   `locale charmap=UTF-8`, `stty size` acompanhando o redimensionamento da janela
   (`30 120` → `30 146`, provando que o SIGWINCH atravessa), acento digitado voltando
   íntegro, `less` e `nano` abrindo e saindo limpos, e — o critério mais importante —
   `Ctrl-C` atingindo **somente** o processo em primeiro plano, com o `jobs` seguinte
   mostrando `[1]+ Running sleep 300 &`;
2. `cgroup: host` via Compose entregando `is-system-running` igual a `running` e nenhuma
   unit falha — medido;
3. `systemctl mask tmp.mount` resolvendo o `degraded` do Ubuntu 26.04 — medido;
4. `stat -c "%a %U %G"` devolvendo `2770 root ana` — medido;
5. `is-active --quiet` e `is-enabled --quiet` devolvendo `0` e `3` — medido;
6. `docker compose down -v` removendo o container e a rede — medido;
7. segundo `up --build` em 6 s com o cache quente — medido;
8. o bloco 8040–8049 livre na máquina — medido, apenas a 8053 está ocupada na faixa.
