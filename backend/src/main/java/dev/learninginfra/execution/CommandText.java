package dev.learninginfra.execution;

/**
 * O erro que interessa é a última linha não vazia da saída. O {@code docker} escreve
 * progresso de download de imagem no stderr também, e sem isto o motivo real da falha
 * aparece afogado sob quatro linhas de "Pulling from library/alpine".
 */
public final class CommandText {

    private CommandText() {
    }

    public static String lastLine(String text) {
        String[] lines = text.strip().split("\\R");
        for (int i = lines.length - 1; i >= 0; i--) {
            if (!lines[i].isBlank()) {
                return lines[i].strip();
            }
        }
        return "sem detalhe";
    }

    public static String lastDetail(CommandOutput output) {
        return lastLine(output.stderr().isBlank() ? output.stdout() : output.stderr());
    }
}
