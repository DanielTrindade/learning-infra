package dev.learninginfra.track;

import java.util.List;

public record QuestionnaireResult(
        int score,
        boolean passed,
        int bestScore,
        int attempts,
        List<Feedback> feedback) {

    public QuestionnaireResult {
        feedback = List.copyOf(feedback);
    }

    public record Feedback(
            String questionId,
            boolean correct,
            String correctOption,
            String explanation,
            String review) {
    }
}
