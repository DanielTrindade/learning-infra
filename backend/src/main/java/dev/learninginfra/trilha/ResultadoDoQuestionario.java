package dev.learninginfra.trilha;

import java.util.List;

public record ResultadoDoQuestionario(
        int percentual,
        boolean aprovado,
        int melhorPercentual,
        int tentativas,
        List<Feedback> feedback) {

    public ResultadoDoQuestionario {
        feedback = List.copyOf(feedback);
    }

    public record Feedback(
            String questaoId,
            boolean acertou,
            String alternativaCorreta,
            String explicacao,
            String revisar) {
    }
}
