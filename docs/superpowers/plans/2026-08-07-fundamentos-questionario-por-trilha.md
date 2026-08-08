# Fundamentos e Questionário por Trilha — Plano de Implementação

> **Estado:** Fase 1 implementada em 2026-08-07, com validação visual manual pendente.
> Este plano começou por Docker, mas o contrato é transversal e será reutilizado por
> Kubernetes, AWS e futuras Trilhas.

**Goal:** Tornar a Trilha conteúdo explícito, dar a ela Fundamentos compostos por texto e
Questionário formativo e integrar essa abertura ao catálogo e ao progresso sem alterar o
lifecycle dos Cenários.

**Architecture:** Um módulo profundo `CatalogoDeTrilhas` compõe metadados do filesystem,
Cenários e progresso atrás de uma interface pequena. `TrilhaController` é o adapter HTTP.
O gabarito permanece no backend. Fundamentos não criam ambiente, não alteram o Cenário
Ativo e não usam o Motor de Verificação.

**Tech Stack:** Java 25, Spring Boot 4, SnakeYAML, Jackson, React 19, TypeScript 6 e
`react-markdown`. Nenhuma dependência nova é necessária para a primeira entrega.

## Contexto obrigatório

Antes de implementar, leia:

- [`CONTEXT.md`](../../../CONTEXT.md);
- [ADR 0001 — sem terminal embutido](../../adr/0001-sem-terminal-embutido.md);
- [ADR 0002 — um Cenário Ativo por vez](../../adr/0002-um-cenario-ativo-por-vez.md);
- [ADR 0003 — Fundamentos pertencem à Trilha](../../adr/0003-fundamentos-pertencem-a-trilha.md);
- [pesquisa curricular de Docker](../../research/docker-course.md).

## Decisões fechadas

- Uma Trilha tem no máximo um Fundamentos.
- Fundamentos são compostos pelo texto e por um Questionário.
- O Questionário é formativo: 80% conclui Fundamentos, há tentativas ilimitadas e cada
  tentativa devolve feedback.
- O percentual usa divisão inteira de `acertos * 100 / total`. Com 12 questões, 9 acertos
  produzem 75% e reprovam; 10 produzem 83% e aprovam.
- A conclusão dos Fundamentos é recomendada, não obrigatória para iniciar Cenários.
- Fundamentos disponíveis contam como uma unidade no progresso da Trilha; cada Cenário
  continua contando como uma unidade.
- Uma Trilha sem Fundamentos publicados mantém o progresso calculado apenas pelos
  Cenários.
- Publicar Fundamentos em uma Trilha antes concluída acrescenta conteúdo novo e pode
  tirá-la temporariamente de 100%. Isso é esperado, não migração de progresso.
- Abrir ou responder Fundamentos nunca inicia, reinicia ou derruba um Cenário.
- O frontend nunca recebe `alternativaCorreta` antes da correção.
- Docker recebe o primeiro conteúdo. Kubernetes e AWS recebem o manifesto de Trilha na
  primeira entrega, mas seus Fundamentos serão escritos depois.

## Estrutura de conteúdo

```text
content/
├── docker/
│   ├── trilha.yaml
│   ├── fundamentos.md
│   ├── questionario.yaml
│   └── 01-servir-html-nginx/...
├── kubernetes/
│   ├── trilha.yaml
│   └── 01-primeiro-pod/...
└── aws/
    ├── trilha.yaml
    └── 01-endpoint-seguro/...
```

Manifesto com Fundamentos:

```yaml
id: docker
titulo: Docker
fundamentos:
  titulo: Fundamentos do Docker
  artigo: fundamentos.md
  questionario: questionario.yaml
  aproveitamentoMinimo: 80
```

Manifesto ainda sem Fundamentos:

```yaml
id: kubernetes
titulo: Kubernetes
```

Formato do Questionário:

```yaml
questoes:
  - id: imagem-ou-container
    enunciado: Qual objeto é o artefato imutável usado para criar execuções?
    alternativas:
      - id: imagem
        texto: A imagem
      - id: container
        texto: O container
    alternativaCorreta: imagem
    explicacao: A imagem é o modelo; o container é uma instância executável dela.
    revisar: imagens-containers-e-camadas
```

## Interface do módulo

O seam principal fica em `CatalogoDeTrilhas`. Controllers e testes devem usar esta
interface, sem conhecer como manifests, Markdown, YAML de questões e progresso são
combinados.

