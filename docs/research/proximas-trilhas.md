# Pesquisa — as próximas três Trilhas

> Estado: recomendação para decisão
>
> Data da pesquisa: 2026-08-20
>
> Escopo: escolha de tema, não desenho de conteúdo. Windows 11 Pro, Docker Desktop
> (Docker Engine 29.6.1), cluster `docker-desktop`, usuário único. Considera apenas
> temas que **não** são recorte de Docker, Kubernetes, AWS ou IaC.

## Resumo executivo

Três Trilhas novas, e uma decisão de ordenação que alcança as sete. A ordem de
**construção** abaixo é por uso real no mercado hoje e por interesse de estudo; a
verificabilidade nesta máquina foi usada só para eliminar o inviável, e nenhuma das três
foi eliminada — as três foram medidas em execução real hoje, nesta máquina.

A ordem em que o leitor **estuda** é outra coisa, e está em
[Ordem recomendada das Trilhas](#ordem-recomendada-das-trilhas): ali o Linux é o
primeiro de tudo, e a recomendação não bloqueia ninguém.

1. **Linux** — o candidato do autor se confirma: é o substrato que as quatro Trilhas
   existentes já usam sem nunca ensinar, e `Bash/Shell` saltou de 33,9% para 48,7% dos
   respondentes do Stack Overflow em um ano.
2. **CI/CD e entrega contínua** — é a prática de infraestrutura com maior presença
   diária no mercado depois de container: 62% dos desenvolvedores usam GitHub Actions em
   projetos pessoais e 41% na organização, e o Actions consumiu 11,5 bilhões de minutos
   só rodando testes em 2025.
3. **Observabilidade** — a disciplina que fecha o ciclo das outras quatro Trilhas:
   depois de subir, orquestrar, provisionar e declarar, falta responder se está de pé —
   com OpenTelemetry graduado na CNCF em maio de 2026 e Prometheus em produção em 67%
   das organizações medidas.

Nenhuma das três exige conta paga em nuvem, e nenhuma exige um binário novo no PATH do
leitor além do que ele já tem.

## Método e fontes

A consulta começou pelo **Context7**, como manda a convenção do projeto: o catálogo
resolveu `/websites/prometheus_io` (2.580 snippets, reputação alta), e o formato exato
do envelope JSON da API de consulta do Prometheus veio de lá — é ele que decide se a
Trilha de Observabilidade precisa ou não de um tipo novo de Asserção.

Versões foram lidas na **API de releases do GitHub** em 2026-08-20, e não em páginas de
documentação, que antecipam versões ainda não publicadas — o mesmo cuidado registrado em
[`iac-course.md`](iac-course.md).

O sinal de mercado veio somente de pesquisas com metodologia publicada: Stack Overflow
Developer Survey 2024 e 2025, JetBrains State of Developer Ecosystem 2025, GitHub
Octoverse 2025, CNCF Annual Cloud Native Survey 2025 e Grafana Observability Survey 2025.
Onde a amostra tem viés conhecido, isso está dito na própria linha da tabela.

O filtro de viabilidade não foi feito por leitura. As três recomendações foram
**executadas nesta máquina hoje**, e as medições estão na seção de cada tema. A pergunta
decisiva — como escrever Asserção de Linux a partir de um host Windows — foi respondida
rodando systemd de verdade, não consultando documentação.

O vocabulário de Asserção foi lido no código: `Assercao.java` define o *sealed interface*
com os dezesseis tipos atuais, `MotorDeVerificacao.java` traz o `switch` exaustivo que os
avalia, e `ExecutorDeComandoReal.java` mostra o que a plataforma pode executar — um
`ProcessBuilder` direto, sem shell, com timeout de 30 segundos.

## Sinal de mercado

| Medida | Valor | Ano | Fonte primária |
|---|---|---|---|
| Bash/Shell (todos os shells), entre linguagens | **48,7%** dos respondentes | 2025 | [SO Developer Survey 2025](https://survey.stackoverflow.co/2025/technology) |
| Bash/Shell, mesma pergunta no ano anterior | 33,9% | 2024 | [SO Developer Survey 2024](https://survey.stackoverflow.co/2024/technology) |
| Ubuntu, uso profissional | 27,7% | 2024 | [SO Developer Survey 2024](https://survey.stackoverflow.co/2024/technology) |
| WSL, uso profissional | 16,8% | 2024 | [SO Developer Survey 2024](https://survey.stackoverflow.co/2024/technology) |
| APT, entre ferramentas | 18,4% | 2025 | [SO Developer Survey 2025](https://survey.stackoverflow.co/2025/technology) |
| GitHub Actions, projetos pessoais | **62%** | 2025 | [JetBrains, The State of CI/CD](https://blog.jetbrains.com/teamcity/2025/10/the-state-of-cicd/) — 805 respondentes |
| GitHub Actions, uso organizacional | **41%** | 2025 | [JetBrains, The State of CI/CD](https://blog.jetbrains.com/teamcity/2025/10/the-state-of-cicd/) |
| Organizações com dois ou mais times de CI/CD | 32% com dois, 9% com três ou mais | 2025 | [JetBrains, The State of CI/CD](https://blog.jetbrains.com/teamcity/2025/10/the-state-of-cicd/) |
| Minutos de GitHub Actions gastos rodando testes | **11,5 bilhões**, +35% no ano | 2025 | [Octoverse 2025](https://github.blog/news-insights/octoverse/what-986-million-code-pushes-say-about-the-developer-workflow-in-2025/) |
| Prometheus, entre ferramentas | 11,8% | 2025 | [SO Developer Survey 2025](https://survey.stackoverflow.co/2025/technology) |
| Prometheus em produção nas organizações | 67% | 2025 | [Grafana Observability Survey](https://grafana.com/observability-survey/2025/) — 1.255 respostas coletadas em eventos da própria Grafana; **viés amostral a favor do tema** |
| Prometheus e OpenTelemetry juntos, em produção | 34% | 2025 | [Grafana Observability Survey](https://grafana.com/observability-survey/2025/) |
| OpenTelemetry graduado na CNCF | 2026-05-21; 2ª maior velocidade entre 240+ projetos | 2026 | [CNCF](https://www.cncf.io/announcements/2026/05/21/cloud-native-computing-foundation-announces-opentelemetrys-graduation-solidifying-status-as-the-de-facto-observability-standard/) |
| Kubernetes em produção entre quem usa container | 82% (era 66% em 2023) | 2025 | [CNCF Annual Cloud Native Survey 2025](https://www.cncf.io/announcements/2026/01/20/kubernetes-established-as-the-de-facto-operating-system-for-ai-as-production-use-hits-82-in-2025-cncf-annual-cloud-native-survey/) |

Três calibrações, para as Trilhas que já existem ficarem na mesma régua: no Stack
Overflow 2025, Docker aparece em 71,1%, Kubernetes em 28,5% e Terraform em 17,8% dos
respondentes. Ou seja, `Bash/Shell` com 48,7% está **entre Docker e Kubernetes**, e as
três Trilhas recomendadas não estão abaixo do piso de popularidade das existentes.

> Ressalva de fonte: a seção de sistema operacional da edição 2025 não veio na conversão
> da página, então os percentuais de Ubuntu e WSL acima são os de **2024**, lidos na
> página primária. As leituras secundárias da edição 2025 — Windows 49,5% profissional,
> Ubuntu 27,7% profissional, WSL 16,8% profissional — apontam estabilidade, não queda,
> mas não entraram na tabela porque não puderam ser conferidas na origem.

## Auditoria da máquina em 2026-08-20

| Peça | Estado medido |
|---|---|
| Docker Engine | 29.6.1, cliente 29.6.1 |
| cgroup | **v2**, driver `cgroupfs` |
| Kernel do Docker Desktop | 6.18.33.2-microsoft-standard-WSL2 |
| WSL | 2.7.11.0, kernel 6.18.33.2; distros `docker-desktop` (rodando) e `Ubuntu-24.04` (parada) |
| Portas escutando em 8000–8069, 9000–9099 e 3000–3200 | somente a **8053** |

Os três blocos de porta propostos adiante — 8030–8039, 8040–8049 e 8060–8069 — estão
livres nesta máquina hoje, e nenhum deles encosta na 8053.

## Candidatos considerados e descartados

| Candidato | Motivo objetivo do descarte |
|---|---|
| Bancos de dados operados (PostgreSQL) | Sinal de mercado alto — 55,6% no SO 2025 — mas o tema é operação de aplicação, e a parte que é infraestrutura (volume, persistência, dado que sobrevive ao container) já é o Cenário 05 da Trilha Docker. |
| Redes e TLS | Vira o Ato de rede da Trilha Linux, não uma Trilha; como Trilha própria colidiria com o Cenário 07 de Docker e com a rede do cluster na Trilha Kubernetes. |
| Segurança de imagem, SBOM e assinatura | Já registrado no README como segunda rodada da Trilha Docker, e o Cenário 09 de Docker já cobre superfície de execução. |
| GitOps e Argo CD | Recorte da Trilha Kubernetes, e depende de um servidor Git local que a Trilha de CI/CD já entrega — cabe melhor como Ato final dela. |
| Service mesh | Recorte da Trilha Kubernetes, e o custo de RAM de um mesh sobre o kind local compete com a invariante de Cenário Ativo único. |
| Ansible | 11,7% no SO 2025 contra 17,8% de Terraform, e já descartado com justificativa em [`iac-course.md`](iac-course.md). |
| Mensageria (Kafka, RabbitMQ) | Sem sinal nas pesquisas com metodologia aberta consultadas, e tema majoritariamente de aplicação. |
| Plataforma interna e Backstage | Resolve escala organizacional que uma plataforma de usuário único não tem. |

## Ordem recomendada das Trilhas

Duas ordens diferentes, e confundi-las é o erro fácil aqui.

- **Ordem de construção** — em que sequência o autor escreve as Trilhas. É a do Resumo
  executivo: Linux, CI/CD, Observabilidade. Vale mercado e interesse.
- **Ordem de estudo** — em que sequência a plataforma sugere ler. É a de baixo. Vale
  dependência conceitual, e nada mais.

### A sequência sugerida

| # | Trilha | Por que ela vem aqui |
|---|---|---|
| 1 | **Linux** | O chão de tudo. Processo, permissão, pacote, serviço e log são o vocabulário que as seis seguintes usam sem definir. |
| 2 | **Docker** | Empacota e isola o que a anterior ensinou a operar — e explica, retroativamente, a única linha de Docker que o leitor decorou para entrar no ambiente da Trilha Linux. |
| 3 | **Kubernetes** | Orquestra os containers da anterior. |
| 4 | **Observabilidade** | Instrumenta o que já está no ar. Vem antes de CI/CD por um motivo só: não se automatiza a entrega de algo que ainda não se sabe observar. |
| 5 | **CI/CD** | Automatiza a construção e a entrega do que as quatro anteriores produzem. |
| 6 | **AWS** | O provedor, com a matriz de fidelidade do MiniStack já registrada no README. |
| 7 | **IaC** | Fecha tudo: declara em código a infraestrutura que o leitor construiu à mão nas seis anteriores. Os Cenários 13, 14 e 18 já exigem o cluster da #3, e os 15 a 18 já exigem o MiniStack da #6 — a posição não é gosto, é a dependência que o conteúdo já tem. |

A aresta discutível é a **4 contra a 5**. Observabilidade e CI/CD dependem das mesmas
três Trilhas anteriores e de nada mais, então a ordem entre elas é escolha pedagógica,
não dependência. Trocar as duas não quebra nada.

Repare que a ordem de estudo e a de construção divergem: CI/CD será escrita antes de
Observabilidade, mas é lida depois. Isso não é problema — uma Trilha que ainda não
existe simplesmente não aparece no catálogo, e como a ordem não trava nada, um buraco
temporário na sequência não impede ninguém de seguir.

### A ordem é recomendação, não trava

> Decidido e registrado na
> [ADR 0005](../adr/0005-ordem-das-trilhas-e-recomendacao.md), que é a fonte de verdade
> desta regra. O que segue é o raciocínio que levou até ela.

Essa é a parte que precisa estar escrita antes de alguém implementar, porque a tentação
de transformar ordem em pré-requisito é grande e seria uma regressão.

A plataforma já tem esse precedente decidido, e em duas camadas: a
[ADR 0003](../adr/0003-fundamentos-pertencem-a-trilha.md) diz que o Questionário
"recomenda revisão e registra compreensão, mas **não bloqueia** o início dos Cenários",
e o README repete no passo 1 do estudo que "a recomendação não bloqueia os Cenários". A
ordenação das Trilhas herda a mesma regra: ela muda o que aparece primeiro na tela e o
que a interface sugere, e **nada mais**.

Consequência de desenho: **nada de campo `requer:` ou `preRequisitos:` no
`trilha.yaml`**. Se um dia alguém quiser sinalizar dependência, que seja texto exibido
ao leitor, nunca uma condição avaliada pelo backend. Uma Trilha nunca fica cinza,
travada ou escondida por causa de outra.

### O que isso custa em código

Hoje **não existe conceito de ordem**. O `CatalogoDeTrilhas.listar()` fecha o pipeline
com `.sorted(Comparator.comparing(Trilha::id))` — ordem alfabética por id. Na prática o
catálogo mostra hoje `aws, docker, iac, kubernetes`, que não é ordem nenhuma: a Trilha
AWS abre o catálogo e a IaC, que é a última do desenho, aparece em terceiro. **A ordem
atual já está errada**, independente das Trilhas novas.

E ela erra ainda mais com o Linux: `linux` é alfabeticamente o último dos cinco ids, ou
seja, sem mudança de código a Trilha que deve abrir o catálogo apareceria no fim dele.

O conserto é pequeno e fica todo no backend mais o manifesto:

```yaml
# content/linux/trilha.yaml
id: linux
titulo: Linux
ordem: 1
fundamentos:
  titulo: Fundamentos do Linux
  artigo: fundamentos.md
  questionario: questionario.yaml
  aproveitamentoMinimo: 80
```

1. `LeitorDeTrilha` lê `ordem` e o carrega em `MetadadosDaTrilha`;
2. `Trilha` ganha o campo;
3. o comparator vira `Comparator.comparingInt(Trilha::ordem).thenComparing(Trilha::id)`
   — o desempate por id mantém o catálogo determinístico se duas Trilhas declararem o
   mesmo número, em vez de deixar a ordem depender do sistema de arquivos;
4. as quatro `trilha.yaml` existentes recebem `ordem: 2, 3, 6, 7`.

O frontend **não precisa mudar**: `Catalogo.tsx` renderiza na ordem em que
`listarTrilhas()` devolve e não reordena nada. Vale decidir se `ordem` é obrigatório ou
tem padrão — a recomendação é **obrigatório**, no mesmo espírito do diretório com
Cenários e sem `trilha.yaml` que o projeto já rejeita: uma Trilha sem posição declarada
volta a ser ordenada por acidente, que é exatamente o estado de hoje.

## Trilha 1 da construção — Linux

### Por que ela é a primeira a ser escrita

O Linux é a única tecnologia que as quatro Trilhas existentes **usam o tempo todo e não
explicam nenhuma vez**. Quem leu a Trilha Docker já escreveu `USER 10001`, `cap_drop` e
`read_only` sem ter visto o modelo de permissão que dá sentido a isso; quem leu a Trilha
Kubernetes já viu um Pod `OOMKilled` sem ter visto um cgroup; quem leu a Trilha IaC já
rodou `terraform apply` contra containers cujo PID 1 nunca foi assunto. A Trilha Linux é
a que retroativamente dá sentido às outras quatro.

E o sinal de mercado é o mais forte do conjunto: `Bash/Shell` passou de 33,9% para 48,7%
dos respondentes do Stack Overflow em um ano — quase quinze pontos — e `APT`, um
gerenciador de pacotes de distribuição Linux, aparece em 18,4%, à frente de Terraform.

### Encaixe: a primeira da ordem recomendada

Uma versão anterior desta pesquisa argumentava o contrário — que o Linux se lia **depois**
de Docker, porque é o container que torna um Cenário de Linux descartável e refazível.
Esse argumento estava errado sobre o leitor e certo sobre a máquina: ele descreve de onde
vem o **ambiente**, não o que a pessoa precisa **saber**. Quem não sabe o que é um
processo, um dono de arquivo ou um serviço não entende `USER 10001` nem `cap_drop`
quando eles aparecem no Cenário 09 de Docker — apenas copia. O Linux é o vocabulário, e
vocabulário vem primeiro.

Fica, porém, uma consequência concreta que a inversão cria e que precisa de resposta,
não de silêncio.

**O ambiente da Trilha Linux é um container, e não há terminal embutido.** Pela
[ADR 0001](../adr/0001-sem-terminal-embutido.md), o leitor executa no terminal dele. Para
entrar no ambiente, então, a primeira coisa que ele digita na plataforma inteira é:

```powershell
docker exec -it learning-infra-linux bash
```

Ou seja, um comando de Docker antes da Trilha de Docker. Três observações que resolvem
isso sem enfraquecer nem a ordem nem a honestidade:

1. **A plataforma já cobra Docker como pré-requisito de máquina, não de conhecimento.**
   O README abre exigindo Docker Desktop no ar para *tudo*. Pedir uma linha decorada não
   introduz uma dependência nova; ela já existe desde o primeiro dia.
2. **O projeto já faz exatamente isso em outros pontos.** O `docker pull` do MiniStack e
   o roteiro de criação do cluster kind são incantações que o leitor executa antes de
   entender — e ninguém considerou isso um defeito.
3. **A dívida vira gancho.** Os Fundamentos de Linux apresentam a linha como "isto te
   coloca dentro da máquina; a próxima Trilha explica cada pedaço dela", e a Trilha
   Docker paga a dívida na posição 2. Terminar uma Trilha devendo uma explicação que a
   seguinte quita é uma boa costura, não um buraco.

O que **não** se deve fazer é esconder o problema trocando o `docker exec` por um
wrapper amigável. Um comando maquiado ensinaria uma abstração falsa logo na primeira
linha da plataforma — o mesmo erro que a ADR 0001 recusou ao rejeitar o shell dentro de
container.

### Fundamentos primeiro

Pela [ADR 0003](../adr/0003-fundamentos-pertencem-a-trilha.md), a Trilha abre por
Fundamentos, e eles não preparam nem alteram ambiente. A ordem de construção segue a
mesma regra: **escrever Fundamentos e Questionário antes do primeiro Cenário**, como as
quatro Trilhas atuais fizeram, e não depois.

O contrato já está estabelecido pelas outras quatro e deve ser repetido sem invenção: 12
situações, aproveitamento recomendado de 80%, feedback por questão e tentativas
ilimitadas com o melhor resultado salvo. Duas restrições vêm do código e não da
convenção — o `LeitorDeTrilha` recusa o conteúdo se elas forem quebradas:

- cada questão precisa de um campo `revisar` apontando para uma seção `##` existente do
  artigo, comparada por slug sem acento; e
- `node frontend/scripts-checar-conteudo.mjs linux` precisa entrar no `ci.yml` ao lado
  das quatro Trilhas que já são checadas lá, senão um link `revisar` quebrado passa
  despercebido.

O que os Fundamentos de Linux precisam construir, para sustentar as seis Trilhas
seguintes: o modelo de processo e PID 1 (que a Trilha Docker vai reencontrar como
entrypoint); usuário, grupo e permissão (que reaparecem em `USER`, `cap_drop` e no
`securityContext` do Kubernetes); o sistema de arquivos como espaço único e montável
(que vira volume e `PersistentVolume`); pacote e distribuição (que vira camada de imagem);
serviço e log (que viram healthcheck, `kubectl logs` e a Trilha de Observabilidade); e
cgroup e namespace como os dois mecanismos do kernel que **são** o container — o conceito
que faz o Cenário 08 de Kubernetes, o do `OOMKilled`, deixar de ser mágica.

E os Fundamentos são o lugar de declarar a fidelidade do substrato, com a mesma
franqueza que o README já usa para o MiniStack: o ambiente é um container que compartilha
o kernel da VM, então processo, permissão, pacote, serviço, log e rede são reais, e
kernel, boot, módulo e disco não são.

### A pergunta decisiva: como as Asserções rodam a partir de um host Windows

A resposta **não** é WSL. É um container. E isso foi medido, não deduzido.

O que foi executado hoje, nesta máquina:

| Premissa | Resultado medido |
|---|---|
| systemd como PID 1 em container **sem `--privileged`** | `systemctl is-system-running` → `running` |
| Receita necessária | `--cgroupns=host --tmpfs /run --tmpfs /run/lock -v /sys/fs/cgroup:/sys/fs/cgroup:rw` |
| `--cgroupns=private`, com e sem montar `/sys/fs/cgroup` | **o container morre**; não há caminho por aí |
| `journalctl` dentro do container | responde; `Startup finished in 389ms` |
| Criar unit, `daemon-reload`, `enable --now` | `active` e `enabled` |
| `systemctl list-timers` | timer agendado e listado |
| Usuário e permissão: `install -d -m 2770 -g ana` | `stat -c "%a %U %G"` → `2770 root ana` |
| `docker exec -u ana` escrevendo no diretório do grupo | escrita permitida |
| `ip -o -4 addr` e `ip route` dentro do container | respondem |
| Ubuntu **26.04 LTS** (systemd 259) com a mesma receita | `degraded` — **uma única unit falha: `tmp.mount`** |
| Ubuntu 26.04 depois de `systemctl mask tmp.mount` | `running`; unit própria `active` e `enabled`; `journalctl` OK |

Duas consequências que mudam o desenho:

1. **`--cgroupns=host` é obrigatório.** Não é preferência: com `private` o container nem
   sobe. O "host" aqui é a VM WSL2 do Docker Desktop, não o Windows, então o alcance é a
   mesma fronteira que a plataforma já atravessa quando monta `/var/run/docker.sock`
   para o MiniStack — e é estritamente menos poder que aquilo. Ainda assim, precisa
   estar escrito no README ao lado do aviso do MiniStack.
2. **A imagem base precisa mascarar `tmp.mount`** — ou receber `--tmpfs /tmp` — para o
   sistema subir `running` no Ubuntu 26.04. Sem isso, um Cenário que ensina a ler
   `systemctl is-system-running` começa mentindo para o leitor.

**Medição de 2026-08-21, posterior à primeira redação:** a receita inteira cabe em um
`compose.yaml` — `cgroup: host`, `tmpfs` e o volume de `/sys/fs/cgroup` são campos que o
Compose já entende. Com isso o `docker compose up -d --build` sobe o systemd e o
`docker compose down -v` derruba tudo, ou seja, **a Trilha reusa `projetoCompose` e não
exige uma linha de ciclo de vida nova no `GerenciadorDeCenarioAtivo`**. Medido: primeiro
`up --build` em 68 s, seguintes em 6 s, `is-system-running` igual a `running` e nenhuma
unit falha.

WSL fica de fora como substrato por três razões concretas: exigiria `wsl --import` e
`wsl --unregister` como um ciclo de vida paralelo ao que o `GerenciadorDeCenarioAtivo` já
implementa para container, namespace e MiniStack; não tem primitiva de teardown
equivalente ao `docker rm -f`, então a invariante da
[ADR 0002](../adr/0002-um-cenario-ativo-por-vez.md) passaria a depender de código novo; e
o container já provou entregar tudo que a Trilha precisa. A distro `Ubuntu-24.04` que já
existe nesta máquina continua sendo do autor, não da plataforma.

### Como as Asserções seriam escritas

Boa parte da Trilha cabe **hoje**, sem tocar no backend, no escape hatch `comando_produz`
— que a Trilha AWS já usa nesse formato exato, com `docker exec` em um sidecar
(`content/aws/16-eks-e-ecr/verificacao.yaml`):

```yaml
- tipo: comando_produz
  comando: ["docker", "exec", "learning-infra-linux", "stat", "-c", "%a %U %G", "/srv/dados"]
  contem: "2770 root ana"
  descricao: o diretório pertence ao grupo do time e carrega o setgid
```

Mas há uma armadilha medida que impede parar por aí: `systemctl is-active` de uma unit
parada imprime **`inactive`**, e `"inactive".contains("active")` é verdadeiro. Uma
Asserção com `contem: active` **aprovaria um serviço parado** — falso positivo
estrutural, exatamente o que a [ADR 0002](../adr/0002-um-cenario-ativo-por-vez.md) existe
para impedir. Daí dois tipos novos, no estilo pequeno e fixo do vocabulário atual:

```yaml
- tipo: servico_systemd
  nome: coletor.service
  ativo: true
  habilitado: true
  descricao: o coletor sobe sozinho depois de um boot
- tipo: arquivo_linux
  caminho: /etc/sudoers.d/plantao
  modo: "440"
  dono: root
  descricao: a regra de sudo não é editável por quem ela beneficia
```

`servico_systemd` roda `systemctl is-active --quiet` e `systemctl is-enabled --quiet` e
compara **códigos de saída** — medidos hoje: `0` para ativo, `3` para parado. Nada de
substring. `arquivo_linux` roda `stat -c` e compara campo a campo, com campos nulos não
verificados, como `container_configuracao` já faz.

O nome do container não aparece no YAML da Asserção: vem do frontmatter do Cenário
(`containerLinux: learning-infra-linux`) e é injetado pelo
`LeitorDeCenario`, exatamente como `diretorioTerraform` e como contexto e namespace do
Kubernetes. O `switch` exaustivo do `MotorDeVerificacao` vai quebrar a compilação até os
dois casos novos serem tratados, como o README já avisa — e isso é proposital.

### Versões a pinar

| Peça | Versão | Data |
|---|---|---|
| Imagem base | `ubuntu:26.04` (Resolute Raccoon) | LTS lançada em [2026-04-23](https://canonical.com/blog/canonical-releases-ubuntu-26-04-lts-resolute-raccoon), suporte até 2031 |
| systemd na imagem | 259 (259.5-0ubuntu3.4) | medido hoje dentro do container |
| Alternativa conservadora | `ubuntu:24.04` | subiu `running` sem mascarar unit nenhuma |

Pinar por digest, como o README já faz com o MiniStack, para uma atualização da tag não
mudar a Trilha em silêncio.

### Bloco de portas

**8040–8049.** Medido livre hoje. A maior parte dos Cenários não publica porta —
permissão, processo, pacote e log não precisam de porta. As poucas que precisam são o
Ato de rede e serviço: 8040 para o serviço gerenciado por systemd, 8041 para o segundo
serviço da comparação, 8042 para o proxy.

### Riscos

- **Kernel compartilhado.** O container não tem kernel próprio: `sysctl`, módulo,
  `dmesg` e boot real não são ensináveis. É a mesma honestidade que a Trilha AWS já
  pratica na tabela de fidelidade do README, e precisa da mesma tabela.
- **A receita de cgroup pode quebrar numa atualização do Docker Desktop.** Ela depende de
  `--cgroupns=host` continuar disponível. Mitigação: um teste no estilo do
  `CatalogoRealTest` que sobe a imagem e exige `is-system-running` igual a `running`,
  para a quebra aparecer na suíte e não no meio de um Cenário.
- **Tentação de escopo.** Linux é grande o suficiente para virar quarenta Cenários. O
  recorte precisa ser decidido antes de escrever o primeiro.

## Trilha 2 da construção — CI/CD e entrega contínua

### Por que ela é a segunda a ser escrita

É a prática de infraestrutura com maior presença no dia a dia depois de container, e a
evidência é volumétrica, não declarada: o GitHub Actions consumiu **11,5 bilhões de
minutos só rodando testes em 2025**, 35% mais que no ano anterior. Na pesquisa da
JetBrains, 62% usam Actions em projeto pessoal e 41% na organização — e 32% das
organizações mantêm **dois** times de CI/CD, o que diz que o tema é mais sobre entender o
modelo do que sobre decorar o YAML de um fornecedor.

Vale registrar a colisão de escopo: o README anuncia "CI/CD, scan, SBOM e cadeia de
fornecimento" como segunda rodada da Trilha Docker. A recomendação aqui é **separar**:
scan, SBOM e assinatura são sobre o artefato e continuam sendo Docker; CI/CD é sobre o
ciclo que produz o artefato, e não cabe como apêndice de outra Trilha.

### Encaixe: posição 5 da ordem recomendada

É a Trilha que amarra as anteriores: o pipeline constrói a imagem que a Trilha Docker
ensinou, aplica o manifesto que a Trilha Kubernetes ensinou e é observado pela Trilha que
vem imediatamente antes dela. E é a primeira em que o leitor não digita o comando que
produz o efeito — ele empurra código e algo acontece sozinho. Essa inversão é a lição.

O `plan` do Terraform dentro de um pipeline é um Cenário natural, mas ele **não** pode
entrar aqui: a Trilha IaC está na posição 7 e um Cenário na 5 não pode depender dela.
Esse assunto pertence ao Ato final da própria Trilha IaC, onde o leitor já tem as duas
metades.

### Como as Asserções rodariam — medido hoje, de ponta a ponta

O substrato é um forge local: **Gitea 1.27.2 com Actions ligado, mais o `act_runner`**,
que executa workflows no dialeto do GitHub Actions. O ciclo completo foi executado hoje
nesta máquina.

| Premissa | Resultado medido |
|---|---|
| Gitea sobe com `GITEA__actions__ENABLED=true` e `INSTALL_LOCK` | `/api/v1/version` → `{"version":"1.27.2"}` |
| Criar o usuário no Iniciar, sem tela de instalação | `gitea admin user create` → sucesso |
| Token de registro do runner, por CLI | `gitea actions generate-runner-token` → token |
| Registro do runner | `Runner registered successfully`; `act_runner v0.6.1`; labels `ubuntu-latest`, `ubuntu-24.04`, `ubuntu-22.04` |
| Push dispara execução e ela conclui | `"status":"completed"`, `"conclusion":"success"` |
| **Primeira execução, imagem fria** | **143 s** |
| Execução seguinte, imagem em cache | **6 s** |
| RAM em repouso | Gitea 225 MiB, runner 13 MiB |
| Imagem do runner | `docker.gitea.com/runner-images:ubuntu-latest`, **2,34 GB** |
| API de execuções **sem** cabeçalho de autorização | **401** `{"message":"token is required"}` |

Esse 401 é o achado que muda o desenho. `http_corpo_contem` não envia cabeçalho — o
`MotorDeVerificacao` monta a requisição com `HttpRequest.newBuilder(uri).GET()` e nada
mais. Então a Trilha **exige um tipo novo**, com o token injetado pelo backend, no molde
exato de `aws_consulta`, que já injeta endpoint, região e credenciais sintéticas e não
deixa o conteúdo substituí-los:

```yaml
- tipo: pipeline_gitea
  repositorio: entrega
  fluxo: ci.yaml
  conclusao: success
  timeout: 120
  descricao: o push disparou a esteira e ela terminou verde
```

O `timeout` existe porque a execução é assíncrona: diferente de container e de state do
Terraform, aqui o leitor empurra e vai embora. Convergência por espera já é o padrão da
casa — `kubernetes_condicao` e `kubernetes_jsonpath` esperam por até dez segundos em vez
de depender de `sleep` fixo, e este tipo faz o mesmo com um teto maior.

Uma Asserção sozinha não basta, pela mesma regra contra falso positivo que a Trilha IaC
adotou: toda Asserção de esteira verde vem acompanhada de uma Asserção do **efeito** —
`imagem_existe` para o artefato que o pipeline construiu, ou `http_responde` para o que
ele publicou. Esteira verde sem efeito é pipeline que não faz nada.

### Versões a pinar

| Peça | Versão | Data |
|---|---|---|
| Gitea | v1.27.2 | publicada em 2026-08-13 |
| `act_runner` | v0.6.1 | medido no daemon em execução hoje; o repositório em `gitea.com` não expõe releases pela API |
| Imagem do runner | `docker.gitea.com/runner-images:ubuntu-latest` | pinar por digest — 2,34 GB, mesmo tratamento do MiniStack |

### Bloco de portas

**8030–8039.** Medido livre hoje. 8030 para o Gitea (HTTP e Git), 8031 para a aplicação
que o pipeline publica, 8032 para o registry local do Cenário de publicação de imagem. O
Cenário 10 de Docker já usa a 5000 para o registry dele, e essa porta não deve ser
reusada: os dois ambientes precisam poder coexistir enquanto o autor escreve.

### Riscos

- **2,34 GB de imagem do runner.** Vira pré-requisito de `docker pull` no README, como já
  é o do MiniStack. Sem ele, o primeiro Cenário leva 143 s e parece travado.
- **O runner monta `/var/run/docker.sock`.** Mesmo poder que a plataforma já concede ao
  MiniStack nos Cenários com `infraestruturaRealAws`, com o mesmo aviso: só rodar o
  conteúdo versionado do projeto.
- **Assincronia é fonte de instabilidade.** O `timeout` do tipo novo precisa ser generoso,
  e a mensagem de reprovação precisa distinguir "ainda rodando" de "terminou vermelho" —
  são diagnósticos opostos para o leitor.
- **Gitea não é GitHub.** O dialeto de workflow é compatível; o ecossistema de actions não
  é. Os Fundamentos precisam nomear a diferença, como a Trilha IaC nomeia o OpenTofu.

## Trilha 3 da construção — Observabilidade

### Por que ela é a terceira a ser escrita

É a disciplina que fecha o ciclo das quatro Trilhas existentes. Depois de subir
container, orquestrar workload, provisionar nuvem e declarar tudo em código, resta a
pergunta que nenhuma delas responde: **está de pé mesmo?** Quem terminou o Cenário 08 de
Docker sabe que `healthy` não é `ready`; falta o passo em que a saúde vira número, série
temporal e alerta.

O sinal de mercado é mais estreito que o dos dois primeiros — Prometheus aparece em 11,8%
dos respondentes do Stack Overflow, contra 71,1% de Docker — e é por isso que a Trilha é
a terceira e não a segunda. Mas é um sinal firme onde importa: OpenTelemetry graduou na
CNCF em 2026-05-21 com a segunda maior velocidade entre mais de 240 projetos, e a
pesquisa da Grafana mede Prometheus em produção em 67% das organizações. Essa última tem
viés de amostra declarado — coletada em eventos da própria Grafana — e por isso não
sustenta sozinha a recomendação.

### Encaixe: posição 4 da ordem recomendada

Entra depois de Kubernetes, porque o cluster `docker-desktop` que aquela Trilha já exige
é o alvo mais interessante de instrumentação, e depois de Docker, porque o Ato inicial
observa containers antes de observar Pods. É também a Trilha que melhor aproveita a
Dificuldade `Mestre`: um ambiente em que a métrica mente é um Cenário de diagnóstico que
nenhuma outra Trilha consegue montar.

Vem **antes** de CI/CD por uma razão de sequência, não de dependência técnica: não se
automatiza a entrega de algo que ainda não se sabe observar. É a aresta discutível da
ordem, e trocar as duas não quebra nenhum Cenário.

### Como as Asserções rodariam — medido hoje

Esta é a Trilha com o menor custo de verificação das três, porque o Prometheus expõe o
estado observado por HTTP e o `MotorDeVerificacao` já sabe fazer GET.

| Premissa | Resultado medido |
|---|---|
| Prometheus 3.14.0 raspando dois alvos | `/-/ready` → 200 |
| `GET /api/v1/query?query=up` | `{"status":"success",…,"result":[{"metric":{…},"value":[<ts>,"1"]}…]}` |
| `count(up{job="nodeexp"}==1)` com a URL percent-encoded | `{"status":"success",…,"value":[<ts>,"1"]}` |
| A mesma URL com `{` e `"` **crus**, pelo `HttpClient` do Java | `IllegalArgumentException: Illegal character in query at index 49` |
| `/api/v1/targets?state=active` | lista alvos e rótulos |

Duas consequências:

1. **A URL da Asserção precisa vir percent-encoded no YAML.** `URI.create` recusa `{` e
   `"` crus, e o `MotorDeVerificacao` chama `URI.create` direto. Isso é regra de autoria,
   não defeito — mas precisa estar escrita, porque uma PromQL sem rótulo funciona e uma
   com rótulo estoura.
2. **O corpo carrega o timestamp do instante da consulta.** Um `contem` casaria contra um
   texto que muda a cada chamada. Dá para escapar mirando `"status":"success"`, mas isso
   prova que a consulta rodou, não que o valor é o esperado — falso positivo.

Por isso, **um tipo novo, e só um**:

```yaml
- tipo: metrica_prometheus
  consulta: count(up == 1)
  esperado: "3"
  timeout: 30
  descricao: os três alvos declarados estão sendo raspados
```

Ele codifica a consulta, chama `/api/v1/query`, extrai o escalar de `value[1]` e compara.
O endpoint do Prometheus é injetado pelo backend a partir do frontmatter
(`prometheus: true`), como o endpoint do MiniStack já é — o conteúdo não escolhe contra
quem a Asserção fala. O `timeout` cobre o intervalo de raspagem: uma métrica que ainda
não foi coletada não é uma métrica errada.

Fora esse tipo, a Trilha reusa o vocabulário existente sem inventar nada:
`container_rodando` para o coletor, `http_responde` para `/metrics` e `/-/ready`,
`http_corpo_contem` para o corpo de `/metrics` da aplicação instrumentada, e
`comando_produz` com `docker exec` para inspecionar a configuração do coletor OTLP.

O formato exato do envelope JSON está na
[documentação da API de consulta do Prometheus](https://prometheus.io/docs/prometheus/latest/querying/api),
lida via Context7.

### Versões a pinar

| Peça | Versão | Publicada em |
|---|---|---|
| Prometheus | v3.14.0 | 2026-08-18 |
| Grafana | v13.2.0 | 2026-08-18 |
| Alertmanager | v0.34.0 | 2026-08-16 |
| `node_exporter` | v1.12.1 | 2026-07-14 |
| Loki | v3.7.6 | 2026-08-06 |
| OpenTelemetry Collector | v0.159.0 | 2026-08-18 |

Fonte: API de releases oficial de cada repositório, lida em 2026-08-20.

Prometheus 3.14.0 e Grafana 13.2.0 têm **dois dias** de publicados. Foi com o 3.14.0 que
as medições acima rodaram, então ele está provado nesta máquina; ainda assim, se aparecer
regressão durante a escrita, os alvos conservadores são Prometheus **v3.13.2**
(2026-07-30) e Grafana **v13.1.4** (2026-08-18).

### Bloco de portas

**8060–8069.** Medido livre hoje. Prometheus na 8060, Grafana na 8061, Alertmanager na
8062, Loki na 8063, OTLP/HTTP do coletor na 8064, aplicação instrumentada na 8065,
`node_exporter` e cAdvisor na 8066 e 8067.

Remapear em vez de usar 9090 e 3000 não é preciosismo de convenção: a 3000 é a porta mais
disputada de uma máquina de desenvolvimento, e o README já registra por que a porta óbvia
é a mais arriscada — quando ela está ocupada por outra aplicação, a Asserção recebe
resposta do app errado e reporta algo que parece defeito do Cenário. As portas internas
dos containers continuam sendo as canônicas; só a publicação no host muda.

### Riscos

- **Métrica é assíncrona.** Entre a ação do leitor e a raspagem passa um intervalo. Todo
  Cenário precisa declarar `scrape_interval` curto e toda Asserção precisa de janela de
  convergência, senão a Verificação reprova quem acertou.
- **Ambição de escopo.** Métrica, log, trace, perfil, alerta e painel não cabem em uma
  Trilha. A recomendação é métrica e alerta como espinha dorsal, log como Ato curto,
  trace só no Ato final com OpenTelemetry, e perfil de fora.
- **Custo de RAM.** Prometheus, Grafana, Loki, coletor e alvos somam bastante. A
  invariante de Cenário Ativo único ajuda, mas não elimina; medir antes de desenhar o
  Cenário final.

## O que fica de fora

- **WSL como substrato de Cenário.** Não tem primitiva de teardown equivalente ao
  `docker rm -f`, e a invariante da [ADR 0002](../adr/0002-um-cenario-ativo-por-vez.md)
  depende de teardown ruidoso. O container provou entregar systemd, journald, timer,
  usuário e permissão — não há o que ganhar.
- **Terminal embutido, em qualquer forma.** As três Trilhas foram desenhadas para
  respeitar a [ADR 0001](../adr/0001-sem-terminal-embutido.md): toda Asserção proposta
  aqui afirma **estado do mundo**, nunca histórico de comando. Nenhuma delas precisa
  saber o que o leitor digitou.
- **Kernel, boot e hardware na Trilha Linux.** `sysctl`, módulo, `dmesg`, initramfs e
  disco não são verificáveis num container que compartilha o kernel da VM. Entram nos
  Fundamentos como mapa do território, com a mesma tabela de fidelidade que o README já
  usa para o MiniStack.
- **Actions do GitHub que dependem do GitHub.** Cache, artifact hospedado, environment e
  approval não existem no forge local. Assunto de Fundamentos, não de Cenário.
- **Trace distribuído com mais de dois serviços, e perfil contínuo.** Custo de RAM
  desproporcional ao retorno numa máquina de usuário único.
- **Qualquer mutação dentro de uma Verificação.** A regra que a Trilha IaC estabeleceu
  vale para as três: a Verificação observa, nunca aplica, nunca destrói.

## O que precisa ser medido antes de escrever a primeira Trilha

1. Confirmar que a receita de cgroup sobrevive a um restart do Docker Desktop, e não só a
   um `docker run` na sessão atual.
2. Medir `docker exec` como Asserção sob carga: trinta Asserções seguidas contra o mesmo
   container, dentro do timeout de 30 s do `ExecutorDeComandoReal`.
3. Confirmar que `systemctl mask tmp.mount` no `Dockerfile` da imagem base entrega
   `running` no Ubuntu 26.04 sem efeito colateral em `systemd-tmpfiles`.
4. Medir o Iniciar de um Cenário de CI/CD ponta a ponta: Gitea sobe, usuário criado,
   runner registrado e repositório semeado — hoje o Gitea sozinho levou mais de 20 s para
   responder à API.
5. Confirmar que o `act_runner` reconecta sozinho quando o Gitea é recriado pelo teardown,
   ou tratar o par como uma unidade que sobe e desce junta.
6. Medir RAM e CPU do Cenário mais pesado de Observabilidade, com Prometheus, Grafana,
   Loki, coletor e dois alvos simultâneos.
7. Reconferir os três blocos de porta no dia em que cada Trilha for escrita — a 8053 já
   está ocupada por algo, e o vizinho de amanhã pode ser outro.
8. ~~Medir `docker exec -it` como sessão interativa de verdade.~~ **Medido em
   2026-08-21, no PowerShell do autor: aprovado.** TTY em `/dev/pts/0`, `TERM=xterm`,
   `locale charmap=UTF-8`, `stty size` indo de `30 120` para `30 146` ao redimensionar a
   janela — o SIGWINCH atravessa —, acento digitado voltando íntegro, `less` e `nano`
   abrindo e saindo limpos, e `Ctrl-C` atingindo somente o processo em primeiro plano,
   com o `jobs` seguinte mostrando `[1]+ Running sleep 300 &`.

   A medição achou de quebra um defeito que não estava previsto: a imagem subia com
   `LANG` vazio e `locale charmap` igual a `ANSI_X3.4-1968`. Corrigido com
   `ENV LANG=C.UTF-8`, que resolve sem instalar o pacote `locales`.
9. Confirmar que tornar `ordem` obrigatório no `trilha.yaml` não derruba
   `CatalogoDeTrilhasTest` nem `CatalogoRealTest` sem que as quatro `trilha.yaml`
   existentes sejam atualizadas no mesmo commit.
