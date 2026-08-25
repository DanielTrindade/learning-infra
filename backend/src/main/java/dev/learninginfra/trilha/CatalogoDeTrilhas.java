package dev.learninginfra.trilha;

import dev.learninginfra.conteudo.RepositorioDeCenarios;
import dev.learninginfra.conteudo.Cenario;
import dev.learninginfra.progresso.RepositorioDeProgresso;
import dev.learninginfra.progresso.ProgressoDosFundamentos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class CatalogoDeTrilhas {

    private static final String MANIFESTO = "trilha.yaml";

    private final Path diretorioDeConteudo;
    private final RepositorioDeCenarios repositorioDeCenarios;
    private final RepositorioDeProgresso repositorioDeProgresso;
    private final Clock clock;
    private final LeitorDeTrilha leitor;

    @Autowired
    public CatalogoDeTrilhas(
            @Value("${learninginfra.diretorio-de-conteudo}") String diretorioDeConteudo,
            RepositorioDeCenarios repositorioDeCenarios,
            RepositorioDeProgresso repositorioDeProgresso,
            Clock clock) {
        this(diretorioDeConteudo, repositorioDeCenarios, repositorioDeProgresso, clock,
                new LeitorDeTrilha());
    }

    CatalogoDeTrilhas(
            String diretorioDeConteudo,
            RepositorioDeCenarios repositorioDeCenarios,
            RepositorioDeProgresso repositorioDeProgresso,
            Clock clock,
            LeitorDeTrilha leitor) {
        this.diretorioDeConteudo = Path.of(diretorioDeConteudo);
        this.repositorioDeCenarios = repositorioDeCenarios;
        this.repositorioDeProgresso = repositorioDeProgresso;
        this.clock = clock;
        this.leitor = leitor;
    }

    public List<Trilha> listar() {
        if (!Files.isDirectory(diretorioDeConteudo)) {
            return List.of();
        }
        List<Cenario> cenarios = repositorioDeCenarios.listar();
        validarManifestosDosCenarios(cenarios);
        try (Stream<Path> filhos = Files.list(diretorioDeConteudo)) {
            return filhos
                    .filter(Files::isDirectory)
                    .map(diretorio -> diretorio.resolve(MANIFESTO))
                    .filter(Files::isRegularFile)
                    .map(manifesto -> montarTrilha(manifesto, cenarios))
                    .sorted(Comparator.comparingInt(Trilha::ordem).thenComparing(Trilha::id))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui listar " + diretorioDeConteudo, e);
        }
    }

    public Optional<Trilha> buscar(String id) {
        return listar().stream().filter(trilha -> trilha.id().equals(id)).findFirst();
    }

    public ResultadoDoQuestionario responder(
            String idDaTrilha, Map<String, String> respostas) {
        Trilha trilha = buscar(idDaTrilha).orElseThrow(
                () -> new IllegalArgumentException("Trilha não encontrada: " + idDaTrilha));
        if (trilha.fundamentos() == null) {
            throw new IllegalArgumentException(
                    "Fundamentos não publicados para a Trilha: " + idDaTrilha);
        }

        Questionario questionario = trilha.fundamentos().questionario();
        var idsEsperados = questionario.questoes().stream()
                .map(Questionario.Questao::id)
                .collect(java.util.stream.Collectors.toSet());
        if (respostas == null || respostas.size() != idsEsperados.size()
                || !respostas.keySet().equals(idsEsperados)) {
            throw new IllegalArgumentException(
                    "a submissão deve conter uma resposta válida para cada questão");
        }

        int acertos = 0;
        var feedback = new java.util.ArrayList<ResultadoDoQuestionario.Feedback>();
        for (Questionario.Questao questao : questionario.questoes()) {
            String resposta = respostas.get(questao.id());
            boolean alternativaExiste = questao.alternativas().stream()
                    .anyMatch(alternativa -> alternativa.id().equals(resposta));
            if (!alternativaExiste) {
                throw new IllegalArgumentException(
                        "alternativa desconhecida para a questão " + questao.id() + ": " + resposta);
            }
            boolean acertou = questao.alternativaCorreta().equals(resposta);
            if (acertou) {
                acertos++;
            }
            feedback.add(new ResultadoDoQuestionario.Feedback(
                    questao.id(),
                    acertou,
                    questao.alternativaCorreta(),
                    questao.explicacao(),
                    questao.revisar()));
        }

        int percentual = acertos * 100 / questionario.questoes().size();
        boolean aprovado = percentual >= trilha.fundamentos().aproveitamentoMinimo();
        var progresso = repositorioDeProgresso.carregar();
        ProgressoDosFundamentos anterior = progresso.fundamentos()
                .getOrDefault(idDaTrilha, ProgressoDosFundamentos.vazio());
        ProgressoDosFundamentos atualizado = anterior.registrar(
                percentual, aprovado, clock.instant().toString());
        repositorioDeProgresso.salvar(progresso.comFundamentos(idDaTrilha, atualizado));

        return new ResultadoDoQuestionario(
                percentual,
                aprovado,
                atualizado.melhorPercentual(),
                atualizado.tentativas(),
                feedback);
    }

    private void validarManifestosDosCenarios(List<Cenario> cenarios) {
        for (Cenario cenario : cenarios) {
            String idDaTrilha = cenario.id().split("/", 2)[0];
            Path manifesto = diretorioDeConteudo.resolve(idDaTrilha).resolve(MANIFESTO);
            if (!Files.isRegularFile(manifesto)) {
                throw new IllegalArgumentException(
                        "trilha.yaml ausente para a Trilha " + idDaTrilha);
            }
        }
    }

    private Trilha montarTrilha(Path manifesto, List<Cenario> todosOsCenarios) {
        var metadados = leitor.ler(manifesto);
        var cenarios = todosOsCenarios.stream()
                .filter(cenario -> cenario.id().startsWith(metadados.id() + "/"))
                .toList();
        return new Trilha(
                metadados.id(), metadados.titulo(), metadados.ordem(),
                metadados.fundamentos(), cenarios);
    }
}
