package dev.learninginfra.verificacao;

public record ResultadoDeAsercao(String descricao, boolean passou, String detalhe) {

    public static ResultadoDeAsercao aprovada(Assercao asercao) {
        return new ResultadoDeAsercao(asercao.descricao(), true, "");
    }

    public static ResultadoDeAsercao reprovada(Assercao asercao, String detalhe) {
        return new ResultadoDeAsercao(asercao.descricao(), false, detalhe);
    }
}
