package dev.learninginfra.track;

import dev.learninginfra.content.ScenarioReader;
import dev.learninginfra.content.ScenarioRepository;
import dev.learninginfra.progress.ProgressRepository;
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

class TrackCatalogTest {

    @TempDir
    Path root;

    @Test
    void listaTrilhasExplicitasEmOrdemECompoeSeusCenarios() throws Exception {
        escreverTrilha("kubernetes", "Kubernetes", 3);
        escreverTrilha("docker", "Docker", 2);
        escreverCenarioDocker();

        var catalogo = catalogo();

        assertThat(catalogo.list()).extracting(Track::id)
                .containsExactly("docker", "kubernetes");
        assertThat(catalogo.find("docker")).isPresent().get()
                .satisfies(track -> {
                    assertThat(track.title()).isEqualTo("Docker");
                    assertThat(track.scenarios()).extracting(scenario -> scenario.id())
                            .containsExactly("docker/01-primeiro");
                });
    }

    @Test
    void recarregaQuandoUmCenarioNovoAparece() throws Exception {
        escreverTrilha("docker", "Docker", 2);
        escreverCenarioDocker();
        var catalogo = catalogo();
        assertThat(catalogo.list()).flatExtracting(Track::scenarios).hasSize(1);

        escreverCenarioExtra();

        assertThat(catalogo.list()).flatExtracting(Track::scenarios).hasSize(2);
    }

    @Test
    void carregaFundamentosEQuestionarioDeclaradosNoManifesto() throws Exception {
        escreverTrilhaComFundamentos();

        Fundamentals fundamentals = catalogo().find("docker").orElseThrow().fundamentals();

        assertThat(fundamentals.title()).isEqualTo("Fundamentos do Docker");
        assertThat(fundamentals.markdown()).contains("## Imagens e containers");
        assertThat(fundamentals.minimumScore()).isEqualTo(80);
        assertThat(fundamentals.questionario().questions()).hasSize(12);
        assertThat(fundamentals.questionario().questions().getFirst().options())
                .extracting(Questionnaire.Option::id)
                .containsExactly("a", "b");
    }

    @Test
    void corrigeRegistraOMelhorResultadoEApenasAPrimeiraAprovacao() throws Exception {
        escreverTrilhaComFundamentos();
        var progressRepository = new ProgressRepository(
                root.resolve("data/progresso.json").toString());
        progressRepository.save(progressRepository.load().withActiveScenario("docker/01-primeiro"));
        var corretor = new QuestionnaireGrader(
                catalogo(), progressRepository,
                Clock.fixed(Instant.parse("2026-08-07T12:00:00Z"), ZoneOffset.UTC));

        QuestionnaireResult passed = corretor.answer(
                "docker", respostasComAcertos(10));
        QuestionnaireResult novaTentativa = corretor.answer(
                "docker", respostasComAcertos(9));

        assertThat(passed.score()).isEqualTo(83);
        assertThat(passed.passed()).isTrue();
        assertThat(passed.bestScore()).isEqualTo(83);
        assertThat(passed.attempts()).isEqualTo(1);
        assertThat(novaTentativa.score()).isEqualTo(75);
        assertThat(novaTentativa.bestScore()).isEqualTo(83);
        assertThat(novaTentativa.attempts()).isEqualTo(2);
        assertThat(novaTentativa.feedback()).hasSize(12);
        assertThat(progressRepository.load().activeScenario()).isEqualTo("docker/01-primeiro");
        assertThat(progressRepository.load().fundamentals().get("docker").completedAt())
                .isEqualTo("2026-08-07T12:00:00Z");
    }

    @Test
    void calculaOsExtremosDeZeroEDozeAcertos() throws Exception {
        escreverTrilhaComFundamentos();

        QuestionnaireResult zerado = corretor().answer(
                "docker", respostasComAcertos(0));
        QuestionnaireResult perfeito = corretor().answer(
                "docker", respostasComAcertos(12));

        assertThat(zerado.score()).isZero();
        assertThat(zerado.passed()).isFalse();
        assertThat(perfeito.score()).isEqualTo(100);
        assertThat(perfeito.passed()).isTrue();
    }

