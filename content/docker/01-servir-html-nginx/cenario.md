---
id: docker/01-servir-html-nginx
titulo: Servir um HTML seu com nginx
dificuldade: guiado
containers: [lab-web]
---
# Servir um HTML seu com nginx

Você vai pegar um arquivo HTML que está no seu disco e fazer o nginx servi-lo, sem
instalar nginx nenhum. Ao final você terá usado as três coisas que sustentam todo o
resto de Docker: rodar um container, mapear uma porta e montar um volume.

## O que você recebeu

Clique em **Iniciar cenário**. Isso limpa o ambiente do cenário anterior e copia os
arquivos deste para o seu diretório de trabalho — o caminho aparece logo acima. Lá
dentro há um `site/index.html`.

Esse diretório existe no **seu disco**, e é isso que torna o exercício possível: o
Docker vai enxergá-lo direto.

## Passo 1 — rodar um container

O comando mais simples possível:

```sh
docker run --rm hello-world
```

- `docker run` cria um container a partir de uma imagem e o executa.
- `--rm` apaga o container quando ele termina. Sem isso, ele fica parado ocupando
  espaço — rode `docker ps -a` depois para ver os cadáveres que você já acumulou.
- `hello-world` é a imagem. Como você não tem ela localmente, o Docker baixa do
  registro público antes de rodar.

Container não é máquina virtual: é um processo do seu sistema, isolado. Quando o
processo termina, o container termina.

## Passo 2 — rodar algo que não termina

O nginx é um servidor: ele fica no ar esperando requisições.

```sh
docker run -d --name lab-web nginx
```

- `-d` (*detached*) devolve o seu terminal em vez de prender no log do processo.
- `--name lab-web` dá um nome fixo ao container. Sem isso o Docker inventa um nome
  aleatório e você precisa do id para tudo. Este cenário **exige** o nome `lab-web`.

Confirme:

```sh
docker ps
```

Agora tente abrir `http://localhost:8088` no navegador. **Não vai funcionar** — e a
razão é o próximo passo.

## Passo 3 — mapear a porta

O nginx está escutando na porta 80 *dentro* do container. Essa porta não é a sua: o
container tem a própria pilha de rede. Para alcançá-la, você publica a porta no host.

Remova o container anterior e recrie com o mapeamento:

```sh
docker rm -f lab-web
docker run -d --name lab-web -p 8088:80 nginx
```

- `-p 8088:80` significa **porta 8088 do seu host** → **porta 80 do container**. A
  ordem é sempre `host:container`. Inverter é o erro mais comum aqui.

Abra `http://localhost:8088`. Você deve ver a página padrão do nginx.

## Passo 4 — montar o seu diretório

Falta servir o *seu* HTML. O nginx serve o que estiver em
`/usr/share/nginx/html` dentro do container. Você vai apontar essa pasta para a sua.

Use o caminho de trabalho que apareceu ao iniciar o cenário, acrescido de `\site`:

```sh
docker rm -f lab-web
docker run -d --name lab-web -p 8088:80 -v C:\caminho\completo\ate\work\site:/usr/share/nginx/html:ro nginx
```

- `-v origem:destino` monta um diretório do host dentro do container. A ordem é
  `host:container`, igual ao `-p`.
- A origem precisa ser um caminho **absoluto**. Caminho relativo o daemon não resolve.
- `:ro` monta somente-leitura. O nginx não precisa escrever ali, e restringir o que
  um container pode fazer é um hábito que vale desde o primeiro dia.

Recarregue `http://localhost:8088`. Agora é o seu HTML.

## Verifique

Clique em **Verificar**. Três Asserções serão checadas: o container `lab-web` está
rodando, a porta 8088 responde 200, e o corpo contém o título do seu HTML.

Se alguma falhar, o detalhe ao lado diz exatamente qual das três quebrou — o que
quase sempre aponta o passo que faltou.

## O que você aprendeu

`docker run` cria e executa. `-d` solta o terminal. `--name` te dá um identificador
estável. `-p host:container` publica uma porta. `-v host:container` monta um
diretório. Praticamente todo comando `docker run` que você vai ver na vida é uma
combinação disso com mais opções.
