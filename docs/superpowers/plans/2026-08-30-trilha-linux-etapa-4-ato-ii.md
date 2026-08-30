# Trilha Linux — Etapa 4: Ato II, Cenários 04 a 07

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fechar o Ato II — a máquina deixa de ser um disco com arquivos e passa a ser um
lugar onde **coisas rodam**. Os quatro Cenários respondem à primeira das quatro perguntas
do modelo mental, "quem está pedindo", e terminam no incidente que mais confunde quem
opera Linux: um disco que diz estar cheio sem ter nada dentro.

**Architecture:** Nenhuma Asserção nova, nenhuma mudança de motor e **nenhuma mudança no
`Dockerfile`** — os quatro Cenários são conteúdo no disco e reusam `arquivo_linux` e
`comando_produz`. A única novidade de infraestrutura é o `compose.yaml` do Cenário 07, que
monta `/var/log/aurora` como um tmpfs de 8 MB com 2048 inodes. É o que torna "o disco
encheu" ensinável sem `--privileged` e sem tocar no disco real de quem estuda.

**Tech Stack:** Ubuntu 26.04 (pinado por digest) · systemd 259 · Docker Compose · tmpfs ·
`procps` · `lsof` · `apt`/`dpkg` · SnakeYAML · JUnit 5 · AssertJ

**Spec:** [`docs/superpowers/specs/2026-08-21-trilha-linux-design.md`](../specs/2026-08-21-trilha-linux-design.md)

## Global Constraints

- **Todas as restrições das Etapas [1](./2026-08-25-trilha-linux-etapa-1-plataforma.md),
  [2](./2026-08-25-trilha-linux-etapa-2-fundamentos.md) e
  [3](./2026-08-29-trilha-linux-etapa-3-ato-i.md) continuam valendo.**
- **O `Dockerfile` não muda.** Os quatro `workspace/Dockerfile` novos são cópia byte a
  byte do que já está no disco, e o `SubstratoLinuxTest` reprova o contrário. Programas de
  apoio que um Cenário precise nascem no próprio `cenario.md`, por heredoc, como o
  Cenário 08 já faz com a unit.
- Dificuldade dos quatro: **`assistido`** — o design fecha o Ato I no `guiado` e sobe um
  degrau aqui. Na prática: o Cenário diz **o que** investigar e **por quê**, mas não
  entrega a linha de comando pronta em todo passo.
- Nenhum dos quatro publica porta. O bloco 8040–8049 continua reservado à Trilha.
- `containerLinux: learning-infra-linux` e `projetoCompose: linux-NN` no frontmatter; o
  `verificacao.yaml` nunca declara `container`.
- Todo texto em **português**; personagens por papel, nunca por pronome.
- A narrativa é contínua e parte do que o Ato I deixou: o leitor já tem o inventário, já
  organizou o editorial e já sabe que os erros se concentram de madrugada.

## Medições que fundamentam este plano

Feitas em 2026-08-30 contra a imagem da Etapa 3, com Docker Engine 29.6.1.

**O que inviabiliza a leitura ingênua do Cenário 07:**

| Medição | Resultado |
|---|---|
| `df -h /` dentro do container | `overlay 1007G, 23G usado, 934G livre` |

O `/` do container é o disco da VM do Docker Desktop. "Encher o disco" ali é impossível na
prática e **perigoso na intenção** — seria o disco real de quem estuda. Daí o tmpfs.

**O que o tmpfs de 8 MB entrega, medido:**

| Situação | Saída |
|---|---|
| `df -h /var/log/aurora` no início | `tmpfs 8.0M 0 8.0M 0%` |
| `df -i /var/log/aurora` no início | `Inodes 2048, IUsed 1, IFree 2047` |
| escrita além do limite | `dd: IO error: No space left on device` |
| log de 60.000 linhas + arquivo de enchimento | `8.0M 8.0M 0 100%` |
| **apagar o log com um processo segurando** | `du -sh` diz **`0`**, `df -h` diz **`4.4M ... 55%`** |
| `lsof +L1` | `python3 68 root 3r REG 0,508 4602181 0 2 /var/log/aurora/pedidos.log (deleted)` |
| matar o processo que segurava | `df -h` volta a **`0 8.0M 0%`** |
| criar 2100 arquivos vazios | falha no **2047º**: `No space left on device` |
| `df -i` depois disso | `2048 2048 0 100%` |
| `df -h` **no mesmo momento** | `8.0M 0 8.0M 0%` — **disco vazio** |
| escrever num arquivo **já existente** com inodes esgotados | funciona |

A penúltima linha é o Cenário inteiro: *"No space left on device" com o disco vazio*. E a
última é a prova de que o problema é inode, não espaço — o detalhe que separa diagnosticar
de chutar.

**Processos, medido:**

| Situação | Saída |
|---|---|
| PPID de um processo criado por `docker exec` | `0` |
| script com `trap "" TERM`, após `kill` | continua **vivo** |
| o mesmo, após `kill -9` | `Killed`, morto |
| filho cujo pai morreu | `PID 187, PPID 1` — adotado pelo PID 1 |
| quem é o PID 1 | `systemd` |
| filho que saiu sem o pai chamar `wait()` | `207 205 Z python3` |
| o mesmo no `ps aux` | `root 247 ... Z ... [python3] <defunct>` |

**Pacotes, medido:**

| Situação | Saída |
|---|---|
| `apt-get update` | 25,7 MB em **9 s** — exige rede no momento do Cenário |
| `/var/lib/apt/lists` depois do update | **40 MB** — por isso não são embutidas na imagem |
| `apt-cache policy jq` **antes** do update | `Candidate: 1.8.1-4ubuntu2`, enganoso: vem do dpkg, não do repositório |
| `apt-cache policy jq` **depois** do update | `Installed: (none)`, `Candidate: 1.8.1-4ubuntu2` |
| `jq --version` após instalar | `jq-1.8.1` |
| `dpkg-query -W -f='${Package} ${Version}' jq` | `jq 1.8.1-4ubuntu2` |
| `dpkg-query -W -f='${Status}' jq` | `install ok installed` |
| `dpkg -L jq` | `/usr/bin/jq` entre os caminhos |

O sufixo `ubuntu2` na versão é o gancho da lição: o número não é o do projeto original, é
o do **pacote que a Ubuntu montou** a partir dele.

## O tmpfs mascara a semente — e o Cenário 07 assume isso

Montar um tmpfs em `/var/log/aurora` esconde o `pedidos.log` que a imagem semeia. Medido e
esperado: um mount cobre o que existia embaixo.

Isso é tratado **no texto**, não escondido. O Cenário 07 abre estabelecendo que na Aurora o
`/var/log/aurora` é uma **partição própria** — prática comum e feita exatamente para que
log não derrube a máquina inteira — e o leitor gera o log do dia dentro dela. Nenhuma
outra Trilha ou Cenário é afetado: só o `compose.yaml` do 07 tem esse mount, e a ADR 0002
garante um Cenário ativo por vez.

