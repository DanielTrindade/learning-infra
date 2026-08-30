# Trilha Linux — Etapa 6: Ato IV, Cenários 12 a 14

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) ou superpowers:executing-plans para implementar tarefa a tarefa. Os passos usam checkbox (`- [ ]`).

**Goal:** Fechar a Trilha Linux. Os três últimos Cenários respondem a última pergunta —
por onde a máquina é alcançada e quem pode o quê — e terminam no plantão que exige as
quatro habilidades do curso ao mesmo tempo, sem dica nenhuma no texto.

**Architecture:** Nenhuma Asserção nova, nenhuma mudança de motor e nenhuma mudança no
`Dockerfile` — `setcap`, `capsh`, `sudo` e `ss` já estão na imagem. O `compose.yaml` do
Cenário 14 combina o tmpfs limitado do Cenário 07 com uma porta publicada, porque o
plantão precisa de disco cheio **e** de porta ocupada. O `CatalogoRealTest` ganha as
contagens por Dificuldade, que só fazem sentido com os 14 Cenários no disco.

**Tech Stack:** Ubuntu 26.04 (pinado por digest) · systemd 259 · iproute2 · libcap ·
sudo · tmpfs · Docker Compose · SnakeYAML · JUnit 5 · AssertJ

**Spec:** [`docs/superpowers/specs/2026-08-21-trilha-linux-design.md`](../specs/2026-08-21-trilha-linux-design.md)

## Global Constraints

- **Todas as restrições das Etapas 1 a 5 continuam valendo**, em especial o `Dockerfile`
  byte a byte, trancado pelo `SubstratoLinuxTest`.
- Dificuldades, conforme o design: **12 `assistido`**, **13 `autonomo`**, **14 `mestre`**.
- Portas: **12 → 8042**, **13 → 8043**, **14 → 8044**. Fecha o bloco 8040–8044 da Trilha.
- Todo texto em **português**; personagens por papel, nunca por pronome.
- O Cenário 14 é **`Mestre`**: entrega o ambiente quebrado e o objetivo, **sem dizer o que
  quebrou**. Nenhuma seção do texto pode nomear as falhas.
- A narrativa fecha o arco: a Aurora chega ao limite do que um servidor operado à mão
  aguenta, e a última seção nomeia o problema que a Trilha Docker vai resolver.

## Medições que fundamentam este plano

Feitas em 2026-08-30 contra a imagem da Etapa 3, com Docker Engine 29.6.1.

### Rede

| Comando | Saída medida |
|---|---|
| `ip -brief addr` | `lo UNKNOWN 127.0.0.1/8`, `eth0@if132 UP 172.24.0.2/16` |
| `ip route` | `default via 172.24.0.1 dev eth0` e a rota da rede local |
| `cat /etc/resolv.conf` | `nameserver 127.0.0.11` — o resolvedor interno do Docker |
| `ss -tlnp` com um serviço no ar | `LISTEN 0 5 0.0.0.0:8042 0.0.0.0:* users:(("python3",pid=223,fd=3))` |

A última linha é o Cenário 12 inteiro: **a porta, o programa e o PID numa linha só**.

### Capabilities — e um limite de fidelidade que muda o Cenário 13

| Medição | Resultado |
|---|---|
| `setcap` e `capsh` na imagem | presentes, sem instalar nada |
| `setcap cap_net_raw+ep` num binário copiado | funciona; `getcap` confirma |
| `capsh --print` como root | **14 capabilities**, não todas |
| `cat /proc/sys/net/ipv4/ip_unprivileged_port_start` | **`0`** |
| não-root ligando na **porta 80**, sem capability | **consegue** |
| `sysctl -w net.ipv4.ip_unprivileged_port_start=1024` | `Read-only file system` — **não dá para corrigir** |

**Isto invalida a demonstração clássica de capability.** "Porta abaixo de 1024 exige
`CAP_NET_BIND_SERVICE`" **não é verdade neste ambiente**, e `/proc/sys` é somente-leitura,
então não há como restaurar o comportamento real.

O Cenário 13 **não finge**. Ele ensina capability por dois caminhos que funcionam de
verdade aqui — `capsh --print`, que mostra que o próprio root está limitado, e
`setcap`/`getcap` num binário — e **diz em voz alta** que a porta baixa não prova nada
nesta máquina. Isso é a tabela de fidelidade dos Fundamentos sendo exercida, e é
literalmente a questão 12 do Questionário: *o que este ambiente não prova sobre uma máquina
de verdade*.

