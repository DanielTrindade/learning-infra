package dev.learninginfra.content;

import java.text.Normalizer;
import java.util.Locale;

public enum Difficulty {
    GUIDED, ASSISTED, AUTONOMOUS, MASTER;

    /**
     * O schema do conteúdo permanece em português (`dificuldade: autonomo`); o código
     * usa os termos em inglês. Os dois vocabulários são aceitos — inclusive na forma
     * normalizada, sem acento.
     */
    public static Difficulty fromText(String text) {
        String normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "GUIADO", "GUIDED" -> GUIDED;
            case "ASSISTIDO", "ASSISTED" -> ASSISTED;
            case "AUTONOMO", "AUTONOMOUS" -> AUTONOMOUS;
            case "MESTRE", "MASTER" -> MASTER;
            default -> throw new IllegalArgumentException("dificuldade desconhecida: " + text);
        };
    }
}
