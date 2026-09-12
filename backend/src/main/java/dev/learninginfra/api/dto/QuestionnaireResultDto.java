package dev.learninginfra.api.dto;

import dev.learninginfra.track.QuestionnaireResult;

import java.util.List;

public record QuestionnaireResultDto(
        int score,
        boolean passed,
        int bestScore,
        int attempts,
        List<Feedback> feedback) {

    public static QuestionnaireResultDto from(QuestionnaireResult result) {
        return new QuestionnaireResultDto(
                result.score(),
                result.passed(),
                result.bestScore(),
                result.attempts(),
                result.feedback().stream()
                        .map(item -> new Feedback(
                                item.questionId(),
                                item.correct(),
                                item.correctOption(),
                                item.explanation(),
                                item.review()))
                        .toList());
    }

    public record Feedback(
            String questionId,
            boolean correct,
            String correctOption,
            String explanation,
            String review) {
    }
}
