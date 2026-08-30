# Trilha Linux — Etapa 3: Ato I, Cenários 01 a 03

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Abrir a Trilha Linux pela porta certa — os três Cenários `Guiado` do Ato I, que
levam o leitor de "recebi um acesso e nenhuma documentação" a "sei onde as coisas moram,
de quem elas são e como derivar uma resposta de um arquivo de texto".

**Architecture:** Nenhuma Asserção nova e nenhuma mudança de motor: os três Cenários
reusam `arquivo_linux` e `comando_produz`, que a Etapa 1 já entregou. A mudança de
plataforma é uma só e acontece **uma vez para as catorze**: o `Dockerfile` cresce com o
ferramental que o Ato I precisa e passa a semear o conteúdo que dá passado à máquina da
Aurora. Um teste novo tranca esse `Dockerfile` como idêntico em todos os Cenários da
Trilha, que é a mitigação de risco que o design pede e que ainda não existia.

**Tech Stack:** Ubuntu 26.04 (pinado por digest) · systemd 259 · Docker Compose ·
Python 3.14.4 (da imagem) · SnakeYAML · JUnit 5 · AssertJ

**Spec:** [`docs/superpowers/specs/2026-08-21-trilha-linux-design.md`](../specs/2026-08-21-trilha-linux-design.md)

## Global Constraints

- **Todas as restrições das Etapas [1](./2026-08-25-trilha-linux-etapa-1-plataforma.md) e
  [2](./2026-08-25-trilha-linux-etapa-2-fundamentos.md) continuam valendo.**
- A imagem base é `ubuntu:26.04@sha256:889d056d5c6c0bfb55789ff3710681d68e50713cb562d2196dc07110599c7a6f`,
  pinada por digest. Nenhuma outra imagem entra na Trilha.
- **Todo Cenário da Trilha Linux carrega o mesmo `Dockerfile`, byte a byte.** Um
  `Dockerfile` divergente por Cenário devolveria o custo de build inteiro a cada Iniciar.
  A Task 1 cria o teste que garante isso.
- `cgroup: host`, `tmpfs: /run` e `/run/lock`, e o bind de `/sys/fs/cgroup` são
  obrigatórios no `compose.yaml` de todo Cenário. Sem `cgroup: host` o container morre.
- `container_name: learning-infra-linux` e `hostname: aurora` em todos os Cenários.
- O frontmatter de todo Cenário Linux declara `containerLinux: learning-infra-linux` e
  `projetoCompose: linux-NN`. O `verificacao.yaml` **nunca** declara `container` — o
  backend o injeta a partir do frontmatter.
- Todo texto em **português**. **Personagens são referidos por papel, não por pronome.**
- Comandos que o leitor roda **no host** são PowerShell; comandos **dentro da máquina**
  são bash. O Cenário sempre deixa claro em qual dos dois o leitor está.
- A Trilha Linux usa o bloco de portas **8040–8049**. Os Cenários 01, 02 e 03 **não
  servem nada** e portanto não publicam porta nenhuma.
- Dificuldade dos três: **`guiado`**. Esta é a porta de entrada da plataforma inteira e a
  primeira hora de um leitor novo não é lugar para autonomia.
- A narrativa é contínua: o Cenário 02 parte do servidor que o 01 deixou, e o 03 do que o
  02 deixou. Nenhum dos três reintroduz o `docker exec` do zero — o 01 é quem o ensina.

## Medições que fundamentam este plano

Feitas em 2026-08-29, na máquina do autor, com Docker Engine 29.6.1:

| Medição | Resultado |
|---|---|
| Imagem atual (`systemd procps iproute2 sudo curl python3`) | 270 MB, sobe `running`, zero units falhas |
| `less`, `nano`, `file`, `lsof` na imagem atual | **ausentes** — o Ato I não tem como ler nem editar |
| `man` na imagem atual | **quebrado**: a imagem Ubuntu vem minimizada, sem manpages |
| Imagem com os quatro pacotes | 284 MB (**+14 MB**), build a frio **48 s**, sobe `running`, zero falhas |
| Semente gerada por `RUN python3` com `random.seed(2026)` | `md5 = 8c64c3bea08a1c093b7491b6dc1f4e2e`, **idêntico num rebuild `--no-cache`** |
| `/var/log/aurora/pedidos.log` semeado | 2160 linhas, 156.725 bytes, `644 root root` |
| Acento no conteúdo semeado | íntegro, `locale charmap` = `UTF-8` |
| `stat -c "%a %U %G" /etc/os-release` | `777 root root` — **é symlink**; `stat` não segue por padrão |

A última linha é uma armadilha real: uma Asserção `arquivo_linux` apontada para
`/etc/os-release` mediria o modo do symlink, não o do arquivo. Nenhuma Asserção deste
plano aponta para symlink.

## Estrutura de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `content/linux/08-um-programa-vira-servico/workspace/Dockerfile` | o `Dockerfile` canônico cresce aqui primeiro |
| `content/linux/01-voce-herdou-um-servidor/` | `cenario.md`, `verificacao.yaml`, `workspace/Dockerfile`, `workspace/compose.yaml` |
| `content/linux/02-todo-arquivo-tem-dono/` | idem |
| `content/linux/03-o-texto-e-a-interface/` | idem |
| `backend/src/test/java/dev/learninginfra/conteudo/SubstratoLinuxTest.java` | tranca o `Dockerfile` idêntico em todos os Cenários da Trilha |
| `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java` | contagens do catálogo |
| `content/linux/08-um-programa-vira-servico/cenario.md` | deixa de ser a porta de entrada e passa a ser o Cenário 08 |
| `README.md` | o bloco de portas e o aviso de custo do primeiro build |

## O contrato de Asserções do Ato

Nenhum dos três Cenários serve HTTP, então nenhum usa `http_responde`. A regra de "efeito
visível acompanhado de configuração" da Etapa 1 vale para serviço; aqui o par equivalente
é **estado no disco** (`arquivo_linux`) acompanhado de **consequência observável**
(`comando_produz`). Um diretório com o modo certo que não faz o que promete não conclui
Cenário.

Contagem final de Asserções da Trilha depois desta Etapa: **11** — 2 no Cenário 01, 3 no
02, 3 no 03 e as 3 que o 08 já tem.

---

### Task 1: O substrato cresce uma vez e fica trancado

