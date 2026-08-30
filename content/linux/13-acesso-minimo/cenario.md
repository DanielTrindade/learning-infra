---
id: linux/13-acesso-minimo
titulo: Acesso mínimo
dificuldade: autonomo
projetoCompose: linux-13
containerLinux: learning-infra-linux
---
# Acesso mínimo

A Aurora vai contratar alguém para operar o catálogo. Essa pessoa precisa reiniciar o
serviço quando ele travar — e não precisa, nem deve, virar dona da máquina.

E tem a outra ponta, que ninguém olhou até agora: o catálogo roda como `root`. Um servidor
de arquivos estáticos, exposto na rede, com poder total sobre a máquina. Não porque alguém
decidiu isso — porque ninguém decidiu nada.

**Seu objetivo:** o catálogo servindo em <http://localhost:8043> sob um usuário que **não é
`root`**, e uma regra de `sudo` que permita àquele usuário **apenas** reiniciar aquele
serviço.

Como no Cenário 09, o caminho é seu.

Entre na máquina:

```powershell
docker exec -it learning-infra-linux bash
```

## Root não é tudo ou nada

Antes de tirar poder de alguém, veja de que o poder é feito:

```bash
capsh --print | head -3
```

A primeira linha lista o que o `root` **desta máquina** pode:

```
Current: cap_chown,cap_dac_override,cap_fowner,cap_fsetid,cap_kill,cap_setgid,
cap_setuid,cap_setpcap,cap_net_bind_service,cap_net_raw,cap_sys_chroot,cap_mknod,
cap_audit_write,cap_setfcap=ep
```

São **catorze**. Não todas.

Este `root` não pode carregar módulo de kernel, não pode mudar o relógio do sistema, não
pode mexer na rede do host. O Docker removeu essas antes de você entrar, e você nem
percebeu — porque nada que você fez até agora precisou delas.

Isso é a prova concreta do que os Fundamentos afirmaram: **"root" é o nome que se dá a ter
todas as capabilities**, e nada obriga um processo a ter todas. Um programa pode receber
exatamente o poder de que precisa, e mais nada.

A pergunta certa nunca foi "isso precisa de root?". É **"de qual poder, exatamente, isso
precisa?"**

## Uma capability num arquivo

Capabilities não vivem só em processos: podem ficar gravadas no próprio arquivo do
programa.

```bash
getcap -r /usr/bin /usr/sbin
```

Nesta máquina não há nenhuma — a lista vem vazia. Crie uma para ver como funciona:

```bash
cp "$(readlink -f /usr/bin/python3)" /usr/local/bin/py-rede
setcap cap_net_raw+ep /usr/local/bin/py-rede
getcap /usr/local/bin/py-rede
```

```
/usr/local/bin/py-rede cap_net_raw=ep
```

O `readlink -f` é necessário: `/usr/bin/python3` é um atalho, e marcar um atalho não marca
nada — a armadilha do symlink do Cenário 01, voltando com consequência.

O `+ep` são dois conjuntos: **p**ermitted, o processo tem direito à capability;
**e**ffective, ela já nasce ativa. E `cap_net_raw` concede abrir socket bruto — é o que o
`ping` precisa e o que um servidor HTTP não precisa. É assim que o `ping` funciona para
usuário comum numa máquina moderna, sem ser `setuid root`.

## O que este ambiente não prova

Esta seção é tão importante quanto as outras, e é sobre desconfiar do próprio laboratório.

O exemplo clássico de capability é a porta baixa: um processo não-`root` não conseguiria
escutar abaixo da porta 1024 sem `CAP_NET_BIND_SERVICE`. **Aqui isso não acontece.**

```bash
cat /proc/sys/net/ipv4/ip_unprivileged_port_start
```

Responde `0`. Esse ajuste define a partir de qual porta um processo comum pode escutar, e o
Docker o deixa em zero. Neste container, **qualquer usuário liga na porta 80** sem
capability nenhuma.

E não dá para corrigir só para estudar:

```bash
sysctl -w net.ipv4.ip_unprivileged_port_start=1024
```

```
sysctl: setting key "net.ipv4.ip_unprivileged_port_start", ignoring: Read-only file system
```

O `/proc/sys` é do host e está montado somente para leitura. É a linha **"Compartilhado"**
da tabela de fidelidade dos Fundamentos acontecendo na sua frente: o kernel é da VM do
Docker Desktop, o Cenário observa e não altera.

Numa máquina de verdade o valor é `1024` e a regra vale.

### E mais uma, que você vai encontrar sozinho

Quando o catálogo estiver rodando como usuário de serviço, tente o comando do Cenário 12:

```bash
ss -tlnp | grep 8043
```

A porta aparece, e a coluna `users:((...))` **vem vazia** — diferente do que aconteceu no
Cenário 12.