## Uma tensão do design, resolvida

A grade descreve a evidência do Cenário 07 como "o espaço liberado **e o serviço de
volta**". Mas serviço só existe a partir do Cenário 08 — o 08 abre dizendo, com todas as
letras, que "até aqui a Aurora tocava seus programas na mão".

Resolução, sem mexer no design: no Cenário 07 o que volta é o **programa que grava os
pedidos**, rodando à mão, como tudo na Aurora antes do 08. A Asserção cobra que ele
consegue escrever de novo. A palavra "serviço" não aparece no Cenário 07.

## Estrutura de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `content/linux/04-quem-esta-consumindo-a-maquina/` | `cenario.md`, `verificacao.yaml`, `workspace/Dockerfile`, `workspace/compose.yaml` |
| `content/linux/05-o-programa-que-nao-morre/` | idem |
| `content/linux/06-de-onde-vem-um-programa/` | idem |
| `content/linux/07-o-disco-encheu/` | idem, com `compose.yaml` **diferente** — o tmpfs limitado |
| `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java` | contagens do catálogo |
| `README.md` | o Ato II publicado |

## Contagens ao fim da Etapa

Catálogo: **69 Cenários**, sendo **8** da Trilha Linux. Asserções da Trilha: **20** — as 11
de hoje mais 2 no Cenário 04, 2 no 05, 2 no 06 e 3 no 07.

## As formas de Asserção deste Ato, já validadas

Todas foram rodadas contra o container antes de entrarem aqui. O padrão é sempre o mesmo e
existe por uma razão: `comando_produz` **reprova quando o comando falha**, e ferramentas de
processo como `pgrep` saem com código 1 quando não acham nada. Uma Asserção ingênua
reprovaria por "a checagem não completou" em vez de dizer o que está errado. Por isso todo
comando abaixo termina imprimindo uma palavra e saindo com 0.

```sh
# processo ausente
if pgrep -x importador-pedidos >/dev/null; then echo presente; else echo ausente; fi

# PPID exatamente 1 — sem casar com 12, 13, 100...
if [ "$(ps -o ppid= -C vigia | tr -d ' ' | head -1)" = "1" ]; then echo adotado-pelo-pid-1; else echo tem-pai-vivo; fi

# nenhum zumbi
if ps -eo stat= | grep -q "^Z"; then echo tem-zumbi; else echo sem-zumbi; fi

# partição saiu do vermelho
P=$(df --output=pcent /var/log/aurora | tail -1 | tr -d ' %'); if [ "$P" -lt 50 ]; then echo espaco-liberado; else echo ainda-cheio; fi

# nenhum descritor preso a arquivo apagado
if lsof +L1 2>/dev/null | grep -q /var/log/aurora; then echo descritor-preso; else echo sem-descritor-preso; fi
```

E uma armadilha medida, para quem for escrever Asserção nova: `dpkg-query -W -f='${Status}'`
**não** pode passar por um shell intermediário, ou `${Status}` é expandido para vazio antes
de chegar ao `dpkg-query`. O motor passa `argv` direto e não sofre disso; quem testar à mão
precisa de aspas simples.

---

### Task 1: Cenário 04 — Quem está consumindo a máquina

Abre o Ato II com a pergunta mais concreta possível: a máquina está lenta, e a única
ferramenta é olhar quem está rodando. Ensina `ps`, a árvore de PID e PPID, e a diferença
entre pedir para um processo morrer e obrigá-lo.

**Files:**
- Create: `content/linux/04-quem-esta-consumindo-a-maquina/cenario.md`
- Create: `content/linux/04-quem-esta-consumindo-a-maquina/verificacao.yaml`
- Create: `content/linux/04-quem-esta-consumindo-a-maquina/workspace/Dockerfile`
- Create: `content/linux/04-quem-esta-consumindo-a-maquina/workspace/compose.yaml`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: a máquina da Etapa 3, com `/srv/inventario.md` e o log semeado.
- Produces: o vocabulário de sinal que o Cenário 05 usa e o `relatorio-aurora` que o
  Cenário 07 vai reaproveitar.

- [ ] **Step 1: Atualizar as contagens e ver o teste falhar**

Modify `CatalogoRealTest.java`:

```java
        assertThat(cenarios).hasSize(66);
```

```java
        assertThat(linux).hasSize(5);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(13);
```

Run: `cd backend; ./mvnw test -Dtest=CatalogoRealTest`
Expected: FAIL — `expected size: 66 but was: 65`.

- [ ] **Step 2: Criar o workspace**

```powershell
mkdir content\linux\04-quem-esta-consumindo-a-maquina\workspace
Copy-Item content\linux\08-um-programa-vira-servico\workspace\Dockerfile `
          content\linux\04-quem-esta-consumindo-a-maquina\workspace\Dockerfile
Copy-Item content\linux\01-voce-herdou-um-servidor\workspace\compose.yaml `
          content\linux\04-quem-esta-consumindo-a-maquina\workspace\compose.yaml
```

O `compose.yaml` do Cenário 01 é o padrão do Ato: sem `ports:`, sem tmpfs extra.

- [ ] **Step 3: Escrever a Verificação**

Create `content/linux/04-quem-esta-consumindo-a-maquina/verificacao.yaml`:

```yaml
asercoes:
  - tipo: comando_produz
    descricao: o importador travado não está mais consumindo a máquina
    comando: ["docker", "exec", "learning-infra-linux", "sh", "-c",
              "if pgrep -x importador-pedidos >/dev/null; then echo presente; else echo ausente; fi"]
    contem: ausente

  - tipo: arquivo_linux
    caminho: /srv/relatorio-noturno.txt
    dono: root
    descricao: o relatório que o importador impedia finalmente saiu
```

A segunda Asserção é o "substituto" que o design pede: matar o processo errado não é o
objetivo, é o meio. O objetivo era o relatório sair.

- [ ] **Step 4: Escrever o Cenário**

Frontmatter exato:

```yaml
---
id: linux/04-quem-esta-consumindo-a-maquina
titulo: Quem está consumindo a máquina
dificuldade: assistido
projetoCompose: linux-04
containerLinux: learning-infra-linux
---
```

Seis seções `##`. Os comandos abaixo são verbatim e foram medidos.

1. `## A pressão` — o relatório noturno da Aurora não saiu esta madrugada, e quem tentou
   rodá-lo à mão desistiu porque a máquina "está impossível". Nenhuma mensagem de erro,
   nenhum log. Só lentidão. A promessa: a máquina sabe responder quem está fazendo isso.

