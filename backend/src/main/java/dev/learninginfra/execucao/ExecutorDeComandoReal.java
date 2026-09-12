package dev.learninginfra.execucao;

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
public class ExecutorDeComandoReal implements ExecutorDeComando {

    private static final Duration TEMPO_LIMITE_PADRAO = Duration.ofSeconds(30);

    @Override
    public SaidaDeComando executar(List<String> comando) {
        return executar(comando, Map.of(), TEMPO_LIMITE_PADRAO);
    }

    @Override
    public SaidaDeComando executar(List<String> comando, Map<String, String> ambiente) {
        return executar(comando, ambiente, TEMPO_LIMITE_PADRAO);
    }

    @Override
    public SaidaDeComando executar(List<String> comando, Duration tempoLimite) {
        return executar(comando, Map.of(), tempoLimite);
    }

    private SaidaDeComando executar(
            List<String> comando, Map<String, String> ambiente, Duration tempoLimite) {
        try {
            ProcessBuilder construtor = new ProcessBuilder(comando);
            construtor.environment().putAll(ambiente);
            Process processo = construtor.start();
            try (ExecutorService leitores = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<String> stdout = leitores.submit(() -> ler(processo.getInputStream()));
                Future<String> stderr = leitores.submit(() -> ler(processo.getErrorStream()));

                if (!processo.waitFor(tempoLimite.toMillis(), TimeUnit.MILLISECONDS)) {
                    processo.destroyForcibly();
                    return new SaidaDeComando(-1, "",
                            "tempo esgotado após " + tempoLimite.toSeconds() + "s");
                }
                return new SaidaDeComando(processo.exitValue(), stdout.get(), stderr.get());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui executar " + comando, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new SaidaDeComando(-1, "", "interrompido");
        } catch (ExecutionException e) {
            return new SaidaDeComando(-1, "", "falha lendo a saída: " + e.getCause());
        }
    }

    private String ler(InputStream entrada) throws IOException {
        return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
    }
}
