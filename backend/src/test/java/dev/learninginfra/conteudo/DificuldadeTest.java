package dev.learninginfra.conteudo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DificuldadeTest {

    @Test
    void aceitaNomeComAcentoEmPortugues() {
        assertThat(Dificuldade.deTexto("autônomo")).isEqualTo(Dificuldade.AUTONOMO);
    }
}
