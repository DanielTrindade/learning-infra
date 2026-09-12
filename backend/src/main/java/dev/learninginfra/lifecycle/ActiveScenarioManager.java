package dev.learninginfra.lifecycle;

import dev.learninginfra.content.Scenario;
import dev.learninginfra.content.ScenarioRepository;
import dev.learninginfra.execution.CommandExecutor;
import dev.learninginfra.progress.Progress;
import dev.learninginfra.progress.ProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Orquestra o ciclo de vida do Cenário Ativo: derruba o anterior, materializa o
 * workspace, sobe o ambiente de cada trilha e só então registra o novo ativo. Cada
 * etapa tem a sua classe; aqui ficam a ordem e o estado.
 */
@Service
public class ActiveScenarioManager {

    private static final Logger log = LoggerFactory.getLogger(ActiveScenarioManager.class);

    private final Path diretorioDeTrabalho;
    private final ProgressRepository progressRepository;
    private final ScenarioRepository scenarios;
    private final WorkspaceMaterializer materializador;
    private final ScenarioTeardown teardown;
    private final ComposePreparer preparadorCompose;
    private final KubernetesPreparer preparadorKubernetes;
    private final AwsPreparer preparadorAws;

    public ActiveScenarioManager(
            @Value("${learning-infra.working-directory}") String diretorioDeTrabalho,
            CommandExecutor executor,
            ProgressRepository progressRepository,
            ScenarioRepository scenarios) {
        this.diretorioDeTrabalho = Path.of(diretorioDeTrabalho);
        this.progressRepository = progressRepository;
        this.scenarios = scenarios;
        this.materializador = new WorkspaceMaterializer();
        this.teardown = new ScenarioTeardown(executor);
        this.preparadorCompose = new ComposePreparer(executor);
        this.preparadorKubernetes = new KubernetesPreparer(executor, teardown);
        this.preparadorAws = new AwsPreparer(executor);
    }

    public Optional<String> activeScenario() {
        return Optional.ofNullable(progressRepository.load().activeScenario());
    }

    /** Derruba o Cenário anterior, materializa o workspace deste e o torna o Cenário Ativo. */
    public Path start(Scenario scenario) {
        Progress progressoAtual = progressRepository.load();
        String anteriorId = progressoAtual.activeScenario();
        if (anteriorId != null) {
            tearDownPrevious(anteriorId);
        }

        Path work = diretorioDeTrabalho.toAbsolutePath().normalize();
        try {
            materializador.delete(work);
            materializador.copy(scenario, work);
            preparadorCompose.start(scenario, work);
            preparadorKubernetes.prepare(scenario, work);
            preparadorAws.prepare(scenario, work);
        } catch (RuntimeException failure) {
            // O ambiente do Cenário novo não está de pé: limpa o que subiu e não deixa
            // o progresso fingindo que ele está ativo.
            cleanUpPartialEnvironment(scenario);
            progressRepository.update(progress -> progress.withActiveScenario(null));
            throw failure;
        }

        progressRepository.update(progress -> progress.withActiveScenario(scenario.id()));
        return work;
    }

    public void markCompleted(Scenario scenario) {
        progressRepository.update(progress ->
                progress.withCompleted(scenario.id(), Instant.now().toString()));
    }

    /**
     * O id ativo pode ter saído do catálogo entre execuções (diretório renomeado). Sem
     * o Cenário não há como saber o que derrubar, então o registro é limpo e o erro
     * explica o que conferir — nunca pular o teardown em silêncio.
     */
    private void tearDownPrevious(String anteriorId) {
        Scenario anterior = scenarios.find(anteriorId).orElse(null);
        if (anterior == null) {
            progressRepository.update(progress -> progress.withActiveScenario(null));
            throw new IllegalStateException(
                    "o Cenário ativo `" + anteriorId + "` não existe mais no catálogo, e sem "
                    + "ele não sei o que derrubar — confira sobras com `docker ps` e "
                    + "`kubectl get namespaces` antes de iniciar outro Cenário");
        }
        teardown.tearDown(anterior);
    }

    private void cleanUpPartialEnvironment(Scenario scenario) {
        try {
            teardown.tearDown(scenario);
        } catch (RuntimeException sobra) {
            log.warn("não consegui limpar o ambiente parcial do Cenário {}: {}",
                    scenario.id(), sobra.getMessage());
        }
    }
}
