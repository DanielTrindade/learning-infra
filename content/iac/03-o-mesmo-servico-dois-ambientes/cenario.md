---
id: iac/03-o-mesmo-servico-dois-ambientes
titulo: O mesmo serviço, dois ambientes
dificuldade: guiado
terraform: true
containers: [mirante-homologacao, mirante-producao]
---
# O mesmo serviço, dois ambientes

Um cliente grande apareceu na Mirante. O serviço que você já sabe subir é exatamente o que o cliente procura — mas o contrato tem uma condição: o cliente não assina sem ver o serviço rodando num ambiente de homologação, separado do de produção. A saída óbvia é copiar o diretório inteiro e trocar dois valores no arquivo novo. É a dívida que o Cenário 08 vai cobrar. Aqui o passo é menor e correto: extrair o que varia, em vez de duplicar o que não varia.

Neste Cenário você escreve o serviço uma única vez e o sobe em dois ambientes ao mesmo tempo — um na porta 8072, outro na 8073 — sem copiar arquivo nenhum. No caminho aparecem quatro peças que o restante da Trilha usa o tempo todo: `variable`, `local`, os arquivos `.tfvars` e `workspace`.

## O que varia e o que não varia

O diretório de trabalho já chega com os três recursos de parametrização escritos, e cada um tem um papel distinto:

- `variable` é **entrada**: um valor que vem de fora do diretório. Não é derivado de nada — é fornecido na hora de planejar ou aplicar.
- `local` é **apelido calculado**: um valor derivado de outra coisa que já está no arquivo. Aqui `locals.nome_do_container` é `mirante-${var.ambiente}` — um nome montado a partir da entrada.
- `output` é **saída**: um valor que o Terraform publica depois do apply, para quem consome o resultado.

A regra prática para decidir onde cada coisa mora: se vem de fora, é `variable`; se é derivado de outra coisa que já está no arquivo, é `local`. O que varia entre os dois ambientes é pouco e está todo declarado no `variables.tf` — o nome do ambiente e a porta. O que não varia — a imagem, o container, a publicação da porta — está no `main.tf`, escrito uma única vez.

O `variables.tf` traz ainda uma `validation` para cada variável: uma condição que o valor precisa satisfazer antes que qualquer recurso seja criado. É o arquivo defendendo a si mesmo.

## Rode sem dizer nada

Agora rode o `apply` sem dizer nada:

```powershell
terraform init
terraform apply
```

Como nenhuma variável tem valor padrão, o Terraform **pergunta** no terminal. Responda `homologacao` quando ele perguntar `var.ambiente`. Na pergunta seguinte, `var.porta`, digite `9090` e leia o que acontece: a `validation` rejeita na hora, com a mensagem que o próprio arquivo escreveu — `a porta precisa estar no bloco 8070-8079 reservado para esta Trilha.` Cancele com `Ctrl+C` e não aplique.

Repare no que esse erro faz por você: um valor errado é barrado **antes** de o Terraform tocar em qualquer container. Sem a `validation`, o `9090` entraria no plano e o serviço nasceria numa porta fora do bloco reservado da Trilha — o erro só apareceria quando nada respondesse no lugar esperado.

E repare no que a pergunta no terminal faz contra você: um pipeline não tem ninguém para digitar. Numa esteira, `apply` sem valor é `apply` que não acontece — o processo espera resposta de um teclado que não existe. Por isso os valores de ambiente têm de vir de fora, em arquivos, e não do teclado.

## Escreva os dois arquivos de valores

Crie, no diretório de trabalho, o arquivo `homologacao.tfvars`:

```hcl
ambiente = "homologacao"
porta    = 8072
```

E o `producao.tfvars`:

```hcl
ambiente = "producao"
porta    = 8073
```

Um arquivo `.tfvars` guarda valores de variáveis, e o `-var-file` entrega o arquivo ao comando. O nome do arquivo é só uma convenção — o que o liga ao Terraform é o caminho passado no comando. Escrever os dois arquivos é o exercício; nada disso vem pronto.

## Um workspace por ambiente

Falta um pedaço do quebra-cabeça. Um único diretório tem um único `state` — o arquivo que registra o que o código criou. Se os dois `apply` usassem o mesmo `state`, o segundo **substituiria** o primeiro: `mirante-producao` entraria no lugar de `mirante-homologacao`, e você nunca teria os dois ambientes no ar ao mesmo tempo. O problema não é o comando — é onde o registro da criação mora.

A ferramenta para isso é o workspace:

```powershell
terraform workspace new homologacao
terraform apply -var-file="homologacao.tfvars"
terraform workspace new producao
terraform apply -var-file="producao.tfvars"
terraform workspace list
```

`terraform workspace new homologacao` cria **e já troca** para o workspace novo. O `apply` seguinte nasce do `homologacao.tfvars` e publica o `mirante-homologacao` na 8072 (responda `yes` quando ele perguntar). Depois `terraform workspace new producao` troca de novo, e o `apply` seguinte sobe o `mirante-producao` na 8073. No `workspace list`, o `*` marca o workspace atual. Neste momento os dois containers estão de pé, lado a lado — confira no navegador: <http://localhost:8072> e <http://localhost:8073>.

Preciso ser direto sobre o que o workspace é e o que ele não é. Workspace do Terraform separa **states** — cada workspace guarda o seu próprio registro do que foi criado. Ele **não** separa credencial, não separa conta. E não é o mecanismo de isolamento que ambientes de produção de verdade usam: lá, cada ambiente é um diretório e um backend de `state` separados. O workspace é a ferramenta prática deste Cenário; o isolamento de verdade é o assunto do Cenário 08.

## Leia a saída

O `output` do `main.tf` publica o endereço de cada ambiente. Leia a saída de cada workspace:

```powershell
terraform workspace select homologacao
terraform output endereco
terraform workspace select producao
terraform output endereco
```

O `terraform output` lê o `state` do workspace selecionado — por isso o `select` antes de cada leitura. O `output` é a interface do diretório com quem o consome, e o Cenário 08 vai depender dela.

## Verificação

A Verificação deste Cenário tem seis Asserções: os dois containers de pé, respondendo em <http://localhost:8072> e <http://localhost:8073>, o container no `state` tendo nascido do código, e o plano limpo. Ela roda o Terraform contra este mesmo diretório, e duas coisas do lado de cá precisam estar prontas.

A primeira: o `terraform_plano_limpo` roda no workspace **selecionado**, sem `-var-file`. O Terraform carrega sozinho um arquivo chamado `terraform.tfvars` — por isso, para a Verificação enxergar os valores, deixe um com os valores do workspace de produção:

```hcl
ambiente = "producao"
porta    = 8073
```

A segunda: a Asserção de estado espera o container de **produção**. Termine no workspace certo:

```powershell
terraform workspace select producao
```

Depois rode a Verificação. Se ela reprovar dizendo que o nome no `state` é `mirante-homologacao`, é sinal de que o workspace selecionado é o de homologação — e a Asserção de estado está olhando para o state errado.