O motivo está na lista que você leu no começo desta página: `cap_sys_ptrace` **não está**
entre as catorze. Para descobrir qual processo é dono de um socket, o `ss` precisa ler o
`/proc` daquele processo — e ler o `/proc` de um processo de **outro usuário** exige essa
capability. No Cenário 12 funcionou porque o serviço rodava como `root`, o mesmo usuário de
quem perguntava.

Numa máquina de verdade o `root` tem `cap_sys_ptrace` e a coluna aparece. Aqui, pergunte ao
systemd em vez de ao `ss`:

```bash
systemctl show -p MainPID --value catalogo.service
stat -c %U /proc/$(systemctl show -p MainPID --value catalogo.service)
```

Duas limitações do ambiente na mesma página, e nenhuma delas é acidente: as duas vêm de o
container ter **menos poder** que uma máquina inteira — que é exatamente o assunto deste
Cenário, visto de fora.

Guarde a lição maior, que serve para qualquer laboratório que você usar na vida: **saber o
que o seu ambiente não reproduz é parte de saber operar.** Um teste que passa por um motivo
diferente do que você imagina é pior que um teste que falha.

É por isso que este Cenário usa a porta 8043, que não precisaria de capability nenhuma nem
numa máquina de verdade. O que se prova aqui é o **usuário** do processo, não o privilégio
da porta.

## Faça

As peças, sem a montagem.

**Para o usuário de serviço:**

```
useradd -r -s /usr/sbin/nologin
```

O `-r` cria uma conta de sistema e o `-s /usr/sbin/nologin` garante que ninguém faça login
com ela. Uma identidade que existe para um programa usar, não uma pessoa.

**Para a unit**, três diretivas que você ainda não usou:

| Diretiva | O que faz |
|---|---|
| `User=` | troca a identidade com que o `ExecStart` roda |
| `NoNewPrivileges=true` | impede o processo de ganhar poder depois de iniciado, mesmo por `setuid` |
| `CapabilityBoundingSet=` | define o teto: nem o `root` daquele processo passa disso |

**Para o `sudo`:** regras vão em arquivos dentro de `/etc/sudoers.d/`, um por assunto, em
vez de editar o `/etc/sudoers` central. O formato de uma linha é:

```
usuario ALL=(root) NOPASSWD: /caminho/completo/do/comando com argumentos
```

O caminho precisa ser completo, e os argumentos fazem parte da regra — `systemctl restart
catalogo.service` é uma permissão bem diferente de `systemctl`.

**E valide antes de confiar:**

```bash
visudo -c
```

Ele responde `parsed OK` por arquivo. Um erro de sintaxe em `/etc/sudoers.d/` tranca o
`sudo` da máquina inteira, e você descobre no pior momento possível. Esta é a única linha
deste Cenário que você não deve pular.

Para conferir o resultado do ponto de vista de quem vai usar:

```bash
runuser -u <o usuário> -- sudo -n -l
```

## Por que 440

Todo mundo escreve `440` nesses arquivos. Quase ninguém sabe dizer por quê, e a explicação
que circula está errada.

O `sudo` **não** exige `440`. Ele recusa o arquivo apenas quando ele é **gravável por
outros**:

| Modo | O que o `sudo` faz |
|---|---|
| `440`, `444`, `400` | aceita |
| `664` | aceita |
| `646`, `666` | recusa: `is world writable` |

Veja acontecer:

```bash
chmod 646 /etc/sudoers.d/aurora-svc
runuser -u aurora-svc -- sudo -n -l
chmod 440 /etc/sudoers.d/aurora-svc
runuser -u aurora-svc -- sudo -n -l
```

Então por que `440` virou convenção? Porque é o **mínimo que funciona** e porque **expressa
a intenção**: o `root` lê, o grupo do `root` lê, e ninguém escreve. Um arquivo que decide
quem pode virar `root` não tem motivo para ser gravável por mais ninguém — nem legível por
quem não precisa.

O `/etc/sudoers.d/README` que já veio nesta máquina está exatamente assim. Confira:

```bash
stat -c "%a %U %G" /etc/sudoers.d/README
```

Convenção não é lei, e saber a diferença é o que separa seguir uma regra de entendê-la.

## Verificação

Três coisas:

1. `/etc/sudoers.d/aurora-svc` existe, em modo **440**, do `root`;
2. o processo principal do serviço do catálogo **não é `root`**;
3. o binário que você marcou carrega a capability que você concedeu.

A segunda é a que resume o Cenário. Ela não pergunta se o catálogo responde — pergunta
**quem** está respondendo. Um serviço no ar rodando como `root` sem precisar não é um
serviço pronto; é um incidente que ainda não aconteceu.
