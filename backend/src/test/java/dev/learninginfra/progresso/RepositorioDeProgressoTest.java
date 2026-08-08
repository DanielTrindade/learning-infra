package dev.learninginfra.progresso;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RepositorioDeProgressoTest {

    @TempDir
    Path raiz;

    @Test
    void carregaESalvaOFormatoAntigoSemPerderDados() throws Exception {
        Path arquivo = raiz.resolve("data/progresso.json");
        Files.createDirectories(arquivo.getParent());
        Files.writeString(arquivo, """
                {
                  "cenarioAtivo": "docker/01-primeiro",
                  "concluidos": {
                    "docker/00-anterior": "2026-08-06T10:00:00Z"
                  }
                }
                """);
        var repositorio = new RepositorioDeProgresso(arquivo.toString());

        Progresso progresso = repositorio.carregar();
        repositorio.salvar(progresso);
        Progresso recarregado = repositorio.carregar();

        assertThat(recarregado.cenarioAtivo()).isEqualTo("docker/01-primeiro");
        assertThat(recarregado.concluidos())
                .containsEntry("docker/00-anterior", "2026-08-06T10:00:00Z");
        assertThat(recarregado.fundamentos()).isEmpty();
    }
}
