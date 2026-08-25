package dev.learninginfra.trilha;

import dev.learninginfra.conteudo.Cenario;

import java.util.List;

public record Trilha(
        String id,
        String titulo,
        int ordem,
        Fundamentos fundamentos,
        List<Cenario> cenarios) {

    public Trilha {
        cenarios = List.copyOf(cenarios);
    }
}
