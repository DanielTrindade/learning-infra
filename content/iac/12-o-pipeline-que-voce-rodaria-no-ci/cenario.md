---
id: iac/12-o-pipeline-que-voce-rodaria-no-ci
titulo: O pipeline que você rodaria no CI
dificuldade: autonomo
terraform: true
containers: [mirante-web]
---
# O pipeline que você rodaria no CI

A Mirante tomou uma decisão que nenhum incidente motivou: ninguém mais aplica
infraestrutura da própria máquina. Cada Cenário desta Trilha até aqui terminou com uma
pessoa em frente a um terminal — e a sequência inteira, do `fmt` ao `apply`, morava na
memória de quem digitava. Isso funciona enquanto a pessoa está no terminal. Deixa de
funcionar quando a pessoa tira férias, muda de time, ou esquece um degrau numa sexta à
noite.

O que vai para o CI precisa ser outra coisa: uma sequência que qualquer pessoa consiga
auditar depois — cada degrau na ordem, o motivo de cada ordem, e o registro do que foi
aprovado antes de virar infraestrutura. Este Cenário monta essa sequência. E o passo
mais importante dela é o único que não é um comando.

Este Cenário é `Autônomo`: você recebe o objetivo e o ambiente, e descobre o caminho.
Há uma exceção combinada: a lista de degraus vem fechada, porque a lição aqui é a ordem
e o motivo — não adivinhar comandos. Montar a sequência inteira, rodá-la até o fim e
recuperá-la de uma quebra é o exercício.

O diretório de trabalho chega com o serviço `mirante-web`, na porta 8072, com a
`precondition` da faixa no lugar — e, pela primeira vez na Trilha, com o teste já
entregue em `tests/unidade.tftest.hcl`: o assunto deste Cenário é a sequência, não
escrever teste de novo. Antes do primeiro degrau, `terraform init` resolve os providers
e o lockfile.

## O objetivo

Montar e rodar, na ordem, a sequência que um runner rodaria — e terminar com um `apply`
que consumiu **um plano salvo**, não um plano recalculado. Ao final, duas afirmações são
verdadeiras ao mesmo tempo: `plano.tfplan` existe no diretório de trabalho, e o serviço
responde em <http://localhost:8072>.

Nenhuma das duas sozinha basta. Infraestrutura no ar sem o arquivo é um `apply` qualquer
— o de sempre, recalculado na hora. O arquivo sem a infraestrutura no ar é uma sequência
que começou e não chegou ao fim. O objetivo é a sequência completa, na ordem, com o
artefato do meio dela ainda presente no fim.

## Os degraus

A lista, na ordem em que um runner a executaria:

1. `terraform fmt -check -recursive` — formatação, e só
2. `terraform validate` — sintaxe e tipos, sem falar com provider
3. `terraform test` — comportamento, contra o provider de verdade
4. `terraform plan -out=plano.tfplan` — o plano vira artefato
5. **revisão humana** do plano salvo — o degrau que não é um comando
6. `terraform apply plano.tfplan` — o apply consome o artefato

Antes de rodar, justifique para si mesmo, degrau a degrau, por que cada um vem antes do
seguinte. A ordem não é cronológica — é econômica. Os três primeiros são baratos **de
propósito**: cada um elimina uma classe de erro no menor preço em que essa classe podia
morrer, para que os degraus caros só rodem sobre código que sobreviveu aos baratos. O
Cenário 11 montou essa escada degrau a degrau; este Cenário a sobe sem parar — pela
primeira vez, a Trilha pede a escada inteira numa sessão só.

## O degrau que não é um comando

A revisão. O `plan -out` do degrau 4 produz um artefato binário: `plano.tfplan` não é
texto para abrir no editor. O `terraform show plano.tfplan` o torna legível — a mesma
tela do plano, agora lida a partir do arquivo. O `terraform show -json plano.tfplan` o
torna automatizável: máquina lendo plano é o que alimenta comentário de pull request,
política e auditoria.

O valor do plano salvo é um só, e convém dizer com todas as letras: **remover a janela
entre o que foi aprovado e o que foi aplicado**. Sem o artefato, o `apply` recalcula o
plano na hora, contra o mundo como ele estiver naquele momento. Entre a revisão e o
apply o mundo pode ter mudado: outro apply entrou, um drift nasceu, uma variável
resolveu diferente. O apply recalculado embute essas diferenças sem que ninguém as
revisou — e o que entra em produção deixa de ser o que passou na revisão. Com o
artefato, o apply aplica exatamente aquilo. Repare no comportamento do comando, aliás:
aplicar um plano salvo não pede confirmação. Não é atalho — é semântica. A aprovação já
aconteceu, no degrau 5.

