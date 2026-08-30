---
id: linux/10-o-trabalho-das-tres-da-manha
titulo: O trabalho das três da manhã
dificuldade: assistido
projetoCompose: linux-10
containerLinux: learning-infra-linux
---
# O trabalho das três da manhã

No Cenário 03 você descobriu que os erros da `livraria-do-porto` acontecem entre 2h e 4h.
Desde então, todo dia alguém da Aurora roda o relatório à mão de manhã — quando lembra.

A empresa quer o relatório pronto na mesa quando o dia começar. Alguém sugeriu "põe no
cron". A máquina já tem uma resposta melhor, e ela é feita da mesma peça que você usou nos
dois Cenários anteriores.

Entre na máquina:

```powershell
docker exec -it learning-infra-linux bash
```

## O trabalho

Recrie o programa do Cenário 04 e o lugar onde o resultado vai morar:

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

Ele funciona. O problema nunca foi o programa — foi depender de alguém lembrar.

## Um serviço que não fica no ar

Envolva o trabalho numa unit, como no Cenário 08. Com uma diferença:

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

O relatório saiu. E o `is-active` responde **`inactive`**.

Isso está **certo**. O `Type=oneshot` descreve um serviço que faz uma coisa e termina — ao
contrário do `pedidos.service` e do `catalogo.service`, que existem para ficar no ar. Para
um `oneshot`, terminar é o sucesso, e `inactive` é o estado normal depois de rodar.

Guarde isso, porque é o erro de leitura mais comum com trabalho agendado: alguém confere
`is-active` do serviço, vê `inactive`, e conclui que o agendamento está quebrado. **O que
se cobra é o timer.**

Repare também que a unit **não diz quando rodar**. Ela só diz o que fazer. O quando é
outra unit.

## O timer é uma unit como outra qualquer

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

Três peças para nomear:

- o arquivo se chama `relatorio.timer` e o serviço se chama `relatorio.service`. **O mesmo
  nome antes da extensão** é o que liga os dois — o timer não precisa dizer qual serviço
  dispara;
- `OnCalendar` é **quando**;
- `Persistent=true` manda executar assim que possível se a máquina estava desligada na hora
  marcada. É justamente o que falta no `cron`, que simplesmente perde a execução.

O `OnCalendar=*:*:0/20` é de propósito **a cada 20 segundos** — para você ver acontecer
agora, em vez de esperar a madrugada.

```bash
systemctl list-timers
systemctl is-active relatorio.timer
systemctl is-enabled relatorio.timer
```

O `list-timers` mostra `NEXT`, `LEFT`, `UNIT` e `ACTIVATES`. Guarde esse comando com
carinho: ele responde **"o que esta máquina faz sozinha"**, que é a primeira pergunta de
quem herda um servidor — e que você não tinha como responder no Cenário 01.

O timer responde `active` e `enabled`. Espere e confirme que ele realmente disparou:

```bash
sleep 25
cat /srv/relatorio-noturno/ultimo.txt
journalctl -u relatorio.service -n 5
```

O journal mostra `Starting relatorio.service` e `Finished relatorio.service`. Repare que
quem aparece no journal é o **serviço**, não o timer: o timer só puxa o gatilho.

## A hora de verdade

Antes de trocar para o horário real, aprenda a conferir a sintaxe **sem esperar um dia
inteiro para descobrir que errou**:

```bash
systemd-analyze calendar "*-*-* 03:00:00"
```

A resposta traz a forma normalizada, a próxima ocorrência e quanto falta para ela. Este
comando é o corretor ortográfico do `OnCalendar`, e usá-lo é a diferença entre achar o erro
agora e achar daqui a três dias.

Experimente errar de propósito para ver o que ele diz:

```bash
systemd-analyze calendar "3am"
```

Agora troque o `OnCalendar` da unit para o horário real:

```bash
sed -i 's|OnCalendar=.*|OnCalendar=*-*-* 03:00:00|' /etc/systemd/system/relatorio.timer
systemctl daemon-reload
systemctl restart relatorio.timer
systemctl list-timers relatorio.timer
```

O `NEXT` agora aponta para as três da manhã. O relatório que o timer já gerou continua no
lugar — é ele que a Verificação vai olhar.

## Timer contra cron

A comparação honesta, sem torcida.

O `cron` existe há décadas, está em qualquer máquina Unix, e a linha que agenda é mais
curta de escrever. Se você encontrar um `crontab` numa máquina herdada, ele não está
errado.

O timer do systemd ganha em quatro pontos concretos — e você usou todos eles nesta página:

| | timer | cron |
|---|---|---|
| onde vai o log | journal, com `journalctl -u` | some, ou vira e-mail que ninguém lê |
| o que vai rodar | `systemctl list-timers` | abrir arquivo e interpretar |
| execução perdida | `Persistent=true` recupera | perdida |
| o trabalho é | uma unit, com `Restart=`, dependência, tudo | uma linha de texto |

Há um quinto ponto, mais silencioso: o `cron` roda seu comando num ambiente **mínimo**, com
um `$PATH` curto e sem as variáveis da sua sessão. É a origem do clássico "funciona pra mim
e não funciona no cron" — e a unit, que declara tudo explicitamente, não tem esse problema.

## Verificação

A Verificação cobra o **timer** ativo e habilitado, e o **artefato** existindo.

As duas juntas são o Cenário. Um timer armado que nunca disparou passaria na primeira e
falharia na segunda — e agendamento que nunca rodou não é agendamento, é intenção.

A Aurora agora tem serviços que sobem sozinhos e um trabalho que a máquina executa sem
ninguém. Falta saber ler o que ela registrou enquanto ninguém olhava, e é o Cenário 11.
