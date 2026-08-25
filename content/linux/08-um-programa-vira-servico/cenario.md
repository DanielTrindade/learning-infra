---
id: linux/08-um-programa-vira-servico
titulo: Um programa vira serviço
dificuldade: guiado
projetoCompose: linux-08
containerLinux: learning-infra-linux
---
# Um programa vira serviço

Até aqui a Aurora tocava seus programas na mão: para servir o catálogo, alguém abria
uma sessão e deixava um processo rodando. Na primeira reinicialização, o catálogo sumia
e ninguém lembrava como ele subia de novo.

Você vai acabar com isso: transformar o programa em um **serviço** que o systemd mantém
no ar e sobe sozinho a cada boot.

## Entre na máquina

Clique em **Iniciar cenário**. Depois, no seu terminal:

```powershell
docker exec -it learning-infra-linux bash
```

Essa linha é a porta de entrada da Trilha inteira — ela te coloca dentro da máquina da
Aurora. A Trilha Docker, logo em seguida, explica cada pedaço dela; por ora, use-a como
uma incantação. Confirme que a máquina está saudável:

```bash
systemctl is-system-running
```

A resposta é `running`: o sistema inteiro está de pé, e o `systemctl` é a ferramenta que
você vai usar para conversar com ele.

## O programa

O catálogo da Aurora é um site estático servido pelo Python, que já está instalado. Crie
o conteúdo e experimente o programa rodando na mão:

```bash
mkdir -p /srv/catalogo
echo '<h1>Aurora — catálogo</h1>' > /srv/catalogo/index.html
python3 -m http.server 8040 --directory /srv/catalogo
```

O comando fica ocupando o terminal enquanto serve. Abra <http://localhost:8040> no
navegador para ver o catálogo. Agora volte ao shell e pressione `Ctrl-C`: o programa
parou, e o catálogo sumiu junto.

É exatamente isso que o serviço resolve. Um programa rodando na mão só vive enquanto a
sessão que o iniciou estiver viva. Um serviço é um programa que o systemd prometeu
manter no ar — e a promessa vale mesmo depois de um boot.

## Escreva a unit

Um serviço systemd é descrito por um arquivo **unit**. Crie o do catálogo:

```bash
tee /etc/systemd/system/catalogo.service > /dev/null <<'EOF'
[Unit]
Description=Catálogo da Aurora

[Service]
ExecStart=/usr/bin/python3 -m http.server 8040 --directory /srv/catalogo

[Install]
WantedBy=multi-user.target
EOF
```

Três seções, cada uma com um papel:

- `[Unit]` descreve o serviço para humanos e para o próprio systemd.
- `[Service]` diz o que executar — o `ExecStart` é o mesmo comando que você rodou na mão.
- `[Install]` diz **quando** o serviço deve subir: `WantedBy=multi-user.target` o liga ao
  boot normal da máquina.

O arquivo ainda não faz nada. O systemd não lê um arquivo novo até você mandar:

```bash
systemctl daemon-reload
```

## A diferença entre `start` e `enable`

`start` e `enable` respondem a perguntas diferentes, e o Cenário inteiro existe por causa
dessa diferença.

```bash
systemctl start catalogo.service
systemctl status catalogo.service
```

O `start` sobe o serviço **agora**. Mas se a máquina reiniciar, o systemd não o subirá de
novo — `start` não deixa registro nenhum de intenção.

O `enable` é o que registra a intenção:

```bash
systemctl enable catalogo.service
```

Repare que `enable` sozinho não sobe o serviço; ele só cria o vínculo de boot. O atalho
para os dois ao mesmo tempo:

```bash
systemctl enable --now catalogo.service
```

Confirme o estado:

```bash
systemctl is-active catalogo.service
systemctl is-enabled catalogo.service
```

O `is-active` responde `active` e o `is-enabled` responde `enabled`. O catálogo está no
ar em <http://localhost:8040>.

## Verificação

A Verificação cobra os dois estados: o serviço precisa estar **ativo** (rodando) **e**
**habilitado** (sobe sozinho no boot). Um serviço iniciado com `start` na mão passa na
primeira checagem e falha na segunda — subir o catálogo certo pelo caminho errado não
conta.
