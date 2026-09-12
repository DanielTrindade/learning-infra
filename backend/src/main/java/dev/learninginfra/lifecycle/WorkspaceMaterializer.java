package dev.learninginfra.lifecycle;

import dev.learninginfra.content.Scenario;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

/** Apaga e recopia o diretório de trabalho a cada Iniciar. */
class WorkspaceMaterializer {

    /** Copia o `workspace/` do Cenário para o diretório de trabalho, criando-o antes. */
    void copy(Scenario scenario, Path destination) {
        Path source = scenario.workspace();
        try {
            Files.createDirectories(destination);
            if (!Files.isDirectory(source)) {
                return;
            }
            try (Stream<Path> paths = Files.walk(source)) {
                for (Path path : paths.toList()) {
                    Path alvo = destination.resolve(source.relativize(path).toString());
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(alvo);
                    } else {
                        Files.createDirectories(alvo.getParent());
                        Files.copy(path, alvo);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui materializar o workspace de " + scenario.id(), e);
        }
    }

    void delete(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes atributos) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException error) throws IOException {
                    Files.delete(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui limpar " + root, e);
        }
    }
}
