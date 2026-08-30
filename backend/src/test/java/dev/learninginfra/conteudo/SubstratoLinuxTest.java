package dev.learninginfra.conteudo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * O substrato da Trilha Linux é um só. Todo Cenário carrega o mesmo Dockerfile para que
 * o cache de camadas sobreviva à troca de Cenário ativo — um Dockerfile divergente
 * devolveria o custo do build inteiro a cada Iniciar.
 */
class SubstratoLinuxTest {

    @Test
    void todoCenarioLinuxCarregaOMesmoDockerfile() {
        List<Path> dockerfiles = dockerfilesDaTrilhaLinux();

        assertThat(dockerfiles)
                .as("todo Cenário da Trilha Linux precisa de um workspace/Dockerfile")
                .isNotEmpty();

        Path referencia = dockerfiles.get(0);
        String esperado = conteudoNormalizado(referencia);
        for (Path dockerfile : dockerfiles) {
            assertThat(conteudoNormalizado(dockerfile))
                    .as("%s diverge de %s", dockerfile, referencia)
                    .isEqualTo(esperado);
        }
    }

    @Test
    void oDockerfileFixaAImagemPorDigestEOLocale() {
        String conteudo = conteudoNormalizado(dockerfilesDaTrilhaLinux().get(0));

        assertThat(conteudo).contains(
                "ubuntu:26.04@sha256:"
                + "889d056d5c6c0bfb55789ff3710681d68e50713cb562d2196dc07110599c7a6f");
        assertThat(conteudo).contains("ENV LANG=C.UTF-8");
        assertThat(conteudo).contains("systemctl mask tmp.mount");
    }

    private List<Path> dockerfilesDaTrilhaLinux() {
        Path trilha = localizarConteudo().resolve("linux");
        try (Stream<Path> diretorios = Files.list(trilha)) {
            return diretorios
                    .filter(Files::isDirectory)
                    .map(diretorio -> diretorio.resolve("workspace").resolve("Dockerfile"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }

    private String conteudoNormalizado(Path arquivo) {
        try {
            return Files.readString(arquivo, StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }

    private Path localizarConteudo() {
        Path naRaiz = Path.of("content");
        return Files.isDirectory(naRaiz) ? naRaiz : Path.of("..", "content");
    }
}
