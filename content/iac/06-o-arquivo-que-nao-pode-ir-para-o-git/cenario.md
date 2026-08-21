---
id: iac/06-o-arquivo-que-nao-pode-ir-para-o-git
titulo: O arquivo que não pode ir para o Git
dificuldade: assistido
terraform: true
containers: [mirante-painel]
---
# O arquivo que não pode ir para o Git

A auditoria de segurança está marcada para a semana que vem. A Mirante recebeu o aviso
como se recebe uma data de prova: com antecedência e sem espaço para remarcar. Uma das
perguntas do roteiro é a mais difícil de responder com honestidade — **onde estão os
segredos da infraestrutura?** Hoje a resposta do time seria "não sabemos", e este Cenário
é sobre começar a saber.

Este Cenário é `Assistido`: você recebe o objetivo, as pistas e a forma dos comandos —
parte do caminho você descobre sozinho.

## Aplique e olhe a saída

O diretório deste Cenário chega completo e aplicável: um `main.tf` que sobe o painel
administrativo da Mirante com uma senha de exemplo no ambiente do container. Nada de
produção — a senha é literalmente `senha-de-exemplo-nao-use`, e isso é de propósito: tudo
o que acontecer com ela aqui aconteceria com uma senha real. Coloque a infraestrutura no
ar:

```powershell
terraform init
terraform apply
```

Confira no navegador: <http://localhost:8076>.

Agora olhe a saída do `apply` até o fim. O arquivo declara uma saída chamada
`senha_em_uso` — e o Terraform a imprime assim:

```
Outputs:

senha_em_uso = <sensitive>
```

O Terraform se recusou a mostrar o valor. Antes de continuar, responda por conta própria:
isso significa que a senha está protegida? Anote a resposta. A próxima seção vai
confrontá-la com os fatos.

## Abra o state

O Terraform guarda o retrato do que está no ar num arquivo chamado `terraform.tfstate`.
Abra esse retrato pelas portas oficiais — primeiro a lista, depois um recurso:

```powershell
terraform state list
terraform state show docker_container.web
```

A segunda saída mostra o container `mirante-painel` inteiro — e, lá dentro, o atributo
`env` aparece assim:

```
env = (sensitive value)
```

De novo. O mesmo filtro da saída do `apply`, agora dentro do `state show` — a grafia muda
entre versões (`<sensitive>` numa hora, `(sensitive value)` na outra), o significado é o
mesmo. Nomeie o que acabou de acontecer, porque este é o conceito que o Cenário existe
para ensinar: `sensitive` é uma proteção de **exibição**. Ela esconde o valor de todas as
telas e saídas do Terraform — e, como você acabou de ver, ela funciona.

Mas há um detalhe que nenhuma tela revela: o state **não é uma tela**. É um arquivo,
`terraform.tfstate`, gravado em disco como o Terraform registra a verdade sobre o mundo
real — e a verdade, ele guarda como recebeu. O filtro de exibição protege as vitrines;
o arquivo não pede licença para ser lido. É isso que a próxima seção prova.

## Encontre-a no arquivo

Prove com as próprias mãos. O `terraform.tfstate` é um arquivo JSON: texto puro, que
qualquer busca por texto atravessa. Procure a senha de exemplo dentro do arquivo com a
busca que você já usa para achar uma palavra num arquivo. O comando não está aqui de
propósito: a prova vale mais quando é sua.

O resultado desfaz a ilusão que as duas seções anteriores montaram: em tela, o Terraform
escondeu a senha duas vezes; no arquivo, ela está lá, em claro, exatamente como entrou.
E aí a pergunta da primeira seção volta com o arquivo aberto na frente: a senha está
protegida **para quem**? Para quem olha as saídas do Terraform, sim — o filtro de
exibição funcionou. Para quem pode ler o arquivo, não — e é esse o mundo real.

## O que sensitive realmente compra

Se `sensitive` não protege o state, o que ele compra? Ele impede o vazamento **acidental**
nos dois lugares onde segredo vaza com mais frequência: o log de pipeline e a captura de
tela. No log, porque um valor marcado como sensitive não é impresso no plano nem no apply;
na tela, porque `(sensitive value)` não aparece numa apresentação de arquitetura nem num
erro colado num ticket. É a diferença entre acidental e intencional: quem procura a senha
continua achando, mas ninguém a derruba sem querer.

