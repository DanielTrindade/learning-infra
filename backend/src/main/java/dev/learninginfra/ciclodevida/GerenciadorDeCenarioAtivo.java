package dev.learninginfra.ciclodevida;

import dev.learninginfra.conteudo.Cenario;
import dev.learninginfra.conteudo.RepositorioDeCenarios;
import dev.learninginfra.execucao.ExecutorDeComando;
import dev.learninginfra.execucao.SaidaDeComando;
import dev.learninginfra.progresso.RepositorioDeProgresso;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class GerenciadorDeCenarioAtivo {

    private final Path diretorioDeTrabalho;
    private final ExecutorDeComando executor;
    private final RepositorioDeProgresso progressos;
    private final RepositorioDeCenarios cenarios;

    public GerenciadorDeCenarioAtivo(
            @Value("${learninginfra.diretorio-de-trabalho}") String diretorioDeTrabalho,
            ExecutorDeComando executor,
            RepositorioDeProgresso progressos,
            RepositorioDeCenarios cenarios) {
        this.diretorioDeTrabalho = Path.of(diretorioDeTrabalho);
        this.executor = executor;
        this.progressos = progressos;
        this.cenarios = cenarios;
    }

    public Optional<String> cenarioAtivo() {
        return Optional.ofNullable(progressos.carregar().cenarioAtivo());
    }

    /** Derruba o Cenário anterior, materializa o workspace deste e o torna o Cenário Ativo. */
    public Path iniciar(Cenario cenario) {
        cenarioAtivo()
                .filter(id -> !id.equals(cenario.id()))
                .flatMap(cenarios::buscar)
                .ifPresent(this::derrubar);

        Path trabalho = diretorioDeTrabalho.toAbsolutePath().normalize();
        apagarRecursivamente(trabalho);
        copiarWorkspace(cenario, trabalho);

        progressos.salvar(progressos.carregar().comAtivo(cenario.id()));
        return trabalho;
    }

    public void marcarConcluido(Cenario cenario) {
        progressos.salvar(progressos.carregar()
                .comConcluido(cenario.id(), Instant.now().toString()));
    }

    private void derrubar(Cenario anterior) {
        for (String container : anterior.containers()) {
            SaidaDeComando saida = executor.executar(List.of("docker", "rm", "-f", container));
            if (!saida.sucesso() && !pareceInexistente(saida)) {
                throw new IllegalStateException(
                        "não consegui remover o container `" + container + "` do Cenário "
                        + anterior.id() + ": " + saida.stderr().strip()
                        + " — resolva isso antes de continuar, senão a próxima Verificação pode "
                        + "passar por sobra de ambiente");
            }
        }
    }

    private boolean pareceInexistente(SaidaDeComando saida) {
        String erro = saida.stderr().toLowerCase();
        return erro.contains("no such container")
                || erro.contains("no such object")
                || erro.contains("not found");
    }

    private void copiarWorkspace(Cenario cenario, Path destino) {
        Path origem = cenario.workspace();
        try {
            Files.createDirectories(destino);
            if (!Files.isDirectory(origem)) {
                return;
            }
            try (Stream<Path> caminhos = Files.walk(origem)) {
                for (Path caminho : caminhos.toList()) {
                    Path alvo = destino.resolve(origem.relativize(caminho).toString());
                    if (Files.isDirectory(caminho)) {
                        Files.createDirectories(alvo);
                    } else {
                        Files.createDirectories(alvo.getParent());
                        Files.copy(caminho, alvo);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui materializar o workspace de " + cenario.id(), e);
        }
    }

    private void apagarRecursivamente(Path raiz) {
        if (!Files.exists(raiz)) {
            return;
        }
        try {
            Files.walkFileTree(raiz, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path arquivo, BasicFileAttributes atributos) throws IOException {
                    Files.delete(arquivo);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path diretorio, IOException erro) throws IOException {
                    Files.delete(diretorio);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui limpar " + raiz, e);
        }
    }
}
