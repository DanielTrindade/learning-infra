package dev.learninginfra.conteudo;

import java.text.Normalizer;
import java.util.Locale;

public enum Dificuldade {
    GUIADO, ASSISTIDO, AUTONOMO, MESTRE;

    public static Dificuldade deTexto(String texto) {
        try {
            String textoNormalizado = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toUpperCase(Locale.ROOT);
            return valueOf(textoNormalizado);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("dificuldade desconhecida: " + texto);
        }
    }
}