```java
public class CatalogoDeTrilhas {
    public List<Trilha> listar();
    public Optional<Trilha> buscar(String id);
    public ResultadoDoQuestionario responder(
            String idDaTrilha, Map<String, String> respostas);
}
```

Invariantes escondidas pelo módulo:

- o id do manifesto é igual ao nome do diretório;
- ids de Trilhas, questões e alternativas são únicos;
- toda questão tem pelo menos duas alternativas;
- `alternativaCorreta` referencia uma alternativa existente;
- `revisar` referencia uma seção `##` existente no Markdown;
- `aproveitamentoMinimo` fica entre 1 e 100;
- uma submissão traz exatamente uma resposta válida para cada questão;
- o melhor percentual nunca diminui;
- `concluidoEm` registra a primeira aprovação e não muda em novas tentativas.

O filesystem e o arquivo JSON são dependências locais substituíveis por `@TempDir`; não
criar ports ou adapters hipotéticos para eles. `TrilhaController` é o único adapter HTTP.

## Contrato HTTP alvo

### `GET /api/trilhas`

Retorna resumos sem o Markdown completo:

```json
[
  {
    "id": "docker",
    "titulo": "Docker",
    "fundamentos": {
      "titulo": "Fundamentos do Docker",
      "estado": "NAO_INICIADO",
      "melhorPercentual": 0,
      "tentativas": 0
    },
    "cenarios": [
      {
        "id": "docker/01-servir-html-nginx",
        "titulo": "Servir um HTML seu com nginx",
        "dificuldade": "GUIADO",
        "quantidadeDeAsercoes": 3,
        "ativo": false,
        "concluido": false
      }
    ],
    "concluidos": 0,
    "total": 6,
    "percentual": 0,
    "concluida": false
  }
]
```

### `GET /api/trilhas/{id}/fundamentos`

Retorna Markdown, estado e questões em ordem alternada. Não retorna gabarito nem
explicação antes da tentativa.

```json
{
  "idDaTrilha": "docker",
  "titulo": "Fundamentos do Docker",
  "markdown": "# Fundamentos do Docker...",
  "aproveitamentoMinimo": 80,
  "estado": "NAO_INICIADO",
  "melhorPercentual": 0,
  "tentativas": 0,
  "questoes": [
    {
      "id": "imagem-ou-container",
      "enunciado": "Qual objeto...",
      "alternativas": [
        { "id": "imagem", "texto": "A imagem" },
        { "id": "container", "texto": "O container" }
      ]
    }
  ]
}
```

### `POST /api/trilhas/{id}/questionario`

Entrada:

```json
{
  "respostas": {
    "imagem-ou-container": "imagem"
  }
}
```

Saída:

```json
{
  "percentual": 100,
  "aprovado": true,
  "melhorPercentual": 100,
  "tentativas": 1,
  "feedback": [
    {
      "questaoId": "imagem-ou-container",
      "acertou": true,
      "alternativaCorreta": "imagem",
      "explicacao": "A imagem é o modelo...",
      "revisar": "imagens-containers-e-camadas"
    }
  ]
}
```

Trilha desconhecida devolve 404. Questão ou alternativa desconhecida e submissão
incompleta devolvem 400.

---

## Fase 1 — Capacidade transversal da plataforma

### Task 1: Tornar Trilha conteúdo explícito

**Files:**

- Create: `backend/src/main/java/dev/learninginfra/trilha/Trilha.java`
- Create: `backend/src/main/java/dev/learninginfra/trilha/LeitorDeTrilha.java`
- Create: `backend/src/main/java/dev/learninginfra/trilha/CatalogoDeTrilhas.java`
- Create: `backend/src/test/java/dev/learninginfra/trilha/CatalogoDeTrilhasTest.java`
- Create: `backend/src/test/resources/conteudo-fixture/docker/trilha.yaml`
- Create: `content/docker/trilha.yaml`
- Create: `content/kubernetes/trilha.yaml`
- Create: `content/aws/trilha.yaml`

**Interface:** `CatalogoDeTrilhas.listar()` e `buscar(id)` compõem manifestos com os
Cenários encontrados pelo `RepositorioDeCenarios`.

- [x] Escrever testes com `@TempDir` para listar Trilhas em ordem estável.
- [x] Testar id do manifesto diferente do diretório e Cenário sem manifesto. Id duplicado
  tornou-se impossível pela invariante `id == nome do diretório`.
