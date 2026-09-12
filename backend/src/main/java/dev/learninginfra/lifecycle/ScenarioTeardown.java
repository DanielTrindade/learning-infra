package dev.learninginfra.lifecycle;

import dev.learninginfra.content.Scenario;
import dev.learninginfra.execution.DockerErrors;
import dev.learninginfra.execution.CommandExecutor;
import dev.learninginfra.execution.CommandOutput;
import dev.learninginfra.execution.CommandText;
import dev.learninginfra.execution.KubernetesErrors;

import java.time.Duration;
import java.util.List;

/**
 * Derruba o ambiente de um Cenário em ordem segura: Compose, containers, volumes —
 * volumes por último, porque um volume em uso por container vivo não é removível.
 */
class ScenarioTeardown {

    private final CommandExecutor executor;
    private final AwsLocalCleanup limpezaAws;

    ScenarioTeardown(CommandExecutor executor) {
        this.executor = executor;
        this.limpezaAws = new AwsLocalCleanup(executor);
    }

    void tearDown(Scenario anterior) {
        if (anterior.usesCompose()) {
            CommandOutput output = executor.execute(
                    List.of("docker", "compose", "-p", anterior.composeProject(), "down", "-v"));
            if (!output.success()) {
                throw new IllegalStateException(
                        "não consegui derrubar o projeto Compose `" + anterior.composeProject()
                        + "` do Cenário " + anterior.id() + ": " + output.stderr().strip()
                        + " — resolva isso antes de continuar, senão a próxima Verificação pode "
                        + "passar por sobra de ambiente");
            }
        }
        for (String container : anterior.containers()) {
            CommandOutput output = executor.execute(List.of("docker", "rm", "-f", container));
            if (!output.success() && !DockerErrors.looksMissing(output)) {
                throw new IllegalStateException(
                        "não consegui remover o container `" + container + "` do Cenário "
                        + anterior.id() + ": " + output.stderr().strip()
                        + " — resolva isso antes de continuar, senão a próxima Verificação pode "
                        + "passar por sobra de ambiente");
            }
        }
        // Volumes por último: um volume em uso por container vivo não é removível.
        for (String volume : anterior.volumes()) {
            CommandOutput output = executor.execute(
                    List.of("docker", "volume", "rm", "-f", volume));
            if (!output.success() && !DockerErrors.looksMissing(output)) {
                throw new IllegalStateException(
                        "não consegui remover o volume `" + volume + "` do Cenário "
                        + anterior.id() + ": " + output.stderr().strip()
                        + " — sem isso a próxima Verificação deste Cenário passaria sozinha, "
                        + "com o dado da vez anterior");
            }
        }
        if (anterior.usesKubernetes()) {
            removeNamespace(anterior, "derrubar o Cenário anterior");
        }
        if (anterior.usesAws()) {
            limpezaAws.clean("derrubar o Cenário AWS anterior");
        }
    }

    /**
     * Um namespace com workloads em encerramento gracioso pode ficar em `Terminating`
     * por mais de vinte segundos (o padrao de graceful termination de um Pod é 30s).
     * O timeout curto fazia um início de Cenário falhar por um namespace que estava
     * apenas terminando. O comando recebe um teto próprio do executor, maior que o
     * timeout do próprio kubectl.
     */
    private static final Duration LIMITE_REMOCAO_NAMESPACE = Duration.ofSeconds(90);

    void removeNamespace(Scenario scenario, String action) {
        CommandOutput output = executor.execute(List.of(
                "kubectl", "--context", scenario.kubernetesContext(),
                "delete", "namespace", scenario.kubernetesNamespace(),
                "--ignore-not-found=true", "--wait=true", "--timeout=60s"),
                LIMITE_REMOCAO_NAMESPACE);
        if (output.success()) {
            return;
        }

        // O `delete --wait` pode estourar o tempo com o namespace ainda terminando em
        // segundo plano. Antes de decretar falha, confere o estado real do recurso.
        CommandOutput check = executor.execute(List.of(
                "kubectl", "--context", scenario.kubernetesContext(),
                "get", "namespace", scenario.kubernetesNamespace(),
                "-o", "jsonpath={.status.phase}", "--request-timeout=10s"));
        if (KubernetesErrors.looksMissing(check)) {
            return; // já sumiu; a remoção terminou enquanto o delete esperava
        }
        if (check.success() && check.stdout().contains("Terminating")) {
            CommandOutput wait = executor.execute(List.of(
                    "kubectl", "--context", scenario.kubernetesContext(),
                    "wait", "--for=delete", "namespace", scenario.kubernetesNamespace(),
                    "--timeout=60s"), LIMITE_REMOCAO_NAMESPACE);
            if (wait.success()) {
                return;
            }
        }

        throw new IllegalStateException(
                "não consegui remover o namespace `" + scenario.kubernetesNamespace()
                + "` para " + action + ": " + CommandText.lastDetail(output)
                + " — o namespace ainda existe; confira os finalizers com `kubectl --context "
                + scenario.kubernetesContext() + " get namespace "
                + scenario.kubernetesNamespace() + " -o yaml` e remova o que sobrar "
                + "antes de continuar, para a próxima Verificação não passar por sobra "
                + "de ambiente");
    }
}