O Ato I ensina a ler e a editar arquivos numa máquina, e a imagem não tem pager nem
editor. Crescer o `Dockerfile` é obrigatório, e por causa da regra do byte-a-byte é uma
mudança que precisa ser feita **uma vez, agora**, cobrindo também o que os Atos seguintes
vão precisar (`lsof`, para o Cenário 07). Junto vem a semente: o Cenário 01 é "você
herdou um servidor", e um servidor herdado sem passado nenhum não tem o que inventariar.

Esta Task começa pelo teste que tranca a regra, porque hoje ela é só uma frase no design
— não há nada impedindo o próximo Cenário de divergir.

**Files:**
- Create: `backend/src/test/java/dev/learninginfra/conteudo/SubstratoLinuxTest.java`
- Modify: `content/linux/08-um-programa-vira-servico/workspace/Dockerfile`
- Modify: `content/linux/08-um-programa-vira-servico/cenario.md`

**Interfaces:**
- Consumes: nada.
- Produces: o `Dockerfile` canônico que as Tasks 2, 3 e 4 copiam byte a byte para os
  `workspace/` novos; e o caminho semeado `/var/log/aurora/pedidos.log`, cujo conteúdo a
  Task 4 consome com números exatos.

- [ ] **Step 1: Escrever o teste que tranca o Dockerfile**

O teste normaliza fim de linha antes de comparar: o repositório não tem `.gitattributes`,
então um checkout com `core.autocrlf=true` entregaria CRLF e um teste ingênuo reprovaria
por um motivo que não é o do design.

Create `backend/src/test/java/dev/learninginfra/conteudo/SubstratoLinuxTest.java`:

```java
package dev.learninginfra.conteudo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * O substrato da Trilha Linux é um só. Todo Cenário carrega o mesmo Dockerfile para que
 * o cache de camadas sobreviva à troca de Cenário ativo — um Dockerfile divergente
 * devolveria o custo do build inteiro a cada Iniciar.
 */
class SubstratoLinuxTest {

    @Test
    void todoCenarioLinuxCarregaOMesmoDockerfile() {
        List<Path> dockerfiles = dockerfilesDaTrilhaLinux();

        assertThat(dockerfiles)
                .as("todo Cenário da Trilha Linux precisa de um workspace/Dockerfile")
                .isNotEmpty();

        Path referencia = dockerfiles.get(0);
        String esperado = conteudoNormalizado(referencia);
        for (Path dockerfile : dockerfiles) {
            assertThat(conteudoNormalizado(dockerfile))
                    .as("%s diverge de %s", dockerfile, referencia)
                    .isEqualTo(esperado);
        }
    }

    @Test
    void oDockerfileFixaAImagemPorDigestEOLocale() {
        String conteudo = conteudoNormalizado(dockerfilesDaTrilhaLinux().get(0));

        assertThat(conteudo).contains(
                "ubuntu:26.04@sha256:"
                + "889d056d5c6c0bfb55789ff3710681d68e50713cb562d2196dc07110599c7a6f");
        assertThat(conteudo).contains("ENV LANG=C.UTF-8");
        assertThat(conteudo).contains("systemctl mask tmp.mount");
    }

    private List<Path> dockerfilesDaTrilhaLinux() {
        Path trilha = localizarConteudo().resolve("linux");
        try (Stream<Path> diretorios = Files.list(trilha)) {
            return diretorios
                    .filter(Files::isDirectory)
                    .map(diretorio -> diretorio.resolve("workspace").resolve("Dockerfile"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }

    private String conteudoNormalizado(Path arquivo) {
        try {
            return Files.readString(arquivo, StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }

    private Path localizarConteudo() {
        Path naRaiz = Path.of("content");
        return Files.isDirectory(naRaiz) ? naRaiz : Path.of("..", "content");
    }
}
```

- [ ] **Step 2: Rodar o teste e ver os dois métodos passarem**

Run: `cd backend; ./mvnw -q test -Dtest=SubstratoLinuxTest`
Expected: PASS nos dois. Com um único Cenário no disco a comparação é trivialmente
verdadeira — é o Step 4 que prova que o teste morde.

- [ ] **Step 3: Crescer o Dockerfile do Cenário 08**

Replace `content/linux/08-um-programa-vira-servico/workspace/Dockerfile` inteiro:

```dockerfile
FROM ubuntu:26.04@sha256:889d056d5c6c0bfb55789ff3710681d68e50713cb562d2196dc07110599c7a6f

ENV LANG=C.UTF-8

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        systemd procps iproute2 sudo curl python3 less nano file lsof \
    && systemctl mask tmp.mount \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /srv/catalogo /etc/aurora /var/log/aurora

RUN printf '%s\n' \
        '<h1>Aurora — catálogo</h1>' \
        '<p>Editora Aurora. Catálogo de títulos.</p>' \
        > /srv/catalogo/index.html \
    && printf '%s\n' \
        '# Catálogo da Aurora — montado em 2019, sem documentação.' \
        'porta=8040' \
        'raiz=/srv/catalogo' \
        > /etc/aurora/catalogo.conf

RUN python3 <<'PY'
import random
random.seed(2026)
clientes = ["livraria-do-porto", "banca-central", "papelaria-sul",
            "leitura-norte", "sebo-da-praca"]
linhas = []
pedido = 4000
for hora in range(24):
    for minuto in range(0, 60, 2):
        for segundo in (7, 23, 51):
            pedido += 1
            cliente = random.choice(clientes)
            if cliente == "livraria-do-porto":
                status = 500 if 2 <= hora <= 4 else random.choices([200, 404], [95, 5])[0]
            else:
                status = random.choices([200, 404, 500], [92, 5, 3])[0]
            ms = random.randint(18, 400)
            linhas.append(
                f"2026-08-14T{hora:02d}:{minuto:02d}:{segundo:02d} "
                f"pedido={pedido} cliente={cliente} status={status} ms={ms}")
with open("/var/log/aurora/pedidos.log", "w") as saida:
    saida.write("\n".join(linhas) + "\n")
PY

CMD ["/usr/lib/systemd/systemd"]
```

Três decisões que este arquivo fixa, e que **não** devem ser "melhoradas" depois:

- O heredoc `RUN python3 <<'PY'` funciona no BuildKit do Docker 29 **sem** a diretiva
  `# syntax=`. Medido. Não adicione a diretiva: ela faria todo build a frio depender de
  baixar o frontend do Docker Hub.
