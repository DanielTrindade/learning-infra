---
id: docker/09-container-privilegios-demais
titulo: O container com privilégios demais
dificuldade: assistido
projetoCompose: lab-09
---
# O container com privilégios demais

Até aqui o que importava era a aplicação **funcionar**. Este Cenário é sobre a aplicação
funcionar com o **mínimo de privilégios** — porque por padrão um container roda como
`root`, com o filesystem gravável e com todas as capabilities de que a imagem precisar.
Funciona, claro. Só que é exatamente o que a
[segurança do Engine](https://docs.docker.com/engine/security/) manda você não fazer.

Este Cenário é `Assistido`: você recebe o objetivo e as peças, não as peças prontas.

## O ambiente

Clique em **Iniciar cenário**. O diretório de trabalho tem `app/` — uma aplicação Node
pura, projeto `lab-09` — e um `compose.yaml` entregue:

```yaml
services:
  app:
    build: ./app
    ports:
      - "8096:3000"
```

Só isso: a imagem sobe, a porta 8096 responde. Há **um detalhe de propósito** na
aplicação: a cada requisição ela escreve um arquivo em `/tmp/ping.txt`. Escrever em
`/tmp` é exatamente o tipo de coisa que uma aplicação legítima faz — e vai ser o que
prova que a solução não quebrou nada. Lembre disso.

## O problema

O `compose.yaml` entregue é o padrão que o Docker assume quando você não pede nada: o
processo roda como **`root`**, o filesystem é **gravável** e o container recebe o
**conjunto completo de capabilities** do Linux.

São quatro camadas de privilégio, e cada uma merece sua dúvida:

**Rodar como `root`.** Parece inofensivo porque "root dentro do container não é root no
host". Isso é só parcialmente verdade: o container **compartilha o kernel do host**. O
`root` de dentro é `root` de fora para o que não é isolado por namespace — e um processo
comprometido rodando como `root` tem muito mais caminho até o host do que um que roda
como um usuário comum. Por isso o Cenário #6 terminou com o `USER node` no Dockerfile:
é a mesma lição, agora no Compose.

**Filesystem gravável.** Se a aplicação não precisa escrever no disco, o disco não
deveria estar gravável. Um processo comprometido num filesystem somente leitura não
planta binário, não edita configuração, não deixa artefato para a próxima etapa.

**Capabilities.** O Linux não dá privilégio em bloco: ele os divide em *capabilities* —
`NET_ADMIN`, `SYS_PTRACE`, `DAC_OVERRIDE` e dezenas de outras. O Docker entrega um
conjunto de origem, e quase nenhuma aplicação precisa dele inteiro.

**O `--privileged`.** Existe uma forma de jogar tudo isso fora: rodar com
`docker run --privileged` — que dá ao container **todas** as capabilities, acesso a
quase todos os dispositivos do host e o mesmo poder do `root` do host sobre o kernel.
É raríssimo precisar, e não raro quem usa não sabe o que está soltando. **Este Cenário
não toca nisso de propósito.** A lição aqui é o caminho contrário: começar com pouco.

## O objetivo

Quatro mudanças no `compose.yaml` entregue, e a aplicação continua respondendo
**`Cenário 09 seguro`** na porta 8096:

1. rodar como usuário **`node`**, e não como `root`;
2. filesystem **somente leitura** (`read_only`);
3. uma área gravável **transitória** em `/tmp` — para a escrita do `ping.txt` continuar
   funcionando (via `tmpfs`);
4. remover **todas** as capabilities (`cap_drop: ALL`).

## As peças

As quatro linhas, no serviço `app`:

```yaml
user: node
read_only: true
tmpfs:
  - /tmp
cap_drop:
  - ALL
```

Cada uma é uma decisão independente:

- **`user: node`** — a imagem `node:22-alpine` já cria o usuário `node`; é só dizer que o
  processo deve rodar com ele em vez do `root`. (No Cenário #6 isso foi feito dentro do
  Dockerfile com `USER`; aqui a decisão fica no Compose — cada projeto escolhe seu lado
  da linha.)
- **`read_only: true`** — congela o filesystem do container. Qualquer escrita fora do
  `tmpfs` falha.
- **`tmpfs: - /tmp`** — monta um filesystem **em memória** em `/tmp`: gravável, rápido e
  efêmero — morre com o container. É a resposta a "mas e se a aplicação precisa
  escrever?". Dado transitório mora em `tmpfs`; dado que precisa durar mora em volume
  (Cenário #5).
- **`cap_drop: ALL`** — tira todas as capabilities do container. A aplicação é um
  servidor HTTP que escreve um arquivo de ping: ela não precisa de nenhuma.

## O caminho

1. Com o `compose.yaml` entregue, suba e confirme o estado inicial:

   ```sh
   docker compose -p lab-09 up -d --build
   docker compose -p lab-09 exec app whoami
   ```

   O `whoami` responde `root`. É o ponto de partida.

2. Clique em **Verificar** sem mexer em nada: as três primeiras Asserções passam — o
   container roda e a 8096 responde com o texto certo — mas a `container_configuracao`
   reprova. É ela que mede a lição.

3. Edite o `compose.yaml` adicionando as quatro peças ao serviço `app`.

4. Recrie o stack:

   ```sh
   docker compose -p lab-09 up -d --build
   ```

5. Confirme que a escrita transitória não quebrou — o `ping.txt` existe e foi gravado
   pela última requisição que você fez ao abrir `http://localhost:8096`:

   ```sh
   docker compose -p lab-09 exec app ls /tmp/ping.txt
   ```

   Se o `read_only` fosse só "negar escrita", a aplicação teria caído na primeira
   requisição. O `tmpfs` não é enfeite: é a parte da solução que mantém a app de pé.

6. Clique em **Verificar**: as quatro Asserções devem passar.

Para provar que a mudança é real e não só cosmética, compare de novo:

```sh
docker compose -p lab-09 exec app whoami
```

Agora responde `node`.

## Verifique

Quatro Asserções: o container `lab-09-app-1` rodando, a 8096 respondendo 200, o corpo
com **`Cenário 09 seguro`** — e a que decide o Cenário, `container_configuracao`, que
confere de uma vez usuário `node`, filesystem somente leitura e capabilities removidas.
As três HTTP existem para amarrar a segurança à funcionalidade: se o `read_only`
quebrasse a escrita do `tmpfs`, a app não responderia o texto, e nada disso passaria.

## O que você aprendeu

Que um container que "funciona" não é um container pronto para produção: o padrão dele
é rodar como `root`, com o disco gravável e com todas as capabilities — e que `root`
dentro do container não é isolamento, porque o kernel é do host. Que `user: node` tira o
processo do `root`; que `read_only: true` congela o filesystem; que `tmpfs` devolve um
lugar gravável só para o dado transitório; e que `cap_drop: ALL` corta o privilégio que
a aplicação não usa. E que existe o `--privileged` — o botão que solta tudo isso de
volta, junto com o socket do Docker que, montado num container, equivale a dar chave do
host. Os dois ficaram de fora deste Cenário de propósito: esta lição é sobre começar com
pouco, não sobre precisar de muito.
