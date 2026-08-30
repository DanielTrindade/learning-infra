---
id: linux/09-o-servico-que-nao-sobe
titulo: O serviço que não sobe
dificuldade: autonomo
projetoCompose: linux-09
containerLinux: learning-infra-linux
---
# O serviço que não sobe

Depois que o catálogo virou serviço, a Aurora quis fazer o mesmo com a entrada de pedidos.
Alguém começou o trabalho, deixou os arquivos no lugar e saiu de férias.

O serviço não sobe. E, diferente de tudo que você fez até agora, **este Cenário não te dá
o passo a passo** — ele te dá o método e o objetivo.

Entre na máquina:

```powershell
docker exec -it learning-infra-linux bash
```

## A situação

Instale exatamente o que a pessoa deixou, sem corrigir nada no caminho:

```bash
tee /usr/local/bin/entrada-pedidos > /dev/null <<'EOF'
#!/bin/bash
if [ ! -d /srv/pedidos ]; then
  echo "diretorio /srv/pedidos nao existe" >&2
  exit 1
fi
exec /usr/bin/python3 -m http.server 8041 --directory /srv/pedidos
EOF
chmod +x /usr/local/bin/entrada-pedidos

tee /etc/systemd/system/pedidos.service > /dev/null <<'EOF'
[Unit]
Description=Entrada de pedidos da Aurora

[Service]
ExecStart=/usr/local/bin/entrada-pedidos

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl start pedidos.service
```

**Seu objetivo:** a entrada de pedidos respondendo em <http://localhost:8041> e subindo
sozinha no boot.

O caminho é seu. A próxima seção dá o método, não a resposta.

## O método

Duas perguntas, sempre nesta ordem.

```bash
systemctl status pedidos.service
```

O `status` responde **que** falhou. Duas linhas importam:

- `Active: failed (Result: exit-code)` — o serviço tentou subir e desistiu;
- `Process: ... (code=exited, status=1/FAILURE)` — com qual código de saída ele morreu. É
  o mesmo código de saída do Cenário 03, agora sendo lido por outro programa.

```bash
journalctl -u pedidos.service -n 20
```

O `journalctl -u` responde **por quê**, e é quase sempre ele que resolve. Enquanto o
`status` mostra a conclusão do systemd, o journal mostra o que o **programa** escreveu:

```
Aug 30 05:42:10 aurora entrada-pedidos[141]: diretorio /srv/pedidos nao existe
Aug 30 05:42:10 aurora systemd[1]: pedidos.service: Main process exited, code=exited, status=1/FAILURE
Aug 30 05:42:10 aurora systemd[1]: pedidos.service: Failed with result 'exit-code'.
```

A primeira linha é o **stderr** do script — o fluxo `2` que você separou à mão no
Cenário 03. Quando um programa roda como serviço, o journald captura os dois fluxos e
carimba cada linha com o nome e o PID de quem escreveu.

É por isso que um serviço bem escrito **escreve no stderr** em vez de inventar um arquivo
de log próprio: quem o operar depois vai procurar no journal, e vai achar.

Os códigos que você mais vai encontrar:

| Código | Significa |
|---|---|
| `status=1/FAILURE` | o programa rodou e saiu com erro — leia o journal |
| `status=203/EXEC` | o systemd não conseguiu **executar** o `ExecStart` — caminho errado, ou falta o bit de execução do Cenário 02 |
| `status=200/CHDIR` | o `WorkingDirectory` aponta para um lugar que não existe |

## Conserte

Agora é com você. Ao final, três coisas precisam ser verdade:

1. `systemctl is-active pedidos.service` responde `active`;
2. `systemctl is-enabled pedidos.service` responde `enabled`;
3. <http://localhost:8041> responde.

Duas armadilhas que você já conhece e que somem da cabeça sob pressão: **mexeu na unit,
rode `daemon-reload`** — o systemd não relê arquivo sozinho; e **`enable` não é `start`**,
que é a diferença inteira do Cenário 08.

Se o journal disser `Address already in use`, alguma coisa já ocupa a 8041. Descobrir
**o quê** é o Cenário 12; por ora, o `Restart=` da próxima seção também não vai salvar
você disso.

## Quando falhar de novo

Consertar é metade. A outra metade é decidir o que a máquina faz quando quebrar às três da
manhã.

Quebre de propósito:

```bash
systemctl stop pedidos.service
mv /srv/pedidos /srv/pedidos-fora
```

Acrescente estas duas linhas à seção `[Service]` da unit:

```ini
Restart=on-failure
RestartSec=1
```

E observe:

```bash
systemctl daemon-reload
systemctl start pedidos.service
sleep 8
systemctl status pedidos.service
journalctl -u pedidos.service | grep -i "repeated too quickly"
```

O systemd tenta **cinco vezes**, em intervalos de um segundo, e desiste:

```
pedidos.service: Start request repeated too quickly.
Failed to start pedidos.service.
```

Essa é a lição, e ela vale mais que o parâmetro: **`Restart=` cobre falha transitória, não
bug.** Um serviço que caiu porque o banco piscou volta sozinho. Um serviço que não sobe
porque a configuração está errada vai falhar as cinco vezes e parar — e você acorda com um
serviço parado **e** um journal cheio de tentativas.

Desfaça e deixe a máquina em pé:

```bash
mv /srv/pedidos-fora /srv/pedidos
systemctl reset-failed pedidos.service
systemctl start pedidos.service
```

O `reset-failed` limpa a marca de falha. Sem ele, o systemd continua contando as tentativas
anteriores e pode recusar iniciar de novo.

## Ordem entre serviços

Falta um par de palavras que você vai encontrar em qualquer unit de verdade, e que o
plantão do Cenário 14 vai cobrar.

- **`After=`** é **ordem**: "só me inicie depois que aquele tiver iniciado". Não cria
  obrigação nenhuma — se o outro nem existir, este sobe do mesmo jeito.
- **`Requires=`** é **dependência**: "sem aquele, eu não faço sentido". Se o outro falhar,
  este é derrubado junto.

O erro clássico é declarar `Requires=` sozinho, achando que ele também ordena. Não ordena:
o systemd sobe os dois **ao mesmo tempo**, e o seu serviço perde a corrida para o banco que
ainda está abrindo. Quando você quer as duas coisas — e quase sempre quer — declara as
duas.

## Verificação

A Verificação cobra o serviço `active` **e** `enabled`, mais a porta 8041 respondendo.

O par não é redundância. Um serviço que você iniciou à mão responde na porta e passa na
metade da checagem — e some no próximo boot, que é exatamente o estado em que a Aurora
estava antes do Cenário 08. Serviço certo pelo caminho errado não é serviço.
