package dev.learninginfra.progress;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.LinkedHashMap;
import java.util.Map;

public record Progress(
        @JsonAlias("cenarioAtivo") String activeScenario,
        @JsonAlias("concluidos") Map<String, String> completed,
        @JsonAlias("fundamentos") Map<String, FundamentalsProgress> fundamentals) {

    public Progress {
        completed = completed == null ? Map.of() : Map.copyOf(completed);
        fundamentals = fundamentals == null ? Map.of() : Map.copyOf(fundamentals);
    }

    public static Progress empty() {
        return new Progress(null, Map.of(), Map.of());
    }

    public Progress withActiveScenario(String id) {
        return new Progress(id, completed, fundamentals);
    }

    public Progress withCompleted(String id, String instant) {
        var novos = new LinkedHashMap<>(completed);
        novos.put(id, instant);
        return new Progress(activeScenario, novos, fundamentals);
    }

    public Progress withoutTrackProgress(String trackId) {
        var remainingCompleted = new LinkedHashMap<>(completed);
        remainingCompleted.keySet().removeIf(id -> id.startsWith(trackId + "/"));
        var remainingFundamentals = new LinkedHashMap<>(fundamentals);
        remainingFundamentals.remove(trackId);
        // O ambiente ativo continua rastreado para o teardown do próximo cenário.
        return new Progress(activeScenario, remainingCompleted, remainingFundamentals);
    }

    public Progress withFundamentals(
            String trackId, FundamentalsProgress novoProgresso) {
        var novos = new LinkedHashMap<>(fundamentals);
        novos.put(trackId, novoProgresso);
        return new Progress(activeScenario, completed, novos);
    }
}
