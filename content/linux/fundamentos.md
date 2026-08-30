# Fundamentos do Linux

Uma máquina Linux é **um kernel e um monte de processos pedindo coisas a ele**. Quase
tudo que parece difícil aqui é uma de quatro perguntas: quem está pedindo, com que
identidade, sobre qual recurso, e quem garante que continue pedindo. Decorar comandos
resolve o dia; entender as quatro resolve o plantão, porque o comando que você ainda não
conhece cai numa delas.

Esta leitura prepara os Cenários práticos. Ela não substitui o terminal: ao terminar,
faça o Questionário para descobrir o que revisar e siga para os laboratórios mesmo que
ainda não tenha atingido 80%. A recomendação não é um bloqueio.

## O problema: a máquina que você nunca viu

A **Aurora** é uma editora pequena. O site do catálogo, a entrada de pedidos e o relatório
que roda de madrugada vivem no mesmo servidor Linux, montado anos atrás por alguém que já
não trabalha lá. Não há documentação nem um segundo servidor para comparar: existe um
endereço, um usuário e a expectativa de que tudo continue funcionando.

A máquina que sustenta um negócio raramente foi desenhada — ela cresceu, um pacote e uma
permissão às pressas de cada vez. Funciona até o dia em que não funciona, e aí não há para
onde correr: operar sem enxergar por dentro é dívida com data de vencimento. O que este
artigo constrói é a máquina por dentro, das fronteiras do kernel ao serviço que sobe
sozinho depois de um reboot.

## Kernel e espaço de usuário

O kernel é o programa que fala com o hardware — CPU, memória, discos, placas de rede — e é
o único que faz isso. Todo o resto, do shell ao servidor web, roda no **espaço de usuário**
e não toca em hardware nenhum.

A ponte entre as duas metades é a **chamada de sistema**. Um programa que quer ler um
arquivo não lê: pede ao kernel que leia. Quer abrir uma conexão, pede; quer criar outro
processo, pede. É a única porta, e por isso é o lugar certo para impor limite, permissão e
contabilidade — tudo passa por ali.

```diagrama
tipo: camadas
visual: fronteiras-kernel
titulo: Duas metades e uma porta
camadas:
  - titulo: espaço de usuário
    detalhe: os processos que você enxerga
  - titulo: chamada de sistema
    detalhe: a única porta entre as metades
    tom: alerta
  - titulo: kernel
    detalhe: quem de fato fala com o hardware
legenda: Nenhum programa comum fala com o hardware; todos pedem, e o kernel atende.
```

Isso explica uma frase que você vai ouvir muito: "o problema é do sistema operacional".
Quase nunca é — na maioria esmagadora dos casos o kernel atendeu exatamente o que foi
pedido, e o pedido é que estava errado. Diagnosticar Linux é descobrir qual pedido foi
feito e qual resposta voltou.

## Tudo é arquivo

O Linux herda do Unix uma decisão radical: quase todo recurso é exposto como arquivo. Um
disco é um arquivo, uma conexão aberta é um arquivo, um processo em execução é um
diretório de arquivos. Você lê todos com as mesmas ferramentas.

Os lugares seguem uma convenção — o **FHS** — igual em qualquer distribuição:

- `/etc` — configuração do sistema, em texto, versionável;
- `/var` — o que cresce com o uso: logs, filas, dados de serviço;
- `/usr` — os programas instalados pela distribuição;
- `/proc` e `/sys` — não são disco: são o kernel se expondo como arquivo;
- `/home` — os dados das pessoas.

Um arquivo, para o sistema, não é o nome. O nome é uma entrada de diretório que aponta
para um **inode**, e é o inode que carrega conteúdo, tamanho, dono e permissões — um mesmo
inode pode ter vários nomes. E o espaço só é devolvido quando o último nome é apagado
**e** o último processo que tinha o arquivo aberto fecha o **descritor**.

Daí a armadilha clássica: `df` diz que o disco está cheio e `du` soma muito menos. Alguém
apagou um log enorme enquanto o serviço ainda escrevia nele. O nome sumiu — `du` percorre
nomes e não vê mais nada — mas o inode continua vivo pelo descritor aberto, e o espaço,
ocupado. Reiniciar o serviço resolve; entender por que resolve é a matéria.

## Processo: quem está pedindo

Um processo é um programa em execução com identidade própria: um número (**PID**), um pai
(**PPID**), um usuário, um diretório atual, um ambiente e uma tabela de arquivos abertos.

