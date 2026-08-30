---
id: linux/11-log-nao-e-arquivo-de-texto
titulo: Log não é arquivo de texto
dificuldade: assistido
projetoCompose: linux-11
containerLinux: learning-infra-linux
---
# Log não é arquivo de texto

No Cenário 03 o log era um arquivo, e `grep` resolveu. Você construiu um relatório inteiro
em cima disso.

O log **do sistema** não é assim, e este Cenário existe para você não descobrir isso às
três da manhã, tentando `grep` num arquivo binário e concluindo que a máquina está
quebrada.

Entre na máquina:

```powershell
docker exec -it learning-infra-linux bash
```

## O hábito que não serve aqui

Onde o systemd guarda o que a máquina registrou:

```bash
ls /var/log/journal/
ls /var/log/journal/*/
file /var/log/journal/*/system.journal
```

O `file` não diz "texto". Ele responde algo assim:

```
Journal file, online, keyed hash siphash24, compressed zstd, compact, header size 0x110
```

Um formato próprio, com hash e compressão. `grep` ali não devolve nada útil, e é por isso
que existe uma ferramenta dedicada para ler: o `journalctl`.

## Por que binário

Uma linha de log de texto é uma **frase**. Uma entrada do journal é um **registro com
campos**. Veja:

```bash
journalctl -n 1 -o json | python3 -m json.tool | head -20
```

Repare no que aparece sem ninguém ter escrito: `PRIORITY`, `_PID`, `_UID`,
`_SYSTEMD_UNIT`, `SYSLOG_IDENTIFIER`, `__REALTIME_TIMESTAMP`, `_BOOT_ID`. O programa
escreveu só o `MESSAGE`; o journald carimbou todo o resto. A ordem dos campos varia, então
não se assuste se a sua saída não vier igual — o que importa é a quantidade de coisa que
veio de graça.

É isso que permite perguntar **"erros da unit X na última hora"** sem depender de o
programa ter escrito a hora e o nome de um jeito que o seu `grep` entenda. No Cenário 03
aquilo funcionou porque o formato do `pedidos.log` era fixo e você o conhecia — um acordo
frágil, que quebra no dia em que alguém muda o formato ou mistura dois programas no mesmo
arquivo.

## Gere o que a madrugada gerou

```bash
echo "catalogo respondeu devagar" | systemd-cat -t aurora -p warning
echo "falha ao gravar pedido 9001" | systemd-cat -t aurora -p err
echo "rotina noturna iniciada" | systemd-cat -t aurora -p info
```

O `systemd-cat` manda a entrada padrão para o journal: `-t` define a etiqueta e `-p` a
prioridade. É assim que um script simples registra no lugar certo em vez de escrever num
arquivo que ninguém vai achar.

## Os três filtros que resolvem um plantão

**Por quem escreveu:**

```bash
journalctl -t aurora
journalctl -u systemd-journald
```

O `-t` filtra pela etiqueta; o `-u` filtra pela unit, e foi ele que resolveu o Cenário 09.

**Por gravidade:**

```bash
journalctl -t aurora -p err
journalctl -t aurora -p warning
```

Compare as duas saídas com atenção, porque o resultado é contraintuitivo. O `-p err` traz
**uma** linha. O `-p warning` traz **duas** — a de aviso *e* a de erro.

O `-p` não filtra *naquela* prioridade: filtra **daquela para cima**. As prioridades são
uma escala de gravidade, e você pede "isto ou pior":

| Número | Nome | Quando |
|---:|---|---|
| 0–2 | `emerg`, `alert`, `crit` | a máquina está em risco |
| 3 | `err` | alguma coisa falhou |
| 4 | `warning` | alguma coisa vai falhar |
| 6 | `info` | operação normal |
| 7 | `debug` | só enquanto você investiga |

A consequência prática vale a página inteira: num plantão, `-p err` mostra **o que
quebrou**; `-p warning` mostra o que quebrou **e o que avisou antes**. A causa quase sempre
está no segundo.

**Por tempo:**

```bash
journalctl -t aurora --since "10 min ago"
journalctl --since "2026-08-30 03:00" --until "2026-08-30 05:00"
```

O `--since` e o `--until` aceitam tanto linguagem natural quanto data e hora. É o filtro
que responde "o que aconteceu na madrugada" sem você recortar timestamp na mão.

E os três se combinam, que é onde a ferramenta ganha do `grep`:

```bash
journalctl -t aurora -p warning --since "10 min ago"
```

## O que sobrevive ao boot

```bash
ls -d /var/log/journal/*
journalctl --disk-usage
```

O diretório existe e o journal ocupa alguns megabytes.

Isso não é automático, e é uma das primeiras coisas a conferir numa máquina que você vai
operar: o journal é **volátil** por padrão em várias distribuições. Nesse modo ele vive em
`/run/log/journal`, que é memória — e some no boot, exatamente quando você mais precisa
dele para entender por que a máquina reiniciou. Ele só persiste quando `/var/log/journal`
existe.

E o tamanho não é infinito: o journald descarta o mais antigo conforme o limite
configurado. Ligue com o Cenário 07 — **log ocupa disco, e o journal também**. A diferença
é que ele se limita sozinho, enquanto o `pedidos.log` da Aurora cresceu até encher a
partição porque ninguém tinha combinado um limite com ele.

## Deixe registrado

O entregável fecha o Ato: transforme a consulta numa página que a próxima pessoa lê.

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

O `{ ... }` agrupa vários comandos para que **um único** redirecionamento pegue a saída de
todos. Sem ele, o `>` só valeria para o último — e é a peça que faltava no seu repertório
de shell desde o Cenário 03.

O `--no-pager` é necessário aqui: quando a saída não vai para um terminal, o `journalctl`
já se comporta bem, mas escrever isso explicitamente é o que torna o comando seguro dentro
de um script.

## Verificação

A Verificação cobra o erro aparecendo numa consulta **com `-p err`**, e o registro do
plantão existindo.

A primeira tem uma sutileza: a mesma mensagem apareceria numa consulta sem filtro nenhum.
Cobrar o `-p err` é cobrar que você saiba **filtrar por gravidade** — que é a única
habilidade desta página que muda um plantão.

## Fim do Ato III

A Aurora saiu de "alguém digitou um comando e foi embora" para serviços que sobem sozinhos,
um trabalho que a máquina executa de madrugada, e um registro que dá para consultar depois.

Falta a última pergunta: **por onde tudo isso é alcançado**, e quem tem permissão para quê.
É o Ato IV.
