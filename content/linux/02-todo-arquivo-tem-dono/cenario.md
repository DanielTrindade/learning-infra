---
id: linux/02-todo-arquivo-tem-dono
titulo: Todo arquivo tem dono
dificuldade: guiado
projetoCompose: linux-02
containerLinux: learning-infra-linux
---
# Todo arquivo tem dono

A Aurora contratou duas pessoas para o editorial. As duas precisam escrever nos mesmos
arquivos de pauta, todo dia, sem pedir licença uma para a outra.

Hoje tudo em `/srv` pertence ao `root`, e existe uma saída rápida circulando por aí: dar
`chmod 777` no diretório e seguir a vida. Ela funciona. Este Cenário existe para você
entender por que ela é a resposta errada para a pergunta certa — e qual é a certa.

Entre na máquina, como no Cenário 01:

```powershell
docker exec -it learning-infra-linux bash
```

## Os três campos

Releia um arquivo que você já conhece, agora com o vocabulário novo:

```bash
ls -l /etc/aurora/catalogo.conf
stat -c "%a %U %G" /etc/aurora/catalogo.conf
```

O `stat` responde `644 root root`. Esses três campos são a resposta inteira do Linux para
a pergunta "quem pode mexer nisso":

- **o modo** — `644`;
- **o dono** — `root`;
- **o grupo** — `root`.

O modo são três dígitos para três públicos, nesta ordem: o **dono**, o **grupo**, e
**todos os outros**. Cada dígito é a soma de três permissões: ler vale 4, escrever vale 2,
executar vale 1. Então `644` se lê assim: o dono lê e escreve (4+2), o grupo só lê (4), o
resto só lê (4).

O `ls -l` mostra a mesma coisa em letras — `-rw-r--r--` — e é a forma que você vai
encontrar com mais frequência. São notações do mesmo número.

Uma palavra sobre o `root`, porque ela explica muita coisa adiante: o `root` não é um
usuário com permissões generosas. É o usuário para quem a **checagem não acontece**. Não
existe modo que barre o `root`, e é por isso que operar como ele o tempo todo é perigoso.

## Identidade

Crie o grupo e as duas pessoas:

```bash
groupadd editorial
useradd -m -g editorial ana
useradd -m -g editorial bruno
id ana
```

O `id` responde:

```
uid=1001(ana) gid=1001(editorial) groups=1001(editorial)
```

Repare no que o sistema realmente guarda: **números**. Para o kernel, a Ana é o `1001`. O
nome "ana" é uma conveniência que mora num arquivo de texto, `/etc/passwd`, e o mesmo vale
para o grupo em `/etc/group`:

```bash
grep '^ana:' /etc/passwd
grep '^editorial:' /etc/group
```

```
ana:x:1001:1001::/home/ana:/bin/sh
editorial:x:1001:
```

O `^` prende o padrão ao começo da linha. Sem ele, `grep ana` traria também o
`Mailing List Manager`, que tem "ana" no meio — a primeira lição do Cenário 03, chegando
adiantada.

Isso tem uma consequência prática que morde muita gente: um arquivo copiado para outra
máquina leva o **número**, não o nome. Se lá o `1001` for outra pessoa, o arquivo passa a
ser dela.

O `-g editorial` definiu o **grupo primário** de cada uma — o grupo que elas usam por
padrão ao criar arquivos. Uma pessoa pode pertencer a vários grupos, mas só um é o
primário.

## O diretório compartilhado

Crie o espaço do editorial e entregue-o ao grupo:

```bash
mkdir /srv/editorial
chgrp editorial /srv/editorial
chmod 770 /srv/editorial
stat -c "%a %U %G" /srv/editorial
```

O `770` diz: dono lê, escreve e entra; grupo lê, escreve e entra; **todos os outros, nada**.
Em diretório, o `1` que vale "executar" significa "pode atravessar", e sem ele nem `cd`
funciona.

Veja funcionando. Rode um comando como se fosse a Ana:

