package dev.learninginfra.conteudo;

import java.util.Locale;

public enum Dificuldade {
    GUIADO, ASSISTIDO, AUTONOMO, MESTRE;

    public static Dificuldade deTexto(String texto) {
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("dificuldade desconhecida: " + texto);
        }
    }
}