- A semente vive **dentro** do `Dockerfile`, não em arquivos ao lado. É o que mantém o
  invariante do design literalmente verdadeiro: um único arquivo idêntico nos catorze
  `workspace/`, sem contexto de build para sincronizar junto.
- `random.seed(2026)` com a imagem pinada por digest torna o log reprodutível. Medido:
  `md5 = 8c64c3bea08a1c093b7491b6dc1f4e2e` num rebuild `--no-cache`.

- [ ] **Step 4: Provar que o teste morde**

Introduza uma divergência temporária e confirme a reprovação, para não ficar com um teste
que passa por vacuidade:

```powershell
mkdir content\linux\zz-teste\workspace
Copy-Item content\linux\08-um-programa-vira-servico\workspace\Dockerfile content\linux\zz-teste\workspace\Dockerfile
Add-Content content\linux\zz-teste\workspace\Dockerfile "# divergente"
cd backend; ./mvnw -q test -Dtest=SubstratoLinuxTest
```

Expected: FAIL em `todoCenarioLinuxCarregaOMesmoDockerfile`, com a mensagem
`content\linux\zz-teste\workspace\Dockerfile diverge de content\linux\08-...`.

Depois remova a divergência:

```powershell
Remove-Item -Recurse -Force content\linux\zz-teste
cd backend; ./mvnw -q test -Dtest=SubstratoLinuxTest
```

Expected: PASS.

- [ ] **Step 5: Subir a imagem nova e confirmar o substrato**

```powershell
cd content\linux\08-um-programa-vira-servico\workspace
docker compose -p linux-08 up -d --build
docker exec learning-infra-linux bash -lc "systemctl is-system-running; systemctl --failed --no-legend; md5sum /var/log/aurora/pedidos.log"
```

Expected, exatamente:

```
running
8c64c3bea08a1c093b7491b6dc1f4e2e  /var/log/aurora/pedidos.log
```

A lista de units falhas vem **vazia**. Se `is-system-running` responder `degraded`, pare:
algum pacote novo trouxe uma unit que falha, e um Cenário que ensina a ler
`systemctl is-system-running` não pode começar mentindo.

- [ ] **Step 6: Corrigir o Cenário 08, que agora afirma coisa falsa**

O `cenario.md` do 08 manda o leitor criar `/srv/catalogo/index.html`. O arquivo agora vem
semeado, então essa instrução passou a ser falsa. Na seção `## O programa`, substitua o
bloco:

```bash
mkdir -p /srv/catalogo
echo '<h1>Aurora — catálogo</h1>' > /srv/catalogo/index.html
python3 -m http.server 8040 --directory /srv/catalogo
```

por:

```bash
cat /srv/catalogo/index.html
python3 -m http.server 8040 --directory /srv/catalogo
```

e ajuste a frase que introduz o bloco, hoje "Crie o conteúdo e experimente o programa
rodando na mão", para dizer que o conteúdo do catálogo **já está lá desde o Cenário 01** e
que o que falta é servi-lo. A costura narrativa completa do 08 com o Ato I é a Task 5;
aqui só se corrige a afirmação factual, porque ela quebra no instante em que este
`Dockerfile` entra.

- [ ] **Step 7: Rodar a suíte inteira**

Run: `cd backend; ./mvnw -q test`
Expected: PASS. `CatalogoRealTest` continua vendo 62 Cenários e 1 Cenário `linux`; nada
de contagem mudou nesta Task.

- [ ] **Step 8: Derrubar o ambiente**

```powershell
cd content\linux\08-um-programa-vira-servico\workspace
docker compose -p linux-08 down -v
```

- [ ] **Step 9: Commit**

```bash
git add backend/src/test/java/dev/learninginfra/conteudo/SubstratoLinuxTest.java \
        content/linux/08-um-programa-vira-servico/
git commit -m "feat: substrato da Trilha Linux ganha ferramental e semente, trancado por teste"
```

---

### Task 2: Cenário 01 — Você herdou um servidor

O primeiro Cenário da Trilha e, pela ADR 0005, o primeiro Cenário da plataforma inteira.
Ele carrega um peso que os outros não têm: é onde o leitor entra numa máquina Linux pela
primeira vez. Ensina a entrar, a se localizar, a ler o que a máquina diz sobre si mesma, e
a diferença entre "o arquivo existe" e "sei o que ele é".

**Files:**
- Create: `content/linux/01-voce-herdou-um-servidor/cenario.md`
- Create: `content/linux/01-voce-herdou-um-servidor/verificacao.yaml`
- Create: `content/linux/01-voce-herdou-um-servidor/workspace/Dockerfile`
- Create: `content/linux/01-voce-herdou-um-servidor/workspace/compose.yaml`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: o `Dockerfile` canônico da Task 1 e os caminhos semeados `/srv/catalogo`,
  `/etc/aurora/catalogo.conf` e `/var/log/aurora/pedidos.log`.
- Produces: `/srv/inventario.md`, escrito pelo leitor — o Cenário 02 parte dele, e a
  Task 3 acrescenta uma linha a esse mesmo arquivo.

- [ ] **Step 1: Atualizar as contagens do catálogo e ver o teste falhar**

Modify `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`:

```java
        assertThat(cenarios).hasSize(63);
```

```java
        assertThat(linux).hasSize(2);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(5);
```

- [ ] **Step 2: Rodar o teste e confirmar a reprovação**

Run: `cd backend; ./mvnw -q test -Dtest=CatalogoRealTest`
Expected: FAIL — `expected size: 63 but was: 62`.

- [ ] **Step 3: Criar o workspace**

`content/linux/01-voce-herdou-um-servidor/workspace/Dockerfile` é **cópia byte a byte** do
`Dockerfile` do Cenário 08:

```powershell
mkdir content\linux\01-voce-herdou-um-servidor\workspace
Copy-Item content\linux\08-um-programa-vira-servico\workspace\Dockerfile `
          content\linux\01-voce-herdou-um-servidor\workspace\Dockerfile
```

Create `content/linux/01-voce-herdou-um-servidor/workspace/compose.yaml`:

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

Sem bloco `ports:` — o Cenário 01 não serve nada.

- [ ] **Step 4: Escrever a Verificação**

Create `content/linux/01-voce-herdou-um-servidor/verificacao.yaml`:

```yaml
asercoes:
  - tipo: arquivo_linux
    caminho: /srv/inventario.md
    modo: "644"
    dono: root
    grupo: root
    descricao: o inventário existe, é legível por todos e pertence ao root

  - tipo: comando_produz
    descricao: o inventário nomeia o log de pedidos que você encontrou
    comando: ["docker", "exec", "learning-infra-linux",
              "grep", "-c", "/var/log/aurora/pedidos.log", "/srv/inventario.md"]
    contem: "1"
