package dev.learninginfra.ciclodevida;

import dev.learninginfra.conteudo.Cenario;
import dev.learninginfra.conteudo.Dificuldade;
import dev.learninginfra.conteudo.RepositorioDeCenarios;
import dev.learninginfra.execucao.ExecutorDeComando;
import dev.learninginfra.execucao.SaidaDeComando;
import dev.learninginfra.progresso.RepositorioDeProgresso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GerenciadorDeCenarioAtivoTest {

    @TempDir
    Path raiz;

    private final List<List<String>> comandosExecutados = new ArrayList<>();

    private final ExecutorDeComando executorEspiao = comando -> {
        comandosExecutados.add(comando);
        return new SaidaDeComando(0, "", "");
    };

    private Cenario cenario(String id, List<String> containers) throws Exception {
        Path diretorio = raiz.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(diretorio.resolve("workspace").resolve("site"));
        Files.writeString(diretorio.resolve("workspace").resolve("site").resolve("index.html"), "<h1>ola</h1>");
        return new Cenario(id, "titulo", Dificuldade.GUIADO, containers, "# corpo", diretorio, List.of());
    }

    private GerenciadorDeCenarioAtivo gerenciador(RepositorioDeCenarios repositorio) {
        return new GerenciadorDeCenarioAtivo(
                raiz.resolve("work").toString(),
                executorEspiao,
                new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString()),
                repositorio);
    }

    private Cenario cenarioCompose(String id, String projeto) throws Exception {
        Path diretorio = raiz.resolve("content").resolve(id.replace('/', '-'));
        Files.createDirectories(diretorio.resolve("workspace"));
        return new Cenario(id, "titulo", Dificuldade.GUIADO, List.of(), "# corpo",
                diretorio, List.of(), projeto);
    }

    @Test
    void iniciarDerrubaOProjetoComposeDoCenarioAnterior() throws Exception {
        Cenario primeiro = cenarioCompose("docker/03", "lab-03");
        Cenario segundo = cenario("docker/04", List.of());
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/03")).thenReturn(Optional.of(primeiro));

        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        gerenciador.iniciar(primeiro);
        comandosExecutados.clear();
        gerenciador.iniciar(segundo);

        assertEquals(
                List.of(List.of("docker", "compose", "-p", "lab-03", "down", "-v")),
                comandosExecutados);
    }

    @Test
    void cenarioSemProjetoComposeNaoChamaCompose() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        Cenario segundo = cenario("docker/02", List.of());
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/01")).thenReturn(Optional.of(primeiro));

        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        gerenciador.iniciar(primeiro);
        comandosExecutados.clear();
        gerenciador.iniciar(segundo);

        assertTrue(comandosExecutados.stream().noneMatch(c -> c.contains("compose")));
    }

    @Test
    void falhaDoComposeDownEhRuidosa() throws Exception {
        Cenario primeiro = cenarioCompose("docker/03", "lab-03");
        Cenario segundo = cenario("docker/04", List.of());
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/03")).thenReturn(Optional.of(primeiro));

        ExecutorDeComando executorQueFalha = comando ->
                comando.contains("compose") ? new SaidaDeComando(1, "", "daemon fora do ar")
                                            : new SaidaDeComando(0, "", "");
        var gerenciador = new GerenciadorDeCenarioAtivo(
                raiz.resolve("work").toString(),
                executorQueFalha,
                new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString()),
                repositorio);

        gerenciador.iniciar(primeiro);

        var erro = assertThrows(IllegalStateException.class, () -> gerenciador.iniciar(segundo));
        assertTrue(erro.getMessage().contains("lab-03"));
    }

    @Test
    void materializaOWorkspaceNoDiretorioDeTrabalho() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        Path trabalho = gerenciador(repositorio).iniciar(primeiro);

        assertTrue(trabalho.isAbsolute());
        assertTrue(Files.exists(trabalho.resolve("site/index.html")));
        assertEquals("<h1>ola</h1>", Files.readString(trabalho.resolve("site/index.html")));
    }

    @Test
    void iniciarDerrubaOsContainersDoCenarioAnterior() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        Cenario segundo = cenario("docker/02", List.of("lab-api"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/01")).thenReturn(Optional.of(primeiro));

        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        gerenciador.iniciar(primeiro);
        comandosExecutados.clear();
        gerenciador.iniciar(segundo);

        assertEquals(List.of(List.of("docker", "rm", "-f", "lab-web")), comandosExecutados);
    }

    @Test
    void iniciarLimpaSobrasDoWorkspaceAnterior() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        Cenario segundo = cenario("docker/02", List.of("lab-api"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/01")).thenReturn(Optional.of(primeiro));

        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);
        Path trabalho = gerenciador.iniciar(primeiro);
        Files.writeString(trabalho.resolve("lixo.txt"), "sobra");

        gerenciador.iniciar(segundo);

        assertFalse(Files.exists(trabalho.resolve("lixo.txt")));
    }

    @Test
    void oCenarioAtivoSobreviveAUmNovoGerenciador() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);

        gerenciador(repositorio).iniciar(primeiro);

        assertEquals(Optional.of("docker/01"), gerenciador(repositorio).cenarioAtivo());
    }

    @Test
    void marcarConcluidoRegistraNoProgresso() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        GerenciadorDeCenarioAtivo gerenciador = gerenciador(repositorio);

        gerenciador.iniciar(primeiro);
        gerenciador.marcarConcluido(primeiro);

        assertTrue(new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString())
                .carregar().concluidos().containsKey("docker/01"));
    }

    @Test
    void falhaDeTeardownEhRuidosa() throws Exception {
        Cenario primeiro = cenario("docker/01", List.of("lab-web"));
        Cenario segundo = cenario("docker/02", List.of("lab-api"));
        var repositorio = Mockito.mock(RepositorioDeCenarios.class);
        Mockito.when(repositorio.buscar("docker/01")).thenReturn(Optional.of(primeiro));

        ExecutorDeComando executorQueFalha = comando ->
                comando.contains("rm") ? new SaidaDeComando(1, "", "daemon fora do ar")
                                       : new SaidaDeComando(0, "", "");
        var gerenciador = new GerenciadorDeCenarioAtivo(
                raiz.resolve("work").toString(),
                executorQueFalha,
                new RepositorioDeProgresso(raiz.resolve("data/progresso.json").toString()),
                repositorio);

        gerenciador.iniciar(primeiro);

        var erro = assertThrows(IllegalStateException.class, () -> gerenciador.iniciar(segundo));
        assertTrue(erro.getMessage().contains("lab-web"));
    }
}
