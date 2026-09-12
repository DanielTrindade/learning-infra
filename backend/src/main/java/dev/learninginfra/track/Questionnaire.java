package dev.learninginfra.track;

import java.util.List;

public record Questionnaire(List<Question> questions) {

    public Questionnaire {
        questions = List.copyOf(questions);
    }

    public record Question(
            String id,
            String statement,
            List<Option> options,
            String correctOption,
            String explanation,
            String review) {

        public Question {
            options = List.copyOf(options);
        }
    }

    public record Option(String id, String text) {
    }
}