2. `## Quem está rodando` — apresente `ps` com a combinação que resolve o dia a dia:

   ```bash
   ps aux | head -5
   ps aux --sort=-%cpu | head -5
   ```

   Explique as colunas que importam — `USER`, `PID`, `%CPU`, `STAT`, `COMMAND` — e diga que
   `ps` é uma **fotografia**, não um filme: ele mostra o instante em que você perguntou.

3. `## Reproduza o problema` — o programa que a Aurora deixou rodando. O leitor o cria,
   porque diagnosticar algo que não está acontecendo não ensina nada:

   ```bash
   tee /usr/local/bin/importador-pedidos > /dev/null <<'EOF'
   #!/bin/bash
   trap "" TERM
   while true; do :; done
   EOF
   chmod +x /usr/local/bin/importador-pedidos
   setsid importador-pedidos </dev/null >/dev/null 2>&1 &
   ```

   Duas coisas para nomear no texto. O `chmod +x` é o bit de execução do Cenário 02,
   aparecendo agora com consequência. E o `trap "" TERM` manda o programa **ignorar** o
   pedido de encerramento — é a linha que faz o resto do Cenário existir, e o leitor
   precisa ter visto que ela está lá.

   Agora encontre o culpado:

   ```bash
   ps aux --sort=-%cpu | head -3
   pgrep -x importador-pedidos
   ```

4. `## A árvore` — todo processo tem um pai:

   ```bash
   ps -eo pid,ppid,comm --forest | head -15
   ps -o pid,ppid,comm -C importador-pedidos
   ```

   Explique `PID` e `PPID` e mostre que a árvore inteira sobe até o `1`. Quem é o `1`:

   ```bash
   ps -o pid,comm -p 1
   ```

   Responde `systemd`. Ele é o primeiro processo que o kernel cria e o ancestral de todos
   os outros — e o Cenário 05 vive dessa propriedade.

5. `## Pedir e obrigar` — o miolo. Peça primeiro:

   ```bash
   pkill -x importador-pedidos
   sleep 2
   pgrep -x importador-pedidos
   ```

   O `pgrep` ainda responde um PID: **o processo continua vivo**. Nomeie o que aconteceu.
   `kill` não mata nada; ele **envia um sinal**. O padrão é o `SIGTERM`, que significa "por
   favor, encerre" — e um programa pode ignorá-lo, que é exatamente o que aquele `trap`
   faz. É por isso que `SIGTERM` é o padrão: ele dá ao programa a chance de fechar arquivo,
   terminar transação e sair inteiro.

   Agora obrigue:

   ```bash
   pkill -9 -x importador-pedidos
   sleep 2
   pgrep -x importador-pedidos && echo "ainda vivo" || echo "morreu"
   ```

   O `-9` é o `SIGKILL`, e ele não é entregue ao programa: quem age é o kernel. Nenhum
   `trap` intercepta, nenhuma limpeza acontece. Diga a regra de ofício em uma frase:
   **`SIGKILL` é o último recurso, não o primeiro** — quem começa por ele troca um problema
   de lentidão por um arquivo corrompido pela metade.

   Mostre o catálogo de sinais de passagem:

   ```bash
   kill -l | head -3
   ```

6. `## O relatório que faltava` — com a máquina livre, o trabalho de ontem finalmente roda:

   ```bash
   tee /usr/local/bin/relatorio-aurora > /dev/null <<'EOF'
   #!/bin/bash
   # Resumo dos pedidos com falha, por cliente.
   grep "status=500" /var/log/aurora/pedidos.log \
     | cut -d" " -f3 | cut -d= -f2 | sort | uniq -c | sort -rn
   EOF
   chmod +x /usr/local/bin/relatorio-aurora
   relatorio-aurora > /srv/relatorio-noturno.txt
   cat /srv/relatorio-noturno.txt
   ```

   É o encadeamento do Cenário 03, agora com nome e endereço em `/usr/local/bin` — e o
   leitor acabou de ver por que esse diretório está no `$PATH`, assunto do Cenário 06.

   Feche dizendo o que a Verificação cobra: o importador ausente **e** o relatório
   existindo. Matar o processo certo era o meio; o relatório era o objetivo.

- [ ] **Step 5: Rodar o Cenário de verdade**

```powershell
cd content\linux\04-quem-esta-consumindo-a-maquina\workspace
docker compose -p linux-04 up -d --build
docker exec -it learning-infra-linux bash
```

Execute todas as seções na ordem. Confira que o `pkill` sem `-9` **realmente deixa o
processo vivo** — se ele morrer, o `trap` não pegou e o Cenário perdeu a lição inteira.

Valide as Asserções:

```powershell
docker exec learning-infra-linux sh -c "if pgrep -x importador-pedidos >/dev/null; then echo presente; else echo ausente; fi"
docker exec learning-infra-linux stat -c "%a %U %G" /srv/relatorio-noturno.txt
```

Expected: `ausente` e um `stat` com dono `root`.

- [ ] **Step 6: Rodar a suíte, derrubar e commitar**

```powershell
cd backend; ./mvnw test
cd ..\content\linux\04-quem-esta-consumindo-a-maquina\workspace; docker compose -p linux-04 down -v
```

```bash
git add content/linux/04-quem-esta-consumindo-a-maquina/ \
        backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: Cenário 04 da Trilha Linux ensina ps, árvore e sinais"
```

---

### Task 2: Cenário 05 — O programa que não morre

O Cenário que desmonta duas confusões que quase todo mundo carrega: que um processo órfão
fica "solto" na máquina, e que um zumbi consome recursos. Nenhuma das duas é verdade, e
entender por quê é o que faz o `PID 1` deixar de ser trivia.

**Files:**
- Create: `content/linux/05-o-programa-que-nao-morre/cenario.md`
- Create: `content/linux/05-o-programa-que-nao-morre/verificacao.yaml`
- Create: `content/linux/05-o-programa-que-nao-morre/workspace/Dockerfile`
- Create: `content/linux/05-o-programa-que-nao-morre/workspace/compose.yaml`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: o vocabulário de PID, PPID e sinal do Cenário 04.
- Produces: a noção de `PID 1` como adotante, que o Cenário 08 reusa ao falar de systemd.

- [ ] **Step 1: Atualizar as contagens e ver o teste falhar**

```java
        assertThat(cenarios).hasSize(67);
```

```java
        assertThat(linux).hasSize(6);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(15);
```

Run: `cd backend; ./mvnw test -Dtest=CatalogoRealTest`
Expected: FAIL — `expected size: 67 but was: 66`.

- [ ] **Step 2: Criar o workspace**

```powershell
mkdir content\linux\05-o-programa-que-nao-morre\workspace
Copy-Item content\linux\08-um-programa-vira-servico\workspace\Dockerfile `
          content\linux\05-o-programa-que-nao-morre\workspace\Dockerfile
