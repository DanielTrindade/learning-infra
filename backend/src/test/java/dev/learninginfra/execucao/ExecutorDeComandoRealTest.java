package dev.learninginfra.execucao;

import org.junit.jupiter.api.Test;

import java.util.List;

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
}