Processos nascem de um jeito só: um processo existente se duplica (`fork`) e a cópia
substitui o próprio conteúdo pelo programa desejado (`exec`). Não há criação espontânea —
por isso todo processo tem um pai, e seguir os PPIDs para cima sempre chega ao **PID 1**.

```diagrama
tipo: fluxo
visual: arvore-de-processos
titulo: A linhagem de um processo
passos:
  - titulo: PID 1
    detalhe: o init, pai de todos
    tom: destaque
  - titulo: o shell
    detalhe: filho do PID 1, a sua sessão
  - titulo: o comando que você roda
    detalhe: neto do PID 1, morre quando termina
legenda: Todo PPID sobe até o 1; nenhum processo aparece do nada.
```

O PID 1 tem uma obrigação que nenhum outro tem. Um processo que morre não some na hora:
vira **zumbi**, uma entrada guardando o código de saída até o pai colhê-la. Se o pai morreu
antes, o filho fica **órfão** e é adotado pelo PID 1, que precisa colher esses códigos —
senão a tabela entope. Um init de verdade faz isso; um programa comum promovido a PID 1,
não.

A comunicação com um processo vivo é por **sinal**. `SIGTERM`, o que o `kill` envia por
padrão, é um **pedido**: o processo pode tratá-lo, adiar ou ignorar, e é assim que serviços
terminam de gravar antes de sair. `SIGKILL` não é pedido — quem executa é o kernel, e o
processo não é consultado. Por isso funciona sempre, e por isso é o último recurso: o que
estava pela metade fica pela metade.

## Identidade e permissão

Cada processo carrega um usuário e um conjunto de grupos. Cada arquivo carrega um dono, um
grupo e um **modo** de nove bits, em três conjuntos de três — ler, escrever, executar —
para o dono, o grupo e todos os outros. O octal de `chmod 640` é isso: `6` é ler e
escrever, `4` é só ler, `0` é nada.

```diagrama
tipo: comparacao
visual: permissao-octal
titulo: Três conjuntos de permissão sobre o mesmo arquivo
colunas:
  - titulo: dono
    detalhe: quem criou o arquivo
    tom: destaque
    itens:
      - ler, escrever e executar
      - o único que pode mudar o modo
  - titulo: grupo
    detalhe: os colegas do mesmo grupo
    itens:
      - só o que o grupo declara
  - titulo: outros
    detalhe: todo o resto do sistema
    itens:
      - o mais restrito de todos
legenda: O sistema aplica o primeiro conjunto que casa com a identidade de quem pede.
```

O sistema não soma os três conjuntos: escolhe **um**. E a permissão é conferida ao
**processo**, não à pessoa: o processo nasce com os grupos que a sessão tinha quando foi
criado, então acrescentar alguém a um grupo não muda o shell que já estava aberto. `id -G`
mostra os grupos que o processo realmente carrega, e é essa lista que o sistema consulta.

Diretórios têm regra própria: `x` não é executar, é **atravessar**; e o `setgid` num
diretório faz todo arquivo criado ali herdar o grupo dele — é assim que se monta uma pasta
compartilhada que continua compartilhada. Root, por fim, não é um usuário poderoso: é o
usuário para quem o kernel **não faz a checagem**. O objetivo da segurança não é dar poder
a quem precisa; é evitar que a checagem seja pulada.

## O shell

O shell é um programa que lê uma linha, decide o que ela significa e cria processos. Não é
o sistema — é um cliente do sistema como qualquer outro.

Todo processo nasce com três canais: entrada padrão, **saída padrão** e **saída de erro**.
A separação entre as duas últimas é deliberada — resultado numa, relato de erro na outra —
e é o que permite encadear comandos sem que um aviso contamine os dados. O pipe (`|`) liga
a saída de um à entrada do outro; o redirecionamento (`>`, `2>`) separa os canais.

Todo processo também devolve um **código de saída**: `0` é sucesso, qualquer outro número é
falha. É esse número, não o texto impresso, que scripts e o systemd usam para decidir o
passo seguinte. Um comando que imprime "erro" e sai com `0` mente para tudo que estiver
automatizado em cima dele.

E o processo nasce com um **ambiente** herdado de quem o criou, do qual `$PATH` é a peça
mais importante. Ou seja: "o comando funciona" nunca é frase completa — a completa é
*funciona no ambiente da minha sessão*. Uma tarefa agendada roda com ambiente mínimo e
herda quase nada disso, e é aí que o comando testado à mão falha às três da manhã.

## Pacote e distribuição

