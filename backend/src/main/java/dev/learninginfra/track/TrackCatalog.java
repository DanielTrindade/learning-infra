package dev.learninginfra.track;

import dev.learninginfra.content.ContentSignature;
import dev.learninginfra.content.Scenario;
import dev.learninginfra.content.ScenarioRepository;
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

/** Leitura pura do catálogo: manifestos, Fundamentos e os Cenários de cada Trilha. */
@Component
public class TrackCatalog {

    private static final String MANIFESTO = "trilha.yaml";

    private final Path diretorioDeConteudo;
    private final ScenarioRepository repositorioDeCenarios;
    private final TrackReader reader;
    private final ContentSignature signature;
    private ContentSignature.Snapshot snapshot;
    private List<Track> cache = List.of();

    public TrackCatalog(
            @Value("${learning-infra.content-directory}") String diretorioDeConteudo,
            ScenarioRepository repositorioDeCenarios,
            TrackReader reader) {
        this.diretorioDeConteudo = Path.of(diretorioDeConteudo);
        this.repositorioDeCenarios = repositorioDeCenarios;
        this.reader = reader;
        this.signature = new ContentSignature(diretorioDeConteudo);
    }

    /** Mesmo contrato de cache do repositório de Cenários: muda o conteúdo, recarrega. */
    public List<Track> list() {
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

    public Optional<Track> find(String id) {
        return list().stream().filter(track -> track.id().equals(id)).findFirst();
    }

    private List<Track> readFromDisk() {
        if (!Files.isDirectory(diretorioDeConteudo)) {
            return List.of();
        }
        List<Scenario> scenarios = repositorioDeCenarios.list();
        validateScenarioManifests(scenarios);
        try (Stream<Path> filhos = Files.list(diretorioDeConteudo)) {
            return filhos
                    .filter(Files::isDirectory)
                    .map(directory -> directory.resolve(MANIFESTO))
                    .filter(Files::isRegularFile)
                    .map(manifesto -> buildTrack(manifesto, scenarios))
                    .sorted(Comparator.comparingInt(Track::ordem).thenComparing(Track::id))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui listar " + diretorioDeConteudo, e);
        }
    }

    private void validateScenarioManifests(List<Scenario> scenarios) {
        for (Scenario scenario : scenarios) {
            String trackId = scenario.id().split("/", 2)[0];
            Path manifesto = diretorioDeConteudo.resolve(trackId).resolve(MANIFESTO);
            if (!Files.isRegularFile(manifesto)) {
                throw new IllegalArgumentException(
                        "trilha.yaml ausente para a Trilha " + trackId);
            }
        }
    }

    private Track buildTrack(Path manifesto, List<Scenario> todosOsCenarios) {
        var metadados = reader.read(manifesto);
        var scenarios = todosOsCenarios.stream()
                .filter(scenario -> scenario.id().startsWith(metadados.id() + "/"))
                .toList();
        return new Track(
                metadados.id(), metadados.title(), metadados.ordem(),
                metadados.fundamentals(), scenarios);
    }
}
