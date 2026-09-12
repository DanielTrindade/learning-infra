package dev.learninginfra.content;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioRepositoryTest {

    @TempDir
    Path root;

    @Test
    void recarregaQuandoUmCenarioNovoAparece() throws Exception {
        Path content = root.resolve("content");
        escreverCenario(content, "docker/01-primeiro", "Primeiro");
        var repositorio = new ScenarioRepository(content.toString(), new ScenarioReader());

        assertThat(repositorio.list()).hasSize(1);

        escreverCenario(content, "docker/02-segundo", "Segundo");

        assertThat(repositorio.list()).extracting(Scenario::id)
                .containsExactly("docker/01-primeiro", "docker/02-segundo");
    }

    @Test
    void recarregaQuandoUmCenarioMuda() throws Exception {
        Path content = root.resolve("content");
        escreverCenario(content, "docker/01-primeiro", "Primeiro");
        var repositorio = new ScenarioRepository(content.toString(), new ScenarioReader());

        assertThat(repositorio.list().getFirst().title()).isEqualTo("Primeiro");

        Thread.sleep(20);
        escreverCenario(content, "docker/01-primeiro", "Primeiro revisado");

        assertThat(repositorio.list().getFirst().title()).isEqualTo("Primeiro revisado");
    }

    private void escreverCenario(Path content, String id, String title) throws Exception {
        Path directory = content.resolve(id);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: %s
                titulo: %s
                dificuldade: guiado
                ---
                # %s
                """.formatted(id, title, title));
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: comando_produz
                    comando: ["echo", "ok"]
                    contem: ok
                    descricao: o comando funciona
                """);
    }
}