E precisa ser dito sem suavizar: `sensitive` **não é criptografia**. Ele não codifica o
valor, não o esconde do state, não protege nada de quem lê o arquivo. É um filtro de
exibição que depende de uma declaração honesta — você diz que o valor é sensível e o
Terraform confia na sua palavra para não imprimi-lo. A proteção do segredo de verdade
acontece em outro lugar, e é para lá que este Cenário está caminhando.

## Proteja o que dá para proteger

O state precisa existir em disco — o Terraform não funciona sem ele. O que você pode
proteger é o que acontece com o arquivo: que ele nunca entre num repositório Git. Na raiz
do diretório de trabalho, crie um arquivo `.gitignore` com quatro linhas:

```
*.tfstate
*.tfstate.*
.terraform/
*.tfvars
```

A primeira cobre o state. A segunda cobre os backups que o Terraform faz a cada `apply` —
o `terraform.tfstate.backup` que você já deve ter visto por perto. A terceira cobre o
diretório `.terraform/`, o depósito de providers que o `init` baixa: ele é recriável e
grande, e não tem lugar num repositório. A quarta é a linha da disciplina: `*.tfvars`
cobre arquivos que podem vir a carregar segredo — uma senha lida de fora do código, por
exemplo — e entra na lista agora, mesmo quando nenhum deles tem segredo hoje. Proteção
que espera o perigo acontecer para ser escrita não é proteção, é desculpa.

Há uma exceção, e ela é de propósito. O `.terraform.lock.hcl`, que o `init` cria na
primeira execução, **vai** para o Git: ele congela as versões exatas dos providers e faz
o time inteiro — e a Verificação desta Trilha — rodar sobre as mesmas. Ignorá-lo seria
trocar reprodutibilidade por economia de uma linha. A diferença entre o que se ignora e o
que se versiona é a diferença entre dado e código: o state é dado, o lock é código. O
Cenário 10 volta a esse arquivo.

## O que isso não resolve

O `.gitignore` protege o repositório — ele não protege o notebook. A senha continua no
`terraform.tfstate`, em disco, na sua máquina pessoal, legível para qualquer coisa que
consiga ler arquivos: um backup de nuvem, um processo comprometido, um computador
perdido. Ignorar o arquivo no Git não é o mesmo que escondê-lo do mundo — é só garantir
que o mundo não o encontre por acidente num repositório.

A saída honesta para state com segredo é não guardar o state em disco nenhum: deixá-lo
num **state remoto**, num serviço com acesso restrito, onde o arquivo nunca chega a morar
na sua máquina. É o assunto do Cenário 16, o último Ato desta Trilha. Até lá, o que dá
para proteger, você protege — e o que não dá, você sabe nomear.

## Verificação

Cinco Asserções, e cada uma prova uma camada deste Cenário.

As duas primeiras conferem o mundo real: o `mirante-painel` de pé e respondendo em
<http://localhost:8076>. Sem o container, não há painel para auditar — e o resto da
Verificação nem faz sentido.

A terceira confere o entregável. Ela lê o seu `.gitignore` e só aprova quando o state
local está coberto pela regra — a descrição dela, "o state local está ignorado pelo
Git", é o objetivo da seção `Proteja o que dá para proteger`. Repare no que ela **não**
faz: não diz a resposta do exercício, não aponta a linha certa, não acusa qual padrão
falta. Ela confirma que o arquivo que protege o segredo existe e cobre o que precisa —
o entregável, não o caminho.

As duas últimas conferem origem e alinhamento. A quarta lê o state e confere que o
container `mirante-painel` nasceu do código, no endereço `docker_container.web`. A quinta
roda um `plan` e só aprova quando não há divergência entre o código, o state e o que está
no ar — o triângulo dos Fundamentos inteiro em sincronia.

Se quiser ver a terceira reprovar, remova o `.gitignore` e verifique de novo: as outras
quatro continuam passando, e só ela cai — é ela que carrega a lição deste Cenário.
