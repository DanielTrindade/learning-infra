package dev.learninginfra.ciclodevida;

import dev.learninginfra.conteudo.Cenario;
import dev.learninginfra.conteudo.RepositorioDeCenarios;
import dev.learninginfra.execucao.ExecutorDeComando;
import dev.learninginfra.execucao.SaidaDeComando;
import dev.learninginfra.progresso.RepositorioDeProgresso;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class GerenciadorDeCenarioAtivo {

    private static final String CONTAINER_MINISTACK = "learning-infra-ministack";
    private static final String IMAGEM_MINISTACK = "ministackorg/ministack:1.4.13-full@sha256:07c3bc4b0eeb8f68669f2352cd624ccb09b477ab78b8b21ba1461f2035f3f610";
    private static final String SCRIPT_ESPERA_MINISTACK = """
            import json, sys, time, urllib.request
            for _ in range(48):
                try:
                    with urllib.request.urlopen('http://127.0.0.1:4566/_ministack/ready', timeout=1) as resposta:
                        estado = json.load(resposta)
                    if estado.get('status') == 'completed':
                        sys.exit(0 if estado.get('failed', 0) == 0 else 2)
                except Exception:
                    pass
                time.sleep(0.5)
            sys.exit(1)
            """;

    private final Path diretorioDeTrabalho;
    private final ExecutorDeComando executor;
    private final RepositorioDeProgresso progressos;
    private final RepositorioDeCenarios cenarios;

    public GerenciadorDeCenarioAtivo(
            @Value("${learninginfra.diretorio-de-trabalho}") String diretorioDeTrabalho,
            ExecutorDeComando executor,
            RepositorioDeProgresso progressos,
            RepositorioDeCenarios cenarios) {
        this.diretorioDeTrabalho = Path.of(diretorioDeTrabalho);
        this.executor = executor;
        this.progressos = progressos;
        this.cenarios = cenarios;
    }

    public Optional<String> cenarioAtivo() {
        return Optional.ofNullable(progressos.carregar().cenarioAtivo());
    }

    /** Derruba o Cenário anterior, materializa o workspace deste e o torna o Cenário Ativo. */
    public Path iniciar(Cenario cenario) {
        cenarioAtivo()
                .flatMap(cenarios::buscar)
                .ifPresent(this::derrubar);

        Path trabalho = diretorioDeTrabalho.toAbsolutePath().normalize();
        apagarRecursivamente(trabalho);
        copiarWorkspace(cenario, trabalho);
        subirCompose(cenario, trabalho);
        prepararKubernetes(cenario, trabalho);
        prepararAws(cenario, trabalho);

        progressos.salvar(progressos.carregar().comAtivo(cenario.id()));
        return trabalho;
    }

    private void prepararAws(Cenario cenario, Path trabalho) {
        if (!cenario.usaAws()) {
            return;
        }

        limparAwsLocal("preparar um ambiente limpo");

        List<String> comando = new java.util.ArrayList<>(List.of(
                "docker", "run", "--detach", "--pull=never",
                "--name", CONTAINER_MINISTACK,
                "--label", "learning-infra.aws=true",
                "--publish", "127.0.0.1:4566:4566",
                "--env", "PERSIST_STATE=0",
                "--env", "S3_PERSIST=0",
                "--env", "RDS_PERSIST=0"));

        if (cenario.infraestruturaRealAws()) {
            comando.addAll(List.of(
                    "--volume", "/var/run/docker.sock:/var/run/docker.sock"));
        }

        if (cenario.temInicializacaoAws()) {
            Path inicializacao = trabalho.resolve(cenario.inicializacaoAws()).normalize();
            if (!inicializacao.startsWith(trabalho) || !Files.isDirectory(inicializacao)) {
                throw new IllegalStateException(
                        "inicializacaoAws aponta para fora do workspace ou não é um diretório: "
                        + cenario.inicializacaoAws());
            }
            comando.addAll(List.of(
                    "--mount", "type=bind,source=" + inicializacao
                            + ",target=/etc/localstack/init/ready.d,readonly"));
        }

        comando.add(IMAGEM_MINISTACK);
        SaidaDeComando iniciado = executor.executar(List.copyOf(comando));
        if (!iniciado.sucesso()) {
            throw new IllegalStateException(
                    "não consegui iniciar o MiniStack para o Cenário " + cenario.id() + ": "
                    + ultimoDetalhe(iniciado) + " — confirme que a imagem `" + IMAGEM_MINISTACK
                    + "` foi baixada e que a porta 4566 está livre");
        }

        SaidaDeComando pronto = executor.executar(List.of(
                "docker", "exec", CONTAINER_MINISTACK,
                "python", "-c", SCRIPT_ESPERA_MINISTACK));
        if (!pronto.sucesso()) {
            SaidaDeComando logs = executor.executar(List.of(
                    "docker", "logs", "--tail", "20", CONTAINER_MINISTACK));
            throw new IllegalStateException(
                    "o MiniStack não concluiu a inicialização do Cenário " + cenario.id() + ": "
                    + ultimoDetalhe(logs) + " — consulte `docker logs " + CONTAINER_MINISTACK + "`");
        }
    }

    /** Sobe o stack quando o workspace materializado traz um compose.yaml. */
    private void subirCompose(Cenario cenario, Path trabalho) {
        Path arquivo = trabalho.resolve("compose.yaml");
        if (!cenario.usaCompose() || !Files.isRegularFile(arquivo)) {
            return;
        }
        SaidaDeComando saida = executor.executar(List.of(
                "docker", "compose", "-p", cenario.projetoCompose(),
                "-f", arquivo.toString(), "up", "-d", "--build"));
        if (!saida.sucesso()) {
            throw new IllegalStateException(
                    "não consegui subir o projeto Compose `" + cenario.projetoCompose()
                    + "` do Cenário " + cenario.id() + ": " + saida.stderr().strip()
                    + " — o ambiente deste Cenário não está pronto");
        }
    }

    /**
     * Recria o namespace exclusivo do Cenário e, quando declarado, aplica o ambiente
     * inicial. O contexto é sempre explícito para um laboratório nunca atingir por
     * acidente outro cluster configurado no kubectl do autor.
     *
     * <p>Em Cenário de Terraform o namespace não é criado adiantado: ele nasce do
     * `terraform apply` do leitor, porque o código é dono do objeto — criá-lo antes
     * faria o apply falhar com "already exists". A remoção prévia continua valendo,
     * para o apply nunca encontrar sobra de uma execução anterior.
     */
    private void prepararKubernetes(Cenario cenario, Path trabalho) {
        if (!cenario.usaKubernetes()) {
            return;
        }

        SaidaDeComando pronto = executor.executar(List.of(
                "kubectl", "--context", cenario.contextoKubernetes(),
                "get", "--raw=/readyz", "--request-timeout=5s"));
        if (!pronto.sucesso()) {
            throw new IllegalStateException(
                    "o cluster Kubernetes do contexto `" + cenario.contextoKubernetes()
                    + "` não está acessível — crie ou inicie o cluster Kubernetes no Docker "
                    + "Desktop antes de começar este Cenário: " + ultimoDetalhe(pronto));
        }

        removerNamespace(cenario, "recriar o ambiente");

        if (cenario.terraform()) {
            return;
        }

        SaidaDeComando criado = executor.executar(List.of(
                "kubectl", "--context", cenario.contextoKubernetes(),
                "create", "namespace", cenario.namespaceKubernetes()));
        if (!criado.sucesso()) {
            throw new IllegalStateException(
                    "não consegui criar o namespace `" + cenario.namespaceKubernetes()
                    + "` do Cenário " + cenario.id() + ": " + ultimoDetalhe(criado));
        }

        if (!cenario.temManifestosIniciais()) {
            return;
        }

        Path manifestos = trabalho.resolve(cenario.manifestosIniciais()).normalize();
        if (!manifestos.startsWith(trabalho) || !Files.exists(manifestos)) {
            throw new IllegalStateException(
                    "manifestosIniciais aponta para fora do workspace ou não existe: "
                    + cenario.manifestosIniciais());
        }

        SaidaDeComando aplicado = executor.executar(List.of(
                "kubectl", "--context", cenario.contextoKubernetes(),
                "--namespace", cenario.namespaceKubernetes(),
                "apply", "-f", manifestos.toString()));
        if (!aplicado.sucesso()) {
            throw new IllegalStateException(
                    "não consegui aplicar o ambiente inicial do Cenário " + cenario.id()
                    + ": " + ultimoDetalhe(aplicado));
        }
    }

    public void marcarConcluido(Cenario cenario) {
        progressos.salvar(progressos.carregar()
                .comConcluido(cenario.id(), Instant.now().toString()));
    }

    private void derrubar(Cenario anterior) {
        if (anterior.usaCompose()) {
            SaidaDeComando saida = executor.executar(
                    List.of("docker", "compose", "-p", anterior.projetoCompose(), "down", "-v"));
            if (!saida.sucesso()) {
                throw new IllegalStateException(
                        "não consegui derrubar o projeto Compose `" + anterior.projetoCompose()
                        + "` do Cenário " + anterior.id() + ": " + saida.stderr().strip()
                        + " — resolva isso antes de continuar, senão a próxima Verificação pode "
                        + "passar por sobra de ambiente");
            }
        }
        for (String container : anterior.containers()) {
            SaidaDeComando saida = executor.executar(List.of("docker", "rm", "-f", container));
            if (!saida.sucesso() && !pareceInexistente(saida)) {
                throw new IllegalStateException(
                        "não consegui remover o container `" + container + "` do Cenário "
                        + anterior.id() + ": " + saida.stderr().strip()
                        + " — resolva isso antes de continuar, senão a próxima Verificação pode "
                        + "passar por sobra de ambiente");
            }
        }
        // Volumes por último: um volume em uso por container vivo não é removível.
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
        if (anterior.usaKubernetes()) {
            removerNamespace(anterior, "derrubar o Cenário anterior");
        }
        if (anterior.usaAws()) {
            limparAwsLocal("derrubar o Cenário AWS anterior");
        }
    }

    private void limparAwsLocal(String acao) {
        SaidaDeComando principal = executor.executar(
                List.of("docker", "rm", "-f", "-v", CONTAINER_MINISTACK));
        if (!principal.sucesso() && !pareceInexistente(principal)) {
            throw new IllegalStateException(
                    "não consegui remover o container do MiniStack para " + acao + ": "
                    + ultimoDetalhe(principal));
        }

        SaidaDeComando listagem = executor.executar(List.of(
                "docker", "ps", "-aq", "--filter", "label=ministack"));
        if (!listagem.sucesso()) {
            throw new IllegalStateException(
                    "não consegui localizar os sidecars do MiniStack para " + acao + ": "
                    + ultimoDetalhe(listagem));
        }

        for (String linha : listagem.stdout().lines().toList()) {
            String id = linha.strip();
            if (id.matches("[0-9a-f]{12,64}")) {
                SaidaDeComando removido = executor.executar(
                        List.of("docker", "rm", "-f", "-v", id));
                if (!removido.sucesso() && !pareceInexistente(removido)) {
                    throw new IllegalStateException(
                            "não consegui remover o sidecar `" + id + "` do MiniStack para "
                            + acao + ": " + ultimoDetalhe(removido));
                }
            }
        }
    }

    /**
     * Um namespace com workloads em encerramento gracioso pode ficar em `Terminating`
     * por mais de vinte segundos (o padrão de graceful termination de um Pod é 30s).
     * O timeout curto fazia um início de Cenário falhar por um namespace que estava
     * apenas terminando. O comando recebe um teto próprio do executor, maior que o
     * timeout do próprio kubectl.
     */
    private static final Duration LIMITE_REMOCAO_NAMESPACE = Duration.ofSeconds(90);

    private void removerNamespace(Cenario cenario, String acao) {
        SaidaDeComando saida = executor.executar(List.of(
                "kubectl", "--context", cenario.contextoKubernetes(),
                "delete", "namespace", cenario.namespaceKubernetes(),
                "--ignore-not-found=true", "--wait=true", "--timeout=60s"),
                LIMITE_REMOCAO_NAMESPACE);
        if (saida.sucesso()) {
            return;
        }

        // O `delete --wait` pode estourar o tempo com o namespace ainda terminando em
        // segundo plano. Antes de decretar falha, confere o estado real do recurso.
        SaidaDeComando verificacao = executor.executar(List.of(
                "kubectl", "--context", cenario.contextoKubernetes(),
                "get", "namespace", cenario.namespaceKubernetes(),
                "-o", "jsonpath={.status.phase}", "--request-timeout=10s"));
        if (pareceInexistente(verificacao)) {
            return; // já sumiu; a remoção terminou enquanto o delete esperava
        }
        if (verificacao.sucesso() && verificacao.stdout().contains("Terminating")) {
            SaidaDeComando espera = executor.executar(List.of(
                    "kubectl", "--context", cenario.contextoKubernetes(),
                    "wait", "--for=delete", "namespace", cenario.namespaceKubernetes(),
                    "--timeout=60s"), LIMITE_REMOCAO_NAMESPACE);
            if (espera.sucesso()) {
                return;
            }
        }

        throw new IllegalStateException(
                "não consegui remover o namespace `" + cenario.namespaceKubernetes()
                + "` para " + acao + ": " + ultimoDetalhe(saida)
                + " — o namespace ainda existe; confira os finalizers com `kubectl --context "
                + cenario.contextoKubernetes() + " get namespace "
                + cenario.namespaceKubernetes() + " -o yaml` e remova o que sobrar "
                + "antes de continuar, para a próxima Verificação não passar por sobra "
                + "de ambiente");
    }

    private String ultimoDetalhe(SaidaDeComando saida) {
        String texto = saida.stderr().isBlank() ? saida.stdout() : saida.stderr();
        String[] linhas = texto.strip().split("\\R");
        return linhas.length == 0 || linhas[linhas.length - 1].isBlank()
                ? "sem detalhe"
                : linhas[linhas.length - 1].strip();
    }

    private boolean pareceInexistente(SaidaDeComando saida) {
        String erro = saida.stderr().toLowerCase();
        return erro.contains("no such container")
                || erro.contains("no such object")
                || erro.contains("not found");
    }

    private void copiarWorkspace(Cenario cenario, Path destino) {
        Path origem = cenario.workspace();
        try {
            Files.createDirectories(destino);
            if (!Files.isDirectory(origem)) {
                return;
            }
            try (Stream<Path> caminhos = Files.walk(origem)) {
                for (Path caminho : caminhos.toList()) {
                    Path alvo = destino.resolve(origem.relativize(caminho).toString());
                    if (Files.isDirectory(caminho)) {
                        Files.createDirectories(alvo);
                    } else {
                        Files.createDirectories(alvo.getParent());
                        Files.copy(caminho, alvo);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui materializar o workspace de " + cenario.id(), e);
        }
    }

    private void apagarRecursivamente(Path raiz) {
        if (!Files.exists(raiz)) {
            return;
        }
        try {
            Files.walkFileTree(raiz, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path arquivo, BasicFileAttributes atributos) throws IOException {
                    Files.delete(arquivo);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path diretorio, IOException erro) throws IOException {
                    Files.delete(diretorio);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui limpar " + raiz, e);
        }
    }
}
