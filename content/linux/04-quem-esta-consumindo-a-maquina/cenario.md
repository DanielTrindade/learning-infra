---
id: linux/04-quem-esta-consumindo-a-maquina
titulo: Quem está consumindo a máquina
dificuldade: assistido
projetoCompose: linux-04
containerLinux: learning-infra-linux
---
# Quem está consumindo a máquina

O relatório noturno da Aurora não saiu esta madrugada. Quem tentou rodá-lo à mão de manhã
desistiu no meio: a máquina "está impossível", cada comando demora, o terminal engasga.

Nenhuma mensagem de erro. Nenhum log novo. Só lentidão — que é o sintoma mais vago que
existe e, ainda assim, tem uma causa concreta rodando neste exato momento.

Até agora você olhou coisas **paradas** no disco. Este Cenário é sobre o que está **em
movimento**.

Entre na máquina:

```powershell
docker exec -it learning-infra-linux bash
```

## Quem está rodando

O `ps` lista processos. Sozinho ele mostra quase nada; a combinação que se usa no dia a
dia é esta:

```bash
ps aux | head -5
```

Cinco colunas importam agora:

| Coluna | O que é |
|---|---|
| `USER` | com que identidade o processo roda — o assunto do Cenário 02, agora vivo |
| `PID` | o número do processo |
| `%CPU` | quanto da CPU ele está consumindo |
| `STAT` | o estado: `S` dormindo, `R` rodando, `Z` zumbi |
| `COMMAND` | o que ele é |

Para achar um consumidor, ordene:

```bash
ps aux --sort=-%cpu | head -5
```

Guarde uma limitação antes de confiar demais: **`ps` é uma fotografia, não um filme**. Ele
mostra o instante em que você perguntou. Um processo que consome em rajadas pode não
aparecer na hora errada — e é por isso que se roda o comando mais de uma vez antes de
concluir.

## Reproduza o problema

Diagnosticar algo que não está acontecendo não ensina nada. O programa que a Aurora deixou
rodando desde ontem é um importador de pedidos antigo, que ninguém mantém. Recrie-o:

```bash
tee /usr/local/bin/importador > /dev/null <<'EOF'
#!/bin/bash
trap "" TERM
while true; do :; done
EOF
chmod +x /usr/local/bin/importador
setsid importador </dev/null >/dev/null 2>&1 &
```

Duas linhas merecem atenção antes de você seguir.

O `chmod +x` é o bit de execução que você viu no Cenário 02, agora com consequência: sem
ele o arquivo existe, tem o conteúdo certo e o sistema recusa rodá-lo.

E o `trap "" TERM` manda o programa **ignorar** o pedido de encerramento. É uma linha só, e
é ela que faz o resto deste Cenário existir. Você acabou de ver que ela está lá — daqui a
duas seções isso vai importar.

Agora encontre o culpado como se não soubesse quem é:

```bash
ps aux --sort=-%cpu | head -3
pgrep -x importador
```

O `pgrep` procura processos por nome e devolve o PID. O `-x` exige nome exato — sem ele,
o padrão casaria com qualquer processo que **contenha** o texto, inclusive o seu próprio
comando de busca.

Um detalhe que economiza meia hora de confusão quando acontecer com você: o nome que o
kernel guarda de cada processo tem **no máximo 15 caracteres**. Se este programa se
chamasse `importador-pedidos`, o `ps` mostraria `importador-pedi` e o `pgrep -x` avisaria
que nunca vai casar. A saída, nesse caso, é `pgrep -f`, que procura na linha de comando
inteira em vez de no nome curto.

## A árvore

Nenhum processo aparece do nada: todo processo é criado por outro. Veja a família inteira:

```bash
ps -eo pid,ppid,comm --forest | head -15
```

O `PPID` é o PID do pai. Siga qualquer galho para cima e você chega sempre ao mesmo lugar:

```bash
ps -o pid,ppid,comm -C importador
ps -o pid,comm -p 1
```

O PID 1 é o `systemd`. Ele é o primeiro processo que o kernel cria no boot e o ancestral de
todo o resto — por isso a árvore tem uma raiz só. Essa propriedade parece trivia agora; no
Cenário 05 ela é a resposta inteira.

## Pedir e obrigar

Peça ao importador que encerre:

```bash
pkill -x importador
sleep 2
pgrep -x importador
```

O `pgrep` ainda devolve um PID. **O processo continua vivo.**

Isso não é bug seu, e o nome do comando é que engana: `kill` não mata nada. Ele **envia um
sinal** — uma notificação que o kernel entrega ao processo. O sinal padrão é o `SIGTERM`,
que significa "por favor, encerre", e um programa pode escolher o que fazer com ele.
Aquele `trap "" TERM` escolheu não fazer nada.

E `SIGTERM` é o padrão justamente por isso: ele dá ao programa a chance de terminar a
transação, fechar o arquivo e sair inteiro. Um banco de dados que recebe `SIGTERM` grava o
que estava pendente.

Quando o pedido não basta, obrigue:

```bash
pkill -9 -x importador
sleep 2
pgrep -x importador && echo "ainda vivo" || echo "morreu"
```

O `-9` é o `SIGKILL`, e ele é diferente de todos os outros: **não é entregue ao processo**.
Quem age é o kernel, que remove o processo da máquina. Nenhum `trap` intercepta, nenhuma
limpeza acontece, nenhum arquivo é fechado com cuidado.

A regra de ofício cabe numa frase: **`SIGKILL` é o último recurso, não o primeiro.** Quem
começa por ele resolve a lentidão e ganha um arquivo gravado pela metade.

Existem dezenas de sinais:

```bash
kill -l | head -3
```

Você vai usar três: `TERM` para pedir, `KILL` para obrigar, e `HUP` — que muitos programas
adotaram como "releia sua configuração".

## O relatório que faltava

Com a máquina livre, o trabalho de ontem finalmente roda. Ele é o encadeamento que você
montou no Cenário 03, agora com nome e endereço:

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

Repare que você chamou `relatorio-aurora` sem dizer onde ele está. O shell encontrou
sozinho porque `/usr/local/bin` é um dos lugares onde ele procura — e **como** ele procura
é o Cenário 06.

## Verificação

A Verificação cobra as duas pontas: o importador **ausente** e o relatório **existindo**.

A segunda é a que importa. Matar o processo certo era o meio; o objetivo era o relatório
sair. Um plantão em que você derruba o consumidor e vai dormir sem checar se o trabalho
represado voltou a rodar é um plantão pela metade.

Você já sabe achar quem consome e como pará-lo. O que ainda não sabe é por que alguns
processos parecem não morrer nunca — e é isso que o Cenário 05 responde.
