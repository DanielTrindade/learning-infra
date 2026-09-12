package dev.learninginfra.track;

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
public class TrackReader {

    public TrackMetadata read(Path file) {
        try {
            Map<String, Object> data = new Yaml().load(Files.readString(file));
            String id = requireText(data, "id", file);
            String idDoDiretorio = file.getParent().getFileName().toString();
            if (!id.equals(idDoDiretorio)) {
                throw new IllegalArgumentException(
                        "id da Trilha deve ser " + idDoDiretorio + " em " + file);
            }
            return new TrackMetadata(
                    id,
                    requireText(data, "titulo", file),
                    requireInteger(data, "ordem", file),
                    readFundamentals(data, file));
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui ler " + file, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Fundamentals readFundamentals(Map<String, Object> data, Path manifesto)
            throws IOException {
        Object value = data.get("fundamentos");
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("fundamentos deve ser um objeto em " + manifesto);
        }
        Map<String, Object> configuracao = (Map<String, Object>) value;
        int aproveitamento = requireInteger(
                configuracao, "aproveitamentoMinimo", manifesto);
        if (aproveitamento < 1 || aproveitamento > 100) {
            throw new IllegalArgumentException(
                    "aproveitamentoMinimo deve ficar entre 1 e 100 em " + manifesto);
        }

        Path directory = manifesto.getParent();
        Path artigo = directory.resolve(requireText(configuracao, "artigo", manifesto)).normalize();
        Path questionario = directory
                .resolve(requireText(configuracao, "questionario", manifesto)).normalize();
        requireInsideDirectory(directory, artigo, manifesto);
        requireInsideDirectory(directory, questionario, manifesto);
        String markdown = Files.readString(artigo);
        Set<String> secoes = markdownSections(markdown);

        return new Fundamentals(
                requireText(configuracao, "titulo", manifesto),
                markdown,
                aproveitamento,
                readQuestionnaire(questionario, secoes));
    }

    @SuppressWarnings("unchecked")
    private Questionnaire readQuestionnaire(Path file, Set<String> secoes) throws IOException {
        Map<String, Object> root = new Yaml().load(Files.readString(file));
        Object value = root == null ? null : root.get("questoes");
        if (!(value instanceof List<?> items) || items.isEmpty()) {
            throw new IllegalArgumentException("questionario sem questões: " + file);
        }

        Set<String> idsDasQuestoes = new HashSet<>();
        List<Questionnaire.Question> questions = items.stream()
                .map(item -> readQuestion((Map<String, Object>) item, file, secoes, idsDasQuestoes))
                .toList();
        return new Questionnaire(questions);
    }

    @SuppressWarnings("unchecked")
    private Questionnaire.Question readQuestion(
            Map<String, Object> data,
            Path file,
            Set<String> secoes,
            Set<String> idsDasQuestoes) {
        String id = requireText(data, "id", file);
        if (!idsDasQuestoes.add(id)) {
            throw new IllegalArgumentException("id de questão duplicado: " + id + " em " + file);
        }

        Object value = data.get("alternativas");
        if (!(value instanceof List<?> items) || items.size() < 2) {
            throw new IllegalArgumentException(
                    "questão deve ter pelo menos duas alternativas: " + id + " em " + file);
        }
        Set<String> idsDasAlternativas = new HashSet<>();
        List<Questionnaire.Option> options = items.stream().map(item -> {
            Map<String, Object> option = (Map<String, Object>) item;
            String alternativaId = requireText(option, "id", file);
            if (!idsDasAlternativas.add(alternativaId)) {
                throw new IllegalArgumentException(
                        "id de alternativa duplicado: " + alternativaId + " em " + file);
            }
            return new Questionnaire.Option(
                    alternativaId, requireText(option, "texto", file));
        }).toList();

        String correct = requireText(data, "alternativaCorreta", file);
        if (!idsDasAlternativas.contains(correct)) {
            throw new IllegalArgumentException(
                    "alternativaCorreta desconhecida: " + correct + " em " + file);
        }
        String review = requireText(data, "revisar", file);
        if (!secoes.contains(review)) {
            throw new IllegalArgumentException(
                    "seção de revisão desconhecida: " + review + " em " + file);
        }
        return new Questionnaire.Question(
                id,
                requireText(data, "enunciado", file),
                options,
                correct,
                requireText(data, "explicacao", file),
                review);
    }

    private Set<String> markdownSections(String markdown) {
        Set<String> secoes = new HashSet<>();
        markdown.lines()
                .filter(line -> line.startsWith("## "))
                .map(line -> slug(line.substring(3)))
                .forEach(secoes::add);
        return secoes;
    }

    private String slug(String text) {
        String semAcentos = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return Pattern.compile("[^a-z0-9]+")
                .matcher(semAcentos.toLowerCase())
                .replaceAll("-")
                .replaceAll("(^-|-$)", "");
    }

    private void requireInsideDirectory(Path directory, Path file, Path manifesto) {
        if (!file.startsWith(directory.normalize())) {
            throw new IllegalArgumentException("referência fora da Trilha em " + manifesto);
        }
    }

    private int requireInteger(Map<String, Object> data, String campo, Path file) {
        Object value = data.get(campo);
        if (!(value instanceof Integer inteiro)) {
            throw new IllegalArgumentException(
                    "campo inteiro obrigatório ausente: " + campo + " em " + file);
        }
        return inteiro;
    }

    private String requireText(Map<String, Object> data, String campo, Path file) {
        Object value = data == null ? null : data.get(campo);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(
                    "campo obrigatório ausente: " + campo + " em " + file);
        }
        return value.toString();
    }

    public record TrackMetadata(String id, String title, int ordem, Fundamentals fundamentals) {
    }
}
