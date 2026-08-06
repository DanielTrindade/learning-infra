package dev.learninginfra.api.dto;

import dev.learninginfra.conteudo.Cenario;
import dev.learninginfra.conteudo.Dificuldade;
import dev.learninginfra.verificacao.Assercao;

import java.util.List;

public record CenarioDetalhado(
        String id,
        String titulo,
        Dificuldade dificuldade,
        String markdown,
        List<String> asercoes,
        List<String> containers,
        boolean ativo,
        boolean concluido) {

    public static CenarioDetalhado de(Cenario cenario, boolean ativo, boolean concluido) {
        return new CenarioDetalhado(
                cenario.id(),
                cenario.titulo(),
                cenario.dificuldade(),
                cenario.markdown(),
                cenario.asercoes().stream().map(Assercao::descricao).toList(),
                cenario.containers(),
                ativo,
                concluido);
    }
}
