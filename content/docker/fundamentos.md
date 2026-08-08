# Fundamentos do Docker

Docker é uma forma de empacotar, distribuir e executar software com um contrato de
ambiente mais previsível. Antes de decorar comandos, vale construir um modelo mental:
o que é produzido por um build, quem executa esse artefato, quais partes são isoladas e
onde os dados realmente vivem.

Esta leitura prepara os Cenários práticos. Ela não substitui o terminal: ao terminar,
faça o Questionário para descobrir o que revisar e siga para os laboratórios mesmo que
ainda não tenha atingido 80%. A recomendação não é um bloqueio.

## O problema de consistência e entrega

Uma aplicação raramente é apenas seu código. Ela depende de runtime, bibliotecas do
sistema, certificados, arquivos de configuração e uma forma correta de iniciar o
processo. Quando cada máquina recebe essas partes manualmente, dois ambientes que
parecem iguais divergem: uma versão muda, um pacote fica ausente, uma configuração é
aplicada de outro jeito. O resultado clássico é “funciona na minha máquina”.

Docker tornou comum entregar essas dependências como uma **imagem** versionável. O
mesmo artefato construído na integração contínua pode ser testado, publicado e
executado em outro ambiente. Isso não garante, sozinho, que toda implantação será
idêntica — configuração externa, arquitetura de CPU, kernel, rede e dados ainda
importam —, mas reduz muito a parte invisível instalada à mão.

Containers também são rápidos de criar e descartar. Em vez de manter uma instalação
mutável por meses, uma equipe pode substituir uma execução por outra produzida a partir
de uma imagem conhecida. Essa ideia de substituir, em vez de consertar manualmente uma
instância, é mais importante do que qualquer flag da CLI.

## Containers e máquinas virtuais

Uma máquina virtual emula hardware e executa seu próprio sistema operacional, inclusive
um kernel. Já um container Linux normalmente é um conjunto de processos isolados que
compartilha o kernel Linux do host. Por isso ele tende a iniciar mais rápido e consumir
menos recursos que uma VM completa. Container não é uma “VM pequena”: o limite de
isolamento e o modelo operacional são diferentes.

No Windows e no macOS, o Docker Desktop precisa oferecer um kernel Linux para executar
containers Linux. Ele faz isso dentro de uma VM gerenciada. A comparação continua
válida: vários containers compartilham o kernel dessa VM; cada container não recebe uma
VM própria.

Escolher containers não elimina VMs. Em produção, é comum executar muitos containers
em hosts que são VMs. A VM cria uma fronteira de infraestrutura; o container empacota e
isola processos de aplicação. São camadas que podem se complementar.

## Como o Docker funciona

Ao executar `docker run`, a CLI não cria o container diretamente. Ela atua como cliente
e chama a API do Docker Engine. O daemon `dockerd` recebe a intenção, gerencia imagens,
redes, volumes e containers e coordena componentes de runtime. Em uma visão um pouco
mais detalhada, `containerd` gerencia o ciclo de vida da execução e um runtime compatível
com OCI prepara e inicia os processos isolados.

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

O cliente pode falar com um daemon local ou remoto. Isso é poderoso e também explica
por que acesso ao socket do Docker equivale a uma permissão altamente privilegiada: a
pessoa ou processo que controla o daemon pode criar mounts, iniciar containers e
alcançar recursos do host.

Um **registry** é o serviço que armazena e distribui imagens. Docker Hub é um exemplo,
mas organizações usam registries privados e podem executar um registry local. O daemon
baixa uma imagem quando ela ainda não existe localmente e envia imagens quando você faz
`push`.

## Imagens containers e camadas

Um Dockerfile descreve etapas para construir uma imagem. A imagem resultante é um
artefato somente leitura, composto por camadas. Instruções de build podem produzir
novas camadas, e o BuildKit reutiliza resultados válidos do cache. Se uma etapa inicial
muda, as etapas dependentes precisam ser refeitas; por isso a ordem do Dockerfile e o
contexto enviado ao build afetam muito o tempo de construção.

Um container é uma **instância executável** de uma imagem, combinada com configuração:
comando, variáveis de ambiente, rede, mounts e limites de recursos. Ao iniciar, ele
recebe uma camada gravável própria sobre as camadas somente leitura. Dois containers da
mesma imagem compartilham a base, mas não a camada gravável.

Essa camada gravável pertence ao container. Remover e recriar o container remove os
dados que existiam apenas nela. Uma nova versão da imagem também não altera um
container já criado: para adotar o artefato novo, substitua a execução.

Cache não é garantia de correção. Copiar o repositório inteiro antes de instalar
dependências faz qualquer edição invalidar cedo o cache. Um `.dockerignore` reduz o
contexto e evita enviar arquivos desnecessários. Builds multi-stage separam ferramentas
de compilação da imagem final. E `ARG` ou `ENV` não são cofres: segredos podem aparecer
em metadados, histórico, cache ou camadas; use secret mounts e o mecanismo de segredos
do ambiente de execução.

## Processos isolamento e limites

O container vive enquanto seu processo principal vive. Se esse processo termina, o
container para, mesmo que tenha iniciado tarefas em background. O processo de PID 1
também precisa tratar sinais e encerrar corretamente; um shell intermediário mal
configurado pode impedir que a aplicação receba o sinal de parada.

No Linux, **namespaces** oferecem visões isoladas de processos, rede, mounts, hostname e
outros recursos. **cgroups** contabilizam e limitam CPU, memória e I/O. O filesystem em
camadas completa o ambiente que o processo enxerga. Essas técnicas isolam, mas não
criam uma fronteira perfeita: os containers compartilham kernel, e uma falha de kernel
ou configuração excessivamente privilegiada pode atravessar o limite esperado.