### sudo — e outro folclore desmontado

| Modo de `/etc/sudoers.d/aurora-svc` | `sudo -n -l` |
|---|---|
| `440` | funciona |
| `444` | funciona |
| `400` | funciona |
| `664` | **funciona** |
| `646` | `sudo: /etc/sudoers.d/aurora-svc is world writable` |
| `666` | `sudo: ... is world writable` |

O `sudo` recusa **apenas escrita para outros**. O `440` é **convenção**, não exigência — e o
Cenário 13 diz isso com essas palavras, em vez de repetir a lenda de que qualquer outro
modo quebra. O `/etc/sudoers.d/README` que já vem na imagem está em `440`, e serve de
exemplo pronto.

`visudo -c` responde `parsed OK` por arquivo, e é o comando que impede o erro de sintaxe que
tranca o `sudo` da máquina inteira.

### `systemctl mask` — a armadilha que redesenha o Cenário 14

| Situação | Resultado |
|---|---|
| `mask` numa unit em `/etc/systemd/system/` | **falha**: `File '/etc/systemd/system/x.service' already exists` |
| `mask` numa unit em `/usr/lib/systemd/system/` | `Created symlink '/etc/systemd/system/x.service' → '/dev/null'` |
| `systemctl start` de unit mascarada | `Failed to start x.service: Unit x.service is masked.` |
| `systemctl is-enabled` | `masked` |
| `systemctl status` | `Loaded: masked (Reason: Unit x.service is masked.)` |
| `ls -l /etc/systemd/system/x.service` | `-> /dev/null` |
| `systemctl list-unit-files --state=masked` | mostra a unit **junto com** `tmp.mount`, `cryptdisks-early.service`, `cryptdisks.service`, `hwclock.service`, `sudo.service` |

Duas consequências obrigatórias para o Cenário 14:

1. **A unit do plantão precisa nascer em `/usr/lib/systemd/system/`**, ou o `mask` do
   preparo falha silenciosamente e o Cenário não quebra como deveria.
2. **A imagem já tem units mascaradas** — `tmp.mount` entre elas, mascarada de propósito
   desde a Etapa 1 para o sistema subir `running`. O leitor que rodar
   `list-unit-files --state=masked` vai ver seis linhas, não uma. Isso é realista e o
   Cenário se beneficia disso, mas a Verificação **não pode** depender de a lista ter um
   item só.

Uma sutileza a favor do Cenário: mascarar **não para** um serviço que já está rodando.
Medido — logo após o `mask`, `is-active` ainda respondia `active`. Mascarar impede
**iniciar**, e é por isso que a falha só aparece no próximo boot ou restart.

## Estrutura de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `content/linux/12-onde-este-pacote-vai-dar/` | `cenario.md`, `verificacao.yaml`, `workspace/` (porta 8042) |
| `content/linux/13-acesso-minimo/` | idem (porta 8043) |
| `content/linux/14-o-plantao/` | idem (porta 8044 **e** tmpfs de 8 MB) |
| `backend/.../CatalogoRealTest.java` | contagens do catálogo e, ao final, por Dificuldade |
| `README.md` | a Trilha Linux completa |

## Contagens ao fim da Etapa

Catálogo: **75 Cenários**, sendo **14** da Trilha Linux. Asserções da Trilha: **35** — as 26
de hoje mais 2 no Cenário 12, 3 no 13 e 4 no 14.

Distribuição de Dificuldade da Trilha, que fecha exatamente como o design previu: **4
`Guiado`** (01, 02, 03, 08), **7 `Assistido`** (04, 05, 06, 07, 10, 11, 12), **2 `Autônomo`**
(09, 13), **1 `Mestre`** (14).

---

### Task 1: Cenário 12 — Onde este pacote vai dar

**Files:**
- Create: `content/linux/12-onde-este-pacote-vai-dar/{cenario.md,verificacao.yaml,workspace/Dockerfile,workspace/compose.yaml}`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: o serviço do Cenário 08 e o `Address already in use` que o Cenário 09 avisou
  que apareceria.
- Produces: o `ss -tlnp` que o Cenário 14 vai exigir sem avisar.

- [ ] **Step 1: Atualizar contagens e ver falhar**

```java
        assertThat(cenarios).hasSize(73);
```

```java
        assertThat(linux).hasSize(12);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(28);
```