Um programa no Linux quase nunca é instalado baixando um arquivo do site do projeto: vem
de um **pacote**, e o pacote vem de um **repositório** mantido pela distribuição. O
gerenciador — `apt` no Debian e no Ubuntu — resolve dependências, verifica a assinatura,
instala nos lugares do FHS e registra o que colocou onde.

A consequência que mais surpreende é a versão. O site anuncia a 3.2, o `apt` entrega a
3.0, e não há nada de errado: a distribuição não promete novidade, promete
**estabilidade**. Ela escolhe uma versão, testa, empacota e congela para todo o ciclo de
vida do lançamento, aplicando por cima apenas correções de segurança. Um programa vindo de
pacote está no inventário e pode ser auditado; um binário copiado à mão para dentro do
`$PATH` funciona igual, não está no inventário de ninguém, e é esse que ninguém lembra de
atualizar quando sai uma falha de segurança.

## Serviço e init

Um processo comum morre quando você fecha a sessão. Um **serviço** precisa sobreviver a
isso, subir sozinho depois de um reboot e ser reiniciado se cair. Quem cuida disso é o
init — nas distribuições atuais, o **systemd**, que é o PID 1.

O systemd descreve tudo como **unit**. Um serviço é uma unit `.service`: `ExecStart` diz
qual comando roda, `User` diz com que identidade, `Restart` diz o que fazer quando o
processo sai, e a seção `[Install]` diz a que alvo a unit se liga. Um **target** é um
conjunto de units — `multi-user.target` é o estado normal de um servidor. O systemd não
relê o disco sozinho: depois de editar uma unit, é o `daemon-reload` que faz o arquivo
novo existir para ele.

E há a distinção que custa um plantão a quem não a conhece. `systemctl start` responde
"agora": sobe o serviço neste momento. `systemctl enable` responde "sempre": grava o
vínculo declarado no `[Install]`, ligando a unit ao target de boot. São independentes. Um
serviço iniciado só com `start` funciona perfeitamente — até o próximo reboot, quando o
systemd sequer olha para ele. "Está rodando" e "vai voltar sozinho" são duas afirmações
diferentes, e só a segunda é uma configuração.

## Log

Sob o systemd, a saída de um serviço não precisa ir para arquivo nenhum: o que o processo
escreve nas saídas padrão e de erro é capturado pelo **journald**, com timestamp, unit de
origem, PID e **prioridade** — de `emerg` a `debug`, o que permite filtrar ruído sem
apagar nada.

A propriedade que faz o journal valer mais que um log da aplicação é esta: ele **sobrevive
ao processo**. Quando uma unit morre, o systemd registra ali o código de saída, o sinal
recebido e quantas vezes tentou reiniciar — frequentemente o único registro que existe,
porque a aplicação que morreu durante a inicialização pode nem ter aberto o log dela.

Consultar é por unit, prioridade e janela de tempo. Dois detalhes operacionais: o journal
pode ser volátil e sumir no reboot, então a primeira pergunta de uma investigação é se o
que você procura ainda existe; e log cresce, então sem rotação o registro que explica o
incidente vira a causa do próximo.

## Rede no host

Do ponto de vista de uma máquina, rede são três perguntas. Que endereços eu tenho — cada
**interface** carrega os seus. Para onde mando um pacote que não é para mim — a tabela de
**rotas** responde, e a rota padrão vale quando nenhuma outra casa. E como transformo um
nome num endereço — a **resolução de nome**, que é configuração local e falha de um jeito
que parece falha de rede sem ser.

A quarta peça é a **porta em escuta**: um processo pede ao kernel o direito de receber
conexões numa porta, e esse direito é **exclusivo**. O primeiro que pede fica com ele; o
segundo recebe *address already in use* e não sobe.

Por isso o diagnóstico de "o serviço não sobe" começa listando quem está escutando o quê,
com qual PID. O erro do serviço novo é a evidência do antigo: quase sempre há uma
instância anterior que nunca morreu, ou um segundo serviço apontado para a mesma porta. A
pergunta não é "por que não subiu", é "quem já está lá".

## Namespace e cgroup: o container

Falta uma pergunta, e ela é a ponte para a próxima Trilha: **e se dois conjuntos de
processos não pudessem se enxergar?**

O kernel responde com dois mecanismos. O **namespace** separa o que um processo consegue
ver: com um namespace de PID próprio, um grupo de processos enxerga a própria numeração e
não vê os de fora; com um de rede, tem interfaces e portas próprias; com um de sistema de
arquivos, a própria raiz. O **cgroup** separa o que ele consegue consumir: CPU, memória,
entrada e saída, com limite e contabilidade.