```

O `grep -c` devolve a contagem no stdout **e** o código de saída 1 quando não encontra
nada — então um inventário sem a linha reprova por dois caminhos independentes.

- [ ] **Step 5: Escrever o Cenário**

Create `content/linux/01-voce-herdou-um-servidor/cenario.md` com este frontmatter exato:

```yaml
---
id: linux/01-voce-herdou-um-servidor
titulo: Você herdou um servidor
dificuldade: guiado
projetoCompose: linux-01
containerLinux: learning-infra-linux
---
```

O corpo tem seis seções `##`, nesta ordem, com os comandos abaixo **verbatim** — todos já
rodados contra a imagem da Task 1:

1. `## Entre na máquina` — a abertura da Trilha inteira. Clicar em **Iniciar cenário** e,
   no PowerShell:

   ```powershell
   docker exec -it learning-infra-linux bash
   ```

   Esta é a única seção da Trilha que apresenta essa linha do zero; os outros treze
   Cenários a assumem. Explique-a como **incantação com prazo de validade**, exatamente
   como os Fundamentos fazem: a Trilha Docker abre cada pedaço dela mais adiante. Diga o
   que mudou na tela — o prompt agora é `root@aurora` — e o que isso significa: você não
   está mais no Windows, está dentro da máquina da Aurora, como o usuário mais poderoso
   dela.

2. `## Onde eu estou` — `pwd`, `ls`, `cd`, `ls -la`. O ponto da seção é que o shell sempre
   tem um diretório corrente e que todo caminho é lido a partir dele ou da raiz.

   ```bash
   pwd
   ls /
   ```

3. `## O que esta máquina diz sobre si mesma` —

   ```bash
   cat /etc/os-release
   uname -r
   hostname
   ```

   Saída medida de `cat /etc/os-release`, primeiras linhas:

   ```
   PRETTY_NAME="Ubuntu 26.04 LTS"
   NAME="Ubuntu"
   VERSION_ID="26.04"
   VERSION="26.04 LTS (Resolute Raccoon)"
   ```

   Aproveite para a distinção que os Fundamentos plantaram: `uname -r` responde sobre o
   **kernel**, `/etc/os-release` responde sobre a **distribuição**. São coisas diferentes,
   e aqui o kernel é o da VM do Docker Desktop.

4. `## Onde as coisas moram` — o FHS como convenção, não como lei. Percorra os três
   diretórios que a Aurora usa, um por vez:

   ```bash
   ls /srv/catalogo
   cat /etc/aurora/catalogo.conf
   ls -lh /var/log/aurora
   ```

   Saída medida de `cat /etc/aurora/catalogo.conf`:

   ```
   # Catálogo da Aurora — montado em 2019, sem documentação.
   porta=8040
   raiz=/srv/catalogo
   ```

   A regra a fixar, em uma frase: `/etc` guarda configuração, `/var` guarda o que cresce,
   `/srv` guarda o que a máquina serve. Quem montou a Aurora seguiu a convenção, e é por
   isso que dá para achar as coisas sem documentação nenhuma.

5. `## Existir não é a mesma coisa que saber o que é` — `stat`, `file` e `less`, os três
   como perguntas diferentes sobre o mesmo arquivo:

   ```bash
   stat /var/log/aurora/pedidos.log
   file /var/log/aurora/pedidos.log
   wc -l /var/log/aurora/pedidos.log
   less /var/log/aurora/pedidos.log
   ```

   Valores medidos: `wc -l` responde `2160`; `file` responde `ASCII text`; `stat` mostra
   `size: 156725`, `Access: (0644/-rw-r--r--)  Uid: (    0/    root)   Gid: (    0/    root)`
   e o `Inode:`, que a seção nomeia de passagem porque o Cenário 07 vai cobrá-lo.

   Diga como sair do `less`: **`q`**. Um leitor novo preso num pager é um leitor que
   desiste da Trilha, e nenhuma outra seção tem essa armadilha.

   Feche a seção com a armadilha do symlink, que é conhecimento que se paga caro depois:

   ```bash
   ls -l /etc/os-release
   ```

   Saída medida:

   ```
   lrwxrwxrwx 1 root root 21 Apr 24 10:24 /etc/os-release -> ../usr/lib/os-release
   ```

   O `l` inicial diz que isso é um atalho para outro arquivo, e `stat` sem `-L` descreve o
   atalho, não o destino.

6. `## Escreva o inventário` — o entregável. O leitor registra o que descobriu:

   ```bash
   tee /srv/inventario.md > /dev/null <<'EOF'
   # Inventário do servidor da Aurora

   - Distribuição: Ubuntu 26.04 LTS
   - Conteúdo do catálogo: /srv/catalogo
   - Configuração do catálogo: /etc/aurora/catalogo.conf
   - Log de pedidos: /var/log/aurora/pedidos.log
   EOF
   ```

   Feche explicando o que a Verificação cobra e por quê: o inventário precisa existir com
   modo `644` (a próxima pessoa também precisa ler) e precisa nomear o log — a descoberta
   que o Cenário 03 vai usar. Documentar o que se achou é a primeira coisa que se faz numa
   máquina herdada, e é a razão de o entregável deste Cenário ser um texto, não um serviço.

- [ ] **Step 6: Rodar o Cenário de verdade, do início ao fim**

Não é opcional. Suba o ambiente, execute **todos** os comandos do `cenario.md` na ordem em
que aparecem, e confira que cada saída bate com o que o texto promete:

```powershell
cd content\linux\01-voce-herdou-um-servidor\workspace
docker compose -p linux-01 up -d --build
docker exec -it learning-infra-linux bash
```

Depois valide as duas Asserções à mão, exatamente como o motor as roda:

```powershell
docker exec learning-infra-linux stat -c "%a %U %G" /srv/inventario.md
docker exec learning-infra-linux grep -c /var/log/aurora/pedidos.log /srv/inventario.md
```

Expected: `644 root root` e `1`.

- [ ] **Step 7: Rodar a suíte e derrubar o ambiente**

```powershell
cd backend; ./mvnw -q test
cd ..\content\linux\01-voce-herdou-um-servidor\workspace; docker compose -p linux-01 down -v
```