Run: `cd backend; ./mvnw test -Dtest=CatalogoRealTest` → FAIL, `expected size: 73 but was: 72`.

- [ ] **Step 2: Workspace**

`Dockerfile` copiado do Cenário 08. `compose.yaml` igual ao do 08, com `"8042:8042"`.

- [ ] **Step 3: Verificação**

```yaml
asercoes:
  - tipo: comando_produz
    descricao: o catálogo escuta na 8042 e você sabe qual processo é o dono
    comando: ["docker", "exec", "learning-infra-linux", "sh", "-c",
              "ss -tlnp | grep ':8042 ' | grep -q 'users:' && echo porta-com-dono || echo porta-sem-dono"]
    contem: porta-com-dono

  - tipo: http_responde
    url: http://localhost:8042
    status: 200
```

- [ ] **Step 4: Escrever o Cenário**

Frontmatter: `id: linux/12-onde-este-pacote-vai-dar`, `titulo: Onde este pacote vai dar`,
`dificuldade: assistido`, `projetoCompose: linux-12`,
`containerLinux: learning-infra-linux`.

Seis seções `##`:

1. `## A pressão` — o catálogo responde na máquina e não responde do navegador de quem está
   fora. Ninguém sabe dizer se o problema é o programa, a máquina ou o caminho até ela.
   A promessa: quatro comandos separam essas três coisas.

2. `## Que endereços esta máquina tem` —

   ```bash
   ip -brief addr
   ip addr show eth0
   ```

   Saída medida: `lo UNKNOWN 127.0.0.1/8` e `eth0@if132 UP 172.24.0.2/16`.

   Nomeie as duas interfaces. O `lo` é o **loopback**: existe só dentro da máquina, e é por
   ele que `localhost` funciona. O `eth0` é a placa de rede, com o endereço pelo qual a
   máquina é alcançada de fora. Explique o `/16` como a máscara — quantos bits do endereço
   identificam a **rede** e não o host.

3. `## Por onde um pacote sai` —

   ```bash
   ip route
   ```

   Saída medida: `default via 172.24.0.1 dev eth0` mais a rota da rede local. Leia as duas
   linhas: a segunda diz "para a minha própria rede, mande direto"; a primeira diz "para
   qualquer outro lugar, entregue ao roteador". **Rota `default` é o "não sei, pergunte
   àquele ali"** — e é a primeira coisa a olhar quando a máquina alcança os vizinhos e não
   alcança a internet.

4. `## Do nome ao endereço` —

   ```bash
   cat /etc/resolv.conf
   getent hosts archive.ubuntu.com
   ```

   O `resolv.conf` aponta para `nameserver 127.0.0.11`, que é o resolvedor interno do
   Docker. Diga que numa máquina de verdade esse endereço seria o do DNS da rede.

   Explique por que `getent hosts` e não `ping`: o `getent` pergunta exatamente o que o
   sistema perguntaria — passando por `/etc/hosts` e pelo DNS na ordem configurada — enquanto
   o `ping` mistura resolução de nome com alcance de rede e confunde dois problemas
   diferentes num sintoma só.

5. `## Quem está ouvindo` — o miolo. Suba o catálogo como serviço:

   ```bash
   tee /etc/systemd/system/catalogo.service > /dev/null <<'EOF'
   [Unit]
   Description=Catálogo da Aurora

   [Service]
   ExecStart=/usr/bin/python3 -m http.server 8042 --directory /srv/catalogo

   [Install]
   WantedBy=multi-user.target
   EOF
   systemctl daemon-reload
   systemctl enable --now catalogo.service
   ss -tlnp
   ```

   Saída medida da linha que importa:

   ```
   LISTEN 0  5  0.0.0.0:8042  0.0.0.0:*  users:(("python3",pid=223,fd=3))
   ```

   Disseque: `LISTEN` é o estado; `0.0.0.0:8042` é onde ele aceita conexão; e
   `users:(("python3",pid=223,fd=3))` é **o dono** — nome, PID e o descritor de arquivo do
   Cenário 07 aparecendo de novo.

   Explique as letras: `-t` TCP, `-l` só quem escuta, `-n` números em vez de nomes de
   serviço, `-p` o processo. E a diferença que resolve metade dos incidentes de rede:
   **`0.0.0.0` aceita de qualquer lugar; `127.0.0.1` aceita só de dentro da máquina.** Um
   serviço que só escuta no loopback funciona para quem testa por dentro e é invisível de
   fora — que é exatamente o sintoma da abertura.