Rodar como `root` dentro do container não é automaticamente igual a root no host, mas
aumenta o impacto de uma fuga ou configuração insegura. Prefira usuário não-root,
capabilities mínimas, filesystem somente leitura quando possível e limites explícitos.
Evite `--privileged` e não monte o socket do Docker sem uma necessidade cuidadosamente
avaliada.

## Rede e publicação de portas

Containers conectados à mesma rede criada pelo usuário encontram-se pelo nome. No
Compose, cada serviço normalmente entra em uma rede do projeto e usa o nome do serviço
como DNS. A aplicação deve conversar com `db:5432`, por exemplo, não com `localhost`:
dentro do container, `localhost` aponta para o próprio container.

Publicar uma porta cria um caminho do host para o container, como `8080:80`. Isso é
diferente de `EXPOSE`: a instrução no Dockerfile documenta a porta esperada, mas não a
publica. Serviços internos, como bancos, não precisam ter portas publicadas apenas para
que outros containers os acessem.

Também importa em qual endereço do host a porta é publicada. `127.0.0.1:8080:80`
restringe o acesso à máquina local; `8080:80` pode escutar em todas as interfaces,
dependendo da configuração. Redes separadas permitem que só a borda converse com o
mundo externo enquanto banco e serviços internos permanecem inacessíveis diretamente.

## Volumes e persistência

Dados que precisam sobreviver à substituição do container devem ficar fora da camada
gravável. Um **volume nomeado** é gerenciado pelo Docker e tem ciclo de vida independente
do container. Um **bind mount** liga um caminho específico do host a um caminho dentro
do container. Ambos persistem dados, mas têm usos e responsabilidades diferentes.

Bind mounts são úteis para editar código ou fornecer arquivos locais durante o
desenvolvimento, porém acoplam a execução à estrutura e às permissões do host. Volumes
costumam ser melhores para dados gerenciados pela aplicação, como o diretório de um
banco local. Persistir não significa proteger: backup, restauração, migração e permissão
continuam necessários.

Mounts podem esconder o conteúdo que já existia no caminho da imagem. Se um volume é
montado sobre `/app/config`, por exemplo, os arquivos daquele diretório na imagem podem
deixar de ser visíveis. Ao diagnosticar “arquivo sumiu”, inspecione os mounts antes de
reconstruir a imagem.

## Compose rodando não significa pronto

Docker Compose descreve vários serviços, suas imagens ou builds, redes, volumes e
configuração. `depends_on` em sua forma curta organiza a ordem de início, mas não prova
que a dependência está pronta para atender. Um processo de banco pode estar `running`
enquanto ainda recupera dados e rejeita conexões.

Um `healthcheck` transforma uma verificação real — por exemplo, uma consulta ou endpoint
de saúde — em estado observado pelo Docker. Com `depends_on` e
`condition: service_healthy`, o Compose pode aguardar a prontidão inicial. Isso melhora
a inicialização local, mas não substitui retry e tolerância a falhas na aplicação: uma
dependência pode ficar indisponível depois que todos os containers já começaram.

Estado `running` responde “o processo principal ainda existe?”. Estado `healthy`
responde à checagem que você definiu. Uma checagem superficial produz confiança falsa;
ela deve testar a capacidade necessária pelo consumidor sem causar efeitos colaterais.

## Construir publicar e executar

O ciclo completo vai além de `docker build` e `docker run`:

1. escrever o Dockerfile e escolher um contexto mínimo;
2. construir e testar a imagem;
3. nomear o artefato para o registry e repositório corretos;
4. publicar com `push`;
5. baixar com `pull` em outro ambiente;
6. executar a configuração desejada;
7. observar logs, saúde e uso de recursos;
8. substituir ou reverter a execução.

Uma **tag** como `1.4` ou `latest` é um ponteiro legível e pode passar a apontar para
outro conteúdo. Um **digest** identifica o conteúdo da imagem. Tags são boas para fluxo
humano; digests são importantes quando a implantação precisa fixar exatamente o
artefato aprovado. Uma estratégia comum publica tags úteis, registra o digest produzido
e implanta por referência imutável.

Não coloque credenciais na imagem. O build deve produzir um artefato que possa ser
promovido entre ambientes, enquanto configurações e segredos entram no momento da
execução por mecanismos apropriados.

## Da teoria aos Cenários

Os Cenários seguintes transformam o modelo mental em evidência na sua máquina:

- **01 — Servir um HTML seu com nginx:** processo, porta e bind mount;
- **02 — Empacotar uma app num Dockerfile:** Dockerfile, imagem, build e cache;
- **03 — Dois containers conversando com Compose:** serviços, DNS e rede;
- **04 — Um stack que não sobe:** diagnóstico com `ps`, `logs` e `config`;
- **05 — Dados que sobrevivem ao container:** camada gravável, bind mount e volume.

A expansão planejada acrescentará otimização e multi-stage, redes internas, healthcheck,
execução com privilégios mínimos, registry e rollback por digest, encerrando com um
incidente integrado. O contrato de Fundamentos e Questionário será aplicado também a
Kubernetes, AWS e futuras Trilhas; muda o conteúdo, não a experiência.

Use o Questionário como recuperação ativa: responda sem procurar no texto, veja o
feedback e volte apenas às seções indicadas. Depois, abra o terminal. Compreensão sem
prática é frágil; prática sem modelo mental vira tentativa e erro.

### Referências para continuar

- [Visão geral do Docker](https://docs.docker.com/get-started/docker-overview/)
- [Boas práticas de build](https://docs.docker.com/build/building/best-practices/)
- [Segurança do Docker Engine](https://docs.docker.com/engine/security/)
- [Ordem de inicialização no Compose](https://docs.docker.com/compose/how-tos/startup-order/)
