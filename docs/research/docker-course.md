# Pesquisa curricular — Trilha Docker

Pesquisa realizada em 7 de agosto de 2026. Este documento embasa a direção registrada na
[ADR 0003](../adr/0003-fundamentos-pertencem-a-trilha.md). O corte vertical de
Fundamentos e Questionário de Docker foi implementado em 7 de agosto de 2026; a expansão
dos Cenários 06–11 permanece planejada.

## Conclusão

A plataforma adotará Fundamentos e Questionário em todas as Trilhas, começando por
Docker. A Trilha Docker deve crescer em duas direções ao mesmo tempo:

1. oferecer um modelo mental antes do primeiro comando, com uma etapa curta de
   **Fundamentos** e um **Questionário** formativo;
2. ampliar a prática para o fluxo que aparece no trabalho real: construir, conectar,
   observar, proteger e distribuir imagens e containers.

A sequência recomendada é:

> Fundamentos (texto → Questionário) → reproduzir → modificar → diagnosticar → projetar

O questionário deve confirmar entendimento e indicar o que revisar, mas não ser uma
barreira rígida. Uma pessoa experiente deve poder tentar o questionário diretamente ou
seguir para a prática com um aviso. A prática continua sendo a fonte de evidência mais
forte da plataforma.

Essa expansão tem relevância além de uma impressão subjetiva. Como sinal observacional
do uso no ecossistema, o GitHub encontrou Dockerfiles em 1,9 milhão de repositórios em
2025, 120% a mais que em 2024 ([GitHub Octoverse 2025](https://github.blog/news-insights/octoverse/octoverse-a-new-developer-joins-github-every-second-as-ai-leads-typescript-to-1/)).
A documentação oficial também trata BuildKit, Compose, segurança, registries e automação
de builds como partes do fluxo moderno, e não como tópicos periféricos
([Docker Build com GitHub Actions](https://docs.docker.com/build/ci/github-actions/)).

## Diagnóstico do conteúdo atual

A Trilha Docker tem cinco Cenários:

| Cenário | O que já ensina bem |
|---|---|
| 01 — Servir um HTML seu com nginx | `docker run`, processo principal, porta e bind mount |
| 02 — Empacotar uma app num Dockerfile | imagem, Dockerfile, build e noção inicial de cache |
| 03 — Dois containers conversando com Compose | serviços, DNS por nome, rede padrão do Compose e logs |
| 04 — Um stack que não sobe | método de diagnóstico com `ps`, `logs` e `config` |
| 05 — Dados que sobrevivem ao container | camada efêmera, bind mount e volume nomeado |

É uma boa introdução e respeita a progressão de autonomia já definida pelo projeto. O
problema é de cobertura: Docker tem 5 Cenários, enquanto Kubernetes tem 14 e AWS tem 18.
O estudante aprende a colocar uma aplicação no ar, mas ainda não percorre o ciclo de
vida de uma imagem que poderia ser usada por uma equipe ou chegar à produção.

As principais lacunas são:

- arquitetura e modelo mental: cliente, API, daemon, runtime, imagem, container, tag,
  digest, registry, namespaces e cgroups;
- builds reais: contexto, `.dockerignore`, invalidação de cache, cache mounts,
  multi-stage, segredos de build e imagens mínimas;
- operação: `inspect`, `exec`, `stats`, `events`, health status, limites de recursos,
  sinais e encerramento gracioso;
- Compose: readiness, `healthcheck`, `depends_on.condition`, configurações, secrets,
  profiles e overrides;
- rede: bridge criada pelo usuário, DNS, redes internas, isolamento entre camadas e
  exposição apenas em `127.0.0.1`;
- segurança: usuário não-root, capabilities, filesystem somente leitura, risco de
  `--privileged` e do socket do Docker;
- distribuição: tags, digests, registry, push, pull e rollback;
- cadeia de fornecimento: build checks, SBOM, provenance, scan e CI/CD.

Essas lacunas aparecem também na organização da própria documentação oficial. O roteiro
de introdução do Docker destaca containers, imagens, registries, Compose, camadas, build,
tags e publicação como conceitos centrais
([Docker — próximos passos](https://docs.docker.com/get-started/introduction/whats-next/)).

## O que os Fundamentos devem explicar

O texto deve ser curto o bastante para ser lido antes da prática — aproximadamente 15 a
20 minutos — e construir um modelo mental durável. Não deve tentar antecipar todo o
conteúdo avançado da Trilha.

### 1. Qual problema o Docker resolveu

O eixo não deve ser apenas “funciona na minha máquina”. O Docker torna a aplicação um
artefato transportável: imagem, runtime, bibliotecas e configuração de execução podem ser
entregues de modo consistente entre desenvolvimento, CI e diferentes ambientes. Isso
reduz divergência de ambiente, torna a entrega reproduzível e acelera criação e descarte
de instâncias ([visão geral do Docker](https://docs.docker.com/get-started/docker-overview/)).

### 2. Container não é uma VM pequena

Uma imagem é um modelo somente leitura; um container é uma instância executável dessa
imagem com configuração e uma camada gravável efêmera. No Linux, containers usam recursos
de isolamento do kernel compartilhado. No Windows e no macOS com Docker Desktop, os
containers Linux rodam dentro da VM gerenciada pelo Desktop. Essa nuance evita ensinar
uma simplificação falsa.

Namespaces isolam processos, rede e mounts; cgroups contabilizam e limitam CPU, memória e
I/O. O isolamento não transforma qualquer configuração em segura
([segurança do Docker Engine](https://docs.docker.com/engine/security/)).

### 3. Arquitetura de funcionamento

O diagrama conceitual deve mostrar:

```text
docker CLI / Docker Compose
            │
            ▼
       Docker API
            │
            ▼
         dockerd ───── registry
            │
            ▼
        containerd
            │
            ▼
       runtime OCI
            │
            ▼
  namespaces, cgroups e filesystem
```

Para o primeiro contato, cliente, API, daemon, registry e objetos Docker são suficientes.
`containerd` e o runtime OCI devem aparecer como uma segunda camada de detalhe, sem
transformar a introdução em uma aula interna de kernel. O Docker documenta a arquitetura
cliente-servidor e o papel do daemon, cliente e registry na sua
[visão geral](https://docs.docker.com/get-started/docker-overview/), além da integração com
[runtimes alternativos](https://docs.docker.com/engine/daemon/alternative-runtimes/).

### 4. Os objetos e seus ciclos de vida

O artigo deve fixar estas distinções:

- Dockerfile descreve como construir;
- imagem é o artefato imutável resultante;
- tag é um nome mutável; digest identifica conteúdo;
- container é uma execução configurada da imagem;
- volume guarda dados fora da camada gravável do container;
- rede conecta containers sem obrigar publicação de portas no host;
- registry armazena e distribui imagens.

O estudante deve terminar a leitura conseguindo narrar o fluxo:

> escrever → construir → testar → nomear → publicar → baixar → executar → observar →
> substituir

## Como deve funcionar o Questionário

Questionários não servem apenas para medir. Experimentos com textos educacionais mostram
que recuperar a informação em testes melhora retenção posterior mais do que apenas reler,
especialmente quando a avaliação não acontece imediatamente após o estudo
([Roediger e Karpicke, 2006](https://www.psychologicalscience.org/journals/psychological-science/j.1467-9280.2006.01693.x/)).

Proposta inicial:

- 12 questões de escolha única;
- aproveitamento recomendado de 80%;
- tentativas ilimitadas;
- ordem das questões alternada a cada tentativa;
- correção depois do envio, com explicação da alternativa correta;
- link direto para a seção dos Fundamentos que deve ser revisada;
- resultado persistido separadamente da conclusão dos Cenários;
- botão para tentar o Questionário sem reler, útil para quem já conhece Docker.

Distribuição sugerida:

- 3 questões sobre imagem, container, camada e arquitetura;
- 3 sobre Dockerfile, cache e processo principal;
- 2 sobre rede e persistência;
- 2 sobre Compose, readiness e healthcheck;
- 2 situações de segurança ou diagnóstico.

As perguntas devem explorar decisões e equívocos comuns, não memorização de flags:

- `EXPOSE` documenta uma porta, mas não a publica no host;
- um container “running” pode ainda não estar pronto;
- `depends_on` curto ordena inicialização, mas não espera prontidão;
- uma tag pode mudar enquanto um digest identifica conteúdo;
- dados na camada gravável somem quando o container é removido;
- `ARG` e `ENV` não são cofres de segredo;
- root dentro do container não significa isolamento absoluto do host.

O Docker documenta explicitamente que o Compose espera um container estar rodando, não
necessariamente pronto, e que `service_healthy` deve ser combinado com `healthcheck`
([ordem de inicialização no Compose](https://docs.docker.com/compose/how-tos/startup-order/)).

## Direção de domínio adotada

O glossário preserva `Cenário` como unidade prática e verificável. Transformar um artigo
em “Cenário 00” exigiria uma Verificação artificial e enfraqueceria esse conceito.

Os termos adotados são:

**Fundamentos**  
Abertura conceitual de uma Trilha, composta pelo texto e por seu Questionário. Apresenta
o problema, o modelo mental e o vocabulário necessários antes da prática. Não inicia
ambiente.

**Questionário**  
Checagem formativa associada aos Fundamentos. Produz feedback e recomendação de revisão;
não substitui a Verificação prática.

**Cenário**  
Continua sendo a unidade prática que prepara um ambiente real e é concluída por
Verificações compostas de Asserções.

A consequência mais importante é que a Trilha passa a ser conteúdo explícito, com
Fundamentos seguidos por uma sequência de Cenários, e não apenas um agrupamento inferido
a partir do prefixo dos ids.

## Aplicação às demais Trilhas

Docker é o piloto de conteúdo, não uma exceção arquitetural. O mesmo contrato será
reutilizado nesta ordem:

1. **Docker** — imagens, containers, arquitetura, armazenamento, rede e registry;
2. **Kubernetes** — estado desejado, control plane, reconciliação, objetos, scheduling,
   rede e persistência;
3. **AWS** — responsabilidade compartilhada, regiões e zonas, identidade, APIs, control
   plane versus data plane, custo e guardrails do ambiente local;
4. **IaC e futuras Trilhas** — Fundamentos próprios, usando o mesmo formato e o mesmo
   mecanismo de Questionário e progresso.

As Trilhas sem Fundamentos publicados continuam funcionando com seus Cenários durante a
migração. Ao receber Fundamentos, a nova abertura passa a contar como uma unidade de
progresso disponível, sem bloquear os Cenários já existentes.

## Currículo prático priorizado

Os cinco Cenários atuais devem ser preservados. A primeira expansão deveria acrescentar
seis Cenários locais, autocontidos e verificáveis no Docker Desktop.

### Núcleo — primeira entrega

| Ordem | Cenário proposto | Competência observável |
|---|---|---|
| 06 | Uma imagem cara e lenta | corrigir contexto, `.dockerignore`, cache e multi-stage |
| 07 | Só quem precisa se enxerga | separar frontend, API e banco em redes e expor só a borda |
| 08 | Rodando ainda não é pronto | criar healthcheck e dependência por `service_healthy` |
| 09 | O container com privilégios demais | executar como não-root, somente leitura e com capabilities mínimas |
| 10 | Da tag ao digest | publicar em registry local, baixar e fazer rollback por digest |
| 11 | Incidente final de Docker | diagnosticar uma stack com falhas combinadas sem causa revelada |

#### 06 — Uma imagem cara e lenta

Entregar uma aplicação cujo Dockerfile copia o repositório inteiro antes de instalar
dependências, inclui artefatos locais e leva ferramentas de build para a imagem final. O
estudante deve criar `.dockerignore`, ordenar camadas para preservar cache e usar
multi-stage. Multi-stage, base mínima, cache e `.dockerignore` são recomendações oficiais
de build ([boas práticas](https://docs.docker.com/build/building/best-practices/),
[otimização de cache](https://docs.docker.com/build/cache/optimize/)).

#### 07 — Só quem precisa se enxerga

Entregar frontend, API e banco na mesma rede com portas demais publicadas. O objetivo é
criar rede de borda e rede interna, manter o banco inacessível pelo host e usar DNS por
nome de serviço. Bridges definidas pelo usuário oferecem resolução automática de nomes e
melhor isolamento que a bridge padrão
([driver bridge](https://docs.docker.com/engine/network/drivers/bridge/)).

#### 08 — Rodando ainda não é pronto

Entregar uma API que inicia antes do banco aceitar conexões. O estudante deve distinguir
processo em execução de serviço pronto, criar `healthcheck` e configurar
`depends_on.condition: service_healthy`. O exercício deve também mostrar retries na
aplicação, porque readiness não elimina falhas posteriores.

#### 09 — O container com privilégios demais

Entregar uma imagem que roda como root, grava onde não precisa e recebe privilégios
amplos. O estudante deve adotar usuário não-root, filesystem somente leitura, `tmpfs` para
escrita transitória e remoção de capabilities. O Docker recomenda reduzir capabilities e
tratar acesso ao daemon como altamente privilegiado
([segurança do Engine](https://docs.docker.com/engine/security/)).

#### 10 — Da tag ao digest

Subir um registry local descartável. O estudante constrói, cria tags semântica e de
commit, publica, remove a cópia local, baixa novamente e demonstra rollback por digest.
O cenário ensina distribuição sem exigir conta no Docker Hub ou GHCR.

#### 11 — Incidente final de Docker

Uma stack `Mestre` deve combinar apenas conceitos já praticados: cache que preservou
artefato errado, serviço unhealthy, DNS incorreto, porta exposta indevidamente e volume
com dado necessário. O estudante recebe sintomas e critérios de sucesso, não a lista de
falhas.

### Avançado — segunda entrega

Depois do núcleo, estes tópicos agregam valor sem bloquear a primeira expansão:

1. sinais, PID 1, `STOPSIGNAL`, encerramento gracioso e políticas de restart;
2. `logs`, rotação, `inspect`, `stats`, `events`, limites de CPU/memória e diagnóstico de
   OOM;
3. backup, restauração e migração de volumes;
4. segredos de build com `RUN --mount=type=secret`, mostrando por que `ARG`/`ENV` vazam
   ([segredos de build](https://docs.docker.com/build/building/secrets/));
5. pipeline com Buildx, cache remoto, teste, push, SBOM e provenance;
6. build multi-plataforma e manifest lists.

CI/CD, scan e publicação externa devem ser avançados ou opcionais porque dependem de
conta, credenciais, rede e políticas externas. A trilha pode ensinar o formato e validar
o workflow localmente sem transformar disponibilidade de um serviço terceiro em
pré-requisito para concluir o núcleo. O fluxo oficial de CI inclui BuildKit, metadados,
login, cache, multi-plataforma, SBOM e provenance
([Docker Build com GitHub Actions](https://docs.docker.com/build/ci/github-actions/)).

## Impacto arquitetural

Uma implementação limpa pode adotar esta estrutura:

```text
content/docker/
├── trilha.yaml
├── fundamentos.md
├── questionario.yaml
├── 01-servir-html-nginx/
├── 02-empacotar-app-dockerfile/
└── ...
```

Cada diretório de Trilha recebe `trilha.yaml`. Docker referencia Fundamentos e
Questionário desde o piloto; Kubernetes e AWS começam apenas com seus metadados e ganham
essas referências quando o conteúdo for escrito. Os diretórios com `cenario.md`
continuam sendo descobertos como hoje.

Mudanças esperadas:

- introduzir `Trilha`, `Fundamentos`, `Questionario`, `Questao` e `Alternativa` no backend;
- criar um repositório de Trilhas separado do `RepositorioDeCenarios`;
- não enviar o gabarito no DTO que carrega as questões;
- corrigir respostas no backend e devolver acerto, explicação e seção para revisão;
- persistir conclusão dos Fundamentos e tentativas sem perder o formato antigo de
  `data/progresso.json`;
- criar rotas próprias para Fundamentos e Questionário;
- mostrar a etapa conceitual no catálogo antes do primeiro Cenário;
- manter os endpoints e o lifecycle dos Cenários inalterados;
- não iniciar nem derrubar ambiente ao ler Fundamentos ou responder o Questionário.

O mecanismo genérico `comando_produz` permite prototipar várias verificações novas, mas
Asserções tipadas dariam feedback melhor e manteriam o vocabulário profundo do motor. Os
tipos mais úteis para a expansão são:

- `container_saudavel`;
- `container_configuracao` para usuário, read-only, limites e capabilities;
- `container_em_rede`;
- `imagem_configuracao` para usuário, entrypoint, labels e camadas;
- `imagem_no_registry` ou verificação por digest.

## Ordem de implementação sugerida

1. registrar `Fundamentos` e `Questionário` no vocabulário e a decisão arquitetural;
2. implementar Trilha explícita, leitura dos Fundamentos, Questionário e progresso;
3. escrever o texto e as 12 questões de Docker;
4. criar os Cenários 06 a 08, que cobrem as maiores lacunas operacionais com baixo risco
   externo;
5. criar os Cenários 09 a 11 e as Asserções tipadas necessárias;
6. aplicar Fundamentos e Questionário a Kubernetes e AWS;
7. avaliar conteúdo avançado de CI/CD e cadeia de fornecimento em uma segunda rodada.

## Decisões adotadas

- Adotar teoria → questionário → prática, sem hard lock.
- Chamar a abertura conceitual de `Fundamentos`, não de Artigo: “artigo” é o formato;
  “Fundamentos” expressa o papel no domínio.
- Manter `Cenário` exclusivamente prático e verificável.
- Conservar os cinco Cenários existentes e expandir a partir do 06, evitando migração de
  ids e perda de progresso.
- Priorizar experiências totalmente locais; registry externo, scan e CI entram depois.
- Evitar um artigo enciclopédico. Conceitos avançados devem aparecer perto do Cenário em
  que serão usados.