6. `## Verificação` — a Verificação cobra a porta **com dono identificável** e a resposta
   HTTP. Feche ligando ao Cenário 09: aquele `Address already in use` que o journal mostrou
   agora tem um comando que responde quem está ocupando.

- [ ] **Step 5: Rodar de verdade**

```powershell
cd content\linux\12-onde-este-pacote-vai-dar\workspace
docker compose -p linux-12 up -d --build
docker exec -it learning-infra-linux bash
```

Confira que `ss -tlnp` mostra `users:((...))` — sem essa coluna a primeira Asserção falha.

```powershell
docker exec learning-infra-linux sh -c "ss -tlnp | grep ':8042 ' | grep -q 'users:' && echo porta-com-dono || echo porta-sem-dono"
curl.exe -s -o NUL -w "%{http_code}" http://localhost:8042
```

Expected: `porta-com-dono` e `200`.

- [ ] **Step 6: Suíte, teardown, commit**

```bash
git commit -m "feat: Cenário 12 da Trilha Linux ensina interface, rota e porta em escuta"
```

---

### Task 2: Cenário 13 — Acesso mínimo

O segundo `Autônomo`. E o Cenário mais honesto da Trilha: ele ensina capability **e** diz o
que este ambiente não consegue provar.

**Files:**
- Create: `content/linux/13-acesso-minimo/{cenario.md,verificacao.yaml,workspace/Dockerfile,workspace/compose.yaml}`
- Modify: `CatalogoRealTest.java`

**Interfaces:**
- Consumes: usuário e grupo do Cenário 02, unit do 08, `ss -tlnp` do 12.
- Produces: o serviço não-root que o Cenário 14 pode quebrar por permissão.

- [ ] **Step 1: Atualizar contagens e ver falhar**

```java
        assertThat(cenarios).hasSize(74);
```

```java
        assertThat(linux).hasSize(13);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(31);
```

- [ ] **Step 2: Workspace** — `Dockerfile` do 08; `compose.yaml` com `"8043:8043"`.

- [ ] **Step 3: Verificação**

```yaml
asercoes:
  - tipo: arquivo_linux
    caminho: /etc/sudoers.d/aurora-svc
    modo: "440"
    dono: root
    grupo: root
    descricao: a regra de sudo existe e não é gravável por ninguém além do root

  - tipo: comando_produz
    descricao: o catálogo é servido por um processo que não é root
    comando: ["docker", "exec", "learning-infra-linux", "sh", "-c",
              "if ss -tlnp | grep -q ':8043 '; then ps -o user= -p $(ss -tlnp | grep ':8043 ' | grep -oP 'pid=\\K[0-9]+' | head -1) | tr -d ' '; else echo ninguem-escutando; fi"]
    contem: aurora-svc

  - tipo: comando_produz
    descricao: o binário marcado carrega exatamente a capability concedida
    comando: ["docker", "exec", "learning-infra-linux", "getcap", "/usr/local/bin/py-rede"]
    contem: cap_net_raw
```

A segunda é a que o design pede como "o usuário do processo": ela lê o PID do dono da porta
e pergunta ao `ps` quem é. Se ninguém estiver escutando, imprime outra coisa e reprova com
mensagem clara em vez de estourar.

- [ ] **Step 4: Escrever o Cenário**

Frontmatter: `dificuldade: autonomo`, `projetoCompose: linux-13`.

Seis seções `##`:

1. `## A situação` — a Aurora vai contratar alguém para operar o catálogo, e essa pessoa
   precisa reiniciar o serviço sem virar dona da máquina. E o próprio catálogo roda como
   `root` hoje, sem precisar. **O objetivo:** o catálogo servindo em
   <http://localhost:8043> sob um usuário que não é `root`, e uma regra de `sudo` que
   permita **só** reiniciar aquele serviço.

2. `## Root não é tudo ou nada` — comece desfazendo a ideia herdada dos Fundamentos:

   ```bash
   capsh --print | head -3
   ```

   Saída medida — e é surpreendente:

   ```
   Current: cap_chown,cap_dac_override,cap_fowner,cap_fsetid,cap_kill,cap_setgid,
   cap_setuid,cap_setpcap,cap_net_bind_service,cap_net_raw,cap_sys_chroot,cap_mknod,
   cap_audit_write,cap_setfcap=ep
   ```

   São **catorze** capabilities, não todas. O `root` desta máquina não pode carregar módulo
   de kernel, nem mudar o relógio, nem mexer na rede do host — o Docker já removeu essas
   antes de você entrar.

   É a prova concreta do que os Fundamentos afirmaram: **"root" é o nome que se dá a ter
   todas as capabilities**, e nada impede que se tenha só algumas. É assim que se dá a um
   programa o poder de que ele precisa sem dar o resto.