Expected: PASS, incluindo `SubstratoLinuxTest` — que agora compara dois `Dockerfile` de
verdade e não mais um só.

- [ ] **Step 8: Commit**

```bash
git add content/linux/01-voce-herdou-um-servidor/ \
        backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: Cenário 01 da Trilha Linux abre a plataforma inteira"
```

---

### Task 3: Cenário 02 — Todo arquivo tem dono

O segundo Cenário responde à segunda das quatro perguntas do modelo mental: **com que
identidade?** Ele existe para desmontar o `chmod 777` antes que o leitor o aprenda em
outro lugar, e o instrumento é um problema concreto: duas pessoas do editorial precisam
escrever no mesmo diretório sem apagar o acesso uma da outra.

**Files:**
- Create: `content/linux/02-todo-arquivo-tem-dono/cenario.md`
- Create: `content/linux/02-todo-arquivo-tem-dono/verificacao.yaml`
- Create: `content/linux/02-todo-arquivo-tem-dono/workspace/Dockerfile`
- Create: `content/linux/02-todo-arquivo-tem-dono/workspace/compose.yaml`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: o `Dockerfile` canônico da Task 1; a máquina que o Cenário 01 deixou.
- Produces: o usuário `ana`, o usuário `bruno`, o grupo `editorial` e o diretório
  compartilhado `/srv/editorial` com setgid — todos criados pelo leitor, nada semeado.

- [ ] **Step 1: Atualizar as contagens e ver o teste falhar**

Modify `CatalogoRealTest.java`:

```java
        assertThat(cenarios).hasSize(64);
```

```java
        assertThat(linux).hasSize(3);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(8);
```

Run: `cd backend; ./mvnw -q test -Dtest=CatalogoRealTest`
Expected: FAIL — `expected size: 64 but was: 63`.

- [ ] **Step 2: Criar o workspace**

```powershell
mkdir content\linux\02-todo-arquivo-tem-dono\workspace
Copy-Item content\linux\08-um-programa-vira-servico\workspace\Dockerfile `
          content\linux\02-todo-arquivo-tem-dono\workspace\Dockerfile
```

Create `content/linux/02-todo-arquivo-tem-dono/workspace/compose.yaml` — idêntico ao do
Cenário 01, sem bloco `ports:`:

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

- [ ] **Step 3: Escrever a Verificação**

Create `content/linux/02-todo-arquivo-tem-dono/verificacao.yaml`:

```yaml
asercoes:
  - tipo: arquivo_linux
    caminho: /srv/editorial
    modo: "2770"
    dono: root
    grupo: editorial
    descricao: o diretório do editorial é do grupo editorial, fechado para os outros e com setgid

  - tipo: arquivo_linux
    caminho: /srv/editorial/pauta.md
    grupo: editorial
    descricao: o arquivo criado dentro herdou o grupo do diretório, que é o que o setgid faz

  - tipo: comando_produz
    descricao: os dois usuários do editorial estão no grupo
    comando: ["docker", "exec", "learning-infra-linux",
              "sh", "-c", "id -nG ana; id -nG bruno"]
    contem: editorial
```

A segunda Asserção é a que separa "modo certo" de "modo que funciona": um `2770` copiado
sem entender passaria na primeira e falharia na segunda se o arquivo tivesse nascido fora
do diretório.

- [ ] **Step 4: Escrever o Cenário**

Create `content/linux/02-todo-arquivo-tem-dono/cenario.md` com este frontmatter:

```yaml
---
id: linux/02-todo-arquivo-tem-dono
titulo: Todo arquivo tem dono
dificuldade: guiado
projetoCompose: linux-02
containerLinux: learning-infra-linux
---
```

Cinco seções `##`, com estes comandos verbatim:

1. `## A pressão` — a Aurora contratou duas pessoas para o editorial. As duas precisam
   escrever nos mesmos arquivos de pauta. Hoje tudo em `/srv` é do `root`, e a saída
   preguiçosa — dar `777` e seguir a vida — é justamente o que este Cenário existe para
   impedir. Não abra com teoria; abra com o problema.

2. `## Os três campos` — releia um arquivo que já existe com o vocabulário novo:

   ```bash
   ls -l /etc/aurora/catalogo.conf
   stat -c "%a %U %G" /etc/aurora/catalogo.conf
   ```

   Saída medida do `stat`: `644 root root`.

   Explique o octal como três dígitos para três públicos — dono, grupo, outros — e cada
   dígito como a soma de 4, 2 e 1. `644` é "o dono lê e escreve, o resto só lê". Nomeie
   também o que `root` é de verdade, como os Fundamentos já disseram: não um usuário
   especial, e sim aquele para quem a checagem não acontece.

3. `## Identidade` — crie as pessoas e o grupo:

   ```bash
   groupadd editorial
   useradd -m -g editorial ana
   useradd -m -g editorial bruno
   id ana
   ```

   Saída medida de `id ana`: `uid=1001(ana) gid=1001(editorial) groups=1001(editorial)`.

   O ponto: para o kernel, `ana` é o número `1001`. O nome é uma conveniência que vive em
   `/etc/passwd`, e é por isso que um arquivo copiado para outra máquina pode aparecer
   como sendo de outra pessoa.

4. `## O diretório compartilhado` — o miolo do Cenário, em três movimentos:

   ```bash
   mkdir /srv/editorial
   chgrp editorial /srv/editorial
   chmod 770 /srv/editorial
   ```

   Agora mostre o problema que o `770` **não** resolve, rodando como a Ana:

   ```bash
   su - ana -c 'touch /srv/editorial/rascunho.md'
   ls -l /srv/editorial
   ```

   O arquivo nasce com grupo `editorial` aqui porque o grupo primário da Ana é
   `editorial` — mas isso é sorte, não garantia: qualquer pessoa cujo grupo primário seja
   outro criaria um arquivo que o Bruno não consegue editar. O setgid tira a sorte da
   equação:

   ```bash
   chmod 2770 /srv/editorial
   stat -c "%a %U %G" /srv/editorial
   ```

   Saída medida: `2770 root editorial`. Explique o quarto dígito: `2` é setgid, e num
   **diretório** ele significa "todo arquivo criado aqui dentro nasce com o grupo do
   diretório, não com o de quem criou".

   Agora o caso que o `chgrp` sozinho não cobre. O rascunho que a Ana criou antes do
   setgid está com o dono certo por acaso; um arquivo que chegasse de fora — copiado de
   outro lugar, restaurado de um backup — chegaria como do `root`. Simule e corrija:

   ```bash
   cp /etc/aurora/catalogo.conf /srv/editorial/referencia.conf
   stat -c "%a %U %G" /srv/editorial/referencia.conf
   chown ana:editorial /srv/editorial/referencia.conf
   stat -c "%a %U %G" /srv/editorial/referencia.conf
   ```

   Saída medida: `644 root editorial` antes, `644 ana editorial` depois. Repare que o
   grupo já veio certo — foi o setgid do diretório agindo sobre um arquivo que o `root`
   criou — mas o **dono** não, e é isso que o `chown` resolve. A forma `chown dono:grupo`
   faz num comando o que `chown` e `chgrp` fariam em dois.

   Feche criando a pauta que a Verificação cobra:

   ```bash
   su - ana -c 'echo "# Pauta de setembro" > /srv/editorial/pauta.md'
   ls -l /srv/editorial/pauta.md
   ```

