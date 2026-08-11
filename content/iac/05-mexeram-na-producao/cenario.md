---
id: iac/05-mexeram-na-producao
titulo: Mexeram na produção
dificuldade: assistido
terraform: true
containers: [mirante-web]
---
# Mexeram na produção

O alerta chegou na madrugada de sábado: a Mirante fora do ar. Alguém de plantão resolveu no
braço — abriu o console e fez o que precisava ser feito — e o serviço voltou. Segunda-feira
de manhã o serviço está de pé, os clientes não perceberam nada, e ninguém sabe o que foi
feito. Não sobrou anotação, não sobrou comando: a correção existe no mundo real e não existe
em lugar nenhum mais.

Este Cenário é sobre descobrir e sobre decidir para onde a correção vai. Descobrir, porque
existe uma distância entre o que o código descreve e o que o mundo real faz — e o `plan` é a
régua que a mede. Decidir, porque uma correção pode morar em dois lugares, e só um deles dura.

## Aplique e confira

O diretório deste Cenário chega com o `main.tf` completo e aplicável: a imagem
`nginx:1.27-alpine` e o container `mirante-web` publicado na porta 8075. Nada foi quebrado
desta vez — o assunto começa depois do apply. Coloque a infraestrutura no ar:

```powershell
terraform init
terraform apply
```

Confira no navegador: <http://localhost:8075>.

## Crie o drift você mesmo

O problema deste Cenário não está no arquivo: está na distância entre o que o arquivo
descreve e o que o mundo real faz quando ninguém está olhando. Aqui você produz essa
distância com as próprias mãos e aprende a ler o que o Terraform responde.

Antes de rodar, **preveja** o plano. O `main.tf` não muda uma linha — o código continua o
mesmo. O que muda é o mundo real. O que o `plan` vai responder para o mesmo arquivo?

```powershell
docker stop mirante-web
terraform plan
docker start mirante-web
docker rm -f mirante-web
terraform plan
```

A resposta tem duas metades. Com o container **parado**, o `plan` responde
`# docker_container.web must be replaced`, com o símbolo `-/+` na frente do recurso e o
rodapé `1 to add, 0 to change, 1 to destroy` — a assinatura de uma troca: o recurso existe,
mas deixou de estar no estado que o state registra. Com o container **removido**, o `plan`
responde `will be created`: o arquivo ainda o descreve, então a saída é criar de novo. A
percepção de que o recurso sumiu do mundo tem um anúncio próprio — `has been deleted` — e
você a encontra no `plan -refresh-only`, na próxima seção.

Repare no que os dois casos têm em comum: o código não mudou. A divergência não é entre o
arquivo e o ambiente — é entre o **state** e o **mundo real**, os dois lados opostos do
triângulo que os Fundamentos desenham na seção `O triângulo`, com o **código** no vértice de
cima. A mão que resolve no braço nunca toca o state: ela muda o mundo real e deixa o
registro que o Terraform guarda contando uma história que já não é verdade. O `plan` é a
régua que mede a distância.

## O plan é leitura, o apply é escrita

Nenhum dos `plan` que você rodou mudou nada. O primeiro, com o container parado, não o
reativou; o segundo, com o container ausente, não o recriou. `plan` é leitura: compara o
código, o state e o mundo real e imprime a diferença, sem tocar em nada. `apply` é escrita:
só ele move o mundo.

É por isso que a Verificação desta plataforma pode rodar um `plan` no seu ambiente sem
risco — plan não escreve. Toda Verificação que aprova um plano limpo está apenas lendo; se
algo precisa ser escrito, ela reprova e espera você escrever.

## refresh-only: atualizar o que se sabe sem mudar o que existe

O `apply` que você conhece move o **mundo** até o **código**: pega a intenção do arquivo e
reescreve a realidade até ela coincidir. Existe um `apply` que move na direção oposta:

```powershell
terraform plan -refresh-only
terraform apply -refresh-only
```

O `apply -refresh-only` move o **state** até o **mundo**: atualiza o que o Terraform sabe
sobre o que existe, sem tocar em nada. Um serve para **reconciliar**; o outro para
**registrar** que a realidade mudou sem alterá-la. No seu exercício, o `plan -refresh-only`
anuncia o que a sua mão fez: `# docker_container.web has been deleted`. O `apply
-refresh-only` grava essa percepção no state — o container que você removeu deixa de
existir para o Terraform — e o próximo `plan` responde `will be created`: só o que falta
criar.

O `refresh-only` é a ferramenta da correção feita à mão que era a certa: alguém resolveu de
madrugada, o serviço voltou, e ainda não houve tempo de transformar aquilo em código. O
`apply -refresh-only` alinha o registro com a realidade sem desfazer a correção. Plan limpo,
mundo intacto — até a correção virar código, ou o próximo `apply` escrever por cima.

## Reconcilie

Agora devolva a correção ao código. O state e o mundo real já se entenderam; falta o código
reinar. Um `apply` de verdade:

```powershell
terraform plan
terraform apply
```

O `plan` responde `will be created`: o arquivo ainda descreve o container, e o Terraform vai
escrever o mundo até ele coincidir com o código. Aplique, confira no navegador:
<http://localhost:8075>. O serviço voltou — desta vez, pelo caminho que dura.

## Onde a correção deveria ter ido

A correção que você acabou de aplicar — e a de sábado — tinha dois lugares possíveis para
morar.

O lugar que dura é o código. Uma correção que precisa continuar valendo na próxima semana,
no próximo mês, para a próxima pessoa, vira um commit no `main.tf`: versionada, revisada, a
fonte de intenção do triângulo dos Fundamentos. Quando o código é o dono da correção, o
próximo `apply` a mantém — porque o mundo real é escrito até coincidir com ele.

O lugar que expira é o mundo real. Uma correção feita só à mão — sem código, sem state —
tem prazo de validade: o próximo `apply`. No instante em que alguém aplicar o `main.tf` de
verdade, o Terraform reescreve o mundo e apaga o que a mão fez sem registro. Quem corrige só
no mundo real está escrevendo com tinta que o próximo apply borra.

A correção de sábado era urgente, e a pessoa de plantão a fez no lugar mais rápido — o mundo
real. Este Cenário foi sobre descobrir a distância que isso cria e decidir para onde a
correção vai. Quem corrige para durar, corrige no código.

## Verificação

A Verificação deste Cenário tem quatro Asserções. As duas primeiras conferem o mundo real: o
`mirante-web` de pé e respondendo em <http://localhost:8075>. A terceira confere a origem:
`docker_container.web` no state, com o `name` `mirante-web` — o container voltou pelo
código, e não à mão. A quarta roda um `plan` e só aprova quando não há mais divergência
entre o código, o state e o que está no ar — o triângulo inteiro em sincronia.

A quarta é a que dá sentido ao Cenário. Suba o mesmo serviço com um `docker run`, e as duas
primeiras Asserções passam — o container existe e responde. Mas o state aponta para um
container que não existe mais, e o plano limpo reprova. O resultado certo pelo caminho
errado não conta nesta Trilha.
