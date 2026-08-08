package dev.learninginfra.api.dto;

import dev.learninginfra.conteudo.Dificuldade;
import dev.learninginfra.progresso.Progresso;
import dev.learninginfra.progresso.ProgressoDosFundamentos;
import dev.learninginfra.trilha.Trilha;

import java.util.List;

public record TrilhaResumo(
        String id,
        String titulo,
        FundamentosResumo fundamentos,
        List<CenarioResumo> cenarios,
        int concluidos,
        int total,
        int percentual,
        boolean concluida) {

    public static TrilhaResumo de(Trilha trilha, Progresso progresso) {
        ProgressoDosFundamentos progressoTeorico = progresso.fundamentos()
                .getOrDefault(trilha.id(), ProgressoDosFundamentos.vazio());
        FundamentosResumo fundamentos = trilha.fundamentos() == null
                ? null
                : new FundamentosResumo(
                        trilha.fundamentos().titulo(),
                        estado(progressoTeorico),
                        progressoTeorico.melhorPercentual(),
                        progressoTeorico.tentativas());
        List<CenarioResumo> cenarios = trilha.cenarios().stream()
                .map(cenario -> new CenarioResumo(
                        cenario.id(),
                        cenario.titulo(),
                        cenario.dificuldade(),
                        cenario.asercoes().size(),
                        cenario.id().equals(progresso.cenarioAtivo()),
                        progresso.concluidos().containsKey(cenario.id())))
                .toList();
        int concluidos = (fundamentos != null && progressoTeorico.concluido() ? 1 : 0)
                + (int) cenarios.stream().filter(CenarioResumo::concluido).count();
        int total = cenarios.size() + (fundamentos == null ? 0 : 1);
        int percentual = total == 0 ? 0 : concluidos * 100 / total;
        return new TrilhaResumo(
                trilha.id(),
                trilha.titulo(),
                fundamentos,
                cenarios,
                concluidos,
                total,
                percentual,
                total > 0 && concluidos == total);
    }

    private static EstadoDosFundamentos estado(ProgressoDosFundamentos progresso) {
        if (progresso.concluido()) {
            return EstadoDosFundamentos.CONCLUIDO;
        }
        return progresso.tentativas() == 0
                ? EstadoDosFundamentos.NAO_INICIADO
                : EstadoDosFundamentos.EM_ANDAMENTO;
    }

    public record FundamentosResumo(
            String titulo,
            EstadoDosFundamentos estado,
            int melhorPercentual,
            int tentativas) {
    }

    public enum EstadoDosFundamentos {
        NAO_INICIADO,
        EM_ANDAMENTO,
        CONCLUIDO
    }

    public record CenarioResumo(
            String id,
            String titulo,
            Dificuldade dificuldade,
            int quantidadeDeAsercoes,
            boolean ativo,
            boolean concluido) {
    }
}
