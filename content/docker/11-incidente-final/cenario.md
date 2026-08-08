---
id: docker/11-incidente-final
titulo: Incidente final de Docker
dificuldade: mestre
projetoCompose: lab-11
volumes: [lab-11-dados]
---
# Incidente final de Docker

Este é o último Cenário da trilha. Como no #4, não há passo a passo.

O ambiente já subiu quando você clicou em **Iniciar cenário**, e ele está quebrado. Sua
tarefa é descobrir por quê e consertar. Não vou dizer onde olhar.

## O objetivo

`http://localhost:8098` deve mostrar **O api disse: tatu-bola**.

Abra agora. Você vai ver outra coisa.

## O que existe no ambiente

Três serviços em `compose.yaml`, projeto `lab-11`:

- `web` — monta a página, publicado na 8098.
- `api` — a peça do meio: lê o valor de quem o guarda e entrega para o `web`.
- `db` — guarda o valor e o serve.

Os arquivos estão no seu diretório de trabalho e você pode alterar qualquer um deles.

## As ferramentas

Você já usou todas nos Cenários anteriores — é isso que este Cenário testa:

```sh
docker compose -p lab-11 ps
docker compose -p lab-11 ps -a
docker compose -p lab-11 logs <serviço>
docker compose -p lab-11 exec <serviço> <comando>
docker compose -p lab-11 up -d --build
docker inspect <container>
docker network ls
docker volume ls
```

- `ps` lista **só os containers de pé**. Compare o que ele mostra com os serviços
  declarados no `compose.yaml`: uma ausência nessa lista já é informação.
- `ps -a` inclui os que saíram. A diferença entre os dois comandos é a primeira coisa
  útil deste Cenário.
- `logs` mostra o que o processo escreveu — inclusive o erro que ele registrou ao subir.
- `inspect` mostra como o container está configurado de fato: redes, portas, montagens.
  O que o `compose.yaml` *parece* pedir e o que o container *recebeu* podem divergir.

Depois de corrigir alguma coisa, é o `up -d --build` que aplica: sem `--build` o Compose
reaproveita a imagem antiga e você vai achar que a correção não funcionou.

## Uma dica sobre método, não sobre o defeito

Sintoma não é causa. Cada mensagem de erro que você encontrar pode ter mais de uma
explicação, e a primeira que vier à cabeça costuma ser a errada. Antes de atacar o
sintoma mais visível, confirme cada camada com a ferramenta certa: quem está de pé, quem
está saudável, quem alcança quem, e onde o dado mora.

Quando **Verificar** apontar uma Asserção falhando, leia o detalhe ao lado com atenção:
ele diz *o que* está errado. Descobrir *por quê* é com você. E não pule etapas na ordem
das Asserções — do começo ao fim, ela é um roteiro de diagnóstico, não uma burocracia.

## Verifique

Nove Asserções: os três containers de pé e o `db` saudável; a 8098 respondendo 200 com o
corpo esperado; o `db` fora da rede que dá para a rua; e o valor que o banco guarda
sobrevivendo no volume `lab-11-dados`. Algumas já passam agora — e é exatamente isso que
torna o problema enganoso.

## O que você aprendeu

Que subir de pé não é a mesma coisa que estar certo: o Compose volta com sucesso e o
ambiente continua quebrado em silêncio. Que o sintoma mais visível costuma ser o último
elo de uma corrente, não a causa — e que a causa raramente está onde a mensagem aponta.
Que diagnosticar é alternar ferramentas, cada uma respondendo a uma pergunta: `ps` diz
quem está de pé, `logs` o que aconteceu, `inspect` como o container está configurado. E
que a Verificação não julga o seu palpite: ela mede o estado do ambiente — e no fim, o
único estado que importa é o que a prova final confirma.
