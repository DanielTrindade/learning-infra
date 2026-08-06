# Cenário #2 — Empacotar uma app num Dockerfile

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Um segundo Cenário Docker sobre construir a própria imagem, e o catálogo com roteamento que torna qualquer Cenário além do primeiro alcançável na interface.

**Architecture:** Estende o vocabulário de Asserções com `imagem_existe`, acrescenta um catálogo e roteamento por hash no frontend, e escreve o conteúdo do Cenário. O backend não ganha suporte a Compose — este Cenário é de um container só, e `iniciar` continua sendo apenas materializar workspace.

**Tech Stack:** A mesma do esqueleto — Java 25, Spring Boot 4.0.0, Vite 8, React 19. Nenhuma dependência nova; o roteamento é feito à mão com `hashchange`.

## Contexto obrigatório

Leia antes de começar: [`CONTEXT.md`](../../../CONTEXT.md) para o vocabulário
(`Trilha`, `Cenário`, `Dificuldade`, `Cenário Ativo`, `Verificação`, `Asserção`), e as
duas ADRs em [`docs/adr/`](../../adr/). O plano do esqueleto,
[`2026-08-04-esqueleto-cenario-docker.md`](./2026-08-04-esqueleto-cenario-docker.md),
já está executado — as oito tarefas dele estão concluídas e não devem ser refeitas.

## Global Constraints

- **Todas as restrições do plano do esqueleto continuam valendo.** Em especial: nomes
  de domínio em português, backend em `127.0.0.1:8099`, Vite em `5180` com
  `strictPort`, Jackson 3 (`tools.jackson`), starters `spring-boot-starter-webmvc` e
  `-webmvc-test`.
