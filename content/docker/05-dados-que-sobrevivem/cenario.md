---
id: docker/05-dados-que-sobrevivem
titulo: Dados que sobrevivem ao container
dificuldade: assistido
volumes: [lab-05-dados]
---
# Dados que sobrevivem ao container

Nos Cenários anteriores tudo que importava estava na imagem ou no seu disco. Falta o
terceiro lugar onde dado pode morar — e é onde bancos de dados moram.

Este Cenário é `Assistido`: você recebe o objetivo e a forma dos comandos, não os
comandos prontos.

## O problema

O sistema de arquivos de um container é uma camada descartável em cima da imagem.
Escreva um arquivo dentro de um container e apague o container: o arquivo vai junto.

Comprove antes de resolver. Rode um container `alpine` que escreva e leia um arquivo no
mesmo comando:

```sh
docker run --rm alpine sh -c 'echo tamandua > /bicho.txt && cat /bicho.txt'
```

Funciona. Agora tente ler o mesmo arquivo num container novo:

```sh
docker run --rm alpine cat /bicho.txt
```

O erro que você vê é o ponto de partida do Cenário.

## O objetivo

Um volume chamado **`lab-05-dados`** deve existir e conter um arquivo **`bicho.txt`**
com o texto **`tamandua`** — e esse dado precisa sobreviver a um container que já morreu.

## As peças

Três comandos, com as partes que você precisa completar:

```sh
docker volume create <nome>
docker run --rm -v <volume>:<caminho-no-container> <imagem> <comando>
docker volume ls
```

O `-v` você já viu no Cenário #1, mas ali o lado esquerdo era um caminho do seu disco.
**Quando o lado esquerdo não parece um caminho, o Docker entende como nome de volume.**
Essa é a diferença inteira entre bind mount e volume nomeado, e ela cabe numa barra.

Uma armadilha que você vai encontrar sozinho se não for avisado: **`-v` cria o volume
caso ele não exista**, em silêncio e sem perguntar. Isso é conveniente e perigoso — um
nome digitado errado não dá erro, só cria um volume novo e vazio, e a sua aplicação sobe
sem os dados. Se você clicar em **Verificar** antes de fazer qualquer coisa, vai ver
exatamente isso acontecer: a Asserção do volume passa a valer, porque a própria checagem
o criou ao tentar ler. O `docker volume create` do passo 1 existe para deixar a intenção
explícita, não porque seja obrigatório.

## O caminho

1. Crie o volume `lab-05-dados`.
2. Rode um container descartável montando esse volume em algum diretório, e escreva
   `tamandua` num arquivo `bicho.txt` **dentro do diretório montado**. Se escrever fora
   dele, o dado morre com o container e a Verificação vai reprovar.
3. O `--rm` já garante que o container acabou.
4. Rode **outro** container, montando o mesmo volume, e leia o arquivo. Se aparecer
   `tamandua`, o dado sobreviveu ao container que o criou.

## Bind mount ou volume nomeado?

Vale saber a diferença antes de escolher em projeto real:

- **Bind mount** (Cenário #1): você escolhe o caminho no host. Ótimo para código-fonte
  em desenvolvimento, porque você edita no seu editor e o container vê na hora. Ruim
  para dado de banco, porque amarra o dado à sua árvore de diretórios e às permissões
  do seu sistema.
- **Volume nomeado** (aqui): o Docker escolhe e gerencia o lugar. Você endereça por
  nome. É o certo para dado que a aplicação produz — banco, upload, cache.

Inspecione com `docker volume inspect lab-05-dados` e repare no campo `Mountpoint`:
existe um caminho real, mas ele é do Docker, não seu. Não é lá que você deve mexer.

## Verifique

Duas Asserções: o volume existe, e o conteúdo dele sobreviveu. A segunda roda um
container novo para ler — se ela passar, é prova de que o dado não estava no container
que você usou para escrever.

## O que você aprendeu

Que o sistema de arquivos de um container morre com ele. Que `-v nome:/caminho` cria ou
usa um volume nomeado, enquanto `-v /caminho/do/host:/caminho` faz bind mount — e que o
Docker decide qual é pela forma do lado esquerdo. Que volume nomeado é gerenciado pelo
Docker e endereçado por nome. E que o dado sobrevive não só ao container, mas a você
esquecer que ele existe: `docker volume ls` costuma revelar entulho de meses.
