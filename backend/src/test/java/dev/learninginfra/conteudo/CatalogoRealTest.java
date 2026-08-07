package dev.learninginfra.conteudo;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoRealTest {

    @Test
    void carregaTodoOCatalogoMantidoNoRepositorio() {
        Path conteudo = localizarConteudo();
        var cenarios = new RepositorioDeCenarios(
                conteudo.toString(), new LeitorDeCenario()).listar();

        var kubernetes = cenarios.stream()
                .filter(cenario -> cenario.id().startsWith("kubernetes/"))
                .toList();
        var aws = cenarios.stream()
                .filter(cenario -> cenario.id().startsWith("aws/"))
                .toList();

        assertThat(cenarios).hasSize(37);
        assertThat(kubernetes).hasSize(14);
        assertThat(kubernetes).allMatch(Cenario::usaKubernetes);
        assertThat(kubernetes.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(40);
        assertThat(aws).hasSize(18);
        assertThat(aws).allMatch(Cenario::usaAws);
        assertThat(aws.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(57);
    }

    private Path localizarConteudo() {
        Path naRaiz = Path.of("content");
        return Files.isDirectory(naRaiz) ? naRaiz : Path.of("..", "content");
    }
}
