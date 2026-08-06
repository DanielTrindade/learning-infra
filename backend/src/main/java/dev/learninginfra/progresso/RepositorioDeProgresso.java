package dev.learninginfra.progresso;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class RepositorioDeProgresso {

    private final Path arquivo;
    private final ObjectMapper json = JsonMapper.builder().build();

    public RepositorioDeProgresso(@Value("${learninginfra.arquivo-de-progresso}") String arquivo) {
        this.arquivo = Path.of(arquivo);
    }

    public Progresso carregar() {
        if (!Files.isRegularFile(arquivo)) {
            return Progresso.vazio();
        }
        return json.readValue(arquivo.toFile(), Progresso.class);
    }

    public void salvar(Progresso progresso) {
        try {
            Files.createDirectories(arquivo.toAbsolutePath().getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui criar o diretório de " + arquivo, e);
        }
        json.writerWithDefaultPrettyPrinter().writeValue(arquivo.toFile(), progresso);
    }
}
