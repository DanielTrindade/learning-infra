package dev.learninginfra.progresso;

import java.util.LinkedHashMap;
import java.util.Map;

public record Progresso(String cenarioAtivo, Map<String, String> concluidos) {

    public static Progresso vazio() {
        return new Progresso(null, Map.of());
    }

    public Progresso comAtivo(String id) {
        return new Progresso(id, concluidos);
    }

    public Progresso comConcluido(String id, String instante) {
        var novos = new LinkedHashMap<>(concluidos);
        novos.put(id, instante);
        return new Progresso(cenarioAtivo, Map.copyOf(novos));
    }
}
