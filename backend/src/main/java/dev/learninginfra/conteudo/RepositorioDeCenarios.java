package dev.learninginfra.conteudo;

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
public class RepositorioDeCenarios {

    private final Path diretorioDeConteudo;
    private final LeitorDeCenario leitor;

    public RepositorioDeCenarios(
            @Value("${learninginfra.diretorio-de-conteudo}") String diretorioDeConteudo,
            LeitorDeCenario leitor) {
        this.diretorioDeConteudo = Path.of(diretorioDeConteudo);
        this.leitor = leitor;
    }

    public List<Cenario> listar() {
        if (!Files.isDirectory(diretorioDeConteudo)) {
            return List.of();
        }
        try (Stream<Path> trilhas = Files.list(diretorioDeConteudo)) {
            return trilhas
                    .filter(Files::isDirectory)
                    .flatMap(this::cenariosDaTrilha)
                    .map(leitor::ler)
                    .sorted(Comparator.comparing(Cenario::id))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui listar " + diretorioDeConteudo, e);
        }
    }

    public Optional<Cenario> buscar(String id) {
        return listar().stream().filter(c -> c.id().equals(id)).findFirst();
    }

    private Stream<Path> cenariosDaTrilha(Path trilha) {
        try (Stream<Path> filhos = Files.list(trilha)) {
            return filhos
                    .filter(p -> Files.isRegularFile(p.resolve("cenario.md")))
                    .toList()
                    .stream();
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui listar " + trilha, e);
        }
    }
}
