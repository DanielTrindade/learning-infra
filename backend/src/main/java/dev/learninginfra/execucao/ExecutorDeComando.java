package dev.learninginfra.execucao;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface ExecutorDeComando {

    SaidaDeComando executar(List<String> comando);

    /**
     * Executa com variáveis adicionais sem obrigar dublês simples a conhecer ambiente.
     * A implementação real sobrescreve este método; o padrão preserva a interface
     * funcional usada nos testes.
     */
    default SaidaDeComando executar(List<String> comando, Map<String, String> ambiente) {
        return executar(comando);
    }

    /**
     * Executa com um tempo limite próprio da chamada. Comandos como o `kubectl delete
     * --wait` legitimamente passam dos trinta segundos do padrão.
     */
    default SaidaDeComando executar(List<String> comando, Duration tempoLimite) {
        return executar(comando);
    }
}
