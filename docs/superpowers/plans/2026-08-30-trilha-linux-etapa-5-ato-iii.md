# Trilha Linux — Etapa 5: Ato III, Cenários 09 a 11

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fechar o Ato III — a Aurora deixa de depender de alguém ter digitado um comando.
O Cenário 08 já mostrou o que é um serviço; estes três ensinam a **consertar** um que não
sobe, a **agendar** o trabalho da madrugada, e a **ler** o que a máquina registrou sobre si
mesma.

**Architecture:** Nenhuma Asserção nova, nenhuma mudança de motor e nenhuma mudança no
`Dockerfile`. Os três Cenários são conteúdo no disco e reusam `servico_systemd`,
`http_responde`, `arquivo_linux` e `comando_produz`. O único `compose.yaml` fora do padrão
continua sendo o do Cenário 07.

**Tech Stack:** Ubuntu 26.04 (pinado por digest) · systemd 259 · journald · systemd timers ·
Docker Compose · SnakeYAML · JUnit 5 · AssertJ

**Spec:** [`docs/superpowers/specs/2026-08-21-trilha-linux-design.md`](../specs/2026-08-21-trilha-linux-design.md)

## Global Constraints

- **Todas as restrições das Etapas 1 a 4 continuam valendo**, em especial: o `Dockerfile`
  é o mesmo byte a byte em todo Cenário da Trilha, e o `SubstratoLinuxTest` reprova o
  contrário.
- Dificuldades, conforme o design: **09 `autonomo`**, **10 `assistido`**,
  **11 `assistido`**.
- Portas: o **Cenário 09 usa a 8041**. Os Cenários 10 e 11 não servem nada e não publicam
  porta. O Cenário 08 continua com a 8040.
- `containerLinux: learning-infra-linux` e `projetoCompose: linux-NN` no frontmatter; o
  `verificacao.yaml` nunca declara `container`.
- Todo texto em **português**; personagens por papel, nunca por pronome.
- **A regra de ordem das Asserções da Etapa 1 vale aqui com força:** toda Asserção de
  efeito visível (`http_responde`) vem acompanhada de `servico_systemd` provando que o
  serviço está **habilitado**, não apenas rodando porque o leitor o iniciou à mão.
- A narrativa parte do que o Ato II deixou: o leitor já sabe achar processo, matar processo
  e diagnosticar disco, e já viu que nada disso sobrevive a um boot.

## Medições que fundamentam este plano

Feitas em 2026-08-30 contra a imagem da Etapa 3, com Docker Engine 29.6.1.

**Serviço que falha — três modos, todos medidos:**

| Modo | `systemctl status` | No journal |
|---|---|---|
| binário inexistente | `Active: failed (Result: exit-code)`, `status=203/EXEC` | — |
| script sai com 1 | `status=1/FAILURE` | **`entrada-pedidos[141]: diretorio /srv/pedidos nao existe`** |
| porta ocupada | `status=1/FAILURE` | `OSError: [Errno 98] Address already in use` |

A linha do meio é a mais valiosa do Ato: **o stderr do programa aparece no journal**, com o
nome e o PID do processo. É o que amarra a lição de stdout contra stderr do Cenário 03 ao
journald, e o que faz `journalctl -u` valer mais que `systemctl status`.

**Uma armadilha que muda o desenho do Cenário 09**, medida:
`python3 -m http.server --directory /srv/pedidos` com o diretório **inexistente** sobe como
`active (running)` — ele só falha na hora de servir. Um Cenário que usasse isso como "o
serviço que não sobe" estaria errado. Daí o `entrada-pedidos`, um script que **checa e sai
com código 1**, que é como um programa de produção se comporta.

**`Restart=on-failure`, medido:** o systemd tenta **5 vezes** e desiste com
`Start request repeated too quickly.` seguido de `Failed to start`. `Restart=` não é
mágica, e o Cenário diz isso.

**Timer, medido:**

