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
        List<String> volumes,
        String contextoKubernetes,
        String namespaceKubernetes,
        String manifestosIniciais,
        boolean ministack,
        boolean infraestruturaRealAws,
        String inicializacaoAws) {

    /** Cenário sem projeto Compose e sem volumes — a maioria. */
    public Cenario(String id, String titulo, Dificuldade dificuldade, List<String> containers,
                   String markdown, Path diretorio, List<Assercao> asercoes) {
        this(id, titulo, dificuldade, containers, markdown, diretorio, asercoes,
                null, List.of(), null, null, null, false, false, null);
    }

    /** Cenário com projeto Compose e sem volumes. */
    public Cenario(String id, String titulo, Dificuldade dificuldade, List<String> containers,
                   String markdown, Path diretorio, List<Assercao> asercoes, String projetoCompose) {
        this(id, titulo, dificuldade, containers, markdown, diretorio, asercoes, projetoCompose,
                List.of(), null, null, null, false, false, null);
    }

    /** Compatibilidade para Cenários Docker com lifecycle completo. */
    public Cenario(String id, String titulo, Dificuldade dificuldade, List<String> containers,
                   String markdown, Path diretorio, List<Assercao> asercoes, String projetoCompose,
                   List<String> volumes) {
        this(id, titulo, dificuldade, containers, markdown, diretorio, asercoes, projetoCompose,
                volumes, null, null, null, false, false, null);
    }

    /** Compatibilidade para Cenários Kubernetes anteriores à Trilha AWS. */
    public Cenario(String id, String titulo, Dificuldade dificuldade, List<String> containers,
                   String markdown, Path diretorio, List<Assercao> asercoes, String projetoCompose,
                   List<String> volumes, String contextoKubernetes, String namespaceKubernetes,
                   String manifestosIniciais) {
        this(id, titulo, dificuldade, containers, markdown, diretorio, asercoes, projetoCompose,
                volumes, contextoKubernetes, namespaceKubernetes, manifestosIniciais,
                false, false, null);
    }

    public boolean usaCompose() {
        return projetoCompose != null && !projetoCompose.isBlank();
    }

    public boolean usaDocker() {
        return id.startsWith("docker/");
    }

    public boolean usaKubernetes() {
        return contextoKubernetes != null && !contextoKubernetes.isBlank()
                && namespaceKubernetes != null && !namespaceKubernetes.isBlank();
    }

    public boolean temManifestosIniciais() {
        return manifestosIniciais != null && !manifestosIniciais.isBlank();
    }

    public boolean usaAws() {
        return ministack;
    }

    public boolean temInicializacaoAws() {
        return inicializacaoAws != null && !inicializacaoAws.isBlank();
    }

    public Path workspace() {
        return diretorio.resolve("workspace");
    }
}
