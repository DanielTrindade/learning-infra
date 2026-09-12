package dev.learninginfra.verification;

import com.sun.net.httpserver.HttpServer;
import dev.learninginfra.execution.CommandExecutor;
import dev.learninginfra.execution.CommandOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerificationEngineTest {

    private HttpServer servidor;
    private String base;

    @BeforeEach
    void subirServidor() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            byte[] corpo = "<h1>Meu primeiro container</h1>".getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(200, corpo.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(corpo);
            }
        });
        servidor.createContext("/lento", troca -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            troca.sendResponseHeaders(200, 0);
            troca.close();
        });
        servidor.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @AfterEach
    void derrubarServidor() {
        servidor.stop(0);
    }

    private VerificationEngine motorQueResponde(String stdout, int codigo) {
        CommandExecutor falso = command -> new CommandOutput(codigo, stdout, "");
        return new VerificationEngine(falso);
    }

    private VerificationEngine motorComTrabalho(String stdout, int codigo, String work) {
        CommandExecutor falso = command -> new CommandOutput(codigo, stdout, "");
        return new VerificationEngine(falso, java.time.Duration.ofSeconds(1), work);
    }

    private static final String ESTADO_DO_CONTAINER = """
            # docker_container.web:
            resource "docker_container" "web" {
                image = "sha256:abc"
                name  = "mirante-web"
                rm    = false
            }
            """;

    @Test
    void containerRodandoPassaQuandoDockerDizTrue() {
        var result = motorQueResponde("true\n", 0)
                .verify(List.of(new Assertion.ContainerRunning("lab-web")));

        assertTrue(result.completed());
        assertTrue(result.assertions().getFirst().passed());
    }

    @Test
    void containerParadoFalhaComDetalheUtil() {
        var result = motorQueResponde("false\n", 0)
                .verify(List.of(new Assertion.ContainerRunning("lab-web")));

        assertFalse(result.completed());
        assertEquals("existe, mas está parado", result.assertions().getFirst().detail());
    }

    @Test
    void containerInexistenteFalhaComDetalheDiferenteDeParado() {
        var result = motorQueResponde("", 1)
                .verify(List.of(new Assertion.ContainerRunning("lab-web")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("não existe"));
    }

    @Test
    void containerSaudavelPassaQuandoHealthDizHealthy() {
        var result = motorQueResponde("healthy\n", 0)
                .verify(List.of(new Assertion.ContainerHealthy("lab-08-db-1")));

        assertTrue(result.completed());
    }

    @Test
    void containerSaudavelFalhaQuandoNaoEstaHealthy() {
        var result = motorQueResponde("starting\n", 0)
                .verify(List.of(new Assertion.ContainerHealthy("lab-08-db-1")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("starting"));
    }

    @Test
    void containerEmRedePassaQuandoOPertence() {
        var result = motorQueResponde("lab-07-borda lab-07-interna\n", 0)
                .verify(List.of(new Assertion.ContainerInNetwork("lab-07-api-1", "lab-07-borda", true)));

        assertTrue(result.completed());
    }

    @Test
    void containerEmRedeFalhaQuandoNaoPertenceMasDeveria() {
        var result = motorQueResponde("lab-07-borda\n", 0)
                .verify(List.of(new Assertion.ContainerInNetwork("lab-07-db-1", "lab-07-interna", true)));

        assertFalse(result.completed());
    }

    @Test
    void containerForaDaRedePassaComPresenteFalse() {
        var result = motorQueResponde("lab-07-interna\n", 0)
                .verify(List.of(new Assertion.ContainerInNetwork("lab-07-db-1", "lab-07-borda", false)));

        assertTrue(result.completed());
    }

    @Test
    void containerEmRedeIgnoraNomeParcial() {
        var result = motorQueResponde("lab-07\n", 0)
                .verify(List.of(new Assertion.ContainerInNetwork("lab-07-api-1", "lab-07-borda", true)));

        assertFalse(result.completed(), "nome parcial de rede não pode aprovar");
    }

    @Test
    void containerConfiguracaoValidaUsuarioSomenteLeituraECapabilities() {
        // O motor consulta o daemon 3 vezes; responda na ordem pedida com um executor por campo.
        var executor = new CommandExecutor() {
            int chamadas = 0;

            @Override
            public CommandOutput execute(java.util.List<String> command) {
                chamadas++;
                return new CommandOutput(0,
                        switch (chamadas) {
                            case 1 -> "node\n";
                            case 2 -> "true\n";
                            default -> "[\"ALL\"]\n";
                        }, "");
            }
        };

        var result = new VerificationEngine(executor).verify(List.of(
                new Assertion.ContainerConfiguration(
                        "lab-09-app-1", "node", true, java.util.List.of("ALL"))));

        assertTrue(result.completed());
    }

    @Test
    void containerConfiguracaoReprovaUsuarioErrado() {
        CommandExecutor executor = command -> new CommandOutput(0, "root\n", "");
        var result = new VerificationEngine(executor).verify(List.of(
                new Assertion.ContainerConfiguration("lab-09-app-1", "node", null, null)));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("node"));
    }

    @Test
    void containerConfiguracaoReprovaCapabilityNaoRemovida() {
        CommandExecutor executor = command -> new CommandOutput(0, "[]\n", "");
        var result = new VerificationEngine(executor).verify(List.of(
                new Assertion.ContainerConfiguration(
                        "lab-09-app-1", null, null, java.util.List.of("ALL"))));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("ALL"));
    }

    @Test
    void httpRespondePassaContraServidorDeVerdade() {
        var result = motorQueResponde("true\n", 0)
                .verify(List.of(new Assertion.HttpResponds(base, 200)));

        assertTrue(result.completed());
    }

    @Test
    void servicoLentoNaoEhConfundidoComServicoMorto() {
        CommandExecutor falso = command -> new CommandOutput(0, "true\n", "");
        var motorImpaciente = new VerificationEngine(falso, java.time.Duration.ofSeconds(1));

        var result = motorImpaciente
                .verify(List.of(new Assertion.HttpResponds(base + "/lento", 200)));

        var detail = result.assertions().getFirst().detail();
        assertFalse(result.completed());
        assertTrue(detail.contains("lento ou travado"), "veio: " + detail);
        assertFalse(detail.contains("nada respondeu"), "veio: " + detail);
    }

    @Test
    void httpRespondeFalhaQuandoNadaEscuta() {
        var result = motorQueResponde("true\n", 0)
                .verify(List.of(new Assertion.HttpResponds("http://127.0.0.1:1", 200)));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("nada respondeu"));
    }

    @Test
    void corpoContemPassaEFalhaConformeOTexto() {
        var passed = motorQueResponde("true\n", 0)
                .verify(List.of(new Assertion.HttpBodyContains(base, "primeiro container")));
        var falhou = motorQueResponde("true\n", 0)
                .verify(List.of(new Assertion.HttpBodyContains(base, "texto ausente")));

        assertTrue(passed.completed());
        assertFalse(falhou.completed());
    }

    @Test
    void imagemExistePassaQuandoInspectDaCerto() {
        var result = motorQueResponde("sha256:abc\n", 0)
                .verify(List.of(new Assertion.ImageExists("lab-app:1.0")));

        assertTrue(result.completed());
    }

    @Test
    void imagemInexistenteFalhaDizendoQueFaltaConstruir() {
        var result = motorQueResponde("", 1)
                .verify(List.of(new Assertion.ImageExists("lab-app:1.0")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("não foi construída"));
    }

    @Test
    void volumeExistePassaQuandoInspectDaCerto() {
        var result = motorQueResponde("[{}]", 0)
                .verify(List.of(new Assertion.VolumeExists("lab-05-dados")));

        assertTrue(result.completed());
    }

    @Test
    void volumeInexistenteFalhaDizendoQueNaoFoiCriado() {
        var result = motorQueResponde("", 1)
                .verify(List.of(new Assertion.VolumeExists("lab-05-dados")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("não existe"));
    }

    @Test
    void imagemNoRegistryPassaQuandoManifestInspectDaCerto() {
        var result = motorQueResponde("{\"schemaVersion\":2}\n", 0)
                .verify(List.of(new Assertion.ImageInRegistry(
                        "localhost:5000/lab-10-app:1.0", "a v1.0 está publicada no registry")));

        assertTrue(result.completed());
    }

    @Test
    void imagemForaDoRegistryFalhaDizendoQueNaoFoiPublicada() {
        var result = motorQueResponde("", 1)
                .verify(List.of(new Assertion.ImageInRegistry(
                        "localhost:5000/lab-10-app:1.0", "a v1.0 está publicada no registry")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("registry"));
    }

    @Test
    void imagemNoRegistryInspecionaOComInsecureContraORegistryHttpLocal() {
        List<List<String>> comandos = new java.util.ArrayList<>();
        CommandExecutor executor = command -> {
            comandos.add(command);
            return new CommandOutput(0, "{\"schemaVersion\":2}\n", "");
        };

        var result = new VerificationEngine(executor).verify(List.of(
                new Assertion.ImageInRegistry(
                        "localhost:5000/lab-10-app:1.0", "a v1.0 está publicada no registry")));

        assertTrue(result.completed());
        assertEquals(List.of("docker", "manifest", "inspect", "--insecure",
                "localhost:5000/lab-10-app:1.0"), comandos.getFirst());
    }

    @Test
    void comandoProduzPassaQuandoASaidaTemOTexto() {
        var result = motorQueResponde("tamandua\n", 0).verify(List.of(
                new Assertion.CommandProduces(List.of("echo", "tamandua"), "tamandua", "o dado sobreviveu")));

        assertTrue(result.completed());
    }

    @Test
    void comandoProduzDistingueComandoQueFalhouDeSaidaErrada() {
        var falhou = motorQueResponde("", 1).verify(List.of(
                new Assertion.CommandProduces(List.of("cat", "/nada"), "tamandua", "o dado sobreviveu")));
        var saidaErrada = motorQueResponde("preguica\n", 0).verify(List.of(
                new Assertion.CommandProduces(List.of("cat", "/x"), "tamandua", "o dado sobreviveu")));

        assertTrue(falhou.assertions().getFirst().detail().contains("não completou"));
        assertTrue(saidaErrada.assertions().getFirst().detail().contains("sem o texto"));
    }

    @Test
    void comandoProduzMostraOErroRealEnaoOProgressoDeDownload() {
        CommandExecutor docker = command -> new CommandOutput(1, "", """
                Unable to find image 'alpine:latest' locally
                latest: Pulling from library/alpine
                Status: Downloaded newer image for alpine:latest
                cat: can't open '/dados/bicho.txt': No such file or directory
                """);
        var result = new VerificationEngine(docker).verify(List.of(
                new Assertion.CommandProduces(List.of("docker", "run"), "tamandua", "o dado sobreviveu")));

        var detail = result.assertions().getFirst().detail();
        assertTrue(detail.contains("can't open"), "veio: " + detail);
        assertFalse(detail.contains("Pulling from"), "veio: " + detail);
    }

    @Test
    void comandoProduzUsaADescricaoDoCenarioENaoOComando() {
        var result = motorQueResponde("tamandua\n", 0).verify(List.of(
                new Assertion.CommandProduces(
                        List.of("docker", "run", "-v", "lab-05-dados:/dados", "alpine", "cat", "/dados/x"),
                        "tamandua",
                        "o dado sobreviveu ao container")));

        var description = result.assertions().getFirst().description();
        assertEquals("o dado sobreviveu ao container", description);
        assertFalse(description.contains("-v"), "a descrição não pode entregar a sintaxe do exercício");
    }

    @Test
    void todasAsAsercoesSaoAvaliadasMesmoQuandoAPrimeiraFalha() {
        var result = motorQueResponde("false\n", 0).verify(List.of(
                new Assertion.ContainerRunning("lab-web"),
                new Assertion.HttpResponds(base, 200)));

        assertEquals(2, result.assertions().size());
        assertFalse(result.assertions().get(0).passed());
        assertTrue(result.assertions().get(1).passed());
    }

    @Test
    void avaliacaoParalelaPreservaAOrdemDeclarada() {
        CommandExecutor lento = command -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new CommandOutput(0, "true\n", "");
        };

        long inicio = System.nanoTime();
        var result = new VerificationEngine(lento, java.time.Duration.ofSeconds(5))
                .verify(List.of(
                        new Assertion.ContainerRunning("lab-a"),
                        new Assertion.ContainerRunning("lab-b")));
        long millis = (System.nanoTime() - inicio) / 1_000_000;

        assertTrue(result.completed());
        assertEquals("o container `lab-a` está rodando",
                result.assertions().get(0).description());
        assertEquals("o container `lab-b` está rodando",
                result.assertions().get(1).description());
        assertTrue(millis < 550, "esperava execução paralela, levou " + millis + "ms");
    }

    @Test
    void condicaoKubernetesUsaWaitComTimeout() {
        List<List<String>> comandos = new java.util.ArrayList<>();
        CommandExecutor executor = command -> {
            comandos.add(command);
            return new CommandOutput(0, "condition met", "");
        };
        var assertion = new Assertion.KubernetesCondition(
                "docker-desktop", "li-k8s-01", "deployment", "web",
                "Available", "True", 12, "o Deployment está disponível");

        var result = new VerificationEngine(executor).verify(List.of(assertion));

        assertTrue(result.completed());
        assertEquals(List.of("kubectl", "--context", "docker-desktop", "--namespace",
                "li-k8s-01", "wait", "deployment/web", "--for=condition=Available=True",
                "--timeout=12s"), comandos.getFirst());
    }

    @Test
    void jsonpathKubernetesEsperaOValorComTimeout() {
        List<List<String>> comandos = new java.util.ArrayList<>();
        CommandExecutor executor = command -> {
            comandos.add(command);
            return new CommandOutput(0, "condition met", "");
        };
        var assertion = new Assertion.KubernetesJsonpath(
                "docker-desktop", "li-k8s-01", "pod", "web", "{.status.phase}",
                "Running", 8, "o Pod está rodando");

        var result = new VerificationEngine(executor).verify(List.of(assertion));

        assertTrue(result.completed());
        assertEquals(List.of("kubectl", "--context", "docker-desktop", "--namespace",
                "li-k8s-01", "wait", "pod/web", "--for=jsonpath={.status.phase}=Running",
                "--timeout=8s"), comandos.getFirst());
    }

    @Test
    void rbacKubernetesValidaPermissaoENegacaoSemAceitarFalhaDeConexao() {
        var allowed = new Assertion.KubernetesRbac(
                "docker-desktop", "li-k8s-10", "auditor", "get", "pods", true,
                "o auditor lê Pods");
        var negado = new Assertion.KubernetesRbac(
                "docker-desktop", "li-k8s-10", "auditor", "delete", "pods", false,
                "o auditor não apaga Pods");

        assertTrue(motorQueResponde("yes\n", 0).verify(List.of(allowed)).completed());
        assertTrue(motorQueResponde("no\n", 1).verify(List.of(negado)).completed());
        assertFalse(motorQueResponde("", 1).verify(List.of(negado)).completed());
    }

    @Test
    void consultaAwsFixaEndpointRegiaoERequisicaoSemCredenciaisReais() {
        List<List<String>> comandos = new java.util.ArrayList<>();
        List<Map<String, String>> ambientes = new java.util.ArrayList<>();
        CommandExecutor executor = new CommandExecutor() {
            @Override
            public CommandOutput execute(List<String> command) {
                throw new AssertionError("a consulta AWS deve declarar o ambiente sintético");
            }

            @Override
            public CommandOutput execute(
                    List<String> command, Map<String, String> ambiente) {
                comandos.add(command);
                ambientes.add(ambiente);
                return new CommandOutput(0, "Enabled\n", "");
            }
        };
        var assertion = new Assertion.AwsQuery(
                "http://127.0.0.1:4566", "us-east-1", "s3api", "get-bucket-versioning",
                List.of("--bucket", "learning-infra-arquivos"), "Status", "Enabled",
                Comparison.EXACT, "o bucket mantém histórico de versões");

        var result = new VerificationEngine(executor).verify(List.of(assertion));

        assertTrue(result.completed());
        assertEquals(List.of(
                "aws", "--endpoint-url", "http://127.0.0.1:4566",
                "--region", "us-east-1", "--no-cli-pager",
                "s3api", "get-bucket-versioning",
                "--bucket", "learning-infra-arquivos",
                "--query", "Status", "--output", "text"), comandos.getFirst());
        assertEquals("000000000000", ambientes.getFirst().get("AWS_ACCESS_KEY_ID"));
        assertEquals("test", ambientes.getFirst().get("AWS_SECRET_ACCESS_KEY"));
        assertEquals("true", ambientes.getFirst().get("AWS_EC2_METADATA_DISABLED"));
    }

    @Test
    void consultaAwsDistingueFalhaDaApiDeValorIncorreto() {
        var assertion = new Assertion.AwsQuery(
                "http://127.0.0.1:4566", "us-east-1", "dynamodb", "describe-table",
                List.of("--table-name", "pedidos"), "Table.TableStatus", "ACTIVE",
                Comparison.EXACT, "a tabela está ativa");

        var apiFalhou = motorQueResponde("", 1).verify(List.of(assertion));
        var valorErrado = motorQueResponde("CREATING\n", 0).verify(List.of(assertion));

        assertTrue(apiFalhou.assertions().getFirst().detail().contains("não consegui consultar"));
        assertTrue(valorErrado.assertions().getFirst().detail().contains("CREATING"));
    }

    @Test
    void consultaAwsExataNaoAprovaPorSubstring() {
        var assertion = new Assertion.AwsQuery(
                "http://127.0.0.1:4566", "us-east-1", "dynamodb", "describe-table",
                List.of("--table-name", "pedidos"), "Table.TableStatus", "ACTIVE",
                Comparison.EXACT, "a tabela está ativa");

        assertFalse(motorQueResponde("INACTIVE\n", 0).verify(List.of(assertion)).completed());

        var contagem = new Assertion.AwsQuery(
                "http://127.0.0.1:4566", "us-east-1", "ecs", "list-tasks",
                List.of("--cluster", "learning-infra-ecs"), "length(taskArns)", "1",
                Comparison.EXACT, "existe uma task");

        assertFalse(motorQueResponde("10\n", 0).verify(List.of(contagem)).completed());
    }

    @Test
    void consultaAwsExataAceitaUmaLinhaIgualEntreVarias() {
        var assertion = new Assertion.AwsQuery(
                "http://127.0.0.1:4566", "us-east-1", "ec2", "describe-subnets",
                List.of(), "sort(Subnets[*].AvailabilityZone)", "us-east-1b",
                Comparison.EXACT, "a topologia ocupa duas zonas");

        assertTrue(motorQueResponde("us-east-1a\nus-east-1b\n", 0)
                .verify(List.of(assertion)).completed());
    }

    @Test
    void consultaAwsContemPreservaPrefixosEStatusComposto() {
        var prefixo = new Assertion.AwsQuery(
                "http://127.0.0.1:4566", "us-east-1", "ec2", "describe-route-tables",
                List.of(), "GatewayId", "igw-", Comparison.CONTAINS,
                "a rota aponta para um Internet Gateway");
        var statusComposto = new Assertion.AwsQuery(
                "http://127.0.0.1:4566", "us-east-1", "cloudformation", "describe-stacks",
                List.of(), "Stacks[0].StackStatus", "COMPLETE", Comparison.CONTAINS,
                "o stack convergiu sem rollback");

        assertTrue(motorQueResponde("igw-0abc123\n", 0).verify(List.of(prefixo)).completed());
        assertTrue(motorQueResponde("CREATE_COMPLETE\n", 0)
                .verify(List.of(statusComposto)).completed());
    }

    @Test
    void terraformEstadoPassaQuandoOAtributoConfere() {
        var result = motorComTrabalho(ESTADO_DO_CONTAINER, 0, "../work")
                .verify(List.of(new Assertion.TerraformState(
                        ".", "docker_container.web", "name", "mirante-web",
                        "o container está sob gestão do Terraform")));

        assertTrue(result.completed());
    }

    @Test
    void servicoSystemdPassaQuandoAtivoEHabilitado() {
        List<List<String>> comandos = new java.util.ArrayList<>();
        CommandExecutor executor = command -> {
            comandos.add(command);
            return new CommandOutput(0, "", "");
        };

        var result = new VerificationEngine(executor).verify(List.of(
                new Assertion.SystemdService(
                        "learning-infra-linux", "catalogo.service", true, true,
                        "o catálogo vira serviço")));

        assertTrue(result.completed());
        assertEquals(List.of("docker", "exec", "learning-infra-linux",
                "systemctl", "is-active", "--quiet", "catalogo.service"), comandos.get(0));
        assertEquals(List.of("docker", "exec", "learning-infra-linux",
                "systemctl", "is-enabled", "--quiet", "catalogo.service"), comandos.get(1));
    }

    @Test
    void servicoSystemdReprovaServicoParado() {
        CommandExecutor executor = command -> new CommandOutput(3, "inactive", "");

        var result = new VerificationEngine(executor).verify(List.of(
                new Assertion.SystemdService(
                        "learning-infra-linux", "catalogo.service", true, null,
                        "o catálogo vira serviço")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("não está ativo"));
    }

    @Test
    void servicoSystemdNaoDeveEstarAtivoQuandoExigidoFalse() {
        CommandExecutor executor = command -> new CommandOutput(0, "active", "");

        var result = new VerificationEngine(executor).verify(List.of(
                new Assertion.SystemdService(
                        "learning-infra-linux", "catalogo.service", false, null,
                        "o serviço não pode subir sozinho")));

        assertFalse(result.completed());
    }

    @Test
    void servicoSystemdReprovaUnitInexistente() {
        CommandExecutor executor = command -> new CommandOutput(4, "", "");

        var result = new VerificationEngine(executor).verify(List.of(
                new Assertion.SystemdService(
                        "learning-infra-linux", "catalogo.service", true, null,
                        "o catálogo vira serviço")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("não existe"));
    }

    @Test
    void servicoSystemdNaoAprovaParadoQuandoAConsultaFalha() {
        CommandExecutor executor = command -> new CommandOutput(
                1, "", "Error response from daemon: ... not running");

        var result = new VerificationEngine(executor).verify(List.of(
                new Assertion.SystemdService(
                        "learning-infra-linux", "catalogo.service", false, null,
                        "o serviço não pode subir sozinho")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("não consegui consultar"));
    }

    @Test
    void arquivoLinuxPassaQuandoOsCamposConferem() {
        List<List<String>> comandos = new java.util.ArrayList<>();
        CommandExecutor executor = command -> {
            comandos.add(command);
            return new CommandOutput(0, "2770 root ana\n", "");
        };

        var result = new VerificationEngine(executor).verify(List.of(
                new Assertion.LinuxFile(
                        "learning-infra-linux", "/srv/dados", "2770", "root", "ana",
                        "o diretório tem o dono e o modo certos")));

        assertTrue(result.completed());
        assertEquals(List.of("docker", "exec", "learning-infra-linux",
                "stat", "-c", "%a %U %G", "/srv/dados"), comandos.getFirst());
    }

    @Test
    void arquivoLinuxReprovaModoErradoDizendoOObservado() {
        CommandExecutor executor = command -> new CommandOutput(0, "755 root ana\n", "");

        var result = new VerificationEngine(executor).verify(List.of(
                new Assertion.LinuxFile(
                        "learning-infra-linux", "/srv/dados", "2770", null, null,
                        "o diretório tem o modo certo")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("755"));
    }

    @Test
    void arquivoLinuxReprovaCaminhoInexistente() {
        CommandExecutor executor = command -> new CommandOutput(1, "", "No such file");

        var result = new VerificationEngine(executor).verify(List.of(
                new Assertion.LinuxFile(
                        "learning-infra-linux", "/nao/existe", "440", null, null,
                        "o arquivo existe")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("não existe"));
    }

    @Test
    void terraformEstadoFalhaDizendoOValorObservado() {
        var result = motorComTrabalho(ESTADO_DO_CONTAINER, 0, "../work")
                .verify(List.of(new Assertion.TerraformState(
                        ".", "docker_container.web", "name", "outro-nome",
                        "o container está sob gestão do Terraform")));

        assertFalse(result.completed());
        assertEquals("`name` no state é `mirante-web`",
                result.assertions().getFirst().detail());
    }

    @Test
    void terraformEstadoFalhaQuandoORecursoNaoNasceuDoCodigo() {
        var result = motorComTrabalho("", 1, "../work")
                .verify(List.of(new Assertion.TerraformState(
                        ".", "docker_container.web", null, null,
                        "o container está sob gestão do Terraform")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("não nasceu do código"));
    }

    @Test
    void terraformEstadoSemAtributoSoExigeQueOEnderecoExista() {
        var result = motorComTrabalho(ESTADO_DO_CONTAINER, 0, "../work")
                .verify(List.of(new Assertion.TerraformState(
                        ".", "docker_container.web", null, null,
                        "o container está sob gestão do Terraform")));

        assertTrue(result.completed());
    }

    @Test
    void terraformEstadoRecusaDiretorioQueEscapaDoTrabalho() {
        var result = motorComTrabalho(ESTADO_DO_CONTAINER, 0, "../work")
                .verify(List.of(new Assertion.TerraformState(
                        "../..", "docker_container.web", null, null,
                        "o container está sob gestão do Terraform")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("fora do diretório"));
    }

    @Test
    void planoLimpoPassaQuandoOTerraformNaoTemMudancaPendente() {
        var result = motorComTrabalho("No changes.", 0, "../work")
                .verify(List.of(new Assertion.TerraformCleanPlan(
                        ".", "o código descreve a infraestrutura que está no ar")));

        assertTrue(result.completed());
    }

    @Test
    void planoComMudancaPendenteFalhaFalandoEmDivergencia() {
        var result = motorComTrabalho("Plan: 1 to add, 0 to change, 0 to destroy.", 2, "../work")
                .verify(List.of(new Assertion.TerraformCleanPlan(
                        ".", "o código descreve a infraestrutura que está no ar")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("divergem"));
    }

    @Test
    void planoQueNemRodaOrientaARodarInit() {
        var result = motorComTrabalho("", 1, "../work")
                .verify(List.of(new Assertion.TerraformCleanPlan(
                        ".", "o código descreve a infraestrutura que está no ar")));

        assertFalse(result.completed());
        assertTrue(result.assertions().getFirst().detail().contains("terraform init"));
    }

    @Test
    void enderecoDeModuloGanhaAspasEscapadasNoWindows() {
        String escapado = TerraformEvaluator.addressForCommandLine(
                "module.ambiente[\"producao\"].docker_container.web", true);

        assertEquals("module.ambiente[\\\"producao\\\"].docker_container.web", escapado);
    }

    @Test
    void enderecoDeModuloFicaIntactoForaDoWindows() {
        String cru = "module.ambiente[\"producao\"].docker_container.web";

        assertEquals(cru, TerraformEvaluator.addressForCommandLine(cru, false));
    }

    @Test
    void enderecoSemAspasNaoEAlterado() {
        assertEquals("docker_container.web",
                TerraformEvaluator.addressForCommandLine("docker_container.web", true));
    }

    @Test
    void avaliadorDeEstadoEntregaOEnderecoAoExecutor() {
        List<List<String>> recebidos = new java.util.ArrayList<>();
        CommandExecutor espiao = command -> {
            recebidos.add(command);
            return new CommandOutput(0, ESTADO_DO_CONTAINER, "");
        };

        new VerificationEngine(espiao, java.time.Duration.ofSeconds(1), "../work")
                .verify(List.of(new Assertion.TerraformState(
                        ".", "module.ambiente[\"producao\"].docker_container.web",
                        null, null, "o container do módulo está no state")));

        String enderecoEnviado = recebidos.getFirst().getLast();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        assertEquals(
                TerraformEvaluator.addressForCommandLine(
                        "module.ambiente[\"producao\"].docker_container.web", windows),
                enderecoEnviado);
    }
}
