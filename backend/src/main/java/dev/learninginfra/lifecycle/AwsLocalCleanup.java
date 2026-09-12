package dev.learninginfra.lifecycle;

import dev.learninginfra.execution.DockerErrors;
import dev.learninginfra.execution.CommandExecutor;
import dev.learninginfra.execution.CommandOutput;
import dev.learninginfra.execution.CommandText;

import java.util.List;

/**
 * Remove o MiniStack e todos os containers com o label global `ministack`. É usado
 * tanto para preparar um ambiente AWS limpo quanto no teardown de um Cenário AWS.
 */
class AwsLocalCleanup {

    static final String CONTAINER_MINISTACK = "learning-infra-ministack";

    private final CommandExecutor executor;

    AwsLocalCleanup(CommandExecutor executor) {
        this.executor = executor;
    }

    void clean(String action) {
        CommandOutput main = executor.execute(
                List.of("docker", "rm", "-f", "-v", CONTAINER_MINISTACK));
        if (!main.success() && !DockerErrors.looksMissing(main)) {
            throw new IllegalStateException(
                    "não consegui remover o container do MiniStack para " + action + ": "
                    + CommandText.lastDetail(main));
        }

        CommandOutput listing = executor.execute(List.of(
                "docker", "ps", "-aq", "--filter", "label=ministack"));
        if (!listing.success()) {
            throw new IllegalStateException(
                    "não consegui localizar os sidecars do MiniStack para " + action + ": "
                    + CommandText.lastDetail(listing));
        }

        for (String line : listing.stdout().lines().toList()) {
            String id = line.strip();
            if (id.matches("[0-9a-f]{12,64}")) {
                CommandOutput removed = executor.execute(
                        List.of("docker", "rm", "-f", "-v", id));
                if (!removed.success() && !DockerErrors.looksMissing(removed)) {
                    throw new IllegalStateException(
                            "não consegui remover o sidecar `" + id + "` do MiniStack para "
                            + action + ": " + CommandText.lastDetail(removed));
                }
            }
        }
    }
}
