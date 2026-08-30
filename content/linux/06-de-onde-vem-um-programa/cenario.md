---
id: linux/06-de-onde-vem-um-programa
titulo: De onde vem um programa
dificuldade: assistido
projetoCompose: linux-06
containerLinux: learning-infra-linux
---
# De onde vem um programa

A Aurora fechou com um parceiro que vai mandar os pedidos em JSON, e o relatório noturno
vai precisar ler esse formato. Falta uma ferramenta na máquina.

Você já usou `grep`, `ps`, `stat`, `df` e mais uma dúzia de comandos sem nunca perguntar de
onde eles vieram. A pergunta deste Cenário não é "como instalo" — é **o que exatamente
acontece quando eu instalo**, e por que a versão que chega quase nunca é a mais nova.

Entre na máquina:

```powershell
docker exec -it learning-infra-linux bash
```

## Qual binário o shell escolhe

Comece pelo que já está aí:

```bash
which python3
type python3
echo $PATH
```

O `$PATH` responde:

```
/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
```

O shell não varre o disco à sua procura. Ele percorre **essa lista, na ordem**, e usa o
primeiro que encontrar.

Isso explica o Cenário 04 em retrospecto: você chamou `relatorio-aurora` sem caminho
nenhum e funcionou porque `/usr/local/bin` está na lista. E repare que ele vem **antes** de
`/usr/bin` — é assim que se substitui um programa do sistema por uma versão própria, e
também é assim que se quebra uma máquina sem querer.

O `which` e o `type` parecem a mesma coisa e não são:

```bash
type cd
which cd
```

O `cd` é embutido no shell: não existe arquivo nenhum para ele. O `type` sabe disso e
responde `cd is a shell builtin`; o `which` procura arquivos e não acha nada. Quando as
duas respostas divergirem, **acredite no `type`** — é ele que responde o que o shell
realmente vai executar.

## O índice vem antes do pacote

Pergunte pelo `jq` **antes** de qualquer outra coisa:

```bash
apt-cache policy jq
```

A resposta é **nada**. Nenhuma linha, nenhum erro.

Não é que o `jq` não exista: é que esta máquina não tem ideia de que ele existe. A imagem
foi montada sem guardar o índice dos repositórios — prática comum, porque esse índice é
grande e envelhece rápido. Sem índice, o `apt` só conhece o que já está instalado aqui.

É exatamente assim que nasce o erro mais comum de quem está começando: `apt` diz que o
pacote não existe, a pessoa conclui que precisa baixar do site do projeto, e a máquina
ganha um programa que ninguém gerencia.

Busque o índice de verdade:

```bash
apt-get update
apt-cache policy jq
```

Agora sim:

```
jq:
  Installed: (none)
  Candidate: 1.8.1-4ubuntu2
```

Duas coisas para fixar. O `apt-get update` **não instala nada** — ele baixa a lista do que
existe nos repositórios configurados, cerca de 26 MB, e leva alguns segundos. E pular esse
passo é a causa mais comum de dois erros: "esse pacote não existe", quando ele existe, e
instalar uma versão velha sem perceber.

Um **repositório** é um servidor com pacotes e um índice assinado. Quais a máquina usa está
escrito em disco, e você pode ler:

```bash
ls /etc/apt/sources.list.d/
```

## Instale e veja onde as coisas foram parar

```bash
apt-get install -y jq
jq --version
dpkg-query -W -f='${Package} ${Version}\n' jq
which jq
```

As respostas: `jq-1.8.1`, depois `jq 1.8.1-4ubuntu2`, depois `/usr/bin/jq`.

**Compare os dois números**, porque é aqui que mora a lição.

O programa se apresenta como `jq-1.8.1` — a versão que o projeto original publicou. O
pacote se chama `1.8.1-4ubuntu2`. Esse sufixo é da **Ubuntu**: alguém pegou aquele código,
compilou para esta distribuição, aplicou as correções que a distribuição mantém e montou o
pacote. Um número descreve o software; o outro descreve o **empacotamento** dele.

E daí sai a resposta para a pergunta que todo mundo faz mais cedo ou mais tarde — por que o
`apt` me deu uma versão mais antiga que a do site do projeto?

Porque é **de propósito**. A distribuição congela as versões no lançamento e, dali em
diante, traz só correção de segurança para elas. Você troca novidade por previsibilidade:
uma máquina que ninguém olha há seis meses continua se comportando como no dia em que foi
montada. Instalar direto do site do projeto é uma escolha legítima e desfaz esse acordo —
e o que não pode é fazer a troca sem saber que fez.

Veja o que o pacote colocou no disco:

```bash
dpkg -L jq | head -6
dpkg -l jq
```

A última linha do `dpkg -l`:

```
ii  jq  1.8.1-4ubuntu2  amd64  lightweight and flexible command-line JSON processor
```

O `ii` são dois estados, não um. O primeiro `i` é o estado **desejado** (*install*); o
segundo é o estado **real** (*installed*). Quando os dois divergem você tem um pacote pela
metade — e essa é a primeira coisa a olhar quando algo "está instalado" e não funciona.

Experimente a ferramenta, que é o motivo de tudo isso:

```bash
echo '{"cliente":"livraria-do-porto","status":500}' | jq .cliente
```

## Pacote contra arquivo largado no disco

Um binário copiado para `/usr/local/bin` roda igualzinho. Não é a mesma coisa:

```bash
dpkg -S /usr/bin/jq
dpkg -S /etc/aurora/catalogo.conf
```

O primeiro responde `jq: /usr/bin/jq`. O segundo diz que **nenhum pacote** é dono daquele
caminho — a configuração da Aurora foi posta ali à mão, anos atrás, por alguém que não
trabalha mais lá. Você já sabia disso desde o Cenário 01; agora tem como provar.

A diferença é o que você ganha com a primeira forma: o gerenciador sabe de onde veio cada
arquivo, sabe removê-lo sem deixar resto, sabe atualizar tudo de uma vez e sabe dizer se
alguém mexeu. Um arquivo largado no disco não tem nada disso, e em seis meses ninguém
lembra quem o colocou lá nem de qual versão ele saiu.

E é por isso que a pergunta "como leio a documentação disto?" também tem resposta local:

```bash
jq --help | head -5
dpkg -L jq | grep -i doc
```

## Verificação

A Verificação cobra que o `jq` responda **e** que o `dpkg` o reconheça como instalado.

A segunda é a que tem conteúdo. Um binário baixado à mão e jogado em `/usr/local/bin`
também responderia `--version` — e passaria numa checagem ingênua. Perguntar ao banco de
dados do `dpkg` é a diferença entre o programa **existir** e o programa ser **gerenciado**.

Falta um último problema antes de a Aurora poder ter serviços de verdade: o que fazer
quando a máquina diz que não tem mais espaço. É o Cenário 07, e ele é mais estranho do que
parece.