Copy-Item content\linux\01-voce-herdou-um-servidor\workspace\compose.yaml `
          content\linux\05-o-programa-que-nao-morre\workspace\compose.yaml
```

- [ ] **Step 3: Escrever a Verificação**

Create `content/linux/05-o-programa-que-nao-morre/verificacao.yaml`:

```yaml
asercoes:
  - tipo: comando_produz
    descricao: o vigia sobreviveu ao pai e foi adotado pelo PID 1
    comando: ["docker", "exec", "learning-infra-linux", "sh", "-c",
              "if [ \"$(ps -o ppid= -C vigia | tr -d ' ' | head -1)\" = \"1\" ]; then echo adotado-pelo-pid-1; else echo tem-pai-vivo; fi"]
    contem: adotado-pelo-pid-1

  - tipo: comando_produz
    descricao: nenhum zumbi ficou para trás
    comando: ["docker", "exec", "learning-infra-linux", "sh", "-c",
              "if ps -eo stat= | grep -q '^Z'; then echo tem-zumbi; else echo sem-zumbi; fi"]
    contem: sem-zumbi
```

A comparação com `=` em vez de `contem: 1` é deliberada: `contem` casaria com `12`, `13` ou
`100`, e a Asserção aprovaria um processo cujo pai continua vivo.

- [ ] **Step 4: Escrever o Cenário**

Frontmatter:

```yaml
---
id: linux/05-o-programa-que-nao-morre
titulo: O programa que não morre
dificuldade: assistido
projetoCompose: linux-05
containerLinux: learning-infra-linux
---
```

Cinco seções `##`:

1. `## A pressão` — depois do incidente do Cenário 04, alguém da Aurora perguntou o óbvio:
   se o processo que travou a máquina tinha um pai, e o pai era a sessão de quem o iniciou,
   por que ele continuou rodando depois que a pessoa fechou o terminal? A resposta é este
   Cenário.

2. `## O processo que fica` — comece pelo caso comum, o programa em segundo plano:

   ```bash
   sleep 300 &
   jobs
   ps -o pid,ppid,comm -C sleep
   ```

   O `&` põe o programa em **segundo plano**: ele roda, e o shell devolve o prompt. O
   `jobs` lista o que este shell está tocando. Repare que o `PPID` do `sleep` é o PID do
   seu próprio `bash`.

   Agora a pergunta que importa: e se o pai morrer?

3. `## Órfão não é solto` — faça o pai morrer de propósito:

   ```bash
   setsid bash -c 'sleep 300 & echo "filho=$!"; echo "pai=$$"' > /tmp/orfao.txt 2>&1
   cat /tmp/orfao.txt
   ```

   O `setsid` cria a sessão separada e o `bash -c` sai imediatamente, deixando o `sleep`
   sem pai. Veja quem assumiu:

   ```bash
   ps -o pid,ppid,comm -C sleep
   ps -o pid,comm -p 1
   ```

   Saída medida: o `sleep` aparece com **`PPID 1`**, e o PID 1 é o `systemd`.

   Nomeie o mecanismo, porque é ele que responde a pergunta da abertura: quando um processo
   morre, o kernel **reatribui** os filhos dele ao PID 1. Órfão não fica solto; órfão troca
   de pai. E isso não é cortesia: existe porque alguém precisa recolher o resultado do
   processo quando ele terminar — o assunto da seção seguinte.

4. `## Zumbi não é o que parece` — o outro lado do mesmo mecanismo:

   ```bash
   tee /usr/local/bin/vigia > /dev/null <<'EOF'
   #!/usr/bin/env python3
   # Vigia do catálogo: cria um filho para a checagem e esquece dele.
   import os, sys, time
   filho = os.fork()
   if filho == 0:
       os._exit(0)
   print(f"pai={os.getpid()} filho={filho}", flush=True)
   if len(sys.argv) > 1 and sys.argv[1] == "--corrigido":
       os.wait()
   time.sleep(3600)
   EOF
   chmod +x /usr/local/bin/vigia
   setsid vigia </dev/null >/tmp/vigia.txt 2>&1 &
   sleep 2
   cat /tmp/vigia.txt
   ```

   Veja o filho:

   ```bash
   ps -eo pid,ppid,stat,comm | awk '$3 ~ /Z/'
   ps aux | grep defunct | grep -v grep
   ```

   Saída medida: `207 205 Z python3`, e no `ps aux` o mesmo processo aparece como
   `[python3] <defunct>`.

   Desmonte a confusão com precisão: o zumbi **já terminou**. Ele não tem memória, não tem
   CPU, não tem arquivo aberto. O que sobrou é **uma linha na tabela de processos** com o
   código de saída dele, esperando alguém ler. Quem deveria ler é o pai, chamando `wait()`.
   Este pai não chama — e é esse o bug, não o filho.

   O custo real de um zumbi é o PID ocupado. Um zumbi não é problema; um programa que
   produz zumbis sem parar esgota a tabela de PIDs, e aí a máquina não consegue criar
   processo nenhum.

   Prove que o problema é o pai:

   ```bash
   pkill -x vigia
   sleep 2
   ps -eo pid,ppid,stat,comm | awk '$3 ~ /Z/'
   ```

   O zumbi sumiu junto com o pai — porque, órfão, ele foi adotado pelo PID 1, e o PID 1
   faz o que o pai não fez: recolhe o resultado. É o mesmo mecanismo da seção anterior,
   visto do outro lado.

5. `## Deixe rodando do jeito certo` — o fecho prático. O leitor sobe o vigia corrigido,
   que **chama `wait()`**, e o deixa sobrevivendo à sessão:

   ```bash
   setsid vigia --corrigido </dev/null >/var/log/aurora/vigia.log 2>&1 &
   sleep 2
   ps -o pid,ppid,comm -C vigia
   ps -eo pid,ppid,stat,comm | awk '$3 ~ /Z/'
   ```

   O `vigia` aparece com `PPID 1` e nenhum zumbi restou.

   Nomeie as três peças que fazem um programa sobreviver à sessão, porque é a receita que o
   leitor vai querer amanhã: **`&`** o põe em segundo plano, **`setsid`** o desliga do
   terminal, e o redirecionamento das três saídas garante que ele não morra tentando
   escrever num terminal que não existe mais. O `nohup` é o atalho clássico para parte
   disso — ele ignora o sinal que o shell manda aos filhos ao sair.

   E o gancho: tudo isso é frágil. Um `docker exec` novo, um boot, e nada disso volta. O
   Cenário 08 troca essa receita artesanal por um serviço de verdade.

   Feche dizendo o que a Verificação cobra: o `vigia` vivo **com PPID exatamente 1**, e
   nenhum zumbi na máquina.

- [ ] **Step 5: Rodar o Cenário de verdade**