```bash
su - ana -c 'touch /srv/editorial/rascunho.md'
ls -l /srv/editorial
```

O arquivo nasceu com grupo `editorial`. Mas isso foi **sorte**, não garantia: aconteceu
porque o grupo primário da Ana já é `editorial`. Uma terceira pessoa, cujo grupo primário
fosse outro, criaria aqui um arquivo que o Bruno não conseguiria editar — e o problema só
apareceria semanas depois, no pior momento.

### O quarto dígito

O `setgid` tira a sorte da equação:

```bash
chmod 2770 /srv/editorial
stat -c "%a %U %G" /srv/editorial
```

Agora são quatro dígitos: `2770`. O `2` na frente é o **setgid**, e num **diretório** ele
significa uma coisa só, mas decisiva: *todo arquivo criado aqui dentro nasce com o grupo do
diretório, não com o grupo de quem criou*.

Deixou de depender de como cada conta foi configurada. Passou a depender do diretório — que
é onde a regra do time deve morar.

### chgrp resolve o grupo, chown resolve o dono

Falta o caso que o `chgrp` não cobre. O rascunho da Ana está com o dono certo por acaso;
um arquivo que chegasse de fora — copiado de outro lugar, restaurado de um backup —
chegaria como sendo do `root`. Simule e corrija:

```bash
cp /etc/aurora/catalogo.conf /srv/editorial/referencia.conf
stat -c "%a %U %G" /srv/editorial/referencia.conf
chown ana:editorial /srv/editorial/referencia.conf
stat -c "%a %U %G" /srv/editorial/referencia.conf
```

Antes: `644 root editorial`. Depois: `644 ana editorial`.

Repare no que já veio certo e no que não veio. O **grupo** era `editorial` mesmo o arquivo
tendo sido criado pelo `root` — isso é o setgid agindo, exatamente como prometido. O
**dono** não, e é isso que o `chown` resolve. A forma `chown dono:grupo` faz num comando o
que `chown` e `chgrp` fariam em dois.

Agora crie a pauta que a Verificação vai cobrar:

```bash
su - ana -c 'echo "# Pauta de setembro" > /srv/editorial/pauta.md'
ls -l /srv/editorial/pauta.md
```

## Por que não 777

`chmod 777 /srv/editorial` teria resolvido o sintoma — as duas pessoas escrevem — e aberto
o diretório para **todo processo da máquina**, incluindo os que atendem a internet. O
catálogo da Aurora responde na porta 8040 para quem quiser; um processo comprometido ali
passaria a poder reescrever as pautas.

A diferença em uma frase: **`770` responde "quem pode agora"; `2770` responde "quem pode e
continua podendo amanhã"**. O `777` responde "qualquer um", que não era a pergunta.

Falta uma peça para fechar o assunto: por que um arquivo novo nasce `644` e não `666`?

```bash
umask
touch /tmp/teste-umask && stat -c "%a" /tmp/teste-umask
```

O `umask` responde `0022` e o arquivo nasce `644`. A `umask` é uma máscara de **subtração**:
ela lista as permissões que o sistema tira de todo arquivo criado. Com `022`, some a
escrita do grupo e a dos outros. É por isso que você quase nunca precisa dar `chmod` num
arquivo recém-criado — e é onde procurar quando um arquivo nasce com permissão estranha.

## Verificação

A Verificação cobra três coisas, e a segunda é a que separa entender de copiar.

O **diretório** precisa estar `2770`, do grupo `editorial`. O **arquivo criado dentro dele**
precisa ter grupo `editorial` — essa é a prova de que o setgid está fazendo efeito, e não
apenas de que alguém digitou `2770`. E as **duas pessoas** precisam estar no grupo, porque
um espaço compartilhado com uma pessoa só não é compartilhado.

Você já sabe onde as coisas moram e de quem elas são. O Cenário 03 usa as duas coisas para
extrair uma resposta de um arquivo de texto.
