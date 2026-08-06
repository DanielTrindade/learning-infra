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
        List<Assercao> asercoes) {

    public Path workspace() {
        return diretorio.resolve("workspace");
    }
}