E o degrau 5 é o único que o runner não executa. O pipeline para e espera uma pessoa,
porque o passo mais caro da sequência é justamente aquele em que a decisão muda de dono:
de quem escreveu o código para quem aprova a mudança.

## Quebre de propósito

Uma sequência só merece confiança depois de vê-la falhar no degrau certo. Chegue ao
degrau 4 com a porta proibida — acrescente a variável ao plano:

```powershell
terraform plan -out=plano.tfplan -var="porta=9090"
```

Os três primeiros degraus passam — e cada um pelo motivo certo: o `fmt -check` não
interpreta significados; o `validate` aprova `9090` porque é um número perfeitamente
tipado; o `test` usa as próprias variáveis e passa, inclusive o `run` que espera a
reprovação de `9090`. Aí o `plan` morre: a `precondition` reprova no plano, antes de
qualquer chamada ao Docker. O Terraform ainda escreve o `plano.tfplan` — mas o marca
como **com erro, não aplicável**: um `apply` contra esse artefato é recusado na hora
(`Cannot apply incomplete plan`). O degrau 6 nunca roda, o mundo real nem fica sabendo
— o pipeline falha barato, e até o artefato que escapa de um degrau vermelho nasce
travado. Leia a mensagem de erro com calma: ela nomeia o recurso e a condição que não
segurou.

Depois rode a sequência de novo, sem a variável, e leve-a até o fim: plano salvo,
revisão, apply do artefato.

## -target é para incêndio

Uma nota curta e categórica, porque o lugar dela é aqui. O `-target` estreita o plano a
um caminho — e um plano estreito não é o retrato do sistema: mudanças pendentes em
outros recursos ficam invisíveis, e a revisão do degrau 5 aprovaria um retrato que
mente. A seção `Blast radius` dos Fundamentos registrou a frase que fica: o `-target`
mente sobre o resto. Serve para apagar incêndio — um incidente, um recurso, pressa — e
cobra o preço depois: um `apply` completo assim que o fogo apagar. Por isso a lista de
degraus não o inclui: o pipeline do dia a dia revisa o sistema inteiro, não um recorte
escolhido às pressas.

## O que ficou de fora

Honestidade sobre o escopo: não há runner aqui. Nenhuma ferramenta de CI foi
configurada, nenhum gatilho de push, nenhum relatório verde num painel. A sequência é a
mesma que o runner rodaria — os degraus, a ordem e o artefato não mudam. O que muda no
CI é quem executa — uma máquina sem pessoa em frente, rodando por gatilho — e onde moram
os segredos: o que hoje vive no ambiente da sua máquina passa a morar em cofre, com
nome, dono e escopo. Quando a sequência sai da máquina de alguém, o state e as
credenciais precisam de uma casa que não seja um notebook — é exatamente o assunto do
Cenário 16.

## Verificação

Seis Asserções — e uma delas olha um artefato, não o mundo.

As duas primeiras conferem o mundo real: o container `mirante-web` de pé e respondendo
em <http://localhost:8072>.

A terceira é a nova: confere que `plano.tfplan` existe no diretório de trabalho — a
evidência de que o apply desta sequência consumiu um plano salvo, e não um plano
recalculado. Aqui vai o limite, dito de frente: a Asserção prova **presença**, não
consumo. O Terraform não deixa marca de que aplicou aquele arquivo específico; o que
separa "o arquivo existe" de "o apply consumiu o arquivo" é, ainda, a honestidade de
quem rodou a sequência. A Asserção fecha a porta que dá para fechar.

A quarta roda o `terraform test` no diretório de trabalho e só aprova com
`2 passed, 0 failed` — a etapa de teste do pipeline, verde. A quinta lê o state e
confere que o container nasceu do código, no endereço `docker_container.web`, com o
`name` `mirante-web`. A última roda um `plan` e só aprova quando não sobrou mudança
pendente: depois do apply do plano salvo, código, state e mundo real em sincronia.

O Ato III termina aqui, e o nome do ato era Confiança. Três respostas empilhadas: a
mesma configuração roda igual em qualquer máquina (Cenário 10); a infraestrutura se
testa antes de nascer (Cenário 11); a mudança que entra em produção é a mudança que foi
revisada (este). O próximo ato tira a Mirante da máquina única: primeiro o cluster,
depois a nuvem.
