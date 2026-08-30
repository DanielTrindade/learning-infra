package dev.learninginfra.trilha;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class LeitorDeTrilha {

    public MetadadosDaTrilha ler(Path arquivo) {
        try {
            Map<String, Object> dados = new Yaml().load(Files.readString(arquivo));
            String id = exigirTexto(dados, "id", arquivo);
            String idDoDiretorio = arquivo.getParent().getFileName().toString();
            if (!id.equals(idDoDiretorio)) {
                throw new IllegalArgumentException(
                        "id da Trilha deve ser " + idDoDiretorio + " em " + arquivo);
            }
            return new MetadadosDaTrilha(
                    id,
                    exigirTexto(dados, "titulo", arquivo),
                    exigirInteiro(dados, "ordem", arquivo),
                    lerFundamentos(dados, arquivo));
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui ler " + arquivo, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Fundamentos lerFundamentos(Map<String, Object> dados, Path manifesto)
            throws IOException {
        Object valor = dados.get("fundamentos");
        if (valor == null) {
            return null;
        }
        if (!(valor instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("fundamentos deve ser um objeto em " + manifesto);
        }
        Map<String, Object> configuracao = (Map<String, Object>) valor;
        int aproveitamento = exigirInteiro(
                configuracao, "aproveitamentoMinimo", manifesto);
        if (aproveitamento < 1 || aproveitamento > 100) {
            throw new IllegalArgumentException(
                    "aproveitamentoMinimo deve ficar entre 1 e 100 em " + manifesto);
        }

        Path diretorio = manifesto.getParent();
        Path artigo = diretorio.resolve(exigirTexto(configuracao, "artigo", manifesto)).normalize();
        Path questionario = diretorio
                .resolve(exigirTexto(configuracao, "questionario", manifesto)).normalize();
        exigirDentroDoDiretorio(diretorio, artigo, manifesto);
        exigirDentroDoDiretorio(diretorio, questionario, manifesto);
        String markdown = Files.readString(artigo);
        Set<String> secoes = secoesDoMarkdown(markdown);

        return new Fundamentos(
                exigirTexto(configuracao, "titulo", manifesto),
                markdown,
                aproveitamento,
                lerQuestionario(questionario, secoes));
    }

    @SuppressWarnings("unchecked")
    private Questionario lerQuestionario(Path arquivo, Set<String> secoes) throws IOException {
        Map<String, Object> raiz = new Yaml().load(Files.readString(arquivo));
        Object valor = raiz == null ? null : raiz.get("questoes");
        if (!(valor instanceof List<?> itens) || itens.isEmpty()) {
            throw new IllegalArgumentException("questionario sem questões: " + arquivo);
        }

        Set<String> idsDasQuestoes = new HashSet<>();
        List<Questionario.Questao> questoes = itens.stream()
                .map(item -> lerQuestao((Map<String, Object>) item, arquivo, secoes, idsDasQuestoes))
                .toList();
        return new Questionario(questoes);
    }

    @SuppressWarnings("unchecked")
    private Questionario.Questao lerQuestao(
            Map<String, Object> dados,
            Path arquivo,
            Set<String> secoes,
            Set<String> idsDasQuestoes) {
        String id = exigirTexto(dados, "id", arquivo);
        if (!idsDasQuestoes.add(id)) {
            throw new IllegalArgumentException("id de questão duplicado: " + id + " em " + arquivo);
        }

        Object valor = dados.get("alternativas");
        if (!(valor instanceof List<?> itens) || itens.size() < 2) {
            throw new IllegalArgumentException(
                    "questão deve ter pelo menos duas alternativas: " + id + " em " + arquivo);
        }
        Set<String> idsDasAlternativas = new HashSet<>();
        List<Questionario.Alternativa> alternativas = itens.stream().map(item -> {
            Map<String, Object> alternativa = (Map<String, Object>) item;
            String alternativaId = exigirTexto(alternativa, "id", arquivo);
            if (!idsDasAlternativas.add(alternativaId)) {
                throw new IllegalArgumentException(
                        "id de alternativa duplicado: " + alternativaId + " em " + arquivo);
            }
            return new Questionario.Alternativa(
                    alternativaId, exigirTexto(alternativa, "texto", arquivo));
        }).toList();

        String correta = exigirTexto(dados, "alternativaCorreta", arquivo);
        if (!idsDasAlternativas.contains(correta)) {
            throw new IllegalArgumentException(
                    "alternativaCorreta desconhecida: " + correta + " em " + arquivo);
        }
        String revisar = exigirTexto(dados, "revisar", arquivo);
        if (!secoes.contains(revisar)) {
            throw new IllegalArgumentException(
                    "seção de revisão desconhecida: " + revisar + " em " + arquivo);
        }
        return new Questionario.Questao(
                id,
                exigirTexto(dados, "enunciado", arquivo),
                alternativas,
                correta,
                exigirTexto(dados, "explicacao", arquivo),
                revisar);
    }

    private Set<String> secoesDoMarkdown(String markdown) {
        Set<String> secoes = new HashSet<>();
        markdown.lines()
                .filter(linha -> linha.startsWith("## "))
                .map(linha -> slug(linha.substring(3)))
                .forEach(secoes::add);
        return secoes;
    }

    private String slug(String texto) {
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return Pattern.compile("[^a-z0-9]+")
                .matcher(semAcentos.toLowerCase())
                .replaceAll("-")
                .replaceAll("(^-|-$)", "");
    }

    private void exigirDentroDoDiretorio(Path diretorio, Path arquivo, Path manifesto) {
        if (!arquivo.startsWith(diretorio.normalize())) {
            throw new IllegalArgumentException("referência fora da Trilha em " + manifesto);
        }
    }

    private int exigirInteiro(Map<String, Object> dados, String campo, Path arquivo) {
        Object valor = dados.get(campo);
        if (!(valor instanceof Integer inteiro)) {
            throw new IllegalArgumentException(
                    "campo inteiro obrigatório ausente: " + campo + " em " + arquivo);
        }
        return inteiro;
    }

    private String exigirTexto(Map<String, Object> dados, String campo, Path arquivo) {
        Object valor = dados == null ? null : dados.get(campo);
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException(
                    "campo obrigatório ausente: " + campo + " em " + arquivo);
        }
        return valor.toString();
    }

    public record MetadadosDaTrilha(String id, String titulo, int ordem, Fundamentos fundamentos) {
    }
}