```powershell
cd content\linux\05-o-programa-que-nao-morre\workspace
docker compose -p linux-05 up -d --build
docker exec -it learning-infra-linux bash
```

Execute tudo na ordem. Dois pontos de atenção, porque são os que costumam falhar:

- o zumbi precisa **aparecer** no `awk '$3 ~ /Z/'`. Se não aparecer, o `os.wait()` foi
  chamado por engano ou o filho ainda não saiu — aumente o `sleep` antes de olhar;
- ao final, o `vigia --corrigido` precisa ter `PPID 1`. Se tiver o PID do seu shell, o
  `setsid` não pegou.

```powershell
docker exec learning-infra-linux sh -c "if [ \"$(ps -o ppid= -C vigia | tr -d ' ' | head -1)\" = \"1\" ]; then echo adotado-pelo-pid-1; else echo tem-pai-vivo; fi"
docker exec learning-infra-linux sh -c "if ps -eo stat= | grep -q '^Z'; then echo tem-zumbi; else echo sem-zumbi; fi"
```

Expected: `adotado-pelo-pid-1` e `sem-zumbi`.

- [ ] **Step 6: Rodar a suíte, derrubar e commitar**

```powershell
cd backend; ./mvnw test
cd ..\content\linux\05-o-programa-que-nao-morre\workspace; docker compose -p linux-05 down -v
```

```bash
git add content/linux/05-o-programa-que-nao-morre/ \
        backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: Cenário 05 da Trilha Linux desmonta órfão e zumbi"
```

---

### Task 3: Cenário 06 — De onde vem um programa

Três Cenários usando comandos e nenhuma pergunta sobre de onde eles vêm. Este responde:
o que é um pacote, o que é um repositório, por que a versão da distribuição não é a do
site do projeto, e como o shell decide qual binário executar.

**Files:**
- Create: `content/linux/06-de-onde-vem-um-programa/cenario.md`
- Create: `content/linux/06-de-onde-vem-um-programa/verificacao.yaml`
- Create: `content/linux/06-de-onde-vem-um-programa/workspace/Dockerfile`
- Create: `content/linux/06-de-onde-vem-um-programa/workspace/compose.yaml`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: o `$PATH` que o Cenário 04 tocou de passagem ao instalar em `/usr/local/bin`.
- Produces: o `jq` instalado — nenhum Cenário posterior depende dele, de propósito.

**Este Cenário exige rede** no momento em que roda: `apt-get update` baixa 25,7 MB em
cerca de 9 s, medido. É a única dependência de rede em tempo de Cenário da Trilha, e ela é
deliberada — embutir as listas na imagem custaria **40 MB** e entregaria um índice velho,
que é justamente o erro que o Cenário ensina a não cometer.

- [ ] **Step 1: Atualizar as contagens e ver o teste falhar**

```java
        assertThat(cenarios).hasSize(68);
```

```java
        assertThat(linux).hasSize(7);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(17);
```

Run: `cd backend; ./mvnw test -Dtest=CatalogoRealTest`
Expected: FAIL — `expected size: 68 but was: 67`.

- [ ] **Step 2: Criar o workspace**

```powershell
mkdir content\linux\06-de-onde-vem-um-programa\workspace
Copy-Item content\linux\08-um-programa-vira-servico\workspace\Dockerfile `
          content\linux\06-de-onde-vem-um-programa\workspace\Dockerfile
Copy-Item content\linux\01-voce-herdou-um-servidor\workspace\compose.yaml `
          content\linux\06-de-onde-vem-um-programa\workspace\compose.yaml
```

- [ ] **Step 3: Escrever a Verificação**

Create `content/linux/06-de-onde-vem-um-programa/verificacao.yaml`:

```yaml
asercoes:
  - tipo: comando_produz
    descricao: o jq está instalado pelo gerenciador de pacotes, não largado no disco
    comando: ["docker", "exec", "learning-infra-linux",
              "dpkg-query", "-W", "-f=${Status}", "jq"]
    contem: install ok installed

  - tipo: comando_produz
    descricao: o jq responde na linha de comando
    comando: ["docker", "exec", "learning-infra-linux", "jq", "--version"]
    contem: jq-1.
```

O `contem: jq-1.` cobra a família da versão e não o número exato: o Cenário não pode
quebrar quando a Ubuntu publicar uma correção de segurança do `jq`.

A primeira Asserção é a que tem conteúdo: um binário copiado para `/usr/local/bin` também
responderia `--version`, mas só um pacote de verdade aparece no banco do `dpkg`.

- [ ] **Step 4: Escrever o Cenário**

Frontmatter:

```yaml
---
id: linux/06-de-onde-vem-um-programa
titulo: De onde vem um programa
dificuldade: assistido
projetoCompose: linux-06
containerLinux: learning-infra-linux
---
```

Cinco seções `##`:

1. `## A pressão` — a Aurora vai passar a receber pedidos em JSON de um parceiro, e o
   relatório noturno precisa ler esse formato. Falta uma ferramenta na máquina. A pergunta
   do Cenário não é "como instalo": é **o que exatamente acontece quando eu instalo**.

2. `## Qual binário o shell escolhe` — comece pelo que o leitor já tem:

   ```bash
   which python3
   type python3
   echo $PATH
   ```

   Saída medida do `$PATH`:

   ```
   /usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
   ```

   Explique que o shell não procura no disco inteiro: ele percorre essa lista **na ordem** e
   pega o primeiro que encontrar. Ligue com o Cenário 04: o `relatorio-aurora` funcionou
   sem caminho completo porque `/usr/local/bin` está na lista — e está **antes** de
   `/usr/bin`, que é como se substitui um programa do sistema por uma versão própria.

   Mostre a diferença entre `which` e `type` num caso concreto, o `cd`:

   ```bash
   type cd
   which cd
   ```

   `cd` é embutido no shell e não tem arquivo nenhum: `type` sabe disso, `which` não acha
   nada. É por isso que `type` é a pergunta mais honesta das duas.

3. `## O índice antes do pacote` — a lição que dá nome ao problema. Pergunte **antes** de
   atualizar o índice:

   ```bash
   apt-cache policy jq
   ```

   A resposta parece útil e é enganosa. Sem índice, o `apt` responde com o que o `dpkg`
   sabe da máquina, não com o que o repositório oferece. Agora busque o índice de verdade:

   ```bash
   apt-get update
   apt-cache policy jq
   ```

   Saída medida depois do update:

   ```
   jq:
     Installed: (none)
     Candidate: 1.8.1-4ubuntu2
   ```

   O `apt-get update` **não instala nada** — ele baixa a lista do que existe. São 25,7 MB e
   uns 9 segundos. Esquecer esse passo é a causa mais comum de "o pacote não existe" e de
   instalar uma versão velha sem perceber.