    @Test
    void rejeitaManifestoCujoIdDifereDoDiretorio() throws Exception {
        Path directory = root.resolve("content/docker");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("trilha.yaml"), "id: outro\ntitulo: Docker\nordem: 2\n");

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> catalogo().list()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deve ser docker");
    }

    @Test
    void rejeitaManifestoSemOrdem() throws Exception {
        Path directory = root.resolve("content/docker");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("trilha.yaml"), "id: docker\ntitulo: Docker\n");

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> catalogo().list()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ordem");
    }

    @Test
    void rejeitaCenarioOrfaoQuandoATrilhaNaoTemManifesto() throws Exception {
        escreverCenarioDocker();

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> catalogo().list()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trilha.yaml ausente")
                .hasMessageContaining("docker");
    }

    @Test
    void rejeitaSubmissaoIncompletaOuComAlternativaDesconhecida() throws Exception {
        escreverTrilhaComFundamentos();

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> corretor().answer("docker", Map.of("q1", "a"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uma resposta válida para cada questão");

        Map<String, String> answers = respostasComAcertos(12);
        answers.put("q1", "desconhecida");
        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> corretor().answer("docker", answers)))
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
        Path file = root.resolve("content/docker/questionario.yaml");
        String yaml = Files.readString(file).replaceFirst(
                "(?m)^\\s+- id: b\\R\\s+texto: Incorreta\\R", "");
        Files.writeString(file, yaml);
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

    private TrackCatalog catalogo() {
        Path content = root.resolve("content");
        var scenarios = new ScenarioRepository(
                content.toString(), new ScenarioReader());
        return new TrackCatalog(
                content.toString(), scenarios, new TrackReader());
    }

    private QuestionnaireGrader corretor() {
        return new QuestionnaireGrader(
                catalogo(),
                new ProgressRepository(root.resolve("data/progresso.json").toString()),
                Clock.fixed(Instant.parse("2026-08-07T12:00:00Z"), ZoneOffset.UTC));
    }

    private void escreverTrilha(String id, String title, int ordem) throws Exception {
        Path directory = root.resolve("content").resolve(id);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("trilha.yaml"), """
                id: %s
                titulo: %s
                ordem: %d
                """.formatted(id, title, ordem));
    }

    private void escreverCenarioDocker() throws Exception {
        Path directory = root.resolve("content/docker/01-primeiro");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: docker/01-primeiro
                titulo: Primeiro
                dificuldade: guiado
                ---
                # Primeiro
                """);
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: comando_produz
                    comando: ["echo", "ok"]
                    contem: ok
                    descricao: o comando funciona
                """);
    }

    private void escreverCenarioExtra() throws Exception {
        Path directory = root.resolve("content/docker/02-segundo");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: docker/02-segundo
                titulo: Segundo
                dificuldade: guiado
                ---
                # Segundo
                """);
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: comando_produz
                    comando: ["echo", "ok"]
                    contem: ok
                    descricao: o comando funciona
                """);
    }

    private void escreverTrilhaComFundamentos() throws Exception {
        Path directory = root.resolve("content/docker");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("trilha.yaml"), """
                id: docker
                titulo: Docker
                ordem: 2
                fundamentos:
                  titulo: Fundamentos do Docker
                  artigo: fundamentos.md
                  questionario: questionario.yaml
                  aproveitamentoMinimo: 80
                """);
        Files.writeString(directory.resolve("fundamentos.md"), """
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
        Files.writeString(directory.resolve("questionario.yaml"), yaml.toString());
    }

    private Map<String, String> respostasComAcertos(int quantidade) {
        Map<String, String> answers = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            answers.put("q" + i, i <= quantidade ? "a" : "b");
        }
        return answers;
    }

    private void substituirNoManifesto(String anterior, String novo) throws Exception {
        substituir(root.resolve("content/docker/trilha.yaml"), anterior, novo);
    }

    private void substituirNoQuestionario(String anterior, String novo) throws Exception {
        substituir(root.resolve("content/docker/questionario.yaml"), anterior, novo);
    }

    private void substituir(Path file, String anterior, String novo) throws Exception {
        String content = Files.readString(file);
        assertThat(content).contains(anterior);
        Files.writeString(file, content.replaceFirst(
                java.util.regex.Pattern.quote(anterior),
                java.util.regex.Matcher.quoteReplacement(novo)));
    }

    private void assertConteudoInvalido(String trechoDaMensagem) {
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> catalogo().list()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(trechoDaMensagem);
    }
}
