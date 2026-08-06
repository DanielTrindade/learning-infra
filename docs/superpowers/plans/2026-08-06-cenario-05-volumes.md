# Cenário #5 — Dados que sobrevivem ao container (Assistido)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** O Cenário sobre volumes nomeados e persistência, o primeiro no degrau `Assistido`, mais as duas Asserções e o teardown de volumes que ele exige.

**Architecture:** O vocabulário de Asserções ganha `volume_existe` e `comando_produz` — a segunda é o escape hatch geral que o desenho original prometeu. O `Cenário` ganha `volumes`, e o teardown passa a removê-los.

**Tech Stack:** A mesma dos planos anteriores. Nenhuma dependência nova. Este é o primeiro Cenário sem porta de host — não há HTTP nele.

## Contexto obrigatório

Leia [`CONTEXT.md`](../../../CONTEXT.md) e as ADRs em [`docs/adr/`](../../adr/). Os
planos dos Cenários [#1](./2026-08-04-esqueleto-cenario-docker.md),
[#2](./2026-08-06-cenario-02-dockerfile.md), [#3](./2026-08-06-cenario-03-compose.md) e
[#4](./2026-08-06-cenario-04-diagnostico.md) já estão executados.

## Por que o teardown de volume não é opcional

Um volume nomeado sobrevive ao container **por definição** — é o que o Cenário ensina.
Isso cria uma armadilha nova: se o volume sobrevivesse também à troca de Cenário, você
faria o exercício hoje, voltaria amanhã, clicaria em **Verificar** sem tocar em nada, e
**passaria**. O dado ainda estaria lá.

É a mesma classe de defeito que a [ADR 0002](../../adr/0002-um-cenario-ativo-por-vez.md)
existe para impedir, mas na forma mais perigosa: falso positivo silencioso, não conflito
barulhento. Uma ferramenta de aprendizado que aprova por engano é pior que nenhuma.

## Global Constraints

- **Todas as restrições dos planos anteriores continuam valendo.**
- **Sem porta de host.** Este Cenário não sobe servidor.
- **O volume deste Cenário é `lab-05-dados`.**
- **O teardown usa `docker volume rm -f`, nunca `docker volume rm`.** Verificado na
  engine 29.6.1: sem `-f`, remover um volume inexistente sai com **código 1** e o
  teardown estouraria toda vez que você trocasse de Cenário sem ter feito o #5. Com
  `-f`, sai com 0.
- **`docker volume inspect` de volume inexistente sai com código 1**, que é o sinal da
  Asserção `volume_existe`.
- **O daemon do Docker precisa estar no ar** para as três tarefas.

---

### Task 1: As Asserções `volume_existe` e `comando_produz`

> **CONCLUÍDA e validada em 2026-08-06** — o `switch` exaustivo quebrou a compilação de
> novo, como previsto. Ganhou também um recorte de stderr: veja abaixo.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/Assercao.java`
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Test: `MotorDeVerificacaoTest.java` e `LeitorDeCenarioTest.java` — acrescentar

**Interfaces:**
- Produces: `record Assercao.VolumeExiste(String nome)` e
  `record Assercao.ComandoProduz(List<String> comando, String contem, String descricao)`.
  Tipos YAML `volume_existe` (campo `nome`) e `comando_produz` (campos `comando`,
  `contem`, `descricao`).

#### Por que `comando_produz` carrega a própria descrição

As outras Asserções derivam a descrição dos seus campos. Esta não pode: o comando de um
Cenário de volume é
`docker run --rm -v lab-05-dados:/dados alpine cat /dados/bicho.txt`, e mostrar isso no
checklist **entregaria a sintaxe que o exercício pede para o leitor descobrir**. Por
isso `descricao` é um campo obrigatório do YAML, e o acessor do record já satisfaz o
método da interface.

- [ ] **Step 1: Escrever os testes que falham**

Acrescente a `MotorDeVerificacaoTest`:

```java
    @Test
    void volumeExistePassaQuandoInspectDaCerto() {
        var resultado = motorQueResponde("[{}]", 0)
                .verificar(List.of(new Assercao.VolumeExiste("lab-05-dados")));

        assertTrue(resultado.concluido());
    }

    @Test
    void volumeInexistenteFalhaDizendoQueNaoFoiCriado() {
        var resultado = motorQueResponde("", 1)
                .verificar(List.of(new Assercao.VolumeExiste("lab-05-dados")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("não existe"));
    }

    @Test
    void comandoProduzPassaQuandoASaidaTemOTexto() {
        var resultado = motorQueResponde("tamandua\n", 0).verificar(List.of(
                new Assercao.ComandoProduz(List.of("echo", "tamandua"), "tamandua", "o dado sobreviveu")));

        assertTrue(resultado.concluido());
    }

    @Test
    void comandoProduzDistingueComandoQueFalhouDeSaidaErrada() {
        var falhou = motorQueResponde("", 1).verificar(List.of(
                new Assercao.ComandoProduz(List.of("cat", "/nada"), "tamandua", "o dado sobreviveu")));
        var saidaErrada = motorQueResponde("preguica\n", 0).verificar(List.of(
                new Assercao.ComandoProduz(List.of("cat", "/x"), "tamandua", "o dado sobreviveu")));

        assertTrue(falhou.asercoes().getFirst().detalhe().contains("não completou"));
        assertTrue(saidaErrada.asercoes().getFirst().detalhe().contains("sem o texto"));
    }

    @Test
    void comandoProduzUsaADescricaoDoCenarioENaoOComando() {
        var resultado = motorQueResponde("tamandua\n", 0).verificar(List.of(
                new Assercao.ComandoProduz(
                        List.of("docker", "run", "-v", "lab-05-dados:/dados", "alpine", "cat", "/dados/x"),
                        "tamandua",
                        "o dado sobreviveu ao container")));

        var descricao = resultado.asercoes().getFirst().descricao();
        assertEquals("o dado sobreviveu ao container", descricao);
        assertFalse(descricao.contains("-v"), "a descrição não pode entregar a sintaxe do exercício");
    }
```

E a `LeitorDeCenarioTest`:

```java
    @Test
    void leAsercoesDeVolumeEDeComando() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: volume_existe
                    nome: lab-05-dados
                  - tipo: comando_produz
                    descricao: o dado sobreviveu ao container
                    comando: ["docker", "run", "--rm", "alpine", "echo", "tamandua"]
                    contem: tamandua
                """);

        List<Assercao> asercoes = new LeitorDeCenario().ler(diretorio).asercoes();

        assertEquals(new Assercao.VolumeExiste("lab-05-dados"), asercoes.get(0));
        assertEquals(
                new Assercao.ComandoProduz(
                        List.of("docker", "run", "--rm", "alpine", "echo", "tamandua"),
                        "tamandua",
                        "o dado sobreviveu ao container"),
                asercoes.get(1));
    }
```

- [ ] **Step 2: Rodar para ver falhar**

Run: `cd backend && ./mvnw -B test -Dtest=MotorDeVerificacaoTest`
Expected: FAIL na compilação — os records novos não existem.

- [ ] **Step 3: Acrescentar os records**

Em `Assercao.java`, depois de `ImagemExiste`:

```java
    record VolumeExiste(String nome) implements Assercao {
        @Override
        public String descricao() {
            return "o volume `" + nome + "` existe";
        }
    }

    /**
     * O escape hatch do vocabulário: roda um comando e confere a saída. A descrição vem
     * do Cenário porque o comando costuma revelar a resposta do exercício.
     */
    record ComandoProduz(java.util.List<String> comando, String contem, String descricao)
            implements Assercao {
    }
```

`ComandoProduz` não declara `descricao()`: o acessor do componente já implementa o
método da interface.

- [ ] **Step 4: Recompilar e ver o switch quebrar**

Run: `cd backend && ./mvnw -B -q compile`
Expected: FAIL — *the switch expression does not cover all possible input values*. Como
nos planos anteriores, esse erro é a garantia funcionando. Não adicione `default`.

- [ ] **Step 5: Tratar os casos novos no motor**

Em `MotorDeVerificacao.avaliar`:

```java
            case Assercao.VolumeExiste a -> avaliarVolume(a);
            case Assercao.ComandoProduz a -> avaliarComando(a);
```

E os métodos:

```java
    private ResultadoDeAsercao avaliarVolume(Assercao.VolumeExiste a) {
        SaidaDeComando saida = executor.executar(List.of("docker", "volume", "inspect", a.nome()));
        return saida.sucesso()
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a, "o volume `" + a.nome() + "` não existe");
    }

    private ResultadoDeAsercao avaliarComando(Assercao.ComandoProduz a) {
        SaidaDeComando saida = executor.executar(a.comando());
        if (!saida.sucesso()) {
            return ResultadoDeAsercao.reprovada(a,
                    "a checagem não completou: " + saida.stderr().strip());
        }
        return saida.stdout().contains(a.contem())
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a, "rodou, mas a saída veio sem o texto esperado");
    }
```

- [ ] **Step 6: Ler os tipos novos no YAML**

Em `LeitorDeCenario.montarAsercao`, antes do `default`:

```java
            case "volume_existe" -> new Assercao.VolumeExiste(exigirTexto(item, "nome"));
            case "comando_produz" -> new Assercao.ComandoProduz(
                    lerLista(item, "comando"),
                    exigirTexto(item, "contem"),
                    exigirTexto(item, "descricao"));
```

E o helper, ao lado de `lerContainers`:

```java
    @SuppressWarnings("unchecked")
    private List<String> lerLista(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        if (valor == null) {
            throw new IllegalArgumentException("campo obrigatório ausente: " + chave);
        }
        return List.copyOf((List<String>) valor);
    }
```

- [ ] **Step 7: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS, 42 testes.

- [ ] **Step 8: Commit**

```bash
git add backend/src && git commit -m "feat: asserções volume_existe e comando_produz"
```

---

### Task 2: Teardown de volumes

> **CONCLUÍDA e validada em 2026-08-06** — confirmado contra o Docker real: o volume
> `lab-05-dados` desaparece ao trocar de Cenário.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/Cenario.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Modify: `backend/src/main/java/dev/learninginfra/ciclodevida/GerenciadorDeCenarioAtivo.java`
- Test: `GerenciadorDeCenarioAtivoTest.java` e `LeitorDeCenarioTest.java` — acrescentar

**Interfaces:**
- Produces: componente `List<String> volumes` em `Cenario` (nono e último), lido do
  frontmatter pela chave `volumes`, e teardown com `docker volume rm -f`.

- [ ] **Step 1: Escrever o teste que falha**

Acrescente a `GerenciadorDeCenarioAtivoTest`:

```java
    @Test
    void iniciarRemoveOsVolumesDoCenarioAnterior() throws Exception {
        Path diretorio = raiz.resolve("content").resolve("docker-05");
        Files.createDirectories(diretorio.resolve("workspace"));
        Cenario primeiro = new Cenario("docker/05", "titulo", Dificuldade.ASSISTIDO,
                List.of(), "# corpo", diretorio, List.of(), null, List.of("lab-05-dados"));
        Cenario segundo = cenario("docker/06", List.of());
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/05")).thenReturn(Optional.of(primeiro));

        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        gerenciador.iniciar(primeiro);
        comandosExecutados.clear();
        gerenciador.iniciar(segundo);

        assertEquals(
                List.of(List.of("docker", "volume", "rm", "-f", "lab-05-dados")),
                comandosExecutados);
    }
```

E a `LeitorDeCenarioTest`:

```java
    @Test
    void leOsVolumesDeclarados() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: docker/05
                titulo: Com volume
                dificuldade: assistido
                volumes: [lab-05-dados]
                ---
                # corpo
                """);

        assertEquals(List.of("lab-05-dados"), new LeitorDeCenario().ler(diretorio).volumes());
    }
```

- [ ] **Step 2: Rodar para ver falhar**

Run: `cd backend && ./mvnw -B test -Dtest=GerenciadorDeCenarioAtivoTest`
Expected: FAIL na compilação — `Cenario` não tem nono componente.

- [ ] **Step 3: Acrescentar o componente ao `Cenario`**

Substitua os construtores de `Cenario.java`, mantendo os dois secundários para não
quebrar chamadas existentes:

```java
public record Cenario(
        String id,
        String titulo,
        Dificuldade dificuldade,
        List<String> containers,
        String markdown,
        Path diretorio,
        List<Assercao> asercoes,
        String projetoCompose,
        List<String> volumes) {

    /** Cenário sem projeto Compose e sem volumes — a maioria. */
    public Cenario(String id, String titulo, Dificuldade dificuldade, List<String> containers,
                   String markdown, Path diretorio, List<Assercao> asercoes) {
        this(id, titulo, dificuldade, containers, markdown, diretorio, asercoes, null, List.of());
    }

    /** Cenário com projeto Compose e sem volumes. */
    public Cenario(String id, String titulo, Dificuldade dificuldade, List<String> containers,
                   String markdown, Path diretorio, List<Assercao> asercoes, String projetoCompose) {
        this(id, titulo, dificuldade, containers, markdown, diretorio, asercoes, projetoCompose,
                List.of());
    }

    public boolean usaCompose() {
        return projetoCompose != null && !projetoCompose.isBlank();
    }

    public Path workspace() {
        return diretorio.resolve("workspace");
    }
}
```

- [ ] **Step 4: Ler o campo**

Em `LeitorDeCenario.ler`, passe o nono argumento:

```java
                textoOpcional(meta, "projetoCompose"),
                lerListaOpcional(meta, "volumes"));
```

E o helper:

```java
    @SuppressWarnings("unchecked")
    private List<String> lerListaOpcional(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        return valor == null ? List.of() : List.copyOf((List<String>) valor);
    }
```

Se `lerContainers` já faz exatamente isso, troque as duas chamadas por este helper e
apague `lerContainers` — são a mesma função com nomes diferentes.

- [ ] **Step 5: Remover os volumes no teardown**

Em `GerenciadorDeCenarioAtivo.derrubar`, depois do laço de containers:

```java
        for (String volume : anterior.volumes()) {
            SaidaDeComando saida = executor.executar(
                    List.of("docker", "volume", "rm", "-f", volume));
            if (!saida.sucesso()) {
                throw new IllegalStateException(
                        "não consegui remover o volume `" + volume + "` do Cenário "
                        + anterior.id() + ": " + saida.stderr().strip()
                        + " — sem isso a próxima Verificação deste Cenário passaria sozinha, "
                        + "com o dado da vez anterior");
            }
        }
```

A ordem importa: containers primeiro, volumes depois. Um volume em uso por container
vivo não é removível.

O `-f` não é preguiça — é o que torna o comando idempotente. Sem ele, `docker volume rm`
de um volume inexistente sai com código 1, e o teardown estouraria toda vez que você
saísse deste Cenário sem tê-lo feito.

- [ ] **Step 6: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS, 44 testes.

- [ ] **Step 7: Commit**

```bash
git add backend/src && git commit -m "feat: teardown de volumes declarados"
```

---

### Task 3: O conteúdo do Cenário #5

> **CONCLUÍDA e validada em 2026-08-06** — 45 testes verdes. As duas Asserções reprovam
> antes e aprovam depois. Expôs um efeito colateral da Verificação; veja abaixo.

**Files:**
- Create: `content/docker/05-dados-que-sobrevivem/verificacao.yaml`
- Create: `content/docker/05-dados-que-sobrevivem/cenario.md`

Não há `workspace/`: este Cenário não entrega arquivo nenhum. Tudo acontece em
containers descartáveis.

- [ ] **Step 1: A Verificação**

`verificacao.yaml`:

```yaml
asercoes:
  - tipo: volume_existe
    nome: lab-05-dados

  - tipo: comando_produz
    descricao: o dado gravado ainda existe depois que o container morreu
    comando: ["docker", "run", "--rm", "-v", "lab-05-dados:/dados", "alpine", "cat", "/dados/bicho.txt"]
    contem: tamandua
```

A ordem dá o diagnóstico em degraus: se você não criou o volume, a primeira já diz isso,
em vez de você ver um erro de arquivo não encontrado.

- [ ] **Step 2: O Cenário**

`cenario.md` — note a dificuldade `assistido`: o objetivo e as formas dos comandos são
dados, mas não os comandos completos.

````markdown
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

Comprove antes de resolver. Rode um container `alpine` que escreva algo num arquivo,
depois rode outro container da mesma imagem e tente ler esse arquivo. Você vai precisar
de `sh -c` para encadear escrita e leitura dentro do mesmo container:

```sh
docker run --rm alpine sh -c 'echo tamandua > /bicho.txt && cat /bicho.txt'
```

Rode esse comando duas vezes. Depois rode só a leitura, num container novo:

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
Essa é a diferença inteira entre bind mount e volume nomeado, e é uma vírgula de sintaxe.

## O caminho

1. Crie o volume `lab-05-dados`.
2. Rode um container descartável montando esse volume em algum diretório, e escreva
   `tamandua` num arquivo `bicho.txt` **dentro do diretório montado**. Se escrever fora
   dele, o dado morre com o container e a Verificação vai reprovar.
3. Confirme que o container acabou — o `--rm` já cuida disso.
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
````

- [ ] **Step 3: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS.

- [ ] **Step 4: Percorrer o Cenário à mão**

1. Catálogo com **cinco** Cenários, este marcado `Assistido` — o primeiro fora de
   `Guiado` e `Mestre`.
2. **Iniciar cenário**, depois **Verificar** sem fazer nada: as duas falham, e a
   primeira deve dizer que o volume não existe.
3. Faça o exercício. **Verificar**: as duas passam.
4. **O teste que importa:** abra outro Cenário e rode `docker volume ls`. O
   `lab-05-dados` **não** pode estar lá. Se sobreviver, volte ao #5 e clique em
   **Verificar** sem fazer nada — se passar, a ADR 0002 está furada por falso positivo.

- [ ] **Step 5: Commit**

```bash
git add content && git commit -m "feat: cenário docker/05 dados que sobrevivem"
```

---

## Duas coisas que a execução revelou

### A Verificação tem efeito colateral

A Asserção `comando_produz` deste Cenário roda
`docker run --rm -v lab-05-dados:/dados alpine cat ...`, e **`-v` cria o volume quando
ele não existe**, em silêncio. Ou seja: verificar antes de fazer o exercício *cria* o
volume, e a Asserção `volume_existe` passa a valer sozinha na chamada seguinte.

Testado e descartado: `--mount type=volume,source=...` **também cria**. Não há flag do
`docker run` que monte sem criar.

Gravidade real: o Cenário **não** pode ser concluído falsamente, porque `concluido`
exige todas as Asserções e a leitura do arquivo segue reprovando. O estrago é uma
Asserção verde enganosa, não uma aprovação indevida.

Como ficou: a armadilha virou parte da aula — `-v` criar volume por engano é uma causa
real de aplicação subindo sem dados em produção. Mas fica registrado que **a Verificação
não é puramente observadora**, e isso é uma fraqueza do desenho, não uma escolha.

### O stderr do Docker afoga o erro real

Um `docker run` que precisa baixar a imagem escreve o progresso do download no stderr.
O detalhe da Asserção vinha assim:

```
Unable to find image 'alpine:latest' locally
latest: Pulling from library/alpine
Status: Downloaded newer image for alpine:latest
cat: can't open '/dados/bicho.txt': No such file or directory
```

Só a última linha interessa. O motor agora usa apenas a última linha não vazia do
stderr, com teste que trava esse comportamento.

## Depois deste Cenário

1. **Verificação sem efeito colateral** — hoje uma Asserção pode alterar o ambiente que
   observa. Um caminho possível é a Verificação rodar num modo que recuse criar
   recursos, ou reverter o que criou.
2. **Asserção que distinga "seu container" de "app alheio na mesma porta"** — segue
   pendente desde o Cenário #1.
2. **O degrau `Autônomo`** é o único dos quatro que nunca foi usado.
3. **Pré-requisitos entre Cenários** — a ordem importa de fato desde o #3.
