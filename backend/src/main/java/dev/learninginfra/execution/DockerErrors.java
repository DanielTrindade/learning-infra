package dev.learninginfra.execution;

/** Reconhece os erros do Docker que significam "o recurso já não existe". */
public final class DockerErrors {

    private DockerErrors() {
    }

    /**
     * No teardown, a ausência do recurso é sucesso: o objetivo é não haver sobra. Os
     * textos cobrem container, objeto e volume — cada um tem a sua mensagem própria — e
     * o genérico `not found` fecha o resto.
     */
    public static boolean looksMissing(CommandOutput output) {
        String error = output.stderr().toLowerCase();
        return error.contains("no such container")
                || error.contains("no such object")
                || error.contains("no such volume")
                || error.contains("not found");
    }
}
