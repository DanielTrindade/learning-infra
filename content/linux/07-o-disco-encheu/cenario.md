---
id: linux/07-o-disco-encheu
titulo: O disco encheu
dificuldade: assistido
projetoCompose: linux-07
containerLinux: learning-infra-linux
---
# O disco encheu

Os pedidos pararam de ser registrados durante a madrugada. O programa que grava roda, não
reclama na tela, e o arquivo não cresce.

Este Cenário entrega dois incidentes que todo mundo que opera Linux encontra e quase
ninguém sabe explicar: um disco que continua cheio depois de você apagar o arquivo grande,
e um `No space left on device` com o disco **vazio**.

Entre na máquina:

```powershell
docker exec -it learning-infra-linux bash
```

## Onde você está pisando

Na Aurora, o `/var/log/aurora` não é uma pasta comum: é uma **partição própria**, separada
do resto do sistema. Isso é prática corrente e existe por um motivo — log que cresce sem
controle enche a partição dele e para ali, em vez de derrubar a máquina inteira.

Veja os dois lados:

```bash
df -h /var/log/aurora
df -h /
```

A partição de log tem **8 MB**. O `/` tem cerca de 1 TB.

Guarde isso, porque é a primeira armadilha do assunto: **"o disco" não existe no
singular**. Uma máquina tem vários sistemas de arquivos montados em pontos diferentes, e
"está cheio" é sempre uma pergunta sobre **qual deles**. Olhar o `df` do lugar errado é como
conferir o tanque do carro do vizinho.

## Reproduza a madrugada

Gere o log que cresceu sem ninguém aparar:

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

Mais da metade da partição — `55%` — num arquivo só.

Agora encha o resto, que é o que a madrugada fez sozinha:

```bash
dd if=/dev/zero of=/var/log/aurora/despejo bs=1M count=20
df -h /var/log/aurora
```

O `dd` para no meio:

```
dd: IO error: No space left on device
```

E a partição está em `100%`. Esse é o erro que o programa de pedidos vinha recebendo a
noite toda — e que ninguém viu, porque ele o mandava para uma saída que ninguém lia.

## Apagar não é o mesmo que liberar

Reproduza o que existe em qualquer máquina de verdade: um programa com o log **aberto**
enquanto trabalha.

```bash
python3 -c "import time; f=open('/var/log/aurora/pedidos.log'); time.sleep(3600)" &
sleep 2
```

Agora faça o que todo mundo faz sob pressão — apague o arquivo grande e respire aliviado:

```bash
rm /var/log/aurora/despejo /var/log/aurora/pedidos.log
du -sh /var/log/aurora
df -h /var/log/aurora
```

E aqui o Cenário acontece. O `du` responde **`0`**. O `df` responde **`4.4M`, `55%`**.

O diretório está vazio e a partição continua ocupada pela metade.

Não é cache, não é atraso, não é bug. Use o vocabulário que o Cenário 01 plantou: o `rm`
não apaga conteúdo — ele remove **um nome** que apontava para um inode. O conteúdo só é
liberado quando aquele inode não tem mais **nenhum nome apontando para ele** *e* **nenhum
processo com ele aberto**.

Aquele `python3` ainda tem o arquivo aberto. Enquanto tiver, os 4,4 MB seguem gastos num
arquivo que já não tem nome nenhum.

E a diferença entre as duas ferramentas deixa de ser trivia:

- **`du` percorre nomes** — anda pelo diretório somando o que encontra. Não vê o que não
  tem mais nome.
- **`df` pergunta ao sistema de arquivos** quanto ele tem livre. Vê tudo.

**Quando `df` e `du` discordam, a resposta quase sempre é esta.**

## Ache quem está segurando

```bash
lsof +L1
```

```
COMMAND PID USER FD   TYPE DEVICE SIZE/OFF NLINK NODE NAME
python3  68 root 3r   REG  0,508  4602181     0    2 /var/log/aurora/pedidos.log (deleted)
```

Leia campo a campo, porque cada um conta um pedaço:

- o `+L1` pede exatamente os arquivos abertos com **menos de um nome**;
- `NLINK` é **`0`** — nenhum nome aponta para ele;
- `(deleted)` é o `lsof` dizendo o mesmo em palavras;
- `SIZE/OFF` mostra os 4,6 MB que não voltaram;
- e `PID` é a resposta que você veio buscar.

