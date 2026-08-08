package dev.learninginfra.conteudo;

import dev.learninginfra.progresso.RepositorioDeProgresso;
import dev.learninginfra.trilha.CatalogoDeTrilhas;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

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
        var docker = cenarios.stream()
                .filter(cenario -> cenario.id().startsWith("docker/"))
                .toList();

        assertThat(cenarios).hasSize(40);
        assertThat(kubernetes).hasSize(14);
        assertThat(kubernetes).allMatch(Cenario::usaKubernetes);
        assertThat(kubernetes.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(40);
        assertThat(aws).hasSize(18);
        assertThat(aws).allMatch(Cenario::usaAws);
        assertThat(aws.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(57);
        assertThat(docker).hasSize(8);
        assertThat(docker).allMatch(Cenario::usaDocker);
        assertThat(docker.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(38);
    }

    @Test
    void carregaTresTrilhasComFundamentosPublicadosSomenteEmDocker() {
        Path conteudo = localizarConteudo();
        var cenarios = new RepositorioDeCenarios(
                conteudo.toString(), new LeitorDeCenario());
        var catalogo = new CatalogoDeTrilhas(
                conteudo.toString(),
                cenarios,
                new RepositorioDeProgresso("target/progresso-catalogo-real.json"),
                Clock.systemUTC());

        var trilhas = catalogo.listar();

        assertThat(trilhas).extracting(trilha -> trilha.id())
                .containsExactly("aws", "docker", "kubernetes");
        assertThat(catalogo.buscar("docker").orElseThrow().fundamentos())
                .satisfies(fundamentos -> {
                    assertThat(fundamentos.questionario().questoes()).hasSize(12);
                    assertThat(fundamentos.markdown()).contains("## Como o Docker funciona");
                });
        assertThat(catalogo.buscar("aws").orElseThrow().fundamentos()).isNull();
        assertThat(catalogo.buscar("kubernetes").orElseThrow().fundamentos()).isNull();
    }

    private Path localizarConteudo() {
        Path naRaiz = Path.of("content");
        return Files.isDirectory(naRaiz) ? naRaiz : Path.of("..", "content");
    }
}