- [x] Rodar `cd backend && ./mvnw -B test -Dtest=CatalogoDeTrilhasTest` e confirmar RED.
- [x] Implementar os records e a leitura mínima de `trilha.yaml`.
- [x] Adicionar manifests das três Trilhas reais.
- [x] Rodar o teste focado e `CatalogoRealTest`; o catálogo continua com 37 Cenários.

### Task 2: Ler e validar Fundamentos e Questionário

**Files:**

- Create: `backend/src/main/java/dev/learninginfra/trilha/Fundamentos.java`
- Create: `backend/src/main/java/dev/learninginfra/trilha/Questionario.java`
- Create: `backend/src/main/java/dev/learninginfra/trilha/ResultadoDoQuestionario.java`
- Modify: `backend/src/main/java/dev/learninginfra/trilha/LeitorDeTrilha.java`
- Modify: `backend/src/main/java/dev/learninginfra/trilha/CatalogoDeTrilhas.java`
- Create: `backend/src/test/resources/conteudo-fixture/docker/fundamentos.md`
- Create: `backend/src/test/resources/conteudo-fixture/docker/questionario.yaml`
- Modify: `backend/src/test/resources/conteudo-fixture/docker/trilha.yaml`
- Test: `backend/src/test/java/dev/learninginfra/trilha/CatalogoDeTrilhasTest.java`

**Interface:** `buscar("docker")` devolve Fundamentos completos para consumidores
confiáveis; `responder` corrige sem expor a estrutura interna do leitor.

- [x] Testar leitura do Markdown, nota mínima, questões, alternativas e gabarito.
- [x] Testar todas as invariantes que podem ser violadas no conteúdo, uma falha por vez;
  duplicidade de id de Trilha é eliminada pela igualdade obrigatória com o diretório.
- [x] Testar correção em 0%, 9 de 12, 10 de 12 e 12 de 12 acertos.
- [x] Testar submissão incompleta, ids desconhecidos e tentativas repetidas.
- [x] Rodar o teste focado e confirmar RED.
- [x] Implementar a correção determinística dentro do módulo.
- [x] Alternar a ordem na representação pública e em cada nova tentativa; o domínio
  mantém ordem determinística.
- [x] Rodar o teste focado e confirmar GREEN.

### Task 3: Persistir o progresso dos Fundamentos sem quebrar dados antigos

**Files:**

- Create: `backend/src/main/java/dev/learninginfra/progresso/ProgressoDosFundamentos.java`
- Create: `backend/src/main/java/dev/learninginfra/configuracao/ConfiguracaoDeTempo.java`
- Modify: `backend/src/main/java/dev/learninginfra/progresso/Progresso.java`
- Modify: `backend/src/main/java/dev/learninginfra/progresso/RepositorioDeProgresso.java`
- Modify: `backend/src/main/java/dev/learninginfra/trilha/CatalogoDeTrilhas.java`
- Create: `backend/src/test/java/dev/learninginfra/progresso/RepositorioDeProgressoTest.java`
- Test: `backend/src/test/java/dev/learninginfra/trilha/CatalogoDeTrilhasTest.java`

**Data:** acrescentar `fundamentos` como mapa por id de Trilha. Cada valor guarda
`tentativas`, `melhorPercentual` e `concluidoEm` anulável.

- [x] Testar a leitura do JSON antigo, contendo apenas `cenarioAtivo` e `concluidos`.
- [x] Testar que salvar depois da leitura preserva o progresso antigo.
- [x] Testar incremento de tentativas, melhor percentual monotônico e primeira aprovação.
- [x] Injetar `Clock` em `CatalogoDeTrilhas` para tornar timestamps determinísticos.
- [x] Testar que responder não muda `cenarioAtivo`.
- [x] Rodar os testes focados e a suíte de progresso/ciclo de vida.

### Task 4: Expor Trilhas e Questionário por HTTP sem vazar o gabarito

**Files:**

- Create: `backend/src/main/java/dev/learninginfra/api/TrilhaController.java`
- Create: `backend/src/main/java/dev/learninginfra/api/dto/TrilhaResumo.java`
- Create: `backend/src/main/java/dev/learninginfra/api/dto/FundamentosDetalhados.java`
- Create: `backend/src/main/java/dev/learninginfra/api/dto/SubmissaoDoQuestionario.java`
- Create: `backend/src/main/java/dev/learninginfra/api/dto/ResultadoDoQuestionarioDto.java`
- Create: `backend/src/test/java/dev/learninginfra/api/TrilhaControllerTest.java`

