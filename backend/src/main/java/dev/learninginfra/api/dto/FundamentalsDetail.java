package dev.learninginfra.api.dto;

import dev.learninginfra.progress.FundamentalsState;
import dev.learninginfra.progress.FundamentalsProgress;
import dev.learninginfra.track.Fundamentals;
import dev.learninginfra.track.Questionnaire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record FundamentalsDetail(
        String trackId,
        String title,
        String markdown,
        int minimumScore,
        FundamentalsState state,
        int bestScore,
        int attempts,
        List<Question> questions) {

    public static FundamentalsDetail from(
            String trackId,
            Fundamentals fundamentals,
            FundamentalsProgress progress) {
        var questions = new ArrayList<>(fundamentals.questionario().questions().stream()
                .map(FundamentalsDetail::questaoPublica)
                .toList());
        Collections.shuffle(questions);
        return new FundamentalsDetail(
                trackId,
                fundamentals.title(),
                fundamentals.markdown(),
                fundamentals.minimumScore(),
                FundamentalsState.from(progress),
                progress.bestScore(),
                progress.attempts(),
                List.copyOf(questions));
    }

    private static Question questaoPublica(Questionnaire.Question question) {
        var options = new ArrayList<>(question.options().stream()
                .map(option -> new Option(option.id(), option.text()))
                .toList());
        Collections.shuffle(options);
        return new Question(question.id(), question.statement(), List.copyOf(options));
    }

    public record Question(
            String id,
            String statement,
            List<Option> options) {
    }

    public record Option(String id, String text) {
    }
}