| Pergunta | Resposta |
|---|---|
| `systemctl is-active relatorio.timer` | `active` |
| `systemctl is-enabled relatorio.timer` | `enabled` |
| `systemctl is-active relatorio.service` (oneshot parado) | `inactive` |
| `systemctl list-timers` | mostra `NEXT`, `LEFT`, `UNIT`, `ACTIVATES` |
| `systemd-analyze calendar "*:*:0/20"` | `Normalized form: *-*-* *:*:00/20` |
| `systemd-analyze calendar "*-*-* 03:00:00"` | `Next elapse: ... 03:00:00`, `21h left` |
| o timer disparou de verdade? | sim — artefato criado, e o journal do `.service` mostra `Starting` e `Finished` |

A terceira linha é o que o Cenário 10 precisa dizer em voz alta: um `.service` do tipo
`oneshot` fica `inactive` **quando está tudo certo**. Quem cobra `is-active` do serviço em
vez do timer conclui que está quebrado.

**Journal, medido:**

| Comando | Resultado |
|---|---|
| `journalctl -t aurora -p err` | só a linha de erro |
| `journalctl -t aurora -p warning` | a de aviso **e** a de erro |
| `journalctl -t aurora -p err -o json` | `PRIORITY: '3'`, `SYSLOG_IDENTIFIER: 'aurora'`, `MESSAGE: ...` |
| `journalctl --disk-usage` | `Archived and active journals take up 8M` |
| `/var/log/journal/<machine-id>` | existe — o journal **persiste** |

A segunda linha é a lição central: `-p` filtra **daquela prioridade para cima**, não
naquela prioridade. A terceira é a tese do Cenário 11 — o journal guarda campos, não texto.

## Estrutura de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `content/linux/09-o-servico-que-nao-sobe/` | `cenario.md`, `verificacao.yaml`, `workspace/Dockerfile`, `workspace/compose.yaml` (porta 8041) |
| `content/linux/10-o-trabalho-das-tres-da-manha/` | idem, sem porta |
| `content/linux/11-log-nao-e-arquivo-de-texto/` | idem, sem porta |
| `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java` | contagens do catálogo |
| `README.md` | o Ato III publicado e a porta 8041 registrada |

## Contagens ao fim da Etapa

Catálogo: **72 Cenários**, sendo **11** da Trilha Linux. Asserções da Trilha: **26** — as 20
de hoje mais 2 em cada Cenário novo.

---

### Task 1: Cenário 09 — O serviço que não sobe

O primeiro `Autônomo` da Trilha. O Cenário entrega uma máquina com um serviço quebrado e o
objetivo; não entrega o passo a passo. O que ele **ensina** é o método de diagnóstico, e o
método é sempre o mesmo: `status` diz que falhou, `journalctl -u` diz por quê.

**Files:**
- Create: `content/linux/09-o-servico-que-nao-sobe/cenario.md`
- Create: `content/linux/09-o-servico-que-nao-sobe/verificacao.yaml`
- Create: `content/linux/09-o-servico-que-nao-sobe/workspace/Dockerfile`
- Create: `content/linux/09-o-servico-que-nao-sobe/workspace/compose.yaml`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: a unit e o `enable` contra `start` do Cenário 08; o código de saída do
  Cenário 03.
- Produces: o `pedidos.service` funcionando, e o vocabulário de `journalctl -u` que o
  Cenário 11 aprofunda.

- [ ] **Step 1: Atualizar as contagens e ver o teste falhar**

```java
        assertThat(cenarios).hasSize(70);
```

```java
        assertThat(linux).hasSize(9);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(22);
```

Run: `cd backend; ./mvnw test -Dtest=CatalogoRealTest`
Expected: FAIL — `expected size: 70 but was: 69`.

- [ ] **Step 2: Criar o workspace**

```powershell
mkdir content\linux\09-o-servico-que-nao-sobe\workspace
Copy-Item content\linux\08-um-programa-vira-servico\workspace\Dockerfile `
          content\linux\09-o-servico-que-nao-sobe\workspace\Dockerfile
```

Create `content/linux/09-o-servico-que-nao-sobe/workspace/compose.yaml` — igual ao do
Cenário 08, trocando a porta:

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
    ports:
      - "8041:8041"
```

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: servico_systemd
    nome: pedidos.service
    ativo: true
    habilitado: true
    descricao: a entrada de pedidos está ativa e sobe sozinha no boot

  - tipo: http_responde
    url: http://localhost:8041
    status: 200
