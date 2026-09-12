package dev.learninginfra.content;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DifficultyTest {

    @Test
    void aceitaNomeComAcentoEmPortugues() {
        assertThat(Difficulty.fromText("autônomo")).isEqualTo(Difficulty.AUTONOMOUS);
    }
}
