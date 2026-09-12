package dev.learninginfra.execution;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class RealCommandExecutor implements CommandExecutor {

    public static final Duration TEMPO_LIMITE_PADRAO = Duration.ofSeconds(30);

    @Override
    public CommandOutput execute(List<String> command) {
        return execute(command, Map.of(), TEMPO_LIMITE_PADRAO);
    }

    @Override
    public CommandOutput execute(List<String> command, Map<String, String> ambiente) {
        return execute(command, ambiente, TEMPO_LIMITE_PADRAO);
    }

    @Override
    public CommandOutput execute(List<String> command, Duration timeLimit) {
        return execute(command, Map.of(), timeLimit);
    }

    @Override
    public CommandOutput execute(
            List<String> command, Map<String, String> ambiente, Duration timeLimit) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().putAll(ambiente);
            Process process = builder.start();
            try (ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<String> stdout = readers.submit(() -> read(process.getInputStream()));
                Future<String> stderr = readers.submit(() -> read(process.getErrorStream()));

                if (!process.waitFor(timeLimit.toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    return new CommandOutput(-1, "",
                            "tempo esgotado após " + timeLimit.toSeconds() + "s");
                }
                return new CommandOutput(process.exitValue(), stdout.get(), stderr.get());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui executar " + command, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CommandOutput(-1, "", "interrompido");
        } catch (ExecutionException e) {
            return new CommandOutput(-1, "", "falha lendo a saída: " + e.getCause());
        }
    }

    private String read(InputStream input) throws IOException {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
}
