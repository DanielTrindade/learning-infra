package dev.learninginfra.execucao;

public record SaidaDeComando(int codigoDeSaida, String stdout, String stderr) {

    public boolean sucesso() {
        return codigoDeSaida == 0;
    }
}
