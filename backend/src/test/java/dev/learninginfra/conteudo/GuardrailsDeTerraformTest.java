package dev.learninginfra.conteudo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os guardrails da plataforma valem também para o código que o Cenário entrega pronto.
 * Um `.tf` versionado que alcance uma conta AWS real ou um cluster que não seja o do
 * Docker Desktop é defeito de conteúdo, não descuido do leitor.
 */
class GuardrailsDeTerraformTest {

    @Test
    void nenhumArquivoTerraformDoConteudoEscapaDoAmbienteLocal() {
        List<Path> arquivos = arquivosTerraform();

        assertThat(arquivos)
                .describedAs("o teste precisa de pelo menos um .tf para não passar em silêncio")
                .isNotEmpty();

        for (Path arquivo : arquivos) {
            String texto = ler(arquivo);

            assertThat(texto)
                    .describedAs("credencial de AWS real em %s", arquivo)
                    .doesNotContainPattern("(AKIA|ASIA)[0-9A-Z]{16}");

            if (texto.contains("provider \"aws\"")) {
                assertThat(texto)
                        .describedAs("provider AWS sem endpoint local em %s", arquivo)
                        .contains("endpoints")
                        .contains("http://127.0.0.1:4566")
                        .contains("skip_credentials_validation")
                        .contains("skip_metadata_api_check")
                        .contains("skip_requesting_account_id");
            }

            if (texto.contains("provider \"kubernetes\"")) {
                assertThat(texto)
                        .describedAs("provider Kubernetes sem contexto fixo em %s", arquivo)
                        .contains("config_context")
                        .contains("docker-desktop");
            }
        }
    }

    private List<Path> arquivosTerraform() {
        Path raiz = localizarConteudo().resolve("iac");
        if (!Files.isDirectory(raiz)) {
            return List.of();
        }
        try (Stream<Path> caminhos = Files.walk(raiz)) {
            return caminhos
                    .filter(Files::isRegularFile)
                    .filter(caminho -> caminho.getFileName().toString().endsWith(".tf"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui varrer " + raiz, e);
        }
    }

    private String ler(Path caminho) {
        try {
            return Files.readString(caminho);
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui ler " + caminho, e);
        }
    }

    private Path localizarConteudo() {
        Path naRaiz = Path.of("content");
        return Files.isDirectory(naRaiz) ? naRaiz : Path.of("..", "content");
    }
}