Resolva:

```bash
kill $(lsof -t +L1)
sleep 2
df -h /var/log/aurora
```

`8.0M 0 8.0M 0%`. O espaço voltou **no instante** em que o último descritor fechou.

A regra de ofício: depois de apagar arquivo grande, **confira o `df`**. Se ele não se
mexeu, o arquivo continua aberto em algum lugar — e reiniciar o programa que o segura
resolve, sem precisar reiniciar a máquina. É a diferença entre um plantão de dois minutos
e uma janela de manutenção.

## Cheio sem nada dentro

O segundo incidente, e o mais confuso dos dois. Simule o diretório de rotação que ninguém
limpou desde 2019:

```bash
mkdir -p /var/log/aurora/rotacao
for i in $(seq 1 2100); do : > /var/log/aurora/rotacao/antigo-$i.log; done
```

A partir do 2047º arquivo, cada linha responde `No space left on device`. Agora olhe os
dois lados:

```bash
df -h /var/log/aurora
df -i /var/log/aurora
```

O `df -h` diz **`8.0M 0 8.0M 0%`** — o disco está **vazio**. O `df -i` diz **`2048 2048 0
100%`**.

Um sistema de arquivos tem **dois** recursos finitos, não um:

- **espaço**, que guarda o conteúdo dos arquivos;
- **inodes**, que guardam os arquivos em si — um por arquivo, independente do tamanho.

Dois mil arquivos vazios não gastam espaço nenhum e gastam dois mil inodes. Quando os
inodes acabam, você não cria mais **nada**, com a partição inteira livre.

O teste que fecha o diagnóstico em dois segundos:

```bash
touch /var/log/aurora/novo.log
echo "linha" >> /var/log/aurora/rotacao/antigo-1.log
```

O `touch` falha e a escrita no arquivo **já existente** funciona. Criar precisa de um inode
novo; escrever num arquivo que já existe, não. É essa assimetria que distingue os dois
incidentes — e nenhum `df -h` do mundo te contaria isso.

Limpe:

```bash
rm -rf /var/log/aurora/rotacao
df -i /var/log/aurora
```

## Rotação é a prevenção

Nomeie o que faltava desde o começo: **nenhum dos dois incidentes é sobre disco**. Os dois
são sobre log crescendo sem ninguém aparar.

Rotacionar é trocar o arquivo ativo de tempos em tempos, comprimir os antigos e apagar os
mais velhos que um limite — por tamanho, por idade, ou pelos dois. É o que impede tanto o
arquivo gigante quanto a montanha de arquivinhos.

E repare na armadilha que este Cenário já demonstrou: uma rotação que **apaga** o arquivo
enquanto o programa o mantém aberto cai exatamente na seção "apagar não é o mesmo que
liberar" — o espaço não volta e o programa segue escrevendo num arquivo sem nome. É por
isso que rotação de verdade avisa o programa, ou trunca o arquivo em vez de removê-lo.

A ferramenta que faz isso na prática, e o agendamento que a dispara toda noite, são o
Cenário 10.

## Deixe a máquina em pé

Recrie o registro de pedidos, agora de tamanho sensato:

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

## Verificação

Três coisas, e as três juntas são o Cenário: a partição **abaixo de 50%**, **nenhum
descritor preso** a arquivo apagado, e o `pedidos.log` **existindo de novo**.

A segunda existe para impedir a solução falsa. Dá para liberar espaço matando qualquer
processo até a barra baixar, e ir dormir com a causa intacta. A terceira impede a outra
solução falsa: apagar tudo, inclusive o que a Aurora precisa gravar.

## Fim do Ato II

Em quatro Cenários a máquina deixou de ser um disco com arquivos e virou um lugar onde
coisas rodam, consomem, morrem, se recusam a morrer e enchem partições.

E em todos eles apareceu o mesmo incômodo: **nada disso sobrevive a um boot**. Todo
programa que você deixou rodando depende de alguém ter digitado um comando, e ninguém
anotou quais foram — que é exatamente a situação em que você recebeu esta máquina no
Cenário 01.

O Ato III resolve isso.
