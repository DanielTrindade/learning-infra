package dev.learninginfra.progresso;

import java.util.LinkedHashMap;
import java.util.Map;

public record Progresso(
        String cenarioAtivo,
        Map<String, String> concluidos,
        Map<String, ProgressoDosFundamentos> fundamentos) {

    public Progresso {
        concluidos = concluidos == null ? Map.of() : Map.copyOf(concluidos);
        fundamentos = fundamentos == null ? Map.of() : Map.copyOf(fundamentos);
    }

    public static Progresso vazio() {
        return new Progresso(null, Map.of(), Map.of());
    }

    public Progresso comAtivo(String id) {
        return new Progresso(id, concluidos, fundamentos);
    }

    public Progresso comConcluido(String id, String instante) {
        var novos = new LinkedHashMap<>(concluidos);
        novos.put(id, instante);
        return new Progresso(cenarioAtivo, novos, fundamentos);
    }

    public Progresso comFundamentos(
            String idDaTrilha, ProgressoDosFundamentos novoProgresso) {
        var novos = new LinkedHashMap<>(fundamentos);
        novos.put(idDaTrilha, novoProgresso);
        return new Progresso(cenarioAtivo, concluidos, novos);
    }
}
