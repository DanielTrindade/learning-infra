package dev.learninginfra.verification;

import dev.learninginfra.execution.CommandOutput;

import java.util.List;

/** Asserções que observam containers, imagens e volumes pelo CLI do Docker. */
class ContainerEvaluator {

    private final VerificationContext context;

    ContainerEvaluator(VerificationContext context) {
        this.context = context;
    }

    AssertionResult evaluateRunning(Assertion.ContainerRunning a) {
        CommandOutput output = context.executor().execute(
                List.of("docker", "inspect", "-f", "{{.State.Running}}", a.name()));
        if (!output.success()) {
            return AssertionResult.rejected(a, "o container `" + a.name() + "` não existe");
        }
        boolean rodando = output.stdout().trim().equals("true");
        return rodando
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a, "existe, mas está parado");
    }

    AssertionResult evaluateHealth(Assertion.ContainerHealthy a) {
        CommandOutput output = context.executor().execute(List.of(
                "docker", "inspect", "-f", "{{.State.Health.Status}}", a.name()));
        if (!output.success()) {
            return AssertionResult.rejected(a, "o container `" + a.name() + "` não existe");
        }
        String status = output.stdout().strip();
        return status.equals("healthy")
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a, "não está saudável — status atual: `" + status + "`");
    }

    AssertionResult evaluateNetwork(Assertion.ContainerInNetwork a) {
        CommandOutput output = context.executor().execute(List.of(
                "docker", "inspect", "-f",
                "{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}", a.name()));
        if (!output.success()) {
            return AssertionResult.rejected(a, "o container `" + a.name() + "` não existe");
        }
        boolean pertence = java.util.Arrays.stream(output.stdout().strip().split("\\s+"))
                .anyMatch(rede -> rede.equals(a.rede()));
        boolean ok = pertence == a.present();
        return ok
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a,
                        pertence
                                ? "está na rede `" + a.rede() + "` e não deveria"
                                : "não está na rede `" + a.rede() + "`");
    }

    AssertionResult evaluateConfiguration(Assertion.ContainerConfiguration a) {
        if (a.user() != null) {
            CommandOutput output = context.executor().execute(List.of(
                    "docker", "inspect", "-f", "{{.Config.User}}", a.name()));
            if (!output.success()) {
                return AssertionResult.rejected(a, "o container `" + a.name() + "` não existe");
            }
            if (!output.stdout().strip().equals(a.user())) {
                return AssertionResult.rejected(a,
                        "esperava o usuário `" + a.user() + "`, veio `"
                                + output.stdout().strip() + "`");
            }
        }
        if (a.readOnly() != null) {
            CommandOutput output = context.executor().execute(List.of(
                    "docker", "inspect", "-f", "{{.HostConfig.ReadonlyRootfs}}", a.name()));
            if (!output.success()) {
                return AssertionResult.rejected(a, "o container `" + a.name() + "` não existe");
            }
            String expected = String.valueOf(a.readOnly());
            if (!output.stdout().strip().equals(expected)) {
                return AssertionResult.rejected(a,
                        "filesystem somente leitura era `" + expected + "`, veio `"
                                + output.stdout().strip() + "`");
            }
        }
        if (a.droppedCapabilities() != null && !a.droppedCapabilities().isEmpty()) {
            CommandOutput output = context.executor().execute(List.of(
                    "docker", "inspect", "-f", "{{json .HostConfig.CapDrop}}", a.name()));
            if (!output.success()) {
                return AssertionResult.rejected(a, "o container `" + a.name() + "` não existe");
            }
            String drop = output.stdout().toLowerCase();
            for (String capability : a.droppedCapabilities()) {
                if (!drop.contains(capability.toLowerCase())) {
                    return AssertionResult.rejected(a,
                            "a capability `" + capability + "` não foi removida");
                }
            }
        }
        return AssertionResult.approved(a);
    }

    AssertionResult evaluateImage(Assertion.ImageExists a) {
        CommandOutput output = context.executor().execute(
                List.of("docker", "image", "inspect", a.reference()));
        return output.success()
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a,
                        "a imagem `" + a.reference() + "` não foi construída ainda");
    }

    AssertionResult evaluateImageInRegistry(Assertion.ImageInRegistry a) {
        CommandOutput output = context.executor().execute(
                List.of("docker", "manifest", "inspect", "--insecure", a.reference()));
        return output.success()
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a,
                        "a imagem `" + a.reference() + "` não está no registry — "
                        + "confirme que o registry está no ar e que a imagem foi publicada");
    }

    AssertionResult evaluateVolume(Assertion.VolumeExists a) {
        CommandOutput output = context.executor().execute(
                List.of("docker", "volume", "inspect", a.name()));
        return output.success()
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a, "o volume `" + a.name() + "` não existe");
    }
}