3. `## Uma capability num arquivo` —

   ```bash
   getcap -r /usr/bin /usr/sbin
   cp "$(readlink -f /usr/bin/python3)" /usr/local/bin/py-rede
   setcap cap_net_raw+ep /usr/local/bin/py-rede
   getcap /usr/local/bin/py-rede
   ```

   Saída medida: `/usr/local/bin/py-rede cap_net_raw=ep`.

   Explique o `+ep`: **e**ffective, o processo já nasce com ela ativa; **p**ermitted, ele
   tem direito a ela. E o que `cap_net_raw` concede — abrir socket bruto, que é o que o
   `ping` precisa e um servidor HTTP não. **A pergunta certa nunca é "isso precisa de
   root?", e sim "de qual poder, exatamente, isso precisa?"**

4. `## O que este ambiente não prova` — a seção que não pode faltar, e que vale mais que as
   outras porque ensina desconfiança calibrada.

   O exemplo clássico de capability é a porta baixa: um programa não-root não conseguiria
   escutar na porta 80 sem `CAP_NET_BIND_SERVICE`. **Aqui isso não acontece.** Meça:

   ```bash
   cat /proc/sys/net/ipv4/ip_unprivileged_port_start
   ```

   Responde `0`. Esse ajuste define a partir de qual porta um processo comum pode escutar, e
   o Docker o deixa em zero — então, neste container, **qualquer usuário liga na porta 80**.
   E não dá para corrigir para estudar:

   ```bash
   sysctl -w net.ipv4.ip_unprivileged_port_start=1024
   ```

   Responde `Read-only file system`. O `/proc/sys` é o do host, montado somente para
   leitura — a linha "Compartilhado" da tabela de fidelidade dos Fundamentos, acontecendo na
   sua frente.

   Numa máquina de verdade o valor é `1024` e a regra vale. Guarde a lição maior: **saber o
   que o seu laboratório não reproduz é parte de saber operar.** Por isso este Cenário usa a
   porta 8043, que não precisaria de capability nenhuma nem lá fora.

5. `## Faça` — sem passo a passo. Ao final, três coisas precisam ser verdade:

   1. o catálogo responde em <http://localhost:8043>;
   2. o processo que escuta na 8043 **não é `root`**;
   3. existe `/etc/sudoers.d/aurora-svc` em modo `440`, permitindo àquele usuário **apenas**
      reiniciar o serviço do catálogo.

   Dê as peças, não a montagem:

   - `useradd -r -s /usr/sbin/nologin` cria usuário de serviço, sem shell e sem casa;
   - na unit, `User=` troca a identidade; `NoNewPrivileges=true` impede o processo de ganhar
     poder depois de iniciado; `CapabilityBoundingSet=` limita o teto do que ele pode pedir;
   - regras de `sudo` vão em arquivos dentro de `/etc/sudoers.d/`, um por assunto, em vez de
     editar o `/etc/sudoers` central;
   - **valide antes de confiar**: `visudo -c` responde `parsed OK` por arquivo. Um erro de
     sintaxe ali tranca o `sudo` da máquina inteira, e essa é a única linha deste Cenário
     que você não deve pular.

6. `## Por que 440` — desmonte o folclore com medição. O modo `440` é **convenção**, não
   exigência: o `sudo` só recusa o arquivo quando ele é **gravável por outros**. Medido:

   | Modo | O que o `sudo` faz |
   |---|---|
   | `440`, `444`, `400` | aceita |
   | `664` | aceita |
   | `646`, `666` | recusa: `is world writable` |

   Experimente:

   ```bash
   chmod 646 /etc/sudoers.d/aurora-svc
   runuser -u aurora-svc -- sudo -n -l
   chmod 440 /etc/sudoers.d/aurora-svc
   ```

   Então por que todo mundo escreve `440`? Porque é o mínimo que funciona e **expressa a
   intenção**: o `root` lê, o grupo do `root` lê, ninguém escreve. Um arquivo que decide
   quem vira `root` não tem motivo para ser gravável nem legível por mais ninguém — e o
   `/etc/sudoers.d/README` que já veio na imagem está exatamente assim.

   Feche dizendo o que a Verificação cobra: a regra em `440`, o processo da 8043 rodando
   como usuário de serviço, e a capability no binário marcado.

