---
id: iac/01-o-sabado-em-que-a-mirante-subiu
titulo: O sábado em que a Mirante subiu
dificuldade: guiado
terraform: true
containers: [mirante-web]
---
# O sábado em que a Mirante subiu

A Mirante vende um sistema de estoque para pequenos varejistas. O serviço está no ar
desde um sábado em que uma pessoa sozinha digitou uma sequência de comandos que funcionou.
Ninguém anotou quais foram. Meses depois, a empresa tem clientes, tem uma segunda pessoa
no time — e continua com uma infraestrutura que existe, mas que ninguém sabe reconstruir.

Você vai reconstruí-la. Não digitando os comandos de novo: **descrevendo o resultado** e
deixando o Terraform descobrir o caminho.

## O que já está no seu diretório

O `versions.tf` fixa a versão do Terraform e do provider. Isso não é burocracia: sem essa
fixação, a próxima pessoa que rodar `init` pode receber outra versão do provider e ver um
plano diferente do seu. Repare que `provider "docker" {}` não recebe nenhum argumento —
no Windows, o provider encontra sozinho o named pipe do Docker Desktop.

## Escreva o main.tf

Crie o arquivo `main.tf` ao lado do `versions.tf`, com exatamente estes dois recursos:

```hcl
resource "docker_image" "web" {
  name         = "nginx:1.27-alpine"
  keep_locally = true
}

resource "docker_container" "web" {
  name  = "mirante-web"
  image = docker_image.web.image_id

  ports {
    internal = 80
    external = 8070
  }
}
```

Duas coisas merecem atenção antes de você aplicar.

A primeira é `image = docker_image.web.image_id`. Você poderia ter escrito
`"nginx:1.27-alpine"` de novo ali. Ao referenciar o outro recurso, você não está evitando
repetição: está **declarando uma dependência**. O Terraform lê essa referência e conclui
sozinho que a imagem precisa existir antes do container. Ninguém escreveu a ordem.

A segunda é `keep_locally = true`. Sem isso, um `destroy` removeria a imagem do seu
disco, e o próximo `apply` faria download de novo.

## Aplique

```powershell
terraform init
terraform plan
terraform apply
```

O `init` baixa o provider e cria o `.terraform.lock.hcl` — o arquivo que registra o
digest exato do que foi baixado, para que a máquina do colega receba o mesmo binário.

O `plan` mostra o que vai acontecer. Leia a saída inteira antes de continuar: o `+` na
frente de cada recurso significa criação, e o número no rodapé é o contrato que o `apply`
vai cumprir.

O `apply` pede confirmação. Digite `yes`.

Confirme no navegador: <http://localhost:8070>.

## Aplique de novo

```powershell
terraform apply
```

Desta vez a resposta é `No changes. Your infrastructure matches the configuration.`

Esse é o comportamento que separa infraestrutura como código de um script. Um script que
cria container falha na segunda execução, porque o container já existe. O Terraform
compara o que você descreveu com o que ele sabe que existe, e não faz nada quando os dois
já coincidem. Isso se chama **idempotência**, e é o que permite rodar o mesmo código
todo dia sem medo.

## Olhe o que o Terraform passou a saber

```powershell
terraform state list
terraform state show docker_container.web
```

Esse é o **state**: o registro do que o Terraform criou e de qual recurso do mundo real
corresponde a cada endereço do seu código. Ele é a terceira peça da história — código,
state e mundo real — e vai ser o assunto de vários Cenários adiante.

## Verificação

A Verificação confere quatro coisas, e uma delas é sutil: além do container estar de pé e
respondendo, ela exige que `docker_container.web` esteja no state e que um `plan` não
encontre mudança pendente. Subir o mesmo container com `docker run` faria as duas
primeiras Asserções passarem e reprovaria nas outras duas — porque nesta Trilha o
resultado certo pelo caminho errado não conta.
