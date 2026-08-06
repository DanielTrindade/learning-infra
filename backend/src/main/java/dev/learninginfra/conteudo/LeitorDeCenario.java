package dev.learninginfra.conteudo;

import dev.learninginfra.verificacao.Assercao;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class LeitorDeCenario {

    private static final String DELIMITADOR = "---";

    public Cenario ler(Path diretorioDoCenario) {
        String bruto = lerArquivo(diretorioDoCenario.resolve("cenario.md"));
        String[] partes = separarFrontmatter(bruto, diretorioDoCenario);

        Map<String, Object> meta = new Yaml().load(partes[0]);
        String corpo = partes[1];

        List<Assercao> asercoes = lerAsercoes(diretorioDoCenario.resolve("verificacao.yaml"));

        return new Cenario(
                exigirTexto(meta, "id"),
                exigirTexto(meta, "titulo"),
                Dificuldade.deTexto(exigirTexto(meta, "dificuldade")),
                lerContainers(meta),
                corpo,
                diretorioDoCenario,
                asercoes);
    }

    private String[] separarFrontmatter(String bruto, Path diretorio) {
        String normalizado = bruto.replace("\r\n", "\n").stripLeading();
        if (!normalizado.startsWith(DELIMITADOR + "\n")) {
            throw new IllegalArgumentException("cenario.md sem frontmatter em " + diretorio);
        }
        int fim = normalizado.indexOf("\n" + DELIMITADOR, DELIMITADOR.length());
        if (fim < 0) {
            throw new IllegalArgumentException("frontmatter não fechado em " + diretorio);
        }
        String frontmatter = normalizado.substring(DELIMITADOR.length() + 1, fim);
        String corpo = normalizado.substring(fim + 1 + DELIMITADOR.length()).stripLeading();
        return new String[]{frontmatter, corpo};
    }

    @SuppressWarnings("unchecked")
    private List<Assercao> lerAsercoes(Path arquivo) {
        Map<String, Object> raiz = new Yaml().load(lerArquivo(arquivo));
        List<Map<String, Object>> itens = (List<Map<String, Object>>) raiz.get("asercoes");
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("verificacao.yaml sem asserções: " + arquivo);
        }
        List<Assercao> asercoes = new ArrayList<>();
        for (Map<String, Object> item : itens) {
            asercoes.add(montarAsercao(item, arquivo));
        }
        return List.copyOf(asercoes);
    }

    private Assercao montarAsercao(Map<String, Object> item, Path arquivo) {
        String tipo = exigirTexto(item, "tipo");
        return switch (tipo) {
            case "container_rodando" -> new Assercao.ContainerRodando(exigirTexto(item, "nome"));
            case "http_responde" -> new Assercao.HttpResponde(
                    exigirTexto(item, "url"), (Integer) item.getOrDefault("status", 200));
            case "http_corpo_contem" -> new Assercao.HttpCorpoContem(
                    exigirTexto(item, "url"), exigirTexto(item, "texto"));
            case "imagem_existe" -> new Assercao.ImagemExiste(exigirTexto(item, "referencia"));
            default -> throw new IllegalArgumentException(
                    "tipo de asserção desconhecido: " + tipo + " em " + arquivo);
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> lerContainers(Map<String, Object> meta) {
        Object valor = meta.get("containers");
        return valor == null ? List.of() : List.copyOf((List<String>) valor);
    }

    private String exigirTexto(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        if (valor == null) {
            throw new IllegalArgumentException("campo obrigatório ausente: " + chave);
        }
        return valor.toString();
    }

    private String lerArquivo(Path caminho) {
        try {
            return Files.readString(caminho);
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui ler " + caminho, e);
        }
    }
}
