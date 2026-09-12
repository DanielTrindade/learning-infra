package dev.learninginfra.content;

import dev.learninginfra.track.TrackCatalog;
import dev.learninginfra.track.TrackReader;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoRealTest {

    @Test
    void carregaTodoOCatalogoMantidoNoRepositorio() {
        Path content = localizarConteudo();
        var scenarios = new ScenarioRepository(
                content.toString(), new ScenarioReader()).list();

        var kubernetes = scenarios.stream()
                .filter(scenario -> scenario.id().startsWith("kubernetes/"))
                .toList();
        var aws = scenarios.stream()
                .filter(scenario -> scenario.id().startsWith("aws/"))
                .toList();
        var docker = scenarios.stream()
                .filter(scenario -> scenario.id().startsWith("docker/"))
                .toList();
        var iac = scenarios.stream()
                .filter(scenario -> scenario.id().startsWith("iac/"))
                .toList();
        var linux = scenarios.stream()
                .filter(scenario -> scenario.id().startsWith("linux/"))
                .toList();

        assertThat(scenarios).hasSize(75);
        assertThat(kubernetes).hasSize(14);
        assertThat(kubernetes).allMatch(Scenario::usesKubernetes);
        assertThat(kubernetes.stream().mapToInt(scenario -> scenario.assertions().size()).sum())
                .isEqualTo(40);
        assertThat(aws).hasSize(18);
        assertThat(aws).allMatch(Scenario::usesAws);
        assertThat(aws.stream().mapToInt(scenario -> scenario.assertions().size()).sum())
                .isEqualTo(57);
        assertThat(docker).hasSize(11);
        assertThat(docker).allMatch(Scenario::usesDocker);
        assertThat(docker.stream().mapToInt(scenario -> scenario.assertions().size()).sum())
                .isEqualTo(56);
        assertThat(iac).hasSize(18);
        assertThat(iac).allMatch(Scenario::usesTerraform);
        assertThat(iac.stream().mapToInt(scenario -> scenario.assertions().size()).sum())
                .isEqualTo(98);
        assertThat(iac).filteredOn(scenario -> scenario.difficulty() == Difficulty.GUIDED)
                .hasSize(4);
        assertThat(iac).filteredOn(scenario -> scenario.difficulty() == Difficulty.ASSISTED)
                .hasSize(8);
        assertThat(iac).filteredOn(scenario -> scenario.difficulty() == Difficulty.AUTONOMOUS)
                .hasSize(5);
        assertThat(iac).filteredOn(scenario -> scenario.difficulty() == Difficulty.MASTER)
                .hasSize(1);
        assertThat(linux).hasSize(14);
        assertThat(linux).allMatch(Scenario::usesLinux);
        assertThat(linux.stream().mapToInt(scenario -> scenario.assertions().size()).sum())
                .isEqualTo(36);
        assertThat(linux).filteredOn(scenario -> scenario.difficulty() == Difficulty.GUIDED)
                .hasSize(4);
        assertThat(linux).filteredOn(scenario -> scenario.difficulty() == Difficulty.ASSISTED)
                .hasSize(7);
        assertThat(linux).filteredOn(scenario -> scenario.difficulty() == Difficulty.AUTONOMOUS)
                .hasSize(2);
        assertThat(linux).filteredOn(scenario -> scenario.difficulty() == Difficulty.MASTER)
                .hasSize(1);
    }

    @Test
    void carregaCincoTrilhasComFundamentosPublicados() {
        Path content = localizarConteudo();
        var scenarios = new ScenarioRepository(
                content.toString(), new ScenarioReader());
        var catalogo = new TrackCatalog(
                content.toString(),
                scenarios,
                new TrackReader());

        var tracks = catalogo.list();

        assertThat(tracks).extracting(track -> track.id())
                .containsExactly("linux", "docker", "kubernetes", "aws", "iac");
        assertThat(catalogo.find("linux").orElseThrow().fundamentals())
                .satisfies(fundamentals -> {
                    assertThat(fundamentals.questionario().questions()).hasSize(12);
                    assertThat(fundamentals.markdown()).contains("## Kernel e espaço de usuário");
                });
        assertThat(catalogo.find("iac").orElseThrow().fundamentals()).isNotNull();
        assertThat(catalogo.find("iac").orElseThrow().fundamentals().questionario())
                .isNotNull();
        assertThat(catalogo.find("docker").orElseThrow().fundamentals())
                .satisfies(fundamentals -> {
                    assertThat(fundamentals.questionario().questions()).hasSize(12);
                    assertThat(fundamentals.markdown()).contains("## Como o Docker funciona");
                });
        assertThat(catalogo.find("kubernetes").orElseThrow().fundamentals())
                .satisfies(fundamentals -> {
                    assertThat(fundamentals.questionario().questions()).hasSize(12);
                    assertThat(fundamentals.markdown()).contains("## Estado desejado e reconciliação");
                });
        assertThat(catalogo.find("aws").orElseThrow().fundamentals())
                .satisfies(fundamentals -> {
                    assertThat(fundamentals.questionario().questions()).hasSize(12);
                    assertThat(fundamentals.markdown()).contains("## Responsabilidade compartilhada");
                });
    }

    private Path localizarConteudo() {
        Path naRaiz = Path.of("content");
        return Files.isDirectory(naRaiz) ? naRaiz : Path.of("..", "content");
    }
}