4. `## Instale e veja onde as coisas foram parar` —

   ```bash
   apt-get install -y jq
   jq --version
   dpkg-query -W -f='${Package} ${Version}\n' jq
   which jq
   ```

   Saídas medidas: `jq-1.8.1`, depois `jq 1.8.1-4ubuntu2`, depois `/usr/bin/jq`.

   Compare os dois números, porque é aqui que mora a lição do Cenário. O programa se
   apresenta como `jq-1.8.1` — é a versão que o projeto publicou. O pacote se chama
   `1.8.1-4ubuntu2` — o sufixo é da **Ubuntu**, que pegou aquele código, compilou para esta
   distribuição, aplicou correções e empacotou. Um número descreve o software; o outro
   descreve o pacote.

   E daí vem a resposta para a pergunta que todo mundo faz: a versão da distribuição é
   quase sempre mais antiga que a do site do projeto, **de propósito**. A Ubuntu congela as
   versões no lançamento e depois só traz correção de segurança, para que uma máquina que
   você não olha há seis meses continue previsível. Instalar direto do site do projeto
   troca essa previsibilidade por atualidade — às vezes vale, e você precisa saber que está
   fazendo a troca.

   Veja o que o pacote colocou no disco:

   ```bash
   dpkg -L jq | head -6
   dpkg -l jq
   ```

   Saída medida do `dpkg -l`, última linha:

   ```
   ii  jq  1.8.1-4ubuntu2  amd64  lightweight and flexible command-line JSON processor
   ```

   Explique o `ii`: o primeiro `i` é o estado desejado (*install*), o segundo é o estado
   real (*installed*). Quando os dois divergem, você tem um pacote pela metade — e é a
   primeira coisa a checar quando algo "está instalado" mas não funciona.

5. `## Pacote contra arquivo largado no disco` — o fecho conceitual. Um binário copiado
   para `/usr/local/bin` roda igual e não é a mesma coisa:

   ```bash
   dpkg -S /usr/bin/jq
   dpkg -S /etc/aurora/catalogo.conf
   ```

   O primeiro responde `jq: /usr/bin/jq`; o segundo diz que nenhum pacote é dono daquele
   caminho — a configuração da Aurora foi posta ali à mão, anos atrás, por alguém. O
   gerenciador sabe de onde veio cada arquivo dele, sabe desinstalar sem deixar
   resto, e sabe atualizar tudo de uma vez. Um arquivo largado no disco não tem nada disso —
   e seis meses depois ninguém sabe mais quem o colocou lá nem qual versão é.

   Feche dizendo o que a Verificação cobra e por quê: não basta o `jq --version` responder,
   porque um binário copiado na mão também responderia. A Asserção pergunta ao `dpkg` se
   ele reconhece o pacote como instalado — a diferença entre o programa existir e o
   programa ser **gerenciado**.

- [ ] **Step 5: Rodar o Cenário de verdade**

```powershell
cd content\linux\06-de-onde-vem-um-programa\workspace
docker compose -p linux-06 up -d --build
docker exec -it learning-infra-linux bash
```

Atenção a um detalhe já resolvido no texto, para não reintroduzi-lo: o `dpkg -S` da seção 5
aponta para `/etc/aurora/catalogo.conf`, que a **imagem** cria e nenhum pacote é dono. Não
o troque por `/usr/local/bin/relatorio-aurora` — aquele arquivo nasce no Cenário 04 e cada
Cenário sobe uma máquina limpa, então ele não existe aqui.

```powershell
docker exec learning-infra-linux dpkg-query -W -f='${Status}' jq
docker exec learning-infra-linux jq --version
```

Expected: `install ok installed` e `jq-1.8.1`.

- [ ] **Step 6: Rodar a suíte, derrubar e commitar**

```powershell
cd backend; ./mvnw test
cd ..\content\linux\06-de-onde-vem-um-programa\workspace; docker compose -p linux-06 down -v
```

```bash
git add content/linux/06-de-onde-vem-um-programa/ \
        backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: Cenário 06 da Trilha Linux explica pacote, repositório e PATH"
```

---

### Task 4: Cenário 07 — O disco encheu

O melhor Cenário do Ato e o que mais depende de medição. Ele entrega dois incidentes que
todo mundo encontra e quase ninguém explica: um disco que continua cheio depois de você
apagar o arquivo, e um "No space left on device" com o disco vazio.

**Files:**
- Create: `content/linux/07-o-disco-encheu/cenario.md`
- Create: `content/linux/07-o-disco-encheu/verificacao.yaml`
- Create: `content/linux/07-o-disco-encheu/workspace/Dockerfile`
- Create: `content/linux/07-o-disco-encheu/workspace/compose.yaml` — **o único diferente**
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: `lsof`, que a Etapa 3 pôs na imagem exatamente para este Cenário; e o `Inode:`
  que o Cenário 01 nomeou de passagem no `stat`.
- Produces: nada que outro Cenário consuma. Fecha o Ato II.

- [ ] **Step 1: Atualizar as contagens e ver o teste falhar**

```java
        assertThat(cenarios).hasSize(69);
```

```java
        assertThat(linux).hasSize(8);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(20);
```

Run: `cd backend; ./mvnw test -Dtest=CatalogoRealTest`
Expected: FAIL — `expected size: 69 but was: 68`.

- [ ] **Step 2: Criar o workspace com o compose especial**

```powershell
mkdir content\linux\07-o-disco-encheu\workspace
Copy-Item content\linux\08-um-programa-vira-servico\workspace\Dockerfile `
          content\linux\07-o-disco-encheu\workspace\Dockerfile
```

Create `content/linux/07-o-disco-encheu/workspace/compose.yaml`:

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
      - /var/log/aurora:size=8m,nr_inodes=2048,mode=755
    volumes:
      - /sys/fs/cgroup:/sys/fs/cgroup:rw
```

A terceira linha do `tmpfs:` é a única diferença em relação aos outros Cenários da Trilha,
e é o que faz este Cenário existir. Medido: com ela o sistema continua subindo `running`,
sem nenhuma unit falha.

**Nunca troque esse tmpfs por um caminho no `/`.** O `/` do container é o disco da VM do
Docker Desktop, com 934 GB livres — enchê-lo seria impossível na prática e um ataque ao
disco de quem estuda.

- [ ] **Step 3: Escrever a Verificação**

Create `content/linux/07-o-disco-encheu/verificacao.yaml`:

```yaml
asercoes:
  - tipo: comando_produz
    descricao: a partição de log saiu do vermelho
    comando: ["docker", "exec", "learning-infra-linux", "sh", "-c",
              "P=$(df --output=pcent /var/log/aurora | tail -1 | tr -d ' %'); if [ \"$P\" -lt 50 ]; then echo espaco-liberado; else echo ainda-cheio; fi"]
    contem: espaco-liberado

  - tipo: comando_produz
    descricao: nenhum processo continua segurando um arquivo apagado
    comando: ["docker", "exec", "learning-infra-linux", "sh", "-c",
              "if lsof +L1 2>/dev/null | grep -q /var/log/aurora; then echo descritor-preso; else echo sem-descritor-preso; fi"]
    contem: sem-descritor-preso

  - tipo: arquivo_linux
    caminho: /var/log/aurora/pedidos.log
    dono: root
    descricao: o registro de pedidos voltou a ser gravado
```

As três juntas são o Cenário: espaço de volta, causa removida, e o programa escrevendo
outra vez. Liberar espaço matando o processo errado satisfaria a primeira e falharia na
terceira.

- [ ] **Step 4: Escrever o Cenário**

Frontmatter:

```yaml
---
id: linux/07-o-disco-encheu
titulo: O disco encheu
dificuldade: assistido
projetoCompose: linux-07
containerLinux: learning-infra-linux
---
```

Sete seções `##`:

1. `## A pressão` — os pedidos pararam de ser registrados de madrugada. O programa que
   grava roda, não reclama na tela, e nada aparece no arquivo.

   Estabeleça o terreno logo na abertura, porque o leitor precisa saber onde está pisando:
   na Aurora o `/var/log/aurora` é uma **partição própria**, separada do resto do sistema.
   É prática comum e existe por um motivo — log que cresce sem controle enche a partição
   dele e para ali, em vez de derrubar a máquina inteira. Ela é pequena:

   ```bash
   df -h /var/log/aurora
   df -h /
   ```

   Saída medida: a partição de log tem `8.0M`; o `/` tem 1 TB. São sistemas de arquivos
   diferentes, e é por isso que o disco pode estar "cheio" e vazio ao mesmo tempo,
   dependendo de qual você olha.

2. `## Reproduza a madrugada` — gere o log que cresceu sem rotação:

   ```bash
   python3 - <<'PY'
   import random
   random.seed(7)
   with open("/var/log/aurora/pedidos.log", "w") as saida:
       for i in range(60000):
           saida.write(
               f"2026-08-15T03:{i%60:02d}:{(i*7)%60:02d} pedido={5000+i} "
               f"cliente=livraria-do-porto status=500 ms={random.randint(18,400)}\n")
   PY
   df -h /var/log/aurora
   ```

   Saída medida: `8.0M 4.4M 3.7M 55%`. Mais da metade da partição, num único arquivo.

   Agora encha o resto e veja o erro que a Aurora estava recebendo sem ver:

   ```bash
   dd if=/dev/zero of=/var/log/aurora/despejo bs=1M count=20
   df -h /var/log/aurora
   ```

   Saída medida: `dd: IO error: No space left on device`, e a partição em `100%`.

3. `## Apagar não é o mesmo que liberar` — o primeiro incidente. Simule o que existe em
   qualquer máquina de verdade: um programa com o log **aberto**.

   ```bash
   python3 -c "import time; f=open('/var/log/aurora/pedidos.log'); time.sleep(3600)" &
   sleep 2
   ```

   Agora faça o que todo mundo faz sob pressão — apagar o arquivo grande:

   ```bash
   rm /var/log/aurora/despejo /var/log/aurora/pedidos.log
   du -sh /var/log/aurora
   df -h /var/log/aurora
   ```

   Saídas medidas, e é aqui que o Cenário acontece: o `du` responde **`0`** e o `df`
   responde **`4.4M`, `55%`**. O diretório está vazio e a partição continua ocupada.

   Explique com o vocabulário que o Cenário 01 plantou. O `rm` não apaga conteúdo: ele
   remove **um nome** que apontava para um inode. O espaço só volta quando o inode não tem
   mais nenhum nome **e** nenhum processo com ele aberto. Aquele `python3` ainda tem — e
   enquanto tiver, os 4,4 MB continuam gastos num arquivo que não tem mais nome.

   E a diferença entre as duas ferramentas passa a ser óbvia: **`du` percorre nomes**, por
   isso não vê o arquivo apagado; **`df` pergunta ao sistema de arquivos**, por isso vê.
   Quando os dois discordam, a resposta quase sempre é esta.

4. `## Ache quem está segurando` —

   ```bash
   lsof +L1
   ```

   Saída medida:

   ```
   COMMAND PID USER FD   TYPE DEVICE SIZE/OFF NLINK NODE NAME
   python3  68 root 3r   REG  0,508  4602181     0    2 /var/log/aurora/pedidos.log (deleted)
   ```

   Leia a linha campo a campo, porque cada um conta parte da história: `+L1` pede
   justamente os arquivos com **menos de 1 nome**; a coluna `NLINK` é `0`, ou seja, nenhum
   nome aponta para ele; o `(deleted)` no fim é o `lsof` dizendo o mesmo em português; e o
   `SIZE/OFF` mostra os 4,6 MB que não voltaram.

   Resolva:

   ```bash
   kill $(lsof -t +L1)
   sleep 2
   df -h /var/log/aurora
   ```

   Saída medida: `8.0M 0 8.0M 0%`. O espaço voltou **no instante** em que o último
   descritor fechou.

   Registre a regra de ofício: depois de apagar arquivo grande, confira o `df`. Se ele não
   se mexeu, o arquivo ainda está aberto em algum lugar — e reiniciar o programa que o
   segura resolve, sem precisar reiniciar a máquina.

5. `## Cheio sem nada dentro` — o segundo incidente, e o mais confuso dos dois. Simule o
   diretório de rotação que ninguém limpou:

   ```bash
   mkdir -p /var/log/aurora/rotacao
   for i in $(seq 1 2100); do : > /var/log/aurora/rotacao/antigo-$i.log; done
   ```

   A partir do 2047º, medido, cada linha responde `No space left on device`. Agora olhe:

   ```bash
   df -h /var/log/aurora
   df -i /var/log/aurora
   ```

   Saídas medidas: o `df -h` diz **`8.0M 0 8.0M 0%`** — o disco está **vazio**. O `df -i`
   diz **`2048 2048 0 100%`**.

   Um sistema de arquivos tem dois recursos finitos, não um. **Espaço** guarda o conteúdo;
   **inodes** guardam os arquivos em si — um por arquivo, independente do tamanho. Dois mil
   arquivos vazios não gastam espaço nenhum e gastam dois mil inodes. Quando os inodes
   acabam, você não cria mais **nada**, mesmo com a partição vazia.

   O teste que confirma o diagnóstico em dois segundos:

   ```bash
   touch /var/log/aurora/novo.log
   echo "linha" >> /var/log/aurora/rotacao/antigo-1.log
   ```

   Medido: o `touch` falha e a escrita no arquivo **já existente** funciona. Criar precisa
   de inode; escrever, não. É essa assimetria que distingue os dois incidentes — e nenhum
   `df -h` do mundo te contaria isso.

   Limpe:

   ```bash
   rm -rf /var/log/aurora/rotacao
   df -i /var/log/aurora
   ```

