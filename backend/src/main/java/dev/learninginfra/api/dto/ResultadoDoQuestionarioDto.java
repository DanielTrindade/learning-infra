package dev.learninginfra.api.dto;

import dev.learninginfra.trilha.ResultadoDoQuestionario;

import java.util.List;

public record ResultadoDoQuestionarioDto(
        int percentual,
        boolean aprovado,
        int melhorPercentual,
        int tentativas,
        List<Feedback> feedback) {

    public static ResultadoDoQuestionarioDto de(ResultadoDoQuestionario resultado) {
        return new ResultadoDoQuestionarioDto(
                resultado.percentual(),
                resultado.aprovado(),
                resultado.melhorPercentual(),
                resultado.tentativas(),
                resultado.feedback().stream()
                        .map(item -> new Feedback(
                                item.questaoId(),
                                item.acertou(),
                                item.alternativaCorreta(),
                                item.explicacao(),
                                item.revisar()))
                        .toList());
    }

    public record Feedback(
            String questaoId,
            boolean acertou,
            String alternativaCorreta,
            String explicacao,
            String revisar) {
    }
}