**Interface:** implementar os três endpoints definidos em “Contrato HTTP alvo”. Manter
os endpoints atuais de Cenário; eles continuam sendo o seam do lifecycle prático.

- [x] Testar lista com Trilha que tem Fundamentos e Trilha que ainda não tem.
- [x] Testar que o resumo calcula total, concluídos, percentual e conclusão no backend.
- [x] Testar que `GET fundamentos` não contém `alternativaCorreta` nem `explicacao`.
- [x] Testar correção aprovada e reprovada, incluindo feedback completo depois do POST.
- [x] Testar 404 de Trilha/Fundamentos ausentes e 400 de submissão inválida.
- [x] Implementar o controller como adapter fino: mapear DTO, delegar e traduzir erros.
- [x] Rodar `cd backend && ./mvnw -B test -Dtest=TrilhaControllerTest` e a suíte completa.

### Task 5: Escrever os Fundamentos e o Questionário de Docker

**Files:**

- Modify: `content/docker/trilha.yaml`
- Create: `content/docker/fundamentos.md`
- Create: `content/docker/questionario.yaml`
- Modify: `backend/src/test/java/dev/learninginfra/conteudo/CatalogoRealTest.java`

**Conteúdo do texto:**

1. o problema de consistência e entrega que Docker resolveu;
2. container versus VM, incluindo a VM do Docker Desktop;
3. cliente, API, `dockerd`, `containerd`, runtime OCI e registry;
4. imagem, container, camadas e camada gravável;
5. namespaces, cgroups e limites do isolamento;
6. rede, volumes e persistência;
7. tag versus digest e o ciclo construir–publicar–executar;
8. mapa dos Cenários práticos da Trilha.

**Banco inicial de 12 questões:**

1. imagem versus container;
2. papel do cliente e do daemon;
3. container versus VM;
4. camadas e cache;
5. processo principal e fim do container;
6. por que `ARG`/`ENV` não protegem segredo;
7. `EXPOSE` versus publicação de porta;
8. camada gravável versus volume;
9. running versus ready no Compose;
10. tag mutável versus digest;
11. root no container e limites de isolamento;
12. papel de um registry.

- [x] Escrever o texto com seções `##` e ids estáveis para revisão.
- [x] Escrever questões situacionais, não perguntas de memorização de flags.
- [x] Garantir exatamente uma alternativa correta e explicação útil por questão.
- [x] Atualizar o manifesto Docker com nota mínima 80 e referências relativas.
- [x] Estender `CatalogoRealTest` para exigir três manifests e Fundamentos apenas em Docker.
- [x] Rodar `cd backend && ./mvnw -B test -Dtest=CatalogoRealTest,CatalogoDeTrilhasTest`.

### Task 6: Criar a página de Fundamentos e o fluxo do Questionário

**Files:**

- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/App.tsx`
- Create: `frontend/src/PaginaDeFundamentos.tsx`
- Create: `frontend/src/Questionario.tsx`
- Create: `frontend/src/ResultadoDoQuestionario.tsx`
- Modify: `frontend/src/estilos.css`

**Rota:** `#/trilhas/{id}/fundamentos`.

- [x] Acrescentar tipos e funções para os três endpoints sem reutilizar tipos de Cenário.
- [x] Renderizar Markdown com sumário e âncoras usando a mesma regra de slug do backend.
- [x] Exigir uma alternativa por questão antes de enviar.
- [x] Após o POST, mostrar percentual, aprovação e feedback de todas as questões.
- [x] Cada erro oferece “Revisar esta seção” e rola até o heading indicado.
- [x] Permitir nova tentativa sem recarregar o artigo.
- [x] Mostrar “Ir para o primeiro Cenário” depois da correção, aprovado ou não.
- [x] Preservar foco, labels, teclado e `prefers-reduced-motion` no código.
- [x] Rodar `cd frontend && npm run lint && npm run build`.

### Task 7: Fazer o catálogo consumir Trilhas explícitas

**Files:**

- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/Catalogo.tsx`
- Modify: `frontend/src/progresso.ts`
- Modify: `frontend/src/estilos.css`

**Interface:** o frontend deixa de inferir Trilha com `idDaTrilha` no catálogo e passa a
consumir o resumo calculado pelo backend. `idDaTrilha` continua útil nas páginas de
Cenário e na preferência local da Trilha atual.

- [x] Trocar `listarCenarios()` por `listarTrilhas()` no catálogo.
- [x] Renderizar Fundamentos antes da lista de Cenários quando estiverem disponíveis.
- [x] Representar Fundamentos como um segmento na barra de progresso.
- [x] Usar `NAO_INICIADO`, `EM_ANDAMENTO` e `CONCLUIDO` no card de Fundamentos.
- [x] Ao aderir a Docker, recomendar Fundamentos como próximo conteúdo se não concluídos.
- [x] Manter Cenários clicáveis; exibir recomendação, nunca bloqueio.
- [x] Manter Kubernetes e AWS no fluxo existente enquanto não tiverem Fundamentos.
- [x] Uma Trilha só vai ao arquivo de concluídas quando todas as unidades disponíveis
  estiverem concluídas.
- [x] Rodar lint e build.

### Task 8: Validar o corte vertical e atualizar a documentação operacional

**Files:**

- Modify: `README.md`
- Modify: este plano, marcando passos concluídos e decisões descobertas em execução.

- [x] Rodar `cd backend && ./mvnw test` com Docker Desktop ativo.
- [x] Rodar `cd frontend && npm run lint && npm run build`.
- [ ] Abrir Docker → Fundamentos e confirmar leitura, sumário e navegação por teclado.
- [ ] Reprovar com menos de 80%, revisar uma seção e tentar novamente.
- [ ] Aprovar com pelo menos 80% e confirmar persistência após reiniciar o backend.
- [ ] Iniciar um Cenário antes de aprovar e confirmar que o backend permite.
- [ ] Manter um Cenário ativo, responder o Questionário e confirmar que ele continua ativo.
- [ ] Abrir um `data/progresso.json` antigo e confirmar migração sem perda.
- [ ] Confirmar que Kubernetes e AWS continuam acessíveis e com percentuais corretos.
- [x] Trocar no README “Evolução planejada” por instruções reais de uso.

---

## Fase 2 — Expansão prática de Docker

Depois do corte vertical, criar planos executáveis individuais, na ordem:

1. `docker/06-uma-imagem-cara-e-lenta` — `.dockerignore`, cache e multi-stage;
2. `docker/07-so-quem-precisa-se-enxerga` — redes de borda e interna;
3. `docker/08-rodando-nao-e-pronto` — healthcheck e readiness;
4. `docker/09-privilegios-demais` — usuário não-root, read-only e capabilities;
5. `docker/10-da-tag-ao-digest` — registry local e rollback;
6. `docker/11-incidente-final` — diagnóstico integrado no degrau `Mestre`.

Antes de escrever cada plano, validar os comandos no Docker Engine instalado e criar
Asserções tipadas somente quando um Cenário real provar a necessidade. A primeira ordem
de candidatos é `container_saudavel`, `container_em_rede`, `container_configuracao` e
`imagem_configuracao`; `comando_produz` permanece como escape hatch.

## Fase 3 — Aplicação às outras Trilhas

O recurso não termina quando Docker estiver pronto:

1. **Kubernetes:** pesquisar e escrever Fundamentos sobre estado desejado, control plane,
   reconciliação, objetos, scheduling, rede e persistência; criar 12 questões no mesmo
   formato e acrescentar as referências ao `content/kubernetes/trilha.yaml`.
2. **AWS:** escrever Fundamentos sobre responsabilidade compartilhada, regiões e zonas,
   IAM, APIs, control plane versus data plane, custo e guardrails do MiniStack; criar o
   Questionário e atualizar `content/aws/trilha.yaml`.
3. **IaC e futuras Trilhas:** criar `trilha.yaml`, Fundamentos e Questionário usando o
   mesmo contrato, sem novos endpoints ou componentes por tecnologia.

Cada adoção futura é conteúdo e validação curricular. Se exigir mudança no contrato
genérico, o desenho do piloto falhou e deve ser revisto antes de duplicar exceções.

## Critérios de conclusão da feature

- Todas as Trilhas são descobertas por `trilha.yaml`.
- Docker oferece Fundamentos e Questionário completos.
- Kubernetes e AWS continuam funcionais sem conteúdo teórico durante a migração.
- O gabarito não aparece em nenhuma resposta anterior à submissão.
- Progresso antigo carrega sem perda e o novo progresso persiste tentativas e aprovação.
- Fundamentos contam no progresso, mas nunca alteram ou bloqueiam Cenários.
- Backend, lint, build e percurso manual estão verdes.
- O roadmap de Docker, Kubernetes e AWS permanece referenciado no README e neste plano.
