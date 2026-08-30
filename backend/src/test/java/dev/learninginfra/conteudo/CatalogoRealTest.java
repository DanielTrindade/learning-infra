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
        var iac = cenarios.stream()
                .filter(cenario -> cenario.id().startsWith("iac/"))
                .toList();
        var linux = cenarios.stream()
                .filter(cenario -> cenario.id().startsWith("linux/"))
                .toList();

        assertThat(cenarios).hasSize(70);
        assertThat(kubernetes).hasSize(14);
        assertThat(kubernetes).allMatch(Cenario::usaKubernetes);
        assertThat(kubernetes.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(40);
        assertThat(aws).hasSize(18);
        assertThat(aws).allMatch(Cenario::usaAws);
        assertThat(aws.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(57);
        assertThat(docker).hasSize(11);
        assertThat(docker).allMatch(Cenario::usaDocker);
        assertThat(docker.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(56);
        assertThat(iac).hasSize(18);
        assertThat(iac).allMatch(Cenario::terraform);
        assertThat(iac.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(98);
        assertThat(iac).filteredOn(cenario -> cenario.dificuldade() == Dificuldade.GUIADO)
                .hasSize(4);
        assertThat(iac).filteredOn(cenario -> cenario.dificuldade() == Dificuldade.ASSISTIDO)
                .hasSize(8);
        assertThat(iac).filteredOn(cenario -> cenario.dificuldade() == Dificuldade.AUTONOMO)
                .hasSize(5);
        assertThat(iac).filteredOn(cenario -> cenario.dificuldade() == Dificuldade.MESTRE)
                .hasSize(1);
        assertThat(linux).hasSize(9);
        assertThat(linux).allMatch(Cenario::usaLinux);
        assertThat(linux.stream().mapToInt(cenario -> cenario.asercoes().size()).sum())
                .isEqualTo(22);
    }

    @Test
    void carregaCincoTrilhasComFundamentosPublicados() {
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
                .containsExactly("linux", "docker", "kubernetes", "aws", "iac");
        assertThat(catalogo.buscar("linux").orElseThrow().fundamentos())
                .satisfies(fundamentos -> {
                    assertThat(fundamentos.questionario().questoes()).hasSize(12);
                    assertThat(fundamentos.markdown()).contains("## Kernel e espaço de usuário");
                });
        assertThat(catalogo.buscar("iac").orElseThrow().fundamentos()).isNotNull();
        assertThat(catalogo.buscar("iac").orElseThrow().fundamentos().questionario())
                .isNotNull();
        assertThat(catalogo.buscar("docker").orElseThrow().fundamentos())
                .satisfies(fundamentos -> {
                    assertThat(fundamentos.questionario().questoes()).hasSize(12);
                    assertThat(fundamentos.markdown()).contains("## Como o Docker funciona");
                });
        assertThat(catalogo.buscar("kubernetes").orElseThrow().fundamentos())
                .satisfies(fundamentos -> {
                    assertThat(fundamentos.questionario().questoes()).hasSize(12);
                    assertThat(fundamentos.markdown()).contains("## Estado desejado e reconciliação");
                });
        assertThat(catalogo.buscar("aws").orElseThrow().fundamentos())
                .satisfies(fundamentos -> {
                    assertThat(fundamentos.questionario().questoes()).hasSize(12);
                    assertThat(fundamentos.markdown()).contains("## Responsabilidade compartilhada");
                });
    }

    private Path localizarConteudo() {
        Path naRaiz = Path.of("content");
        return Files.isDirectory(naRaiz) ? naRaiz : Path.of("..", "content");
    }
}