```

O par é a regra da Etapa 1: consertar o serviço e esquecer o `enable` deixa a máquina no
mesmo estado em que ela estava antes do Cenário 08 — funcionando até o próximo boot.

- [ ] **Step 4: Escrever o Cenário**

Frontmatter:

```yaml
---
id: linux/09-o-servico-que-nao-sobe
titulo: O serviço que não sobe
dificuldade: autonomo
projetoCompose: linux-09
containerLinux: learning-infra-linux
---
```

Estrutura, em cinco seções `##`. **Sendo `Autônomo`, o texto dá o método e o objetivo, e
não entrega a solução pronta** — as unidades quebradas vêm prontas, o conserto não.

1. `## A situação` — a Aurora quer pôr a entrada de pedidos no ar como serviço, seguindo o
   que o Cenário 08 ensinou. A pessoa que tentou deixou os arquivos no lugar e foi embora,
   e o serviço não sobe. Instale o que ela deixou, exatamente como está:

   ```bash
   tee /usr/local/bin/entrada-pedidos > /dev/null <<'EOF'
   #!/bin/bash
   if [ ! -d /srv/pedidos ]; then
     echo "diretorio /srv/pedidos nao existe" >&2
     exit 1
   fi
   exec /usr/bin/python3 -m http.server 8041 --directory /srv/pedidos
   EOF
   chmod +x /usr/local/bin/entrada-pedidos

   tee /etc/systemd/system/pedidos.service > /dev/null <<'EOF'
   [Unit]
   Description=Entrada de pedidos da Aurora

   [Service]
   ExecStart=/usr/local/bin/entrada-pedidos

   [Install]
   WantedBy=multi-user.target
   EOF
   systemctl daemon-reload
   systemctl start pedidos.service
   ```

   **O objetivo:** a entrada de pedidos respondendo em <http://localhost:8041> e subindo
   sozinha no boot. O caminho é seu.

2. `## O método` — as duas perguntas, nesta ordem. Esta é a única seção que entrega
   comando pronto, porque é o que o Cenário existe para ensinar:

   ```bash
   systemctl status pedidos.service
   journalctl -u pedidos.service -n 20
   ```

   Explique como ler o `status`: `Active: failed (Result: exit-code)` diz **que** falhou;
   `Process: ... (code=exited, status=1/FAILURE)` diz **com qual código**. E explique por
   que o `journalctl -u` importa mais: ele traz o que o **programa** escreveu, não só o que
   o systemd concluiu. Saída medida, e é ela que resolve este Cenário:

   ```
   Aug 30 05:42:10 aurora entrada-pedidos[141]: diretorio /srv/pedidos nao existe
   Aug 30 05:42:10 aurora systemd[1]: pedidos.service: Main process exited, code=exited, status=1/FAILURE
   Aug 30 05:42:10 aurora systemd[1]: pedidos.service: Failed with result 'exit-code'.
   ```

   A primeira linha é o `stderr` do script — o mesmo fluxo `2` do Cenário 03, agora
   capturado pelo journald porque o programa roda como serviço. **É por isso que um serviço
   bem escrito escreve no stderr em vez de num arquivo próprio.**

   Dê a tabela de códigos que aparece com mais frequência, porque ela poupa buscas:

   | Código | Significa |
   |---|---|
   | `status=1/FAILURE` | o programa rodou e saiu com erro — leia o journal |
   | `status=203/EXEC` | o systemd não conseguiu **executar** o `ExecStart` — caminho errado ou sem bit de execução |
   | `status=200/CHDIR` | `WorkingDirectory` aponta para lugar que não existe |

3. `## Conserte` — sem passo a passo. Diga o que precisa ser verdade ao final: o serviço
   `active`, `enabled`, e a porta 8041 respondendo. Lembre de duas coisas que o leitor já
   sabe e vai esquecer sob pressão: **`daemon-reload` depois de mexer na unit**, e
   **`enable` não é `start`**.

   Deixe um aviso honesto: se o `journalctl` disser `Address already in use`, alguma coisa
   já está na 8041 — e achar o quê é o Cenário 12.

