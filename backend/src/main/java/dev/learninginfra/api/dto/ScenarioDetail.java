package dev.learninginfra.api.dto;

import dev.learninginfra.content.Scenario;
import dev.learninginfra.content.Difficulty;
import dev.learninginfra.verification.Assertion;

import java.util.List;

public record ScenarioDetail(
        String id,
        String title,
        Difficulty difficulty,
        String markdown,
        List<String> assertions,
        List<String> containers,
        boolean active,
        boolean completed) {

    public static ScenarioDetail from(Scenario scenario, boolean active, boolean completed) {
        return new ScenarioDetail(
                scenario.id(),
                scenario.title(),
                scenario.difficulty(),
                scenario.markdown(),
                scenario.assertions().stream().map(Assertion::description).toList(),
                scenario.containers(),
                active,
                completed);
    }
}