6. `## Rotação é a prevenção` — nomeie o que estava faltando desde o começo. Nenhum dos
   dois incidentes é sobre disco: os dois são sobre **log que cresce sem ninguém aparar**.
   Rotacionar é trocar o arquivo ativo periodicamente, comprimir os antigos e apagar os
   mais velhos que um limite — por tamanho, por idade, ou pelos dois.

   E a armadilha que o próprio Cenário demonstrou: uma rotação que **apaga** o arquivo em
   vez de truncá-lo, enquanto o programa o mantém aberto, é exatamente a seção 3 —
   o espaço não volta, e o programa continua escrevendo num arquivo sem nome.

   Diga o que fica de fora e por quê: a ferramenta que faz isso na prática, e o timer que a
   dispara toda noite, são o Cenário 10.

7. `## Deixe a máquina em pé` — o entregável. Recrie o registro de pedidos, agora de
   tamanho sensato:

   ```bash
   python3 - <<'PY'
   import random
   random.seed(7)
   with open("/var/log/aurora/pedidos.log", "w") as saida:
       for i in range(2000):
           saida.write(
               f"2026-08-15T03:{i%60:02d}:{(i*7)%60:02d} pedido={5000+i} "
               f"cliente=livraria-do-porto status=500 ms={random.randint(18,400)}\n")
   PY
   df -h /var/log/aurora
   lsof +L1
   ```

   O `lsof +L1` não deve listar nada de `/var/log/aurora`.

   Feche dizendo o que a Verificação cobra: a partição abaixo de 50%, nenhum descritor
   preso a arquivo apagado, e o `pedidos.log` existindo de novo. E feche o Ato: em quatro
   Cenários a máquina deixou de ser um disco com arquivos e virou um lugar onde coisas
   rodam, consomem, morrem e às vezes se recusam a morrer. O Ato III resolve o problema que
   apareceu em todos eles — que **nada disso sobrevive a um boot**.

- [ ] **Step 5: Rodar o Cenário de verdade**

```powershell
cd content\linux\07-o-disco-encheu\workspace
docker compose -p linux-07 up -d --build
docker exec learning-infra-linux systemctl is-system-running
docker exec learning-infra-linux df -h /var/log/aurora
```

Expected antes de tudo: `running` e uma partição de `8.0M`. Se o `df` mostrar 1 TB, o
tmpfs não foi montado e **o Cenário inteiro é uma mentira** — pare e corrija o
`compose.yaml`.

Depois execute todas as seções na ordem e confira, uma a uma, as saídas medidas: `55%`
depois do log, `No space left on device` no `dd`, `du` igual a `0` contra `df` igual a
`55%`, o `lsof +L1` com `NLINK 0`, o `0%` depois do `kill`, a falha no 2047º arquivo, e o
par `df -h` vazio contra `df -i` em 100%.

Valide as três Asserções:

```powershell
docker exec learning-infra-linux sh -c "P=$(df --output=pcent /var/log/aurora | tail -1 | tr -d ' %'); if [ \"$P\" -lt 50 ]; then echo espaco-liberado; else echo ainda-cheio; fi"
docker exec learning-infra-linux sh -c "if lsof +L1 2>/dev/null | grep -q /var/log/aurora; then echo descritor-preso; else echo sem-descritor-preso; fi"
docker exec learning-infra-linux stat -c "%a %U %G" /var/log/aurora/pedidos.log
```

Expected: `espaco-liberado`, `sem-descritor-preso`, e um `stat` com dono `root`.

- [ ] **Step 6: Rodar a suíte, derrubar e commitar**

```powershell
cd backend; ./mvnw test
cd ..\content\linux\07-o-disco-encheu\workspace; docker compose -p linux-07 down -v
```

```bash
git add content/linux/07-o-disco-encheu/ \
        backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: Cenário 07 da Trilha Linux ensina df contra du e inode"
```

---

### Task 5: Fechar o Ato II na documentação

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: os quatro Cenários das Tasks 1 a 4.
- Produces: nada. É a última desta Etapa.

- [ ] **Step 1: Atualizar o parágrafo de estado da Trilha**

O README hoje diz que a Trilha Linux tem "o **Ato I completo** — os Cenários 01 a 03 estão
no catálogo, mais o Cenário 08". Passa a dizer que os **Atos I e II** estão completos —
Cenários 01 a 08 no catálogo — e que os Atos III e IV, isto é, os Cenários 09 a 14, estão
em construção.

- [ ] **Step 2: Registrar as duas particularidades do Ato II**

Na seção "Preparando a Trilha Linux", acrescente dois avisos que valem para quem for rodar
os Cenários:

- **o Cenário 06 exige rede** no momento em que roda, porque faz `apt-get update` — cerca
  de 26 MB. É o único da Trilha com essa dependência;
- **o Cenário 07 monta `/var/log/aurora` como um tmpfs de 8 MB**, e é o único Cenário da
  Trilha cujo `compose.yaml` difere dos demais. Diga o porquê em uma linha: é o que permite
  ensinar disco cheio sem `--privileged` e sem encostar no disco de quem estuda.

- [ ] **Step 3: Rodar tudo**

```powershell
cd backend; ./mvnw test
cd ..\frontend; node scripts-checar-conteudo.mjs linux
```

Expected: PASS nos dois. O checador não olha para Cenários, então nada nesta Etapa deveria
afetá-lo.

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "docs: README registra os Atos I e II da Trilha Linux completos"
```

---

## O que esta Etapa deixa aberto

- **O `man` continua quebrado** e o Cenário 06 **não** o usa. A imagem Ubuntu vem
  minimizada e `man ls` responde com o aviso de minimização. O Cenário ensina `--help`,
  `type` e `dpkg -L` no lugar, que resolvem o problema real. Instalar `man-db` e rodar
  `unminimize` continua sendo uma opção não medida — se algum Cenário do Ato III ou IV
  precisar de manpage, meça o custo antes de prometer.
- **A distribuição de Dificuldade da Trilha** só fecha no fim do Ato IV. Ao chegar lá,
  acrescente ao `CatalogoRealTest` as contagens por Dificuldade, como a Trilha IaC já tem:
  serão quatro `Guiado`, sete `Assistido`, dois `Autônomo` e um `Mestre`.
- **O risco do `cgroup: host`** continua sem teste, pelo mesmo motivo da Etapa 3: seria o
  primeiro teste da suíte a depender de Docker no CI. Com o Cenário 07 no ar o risco cresce
  um pouco, porque agora a Trilha depende também de o Docker Desktop aceitar `nr_inodes`
  no tmpfs — que foi medido nesta data, mas não é verificado por nada.