4. `## Quando falhar de novo` — a seção que transforma o conserto em aprendizado. Peça ao
   leitor que quebre de propósito outra vez e observe o `Restart=`:

   ```bash
   systemctl stop pedidos.service
   mv /srv/pedidos /srv/pedidos-fora
   ```

   Acrescente à seção `[Service]` da unit:

   ```ini
   Restart=on-failure
   RestartSec=1
   ```

   E observe:

   ```bash
   systemctl daemon-reload
   systemctl start pedidos.service
   sleep 8
   systemctl status pedidos.service
   journalctl -u pedidos.service | grep -i "repeated too quickly"
   ```

   Medido: o systemd tenta **5 vezes** e desiste com `Start request repeated too quickly.`

   A lição, que vale mais que o parâmetro: **`Restart=` cobre falha transitória, não bug.**
   Um serviço que não sobe por configuração errada vai falhar as cinco vezes e parar. Quem
   confia no `Restart=` como conserto troca um serviço parado por um serviço parado **e**
   um journal cheio.

   Devolva o diretório e deixe o serviço em pé:

   ```bash
   mv /srv/pedidos-fora /srv/pedidos
   systemctl reset-failed pedidos.service
   systemctl start pedidos.service
   ```

5. `## Ordem entre serviços` — feche com `After=` e `Requires=`, que o design pede e que o
   Cenário 14 vai cobrar. Explique a diferença em uma frase cada: **`After=` é ordem**, diz
   quando iniciar; **`Requires=` é dependência**, diz que sem o outro este não faz sentido.
   Declarar `Requires=` sem `After=` é o erro clássico — o systemd sobe os dois ao mesmo
   tempo, e o seu perde a corrida.

   Não peça alteração de unit aqui; o Cenário já entregou o objetivo dele. Basta o leitor
   entender os dois nomes antes do Ato IV.

- [ ] **Step 5: Rodar o Cenário de verdade**

```powershell
cd content\linux\09-o-servico-que-nao-sobe\workspace
docker compose -p linux-09 up -d --build
docker exec -it learning-infra-linux bash
```

Percorra o Cenário como um leitor faria: instale o que a seção 1 manda, confirme que o
serviço **falha** com `status=1/FAILURE` e que o journal mostra a linha do `stderr`,
conserte, e valide.

```powershell
docker exec learning-infra-linux systemctl is-active pedidos.service
docker exec learning-infra-linux systemctl is-enabled pedidos.service
curl.exe -s -o NUL -w "%{http_code}" http://localhost:8041
```

Expected: `active`, `enabled`, `200`.

- [ ] **Step 6: Rodar a suíte, derrubar e commitar**

```powershell
cd backend; ./mvnw test
cd ..\content\linux\09-o-servico-que-nao-sobe\workspace; docker compose -p linux-09 down -v
```

```bash
git add content/linux/09-o-servico-que-nao-sobe/ \
        backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: Cenário 09 da Trilha Linux ensina a diagnosticar serviço que falha"
```

---

### Task 2: Cenário 10 — O trabalho das três da manhã

Fecha o arco aberto no Cenário 03, quando o leitor descobriu que os erros da
`livraria-do-porto` acontecem entre 2h e 4h. Agora ele monta o trabalho agendado que a
Aurora precisa — e descobre que, no systemd, um agendamento é só mais uma unit.

**Files:**
- Create: `content/linux/10-o-trabalho-das-tres-da-manha/cenario.md`
- Create: `content/linux/10-o-trabalho-das-tres-da-manha/verificacao.yaml`
- Create: `content/linux/10-o-trabalho-das-tres-da-manha/workspace/Dockerfile`
- Create: `content/linux/10-o-trabalho-das-tres-da-manha/workspace/compose.yaml`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: o `relatorio-aurora` do Cenário 04 e a descoberta do Cenário 03.
- Produces: o par `.service` mais `.timer` que o Cenário 14 pode quebrar.

- [ ] **Step 1: Atualizar as contagens e ver o teste falhar**

```java
        assertThat(cenarios).hasSize(71);
```

```java
        assertThat(linux).hasSize(10);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(24);
```

Run: `cd backend; ./mvnw test -Dtest=CatalogoRealTest`
Expected: FAIL — `expected size: 71 but was: 70`.

