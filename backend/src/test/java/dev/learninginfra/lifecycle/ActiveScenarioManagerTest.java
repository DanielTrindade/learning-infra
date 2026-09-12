package dev.learninginfra.lifecycle;

import dev.learninginfra.content.Scenario;
import dev.learninginfra.content.Difficulty;
import dev.learninginfra.content.ScenarioRepository;
import dev.learninginfra.execution.CommandExecutor;
import dev.learninginfra.execution.CommandOutput;
import dev.learninginfra.progress.Progress;
import dev.learninginfra.progress.ProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ActiveScenarioManagerTest {

    @TempDir
    Path root;

    private final List<List<String>> comandosExecutados = new ArrayList<>();

    private final CommandExecutor executorEspiao = command -> {
        comandosExecutados.add(command);
        return new CommandOutput(0, "", "");
    };

    private Scenario scenario(String id, List<String> containers) throws Exception {
        Path directory = root.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(directory.resolve("workspace").resolve("site"));
        Files.writeString(directory.resolve("workspace").resolve("site").resolve("index.html"), "<h1>ola</h1>");
        return Scenario.simples(id, "titulo", Difficulty.GUIDED, containers,
                "# corpo", directory, List.of());
    }

    private ActiveScenarioManager gerenciador(ScenarioRepository repositorio) {
        return new ActiveScenarioManager(
                root.resolve("work").toString(),
                executorEspiao,
                new ProgressRepository(root.resolve("data/progresso.json").toString()),
                repositorio);
    }

    private Scenario comInfra(
            String id, Path directory,
            Scenario.ComposeConfiguration compose,
            Scenario.KubernetesConfiguration kubernetes,
            Scenario.AwsConfiguration aws,
            Scenario.TerraformConfiguration terraform,
            Scenario.LinuxConfiguration linux) {
        return new Scenario(id, "titulo", Difficulty.GUIDED, List.of(), "# corpo", directory,
                List.of(), compose, kubernetes, aws, terraform, linux);
    }

    private Scenario cenarioCompose(String id, String projeto) throws Exception {
        Path directory = root.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(directory.resolve("workspace"));
        return comInfra(id, directory, Scenario.ComposeConfiguration.de(projeto),
                Scenario.KubernetesConfiguration.nenhuma(), Scenario.AwsConfiguration.nenhuma(),
                Scenario.TerraformConfiguration.nenhuma(), Scenario.LinuxConfiguration.nenhuma());
    }

    private Scenario cenarioComposeComArquivo(String id, String projeto) throws Exception {
        Path directory = root.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(directory.resolve("workspace"));
        Files.writeString(directory.resolve("workspace").resolve("compose.yaml"),
                "services:\n  alfa:\n    image: alpine\n");
        return comInfra(id, directory, Scenario.ComposeConfiguration.de(projeto),
                Scenario.KubernetesConfiguration.nenhuma(), Scenario.AwsConfiguration.nenhuma(),
                Scenario.TerraformConfiguration.nenhuma(), Scenario.LinuxConfiguration.nenhuma());
    }

    private Scenario cenarioKubernetes(String id, String namespace, boolean comSetup) throws Exception {
        Path directory = root.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(directory.resolve("workspace"));
        if (comSetup) {
            Files.createDirectories(directory.resolve("workspace/setup"));
            Files.writeString(directory.resolve("workspace/setup/app.yaml"), "kind: Pod\n");
        }
        return comInfra(id, directory, Scenario.ComposeConfiguration.nenhuma(),
                new Scenario.KubernetesConfiguration("docker-desktop", namespace,
                        comSetup ? "setup" : null),
                Scenario.AwsConfiguration.nenhuma(), Scenario.TerraformConfiguration.nenhuma(),
                Scenario.LinuxConfiguration.nenhuma());
    }

    private Scenario cenarioTerraformKubernetes(String id, String namespace) throws Exception {
        Path directory = root.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(directory.resolve("workspace"));
        return comInfra(id, directory, Scenario.ComposeConfiguration.nenhuma(),
                new Scenario.KubernetesConfiguration("docker-desktop", namespace, null),
                Scenario.AwsConfiguration.nenhuma(), new Scenario.TerraformConfiguration(true, "."),
                Scenario.LinuxConfiguration.nenhuma());
    }

    private Scenario cenarioAws(String id, boolean infraestruturaReal, boolean comInit) throws Exception {
        Path directory = root.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(directory.resolve("workspace"));
        if (comInit) {
            Files.createDirectories(directory.resolve("workspace/init"));
            Files.writeString(directory.resolve("workspace/init/00-setup.sh"), "echo pronto\n");
        }
        return comInfra(id, directory, Scenario.ComposeConfiguration.nenhuma(),
                Scenario.KubernetesConfiguration.nenhuma(),
                new Scenario.AwsConfiguration(true, infraestruturaReal, comInit ? "init" : null),
                Scenario.TerraformConfiguration.nenhuma(), Scenario.LinuxConfiguration.nenhuma());
    }

    @Test
    void iniciarRecriaNamespaceEAplicaAmbienteInicialKubernetes() throws Exception {
        Scenario scenario = cenarioKubernetes("kubernetes/06", "learning-infra-k8s-06", true);
        var repositorio = Mockito.mock(ScenarioRepository.class);

        Path work = gerenciador(repositorio).start(scenario);

        assertEquals(List.of(
                List.of("kubectl", "--context", "docker-desktop", "get", "--raw=/readyz",
                        "--request-timeout=5s"),
                List.of("kubectl", "--context", "docker-desktop", "delete", "namespace",
                        "learning-infra-k8s-06", "--ignore-not-found=true", "--wait=true",
                        "--timeout=60s"),
                List.of("kubectl", "--context", "docker-desktop", "create", "namespace",
                        "learning-infra-k8s-06"),
                List.of("kubectl", "--context", "docker-desktop", "--namespace",
                        "learning-infra-k8s-06", "apply", "-f",
                        work.resolve("setup").toString())), comandosExecutados);
    }

    @Test
    void iniciarKubernetesFalhaComInstrucaoQuandoClusterEstaInacessivel() throws Exception {
        Scenario scenario = cenarioKubernetes("kubernetes/01", "learning-infra-k8s-01", false);
        var repositorio = Mockito.mock(ScenarioRepository.class);
        CommandExecutor semCluster = command ->
                new CommandOutput(1, "", "contexto docker-desktop não encontrado");
        var gerenciador = new ActiveScenarioManager(
                root.resolve("work").toString(), semCluster,
                new ProgressRepository(root.resolve("data/progresso.json").toString()),
                repositorio);

        var error = assertThrows(IllegalStateException.class, () -> gerenciador.start(scenario));

        assertTrue(error.getMessage().contains("Docker Desktop"));
        assertTrue(error.getMessage().contains("docker-desktop"));
    }

    @Test
    void trocarDeCenarioKubernetesRemoveNamespaceAnterior() throws Exception {
        Scenario primeiro = cenarioKubernetes("kubernetes/01", "learning-infra-k8s-01", false);
        Scenario segundo = scenario("docker/01", List.of());
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("kubernetes/01")).thenReturn(Optional.of(primeiro));
        ActiveScenarioManager gerenciador = gerenciador(repositorio);
        gerenciador.start(primeiro);
        comandosExecutados.clear();

        gerenciador.start(segundo);

        assertEquals(List.of(List.of(
                "kubectl", "--context", "docker-desktop", "delete", "namespace",
                "learning-infra-k8s-01", "--ignore-not-found=true", "--wait=true",
                "--timeout=60s")), comandosExecutados);
    }

    @Test
    void iniciarCenarioTerraformDeixaONamespaceNascerDoApplyDoLeitor() throws Exception {
        Scenario scenario = cenarioTerraformKubernetes("iac/13", "learning-infra-iac-13");
        var repositorio = Mockito.mock(ScenarioRepository.class);

        gerenciador(repositorio).start(scenario);

        assertEquals(List.of(
                List.of("kubectl", "--context", "docker-desktop", "get", "--raw=/readyz",
                        "--request-timeout=5s"),
List.of("kubectl", "--context", "docker-desktop", "delete", "namespace",
                        "learning-infra-iac-13", "--ignore-not-found=true", "--wait=true",
                        "--timeout=60s")), comandosExecutados);
    }

    @Test
    void namespacePresoEmTerminatingNaoBloqueiaAEsperaQueConclui() throws Exception {
        Scenario primeiro = cenarioKubernetes("kubernetes/01", "learning-infra-k8s-01", false);
        Scenario segundo = scenario("docker/01", List.of());
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("kubernetes/01")).thenReturn(Optional.of(primeiro));
        CommandExecutor executorComNamespaceTerminando = command -> {
            if (command.contains("delete") && command.contains("namespace")) {
                return new CommandOutput(1, "",
                        "error: timed out waiting for the condition on namespaces/learning-infra-k8s-01");
            }
            if (command.contains("get") && command.contains("namespace")) {
                return new CommandOutput(0, "Terminating", "");
            }
            return new CommandOutput(0, "", "");
        };
        var gerenciador = new ActiveScenarioManager(
                root.resolve("work").toString(), executorComNamespaceTerminando,
                new ProgressRepository(root.resolve("data/progresso.json").toString()),
                repositorio);
        gerenciador.start(primeiro);
        comandosExecutados.clear();

        assertDoesNotThrow(() -> gerenciador.start(segundo));
    }

    @Test
    void namespaceJaRemovidoAposTimeoutNaoBloqueiaOIniciar() throws Exception {
        Scenario primeiro = cenarioKubernetes("kubernetes/01", "learning-infra-k8s-01", false);
        Scenario segundo = scenario("docker/01", List.of());
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("kubernetes/01")).thenReturn(Optional.of(primeiro));
        CommandExecutor executorComNamespaceSumido = command -> {
            if (command.contains("delete") && command.contains("namespace")) {
                return new CommandOutput(1, "",
                        "error: timed out waiting for the condition on namespaces/learning-infra-k8s-01");
            }
            if (command.contains("get") && command.contains("namespace")) {
                return new CommandOutput(1, "",
                        "Error from server (NotFound): namespaces \"learning-infra-k8s-01\" not found");
            }
            return new CommandOutput(0, "", "");
        };
        var gerenciador = new ActiveScenarioManager(
                root.resolve("work").toString(), executorComNamespaceSumido,
                new ProgressRepository(root.resolve("data/progresso.json").toString()),
                repositorio);
        gerenciador.start(primeiro);
        comandosExecutados.clear();

        assertDoesNotThrow(() -> gerenciador.start(segundo));
    }

    @Test
    void namespacePresoEmTerminatingAposEsperaFalhaComInstrucaoDeFinalizers() throws Exception {
        Scenario primeiro = cenarioKubernetes("kubernetes/01", "learning-infra-k8s-01", false);
        Scenario segundo = scenario("docker/01", List.of());
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("kubernetes/01")).thenReturn(Optional.of(primeiro));
        var deletes = new AtomicInteger();
        CommandExecutor executorComNamespacePreso = command -> {
            if (command.contains("delete") && command.contains("namespace")) {
                if (deletes.getAndIncrement() == 0) {
                    return new CommandOutput(0, "", ""); // prepara o primeiro, deixa subir
                }
                return new CommandOutput(1, "",
                        "error: timed out waiting for the condition on namespaces/learning-infra-k8s-01");
            }
            if (command.contains("wait") && command.contains("namespace")) {
                return new CommandOutput(1, "",
                        "error: timed out waiting for the condition on namespaces/learning-infra-k8s-01");
            }
            if (command.contains("get") && command.contains("namespace")) {
                return new CommandOutput(0, "Terminating", "");
            }
            return new CommandOutput(0, "", "");
        };
        var gerenciador = new ActiveScenarioManager(
                root.resolve("work").toString(), executorComNamespacePreso,
                new ProgressRepository(root.resolve("data/progresso.json").toString()),
                repositorio);
        gerenciador.start(primeiro);
        comandosExecutados.clear();

        var error = assertThrows(IllegalStateException.class, () -> gerenciador.start(segundo));

        assertTrue(error.getMessage().contains("learning-infra-k8s-01"));
        assertTrue(error.getMessage().contains("finalizers"));
    }

    @Test
    void iniciarAwsSobeMinistackPinadoEAguardaInicializacao() throws Exception {
        Scenario scenario = cenarioAws("aws/11-rds", true, true);
        var repositorio = Mockito.mock(ScenarioRepository.class);

        Path work = gerenciador(repositorio).start(scenario);

        List<String> dockerRun = comandosExecutados.get(2);
        assertEquals(List.of("docker", "rm", "-f", "-v", "learning-infra-ministack"),
                comandosExecutados.get(0));
        assertEquals(List.of("docker", "ps", "-aq", "--filter", "label=ministack"),
                comandosExecutados.get(1));
        assertTrue(dockerRun.contains("--pull=never"));
        assertTrue(dockerRun.contains("/var/run/docker.sock:/var/run/docker.sock"));
        assertTrue(dockerRun.stream().anyMatch(argumento ->
                argumento.contains(work.resolve("init").toString())
                        && argumento.contains("/etc/localstack/init/ready.d")));
        assertTrue(dockerRun.getLast().contains("ministackorg/ministack:1.4.13-full@sha256:"));
        assertEquals(List.of("docker", "exec", "learning-infra-ministack"),
                comandosExecutados.get(3).subList(0, 3));
    }

    @Test
    void iniciarAwsSemInfraestruturaRealNaoMontaSocketDocker() throws Exception {
        Scenario scenario = cenarioAws("aws/02-s3", false, false);
        var repositorio = Mockito.mock(ScenarioRepository.class);

        gerenciador(repositorio).start(scenario);

        List<String> dockerRun = comandosExecutados.get(2);
        assertFalse(dockerRun.contains("/var/run/docker.sock:/var/run/docker.sock"));
    }

    @Test
    void iniciarAwsRemoveSidecarsDescobertosPorLabel() throws Exception {
        Scenario scenario = cenarioAws("aws/11-rds", true, false);
        var repositorio = Mockito.mock(ScenarioRepository.class);
        String idSidecar = "1c8164819eef";
        CommandExecutor executorComSobra = command -> {
            comandosExecutados.add(command);
            if (command.equals(List.of(
                    "docker", "ps", "-aq", "--filter", "label=ministack"))) {
                return new CommandOutput(0, idSidecar + "\n", "");
            }
            return new CommandOutput(0, "", "");
        };
        var gerenciador = new ActiveScenarioManager(
                root.resolve("work").toString(), executorComSobra,
                new ProgressRepository(root.resolve("data/progresso.json").toString()),
                repositorio);

        gerenciador.start(scenario);

        assertTrue(comandosExecutados.contains(
                List.of("docker", "rm", "-f", "-v", idSidecar)));
    }

    @Test
    void reiniciarMesmoCenarioAwsRemoveContainerESidecarsAntesDeSubirOutro() throws Exception {
        Scenario scenario = cenarioAws("aws/14-ecs", true, false);
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("aws/14-ecs")).thenReturn(Optional.of(scenario));
        ActiveScenarioManager gerenciador = gerenciador(repositorio);
        gerenciador.start(scenario);
        comandosExecutados.clear();

        gerenciador.start(scenario);

        assertEquals(List.of("docker", "rm", "-f", "-v", "learning-infra-ministack"),
                comandosExecutados.get(0));
        assertEquals(List.of("docker", "ps", "-aq", "--filter", "label=ministack"),
                comandosExecutados.get(1));
        assertTrue(comandosExecutados.stream().anyMatch(command -> command.contains("run")));
    }

    @Test
    void iniciarSobeOComposeQuandoOWorkspaceTrazUm() throws Exception {
        Scenario scenario = cenarioComposeComArquivo("docker/04", "lab-04");
        var repositorio = Mockito.mock(ScenarioRepository.class);

        Path work = gerenciador(repositorio).start(scenario);

        assertEquals(
                List.of(List.of("docker", "compose", "-p", "lab-04",
                        "-f", work.resolve("compose.yaml").toString(),
                        "up", "-d", "--build")),
                comandosExecutados);
    }

    @Test
    void iniciarNaoSobeComposeQuandoOWorkspaceNaoTrazArquivo() throws Exception {
        Scenario scenario = cenarioCompose("docker/03", "lab-03");
        var repositorio = Mockito.mock(ScenarioRepository.class);

        gerenciador(repositorio).start(scenario);

        assertTrue(comandosExecutados.stream().noneMatch(c -> c.contains("up")));
    }

    @Test
    void falhaDoComposeUpEhRuidosa() throws Exception {
        Scenario scenario = cenarioComposeComArquivo("docker/04", "lab-04");
        var repositorio = Mockito.mock(ScenarioRepository.class);

        CommandExecutor executorQueFalha = command ->
                command.contains("up") ? new CommandOutput(1, "", "imagem nao encontrada")
                                       : new CommandOutput(0, "", "");
        var gerenciador = new ActiveScenarioManager(
                root.resolve("work").toString(),
                executorQueFalha,
                new ProgressRepository(root.resolve("data/progresso.json").toString()),
                repositorio);

        var error = assertThrows(IllegalStateException.class, () -> gerenciador.start(scenario));
        assertTrue(error.getMessage().contains("lab-04"));
    }

    @Test
    void iniciarRemoveOsVolumesDoCenarioAnterior() throws Exception {
        Path directory = root.resolve("content").resolve("docker-05");
        Files.createDirectories(directory.resolve("workspace"));
        Scenario primeiro = comInfra("docker/05", directory,
                new Scenario.ComposeConfiguration(null, List.of("lab-05-dados")),
                Scenario.KubernetesConfiguration.nenhuma(), Scenario.AwsConfiguration.nenhuma(),
                Scenario.TerraformConfiguration.nenhuma(), Scenario.LinuxConfiguration.nenhuma());
        Scenario segundo = scenario("docker/06", List.of());
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("docker/05")).thenReturn(Optional.of(primeiro));

        ActiveScenarioManager gerenciador = gerenciador(repositorio);
        gerenciador.start(primeiro);
        comandosExecutados.clear();
        gerenciador.start(segundo);

        assertEquals(
                List.of(List.of("docker", "volume", "rm", "-f", "lab-05-dados")),
                comandosExecutados);
    }

    @Test
    void teardownDeVolumeAusenteNaoBloqueiaOIniciar() throws Exception {
        Path directory = root.resolve("content").resolve("docker-05");
        Files.createDirectories(directory.resolve("workspace"));
        Scenario primeiro = comInfra("docker/05", directory,
                new Scenario.ComposeConfiguration(null, List.of("lab-05-dados")),
                Scenario.KubernetesConfiguration.nenhuma(), Scenario.AwsConfiguration.nenhuma(),
                Scenario.TerraformConfiguration.nenhuma(), Scenario.LinuxConfiguration.nenhuma());
        Scenario segundo = scenario("docker/06", List.of());
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("docker/05")).thenReturn(Optional.of(primeiro));

        CommandExecutor executorComVolumeAusente = command ->
                command.contains("volume") && command.contains("rm")
                        ? new CommandOutput(1, "",
                                "Error response from daemon: get lab-05-dados: no such volume")
                        : new CommandOutput(0, "", "");
        var gerenciador = new ActiveScenarioManager(
                root.resolve("work").toString(),
                executorComVolumeAusente,
                new ProgressRepository(root.resolve("data/progresso.json").toString()),
                repositorio);

        gerenciador.start(primeiro);

        assertDoesNotThrow(() -> gerenciador.start(segundo));
    }

    @Test
    void cenarioAtivoForaDoCatalogoFalhaComInstrucaoELimpaORegistro() throws Exception {
        Scenario segundo = scenario("docker/06", List.of());
        var repositorio = Mockito.mock(ScenarioRepository.class);
        var progressRepository = new ProgressRepository(root.resolve("data/progresso.json").toString());
        progressRepository.save(Progress.empty().withActiveScenario("docker/99-antigo"));
        var gerenciador = new ActiveScenarioManager(
                root.resolve("work").toString(), executorEspiao, progressRepository, repositorio);

        var error = assertThrows(IllegalStateException.class, () -> gerenciador.start(segundo));

        assertTrue(error.getMessage().contains("docker/99-antigo"));
        assertNull(progressRepository.load().activeScenario());
    }

    @Test
    void falhaNoComposeUpDerrubaAmbienteParcialEZeraOAtivo() throws Exception {
        Scenario primeiro = cenarioComposeComArquivo("docker/04", "lab-04");
        Scenario segundo = cenarioComposeComArquivo("docker/05", "lab-05");
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("docker/04")).thenReturn(Optional.of(primeiro));
        CommandExecutor executorQueFalhaNoUp = command -> {
            comandosExecutados.add(command);
            return command.contains("up") && command.contains("lab-05")
                    ? new CommandOutput(1, "", "imagem nao encontrada")
                    : new CommandOutput(0, "", "");
        };
        var progressRepository = new ProgressRepository(root.resolve("data/progresso.json").toString());
        var gerenciador = new ActiveScenarioManager(
                root.resolve("work").toString(), executorQueFalhaNoUp, progressRepository, repositorio);

        gerenciador.start(primeiro);
        comandosExecutados.clear();

        assertThrows(IllegalStateException.class, () -> gerenciador.start(segundo));

        assertNull(progressRepository.load().activeScenario());
        assertEquals(List.of("docker", "compose", "-p", "lab-05", "down", "-v"),
                comandosExecutados.getLast());
    }

    @Test
    void iniciarDerrubaOProjetoComposeDoCenarioAnterior() throws Exception {
        Scenario primeiro = cenarioCompose("docker/03", "lab-03");
        Scenario segundo = scenario("docker/04", List.of());
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("docker/03")).thenReturn(Optional.of(primeiro));

        ActiveScenarioManager gerenciador = gerenciador(repositorio);
        gerenciador.start(primeiro);
        comandosExecutados.clear();
        gerenciador.start(segundo);

        assertEquals(
                List.of(List.of("docker", "compose", "-p", "lab-03", "down", "-v")),
                comandosExecutados);
    }

    @Test
    void cenarioSemProjetoComposeNaoChamaCompose() throws Exception {
        Scenario primeiro = scenario("docker/01", List.of("lab-web"));
        Scenario segundo = scenario("docker/02", List.of());
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("docker/01")).thenReturn(Optional.of(primeiro));

        ActiveScenarioManager gerenciador = gerenciador(repositorio);
        gerenciador.start(primeiro);
        comandosExecutados.clear();
        gerenciador.start(segundo);

        assertTrue(comandosExecutados.stream().noneMatch(c -> c.contains("compose")));
    }

    @Test
    void falhaDoComposeDownEhRuidosa() throws Exception {
        Scenario primeiro = cenarioCompose("docker/03", "lab-03");
        Scenario segundo = scenario("docker/04", List.of());
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("docker/03")).thenReturn(Optional.of(primeiro));

        CommandExecutor executorQueFalha = command ->
                command.contains("compose") ? new CommandOutput(1, "", "daemon fora do ar")
                                            : new CommandOutput(0, "", "");
        var gerenciador = new ActiveScenarioManager(
                root.resolve("work").toString(),
                executorQueFalha,
                new ProgressRepository(root.resolve("data/progresso.json").toString()),
                repositorio);

        gerenciador.start(primeiro);

        var error = assertThrows(IllegalStateException.class, () -> gerenciador.start(segundo));
        assertTrue(error.getMessage().contains("lab-03"));
    }

    @Test
    void materializaOWorkspaceNoDiretorioDeTrabalho() throws Exception {
        Scenario primeiro = scenario("docker/01", List.of("lab-web"));
        var repositorio = Mockito.mock(ScenarioRepository.class);

        Path work = gerenciador(repositorio).start(primeiro);

        assertTrue(work.isAbsolute());
        assertTrue(Files.exists(work.resolve("site/index.html")));
        assertEquals("<h1>ola</h1>", Files.readString(work.resolve("site/index.html")));
    }

    @Test
    void iniciarDerrubaOsContainersDoCenarioAnterior() throws Exception {
        Scenario primeiro = scenario("docker/01", List.of("lab-web"));
        Scenario segundo = scenario("docker/02", List.of("lab-api"));
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("docker/01")).thenReturn(Optional.of(primeiro));

        ActiveScenarioManager gerenciador = gerenciador(repositorio);
        gerenciador.start(primeiro);
        comandosExecutados.clear();
        gerenciador.start(segundo);

        assertEquals(List.of(List.of("docker", "rm", "-f", "lab-web")), comandosExecutados);
    }

    @Test
    void iniciarLimpaSobrasDoWorkspaceAnterior() throws Exception {
        Scenario primeiro = scenario("docker/01", List.of("lab-web"));
        Scenario segundo = scenario("docker/02", List.of("lab-api"));
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("docker/01")).thenReturn(Optional.of(primeiro));

        ActiveScenarioManager gerenciador = gerenciador(repositorio);
        Path work = gerenciador.start(primeiro);
        Files.writeString(work.resolve("lixo.txt"), "sobra");

        gerenciador.start(segundo);

        assertFalse(Files.exists(work.resolve("lixo.txt")));
    }

    @Test
    void oCenarioAtivoSobreviveAUmNovoGerenciador() throws Exception {
        Scenario primeiro = scenario("docker/01", List.of("lab-web"));
        var repositorio = Mockito.mock(ScenarioRepository.class);

        gerenciador(repositorio).start(primeiro);

        assertEquals(Optional.of("docker/01"), gerenciador(repositorio).activeScenario());
    }

    @Test
    void marcarConcluidoRegistraNoProgresso() throws Exception {
        Scenario primeiro = scenario("docker/01", List.of("lab-web"));
        var repositorio = Mockito.mock(ScenarioRepository.class);
        ActiveScenarioManager gerenciador = gerenciador(repositorio);

        gerenciador.start(primeiro);
        gerenciador.markCompleted(primeiro);

        assertTrue(new ProgressRepository(root.resolve("data/progresso.json").toString())
                .load().completed().containsKey("docker/01"));
    }

    @Test
    void falhaDeTeardownEhRuidosa() throws Exception {
        Scenario primeiro = scenario("docker/01", List.of("lab-web"));
        Scenario segundo = scenario("docker/02", List.of("lab-api"));
        var repositorio = Mockito.mock(ScenarioRepository.class);
        Mockito.when(repositorio.find("docker/01")).thenReturn(Optional.of(primeiro));

        CommandExecutor executorQueFalha = command ->
                command.contains("rm") ? new CommandOutput(1, "", "daemon fora do ar")
                                       : new CommandOutput(0, "", "");
        var gerenciador = new ActiveScenarioManager(
                root.resolve("work").toString(),
                executorQueFalha,
                new ProgressRepository(root.resolve("data/progresso.json").toString()),
                repositorio);

        gerenciador.start(primeiro);

        var error = assertThrows(IllegalStateException.class, () -> gerenciador.start(segundo));
        assertTrue(error.getMessage().contains("lab-web"));
    }
}