5. `## Por que não 777` — a seção que dá nome ao Cenário. `777` resolveria o sintoma e
   abriria o diretório para **todo processo da máquina**, incluindo os que servem a
   internet. Nomeie a diferença: `770` responde "quem pode", `2770` responde "quem pode e
   continua podendo amanhã". Mencione `umask` — medido, o padrão aqui é `0022`, e é ele
   que explica por que um arquivo novo nasce `644` e não `666`:

   ```bash
   umask
   touch /tmp/teste-umask && stat -c "%a" /tmp/teste-umask
   ```

   Saída medida: `0022` e `644`.

   Termine dizendo o que a Verificação cobra e por quê: o modo do diretório, o grupo do
   arquivo que nasceu dentro dele — que é a prova de que o setgid está fazendo efeito, e
   não só de que alguém digitou `2770` — e a filiação das duas pessoas ao grupo.

- [ ] **Step 5: Rodar o Cenário de verdade**

```powershell
cd content\linux\02-todo-arquivo-tem-dono\workspace
docker compose -p linux-02 up -d --build
docker exec -it learning-infra-linux bash
```

Execute todas as seções na ordem. Depois valide as três Asserções como o motor as roda:

```powershell
docker exec learning-infra-linux stat -c "%a %U %G" /srv/editorial
docker exec learning-infra-linux stat -c "%a %U %G" /srv/editorial/pauta.md
docker exec learning-infra-linux sh -c "id -nG ana; id -nG bruno"
```

Expected: `2770 root editorial`; o terceiro campo do segundo comando é `editorial`; e o
terceiro comando imprime `editorial` nas duas linhas.

Se `id ana` divergir do que o `cenario.md` promete — uid ou gid diferentes de `1000` —
corrija o **texto** para o valor medido, nunca o contrário.

- [ ] **Step 6: Rodar a suíte e derrubar o ambiente**

```powershell
cd backend; ./mvnw -q test
cd ..\content\linux\02-todo-arquivo-tem-dono\workspace; docker compose -p linux-02 down -v
```

- [ ] **Step 7: Commit**

```bash
git add content/linux/02-todo-arquivo-tem-dono/ \
        backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: Cenário 02 da Trilha Linux ensina dono, grupo e setgid"
```

---

### Task 4: Cenário 03 — O texto é a interface

O Cenário que fecha o Ato I e que dá ao leitor a ferramenta que ele mais vai usar no resto
da Trilha: transformar um arquivo de texto numa resposta. O log semeado tem um sinal real
plantado dentro — a `livraria-do-porto` concentra 37 dos 87 erros, e todos entre 2h e 4h.
O leitor descobre isso com `grep`, `cut`, `sort` e `uniq`, e a descoberta é o gancho
narrativo do Cenário 10, o trabalho das três da manhã.

**Files:**
- Create: `content/linux/03-o-texto-e-a-interface/cenario.md`
- Create: `content/linux/03-o-texto-e-a-interface/verificacao.yaml`
- Create: `content/linux/03-o-texto-e-a-interface/workspace/Dockerfile`
- Create: `content/linux/03-o-texto-e-a-interface/workspace/compose.yaml`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Interfaces:**
- Consumes: `/var/log/aurora/pedidos.log` semeado na Task 1, com estes números medidos —
  2160 linhas, 100 respostas `500`, e a distribuição por cliente da tabela abaixo.
- Produces: `/srv/relatorio-500.txt` e `/srv/erros.txt`.

**Os números que o Cenário promete**, medidos contra a imagem da Task 1. O texto do
Cenário só pode citar valores desta tabela:

| Comando | Saída |
|---|---|
| `wc -l < /var/log/aurora/pedidos.log` | `2160` |
| `grep -c "status=500" /var/log/aurora/pedidos.log` | `87` |
| `head -1 /var/log/aurora/pedidos.log` | `2026-08-14T00:00:07 pedido=4001 cliente=livraria-do-porto status=200 ms=280` |
| o relatório por cliente | `37 livraria-do-porto`, `15 banca-central`, `14 papelaria-sul`, `13 leitura-norte`, `8 sebo-da-praca` |

- [ ] **Step 1: Atualizar as contagens e ver o teste falhar**

Modify `CatalogoRealTest.java`:

```java
        assertThat(cenarios).hasSize(65);
```

```java
        assertThat(linux).hasSize(4);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(11);
```

Run: `cd backend; ./mvnw -q test -Dtest=CatalogoRealTest`
Expected: FAIL — `expected size: 65 but was: 64`.

- [ ] **Step 2: Criar o workspace**

```powershell
mkdir content\linux\03-o-texto-e-a-interface\workspace
Copy-Item content\linux\08-um-programa-vira-servico\workspace\Dockerfile `
          content\linux\03-o-texto-e-a-interface\workspace\Dockerfile
```

Create `content/linux/03-o-texto-e-a-interface/workspace/compose.yaml`:

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

- [ ] **Step 3: Escrever a Verificação**

Create `content/linux/03-o-texto-e-a-interface/verificacao.yaml`:

```yaml
asercoes:
  - tipo: arquivo_linux
    caminho: /srv/relatorio-500.txt
    dono: root
    descricao: o relatório de erros existe e pertence ao root

  - tipo: comando_produz
    descricao: o relatório aponta a livraria-do-porto como origem da maior fatia dos erros
    comando: ["docker", "exec", "learning-infra-linux",
              "head", "-1", "/srv/relatorio-500.txt"]
    contem: "37 livraria-do-porto"

  - tipo: comando_produz
    descricao: o erro do comando foi para o arquivo de erros, e não para o relatório
    comando: ["docker", "exec", "learning-infra-linux",
              "cat", "/srv/erros.txt"]
    contem: "No such file or directory"
