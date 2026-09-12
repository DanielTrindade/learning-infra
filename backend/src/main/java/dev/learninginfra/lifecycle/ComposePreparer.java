package dev.learninginfra.lifecycle;

import dev.learninginfra.content.Scenario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import dev.learninginfra.execution.CommandExecutor;
import dev.learninginfra.execution.CommandOutput;

/** Sobe o stack quando o workspace materializado traz um `compose.yaml`. */
class ComposePreparer {

    /**
     * O primeiro build da Trilha Linux passa de um minuto a frio, muito além do padrão
     * de trinta segundos do executor. O teto generoso cobre uma máquina carregada sem
     * transformar um build travado em espera infinita.
     */
    private static final Duration TEMPO_LIMITE = Duration.ofMinutes(10);

    private final CommandExecutor executor;

    ComposePreparer(CommandExecutor executor) {
        this.executor = executor;
    }

    void start(Scenario scenario, Path work) {
        Path file = work.resolve("compose.yaml");
        if (!scenario.usesCompose() || !Files.isRegularFile(file)) {
            return;
        }
        CommandOutput output = executor.execute(List.of(
                "docker", "compose", "-p", scenario.composeProject(),
                "-f", file.toString(), "up", "-d", "--build"), TEMPO_LIMITE);
        if (!output.success()) {
            throw new IllegalStateException(
                    "não consegui subir o projeto Compose `" + scenario.composeProject()
                    + "` do Cenário " + scenario.id() + ": " + output.stderr().strip()
                    + " — o ambiente deste Cenário não está pronto");
        }
    }
}
