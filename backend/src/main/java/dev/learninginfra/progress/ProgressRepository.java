package dev.learninginfra.progress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class ProgressRepository {

    private static final Logger log = LoggerFactory.getLogger(ProgressRepository.class);

    private final Path file;
    private final ObjectMapper json = JsonMapper.builder().build();

    public ProgressRepository(@Value("${learning-infra.progress-file}") String file) {
        this.file = Path.of(file);
    }

    /**
     * Um arquivo corrompido não pode derrubar o app inteiro: ele é posto de quarentena
     * e o progresso recomeça vazio, com aviso no log. Antes de ler, o arquivo legado
     * (`progresso.json`) é copiado para o nome novo, preservando o histórico.
     */
    public synchronized Progress load() {
        migrarArquivoLegado();
        if (!Files.isRegularFile(file)) {
            return Progress.empty();
        }
        try {
            return json.readValue(file.toFile(), Progress.class);
        } catch (RuntimeException error) {
            return quarantine(error);
        }
    }

    private void migrarArquivoLegado() {
        if (Files.isRegularFile(file)) {
            return;
        }
        Path legado = file.resolveSibling("progresso.json");
        if (!Files.isRegularFile(legado)) {
            return;
        }
        try {
            Files.copy(legado, file);
            log.info("progresso legado copiado de {} para {}", legado, file);
        } catch (IOException e) {
            log.warn("não consegui migrar o progresso legado {}: {}", legado, e.getMessage());
        }
    }

    /**
     * Escrita atômica: o conteúdo vai para um temporário e só então substitui o
     * arquivo real. Uma queda no meio da gravação não trunca o progresso existente.
     * O lock serializa leitura-modificação-escrita entre requisições concorrentes.
     */
    public synchronized void save(Progress progress) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            json.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), progress);
            try {
                Files.move(temporary, file,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui salvar " + file, e);
        }
    }

    /**
     * Leitura, mudança e escrita sob o mesmo lock. É o que evita perda de atualização
     * quando duas requisições concorrentes mutam mapas diferentes do progresso.
     */
    public synchronized Progress update(java.util.function.UnaryOperator<Progress> mudanca) {
        Progress atualizado = mudanca.apply(load());
        save(atualizado);
        return atualizado;
    }

    private Progress quarantine(Exception error) {
        Path quarentena = file.resolveSibling(quarantineName());
        try {
            Files.move(file, quarentena, StandardCopyOption.REPLACE_EXISTING);
            log.warn("progresso corrompido em {} — movido para {}: {}",
                    file, quarentena, error.getMessage());
        } catch (IOException failure) {
            log.warn("progresso corrompido em {} e não consegui movê-lo para quarentena: {}",
                    file, failure.getMessage());
        }
        return Progress.empty();
    }

    private String quarantineName() {
        String name = file.getFileName().toString();
        int ponto = name.lastIndexOf('.');
        String base = ponto > 0 ? name.substring(0, ponto) : name;
        return base + ".corrompido-" + System.currentTimeMillis() + ".json";
    }
}
