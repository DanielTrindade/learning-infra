---
id: linux/14-o-plantao
titulo: O plantão
dificuldade: mestre
projetoCompose: linux-14
containerLinux: learning-infra-linux
---
# O plantão

**03h47.**

O catálogo da Aurora está fora do ar. A mensagem chegou de um cliente, não de um alerta —
a Aurora não tem alertas.

Você é quem está de plantão. Não há a quem perguntar, não há documentação além do
inventário que você mesmo escreveu no Cenário 01, e a pessoa que montou este servidor não
trabalha mais aqui há anos.

Este é o último Cenário da Trilha, e ele não te diz o que está quebrado.

## Ponha o ambiente no ar

Clique em **Iniciar cenário**, entre na máquina, e rode o preparo. Ele restaura o servidor
no estado em que foi encontrado:

```powershell
docker exec -it learning-infra-linux bash
```

```bash
tee /usr/local/bin/preparar-plantao > /dev/null <<'FIM'
#!/bin/bash
set -e
mkdir -p /srv/catalogo /var/log/aurora
printf '%s\n' '<h1>Aurora — catálogo</h1>' > /srv/catalogo/index.html
cat > /usr/lib/systemd/system/catalogo.service <<'UNIT'
[Unit]
Description=Catálogo da Aurora

[Service]
ExecStart=/usr/bin/python3 -m http.server 8044 --directory /srv/catalogo

[Install]
WantedBy=multi-user.target
UNIT
systemctl daemon-reload
systemctl enable catalogo.service
systemctl mask catalogo.service
chmod 600 /srv/catalogo/index.html
python3 - <<'PY'
import random
random.seed(11)
with open("/var/log/aurora/pedidos.log", "w") as saida:
    for i in range(60000):
        saida.write(f"2026-08-30T03:{i%60:02d}:{(i*7)%60:02d} pedido={7000+i} "
                    f"cliente=banca-central status=500 ms={random.randint(18,400)}\n")
PY
dd if=/dev/zero of=/var/log/aurora/despejo bs=1M count=20 2>/dev/null || true
setsid --fork python3 -c "import time; f=open('/var/log/aurora/pedidos.log'); time.sleep(86400)" </dev/null >/dev/null 2>&1
sleep 1
rm -f /var/log/aurora/pedidos.log /var/log/aurora/despejo
setsid --fork python3 -m http.server 8044 --directory /tmp </dev/null >/dev/null 2>&1
sleep 1
echo "ambiente restaurado"
FIM
chmod +x /usr/local/bin/preparar-plantao
preparar-plantao
```

O script está no seu terminal e você pode lê-lo. Se preferir descobrir sozinho, não leia —
o Cenário funciona melhor assim.

**Seu objetivo:** o catálogo respondendo em <http://localhost:8044>, ativo e habilitado, e
a máquina saudável.

## O que você já sabe

Nenhuma dica sobre este plantão. Só o mapa do que a Trilha te deu:

| Pergunta | Onde você aprendeu |
|---|---|
| quem está rodando e consumindo? | Cenário 04 |
| quanto espaço e quantos inodes restam? | Cenário 07 |
| esta unit existe, está habilitada, está mascarada? | Cenários 08 e 09 |
| o que o serviço disse antes de morrer? | Cenários 09 e 11 |
| quem é o dono desta porta? | Cenário 12 |
| quem consegue ler este arquivo? | Cenários 02 e 13 |

Um método, que é o que um plantão exige mais que qualquer comando: **vá do sintoma para a
máquina, não da máquina para o sintoma.** Comece pelo que o cliente não consegue fazer e
ande para trás — a porta, o processo, o serviço, o arquivo. Varrer a máquina inteira
procurando "algo errado" às quatro da manhã é como o `chmod 777`: parece progresso e não é.

Dois avisos honestos, para você não perder tempo com falso positivo:

- `systemctl list-unit-files --state=masked` mostra **várias** units mascaradas nesta
  imagem, e a maioria é assim de fábrica, de propósito. Encontrar uma unit mascarada não é,
  por si só, encontrar o problema;
- pode haver mais de uma coisa errada. Consertar uma e ver que ainda não funciona não
  significa que você errou o diagnóstico.

## Verificação

Quatro coisas precisam ser verdade ao final:

1. `catalogo.service` **ativo e habilitado**;
2. <http://localhost:8044> respondendo, e respondendo **o catálogo da Aurora** — não
   qualquer coisa que esteja naquela porta;
3. `/srv/catalogo/index.html` em modo **644**, do `root`;
4. a partição `/var/log/aurora` **abaixo de 50%**.

Nada além disso. Como chegar lá é o que a Trilha inteira te preparou para decidir.

## Fim da Trilha

Você resolveu o plantão. Vale olhar o que isso custou.

Cada conserto desta noite foi manual. Você digitou comandos que ninguém viu, num servidor
que só você acabou de entender, e **nada disso está escrito em lugar nenhum**. Se a Aurora
precisar de um segundo servidor amanhã, alguém vai montá-lo do zero — e ele não vai ficar
igual a este. Se você sair de férias, a próxima pessoa começa exatamente onde você começou
no Cenário 01: com um acesso e nenhuma documentação.

A Aurora chegou ao limite do que um servidor operado à mão aguenta. O problema não é mais
técnico, é de **repetibilidade**: subir esta máquina de novo, igual, sem depender de
memória.

A resposta é empacotar a aplicação junto com tudo de que ela precisa — sistema de arquivos,
dependências, configuração, o comando que a inicia — num artefato que sobe igual em
qualquer lugar. É onde a **Trilha Docker** começa.

E você já viu os dois mecanismos que tornam isso possível, nomeados nos Fundamentos:
**namespace**, que faz um conjunto de processos não enxergar os outros, e **cgroup**, que
limita o que eles podem consumir. Não é mágica nova. É o Linux que você acabou de aprender,
usado de outro jeito.

Você entrou nesta Trilha sem saber o que era um processo. Sai dela tendo resolvido um
plantão sozinho, às quatro da manhã, numa máquina que ninguém te explicou.
