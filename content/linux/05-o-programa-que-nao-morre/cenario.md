---
id: linux/05-o-programa-que-nao-morre
titulo: O programa que não morre
dificuldade: assistido
projetoCompose: linux-05
containerLinux: learning-infra-linux
---
# O programa que não morre

Depois do incidente de ontem, alguém da Aurora fez a pergunta óbvia: se aquele importador
foi iniciado por uma pessoa, numa sessão de terminal, e essa pessoa fechou o terminal e foi
embora — por que ele continuou rodando?

E veio a segunda pergunta, de quem andou olhando o `ps`: o que são aquelas linhas marcadas
com `Z`, e por que elas não somem?

As duas têm a mesma resposta, e ela passa pelo PID 1.

Entre na máquina:

```powershell
docker exec -it learning-infra-linux bash
```

## O processo que fica

Comece pelo caso comum. O `&` põe um programa em **segundo plano**: ele roda, e o shell
devolve o prompt na hora.

```bash
sleep 300 &
jobs
ps -o pid,ppid,comm -C sleep
```

O `jobs` lista o que **este** shell está tocando. E repare no `PPID` do `sleep`: é o PID do
seu próprio `bash`. O processo em segundo plano continua sendo filho da sua sessão.

Daí a pergunta que interessa: e se o pai morrer?

## Órfão não é solto

Limpe o `sleep` anterior primeiro, para que a próxima lista tenha uma linha só e você não
fique em dúvida sobre qual processo está olhando:

```bash
kill %1
jobs
```

O `%1` é o número do job que o `jobs` mostrou — o shell aceita essa referência no lugar do
PID.

Agora faça o pai morrer de propósito. O `bash -c` abaixo cria um `sleep` e sai
imediatamente, sem esperar por ele:

```bash
setsid bash -c 'sleep 300 & echo "filho=$!"; echo "pai=$$"' > /tmp/orfao.txt 2>&1
cat /tmp/orfao.txt
```

O `$!` é o PID do último processo posto em segundo plano; o `$$` é o PID do próprio shell.
O arquivo guarda os dois números para você comparar.

Agora veja quem assumiu o filho:

```bash
ps -o pid,ppid,comm -C sleep
ps -o pid,comm -p 1
```

O `sleep` aparece com **`PPID 1`**, e o PID 1 é o `systemd`.

Esse é o mecanismo: quando um processo morre, o kernel **reatribui** os filhos dele ao PID
1. Um órfão não fica solto na máquina, nem vira um processo sem pai — ele troca de pai.

E isso não é cortesia do sistema. Existe porque alguém **precisa** estar lá quando o filho
terminar, para recolher o resultado dele. Que é exatamente o assunto da próxima seção.

Isso responde a pergunta da abertura: o importador de ontem sobreviveu ao terminal porque
perder o pai não mata ninguém.

## Zumbi não é o que parece

O outro lado do mesmo mecanismo. Crie um programa que gera um filho e **esquece dele**:

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

O `os.fork()` duplica o processo. O filho sai imediatamente com `os._exit(0)`; o pai
imprime os dois PIDs e vai dormir — **sem nunca chamar `os.wait()`**.

Veja o que sobrou do filho:

```bash
ps -eo pid,ppid,stat,comm | awk '$3 ~ /Z/'
ps aux | grep defunct | grep -v grep
```

Ele aparece com estado `Z`, e no `ps aux` com a marca `<defunct>`.

### Onde o `vigia` se escondeu

Antes de seguir, tente achar o pai pelo nome, como você fez no Cenário 04:

```bash
ps -o pid,ppid,comm -C vigia
```

**Não vem nada.** E `pkill -x vigia` também não mataria nada.

O motivo é o mesmo limite de nome que você viu no Cenário 04, por outro ângulo: o nome que
o kernel guarda é o do programa que ele **realmente executou**. Este arquivo começa com
`#!/usr/bin/env python3`, então quem está rodando é o `python3` — o `vigia` é só um
argumento. Veja:

```bash
ps -eo pid,ppid,comm,args | grep -w vigia | grep -v grep
```

