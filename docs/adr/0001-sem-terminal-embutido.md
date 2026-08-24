# Sem terminal embutido na plataforma

Plataformas de laboratório (Killercoda, KodeKloud) embutem um terminal no browser
porque o usuário é remoto e não tem ambiente próprio. Aqui o usuário é único e
executa na própria máquina, então decidimos não ter terminal na plataforma — o
leitor roda os comandos no terminal nativo dele. Um `xterm.js` conversando com um
pty por websocket seria o componente mais caro do sistema e entregaria uma
experiência pior que o terminal que o leitor já tem: sem o shell dele, sem os
aliases dele, sem o editor dele, com copy/paste pior.

## Alternativas consideradas

- **PTY no host** (`pty4j` com ConPTY, via websocket para `xterm.js`). Funciona, mas
  entrega um shell idêntico ao que o leitor já tem, com uma camada de rede no meio.
- **Shell dentro de um container** com o socket do Docker montado — o que as
  plataformas remotas fazem. Rejeitada por um motivo específico deste projeto: os
  caminhos de `-v` são resolvidos pelo daemon no **host**, não dentro do container
  onde o leitor digitou. Um arquivo criado no shell não está no caminho que o
  `docker run` procura. Isso ensinaria monta de volume errado logo no primeiro
  Cenário, cujo tema é exatamente esse.
- **Executor de comandos sem PTY** (um input, um `exec`, saída como log). Continua
  sendo a porta de entrada se algum dia quisermos Asserções sobre método — é
  aditivo e não invalida esta decisão. Rejeitado agora por não ter estado de shell
  entre comandos e não suportar nada interativo.

## Consequências

- **A plataforma nunca vê os comandos digitados.** Só o estado do ambiente. Isso é
  uma restrição dura no vocabulário de Asserções: toda Asserção precisa ser sobre o
  estado do mundo ("o container está no ar", "a porta responde"), nunca sobre
  histórico ("você digitou `docker run -p`"). Nenhuma Asserção pode depender de
  observar o processo de resolução — só o resultado.
- Se algum dia o projeto virar multiusuário ou remoto, esta decisão cai junto com
  boa parte da arquitetura. Ela é consequência direta de haver um único usuário
  local, não uma preferência de UX.

## Nota de 2026-08-21 — o Cenário cujo ambiente é um container

A Trilha Linux coloca o leitor **dentro** de um container, e ele entra digitando
`docker exec -it learning-infra-linux bash` no terminal dele. Isso não reabre a
alternativa rejeitada acima, e a diferença importa.

O que foi rejeitado era a plataforma **fornecer** o shell como interface — um `xterm.js`
no navegador falando com um shell em container, com o socket do Docker montado. O que a
Trilha Linux faz é o oposto: o leitor usa o terminal dele, o shell dele e o editor dele,
e a plataforma continua sem ver comando nenhum. Nenhuma Asserção da Trilha Linux observa
o que foi digitado; todas observam estado — arquivo, permissão, processo, unit, porta.

O motivo específico da rejeição também não se aplica: o container da Trilha Linux **não**
monta o socket do Docker. O perigo original era o leitor rodar `docker run -v` de dentro
do container e o caminho ser resolvido pelo daemon no host, ensinando monta de volume
errada. Aqui não há Docker lá dentro para ser usado errado.

Fica, porém, uma regra que decorre disto: **nenhum Cenário da Trilha Linux pode pedir
que o leitor rode comandos de Docker de dentro do container.** Docker é assunto da Trilha
seguinte, e é lá que o leitor aprende por que o caminho do `-v` pertence ao host.
