package dev.learninginfra.track;

import dev.learninginfra.content.Scenario;

import java.util.List;

public record Track(
        String id,
        String title,
        int ordem,
        Fundamentals fundamentals,
        List<Scenario> scenarios) {

    public Track {
        scenarios = List.copyOf(scenarios);
    }
}
