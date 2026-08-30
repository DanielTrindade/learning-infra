---
id: linux/12-onde-este-pacote-vai-dar
titulo: Onde este pacote vai dar
dificuldade: assistido
projetoCompose: linux-12
containerLinux: learning-infra-linux
---
# Onde este pacote vai dar

O catálogo da Aurora responde quando alguém testa **dentro** da máquina e não responde do
navegador de quem está fora. Ninguém sabe dizer se o problema é o programa, a máquina, ou o
caminho entre elas.

São três coisas diferentes e o instinto é chutar uma. Quatro comandos separam as três.

Entre na máquina:

```powershell
docker exec -it learning-infra-linux bash
```

## Que endereços esta máquina tem

```bash
ip -brief addr
ip addr show eth0
```

O `-brief` dá o resumo:

```
lo               UNKNOWN        127.0.0.1/8 ::1/128
eth0@if138       UP             172.24.0.2/16
```

O número depois do `@` e o endereço exato variam a cada vez que o ambiente sobe — o que
não muda são os papéis.

Duas interfaces, com papéis diferentes:

- **`lo`** é o *loopback*. Ele existe só dentro da máquina e não tem cabo nem placa. É por
  ele que `localhost` e `127.0.0.1` funcionam — e um pacote que sai por ele **nunca** chega
  a outro computador.
- **`eth0`** é a placa de rede de verdade, com o endereço pelo qual esta máquina é
  alcançada de fora: `172.24.0.2`.

O `/16` é a **máscara**: diz quantos bits do endereço identificam a *rede* em vez do host.
Com `/16`, tudo que começa com `172.24.` é vizinho — alcançável direto, sem intermediário.

## Por onde um pacote sai

```bash
ip route
```

```
default via 172.24.0.1 dev eth0
172.24.0.0/16 dev eth0 proto kernel scope link src 172.24.0.2
```

Leia de baixo para cima. A segunda linha diz: "para a minha própria rede, entregue direto
pela `eth0`". A primeira diz: "para **qualquer outro lugar**, entregue a `172.24.0.1` e
deixe que ele resolva".

Essa primeira é a rota **default**, e ela é o "não sei, pergunte àquele ali". É a primeira
coisa a olhar quando a máquina alcança os vizinhos e não alcança a internet: sem rota
default, o pacote não tem para onde ir e o erro que você vê é um tempo esgotado, que não
diz nada.

## Do nome ao endereço

```bash
cat /etc/resolv.conf
getent hosts archive.ubuntu.com
```

O `resolv.conf` aponta para `nameserver 127.0.0.11` — o resolvedor interno do Docker. Numa
máquina de verdade esse endereço seria o do DNS da sua rede, e é o arquivo que se olha
quando "a internet caiu" mas o IP responde.

Use `getent hosts`, não `ping`, e por um motivo que economiza tempo: o `getent` pergunta
exatamente o que o sistema perguntaria, passando por `/etc/hosts` e pelo DNS na ordem
configurada, e responde **só** sobre resolução de nome. O `ping` mistura resolver o nome
com alcançar a máquina — quando ele falha, você ainda não sabe qual das duas coisas quebrou.

Separar sintomas é metade do diagnóstico.

## Quem está ouvindo

Suba o catálogo como serviço, do jeito que você aprendeu no Cenário 08:

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

A linha que importa:

```
LISTEN 0  5  0.0.0.0:8042  0.0.0.0:*  users:(("python3",pid=223,fd=3))
```

Campo a campo:

- `LISTEN` — o estado: este socket está esperando conexão;
- `0.0.0.0:8042` — **onde** ele aceita;
- `users:(("python3",pid=223,fd=3))` — **quem é o dono**: o nome do programa, o PID, e o
  descritor de arquivo, o mesmo número que você viu o `lsof` mostrar no Cenário 07.

As letras do comando: `-t` só TCP, `-l` só quem escuta, `-n` números em vez de nomes de
serviço, `-p` o processo dono. `ss -tlnp` é uma das combinações mais úteis que existem, e
vale decorar.

### A diferença que resolve metade dos incidentes

Compare estes dois endereços de escuta:

| Endereço | Quem alcança |
|---|---|
| `0.0.0.0:8042` | qualquer um, de dentro ou de fora |
| `127.0.0.1:8042` | **só** processos da própria máquina |

Um serviço que escuta apenas em `127.0.0.1` funciona perfeitamente para quem testa por
dentro com `curl localhost` — e é **invisível** de fora. É exatamente o sintoma que abriu
este Cenário, e é uma configuração, não um defeito: muitos programas escutam só no loopback
por padrão, de propósito, para não ficarem expostos antes de alguém decidir isso.

Confirme de dentro e depois pelo navegador, em <http://localhost:8042>:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8042
```

## Verificação

A Verificação cobra que a porta 8042 esteja escutando **com dono identificável** no
`ss -tlnp`, e que ela responda HTTP.

Repare no que a primeira exige de verdade: não basta algo respondendo na porta — o `ss`
precisa conseguir dizer **qual processo** é. Numa máquina de plantão, uma porta ocupada por
um dono que você não consegue nomear é pior que uma porta livre.

E fecha uma pendência do Cenário 09: aquele `Address already in use` que o journal mostrou
agora tem um comando que responde quem está ocupando. Guarde isso — você vai precisar dele
sem ninguém avisar.