- [ ] **Step 2: Criar o workspace**

`Dockerfile` copiado do Cenário 08; `compose.yaml` copiado do Cenário 01 — **sem porta**.

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: servico_systemd
    nome: relatorio.timer
    ativo: true
    habilitado: true
    descricao: o timer do relatório está armado e sobe sozinho no boot

  - tipo: arquivo_linux
    caminho: /srv/relatorio-noturno/ultimo.txt
    dono: root
    descricao: o timer disparou de verdade e o relatório saiu
```

As duas juntas são o Cenário: a primeira prova que o agendamento existe e persiste, a
segunda prova que ele **rodou**. Um timer armado que nunca disparou passaria na primeira e
falharia na segunda.

- [ ] **Step 4: Escrever o Cenário**

Frontmatter:

```yaml
---
id: linux/10-o-trabalho-das-tres-da-manha
titulo: O trabalho das três da manhã
dificuldade: assistido
projetoCompose: linux-10
containerLinux: learning-infra-linux
---
```

Seis seções `##`:

1. `## A pressão` — retome o Cenário 03: os erros da `livraria-do-porto` se concentram
   entre 2h e 4h, e ninguém está acordado para ver. A Aurora quer o relatório pronto na
   mesa quando o dia começar. Alguém sugeriu "põe no cron"; o Cenário mostra por que a
   máquina já tem uma resposta melhor.

2. `## O trabalho` — recrie o programa do Cenário 04 e o lugar do artefato:

   ```bash
   mkdir -p /srv/relatorio-noturno
   tee /usr/local/bin/relatorio-aurora > /dev/null <<'EOF'
   #!/bin/bash
   # Resumo dos pedidos com falha, por cliente.
   grep "status=500" /var/log/aurora/pedidos.log \
     | cut -d" " -f3 | cut -d= -f2 | sort | uniq -c | sort -rn
   EOF
   chmod +x /usr/local/bin/relatorio-aurora
   relatorio-aurora
   ```

3. `## Um serviço que não fica no ar` — a unit do trabalho. E o conceito novo:

   ```bash
   tee /etc/systemd/system/relatorio.service > /dev/null <<'EOF'
   [Unit]
   Description=Relatório noturno da Aurora

   [Service]
   Type=oneshot
   ExecStart=/bin/sh -c "/usr/local/bin/relatorio-aurora > /srv/relatorio-noturno/ultimo.txt"
   EOF
   systemctl daemon-reload
   systemctl start relatorio.service
   cat /srv/relatorio-noturno/ultimo.txt
   systemctl is-active relatorio.service
   ```

   O `is-active` responde **`inactive`** — medido — e **isso está certo**. Nomeie o
   `Type=oneshot`: é um serviço que faz uma coisa e termina, ao contrário do
   `pedidos.service`, que fica no ar. Para um `oneshot`, `inactive` é o estado de sucesso.

   Este é o erro de leitura mais comum com trabalho agendado: alguém cobra `is-active` do
   serviço, vê `inactive` e conclui que está quebrado. **O que se cobra é o timer.**

4. `## O timer é uma unit como outra qualquer` — o par:

   ```bash
   tee /etc/systemd/system/relatorio.timer > /dev/null <<'EOF'
   [Unit]
   Description=Dispara o relatório da Aurora

   [Timer]
   OnCalendar=*:*:0/20
   Persistent=true

   [Install]
   WantedBy=timers.target
   EOF
   systemctl daemon-reload
   systemctl enable --now relatorio.timer
   ```

   Explique as três peças: o `.timer` tem o **mesmo nome** do `.service` que ele dispara, e
   é por isso que não precisa dizer qual é; o `OnCalendar` é **quando**; e o
   `Persistent=true` manda executar assim que possível se a máquina estava desligada na
   hora marcada — que é exatamente o que faltava no `cron`.

   O `OnCalendar=*:*:0/20` é **de propósito** a cada 20 segundos, para você ver acontecer
   agora em vez de esperar a madrugada.

   ```bash
   systemctl list-timers
   systemctl is-active relatorio.timer
   systemctl is-enabled relatorio.timer
   ```

   O `list-timers` mostra `NEXT`, `LEFT`, `UNIT` e `ACTIVATES` — e é o comando que responde
   "o que esta máquina faz sozinha", que é a primeira pergunta de quem herda um servidor.
   O timer responde `active` e `enabled`.

   Espere e confirme que disparou:

   ```bash
   sleep 25
   cat /srv/relatorio-noturno/ultimo.txt
   journalctl -u relatorio.service -n 5
   ```

   O journal mostra `Starting relatorio.service` e `Finished relatorio.service` — medido.

