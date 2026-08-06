package dev.learninginfra.conteudo;

import dev.learninginfra.verificacao.Assercao;

import java.nio.file.Path;
import java.util.List;

public record Cenario(
        String id,
        String titulo,
        Dificuldade dificuldade,
        List<String> containers,
        String markdown,
        Path diretorio,
        List<Assercao> asercoes,
        String projetoCompose) {

    /** Cenário sem projeto Compose — a maioria. */
    public Cenario(String id, String titulo, Dificuldade dificuldade, List<String> containers,
                   String markdown, Path diretorio, List<Assercao> asercoes) {
        this(id, titulo, dificuldade, containers, markdown, diretorio, asercoes, null);
    }

    public boolean usaCompose() {
        return projetoCompose != null && !projetoCompose.isBlank();
    }

    public Path workspace() {
        return diretorio.resolve("workspace");
    }
}
