package dev.learninginfra.execucao;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorDeComandoRealTest {

    @Test
    void capturaSaidaEcodigoDeComandoQueExiste() {
        SaidaDeComando saida = new ExecutorDeComandoReal().executar(List.of("docker", "--version"));

        assertTrue(saida.sucesso(), "esperava sucesso, veio: " + saida);
        assertTrue(saida.stdout().toLowerCase().contains("docker"));
    }

    @Test
    void devolveCodigoDeErroSemLancarQuandoOComandoFalha() {
        SaidaDeComando saida = new ExecutorDeComandoReal()
                .executar(List.of("docker", "inspect", "container-que-nao-existe-jamais"));

        assertFalse(saida.sucesso());
    }

    @Test
    void acrescentaAmbienteSomenteAoProcessoFilho() {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        List<String> comando = windows
                ? List.of("cmd.exe", "/c", "echo", "%LEARNING_INFRA_TEST%")
                : List.of("sh", "-c", "echo \"$LEARNING_INFRA_TEST\"");

        SaidaDeComando saida = new ExecutorDeComandoReal().executar(
                comando,
                Map.of("LEARNING_INFRA_TEST", "credencial-sintetica"));

        assertTrue(saida.sucesso());
        assertEquals("credencial-sintetica", saida.stdout().strip());
    }
}
