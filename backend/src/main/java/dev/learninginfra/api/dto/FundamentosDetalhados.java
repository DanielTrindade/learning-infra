package dev.learninginfra.api.dto;

import dev.learninginfra.progresso.ProgressoDosFundamentos;
import dev.learninginfra.trilha.Fundamentos;
import dev.learninginfra.trilha.Questionario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record FundamentosDetalhados(
        String idDaTrilha,
        String titulo,
        String markdown,
        int aproveitamentoMinimo,
        TrilhaResumo.EstadoDosFundamentos estado,
        int melhorPercentual,
        int tentativas,
        List<Questao> questoes) {

    public static FundamentosDetalhados de(
            String idDaTrilha,
            Fundamentos fundamentos,
            ProgressoDosFundamentos progresso) {
        var questoes = new ArrayList<>(fundamentos.questionario().questoes().stream()
                .map(FundamentosDetalhados::questaoPublica)
                .toList());
        Collections.shuffle(questoes);
        return new FundamentosDetalhados(
                idDaTrilha,
                fundamentos.titulo(),
                fundamentos.markdown(),
                fundamentos.aproveitamentoMinimo(),
                estado(progresso),
                progresso.melhorPercentual(),
                progresso.tentativas(),
                List.copyOf(questoes));
    }

    private static Questao questaoPublica(Questionario.Questao questao) {
        var alternativas = new ArrayList<>(questao.alternativas().stream()
                .map(alternativa -> new Alternativa(alternativa.id(), alternativa.texto()))
                .toList());
        Collections.shuffle(alternativas);
        return new Questao(questao.id(), questao.enunciado(), List.copyOf(alternativas));
    }

    private static TrilhaResumo.EstadoDosFundamentos estado(
            ProgressoDosFundamentos progresso) {
        if (progresso.concluido()) {
            return TrilhaResumo.EstadoDosFundamentos.CONCLUIDO;
        }
        return progresso.tentativas() == 0
                ? TrilhaResumo.EstadoDosFundamentos.NAO_INICIADO
                : TrilhaResumo.EstadoDosFundamentos.EM_ANDAMENTO;
    }

    public record Questao(
            String id,
            String enunciado,
            List<Alternativa> alternativas) {
    }

    public record Alternativa(String id, String texto) {
    }
}
