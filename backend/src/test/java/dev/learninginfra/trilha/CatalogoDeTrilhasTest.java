package dev.learninginfra.trilha;

import dev.learninginfra.conteudo.LeitorDeCenario;
import dev.learninginfra.conteudo.RepositorioDeCenarios;
import dev.learninginfra.progresso.RepositorioDeProgresso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoDeTrilhasTest {

    @TempDir
    Path raiz;

    @Test
    void listaTrilhasExplicitasEmOrdemECompoeSeusCenarios() throws Exception {
        escreverTrilha("kubernetes", "Kubernetes", 3);
        escreverTrilha("docker", "Docker", 2);
        escreverCenarioDocker();

        var catalogo = catalogo();

        assertThat(catalogo.listar()).extracting(Trilha::id)
                .containsExactly("docker", "kubernetes");
        assertThat(catalogo.buscar("docker")).isPresent().get()
                .satisfies(trilha -> {
                    assertThat(trilha.titulo()).isEqualTo("Docker");
                    assertThat(trilha.cenarios()).extracting(cenario -> cenario.id())
                            .containsExactly("docker/01-primeiro");
                });
    }

    @Test
    void carregaFundamentosEQuestionarioDeclaradosNoManifesto() throws Exception {
        escreverTrilhaComFundamentos();

        Fundamentos fundamentos = catalogo().buscar("docker").orElseThrow().fundamentos();

        assertThat(fundamentos.titulo()).isEqualTo("Fundamentos do Docker");
        assertThat(fundamentos.markdown()).contains("## Imagens e containers");
        assertThat(fundamentos.aproveitamentoMinimo()).isEqualTo(80);
        assertThat(fundamentos.questionario().questoes()).hasSize(12);
        assertThat(fundamentos.questionario().questoes().getFirst().alternativas())
                .extracting(Questionario.Alternativa::id)
                .containsExactly("a", "b");
    }

    @Test
    void corrigeRegistraOMelhorResultadoEApenasAPrimeiraAprovacao() throws Exception {
        escreverTrilhaComFundamentos();
        var progressos = new RepositorioDeProgresso(
                raiz.resolve("data/progresso.json").toString());
        progressos.salvar(progressos.carregar().comAtivo("docker/01-primeiro"));
        var catalogo = new CatalogoDeTrilhas(
                raiz.resolve("content").toString(),
                new RepositorioDeCenarios(
                        raiz.resolve("content").toString(), new LeitorDeCenario()),
                progressos,
                Clock.fixed(Instant.parse("2026-08-07T12:00:00Z"), ZoneOffset.UTC));

        ResultadoDoQuestionario aprovado = catalogo.responder(
                "docker", respostasComAcertos(10));
        ResultadoDoQuestionario novaTentativa = catalogo.responder(
                "docker", respostasComAcertos(9));

        assertThat(aprovado.percentual()).isEqualTo(83);
        assertThat(aprovado.aprovado()).isTrue();
        assertThat(aprovado.melhorPercentual()).isEqualTo(83);
        assertThat(aprovado.tentativas()).isEqualTo(1);
        assertThat(novaTentativa.percentual()).isEqualTo(75);
        assertThat(novaTentativa.melhorPercentual()).isEqualTo(83);
        assertThat(novaTentativa.tentativas()).isEqualTo(2);
        assertThat(novaTentativa.feedback()).hasSize(12);
        assertThat(progressos.carregar().cenarioAtivo()).isEqualTo("docker/01-primeiro");
        assertThat(progressos.carregar().fundamentos().get("docker").concluidoEm())
                .isEqualTo("2026-08-07T12:00:00Z");
    }

    @Test
    void calculaOsExtremosDeZeroEDozeAcertos() throws Exception {
        escreverTrilhaComFundamentos();

        ResultadoDoQuestionario zerado = catalogo().responder(
                "docker", respostasComAcertos(0));
        ResultadoDoQuestionario perfeito = catalogo().responder(
                "docker", respostasComAcertos(12));

        assertThat(zerado.percentual()).isZero();
        assertThat(zerado.aprovado()).isFalse();
        assertThat(perfeito.percentual()).isEqualTo(100);
        assertThat(perfeito.aprovado()).isTrue();
    }

    @Test
    void rejeitaManifestoCujoIdDifereDoDiretorio() throws Exception {
        Path diretorio = raiz.resolve("content/docker");
        Files.createDirectories(diretorio);
        Files.writeString(diretorio.resolve("trilha.yaml"), "id: outro\ntitulo: Docker\nordem: 2\n");

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> catalogo().listar()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deve ser docker");
    }

    @Test
    void rejeitaManifestoSemOrdem() throws Exception {
        Path diretorio = raiz.resolve("content/docker");
        Files.createDirectories(diretorio);
        Files.writeString(diretorio.resolve("trilha.yaml"), "id: docker\ntitulo: Docker\n");

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> catalogo().listar()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ordem");
    }

    @Test
    void rejeitaCenarioOrfaoQuandoATrilhaNaoTemManifesto() throws Exception {
        escreverCenarioDocker();

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> catalogo().listar()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trilha.yaml ausente")
                .hasMessageContaining("docker");
    }

    @Test
    void rejeitaSubmissaoIncompletaOuComAlternativaDesconhecida() throws Exception {
        escreverTrilhaComFundamentos();

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> catalogo().responder("docker", Map.of("q1", "a"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uma resposta válida para cada questão");

        Map<String, String> respostas = respostasComAcertos(12);
        respostas.put("q1", "desconhecida");
        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> catalogo().responder("docker", respostas)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alternativa desconhecida");
    }

    @Test
    void rejeitaAproveitamentoForaDoIntervaloValido() throws Exception {
        escreverTrilhaComFundamentos();
        substituirNoManifesto("aproveitamentoMinimo: 80", "aproveitamentoMinimo: 0");
        assertConteudoInvalido("entre 1 e 100");
    }

    @Test
    void rejeitaIdsDuplicadosDeQuestaoEAlternativa() throws Exception {
        escreverTrilhaComFundamentos();
        substituirNoQuestionario("- id: q2", "- id: q1");
        assertConteudoInvalido("id de questão duplicado");

        escreverTrilhaComFundamentos();
        substituirNoQuestionario("- id: b", "- id: a");
        assertConteudoInvalido("id de alternativa duplicado");
    }

    @Test
    void rejeitaQuestaoComMenosDeDuasAlternativas() throws Exception {
        escreverTrilhaComFundamentos();
        Path arquivo = raiz.resolve("content/docker/questionario.yaml");
        String yaml = Files.readString(arquivo).replaceFirst(
                "(?m)^\\s+- id: b\\R\\s+texto: Incorreta\\R", "");
        Files.writeString(arquivo, yaml);
        assertConteudoInvalido("pelo menos duas alternativas");
    }

    @Test
    void rejeitaGabaritoOuSecaoDeRevisaoDesconhecidos() throws Exception {
        escreverTrilhaComFundamentos();
        substituirNoQuestionario("alternativaCorreta: a", "alternativaCorreta: ausente");
        assertConteudoInvalido("alternativaCorreta desconhecida");

        escreverTrilhaComFundamentos();
        substituirNoQuestionario(
                "revisar: imagens-e-containers", "revisar: secao-ausente");
        assertConteudoInvalido("seção de revisão desconhecida");
    }

    private CatalogoDeTrilhas catalogo() {
        Path conteudo = raiz.resolve("content");
        var cenarios = new RepositorioDeCenarios(
                conteudo.toString(), new LeitorDeCenario());
        var progressos = new RepositorioDeProgresso(
                raiz.resolve("data/progresso.json").toString());
        return new CatalogoDeTrilhas(
                conteudo.toString(), cenarios, progressos,
                Clock.fixed(Instant.parse("2026-08-07T12:00:00Z"), ZoneOffset.UTC));
    }

    private void escreverTrilha(String id, String titulo, int ordem) throws Exception {
        Path diretorio = raiz.resolve("content").resolve(id);
        Files.createDirectories(diretorio);
        Files.writeString(diretorio.resolve("trilha.yaml"), """
                id: %s
                titulo: %s
                ordem: %d
                """.formatted(id, titulo, ordem));
    }

    private void escreverCenarioDocker() throws Exception {
        Path diretorio = raiz.resolve("content/docker/01-primeiro");
        Files.createDirectories(diretorio);
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: docker/01-primeiro
                titulo: Primeiro
                dificuldade: guiado
                ---
                # Primeiro
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: comando_produz
                    comando: ["echo", "ok"]
                    contem: ok
                    descricao: o comando funciona
                """);
    }

    private void escreverTrilhaComFundamentos() throws Exception {
        Path diretorio = raiz.resolve("content/docker");
        Files.createDirectories(diretorio);
        Files.writeString(diretorio.resolve("trilha.yaml"), """
                id: docker
                titulo: Docker
                ordem: 2
                fundamentos:
                  titulo: Fundamentos do Docker
                  artigo: fundamentos.md
                  questionario: questionario.yaml
                  aproveitamentoMinimo: 80
                """);
        Files.writeString(diretorio.resolve("fundamentos.md"), """
                # Fundamentos do Docker

                ## Imagens e containers

                Uma imagem é o modelo de um container.
                """);

        StringBuilder yaml = new StringBuilder("questoes:\n");
        for (int i = 1; i <= 12; i++) {
            yaml.append("""
                      - id: q%d
                        enunciado: Questão %d?
                        alternativas:
                          - id: a
                            texto: Correta
                          - id: b
                            texto: Incorreta
                        alternativaCorreta: a
                        explicacao: A alternativa A explica o conceito.
                        revisar: imagens-e-containers
                    """.formatted(i, i));
        }
        Files.writeString(diretorio.resolve("questionario.yaml"), yaml.toString());
    }

    private Map<String, String> respostasComAcertos(int quantidade) {
        Map<String, String> respostas = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            respostas.put("q" + i, i <= quantidade ? "a" : "b");
        }
        return respostas;
    }

    private void substituirNoManifesto(String anterior, String novo) throws Exception {
        substituir(raiz.resolve("content/docker/trilha.yaml"), anterior, novo);
    }

    private void substituirNoQuestionario(String anterior, String novo) throws Exception {
        substituir(raiz.resolve("content/docker/questionario.yaml"), anterior, novo);
    }

    private void substituir(Path arquivo, String anterior, String novo) throws Exception {
        String conteudo = Files.readString(arquivo);
        assertThat(conteudo).contains(anterior);
        Files.writeString(arquivo, conteudo.replaceFirst(
                java.util.regex.Pattern.quote(anterior),
                java.util.regex.Matcher.quoteReplacement(novo)));
    }

    private void assertConteudoInvalido(String trechoDaMensagem) {
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> catalogo().listar()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(trechoDaMensagem);
    }
}
