# Learning Infra

Laboratórios práticos de infraestrutura, para uso pessoal. Você lê a aula no navegador,
executa os comandos **no seu próprio terminal**, e a plataforma verifica o estado real da
sua máquina para dizer se o Cenário foi concluído.

Não há terminal embutido, e isso é decisão de projeto — veja
[ADR 0001](docs/adr/0001-sem-terminal-embutido.md).

## Antes de começar

- **Docker Desktop rodando.** `docker ps` precisa responder sem pendurar. Quase tudo
  aqui depende dele.
- **Java 25.** O Maven vem pelo wrapper (`./mvnw`), não precisa instalar.
- **Node 22+.**

## Rodando

Dois processos, em dois terminais. Nenhum dos dois é o terminal onde você vai fazer os
exercícios — use um terceiro para isso.

```sh
# terminal 1 — backend em 127.0.0.1:8099
cd backend
./mvnw spring-boot:run
```

```sh
# terminal 2 — frontend em localhost:5180
cd frontend
npm install     # só na primeira vez
npm run dev
```

Abra **<http://localhost:5180>**.

> A porta é **5180**, não a 5173 padrão do Vite. A 5173 costuma já estar ocupada por
> outro projeto, e o `strictPort` está ligado justamente para o Vite falhar em vez de
> deslizar em silêncio para outra porta e te fazer abrir o app errado.

## Fazendo uma lição

1. Escolha um Cenário no catálogo. A etiqueta de dificuldade diz o quanto ele entrega:
   `Guiado` dá todos os comandos, `Assistido` dá o objetivo e a forma, `Autônomo` dá só
   o objetivo, `Mestre` entrega um ambiente quebrado sem dizer o que quebrou.
2. Clique em **Iniciar cenário**. Isso derruba o ambiente do Cenário anterior e prepara
   o seu diretório de trabalho — o caminho absoluto aparece na tela, e é ele que você
   usa nos comandos com `-v`.
3. **Clique em Verificar antes de fazer qualquer coisa.** Todas as Asserções devem
   falhar. Se alguma passar de cara, ou o Cenário está mal escrito ou sobrou ambiente —
   nos dois casos é bug, não sucesso.
4. Faça o exercício no seu terminal.
5. Clique em **Verificar**. O checklist mostra cada Asserção separadamente, com o motivo
   ao lado das que falharam.

## O que a plataforma mexe na sua máquina

| Caminho | O que é |
|---|---|
| `work/` | Diretório de trabalho. **Apagado e recriado a cada Iniciar** — não guarde nada seu aqui. |
| `data/progresso.json` | Qual Cenário está ativo e quais você concluiu. |
| `content/` | As aulas. É aqui que você edita ou escreve Cenários. |

Iniciar um Cenário **remove** os containers, projetos Compose e volumes declarados pelo
Cenário anterior. Isso é intencional: sem essa limpeza, sobra de ambiente faria a
Verificação aprovar você por engano. Veja
[ADR 0002](docs/adr/0002-um-cenario-ativo-por-vez.md).

Só um Cenário fica ativo por vez. Trocar de Cenário perde o que você não tiver salvo
fora do `work/`.

### Portas usadas pelos Cenários

Reserve estas: **8088** (#1), **8089** (#2), **8090** (#3), **8091** (#4). O #5 não usa
porta. O backend fica na **8099** e o frontend na **5180**.

Se for escrever um Cenário novo, escolha a porta conferindo o que já roda na sua
máquina. A 8080 parece a escolha óbvia e é justamente a mais arriscada — quando ela está
ocupada por outra aplicação, a Asserção recebe um `404` do app errado e reporta algo que
parece defeito do seu exercício.

## Quando algo der errado

**A porta está ocupada mas nada parece estar rodando.** Matar `./mvnw` ou `npm run dev`
mata só o wrapper; o processo filho (a JVM ou o `vite`) sobrevive e continua segurando a
porta. Ache o dono e confira antes de matar:

```powershell
Get-NetTCPConnection -LocalPort 8099 -State Listen |
  ForEach-Object { Get-CimInstance Win32_Process -Filter "ProcessId=$($_.OwningProcess)" } |
  Select-Object ProcessId, CommandLine
```

**A Verificação diz que nada respondeu, mas o `curl` funciona.** Confira se o backend em
execução é mesmo o que você acabou de compilar — provavelmente há uma JVM antiga na
8099, pelo motivo acima.

**Sobrou ambiente de um Cenário anterior.** O teardown roda ao iniciar outro Cenário. Na
mão:

```sh
docker rm -f <container>
docker compose -p <projeto> down -v
docker volume rm -f <volume>
```

## Escrevendo um Cenário

Cada Cenário é um diretório em `content/<trilha>/<slug>/`:

- `cenario.md` — frontmatter com `id`, `titulo`, `dificuldade` e, se precisar,
  `containers`, `projetoCompose` e `volumes` (o que o teardown vai remover). O corpo é a
  aula em Markdown.
- `verificacao.yaml` — as Asserções. Tipos disponíveis: `container_rodando`,
  `http_responde`, `http_corpo_contem`, `imagem_existe`, `volume_existe` e
  `comando_produz`.
- `workspace/` — opcional. Copiado para `work/` no Iniciar. Se contiver um
  `compose.yaml` **e** o Cenário declarar `projetoCompose`, o stack sobe sozinho; é
  assim que um Cenário entrega ambiente pronto ou quebrado de propósito.

Não há cadastro em banco nem passo de build: criar o diretório basta, e o catálogo o
encontra na próxima chamada.

Para acrescentar um tipo de Asserção, comece pelo record em `Assercao.java`. O `switch`
do `MotorDeVerificacao` é exaustivo e **vai quebrar a compilação** até você tratar o caso
novo. Esse erro é proposital; não o resolva com um `default`.

## Testes

```sh
cd backend && ./mvnw test      # precisa do Docker no ar
cd frontend && npm run build   # typecheck e bundle
```

## Onde está o resto da documentação

- [`CONTEXT.md`](CONTEXT.md) — o vocabulário do domínio. Leia antes de escrever Cenário.
- [`docs/adr/`](docs/adr/) — decisões de arquitetura e o porquê delas.
- [`docs/superpowers/plans/`](docs/superpowers/plans/) — os planos de implementação, com
  o comportamento do CLI do Docker verificado em execução real. São documentos de
  construção, não de uso.
