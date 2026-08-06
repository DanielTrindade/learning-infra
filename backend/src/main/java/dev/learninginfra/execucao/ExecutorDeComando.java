package dev.learninginfra.execucao;

import java.util.List;

public interface ExecutorDeComando {

    SaidaDeComando executar(List<String> comando);
}
