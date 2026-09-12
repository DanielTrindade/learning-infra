package dev.learninginfra.execution;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RealCommandExecutorTest {

    @Test
    void capturaSaidaEcodigoDeComandoQueExiste() {
        CommandOutput saida = new RealCommandExecutor().execute(List.of("docker", "--version"));

        assertTrue(saida.success(), "esperava sucesso, veio: " + saida);
        assertTrue(saida.stdout().toLowerCase().contains("docker"));
    }

    @Test
    void devolveCodigoDeErroSemLancarQuandoOComandoFalha() {
        CommandOutput saida = new RealCommandExecutor()
                .execute(List.of("docker", "inspect", "container-que-nao-existe-jamais"));

        assertFalse(saida.success());
    }

    @Test
    void acrescentaAmbienteSomenteAoProcessoFilho() {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        List<String> command = windows
                ? List.of("cmd.exe", "/c", "echo", "%LEARNING_INFRA_TEST%")
                : List.of("sh", "-c", "echo \"$LEARNING_INFRA_TEST\"");

        CommandOutput saida = new RealCommandExecutor().execute(
                command,
                Map.of("LEARNING_INFRA_TEST", "credencial-sintetica"));

        assertTrue(saida.success());
        assertEquals("credencial-sintetica", saida.stdout().strip());
    }

    @Test
    void respeitaOTempoLimiteDaChamada() {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        List<String> command = windows
                ? List.of("cmd.exe", "/c", "ping", "-n", "6", "127.0.0.1")
                : List.of("sh", "-c", "sleep 5");

        CommandOutput saida = new RealCommandExecutor().execute(
                command, java.time.Duration.ofSeconds(1));

        assertFalse(saida.success());
        assertTrue(saida.stderr().contains("tempo esgotado"), "veio: " + saida);
    }
}
