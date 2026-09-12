package dev.learninginfra.progress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressRepositoryTest {

    @TempDir
    Path root;

    @Test
    void loadsLegacyKeysAndMigratesTheFile() throws Exception {
        Path data = root.resolve("data");
        Files.createDirectories(data);
        Path legacy = data.resolve("progresso.json");
        Files.writeString(legacy, """
                {
                  "cenarioAtivo": "docker/01-primeiro",
                  "concluidos": {
                    "docker/00-anterior": "2026-08-06T10:00:00Z"
                  },
                  "fundamentos": {
                    "docker": {
                      "tentativas": 2,
                      "melhorPercentual": 83,
                      "concluidoEm": "2026-08-07T12:00:00Z"
                    }
                  }
                }
                """);
        Path migrated = data.resolve("progress.json");
        var repository = new ProgressRepository(migrated.toString());

        Progress progress = repository.load();

        assertThat(progress.activeScenario()).isEqualTo("docker/01-primeiro");
        assertThat(progress.completed())
                .containsEntry("docker/00-anterior", "2026-08-06T10:00:00Z");
        assertThat(progress.fundamentals().get("docker"))
                .satisfies(fundamentals -> {
                    assertThat(fundamentals.attempts()).isEqualTo(2);
                    assertThat(fundamentals.bestScore()).isEqualTo(83);
                    assertThat(fundamentals.completedAt()).isEqualTo("2026-08-07T12:00:00Z");
                    assertThat(fundamentals.completed()).isTrue();
                });
        assertThat(Files.readString(migrated)).contains("cenarioAtivo");
        assertThat(Files.readString(legacy)).isNotEmpty();
    }

    @Test
    void writesTheNewFormatAfterMigration() throws Exception {
        Path data = root.resolve("data");
        Files.createDirectories(data);
        Files.writeString(data.resolve("progresso.json"), """
                {"cenarioAtivo": "docker/01", "concluidos": {"docker/00": "2026-08-06T10:00:00Z"}}
                """);
        Path migrated = data.resolve("progress.json");
        var repository = new ProgressRepository(migrated.toString());

        repository.save(repository.load());

        String written = Files.readString(migrated);
        assertThat(written).contains("activeScenario");
        assertThat(written).contains("completed");
    }

    @Test
    void corruptedFileGoesToQuarantineAndBecomesEmptyProgress() throws Exception {
        Path file = root.resolve("data/progress.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{ isto não é json");

        var repository = new ProgressRepository(file.toString());
        Progress progress = repository.load();

        assertThat(progress.activeScenario()).isNull();
        assertThat(progress.completed()).isEmpty();
        assertThat(Files.exists(file)).isFalse();
        try (var files = Files.list(file.getParent())) {
            assertThat(files.map(path -> path.getFileName().toString()).toList())
                    .filteredOn(name -> name.startsWith("progress.corrompido-")
                            && name.endsWith(".json"))
                    .hasSize(1);
        }
    }

    @Test
    void saveLeavesNoTemporaryFileBehind() throws Exception {
        Path file = root.resolve("data/progress.json");
        var repository = new ProgressRepository(file.toString());

        repository.save(Progress.empty().withActiveScenario("docker/01"));

        assertThat(Files.exists(file.resolveSibling("progress.json.tmp"))).isFalse();
        assertThat(repository.load().activeScenario()).isEqualTo("docker/01");
    }

    @Test
    void concurrentUpdatesDoNotLoseWrites() throws Exception {
        Path file = root.resolve("data/progress.json");
        var repository = new ProgressRepository(file.toString());
        int tasks = 16;
        var start = new java.util.concurrent.CountDownLatch(1);

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                int index = i;
                executor.submit(() -> {
                    start.await();
                    repository.update(progress -> progress.withCompleted(
                            "docker/" + index, "2026-09-10T00:00:00Z"));
                    return null;
                });
            }
            start.countDown();
        }

        assertThat(repository.load().completed()).hasSize(tasks);
    }
}
