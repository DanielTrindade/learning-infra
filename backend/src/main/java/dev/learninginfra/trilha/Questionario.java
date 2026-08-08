package dev.learninginfra.trilha;

import java.util.List;

public record Questionario(List<Questao> questoes) {

    public Questionario {
        questoes = List.copyOf(questoes);
    }

    public record Questao(
            String id,
            String enunciado,
            List<Alternativa> alternativas,
            String alternativaCorreta,
            String explicacao,
            String revisar) {

        public Questao {
            alternativas = List.copyOf(alternativas);
        }
    }

    public record Alternativa(String id, String texto) {
    }
}
