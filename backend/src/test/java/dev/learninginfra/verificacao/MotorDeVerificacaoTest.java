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
    void todasAsAsercoesSaoAvaliadasMesmoQuandoAPrimeiraFalha() {
        var resultado = motorQueResponde("false\n", 0).verificar(List.of(
                new Assercao.ContainerRodando("lab-web"),
                new Assercao.HttpResponde(base, 200)));

        assertEquals(2, resultado.asercoes().size());
        assertFalse(resultado.asercoes().get(0).passou());
        assertTrue(resultado.asercoes().get(1).passou());
    }
}
