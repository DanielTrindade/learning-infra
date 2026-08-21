---
id: iac/02-o-plano-e-o-contrato
titulo: O plano é o contrato
dificuldade: guiado
terraform: true
containers: [mirante-web]
---
# O plano é o contrato

A Mirante contratou uma segunda pessoa. O primeiro `apply` que a pessoa nova rodou
derrubou o serviço por quarenta minutos — não porque o comando estava errado, mas porque
um plano foi aprovado sem ser lido. O serviço voltou por sorte, não por previsão.

Neste Cenário você aprende a ler o `plan` antes de aprovar. O plano não é ruído que
antecede o `yes`: é o **contrato** do que vai acontecer no seu ambiente. Quem assina o
contrato lendo as cláusulas não descobre as consequências depois.

## Aplique o que já está escrito

O diretório já chega com o `main.tf` do Cenário anterior escrito: a imagem
`nginx:1.27-alpine` e o container `mirante-web` publicado na porta 8071. Aqui o assunto
não é redigitar recursos — é ler o plano. Coloque a infraestrutura no ar:

```powershell
terraform init
terraform apply
```

Confirme no navegador: <http://localhost:8071>.

## Os quatro símbolos

Todo plano é montado com poucos símbolos, e cada um significa uma operação. A tabela
abaixo foi medida neste mesmo provider e nesta mesma versão do Terraform:

| O arquivo diz | Símbolo | O plano responde |
|---|---|---|
| um recurso novo aparece | `+` | `will be created` |
| um atributo muda sem derrubar nada | `~` | `updated in-place` |
| um recurso sai do arquivo | `-` | `will be destroyed` |
| um atributo que não se muda em lugar | `-/+` | `must be replaced` |

O `+` anuncia criação: algo que ainda não existe vai passar a existir.

O `~` é a mudança barata: um atributo é atualizado no recurso em execução, e nada é
destruído no caminho.

O `-` anuncia destruição: o recurso saiu do arquivo, e o Terraform vai removê-lo do mundo.

O `-/+` é o mais caro dos quatro — o único que, numa única operação, destrói o que está
no ar para criar outra coisa no lugar. Ele aparece quando um atributo não pode ser
ajustado em um recurso já criado: o plano marca a linha com `# forces replacement`, o
container responde como `must be replaced`, e o rodapé passa a contar `1 to add ... 1 to
destroy`. O preço de um `-/+` é o recurso inteiro, e é por isso que ele exige a leitura
mais cuidadosa.

## Faça uma mudança barata

Abra o `main.tf` e troque `restart = "no"` por `restart = "unless-stopped"` — o atributo
que a medição confirmou como atualizável em lugar. Agora planeje:

```powershell
terraform plan
```

O plano mostra `~ restart = "no" -> "unless-stopped"` com `will be updated in-place`, e o
rodapé fecha em `Plan: 0 to add, 1 to change, 0 to destroy.` — nada é somado, nada é
destruído. Aplique. O container continua o mesmo, apenas com a política de reinício
mudada.

## Faça uma mudança cara — e não aplique

Agora troque `external = 8071` por `external = 8075` no `main.tf` e planeje de novo:

```powershell
terraform plan
```

Desta vez o `~` sozinho não resolve. O plano responde `# docker_container.web must be
replaced`, e a linha da porta aparece como `~ external = 8071 -> 8075 # forces
replacement`. A porta publicada é definida na criação do container e não pode ser
ajustada em um container em execução — o provider precisa destruir o que está de pé e
criar outro na porta nova. Repare no rodapé: `Plan: 1 to add, 0 to change, 1 to destroy.`
O `~` do passo anterior não custava nada; aqui o plano cobra o container inteiro.

**Desfaça a mudança antes de seguir.** Devolva `external = 8071` ao `main.tf` e não
aplique. Se você aplicar com a porta em 8075, a Verificação fica vermelha: a Asserção de
HTTP espera o serviço respondendo em <http://localhost:8071>, e não na 8075.

## O plano salvo

Até aqui você planejou e aplicou em duas rodadas. Um pipeline faz diferente: separa a
decisão da execução. Primeiro, salve o plano:

```powershell
terraform plan -out plano.tfplan
terraform show plano.tfplan
```

O `show` lê o arquivo salvo e imprime o mesmo plano de antes. Depois, aplique o arquivo:

```powershell
terraform apply plano.tfplan
```

Repare no que **não** aconteceu: não houve pergunta, não houve `yes`. O `apply` de um
plano salvo vai direto para as ações, porque a decisão já foi tomada quando o plano foi
revisado — o arquivo é o contrato assinado. É exatamente por isso que um pipeline pode
aplicar um plano salvo sem ninguém na frente do teclado: quem decide é a revisão, não a
execução. O Cenário 12, no fim desta Trilha, vai montar esse pipeline por inteiro; aqui,
basta reconhecer o padrão.

## Verificação

A Verificação deste Cenário tem quatro Asserções: o container de pé, respondendo na 8071,
e registrado no state — e uma quarta, sutil. `terraform_plano_limpo` roda um `plan` e só
aprova se o arquivo e a infraestrutura no ar descreverem a mesma coisa. Quem deixou a
porta em 8075 no arquivo sem aplicar reprova nela, mesmo com o container no ar; quem
aplicou a mudança reprova na Asserção de HTTP, porque a 8071 não responde. As duas juntas
fecham a saída: o resultado certo pelo caminho errado não conta.
