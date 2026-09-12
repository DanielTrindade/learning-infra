package dev.learninginfra.verification;

/**
 * Como o valor observado de uma consulta externa é comparado com o esperado.
 *
 * <p>{@link #EXACT} é o padrão: alguma linha da saída, após {@code strip}, precisa ser
 * igual ao esperado. É o que impede que {@code INACTIVE} aprove {@code ACTIVE} e que
 * {@code "10"} aprove {@code "1"}. {@link #CONTAINS} existe para os casos em que o
 * conteúdo consulta um trecho de um valor maior de propósito, como o prefixo de um id
 * de recurso ({@code igw-}) ou um pedaço de um status composto ({@code COMPLETE} em
 * {@code CREATE_COMPLETE}).
 *
 * <p>O schema do conteúdo permanece em português: {@code comparacao: contem} e
 * {@code comparacao: exato} mapeiam para os termos em inglês.
 */
public enum Comparison {
    EXACT, CONTAINS;

    public static Comparison fromText(String text) {
        if (text == null) {
            return EXACT;
        }
        return switch (text.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "exato", "exata", "exact" -> EXACT;
            case "contem", "contains" -> CONTAINS;
            default -> throw new IllegalArgumentException("comparação desconhecida: " + text);
        };
    }
}