- [ ] **Step 5: Rodar de verdade** — monte a solução você mesmo, como um leitor faria, e
  valide as três Asserções. Atenção ao `getcap`: se o `cp` do binário não for do executável
  real (`readlink -f`), o `setcap` marca um symlink e a Asserção falha.

- [ ] **Step 6: Suíte, teardown, commit**

```bash
git commit -m "feat: Cenário 13 da Trilha Linux ensina sudo, capability e acesso mínimo"
```

---

### Task 3: Cenário 14 — O plantão

O único `Mestre` da Trilha. **Nenhuma seção pode dizer o que quebrou.**

**Files:**
- Create: `content/linux/14-o-plantao/{cenario.md,verificacao.yaml,workspace/Dockerfile,workspace/compose.yaml}`
- Modify: `CatalogoRealTest.java`

- [ ] **Step 1: Atualizar contagens e ver falhar**

```java
        assertThat(cenarios).hasSize(75);
```

```java
        assertThat(linux).hasSize(14);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(35);
```

- [ ] **Step 2: Workspace — o compose que combina 07 e 12**

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
    ports:
      - "8044:8044"
```

- [ ] **Step 3: Verificação — as três famílias juntas**

```yaml
asercoes:
  - tipo: servico_systemd
    nome: catalogo.service
    ativo: true
    habilitado: true
    descricao: o catálogo voltou, ativo e habilitado

  - tipo: http_responde
    url: http://localhost:8044
    status: 200

  - tipo: arquivo_linux
    caminho: /srv/catalogo/index.html
    modo: "644"
    dono: root
    descricao: o conteúdo do catálogo está legível por quem serve

  - tipo: comando_produz
    descricao: a partição de log saiu do vermelho
    comando: ["docker", "exec", "learning-infra-linux", "sh", "-c",
              "P=$(df --output=pcent /var/log/aurora | tail -1 | tr -d ' %'); if [ \"$P\" -lt 50 ]; then echo espaco-liberado; else echo ainda-cheio; fi"]
    contem: espaco-liberado
```

- [ ] **Step 4: Escrever o Cenário**

Frontmatter: `dificuldade: mestre`, `projetoCompose: linux-14`.

**Quatro seções apenas**, e nenhuma delas nomeia uma falha:

1. `## 03h47` — o chamado. O catálogo da Aurora está fora do ar. Você é quem está de
   plantão. Não há quem perguntar.

2. `## Ponha o ambiente no ar` — o bloco de preparo. É o **único** lugar com comandos, e ele
   é opaco de propósito: um script que "restaura o estado em que a máquina foi encontrada".
   Escreva-o de forma que o leitor **possa** ler se quiser — está no terminal dele, seria
   desonesto esconder — mas sem comentário nenhum explicando o efeito de cada linha.

   O preparo precisa produzir, exatamente, estas quatro falhas:

   - a unit `catalogo.service` instalada em **`/usr/lib/systemd/system/`** e depois
     **mascarada** — obrigatório nascer ali, senão o `mask` falha (medido);
   - `/srv/catalogo/index.html` com modo **`600`**, ilegível para quem serve;
   - `/var/log/aurora` **cheio**, com um arquivo grande apagado e ainda seguro por um
     processo vivo — o incidente do Cenário 07;
   - a porta **8044 ocupada** por um processo que não é o catálogo.

   Feche a seção com o objetivo, e nada mais: **o catálogo respondendo em
   <http://localhost:8044>, ativo e habilitado, e a máquina saudável.**

3. `## O que você já sabe` — sem dica sobre este plantão. Uma lista de perguntas, cada uma
   apontando para o Cenário que a respondeu:

   | Pergunta | Onde você aprendeu |
   |---|---|
   | quem está rodando e consumindo? | 04 |
   | quanto espaço e quantos inodes restam? | 07 |
   | esta unit existe, está habilitada, está mascarada? | 08, 09 |
   | o que o serviço disse antes de morrer? | 09, 11 |
   | quem é o dono desta porta? | 12 |
   | quem pode ler este arquivo? | 02, 13 |

   Uma frase de método, que é o que um `Mestre` deve deixar: **vá do sintoma para a
   máquina, não da máquina para o sintoma.** Comece pelo que o usuário não consegue fazer e
   ande para trás.

   E um aviso honesto, que a medição tornou necessário: `list-unit-files --state=masked`
   mostra **várias** units mascaradas nesta imagem, e a maioria é assim de fábrica. Encontrar
   uma unit mascarada não é, por si só, encontrar o problema.

