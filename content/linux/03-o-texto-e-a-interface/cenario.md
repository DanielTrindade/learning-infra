---
id: linux/03-o-texto-e-a-interface
titulo: O texto é a interface
dificuldade: guiado
projetoCompose: linux-03
containerLinux: learning-infra-linux
---
# O texto é a interface

Chegou uma reclamação por telefone: "nossos pedidos estão falhando". Sem detalhe, sem
horário, sem nome.

Não existe painel nesta máquina. Não existe alerta, não existe gráfico, não existe uma
tela para abrir. Existe um arquivo de texto com 2160 linhas e um dia inteiro de operação
dentro dele.

Isso é suficiente. Ao fim desta página, cinco comandos encadeados vão te levar da
reclamação vaga ao nome do cliente afetado e à hora exata em que o problema acontece.

Entre na máquina:

```powershell
docker exec -it learning-infra-linux bash
```

## Uma linha de cada vez

Antes de processar 2160 linhas, entenda uma:

```bash
head -1 /var/log/aurora/pedidos.log
wc -l /var/log/aurora/pedidos.log
```

```
2026-08-14T00:00:07 pedido=4001 cliente=livraria-do-porto status=200 ms=280
```

Cinco campos separados por espaço:

| Posição | Campo | O que é |
|---:|---|---|
| 1 | `2026-08-14T00:00:07` | quando |
| 2 | `pedido=4001` | qual pedido |
| 3 | `cliente=livraria-do-porto` | de quem |
| 4 | `status=200` | como terminou |
| 5 | `ms=280` | quanto demorou |

O formato é o contrato. É por ele ser sempre igual — mesmo separador, mesma ordem — que
dá para recortar por posição daqui a duas seções. Um log sem formato fixo é um log que só
serve para ler com os olhos.

E `status=200` significa que deu certo; `500` significa que o servidor falhou.

## Filtrar

O `grep` mostra só as linhas que casam com um padrão:

```bash
grep "status=500" /var/log/aurora/pedidos.log | head -3
grep -c "status=500" /var/log/aurora/pedidos.log
```

O `-c` conta em vez de mostrar: **87**. Oitenta e sete falhas em 2160 pedidos. A
reclamação é real.

Você acabou de usar o `|`, o **pipe**, e ele merece um parágrafo porque é a ideia central
desta página. O pipe liga a saída de um programa à entrada do próximo. O `grep` não sabe
que está falando com o `head`; o `head` não sabe de onde vem o texto. Cada um faz uma
coisa só e não presume nada sobre o vizinho.

É por isso que o Linux não tem um comando para "analisar log": ele tem peças pequenas que
você combina para a pergunta que **você** tem hoje.

## Recortar, ordenar, contar

De quem são as 87 falhas? Construa a resposta um estágio por vez, observando a saída mudar
de forma a cada acréscimo.

Primeiro recorte o campo do cliente. O `cut` corta por separador: `-d" "` diz que o
separador é o espaço, `-f3` pede o terceiro campo:

```bash
grep "status=500" /var/log/aurora/pedidos.log | cut -d" " -f3 | head -3
```

Vem `cliente=leitura-norte` e companhia. Corte de novo, agora no `=`, para ficar só com o
nome:

```bash
grep "status=500" /var/log/aurora/pedidos.log | cut -d" " -f3 | cut -d= -f2 | head -3
```

E agora conte cada nome:

```bash
grep "status=500" /var/log/aurora/pedidos.log | cut -d" " -f3 | cut -d= -f2 | sort | uniq -c | sort -rn
```

```
     37 livraria-do-porto
     15 banca-central
     14 papelaria-sul
     13 leitura-norte
      8 sebo-da-praca
```

**Por que o `sort` antes do `uniq`.** O `uniq` só enxerga repetições que estejam
**adjacentes** — ele compara cada linha com a anterior e nada mais. Sem ordenar antes, ele
conta cada bloquinho separadamente e devolve o mesmo nome várias vezes. Rode sem o `sort`
para ver acontecer:

```bash
grep "status=500" /var/log/aurora/pedidos.log | cut -d" " -f3 | cut -d= -f2 | uniq -c | head
```

