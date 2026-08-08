package dev.learninginfra.progresso;

public record ProgressoDosFundamentos(
        int tentativas,
        int melhorPercentual,
        String concluidoEm) {

    public static ProgressoDosFundamentos vazio() {
        return new ProgressoDosFundamentos(0, 0, null);
    }

    public ProgressoDosFundamentos registrar(
            int percentual, boolean aprovado, String instante) {
        String primeiraAprovacao = concluidoEm;
        if (aprovado && primeiraAprovacao == null) {
            primeiraAprovacao = instante;
        }
        return new ProgressoDosFundamentos(
                tentativas + 1,
                Math.max(melhorPercentual, percentual),
                primeiraAprovacao);
    }

    public boolean concluido() {
        return concluidoEm != null;
    }
}
