package dev.learninginfra.ciclodevida;

import dev.learninginfra.conteudo.Cenario;
import dev.learninginfra.conteudo.Dificuldade;
import dev.learninginfra.conteudo.RepositorioDeCenarios;
import dev.learninginfra.execucao.ExecutorDeComando;
import dev.learninginfra.execucao.SaidaDeComando;
import dev.learninginfra.progresso.RepositorioDeProgresso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GerenciadorDeCenarioAtivoTest {

    @TempDir
    Path raiz;

    private final List<List<String>> comandosExecutados = new ArrayList<>();

    private final ExecutorDeComando executorEspiao = comando -> {
        comandosExecutados.add(comando);
        return new SaidaDeComando(0, "", "");
    };

    private Cenario cenario(String id, List<String> containers) throws Exception {
        Path diretorio = raiz.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(diretorio.resolve("workspace").resolve("site"));
        Files.writeString(diretorio.resolve("workspace").resolve("site").resolve("index.html"), "<h1>ola</h1>");
        return new Cenario(id, "titulo", Dificuldade.GUIADO, containers, "# corpo", diretorio, List.of());
    }

    private GerenciadorDeCenarioAtivo gerenciador(RepositorioDeCenarios repositorio) {
        return new GerenciadorDeCenarioAtivo(
                raiz.resolve("work").toString(),
                executorEspiao,
                new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString()),
                repositorio);
    }

    private Cenario cenarioCompose(String id, String projeto) throws Exception {
        Path diretorio = raiz.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(diretorio.resolve("workspace"));
        return new Cenario(id, "titulo", Dificuldade.GUIADO, List.of(), "# corpo",
                diretorio, List.of(), projeto);
    }

    private Cenario cenarioComposeComArquivo(String id, String projeto) throws Exception {
        Path diretorio = raiz.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(diretorio.resolve("workspace"));
        Files.writeString(diretorio.resolve("workspace").resolve("compose.yaml"),
                "services:\n  alfa:\n    image: alpine\n");
        return new Cenario(id, "titulo", Dificuldade.GUIADO, List.of(), "# corpo",
                diretorio, List.of(), projeto);
    }

    private Cenario cenarioKubernetes(String id, String namespace, boolean comSetup) throws Exception {
        Path diretorio = raiz.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(diretorio.resolve("workspace"));
        if (comSetup) {
            Files.createDirectories(diretorio.resolve("workspace/setup"));
            Files.writeString(diretorio.resolve("workspace/setup/app.yaml"), "kind: Pod\n");
        }
        return new Cenario(id, "titulo", Dificuldade.GUIADO, List.of(), "# corpo",
                diretorio, List.of(), null, List.of(), "docker-desktop", namespace,
                comSetup ? "setup" : null);
    }

    private Cenario cenarioTerraformKubernetes(String id, String namespace) throws Exception {
        Path diretorio = raiz.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(diretorio.resolve("workspace"));
        return new Cenario(id, "titulo", Dificuldade.GUIADO, List.of(), "# corpo",
                diretorio, List.of(), null, List.of(), "docker-desktop", namespace,
                null, false, false, null, true, ".");
    }

    private Cenario cenarioAws(String id, boolean infraestruturaReal, boolean comInit) throws Exception {
        Path diretorio = raiz.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(diretorio.resolve("workspace"));
        if (comInit) {
            Files.createDirectories(diretorio.resolve("workspace/init"));
            Files.writeString(diretorio.resolve("workspace/init/00-setup.sh"), "echo pronto\n");
        }
        return new Cenario(id, "titulo", Dificuldade.GUIADO, List.of(), "# corpo",
                diretorio, List.of(), null, List.of(), null, null, null,
                true, infraestruturaReal, comInit ? "init" : null);
    }

    @Test
    void iniciarRecriaNamespaceEAplicaAmbienteInicialKubernetes() throws Exception {
        Cenario cenario = cenarioKubernetes("kubernetes/06", "learning-infra-k8s-06", true);
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        Path trabalho = gerenciador(repositorio).iniciar(cenario);

        assertEquals(List.of(
                List.of("kubectl", "--context", "docker-desktop", "get", "--raw=/readyz",
                        "--request-timeout=5s"),
                List.of("kubectl", "--context", "docker-desktop", "delete", "namespace",
                        "learning-infra-k8s-06", "--ignore-not-found=true", "--wait=true",
                        "--timeout=20s"),
                List.of("kubectl", "--context", "docker-desktop", "create", "namespace",
                        "learning-infra-k8s-06"),
                List.of("kubectl", "--context", "docker-desktop", "--namespace",
                        "learning-infra-k8s-06", "apply", "-f",
                        trabalho.resolve("setup").toString())), comandosExecutados);
    }

    @Test
    void iniciarKubernetesFalhaComInstrucaoQuandoClusterEstaInacessivel() throws Exception {
        Cenario cenario = cenarioKubernetes("kubernetes/01", "learning-infra-k8s-01", false);
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        ExecutorDeComando semCluster = comando ->
                new SaidaDeComando(1, "", "contexto docker-desktop não encontrado");
        var gerenciador = new GerenciadorDeCenarioAtivo(
                raiz.resolve("work").toString(), semCluster,
                new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString()),
                repositorio);

        var erro = assertThrows(IllegalStateException.class, () -> gerenciador.iniciar(cenario));

        assertTrue(erro.getMessage().contains("Docker Desktop"));
        assertTrue(erro.getMessage().contains("docker-desktop"));
    }

    @Test
    void trocarDeCenarioKubernetesRemoveNamespaceAnterior() throws Exception {
        Cenario primeiro = cenarioKubernetes("kubernetes/01", "learning-infra-k8s-01", false);
        Cenario segundo = cenario("docker/01", List.of());
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("kubernetes/01")).thenReturn(Optional.of(primeiro));
        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        gerenciador.iniciar(primeiro);
        comandosExecutados.clear();

        gerenciador.iniciar(segundo);

        assertEquals(List.of(List.of(
                "kubectl", "--context", "docker-desktop", "delete", "namespace",
                "learning-infra-k8s-01", "--ignore-not-found=true", "--wait=true",
                "--timeout=20s")), comandosExecutados);
    }

    @Test
    void iniciarCenarioTerraformDeixaONamespaceNascerDoApplyDoLeitor() throws Exception {
        Cenario cenario = cenarioTerraformKubernetes("iac/13", "learning-infra-iac-13");
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        gerenciador(repositorio).iniciar(cenario);

        assertEquals(List.of(
                List.of("kubectl", "--context", "docker-desktop", "get", "--raw=/readyz",
                        "--request-timeout=5s"),
                List.of("kubectl", "--context", "docker-desktop", "delete", "namespace",
                        "learning-infra-iac-13", "--ignore-not-found=true", "--wait=true",
                        "--timeout=20s")), comandosExecutados);
    }

    @Test
    void iniciarAwsSobeMinistackPinadoEAguardaInicializacao() throws Exception {
        Cenario cenario = cenarioAws("aws/11-rds", true, true);
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        Path trabalho = gerenciador(repositorio).iniciar(cenario);

        List<String> dockerRun = comandosExecutados.get(2);
        assertEquals(List.of("docker", "rm", "-f", "-v", "learning-infra-ministack"),
                comandosExecutados.get(0));
        assertEquals(List.of("docker", "ps", "-aq", "--filter", "label=ministack"),
                comandosExecutados.get(1));
        assertTrue(dockerRun.contains("--pull=never"));
        assertTrue(dockerRun.contains("/var/run/docker.sock:/var/run/docker.sock"));
        assertTrue(dockerRun.stream().anyMatch(argumento ->
                argumento.contains(trabalho.resolve("init").toString())
                        && argumento.contains("/etc/localstack/init/ready.d")));
        assertTrue(dockerRun.getLast().contains("ministackorg/ministack:1.4.13-full@sha256:"));
        assertEquals(List.of("docker", "exec", "learning-infra-ministack"),
                comandosExecutados.get(3).subList(0, 3));
    }

    @Test
    void iniciarAwsSemInfraestruturaRealNaoMontaSocketDocker() throws Exception {
        Cenario cenario = cenarioAws("aws/02-s3", false, false);
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        gerenciador(repositorio).iniciar(cenario);

        List<String> dockerRun = comandosExecutados.get(2);
        assertFalse(dockerRun.contains("/var/run/docker.sock:/var/run/docker.sock"));
    }

    @Test
    void iniciarAwsRemoveSidecarsDescobertosPorLabel() throws Exception {
        Cenario cenario = cenarioAws("aws/11-rds", true, false);
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        String idSidecar = "1c8164819eef";
        ExecutorDeComando executorComSobra = comando -> {
            comandosExecutados.add(comando);
            if (comando.equals(List.of(
                    "docker", "ps", "-aq", "--filter", "label=ministack"))) {
                return new SaidaDeComando(0, idSidecar + "\n", "");
            }
            return new SaidaDeComando(0, "", "");
        };
        var gerenciador = new GerenciadorDeCenarioAtivo(
                raiz.resolve("work").toString(), executorComSobra,
                new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString()),
                repositorio);

        gerenciador.iniciar(cenario);

        assertTrue(comandosExecutados.contains(
                List.of("docker", "rm", "-f", "-v", idSidecar)));
    }

    @Test
    void reiniciarMesmoCenarioAwsRemoveContainerESidecarsAntesDeSubirOutro() throws Exception {
        Cenario cenario = cenarioAws("aws/14-ecs", true, false);
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("aws/14-ecs")).thenReturn(Optional.of(cenario));
        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        gerenciador.iniciar(cenario);
        comandosExecutados.clear();

        gerenciador.iniciar(cenario);

        assertEquals(List.of("docker", "rm", "-f", "-v", "learning-infra-ministack"),
                comandosExecutados.get(0));
        assertEquals(List.of("docker", "ps", "-aq", "--filter", "label=ministack"),
                comandosExecutados.get(1));
        assertTrue(comandosExecutados.stream().anyMatch(comando -> comando.contains("run")));
    }

    @Test
    void iniciarSobeOComposeQuandoOWorkspaceTrazUm() throws Exception {
        Cenario cenario = cenarioComposeComArquivo("docker/04", "lab-04");
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        Path trabalho = gerenciador(repositorio).iniciar(cenario);

        assertEquals(
                List.of(List.of("docker", "compose", "-p", "lab-04",
                        "-f", trabalho.resolve("compose.yaml").toString(),
                        "up", "-d", "--build")),
                comandosExecutados);
    }

    @Test
    void iniciarNaoSobeComposeQuandoOWorkspaceNaoTrazArquivo() throws Exception {
        Cenario cenario = cenarioCompose("docker/03", "lab-03");
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        gerenciador(repositorio).iniciar(cenario);

        assertTrue(comandosExecutados.stream().noneMatch(c -> c.contains("up")));
    }

    @Test
    void falhaDoComposeUpEhRuidosa() throws Exception {
        Cenario cenario = cenarioComposeComArquivo("docker/04", "lab-04");
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        ExecutorDeComando executorQueFalha = comando ->
                comando.contains("up") ? new SaidaDeComando(1, "", "imagem nao encontrada")
                                       : new SaidaDeComando(0, "", "");
        var gerenciador = new GerenciadorDeCenarioAtivo(
                raiz.resolve("work").toString(),
                executorQueFalha,
                new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString()),
                repositorio);

        var erro = assertThrows(IllegalStateException.class, () -> gerenciador.iniciar(cenario));
        assertTrue(erro.getMessage().contains("lab-04"));
    }

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

    @Test
    void iniciarDerrubaOProjetoComposeDoCenarioAnterior() throws Exception {
        Cenario primeiro = cenarioCompose("docker/03", "lab-03");
        Cenario segundo = cenario("docker/04", List.of());
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/03")).thenReturn(Optional.of(primeiro));

        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        gerenciador.iniciar(primeiro);
        comandosExecutados.clear();
        gerenciador.iniciar(segundo);

        assertEquals(
                List.of(List.of("docker", "compose", "-p", "lab-03", "down", "-v")),
                comandosExecutados);
    }

    @Test
    void cenarioSemProjetoComposeNaoChamaCompose() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        Cenario segundo = cenario("docker/02", List.of());
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/01")).thenReturn(Optional.of(primeiro));

        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        gerenciador.iniciar(primeiro);
        comandosExecutados.clear();
        gerenciador.iniciar(segundo);

        assertTrue(comandosExecutados.stream().noneMatch(c -> c.contains("compose")));
    }

    @Test
    void falhaDoComposeDownEhRuidosa() throws Exception {
        Cenario primeiro = cenarioCompose("docker/03", "lab-03");
        Cenario segundo = cenario("docker/04", List.of());
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/03")).thenReturn(Optional.of(primeiro));

        ExecutorDeComando executorQueFalha = comando ->
                comando.contains("compose") ? new SaidaDeComando(1, "", "daemon fora do ar")
                                            : new SaidaDeComando(0, "", "");
        var gerenciador = new GerenciadorDeCenarioAtivo(
                raiz.resolve("work").toString(),
                executorQueFalha,
                new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString()),
                repositorio);

        gerenciador.iniciar(primeiro);

        var erro = assertThrows(IllegalStateException.class, () -> gerenciador.iniciar(segundo));
        assertTrue(erro.getMessage().contains("lab-03"));
    }

    @Test
    void materializaOWorkspaceNoDiretorioDeTrabalho() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        Path trabalho = gerenciador(repositorio).iniciar(primeiro);

        assertTrue(trabalho.isAbsolute());
        assertTrue(Files.exists(trabalho.resolve("site/index.html")));
        assertEquals("<h1>ola</h1>", Files.readString(trabalho.resolve("site/index.html")));
    }

    @Test
    void iniciarDerrubaOsContainersDoCenarioAnterior() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        Cenario segundo = cenario("docker/02", List.of("lab-api"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/01")).thenReturn(Optional.of(primeiro));

        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        gerenciador.iniciar(primeiro);
        comandosExecutados.clear();
        gerenciador.iniciar(segundo);

        assertEquals(List.of(List.of("docker", "rm", "-f", "lab-web")), comandosExecutados);
    }

    @Test
    void iniciarLimpaSobrasDoWorkspaceAnterior() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        Cenario segundo = cenario("docker/02", List.of("lab-api"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/01")).thenReturn(Optional.of(primeiro));

        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        Path trabalho = gerenciador.iniciar(primeiro);
        Files.writeString(trabalho.resolve("lixo.txt"), "sobra");

        gerenciador.iniciar(segundo);

        assertFalse(Files.exists(trabalho.resolve("lixo.txt")));
    }

    @Test
    void oCenarioAtivoSobreviveAUmNovoGerenciador() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        gerenciador(repositorio).iniciar(primeiro);

        assertEquals(Optional.of("docker/01"), gerenciador(repositorio).cenarioAtivo());
    }

    @Test
    void marcarConcluidoRegistraNoProgresso() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);

        gerenciador.iniciar(primeiro);
        gerenciador.marcarConcluido(primeiro);

        assertTrue(new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString())
                .carregar().concluidos().containsKey("docker/01"));
    }

    @Test
    void falhaDeTeardownEhRuidosa() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        Cenario segundo = cenario("docker/02", List.of("lab-api"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/01")).thenReturn(Optional.of(primeiro));

        ExecutorDeComando executorQueFalha = comando ->
                comando.contains("rm") ? new SaidaDeComando(1, "", "daemon fora do ar")
                                       : new SaidaDeComando(0, "", "");
        var gerenciador = new GerenciadorDeCenarioAtivo(
                raiz.resolve("work").toString(),
                executorQueFalha,
                new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString()),
                repositorio);

        gerenciador.iniciar(primeiro);

        var erro = assertThrows(IllegalStateException.class, () -> gerenciador.iniciar(segundo));
        assertTrue(erro.getMessage().contains("lab-web"));
    }
}
