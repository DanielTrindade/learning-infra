package dev.learninginfra.content;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Impressão barata do estado do diretório de conteúdo: quantidade de arquivos e soma de
 * mtime + hash do nome. Muda quando um arquivo é criado, apagado ou editado — é o
 * gatilho do cache dos repositórios, não uma prova criptográfica. Caminhar pelos
 * arquivos é muito mais barato que reparsear 75 Cenários de YAML e Markdown.
 */
public class ContentSignature {

    private final Path directory;

    public ContentSignature(String directory) {
        this.directory = Path.of(directory);
    }

    public Snapshot compute() {
        if (!Files.isDirectory(directory)) {
            return new Snapshot(0, 0);
        }
        try (Stream<Path> files = Files.walk(directory)) {
            long quantidade = 0;
            long soma = 0;
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                quantidade++;
                soma += fingerprint(file);
            }
            return new Snapshot(quantidade, soma);
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui medir " + directory, e);
        }
    }

    private long fingerprint(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis()
                    + file.getFileName().toString().hashCode();
        } catch (IOException e) {
            return file.getFileName().toString().hashCode();
        }
    }

    public record Snapshot(long fileCount, long checksum) {
    }
}
