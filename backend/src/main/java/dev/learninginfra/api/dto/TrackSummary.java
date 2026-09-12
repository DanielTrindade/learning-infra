package dev.learninginfra.api.dto;

import dev.learninginfra.content.Difficulty;
import dev.learninginfra.progress.FundamentalsState;
import dev.learninginfra.progress.Progress;
import dev.learninginfra.progress.FundamentalsProgress;
import dev.learninginfra.track.Track;

import java.util.List;

public record TrackSummary(
        String id,
        String title,
        FundamentalsSummary fundamentals,
        List<ScenarioSummary> scenarios,
        int completed,
        int total,
        int score,
        boolean allCompleted) {

    public static TrackSummary from(Track track, Progress progress) {
        FundamentalsProgress progressoTeorico = progress.fundamentals()
                .getOrDefault(track.id(), FundamentalsProgress.empty());
        FundamentalsSummary fundamentals = track.fundamentals() == null
                ? null
                : new FundamentalsSummary(
                        track.fundamentals().title(),
                        FundamentalsState.from(progressoTeorico),
                        progressoTeorico.bestScore(),
                        progressoTeorico.attempts());
        List<ScenarioSummary> scenarios = track.scenarios().stream()
                .map(scenario -> new ScenarioSummary(
                        scenario.id(),
                        scenario.title(),
                        scenario.difficulty(),
                        scenario.assertions().size(),
                        scenario.id().equals(progress.activeScenario()),
                        progress.completed().containsKey(scenario.id())))
                .toList();
        int completed = (fundamentals != null && progressoTeorico.completed() ? 1 : 0)
                + (int) scenarios.stream().filter(ScenarioSummary::completed).count();
        int total = scenarios.size() + (fundamentals == null ? 0 : 1);
        int score = total == 0 ? 0 : completed * 100 / total;
        return new TrackSummary(
                track.id(),
                track.title(),
                fundamentals,
                scenarios,
                completed,
                total,
                score,
                total > 0 && completed == total);
    }

    public record FundamentalsSummary(
            String title,
            FundamentalsState state,
            int bestScore,
            int attempts) {
    }

    public record ScenarioSummary(
            String id,
            String title,
            Difficulty difficulty,
            int assertionCount,
            boolean active,
            boolean completed) {
    }
}
