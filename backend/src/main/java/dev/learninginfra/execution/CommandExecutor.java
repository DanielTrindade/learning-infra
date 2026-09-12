package dev.learninginfra.execution;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface CommandExecutor {

    CommandOutput execute(List<String> command);

    /**
     * Executa com variáveis adicionais sem obrigar dublês simples a conhecer ambiente.
     * A implementação real sobrescreve este método; o padrão preserva a interface
     * funcional usada nos testes.
     */
    default CommandOutput execute(List<String> command, Map<String, String> ambiente) {
        return execute(command);
    }

    /**
     * Executa com um tempo limite próprio da chamada. Comandos como o build do Compose
     * e o `terraform plan` legitimamente passam dos trinta segundos do padrão.
     */
    default CommandOutput execute(List<String> command, Duration timeLimit) {
        return execute(command);
    }

    /** Tempo limite próprio e ambiente adicional, na mesma chamada. */
    default CommandOutput execute(
            List<String> command, Map<String, String> ambiente, Duration timeLimit) {
        return execute(command, ambiente);
    }
}
