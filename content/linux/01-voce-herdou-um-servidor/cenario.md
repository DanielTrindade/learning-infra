---
id: linux/01-voce-herdou-um-servidor
titulo: Você herdou um servidor
dificuldade: guiado
projetoCompose: linux-01
containerLinux: learning-infra-linux
---
# Você herdou um servidor

A **Aurora** é uma editora pequena. O site do catálogo, a entrada de pedidos e o relatório
que roda de madrugada vivem no mesmo servidor Linux, montado anos atrás por alguém que já
não trabalha lá.

Hoje esse servidor é seu. Você recebeu o acesso e mais nada: nenhuma documentação, nenhum
desenho, nenhum segundo servidor para comparar. A primeira tarefa de quem herda uma
máquina não é consertar nada — é descobrir o que existe. Ao fim deste Cenário você terá
escrito o documento que não te deram.

## Entre na máquina

Clique em **Iniciar cenário** e espere o ambiente subir. Depois, no seu terminal
PowerShell:

```powershell
docker exec -it learning-infra-linux bash
```

Trate essa linha como uma **incantação com prazo de validade**: ela abre uma sessão dentro
da máquina da Aurora, e a Trilha Docker vai abrir cada pedaço dela mais adiante. Por ora,
o que importa é o que mudou na tela. O prompt agora é outro:

```
root@aurora:/#
```

Três informações nessa linha. Você é o usuário `root` — o mais poderoso da máquina. A
máquina se chama `aurora`. E você está no diretório `/`. Você não está mais no Windows.

Antes de qualquer coisa, pergunte à máquina se ela está bem:

```bash
systemctl is-system-running
```

A resposta é `running`. Guarde esse comando: ele é o primeiro reflexo de quem chega numa
máquina que não conhece, e a linha de base contra a qual você vai comparar tudo que
quebrar mais tarde.

## Onde eu estou

O shell sempre tem um diretório corrente, e todo caminho que você digita é lido a partir
dele — ou a partir da raiz, se começar com `/`.

```bash
pwd
ls /
```

O `pwd` responde `/`. O `ls /` mostra a raiz do sistema de arquivos: `bin`, `etc`, `home`,
`proc`, `srv`, `usr`, `var` e alguns outros. Não existe `C:` nem `D:` aqui. Existe **uma**
árvore, e tudo pendura nela — discos, dispositivos e até o próprio estado do kernel.

Ande um pouco:

```bash
cd /var/log
pwd
ls
cd /
```

`cd` sem argumento nenhum te leva para o diretório do seu usuário. Como você é o `root`,
esse lugar é `/root`.

## O que esta máquina diz sobre si mesma

```bash
cat /etc/os-release
uname -r
hostname
```

O `cat /etc/os-release` começa assim:

```
PRETTY_NAME="Ubuntu 26.04 LTS"
NAME="Ubuntu"
VERSION_ID="26.04"
VERSION="26.04 LTS (Resolute Raccoon)"
```

Os três comandos respondem a perguntas diferentes, e confundi-las custa caro num plantão:

- `/etc/os-release` descreve a **distribuição** — a coleção de programas, o gerenciador de
  pacotes, a política de atualização. É o que decide se você digita `apt` ou `dnf`.
- `uname -r` descreve o **kernel**, que é outra coisa. Aqui ele responde algo terminado em
  `-microsoft-standard-WSL2`, porque este ambiente compartilha o kernel da VM do Docker
  Desktop. Uma máquina Ubuntu de verdade responderia um número puro.
- `hostname` é só o nome que a máquina usa para se apresentar: `aurora`.

## Onde as coisas moram

Um sistema Linux não espalha arquivos a esmo. Existe uma convenção — o **FHS** — que diz
que tipo de coisa mora em que diretório. Não é uma lei que o kernel imponha: é um acordo
que quase todo mundo cumpre. É por causa dele que você consegue achar as coisas numa
máquina que nunca viu.

Percorra os três lugares que a Aurora usa, um por vez.

O que a máquina **serve**:

```bash
ls /srv/catalogo
```

Como isso está **configurado**:

