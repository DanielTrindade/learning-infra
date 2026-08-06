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
        String projetoCompose,
        List<String> volumes) {

    /** Cenário sem projeto Compose e sem volumes — a maioria. */
    public Cenario(String id, String titulo, Dificuldade dificuldade, List<String> containers,
                   String markdown, Path diretorio, List<Assercao> asercoes) {
        this(id, titulo, dificuldade, containers, markdown, diretorio, asercoes, null, List.of());
    }

    /** Cenário com projeto Compose e sem volumes. */
    public Cenario(String id, String titulo, Dificuldade dificuldade, List<String> containers,
                   String markdown, Path diretorio, List<Assercao> asercoes, String projetoCompose) {
        this(id, titulo, dificuldade, containers, markdown, diretorio, asercoes, projetoCompose,
                List.of());
    }

    public boolean usaCompose() {
        return projetoCompose != null && !projetoCompose.isBlank();
    }

    public Path workspace() {
        return diretorio.resolve("workspace");
    }
}