5. `## A hora de verdade` — troque para o horário real e aprenda a conferir sintaxe **antes**
   de esperar um dia inteiro:

   ```bash
   systemd-analyze calendar "*-*-* 03:00:00"
   ```

   Saída medida: `Normalized form: *-*-* 03:00:00`, `Next elapse: ...`, `21h left`. Diga que
   este comando é o corretor ortográfico do `OnCalendar` e que usá-lo é a diferença entre
   descobrir o erro agora e descobrir em três dias.

   Edite o `OnCalendar` da unit para `*-*-* 03:00:00` e recarregue:

   ```bash
   systemctl daemon-reload
   systemctl restart relatorio.timer
   systemctl list-timers relatorio.timer
   ```

   O artefato que o timer já gerou continua lá — é ele que a Verificação cobra.

6. `## Timer contra cron` — a comparação honesta, sem torcida. O `cron` existe há décadas,
   está em qualquer máquina e é mais curto de escrever. O timer do systemd ganha em quatro
   pontos concretos, e todos eles o leitor acabou de usar:

   - o log vai para o journal, com `journalctl -u`, em vez de sumir ou virar e-mail;
   - `list-timers` mostra o que vai rodar e quando, sem abrir arquivo nenhum;
   - `Persistent=true` recupera execução perdida;
   - o trabalho é uma unit, então ganha `Restart=`, dependência e tudo o mais.

   Feche dizendo o que a Verificação cobra: o **timer** ativo e habilitado, e o **artefato**
   existindo — porque agendamento que nunca rodou não é agendamento.

- [ ] **Step 5: Rodar o Cenário de verdade**

```powershell
cd content\linux\10-o-trabalho-das-tres-da-manha\workspace
docker compose -p linux-10 up -d --build
docker exec -it learning-infra-linux bash
```

Ponto de atenção: confirme que o timer **realmente dispara** com `OnCalendar=*:*:0/20`
antes de trocar para as 3h. Se o artefato não aparecer em 30 segundos, pare — a Asserção
depende disso.

```powershell
docker exec learning-infra-linux systemctl is-active relatorio.timer
docker exec learning-infra-linux systemctl is-enabled relatorio.timer
docker exec learning-infra-linux stat -c "%a %U %G" /srv/relatorio-noturno/ultimo.txt
```

Expected: `active`, `enabled`, e um `stat` com dono `root`.

- [ ] **Step 6: Rodar a suíte, derrubar e commitar**

```bash
git add content/linux/10-o-trabalho-das-tres-da-manha/ \
        backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: Cenário 10 da Trilha Linux ensina timer do systemd"
```

---

### Task 3: Cenário 11 — Log não é arquivo de texto

Fecha o Ato III desfazendo uma suposição que o Ato I plantou de propósito: no Cenário 03 o
log era um arquivo de texto e `grep` bastava. O journal não é isso — e o leitor precisa
saber por que, senão vai tentar `grep` nele e concluir que a máquina está quebrada.

**Files:**
- Create: `content/linux/11-log-nao-e-arquivo-de-texto/cenario.md`
- Create: `content/linux/11-log-nao-e-arquivo-de-texto/verificacao.yaml`
- Create: `content/linux/11-log-nao-e-arquivo-de-texto/workspace/Dockerfile`
- Create: `content/linux/11-log-nao-e-arquivo-de-texto/workspace/compose.yaml`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: o `journalctl -u` do Cenário 09 e o encadeamento de texto do Cenário 03.
- Produces: nada. Fecha o Ato III.

- [ ] **Step 1: Atualizar as contagens e ver o teste falhar**

```java
        assertThat(cenarios).hasSize(72);
```

