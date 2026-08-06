package dev.learninginfra.verificacao;

import java.util.List;

public record ResultadoDaVerificacao(boolean concluido, List<ResultadoDeAsercao> asercoes) {
}