- **Sem Docker Compose.** Este Cenário é de um container só.
- **A porta de host deste Cenário é 8089.** Verificada livre. Não use 8080 (ocupada
  por outra aplicação da máquina) nem 8088 (é a do Cenário #1).
- **Não remova o `default` que não existe.** O `switch` do `MotorDeVerificacao` é
  exaustivo sobre uma `sealed interface`. Acrescentar uma Asserção **quebra a
  compilação** de propósito, até que ela seja tratada. Esse erro é a feature.
- **O daemon do Docker precisa estar no ar** para as Tasks 1 e 3.

---

## Estrutura de arquivos

```
learning-infra/
├── content/docker/
│   ├── 01-servir-html-nginx/                  (existe)
│   └── 02-empacotar-app-dockerfile/
│       ├── cenario.md                         Task 3
│       ├── verificacao.yaml                   Task 3
│       └── workspace/app/server.js            Task 3
├── backend/src/
│   ├── main/java/dev/learninginfra/
│   │   ├── verificacao/Assercao.java          Task 1 — modificar
│   │   ├── verificacao/MotorDeVerificacao.java Task 1 — modificar
│   │   └── conteudo/LeitorDeCenario.java      Task 1 — modificar
│   └── test/java/dev/learninginfra/...        Task 1
└── frontend/src/
    ├── useRota.ts                             Task 2
    ├── Catalogo.tsx                           Task 2
    ├── App.tsx                                Task 2 — modificar
    └── estilos.css                            Task 2 — modificar
```

---

### Task 1: A Asserção `imagem_existe`

> **CONCLUÍDA e validada em 2026-08-06** — 27 testes verdes. O `switch` exaustivo
> falhou a compilação como previsto no Step 4, confirmando a garantia do design.

**Files:**
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/Assercao.java`
- Modify: `backend/src/main/java/dev/learninginfra/verificacao/MotorDeVerificacao.java`
- Modify: `backend/src/main/java/dev/learninginfra/conteudo/LeitorDeCenario.java`
- Test: `backend/src/test/java/dev/learninginfra/verificacao/MotorDeVerificacaoTest.java` — acrescentar
- Test: `backend/src/test/java/dev/learninginfra/conteudo/LeitorDeCenarioTest.java` — acrescentar

**Interfaces:**
- Consumes: `Assercao`, `MotorDeVerificacao`, `ResultadoDeAsercao` do esqueleto.
- Produces: `record Assercao.ImagemExiste(String referencia) implements Assercao`, e o
  tipo YAML `imagem_existe` com o campo `referencia`.

- [ ] **Step 1: Escrever os testes que falham**

Acrescente a `MotorDeVerificacaoTest`:

```java
    @Test
    void imagemExistePassaQuandoInspectDaCerto() {
        var resultado = motorQueResponde("sha256:abc\n", 0)
                .verificar(List.of(new Assercao.ImagemExiste("lab-app:1.0")));

        assertTrue(resultado.concluido());
    }

    @Test
    void imagemInexistenteFalhaDizendoQueFaltaConstruir() {
        var resultado = motorQueResponde("", 1)
                .verificar(List.of(new Assercao.ImagemExiste("lab-app:1.0")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("não foi construída"));
    }
```

E a `LeitorDeCenarioTest`:

```java
    @Test
    void leAsercaoDeImagem() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: imagem_existe
                    referencia: lab-app:1.0
                """);

        List<Assercao> asercoes = new LeitorDeCenario().ler(diretorio).asercoes();

        assertEquals(List.of(new Assercao.ImagemExiste("lab-app:1.0")), asercoes);
    }
```

- [ ] **Step 2: Rodar para ver falhar**

Run: `cd backend && ./mvnw -B test -Dtest=MotorDeVerificacaoTest+LeitorDeCenarioTest`
Expected: FAIL na compilação — `Assercao.ImagemExiste` não existe.

- [ ] **Step 3: Acrescentar o record à `sealed interface`**

Em `Assercao.java`, depois de `HttpCorpoContem`:

```java
    record ImagemExiste(String referencia) implements Assercao {
        @Override
        public String descricao() {
            return "a imagem `" + referencia + "` existe localmente";
        }
    }
```

- [ ] **Step 4: Recompilar e observar o erro que o design produz**

Run: `cd backend && ./mvnw -B -q compile`
Expected: FAIL em `MotorDeVerificacao.java` com algo como *the switch statement does not
cover all possible input values*.

**Este erro é o ponto.** Ele prova que o vocabulário de Asserções não pode crescer sem
que o motor saiba avaliar o tipo novo. Se em algum momento alguém "resolver" isso
adicionando um `default`, essa garantia desaparece em silêncio. Não faça isso.

- [ ] **Step 5: Tratar o caso novo no motor**

Em `MotorDeVerificacao.avaliar`, acrescente o `case`:

```java
            case Assercao.ImagemExiste a -> avaliarImagem(a);
```

E o método, ao lado de `avaliarContainer`:

```java
    private ResultadoDeAsercao avaliarImagem(Assercao.ImagemExiste a) {
        SaidaDeComando saida = executor.executar(
                List.of("docker", "image", "inspect", a.referencia()));
        return saida.sucesso()
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a,
                        "a imagem `" + a.referencia() + "` não foi construída ainda");
    }
```

Comportamento do CLI verificado na engine 29.6.1: `docker image inspect` sai com
código 1 e escreve `Error response from daemon: No such image: ...` quando a imagem não
existe, e com 0 imprimindo o JSON quando existe. Só o código de saída importa aqui.

- [ ] **Step 6: Ensinar o leitor de YAML a montar o tipo novo**

Em `LeitorDeCenario.montarAsercao`, acrescente o `case` antes do `default`:

```java
            case "imagem_existe" -> new Assercao.ImagemExiste(exigirTexto(item, "referencia"));
```

- [ ] **Step 7: Rodar a suíte inteira**

Run: `cd backend && ./mvnw -B test`
Expected: PASS, 27 testes (os 24 do esqueleto mais os 3 novos).

- [ ] **Step 8: Commit**

```bash
git add backend/src
git commit -m "feat: asserção imagem_existe"
```

---

### Task 2: Catálogo e roteamento

> **CONCLUÍDA em 2026-08-06** — `npm run build` verde e o catálogo servindo os dois
> Cenários pela API. A navegação em si não foi verificada em navegador.

**Files:**
- Create: `frontend/src/useRota.ts`
- Create: `frontend/src/Catalogo.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/estilos.css`

**Interfaces:**
- Consumes: `GET /api/cenarios` (já existe, devolve `CenarioDetalhado[]`).
- Produces: `useRota() -> string` (o caminho depois do `#`), e `Catalogo` renderizando a
  lista de Cenários.

Roteamento é feito com `hashchange` em vez de `react-router`. São doze linhas contra
uma dependência, o botão voltar do navegador funciona, e este projeto existe para você
aprender infraestrutura — não para praticar React.

- [ ] **Step 1: Escrever o hook de rota**

`frontend/src/useRota.ts`:

```ts
import { useEffect, useState } from 'react'

/** Devolve o caminho corrente depois do `#`, sem a cerquilha. Ex.: "cenarios/docker/01". */
export function useRota(): string {
  const ler = () => window.location.hash.replace(/^#\/?/, '')
  const [rota, setRota] = useState(ler)

  useEffect(() => {
    const aoMudar = () => setRota(ler())
    window.addEventListener('hashchange', aoMudar)
    return () => window.removeEventListener('hashchange', aoMudar)
  }, [])

  return rota
}
```

- [ ] **Step 2: Escrever o catálogo**

`frontend/src/Catalogo.tsx`:

```tsx
import { useEffect, useState } from 'react'
import { listarCenarios, type CenarioDetalhado } from './api'

const rotulos: Record<string, string> = {
  GUIADO: 'Guiado',
  ASSISTIDO: 'Assistido',
  AUTONOMO: 'Autônomo',
  MESTRE: 'Mestre',
}

export function Catalogo() {
  const [cenarios, setCenarios] = useState<CenarioDetalhado[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    listarCenarios().then(setCenarios).catch((e) => setErro(String(e)))
  }, [])

  if (erro) return <p className="erro">{erro}</p>
  if (!cenarios) return <p>Carregando…</p>
  if (cenarios.length === 0) return <p>Nenhum Cenário em `content/` ainda.</p>

  const trilhas = [...new Set(cenarios.map((c) => c.id.split('/')[0]))]

  return (
    <>
      <h1>Laboratórios</h1>
      {trilhas.map((trilha) => (
        <section key={trilha} className="trilha">
          <h2>{trilha}</h2>
          <ul className="cenarios">
            {cenarios
              .filter((c) => c.id.startsWith(`${trilha}/`))
              .map((c) => (
                <li key={c.id}>
                  <a href={`#/cenarios/${c.id}`}>
                    <span className="dificuldade">{rotulos[c.dificuldade]}</span>
                    <span className="titulo">{c.titulo}</span>
                    {c.concluido && <span className="selo" title="concluído">✓</span>}
                    {c.ativo && <span className="selo ativo" title="ambiente no ar">●</span>}
                  </a>
                </li>
              ))}
          </ul>
        </section>
      ))}
    </>
  )
}
```

- [ ] **Step 3: Acrescentar `listarCenarios` ao cliente da API**

Em `frontend/src/api.ts`, ao lado de `buscarCenario`:

```ts
export const listarCenarios = () => pedir<CenarioDetalhado[]>('/api/cenarios')
```

- [ ] **Step 4: Ligar as rotas no `App`**

Substitua `frontend/src/App.tsx`:

```tsx
import { Catalogo } from './Catalogo'
import { PaginaDoCenario } from './PaginaDoCenario'
import { useRota } from './useRota'
import './estilos.css'

export default function App() {
  const rota = useRota()
  const id = rota.startsWith('cenarios/') ? rota.slice('cenarios/'.length) : null

  return (
    <main>
      {id ? (
        <>
          <a className="voltar" href="#/">← todos os laboratórios</a>
          <PaginaDoCenario id={id} />
        </>
      ) : (
        <Catalogo />
      )}
    </main>
  )
}
```

O id do Cenário contém uma barra (`docker/02-...`), e é justamente por isso que a rota
usa `slice` em vez de dividir o caminho em segmentos.

- [ ] **Step 5: Estilos do catálogo**

Acrescente ao fim de `frontend/src/estilos.css`:

```css
.voltar { display: inline-block; margin-bottom: 1.5rem; font-size: 0.9rem; }

.trilha h2 { text-transform: capitalize; font-size: 1.1rem; opacity: 0.7; }

ul.cenarios { list-style: none; padding: 0; }

ul.cenarios li { margin: 0.4rem 0; }

ul.cenarios a {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.7rem 0.9rem;
  border: 1px solid rgba(127, 127, 127, 0.3);
  border-radius: 8px;
  text-decoration: none;
  color: inherit;
}

ul.cenarios a:hover { border-color: currentColor; }

ul.cenarios .titulo { flex: 1; }

.selo { color: #1a7f37; font-weight: bold; }
.selo.ativo { color: #b8860b; }
```

- [ ] **Step 6: Construir e verificar**

```bash
cd frontend && npm run build
```

Expected: `tsc -b` e `vite build` sem erro.

Depois, com backend e frontend no ar, abra `http://localhost:5180`. Deve aparecer a
lista com o Cenário #1; clicar nele muda a URL para `#/cenarios/docker/01-servir-html-nginx`
e mostra a página do Cenário. O botão voltar do navegador deve retornar ao catálogo.

- [ ] **Step 7: Commit**

```bash
git add frontend
git commit -m "feat: catálogo de cenários e roteamento por hash"
```

---

### Task 3: O conteúdo do Cenário #2

> **CONCLUÍDA e validada em 2026-08-06** — percorrido contra o Docker real: as quatro
> Asserções reprovam antes e aprovam depois, o cache de camadas confere, e a invariante
> da ADR 0002 foi observada trocando do Cenário #1 para o #2.

**Files:**
- Create: `content/docker/02-empacotar-app-dockerfile/workspace/app/server.js`
- Create: `content/docker/02-empacotar-app-dockerfile/verificacao.yaml`
- Create: `content/docker/02-empacotar-app-dockerfile/cenario.md`

**Interfaces:**
- Consumes: o formato lido pelo `LeitorDeCenario`, incluindo o tipo `imagem_existe` da Task 1.
- Produces: o Cenário `docker/02-empacotar-app-dockerfile`.

- [ ] **Step 1: A aplicação entregue ao leitor**

`workspace/app/server.js` — sem nenhuma dependência, de propósito: o exercício é sobre
Docker, não sobre `npm install`.

```js
const http = require('node:http')

const porta = process.env.PORT || 3000

http
  .createServer((_req, resposta) => {
    resposta.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
    resposta.end('<!doctype html><title>App empacotada</title><h1>Empacotei minha app</h1>')
  })
  .listen(porta, () => console.log(`ouvindo na porta ${porta}`))
```

- [ ] **Step 2: A Verificação**

`verificacao.yaml`:

```yaml
asercoes:
  - tipo: imagem_existe
    referencia: lab-app:1.0
  - tipo: container_rodando
    nome: lab-app
  - tipo: http_responde
    url: http://localhost:8089
    status: 200
  - tipo: http_corpo_contem
    url: http://localhost:8089
    texto: Empacotei minha app
```

A ordem importa para o feedback: se o leitor esqueceu de construir a imagem, a primeira
Asserção já diz isso, em vez de ele ver três falhas de rede sem explicação.

- [ ] **Step 3: O Cenário**

`cenario.md`:

````markdown
---
id: docker/02-empacotar-app-dockerfile
titulo: Empacotar uma app num Dockerfile
dificuldade: guiado
containers: [lab-app]
---
# Empacotar uma app num Dockerfile

No Cenário anterior você rodou uma imagem que outra pessoa construiu. Agora você vai
construir a sua. É a diferença entre usar Docker e realmente trabalhar com Docker.

## O que você recebeu

Clique em **Iniciar cenário**. No seu diretório de trabalho há `app/server.js`: um
servidor HTTP em Node, sem nenhuma dependência, que responde uma página e escuta na
porta 3000.

Ele não tem Dockerfile. Esse é o exercício.

## Passo 1 — escrever o Dockerfile

Crie um arquivo chamado `Dockerfile`, sem extensão, dentro de `app/`:

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY server.js .
EXPOSE 3000
CMD ["node", "server.js"]
```

Linha por linha:

- `FROM node:22-alpine` — toda imagem começa de outra. `alpine` é uma distribuição
  Linux enxuta, então a variante `-alpine` é bem menor que a padrão. Não espere
  milagre: esta imagem sai por volta de 230MB, porque o Node em si é grande. Se quiser
  medir a diferença, construa uma segunda vez trocando para `FROM node:22` e compare
  com `docker images lab-app`.
- `WORKDIR /app` — cria e entra no diretório. Os comandos seguintes rodam a partir dele.
- `COPY server.js .` — copia do **seu disco** para **dentro da imagem**. Repare na
  diferença em relação ao `-v` do Cenário anterior: `COPY` grava o arquivo dentro da
  imagem para sempre, enquanto `-v` só empresta um diretório seu durante a execução.
- `EXPOSE 3000` — **documenta** que a aplicação escuta nessa porta. E é só isso: não
  publica nada. Muita gente perde tempo aqui achando que `EXPOSE` substitui o `-p`.
- `CMD ["node", "server.js"]` — o processo que roda quando o container sobe. Quando ele
  termina, o container termina.

## Passo 2 — construir a imagem

De dentro de `app/`:

```sh
docker build -t lab-app:1.0 .
```

- `-t lab-app:1.0` dá nome e versão à imagem. Sem isso ela nasce sem nome e você só a
  alcança pelo id. Este cenário **exige** a etiqueta `lab-app:1.0`.
- O `.` no fim é o **contexto de build**: o diretório enviado ao daemon. Não é a
  localização do Dockerfile — é o que o `COPY` consegue ver. Um `COPY` de arquivo fora
  do contexto falha, e é uma das confusões mais comuns de quem está começando.

Confirme:

```sh
docker images lab-app
```

## Passo 3 — rodar a sua imagem

```sh
docker run -d --name lab-app -p 8089:3000 lab-app:1.0
```

O `-p 8089:3000` é o mesmo mecanismo do Cenário anterior: porta do host à esquerda,
porta do container à direita. O `3000` tem que casar com a porta em que o `server.js`
escuta — o `EXPOSE` não fez esse trabalho.

Abra `http://localhost:8089`.

## Passo 4 — ver o cache de camadas trabalhando

Cada instrução do Dockerfile cria uma camada, e o Docker reaproveita as que não
mudaram. Construa de novo sem alterar nada:

```sh
docker build -t lab-app:1.0 .
```

Repare nos `CACHED` na saída, e em quanto mais rápido foi.

Agora edite o `<h1>` dentro de `server.js` e construa outra vez. Só as camadas a partir
do `COPY` são refeitas — o `FROM` e o `WORKDIR` seguem em cache. É por isso que, em
projetos reais, se copia o arquivo de dependências e se instala **antes** de copiar o
código: o código muda toda hora, as dependências quase nunca.

Para ver a sua alteração no ar, recrie o container:

```sh
docker rm -f lab-app
docker run -d --name lab-app -p 8089:3000 lab-app:1.0
```

Se você mudou o texto do `<h1>`, devolva-o para **Empacotei minha app** antes de
verificar — é esse texto que a Asserção procura.

## Verifique

Clique em **Verificar**. Quatro Asserções: a imagem `lab-app:1.0` existe, o container
`lab-app` está rodando, a porta 8089 responde 200, e o corpo tem o texto esperado.

## O que você aprendeu

`FROM` escolhe a base. `WORKDIR` define o diretório. `COPY` grava arquivos na imagem —
permanentemente, ao contrário de `-v`. `EXPOSE` documenta e não publica. `CMD` define o
processo. `docker build -t nome:versão .` constrói, e o `.` é o contexto, não o
Dockerfile. E camadas são cacheadas na ordem em que você as escreveu, o que faz da
ordem das instruções uma decisão de performance.
````

- [ ] **Step 4: Rodar a suíte**

Run: `cd backend && ./mvnw -B test`
Expected: PASS. O conteúdo novo não deve quebrar nada — os testes usam fixture próprio.

- [ ] **Step 5: Percorrer o Cenário à mão**

1. Suba backend e frontend, abra `http://localhost:5180`.
2. O catálogo deve listar **dois** Cenários. Abra o segundo.
3. **Iniciar cenário** — confirme que `work/app/server.js` existe e que o `work/site/`
   do Cenário #1 **desapareceu** (a invariante da ADR 0002).
4. **Verificar** sem fazer nada: as quatro devem falhar, e a primeira deve dizer que a
   imagem não foi construída.
5. Escreva o Dockerfile, construa, rode.
6. **Verificar**: as quatro passam.
7. Confirme a conclusão em `data/progresso.json`.

- [ ] **Step 6: Commit**

```bash
git add content
git commit -m "feat: cenário docker/02 empacotar app num Dockerfile"
```

---

## Depois deste Cenário

1. **Suporte a Compose** — no primeiro Cenário multi-serviço. `iniciar` ganha
   `docker compose -p <id> up -d`, `derrubar` ganha `down -v`. A metade "setup" do
   ciclo de vida segue sem validação até lá.
2. **Asserção que distinga "nada respondeu" de "respondeu, mas não é seu container"** —
   hoje um app alheio na porta produz um `respondeu 404` que parece erro do leitor.
3. **Pré-requisitos entre Cenários** — quando a ordem passar a importar de verdade.