```diagrama
tipo: comparacao
visual: fronteiras-runtime
titulo: O que separa um container de uma máquina virtual
colunas:
  - titulo: máquina virtual
    itens:
      - virtualiza o hardware
      - inicia um kernel próprio
  - titulo: container
    tom: destaque
    itens:
      - isola processos
      - namespace e cgroup sobre o mesmo kernel
legenda: A VM cria uma máquina; o container cria uma vista.
```

Um container é isso: não é máquina pequena nem virtualização, são processos comuns do
mesmo kernel dentro de namespaces e sob cgroups. Tudo que este artigo descreveu continua
valendo lá dentro — inclusive o problema do PID 1 que precisa colher órfãos. O container
deixa de ser mágica antes mesmo de a Trilha Docker começar.

## Acesso mínimo

Como um processo faz algo que a identidade dele não permite? A resposta preguiçosa é "vira
root". A profissional é conceder o mínimo.

O `sudo` é **delegação seletiva**, não um botão de virar root: a configuração descreve quem
executa qual comando como qual usuário, e uma regra bem escrita autoriza exatamente um
comando. As regras vivem sob `/etc/sudoers.d`, e o modo `440` delas não é capricho — um
arquivo de regras que qualquer um pode escrever é um arquivo que qualquer um pode usar
para se autorizar. O `sudo` recusa permissão frouxa: a ferramenta se protege da própria
configuração.

As **capabilities** quebram os poderes do root em pedaços nomeados. Ligar numa porta abaixo
de 1024 é uma capability específica, concedível a um binário sem que ele ganhe nenhum
outro poder — foi assim que servidores web deixaram de precisar rodar como root.

E é por isso que `chmod 777` é a resposta errada para a pergunta certa. Quando um serviço
falha por permissão, o problema quase nunca é "permissão de menos": é **identidade
errada**, e o processo deveria rodar como um usuário que já pode o que precisa. `777`
concede a todo o sistema o que um só precisava.

## O ambiente desta Trilha

Os Cenários rodam num Ubuntu com systemd como PID 1, dentro de um container que o Compose
sobe. Você entra nele com uma linha:

```bash
docker exec -it learning-infra-linux bash
```

Trate essa linha como uma incantação com prazo de validade: ela abre uma sessão interativa
dentro do container que já está no ar. A Trilha Docker explica cada pedaço dela, e a seção
anterior já entregou o essencial — não há máquina nova, há uma vista diferente sobre o
mesmo kernel.

E o aviso de fidelidade que as outras Trilhas fazem vale aqui, porque saber o que uma
conclusão **não** prova é parte do ofício:

| Fidelidade | O que a Trilha usa | O que a conclusão prova |
|---|---|---|
| Real | processo, sinal, usuário, grupo, permissão, pacote, systemd, journald, rota, porta | tudo isso é o Linux de verdade, sem emulação |
| Compartilhado | kernel, `/proc/sys`, relógio | são da VM do Docker Desktop; o Cenário observa, não altera |
| Ausente | boot, initramfs, módulo, disco, `dmesg`, firewall do host | não são ensináveis aqui e entram só como mapa do território |

A linha do meio merece atenção: o kernel é da VM do Docker Desktop, e alterá-lo não é
assunto de Cenário porque o efeito não seria seu.

## Da teoria aos Cenários

Os Cenários acompanham a Aurora do primeiro acesso ao primeiro plantão, em quatro atos:

- **Ato I — O chão (Cenários 01 a 03):** entrar na máquina, descobrir onde as coisas
  moram, entender dono e permissão, e usar texto como interface;
- **Ato II — Processos e pacotes (Cenários 04 a 07):** quem está consumindo a máquina, o
  programa que não morre, de onde vem um programa e o disco que encheu;
- **Ato III — Serviços e log (Cenários 08 a 11):** um programa vira serviço, o serviço que
  não sobe, o trabalho das três da manhã e o journal como fonte de verdade;
- **Ato IV — Rede, acesso e plantão (Cenários 12 a 14):** onde este pacote vai dar, acesso
  mínimo, e um plantão com várias falhas ao mesmo tempo e nenhuma dica no texto.

Um contrato atravessa os quatro atos: quando um Cenário pede um serviço no ar, a
Verificação exige que ele esteja **habilitado**, e não apenas rodando porque você o iniciou
à mão. Nesta Trilha, resultado certo pelo caminho errado não conta.

Use o Questionário como recuperação ativa: responda sem procurar no texto, leia o feedback
e volte apenas às seções indicadas. Depois abra o terminal — é lá que a Aurora está
esperando.