```

A terceira Asserção é a única maneira honesta de cobrar a lição de stdout contra stderr: o
leitor só a satisfaz se tiver redirecionado os dois fluxos para lugares diferentes.

- [ ] **Step 4: Escrever o Cenário**

Create `content/linux/03-o-texto-e-a-interface/cenario.md` com este frontmatter:

```yaml
---
id: linux/03-o-texto-e-a-interface
titulo: O texto é a interface
dificuldade: guiado
projetoCompose: linux-03
containerLinux: learning-infra-linux
---
```

Seis seções `##`:

1. `## A pergunta` — chegou uma reclamação: pedidos falhando. Não há painel, não há
   alerta, não há dashboard. Há um arquivo de texto com 2160 linhas e um dia inteiro
   dentro. A promessa da seção: em cinco comandos encadeados o leitor sai da reclamação
   para o nome do cliente afetado.

2. `## Uma linha de cada vez` —

   ```bash
   head -1 /var/log/aurora/pedidos.log
   wc -l /var/log/aurora/pedidos.log
   ```

   Saída medida da primeira:

   ```
   2026-08-14T00:00:07 pedido=4001 cliente=livraria-do-porto status=200 ms=280
   ```

   Disseque os cinco campos separados por espaço. O formato é o contrato: é ele que
   permite recortar por posição na seção seguinte.

3. `## Filtrar` — `grep` como o primeiro corte:

   ```bash
   grep "status=500" /var/log/aurora/pedidos.log | head -3
   grep -c "status=500" /var/log/aurora/pedidos.log
   ```

   Saída medida do segundo: `87`. Oitenta e sete falhas em 2160 pedidos. Introduza o `|` aqui, com
   a definição que vai valer para o resto da Trilha: o pipe liga a saída de um programa à
   entrada do próximo, e é por isso que ferramentas pequenas bastam.

4. `## Recortar, ordenar, contar` — o encadeamento completo, construído um estágio por
   vez para que o leitor veja a saída mudar de forma:

   ```bash
   grep "status=500" /var/log/aurora/pedidos.log | cut -d" " -f3 | head -3
   grep "status=500" /var/log/aurora/pedidos.log | cut -d" " -f3 | cut -d= -f2 | sort | uniq -c | sort -rn
   ```

   Saída medida do segundo comando, exatamente:

   ```
        37 livraria-do-porto
        15 banca-central
        14 papelaria-sul
        13 leitura-norte
         8 sebo-da-praca
   ```

   Explique por que `sort` vem antes de `uniq`: o `uniq` só enxerga linhas repetidas que
   estejam **adjacentes**, e essa é a pegadinha que faz o encadeamento errado devolver
   contagens infladas. Peça ao leitor que rode sem o `sort` para ver acontecer.

   O sinal está na primeira linha: 37 das 87 falhas vêm de um cliente só. Fecha com a
   pergunta que o Cenário 10 vai responder — a que horas isso acontece:

   ```bash
   grep "status=500" /var/log/aurora/pedidos.log | grep livraria-do-porto | cut -d"T" -f2 | cut -d: -f1 | sort -u
   ```

   Saída medida:

   ```
   02
   03
   04
   ```

5. `## Dois fluxos, não um` — a seção que a terceira Asserção cobra. Rode um comando que
   falha e mostre que a mensagem **não** obedece ao `>`:

   ```bash
   cat /var/log/aurora/pedidos.log /var/log/aurora/vendas.log > /srv/tentativa.txt
   ```

   Saída medida no terminal, apesar do redirecionamento:

   ```
   cat: /var/log/aurora/vendas.log: No such file or directory
   ```

   Nomeie os dois descritores: `1` é stdout, `2` é stderr, e `>` sozinho só desvia o `1`.
   Mostre o código de saída como o terceiro canal, o que programas leem:

   ```bash
   echo $?
   ```

   Saída medida: `1`. Depois separe de verdade:

   ```bash
   cat /var/log/aurora/pedidos.log /var/log/aurora/vendas.log > /srv/tentativa.txt 2> /srv/erros.txt
   cat /srv/erros.txt
   ```

   Diga por que isso importa fora do exercício: um script que joga tudo no mesmo arquivo
   entrega um relatório com mensagem de erro no meio, e o Cenário 09 vai depender de o
   leitor saber ler os dois separados.

6. `## Escreva o relatório` — o entregável:

   ```bash
   grep "status=500" /var/log/aurora/pedidos.log \
     | cut -d" " -f3 | cut -d= -f2 | sort | uniq -c | sort -rn \
     > /srv/relatorio-500.txt
   cat /srv/relatorio-500.txt
   ```

   E a linha que costura com o Cenário 01:

   ```bash
   echo "- Erros de pedido concentrados na livraria-do-porto, entre 2h e 4h" >> /srv/inventario.md
   ```

   Repare no `>>`: acrescenta, enquanto `>` teria apagado o inventário inteiro. Vale um
   parágrafo — é o erro mais caro desta seção e o leitor vai cometê-lo em outro lugar se
   não o cometer aqui.

   Feche o Ato: em três Cenários o leitor entrou numa máquina que não conhecia, descobriu
   onde as coisas moram, de quem elas são, e derivou uma resposta de um arquivo de texto.
   O que ainda não se sabe é **quem está rodando** — que é o Ato II.

- [ ] **Step 5: Rodar o Cenário de verdade**

```powershell
cd content\linux\03-o-texto-e-a-interface\workspace
docker compose -p linux-03 up -d --build
docker exec -it learning-infra-linux bash
```

Execute todas as seções na ordem e confira cada saída contra a tabela de números acima.
Qualquer divergência é bloqueante: significa que a semente não é determinística como
medido, e o Cenário inteiro depende disso.

Depois valide as três Asserções:

```powershell
docker exec learning-infra-linux stat -c "%a %U %G" /srv/relatorio-500.txt
docker exec learning-infra-linux head -1 /srv/relatorio-500.txt
docker exec learning-infra-linux cat /srv/erros.txt
```