O último `sort -rn` ordena por número (`-n`) e do maior para o menor (`-r`), que é o que
põe a resposta na primeira linha.

E a resposta está lá: **37 das 87 falhas vêm de um cliente só**. Nenhum outro passa de 15.

### A que horas

Um cliente concentrando quase metade dos erros não é acaso. Recorte a hora — desta vez o
separador é o `T` que divide data e horário:

```bash
grep "status=500" /var/log/aurora/pedidos.log | grep livraria-do-porto | cut -d"T" -f2 | cut -d: -f1 | sort -u
```

```
02
03
04
```

Três horas da madrugada, e só. Fora dessa janela a livraria-do-porto não falha nunca.

Guarde esse achado: alguma coisa acontece nesta máquina de madrugada. Você vai descobrir o
que é no Cenário 10.

## Dois fluxos, não um

Você vai querer guardar esse resultado num arquivo. Antes disso, um comportamento que
surpreende quem está começando. Peça ao `cat` dois arquivos, sendo que um não existe:

```bash
cat /var/log/aurora/pedidos.log /var/log/aurora/vendas.log > /srv/tentativa.txt
```

O `>` deveria mandar tudo para o arquivo. Mas a mensagem aparece na tela assim mesmo:

```
cat: /var/log/aurora/vendas.log: No such file or directory
```

Porque **não existe "a saída" de um programa; existem duas**. O descritor `1` é a
**stdout** — o resultado. O descritor `2` é a **stderr** — o que deu errado. O `>` sozinho
é atalho para `1>`, e por isso só desvia a primeira.

A separação é deliberada e útil: ela deixa você encadear o resultado num pipe sem que
mensagens de erro contaminem o processamento.

Existe ainda um terceiro canal, que não é texto — o **código de saída**:

```bash
echo $?
```

Responde `1`. Zero significa que deu certo, qualquer outro número significa que não. Você
não costuma olhar para ele à mão, mas é por ele que o systemd vai decidir se um serviço
morreu, no Cenário 09.

Agora separe de verdade:

```bash
cat /var/log/aurora/pedidos.log /var/log/aurora/vendas.log > /srv/tentativa.txt 2> /srv/erros.txt
cat /srv/erros.txt
```

Nada apareceu na tela: o `2>` levou o erro para o arquivo dele. Fora do exercício isso
importa muito — um script que joga os dois fluxos no mesmo lugar entrega um relatório com
mensagem de erro no meio dos dados, e quem for lê-lo não vai saber de onde ela veio.

## Escreva o relatório

Junte tudo e guarde. A barra invertida no fim da linha continua o comando na linha
seguinte, só para o texto caber na tela:

```bash
grep "status=500" /var/log/aurora/pedidos.log \
  | cut -d" " -f3 | cut -d= -f2 | sort | uniq -c | sort -rn \
  > /srv/relatorio-500.txt
cat /srv/relatorio-500.txt
```

E acrescente a descoberta ao inventário que você escreveu no Cenário 01:

```bash
echo "- Erros de pedido concentrados na livraria-do-porto, entre 2h e 4h" >> /srv/inventario.md
cat /srv/inventario.md
```

Repare no **`>>`**. Ele acrescenta ao fim do arquivo; o `>` de um sinal só teria **apagado
o inventário inteiro** e deixado apenas essa linha. É um caractere de diferença e não há
desfazer. Vale conferir duas vezes toda vez que você redireciona para um arquivo que já
tem conteúdo.

## Verificação

A Verificação cobra o relatório com a linha certa no topo, e cobra o `/srv/erros.txt`
contendo a mensagem de erro — que é a única forma honesta de provar que você separou os
dois fluxos em vez de jogar tudo num arquivo só.

## Fim do Ato I

Em três Cenários você entrou numa máquina que nunca tinha visto, descobriu onde as coisas
moram, de quem elas são, e extraiu de um arquivo de texto uma resposta que ninguém tinha.

Tudo isso foi sobre coisas **paradas** no disco. O que você ainda não sabe é quem está
**rodando** nesta máquina agora — e é essa a pergunta que abre o Ato II.
