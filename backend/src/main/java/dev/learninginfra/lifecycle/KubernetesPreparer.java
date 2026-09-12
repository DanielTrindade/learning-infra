package dev.learninginfra.lifecycle;

import dev.learninginfra.content.Scenario;
import dev.learninginfra.execution.CommandExecutor;
import dev.learninginfra.execution.CommandOutput;
import dev.learninginfra.execution.CommandText;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Recria o namespace exclusivo do Cenário e, quando declarado, aplica o ambiente
 * inicial. O contexto é sempre explícito para um laboratório nunca atingir por
 * acidente outro cluster configurado no kubectl do autor.
 *
 * <p>Em Cenário de Terraform o namespace não é criado adiantado: ele nasce do
 * `terraform apply` do leitor, porque o código é dono do objeto — criá-lo antes faria o
 * apply falhar com "already exists". A remoção prévia continua valendo, para o apply
 * nunca encontrar sobra de uma execução anterior.
 */
class KubernetesPreparer {

    private final CommandExecutor executor;
    private final ScenarioTeardown teardown;

    KubernetesPreparer(CommandExecutor executor, ScenarioTeardown teardown) {
        this.executor = executor;
        this.teardown = teardown;
    }

    void prepare(Scenario scenario, Path work) {
        if (!scenario.usesKubernetes()) {
            return;
        }

        CommandOutput ready = executor.execute(List.of(
                "kubectl", "--context", scenario.kubernetesContext(),
                "get", "--raw=/readyz", "--request-timeout=5s"));
        if (!ready.success()) {
            throw new IllegalStateException(
                    "o cluster Kubernetes do contexto `" + scenario.kubernetesContext()
                    + "` não está acessível — crie ou inicie o cluster Kubernetes no Docker "
                    + "Desktop antes de começar este Cenário: "
                    + CommandText.lastDetail(ready));
        }

        teardown.removeNamespace(scenario, "recriar o ambiente");

        if (scenario.usesTerraform()) {
            return;
        }

        CommandOutput created = executor.execute(List.of(
                "kubectl", "--context", scenario.kubernetesContext(),
                "create", "namespace", scenario.kubernetesNamespace()));
        if (!created.success()) {
            throw new IllegalStateException(
                    "não consegui criar o namespace `" + scenario.kubernetesNamespace()
                    + "` do Cenário " + scenario.id() + ": "
                    + CommandText.lastDetail(created));
        }

        if (!scenario.hasInitialManifests()) {
            return;
        }

        Path manifestos = work.resolve(scenario.initialManifests()).normalize();
        if (!manifestos.startsWith(work) || !Files.exists(manifestos)) {
            throw new IllegalStateException(
                    "manifestosIniciais aponta para fora do workspace ou não existe: "
                    + scenario.initialManifests());
        }

        CommandOutput applied = executor.execute(List.of(
                "kubectl", "--context", scenario.kubernetesContext(),
                "--namespace", scenario.kubernetesNamespace(),
                "apply", "-f", manifestos.toString()));
        if (!applied.success()) {
            throw new IllegalStateException(
                    "não consegui aplicar o ambiente inicial do Cenário " + scenario.id()
                    + ": " + CommandText.lastDetail(applied));
        }
    }
}
