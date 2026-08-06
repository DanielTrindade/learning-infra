---
id: docker/04-stack-que-nao-sobe
titulo: Um stack que não sobe
dificuldade: mestre
projetoCompose: lab-04
---
# Um stack que não sobe

Este Cenário é diferente dos anteriores. Não há passo a passo.

O ambiente já subiu quando você clicou em **Iniciar cenário**, e ele está quebrado. Sua
tarefa é descobrir por quê e consertar. Não vou dizer onde olhar.

## O objetivo

`http://localhost:8091` deve mostrar **O api disse: tatu-bola**.

Abra agora. Você vai ver outra coisa.

## O que existe no ambiente

Dois serviços em `compose.yaml`, projeto `lab-04`:

- `api` — deveria responder um JSON na porta 3000, alcançável só de dentro da rede.
- `web` — busca esse JSON e monta a página, publicado na 8091.

Os arquivos estão no seu diretório de trabalho e você pode alterar qualquer um deles.

## As ferramentas

Você já usou todas nos Cenários anteriores, exceto as duas últimas:

```sh
docker compose -p lab-04 ps
docker compose -p lab-04 ps -a
docker compose -p lab-04 logs <serviço>
docker compose -p lab-04 exec <serviço> <comando>
docker compose -p lab-04 up -d --build
```

- `ps` lista **só os containers de pé**. Compare o que ele mostra com os serviços
  declarados no `compose.yaml`: uma ausência nessa lista já é informação.
- `ps -a` inclui os que saíram, com o estado de cada um. A diferença entre os dois
  comandos é a primeira coisa útil deste Cenário.
- `logs` mostra o que o processo escreveu antes de morrer. Um container que sai deixa o
  log para trás; ele não desaparece junto.

Depois de corrigir alguma coisa, é o `up -d --build` que aplica: sem `--build` o Compose
reaproveita a imagem antiga e você vai achar que a correção não funcionou.

## Uma dica sobre método, não sobre o defeito

A primeira mensagem de erro que você encontrar vai sugerir um problema de rede. Antes de
investigar rede, confirme que todos os containers que deveriam estar de pé realmente
estão. Mensagem de conexão recusada tem duas explicações possíveis — o caminho até o
destino está errado, ou não há ninguém no destino — e a segunda é muito mais comum.

Quando **Verificar** apontar uma Asserção falhando, leia o detalhe ao lado com atenção:
ele diz *o que* está errado. Descobrir *por quê* é com você.

## Verifique

Quatro Asserções: os dois containers de pé, a 8091 respondendo 200, e o corpo com a
frase esperada. Duas delas já passam agora — e é exatamente isso que torna o problema
enganoso.

## O que você aprendeu

Que `up` bem-sucedido não significa ambiente saudável: o Compose retorna assim que os
containers foram iniciados, e um deles pode morrer logo depois. Que o log de um
container morto sobrevive a ele. Que `--build` é necessário para que uma correção no
Dockerfile chegue à imagem. E que a mensagem de erro mais visível costuma ser a
consequência, não a causa.
