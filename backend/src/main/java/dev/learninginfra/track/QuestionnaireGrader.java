package dev.learninginfra.track;

import dev.learninginfra.progress.FundamentalsProgress;
import dev.learninginfra.progress.ProgressRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Corrige uma submissão do Questionário e registra o resultado no progresso. O catálogo
 * só responde o que está publicado; a regra de correção e a escrita ficam aqui.
 */
@Component
public class QuestionnaireGrader {

    private final TrackCatalog catalog;
    private final ProgressRepository repositorioDeProgresso;
    private final Clock clock;

    public QuestionnaireGrader(
            TrackCatalog catalog,
            ProgressRepository repositorioDeProgresso,
            Clock clock) {
        this.catalog = catalog;
        this.repositorioDeProgresso = repositorioDeProgresso;
        this.clock = clock;
    }

    public QuestionnaireResult answer(
            String trackId, Map<String, String> answers) {
        Track track = catalog.find(trackId).orElseThrow(
                () -> new IllegalArgumentException("Trilha não encontrada: " + trackId));
        if (track.fundamentals() == null) {
            throw new IllegalArgumentException(
                    "Fundamentos não publicados para a Trilha: " + trackId);
        }

        Questionnaire questionario = track.fundamentals().questionario();
        Set<String> idsEsperados = questionario.questions().stream()
                .map(Questionnaire.Question::id)
                .collect(Collectors.toSet());
        if (answers == null || answers.size() != idsEsperados.size()
                || !answers.keySet().equals(idsEsperados)) {
            throw new IllegalArgumentException(
                    "a submissão deve conter uma resposta válida para cada questão");
        }

        int correctCount = 0;
        var feedback = new ArrayList<QuestionnaireResult.Feedback>();
        for (Questionnaire.Question question : questionario.questions()) {
            String response = answers.get(question.id());
            boolean alternativaExiste = question.options().stream()
                    .anyMatch(option -> option.id().equals(response));
            if (!alternativaExiste) {
                throw new IllegalArgumentException(
                        "alternativa desconhecida para a questão " + question.id() + ": " + response);
            }
            boolean correct = question.correctOption().equals(response);
            if (correct) {
                correctCount++;
            }
            feedback.add(new QuestionnaireResult.Feedback(
                    question.id(),
                    correct,
                    question.correctOption(),
                    question.explanation(),
                    question.review()));
        }

        int score = correctCount * 100 / questionario.questions().size();
        boolean passed = score >= track.fundamentals().minimumScore();
        FundamentalsProgress updated = repositorioDeProgresso.update(progress -> {
            FundamentalsProgress previous = progress.fundamentals()
                    .getOrDefault(trackId, FundamentalsProgress.empty());
            return progress.withFundamentals(trackId, previous.register(
                    score, passed, clock.instant().toString()));
        }).fundamentals().get(trackId);

        return new QuestionnaireResult(
                score,
                passed,
                updated.bestScore(),
                updated.attempts(),
                feedback);
    }
}
