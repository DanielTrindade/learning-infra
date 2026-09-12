package dev.learninginfra.verification;

import dev.learninginfra.execution.CommandExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Decide qual avaliador cuida de cada Asserção. O `switch` exaustivo é proposital e
 * continua sendo a lista completa do vocabulário: um tipo de Asserção novo quebra a
 * compilação aqui até ganhar um avaliador.
 */
@Service
public class VerificationEngine {

    /**
     * Generoso de propósito. Um serviço do Cenário pode depender de outro que está
     * morrendo, e aí ele responde só depois do próprio timeout interno — cinco segundos
     * é comum. Um limite apertado reprovaria como "morto" um serviço que está de pé.
     */
    private static final Duration ESPERA_PADRAO = Duration.ofSeconds(10);

    private final ContainerEvaluator containers;
    private final HttpEvaluator http;
    private final KubernetesEvaluator kubernetes;
    private final AwsEvaluator aws;
    private final TerraformEvaluator terraform;
    private final LinuxEvaluator linux;
    private final CommandEvaluator command;

    /**
     * O {@code @Autowired} é obrigatório: com mais de um construtor, o Spring não elege
     * nenhum sozinho e procura um construtor sem argumentos, que não existe.
     */
    @Autowired
    public VerificationEngine(
            CommandExecutor executor,
            @Value("${learning-infra.working-directory}") String diretorioDeTrabalho) {
        this(executor, ESPERA_PADRAO, diretorioDeTrabalho);
    }

    /** Só para teste: o dublê simples, sem interesse em espera nem em Terraform. */
    VerificationEngine(CommandExecutor executor) {
        this(executor, ESPERA_PADRAO, "../work");
    }

    /** Só para teste: permite uma espera curta sem deixar a suíte lenta. */
    VerificationEngine(CommandExecutor executor, Duration espera) {
        this(executor, espera, "../work");
    }

    /** Só para teste: espera curta e um diretório de trabalho controlado. */
    VerificationEngine(CommandExecutor executor, Duration espera, String diretorioDeTrabalho) {
        VerificationContext context = new VerificationContext(
                executor,
                espera,
                HttpClient.newBuilder().connectTimeout(espera).build(),
                Path.of(diretorioDeTrabalho));
        this.containers = new ContainerEvaluator(context);
        this.http = new HttpEvaluator(context);
        this.kubernetes = new KubernetesEvaluator(context);
        this.aws = new AwsEvaluator(context);
        this.terraform = new TerraformEvaluator(context);
        this.linux = new LinuxEvaluator(context);
        this.command = new CommandEvaluator(context);
    }

    /**
     * As Asserções são independentes e rodam em paralelo — um HTTP travado ou um
     * `kubectl wait` não espera os demais. A ordem declarada é preservada no checklist.
     */
    public VerificationResult verify(List<Assertion> assertions) {
        try (java.util.concurrent.ExecutorService avaliadores =
                     java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            List<java.util.concurrent.Future<AssertionResult>> futuros = assertions.stream()
                    .map(assertion -> avaliadores.submit(() -> avaliar(assertion)))
                    .toList();
            List<AssertionResult> results = new java.util.ArrayList<>(assertions.size());
            for (java.util.concurrent.Future<AssertionResult> futuro : futuros) {
                results.add(await(futuro));
            }
            boolean completed = results.stream().allMatch(AssertionResult::passed);
            return new VerificationResult(completed, List.copyOf(results));
        }
    }

    private AssertionResult await(
            java.util.concurrent.Future<AssertionResult> futuro) {
        try {
            return futuro.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("verificação interrompida", e);
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof RuntimeException error) {
                throw error;
            }
            throw new IllegalStateException("falha ao avaliar a Asserção", e.getCause());
        }
    }

    private AssertionResult avaliar(Assertion assertion) {
        return switch (assertion) {
            case Assertion.ContainerRunning a -> containers.evaluateRunning(a);
            case Assertion.ContainerHealthy a -> containers.evaluateHealth(a);
            case Assertion.ContainerInNetwork a -> containers.evaluateNetwork(a);
            case Assertion.ContainerConfiguration a -> containers.evaluateConfiguration(a);
            case Assertion.HttpResponds a -> http.evaluateStatus(a);
            case Assertion.HttpBodyContains a -> http.evaluateBody(a);
            case Assertion.ImageExists a -> containers.evaluateImage(a);
            case Assertion.ImageInRegistry a -> containers.evaluateImageInRegistry(a);
            case Assertion.VolumeExists a -> containers.evaluateVolume(a);
            case Assertion.CommandProduces a -> command.avaliar(a);
            case Assertion.KubernetesCondition a -> kubernetes.evaluateCondition(a);
            case Assertion.KubernetesJsonpath a -> kubernetes.evaluateJsonpath(a);
            case Assertion.KubernetesRbac a -> kubernetes.evaluateRbac(a);
            case Assertion.AwsQuery a -> aws.evaluateQuery(a);
            case Assertion.TerraformState a -> terraform.evaluateState(a);
            case Assertion.TerraformCleanPlan a -> terraform.evaluateCleanPlan(a);
            case Assertion.SystemdService a -> linux.evaluateSystemdService(a);
            case Assertion.LinuxFile a -> linux.evaluateFile(a);
        };
    }
}