A coluna `comm` diz `python3`; a coluna `args`, que é a linha de comando inteira, mostra
`python3 /usr/local/bin/vigia`. Um script de `bash` se comporta diferente e aparece com o
próprio nome, que foi o caso do Cenário 04 — e é justamente por essa inconsistência que se
prefere `args` quando o que você quer é **achar** alguma coisa.

Agora desmonte a confusão, porque quase todo mundo carrega ela errada. **O zumbi já
terminou.** Ele não tem memória alocada, não consome CPU, não tem arquivo aberto, não está
travado em nada. O que sobrou dele é **uma linha na tabela de processos**, guardando o
código de saída, à espera de que alguém leia.

Quem deveria ler é o pai, chamando `wait()`. Este pai não chama. **O bug é do pai, não do
filho** — e é por isso que matar um zumbi não funciona: não há o que matar.

O custo real de um zumbi é o número de PID que ele ocupa. Um zumbi é irrelevante; um
programa que produz zumbis sem parar esgota a tabela de PIDs, e aí a máquina não consegue
criar **nenhum** processo novo — nem o que você usaria para investigar.

Prove que o problema é o pai, matando o pai. Use o PID que a lista acima mostrou — matar
pelo número é sempre o caminho mais seguro quando o nome é ambíguo:

```bash
ps -eo pid,ppid,comm,args | grep -w vigia | grep -v grep
kill <o PID do python3 que roda o vigia>
sleep 2
ps -eo pid,ppid,stat,comm | awk '$3 ~ /Z/'
```

O zumbi sumiu junto. Porque, ao perder o pai, ele foi adotado pelo PID 1 — e o PID 1 faz o
que aquele pai não fazia: recolhe o resultado e libera a linha da tabela.

É o mesmo mecanismo da seção anterior, visto do outro lado. Adotar órfãos e recolher o que
eles deixam são a mesma responsabilidade, e é por isso que o PID 1 é diferente de todos os
outros processos.

## Deixe rodando do jeito certo

Suba a versão corrigida — a que chama `wait()` — e deixe-a sobrevivendo à sua sessão:

```bash
setsid --fork vigia --corrigido </dev/null >/var/log/aurora/vigia.log 2>&1
sleep 2
ps -eo pid,ppid,comm,args | grep -w vigia | grep -v grep
ps -eo pid,ppid,stat,comm | awk '$3 ~ /Z/'
```

O `vigia` aparece com `PPID 1` e nenhum zumbi resta.

São três peças, e vale saber o que cada uma faz porque você vai precisar delas amanhã:

- **`setsid --fork`** desliga o processo do terminal, criando uma sessão nova, e o `--fork`
  garante que ele nasça como filho de ninguém — sem isso o comportamento depende de detalhes
  do seu shell, e você acaba com um processo ainda atrelado à sessão sem perceber;
- **redirecionar as três saídas** garante que ele não morra ao tentar escrever num terminal
  que não existe mais;
- o **`&`** que você usaria aqui não é necessário: com `--fork`, o `setsid` já devolve o
  prompt sozinho.

O `nohup` é o atalho clássico para parte disso: ele faz o programa ignorar o `SIGHUP`, o
sinal que o shell envia aos filhos quando a sessão termina.

## Verificação

A Verificação cobra o `vigia` vivo com **PPID exatamente 1** — não um PPID qualquer, o `1`
— e **nenhum zumbi** na máquina. As duas juntas provam que você entendeu as duas metades do
mesmo mecanismo.

E agora o incômodo honesto: tudo isso é frágil. Essa receita de `setsid` mais
redirecionamento não sobrevive a um reboot, não reinicia se o programa quebrar, não deixa
registro de que deveria estar rodando, e ninguém além de você sabe que ela existe. É
exatamente o estado em que a Aurora está desde o primeiro Cenário.

Antes de resolver isso, faltam duas perguntas: de onde vêm os programas que você tem
instalado, que é o Cenário 06, e o que fazer quando o disco enche, que é o 07.