4. `## Verificação` — diga apenas o que é cobrado, sem dizer como chegar: serviço ativo e
   habilitado, catálogo respondendo, conteúdo legível, partição de log fora do vermelho.

   Depois, o **fecho da Trilha**, que é o fecho do arco narrativo. A Aurora chegou ao limite
   do que um servidor operado à mão aguenta: cada conserto desta noite foi manual, nada
   está descrito em lugar nenhum, e a próxima pessoa vai começar do zero como você começou
   no Cenário 01. Nomeie o problema sem vender a solução — **empacotar a aplicação junto com
   tudo de que ela precisa, para que subir de novo seja repetível** — e diga que é onde a
   Trilha Docker começa. Amarre com os Fundamentos: namespace e cgroup, os dois mecanismos
   do kernel que você já viu nomeados, são o que torna isso possível.

- [ ] **Step 5: Rodar de verdade — o passo mais importante desta Etapa**

Rode o preparo e **confirme as quatro falhas separadamente** antes de consertar:

```powershell
docker exec learning-infra-linux systemctl is-enabled catalogo.service   # masked
docker exec learning-infra-linux stat -c "%a" /srv/catalogo/index.html   # 600
docker exec learning-infra-linux df -h /var/log/aurora                   # ~100%
docker exec learning-infra-linux sh -c "ss -tlnp | grep ':8044 '"        # ocupada por outro
```

Depois **resolva o plantão inteiro como um leitor**, sem consultar o preparo, e valide as
quatro Asserções. Se qualquer uma passar **antes** do conserto, a falha correspondente não
foi criada — corrija o preparo.

- [ ] **Step 6: Suíte, teardown, commit**

```bash
git commit -m "feat: Cenário 14 da Trilha Linux fecha a Trilha com o plantão"
```

---

### Task 4: Fechar a Trilha

**Files:**
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`
- Modify: `README.md`

- [ ] **Step 1: Cobrar a distribuição de Dificuldade**

Agora que os 14 estão no disco, acrescente ao `CatalogoRealTest`, no mesmo molde do que a
Trilha IaC já tem:

```java
        assertThat(linux).filteredOn(cenario -> cenario.dificuldade() == Dificuldade.GUIADO)
                .hasSize(4);
        assertThat(linux).filteredOn(cenario -> cenario.dificuldade() == Dificuldade.ASSISTIDO)
                .hasSize(7);
        assertThat(linux).filteredOn(cenario -> cenario.dificuldade() == Dificuldade.AUTONOMO)
                .hasSize(2);
        assertThat(linux).filteredOn(cenario -> cenario.dificuldade() == Dificuldade.MESTRE)
                .hasSize(1);
```

- [ ] **Step 2: README** — a Trilha Linux passa a "completa: Fundamentos e os 14 Cenários
  nos quatro atos". A linha de portas passa a **8040 a 8044**. Registre que o Cenário 14,
  como o 07, monta o tmpfs de 8 MB.

- [ ] **Step 3: Rodar tudo**

```powershell
cd backend; ./mvnw test
cd ..\frontend; node scripts-checar-conteudo.mjs linux
```

- [ ] **Step 4: Commit**

```bash
git commit -m "docs: Trilha Linux completa, com a distribuição de Dificuldade coberta por teste"
```

---

## O que fica aberto depois desta Etapa

- **O risco do `cgroup: host`** segue sem teste automatizado, pelo mesmo motivo das Etapas
  3 a 5: seria o primeiro teste da suíte a depender de Docker no CI. Com os 14 Cenários no
  ar, a Trilha depende de três comportamentos do Docker Desktop que ninguém verifica —
  `cgroup: host`, `nr_inodes` no tmpfs, e o `ip_unprivileged_port_start` continuar em `0`
  (se mudar, o Cenário 13 passa a dizer uma coisa que não é mais verdade).
- **O `man` continua quebrado.** Nenhum Cenário depende dele.
- **Shell scripting avançado** segue fora de escopo, como o design determinou.