```java
        assertThat(linux).hasSize(11);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(26);
```

Run: `cd backend; ./mvnw test -Dtest=CatalogoRealTest`
Expected: FAIL — `expected size: 72 but was: 71`.

- [ ] **Step 2: Criar o workspace**

`Dockerfile` do Cenário 08; `compose.yaml` do Cenário 01, sem porta.

- [ ] **Step 3: Escrever a Verificação**

```yaml
asercoes:
  - tipo: comando_produz
    descricao: o journal registrou o erro da Aurora com prioridade de erro
    comando: ["docker", "exec", "learning-infra-linux",
              "journalctl", "-t", "aurora", "-p", "err", "--no-pager"]
    contem: falha ao gravar pedido 9001

  - tipo: arquivo_linux
    caminho: /srv/plantao-madrugada.txt
    dono: root
    descricao: a consulta ao journal virou um registro que a próxima pessoa vai ler
```

A primeira prova que o leitor sabe **filtrar por prioridade**: a mesma mensagem existe sem
o `-p err`, então uma Asserção sem o filtro não provaria nada.

- [ ] **Step 4: Escrever o Cenário**

Frontmatter:

```yaml
---
id: linux/11-log-nao-e-arquivo-de-texto
titulo: Log não é arquivo de texto
dificuldade: assistido
projetoCompose: linux-11
containerLinux: learning-infra-linux
---
```

Seis seções `##`:

1. `## O hábito que não serve aqui` — no Cenário 03 o log era um arquivo e `grep` resolveu.
   Tente o mesmo com o log do sistema:

   ```bash
   ls /var/log/journal/
   ls /var/log/journal/*/
   file /var/log/journal/*/system.journal
   ```

   São arquivos binários. `grep` neles não funciona, e é por isso que existe o `journalctl`.

2. `## Por que binário` — a tese do Cenário. Uma linha de log de texto é uma **frase**; uma
   entrada do journal é um **registro com campos**. Prove:

   ```bash
   journalctl -n 1 -o json | python3 -m json.tool | head -20
   ```

   Aponte os campos que aparecem sem ninguém ter escrito: `PRIORITY`, `_PID`, `_UID`,
   `_SYSTEMD_UNIT`, `__REALTIME_TIMESTAMP`. **É isso que permite perguntar "erros da unit X
   na última hora" sem depender de o programa ter escrito a hora e o nome de um jeito que o
   seu `grep` entenda.** Foi exatamente esse acordo frágil que o Cenário 03 dependia.

3. `## Gere o que a madrugada gerou` — dê ao leitor um journal com conteúdo:

   ```bash
   echo "catalogo respondeu devagar" | systemd-cat -t aurora -p warning
   echo "falha ao gravar pedido 9001" | systemd-cat -t aurora -p err
   echo "rotina noturna iniciada" | systemd-cat -t aurora -p info
   ```

   O `systemd-cat` manda a entrada padrão para o journal; o `-t` define a etiqueta e o `-p`
   a prioridade.

4. `## Os três filtros que resolvem um plantão` — cada um com o comando e o que ele
   responde:

   ```bash
   journalctl -t aurora
   journalctl -u systemd-journald
   journalctl -t aurora -p err
   journalctl -t aurora -p warning
   journalctl -t aurora --since "10 min ago"
   ```

   A dupla de `-p` é a lição, e é contraintuitiva. Medido: `-p err` traz **uma** linha;
   `-p warning` traz **duas** — a de aviso **e** a de erro. O `-p` filtra **daquela
   prioridade para cima**, porque as prioridades são uma escala de gravidade:

   | Número | Nome | Quando |
   |---:|---|---|
   | 0–2 | `emerg`, `alert`, `crit` | a máquina está em risco |
   | 3 | `err` | alguma coisa falhou |
   | 4 | `warning` | alguma coisa vai falhar |
   | 6 | `info` | operação normal |
   | 7 | `debug` | só quando você está investigando |

   Diga a consequência prática: `-p err` num plantão mostra o que quebrou; `-p warning`
   mostra o que quebrou **e** o que avisou antes — e é quase sempre no segundo que está a
   causa.

