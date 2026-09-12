package dev.learninginfra.content;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class ScenarioRepository {

    private final Path diretorioDeConteudo;
    private final ScenarioReader reader;
    private final ContentSignature signature;
    private ContentSignature.Snapshot snapshot;
    private List<Scenario> cache = List.of();

    public ScenarioRepository(
            @Value("${learning-infra.content-directory}") String diretorioDeConteudo,
            ScenarioReader reader) {
        this.diretorioDeConteudo = Path.of(diretorioDeConteudo);
        this.reader = reader;
        this.signature = new ContentSignature(diretorioDeConteudo);
    }

    /**
     * O catálogo é relido quando o conteúdo muda — criar um diretório continua bastando.
     * Enquanto nada muda, a lista vem do cache.
     */
    public List<Scenario> list() {
        ContentSignature.Snapshot atual = signature.compute();
        synchronized (this) {
            if (atual.equals(snapshot)) {
                return cache;
            }
            cache = readFromDisk();
            snapshot = atual;
            return cache;
        }
    }

    private List<Scenario> readFromDisk() {
        if (!Files.isDirectory(diretorioDeConteudo)) {
            return List.of();
        }
        try (Stream<Path> tracks = Files.list(diretorioDeConteudo)) {
            return tracks
                    .filter(Files::isDirectory)
                    .flatMap(this::cenariosDaTrilha)
                    .map(reader::read)
                    .sorted(Comparator.comparing(Scenario::id))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui listar " + diretorioDeConteudo, e);
        }
    }

    public Optional<Scenario> find(String id) {
        return list().stream().filter(c -> c.id().equals(id)).findFirst();
    }

    private Stream<Path> cenariosDaTrilha(Path track) {
        try (Stream<Path> filhos = Files.list(track)) {
            return filhos
                    .filter(p -> Files.isRegularFile(p.resolve("cenario.md")))
                    .toList()
                    .stream();
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui listar " + track, e);
        }
    }
}
