package dev.learninginfra.verificacao;

import com.sun.net.httpserver.HttpServer;
import dev.learninginfra.execucao.ExecutorDeComando;
import dev.learninginfra.execucao.SaidaDeComando;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MotorDeVerificacaoTest {

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

    private MotorDeVerificacao motorQueResponde(String stdout, int codigo) {
        ExecutorDeComando falso = comando -> new SaidaDeComando(codigo, stdout, "");
        return new MotorDeVerificacao(falso);
    }

    @Test
    void containerRodandoPassaQuandoDockerDizTrue() {
        var resultado = motorQueResponde("true\n", 0)
                .verificar(List.of(new Assercao.ContainerRodando("lab-web")));

        assertTrue(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().passou());
    }

    @Test
    void containerParadoFalhaComDetalheUtil() {
        var resultado = motorQueResponde("false\n", 0)
                .verificar(List.of(new Assercao.ContainerRodando("lab-web")));

        assertFalse(resultado.concluido());
        assertEquals("existe, mas está parado", resultado.asercoes().getFirst().detalhe());
    }

    @Test
    void containerInexistenteFalhaComDetalheDiferenteDeParado() {
        var resultado = motorQueResponde("", 1)
                .verificar(List.of(new Assercao.ContainerRodando("lab-web")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("não existe"));
    }

    @Test
    void containerSaudavelPassaQuandoHealthDizHealthy() {
        var resultado = motorQueResponde("healthy\n", 0)
                .verificar(List.of(new Assercao.ContainerSaudavel("lab-08-db-1")));

        assertTrue(resultado.concluido());
    }

    @Test
    void containerSaudavelFalhaQuandoNaoEstaHealthy() {
        var resultado = motorQueResponde("starting\n", 0)
                .verificar(List.of(new Assercao.ContainerSaudavel("lab-08-db-1")));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("starting"));
    }

    @Test
    void containerEmRedePassaQuandoOPertence() {
        var resultado = motorQueResponde("lab-07-borda lab-07-interna\n", 0)
                .verificar(List.of(new Assercao.ContainerEmRede("lab-07-api-1", "lab-07-borda", true)));

        assertTrue(resultado.concluido());
    }

    @Test
    void containerEmRedeFalhaQuandoNaoPertenceMasDeveria() {
        var resultado = motorQueResponde("lab-07-borda\n", 0)
                .verificar(List.of(new Assercao.ContainerEmRede("lab-07-db-1", "lab-07-interna", true)));

        assertFalse(resultado.concluido());
    }

    @Test
    void containerForaDaRedePassaComPresenteFalse() {
        var resultado = motorQueResponde("lab-07-interna\n", 0)
                .verificar(List.of(new Assercao.ContainerEmRede("lab-07-db-1", "lab-07-borda", false)));

        assertTrue(resultado.concluido());
    }

    @Test
    void containerEmRedeIgnoraNomeParcial() {
        var resultado = motorQueResponde("lab-07\n", 0)
                .verificar(List.of(new Assercao.ContainerEmRede("lab-07-api-1", "lab-07-borda", true)));

        assertFalse(resultado.concluido(), "nome parcial de rede não pode aprovar");
    }

    @Test
    void httpRespondePassaContraServidorDeVerdade() {
        var resultado = motorQueResponde("true\n", 0)
                .verificar(List.of(new Assercao.HttpResponde(base, 200)));

        assertTrue(resultado.concluido());
    }

    @Test
    void servicoLentoNaoEhConfundidoComServicoMorto() {
        ExecutorDeComando falso = comando -> new SaidaDeComando(0, "true\n", "");
        var motorImpaciente = new MotorDeVerificacao(falso, java.time.Duration.ofSeconds(1));

        var resultado = motorImpaciente
                .verificar(List.of(new Assercao.HttpResponde(base + "/lento", 200)));

        var detalhe = resultado.asercoes().getFirst().detalhe();
        assertFalse(resultado.concluido());
        assertTrue(detalhe.contains("lento ou travado"), "veio: " + detalhe);
        assertFalse(detalhe.contains("nada respondeu"), "veio: " + detalhe);
    }

    @Test
    void httpRespondeFalhaQuandoNadaEscuta() {
        var resultado = motorQueResponde("true\n", 0)
                .verificar(List.of(new Assercao.HttpResponde("http://127.0.0.1:1", 200)));

        assertFalse(resultado.concluido());
        assertTrue(resultado.asercoes().getFirst().detalhe().contains("nada respondeu"));
    }

    @Test
    void corpoContemPassaEFalhaConformeOTexto() {
        var passou = motorQueResponde("true\n", 0)
                .verificar(List.of(new Assercao.HttpCorpoContem(base, "primeiro container")));
        var falhou = motorQueResponde("true\n", 0)
                .verificar(List.of(new Assercao.HttpCorpoContem(base, "texto ausente")));

        assertTrue(passou.concluido());
        assertFalse(falhou.concluido());
    }

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
    void comandoProduzMostraOErroRealEnaoOProgressoDeDownload() {
        ExecutorDeComando docker = comando -> new SaidaDeComando(1, "", """
                Unable to find image 'alpine:latest' locally
                latest: Pulling from library/alpine
                Status: Downloaded newer image for alpine:latest
                cat: can't open '/dados/bicho.txt': No such file or directory
                """);
        var resultado = new MotorDeVerificacao(docker).verificar(List.of(
                new Assercao.ComandoProduz(List.of("docker", "run"), "tamandua", "o dado sobreviveu")));

        var detalhe = resultado.asercoes().getFirst().detalhe();
        assertTrue(detalhe.contains("can't open"), "veio: " + detalhe);
        assertFalse(detalhe.contains("Pulling from"), "veio: " + detalhe);
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

    @Test
    void todasAsAsercoesSaoAvaliadasMesmoQuandoAPrimeiraFalha() {
        var resultado = motorQueResponde("false\n", 0).verificar(List.of(
                new Assercao.ContainerRodando("lab-web"),
                new Assercao.HttpResponde(base, 200)));

        assertEquals(2, resultado.asercoes().size());
        assertFalse(resultado.asercoes().get(0).passou());
        assertTrue(resultado.asercoes().get(1).passou());
    }

    @Test
    void condicaoKubernetesUsaWaitComTimeout() {
        List<List<String>> comandos = new java.util.ArrayList<>();
        ExecutorDeComando executor = comando -> {
            comandos.add(comando);
            return new SaidaDeComando(0, "condition met", "");
        };
        var asercao = new Assercao.KubernetesCondicao(
                "docker-desktop", "li-k8s-01", "deployment", "web",
                "Available", "True", 12, "o Deployment está disponível");

        var resultado = new MotorDeVerificacao(executor).verificar(List.of(asercao));

        assertTrue(resultado.concluido());
        assertEquals(List.of("kubectl", "--context", "docker-desktop", "--namespace",
                "li-k8s-01", "wait", "deployment/web", "--for=condition=Available=True",
                "--timeout=12s"), comandos.getFirst());
    }

    @Test
    void jsonpathKubernetesEsperaOValorComTimeout() {
        List<List<String>> comandos = new java.util.ArrayList<>();
        ExecutorDeComando executor = comando -> {
            comandos.add(comando);
            return new SaidaDeComando(0, "condition met", "");
        };
        var asercao = new Assercao.KubernetesJsonpath(
                "docker-desktop", "li-k8s-01", "pod", "web", "{.status.phase}",
                "Running", 8, "o Pod está rodando");

        var resultado = new MotorDeVerificacao(executor).verificar(List.of(asercao));

        assertTrue(resultado.concluido());
        assertEquals(List.of("kubectl", "--context", "docker-desktop", "--namespace",
                "li-k8s-01", "wait", "pod/web", "--for=jsonpath={.status.phase}=Running",
                "--timeout=8s"), comandos.getFirst());
    }

    @Test
    void rbacKubernetesValidaPermissaoENegacaoSemAceitarFalhaDeConexao() {
        var permitido = new Assercao.KubernetesRbac(
                "docker-desktop", "li-k8s-10", "auditor", "get", "pods", true,
                "o auditor lê Pods");
        var negado = new Assercao.KubernetesRbac(
                "docker-desktop", "li-k8s-10", "auditor", "delete", "pods", false,
                "o auditor não apaga Pods");

        assertTrue(motorQueResponde("yes\n", 0).verificar(List.of(permitido)).concluido());
        assertTrue(motorQueResponde("no\n", 1).verificar(List.of(negado)).concluido());
        assertFalse(motorQueResponde("", 1).verificar(List.of(negado)).concluido());
    }

    @Test
    void consultaAwsFixaEndpointRegiaoERequisicaoSemCredenciaisReais() {
        List<List<String>> comandos = new java.util.ArrayList<>();
        List<Map<String, String>> ambientes = new java.util.ArrayList<>();
        ExecutorDeComando executor = new ExecutorDeComando() {
            @Override
            public SaidaDeComando executar(List<String> comando) {
                throw new AssertionError("a consulta AWS deve declarar o ambiente sintético");
            }

            @Override
            public SaidaDeComando executar(
                    List<String> comando, Map<String, String> ambiente) {
                comandos.add(comando);
                ambientes.add(ambiente);
                return new SaidaDeComando(0, "Enabled\n", "");
            }
        };
        var asercao = new Assercao.AwsConsulta(
                "http://127.0.0.1:4566", "us-east-1", "s3api", "get-bucket-versioning",
                List.of("--bucket", "learning-infra-arquivos"), "Status", "Enabled",
                "o bucket mantém histórico de versões");

        var resultado = new MotorDeVerificacao(executor).verificar(List.of(asercao));

        assertTrue(resultado.concluido());
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
        var asercao = new Assercao.AwsConsulta(
                "http://127.0.0.1:4566", "us-east-1", "dynamodb", "describe-table",
                List.of("--table-name", "pedidos"), "Table.TableStatus", "ACTIVE",
                "a tabela está ativa");

        var apiFalhou = motorQueResponde("", 1).verificar(List.of(asercao));
        var valorErrado = motorQueResponde("CREATING\n", 0).verificar(List.of(asercao));

        assertTrue(apiFalhou.asercoes().getFirst().detalhe().contains("não consegui consultar"));
        assertTrue(valorErrado.asercoes().getFirst().detalhe().contains("CREATING"));
    }
}