Expected: dono `root`; `     37 livraria-do-porto`; e
`cat: /var/log/aurora/vendas.log: No such file or directory`.

- [ ] **Step 6: Rodar a suíte e derrubar o ambiente**

```powershell
cd backend; ./mvnw -q test
cd ..\content\linux\03-o-texto-e-a-interface\workspace; docker compose -p linux-03 down -v
```

- [ ] **Step 7: Commit**

```bash
git add content/linux/03-o-texto-e-a-interface/ \
        backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java
git commit -m "feat: Cenário 03 da Trilha Linux deriva resposta de um log"
```

---

### Task 5: Costurar o Ato I ao Cenário 08 e à documentação

O Cenário 08 foi escrito na Etapa 1 como prova de que o caminho de verificação funcionava,
e por isso abre como se fosse o primeiro Cenário da Trilha: apresenta o `docker exec`, o
`systemctl is-system-running` e a máquina inteira do zero. Com o Ato I no ar isso virou
repetição — e pior, uma repetição que contradiz a ordem, porque o leitor chega ao 08
tendo entrado na máquina três vezes.

**Files:**
- Modify: `content/linux/08-um-programa-vira-servico/cenario.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: os Cenários 01 a 03 das Tasks 2, 3 e 4.
- Produces: nada que outra Task consuma. É a última desta Etapa.

- [ ] **Step 1: Reescrever a abertura do Cenário 08**

Duas substituições literais.

**Primeira.** O parágrafo de abertura, hoje:

> Até aqui a Aurora tocava seus programas na mão: para servir o catálogo, alguém abria
> uma sessão e deixava um processo rodando. Na primeira reinicialização, o catálogo sumia
> e ninguém lembrava como ele subia de novo.

passa a ser:

> Você já inventariou a máquina, organizou o acesso do editorial e descobriu no log de
> onde vinham os erros de pedido. Tudo isso foi olhar. Agora você vai mudar como a
> máquina se comporta.
>
> Até aqui a Aurora tocava seus programas na mão: para servir o catálogo, alguém abria
> uma sessão e deixava um processo rodando. Na primeira reinicialização, o catálogo sumia
> e ninguém lembrava como ele subia de novo.

**Segunda.** Na seção `## Entre na máquina`, o parágrafo:

> Essa linha é a porta de entrada da Trilha inteira — ela te coloca dentro da máquina da
> Aurora. A Trilha Docker, logo em seguida, explica cada pedaço dela; por ora, use-a como
> uma incantação. Confirme que a máquina está saudável:

passa a ser:

> A mesma linha do Cenário 01. Antes de mexer em qualquer coisa, confirme que a máquina
> está saudável — esse é o primeiro reflexo de quem opera um servidor, e é a linha de base
> contra a qual você vai comparar tudo que quebrar depois:

- [ ] **Step 2: Verificar que o 08 continua íntegro**

```powershell
cd content\linux\08-um-programa-vira-servico\workspace
docker compose -p linux-08 up -d --build
docker exec -it learning-infra-linux bash
```

Execute o Cenário 08 inteiro do jeito que ficou e valide as três Asserções:

```powershell
docker exec learning-infra-linux systemctl is-active catalogo.service
docker exec learning-infra-linux systemctl is-enabled catalogo.service
docker exec learning-infra-linux stat -c "%a %U %G" /srv/catalogo/index.html
curl.exe -s -o NUL -w "%{http_code}" http://localhost:8040
```

Expected: `active`, `enabled`, `644 root root` e `200`.

- [ ] **Step 3: Atualizar o README**

Três ajustes, todos com valores medidos nesta Etapa:

- a linha de portas passa a nomear os Cenários que usam o bloco: hoje ela diz
  "começando pela **8040** no Cenário 08"; os Cenários 01 a 03 não publicam porta, e isso
  merece ser dito para quem for escrever o Ato II;
- o aviso de custo do primeiro Iniciar: a medição de **68 s** da Etapa 1 saiu de uma
  imagem que não existe mais. O valor medido para a imagem desta Etapa é **48 s** a frio
  e alguns segundos com o cache quente. Corrija o número.
- registre o que a imagem passou a trazer — `less`, `nano`, `file`, `lsof` — e que ela
  semeia `/srv/catalogo`, `/etc/aurora/catalogo.conf` e `/var/log/aurora/pedidos.log`, com
  a razão: os Cenários partem de uma máquina que tem passado.

- [ ] **Step 4: Rodar tudo**

```powershell
cd backend; ./mvnw -q test
cd ..\frontend; node scripts-checar-conteudo.mjs linux
```

Expected: PASS nos dois. O checador de conteúdo valida `fundamentos.md` e
`questionario.yaml` e **não** olha para Cenários — nada nesta Etapa deveria afetá-lo, e se
afetar, algo saiu do lugar.

- [ ] **Step 5: Derrubar o ambiente e commitar**

```powershell
cd ..\content\linux\08-um-programa-vira-servico\workspace; docker compose -p linux-08 down -v
```

```bash
git add content/linux/08-um-programa-vira-servico/cenario.md README.md
git commit -m "docs: Cenário 08 parte do Ato I e o README registra o substrato novo"
```

---

## O que esta Etapa deixa aberto

Registrado aqui para a Etapa 4 não redescobrir:

- **`man` não funciona.** A imagem Ubuntu vem minimizada, sem manpages, e `man ls`
  responde com o aviso de minimização. Nenhum Cenário do Ato I usa `man`, e os Fundamentos
  também não o prometem — mas o Ato II ensina `apt`, e é lá que a pergunta "como leio a
  documentação de um comando" aparece naturalmente. As opções, para decidir na Etapa 4:
  ensinar `--help`, ou instalar `man-db` e rodar `unminimize` — que é caro e não foi
  medido.
- **A distribuição de Dificuldade da Trilha** só fecha no fim do Ato IV. O
  `CatalogoRealTest` cobra as contagens por Dificuldade da Trilha IaC mas ainda não as da
  Linux; vale acrescentá-las quando os catorze estiverem no disco, não antes.
- **Os riscos do design ainda sem teste.** A Task 1 fecha o risco do `Dockerfile`
  divergente. O outro — `cgroup: host` sumir numa atualização do Docker Desktop — pede um
  teste que suba a imagem e exija `is-system-running` igual a `running`. Ele não entra
  aqui porque seria o primeiro teste da suíte a depender de Docker no CI, e essa é uma
  decisão de infraestrutura de teste, não de conteúdo.
