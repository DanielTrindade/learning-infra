package dev.learninginfra.execution;

/** Reconhece os erros do kubectl que significam "o recurso já não existe". */
public final class KubernetesErrors {

    private KubernetesErrors() {
    }

    /**
     * No teardown, a ausência do recurso é sucesso: o objetivo é não haver sobra. O
     * kubectl escreve `Error from server (NotFound): ... not found` para namespace e
     * para os demais recursos removidos.
     */
    public static boolean looksMissing(CommandOutput output) {
        String error = output.stderr().toLowerCase();
        return error.contains("not found") || error.contains("notfound");
    }
}