```bash
cat /etc/aurora/catalogo.conf
```

```
# Catálogo da Aurora — montado em 2019, sem documentação.
porta=8040
raiz=/srv/catalogo
```

Duas descobertas de uma vez: o catálogo escuta na porta 8040 e serve o conteúdo de
`/srv/catalogo`. Você acabou de aprender, lendo um arquivo, algo que ninguém te contou.

E o que a máquina **acumula**:

```bash
ls -lh /var/log/aurora
```

A regra vale para o resto da Trilha, em uma linha: **`/etc` guarda configuração, `/var`
guarda o que cresce, `/srv` guarda o que a máquina serve.** Quem montou a Aurora seguiu a
convenção — e é só por isso que você está achando as coisas sem documentação nenhuma.

## Existir não é a mesma coisa que saber o que é

Você achou um arquivo de log. Sabe o nome dele e nada além disso. Três comandos fazem três
perguntas diferentes sobre o mesmo arquivo.

**Quanto e de quem:**

```bash
stat /var/log/aurora/pedidos.log
```

Repare em três linhas da resposta. `size: 156725` é o tamanho em bytes.
`Access: (0644/-rw-r--r--)  Uid: (    0/    root)   Gid: (    0/    root)` diz quem pode o
quê — assunto inteiro do Cenário 02. E `Inode:` é o número de registro do arquivo no
sistema de arquivos, um detalhe que parece burocrático agora e que vai explicar um disco
cheio no Cenário 07.

**Que tipo de coisa é:**

```bash
file /var/log/aurora/pedidos.log
```

Responde `ASCII text`. A extensão `.log` não significa nada para o Linux — nomes de
arquivo não carregam tipo. O `file` responde olhando o conteúdo.

**Quanto conteúdo tem:**

```bash
wc -l /var/log/aurora/pedidos.log
```

`2160` linhas. Um dia inteiro de pedidos da Aurora.

**E o que tem dentro:**

```bash
less /var/log/aurora/pedidos.log
```

O `less` abre o arquivo para leitura sem carregar tudo na memória. Navegue com as setas ou
com `Espaço`, e **saia com `q`**. Decore essa tecla agora: `q` para sair. Um pager aberto
que você não sabe fechar trava a sessão inteira.

### A armadilha do atalho

Antes de fechar a seção, um detalhe que se paga caro mais adiante:

```bash
ls -l /etc/os-release
```

```
lrwxrwxrwx 1 root root 21 Apr 24 10:24 /etc/os-release -> ../usr/lib/os-release
```

Aquele `l` no começo diz que isso não é um arquivo: é um **atalho** para outro. A seta
mostra o destino. E o `stat`, por padrão, descreve o atalho — não o destino. É por isso que
o modo aqui aparece como `777`, que não é o modo do arquivo de verdade.

## Escreva o inventário

Hora do entregável. Registre o que você descobriu, para que a próxima pessoa que herdar
esta máquina não precise repetir a última meia hora:

```bash
tee /srv/inventario.md > /dev/null <<'EOF'
# Inventário do servidor da Aurora

- Distribuição: Ubuntu 26.04 LTS
- Conteúdo do catálogo: /srv/catalogo
- Configuração do catálogo: /etc/aurora/catalogo.conf
- Log de pedidos: /var/log/aurora/pedidos.log
EOF
```

O `tee` escreve o que recebe num arquivo; o `> /dev/null` descarta a cópia que ele também
mandaria para a tela. O bloco entre `<<'EOF'` e `EOF` é o texto que entra. Confira:

```bash
cat /srv/inventario.md
```

## Verificação

A Verificação cobra duas coisas.

O inventário precisa existir com modo **644** e pertencer ao `root`: documentação que só
quem escreveu consegue ler não é documentação, e o `644` é o que deixa qualquer pessoa da
máquina lê-la. E ele precisa **nomear o log de pedidos** — a descoberta que o Cenário 03
vai transformar numa resposta.

Ao terminar, você sabe onde as coisas moram nesta máquina. O que ainda não sabe é **de
quem elas são**, e é aí que o Cenário 02 começa.