5. `## O que sobrevive ao boot` — a persistência:

   ```bash
   ls -d /var/log/journal/*
   journalctl --disk-usage
   ```

   Medido: o diretório existe e o journal ocupa cerca de `8M`.

   Explique o que decide isso: o journal é **volátil** por padrão em muitas distribuições —
   vive em `/run/log/journal`, que é memória, e some no boot. Ele só persiste quando
   `/var/log/journal` existe, e é por isso que criar esse diretório é uma das primeiras
   coisas que se faz numa máquina que vai ser operada. E o limite não é infinito: o
   journald descarta o mais antigo conforme a configuração.

   Ligue com o Cenário 07: **log ocupa disco, e o journal também**. A diferença é que ele
   se limita sozinho, enquanto o arquivo de texto da Aurora cresceu até encher a partição.

6. `## Deixe registrado` — o entregável, que amarra o Ato inteiro:

   ```bash
   {
     echo "# Plantão da madrugada — o que o journal mostrou"
     echo
     echo "## Erros"
     journalctl -t aurora -p err --no-pager
     echo
     echo "## Avisos e erros"
     journalctl -t aurora -p warning --no-pager
   } > /srv/plantao-madrugada.txt
   cat /srv/plantao-madrugada.txt
   ```

   O agrupamento com `{ ... }` manda a saída de vários comandos para um redirecionamento só
   — uma peça de shell que o leitor ainda não tinha visto e que fecha o assunto do
   Cenário 03.

   Feche o Ato III: a Aurora saiu de "alguém digitou um comando e foi embora" para serviços
   que sobem sozinhos, um trabalho agendado que a máquina executa, e um registro que dá para
   consultar. O que falta é a última pergunta — **por onde isso tudo é alcançado**, e quem
   tem permissão para quê. É o Ato IV.

- [ ] **Step 5: Rodar o Cenário de verdade**

Atenção a um detalhe já resolvido no texto, para não reintroduzi-lo: o `journalctl -u` da
seção 4 aponta para `systemd-journald`, que existe em qualquer máquina. **Não o troque por
`pedidos.service`** — aquele serviço nasce no Cenário 09 e cada Cenário sobe uma máquina
limpa, então o leitor veria uma saída vazia onde o texto promete conteúdo.

```powershell
docker exec learning-infra-linux journalctl -t aurora -p err --no-pager
docker exec learning-infra-linux stat -c "%a %U %G" /srv/plantao-madrugada.txt
```

Expected: a linha `falha ao gravar pedido 9001` e um `stat` com dono `root`.

- [ ] **Step 6: Rodar a suíte, derrubar e commitar**

```bash
git add content/linux/11-log-nao-e-arquivo-de-texto/ \
        backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: Cenário 11 da Trilha Linux ensina journald e prioridade"
```

---

### Task 4: Fechar o Ato III na documentação

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Atualizar o estado da Trilha e a linha de portas**

O README passa a dizer que os **Atos I, II e III** estão completos — Cenários 01 a 11 no
catálogo — e que o Ato IV, Cenários 12 a 14, está em construção.

Na linha de portas, registre que a Trilha usa a **8040** no Cenário 08 e a **8041** no
Cenário 09, e que os demais não publicam porta.

- [ ] **Step 2: Rodar tudo e commitar**

```powershell
cd backend; ./mvnw test
cd ..\frontend; node scripts-checar-conteudo.mjs linux
```

```bash
git add README.md
git commit -m "docs: README registra o Ato III da Trilha Linux completo"
```

---

## O que esta Etapa deixa aberto

- O **Cenário 14** vai precisar combinar unit mascarada, permissão errada, disco tomado e
  porta ocupada. Três dessas quatro já têm mecânica medida: o disco no Cenário 07, a
  permissão no 02, e a unit no 09. Falta medir `systemctl mask` — e há uma sutileza
  conhecida: a imagem **já mascara** `tmp.mount`, então o Cenário 14 precisa escolher outra
  unit para mascarar, e o leitor pode encontrar as duas ao investigar.
- As **contagens por Dificuldade** entram no `CatalogoRealTest` na Etapa 6, quando os 14
  Cenários estiverem no disco: quatro `Guiado`, sete `Assistido`, dois `Autônomo`, um
  `Mestre`.
