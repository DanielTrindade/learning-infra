package dev.learninginfra.verification;

import dev.learninginfra.execution.CommandOutput;
import dev.learninginfra.execution.CommandText;

import java.util.List;

/** Asserções que inspecionam o container Linux do Cenário. */
class LinuxEvaluator {

    private final VerificationContext context;

    LinuxEvaluator(VerificationContext context) {
        this.context = context;
    }

    /**
     * Compara códigos de saída, e não a saída em si: `systemctl is-active` de uma unit
     * parada imprime `inactive`, e `"inactive".contains("active")` aprovaria um serviço
     * morto. `0` é o único positivo; o código do "não" é específico de cada consulta.
     */
    AssertionResult evaluateSystemdService(Assertion.SystemdService a) {
        if (a.active() != null) {
            CommandOutput output = context.executor().execute(List.of(
                    "docker", "exec", a.container(),
                    "systemctl", "is-active", "--quiet", a.name()));
            AssertionResult result = checkState(a, output, a.active(), "ativo", 3);
            if (result != null) {
                return result;
            }
        }
        if (a.enabled() != null) {
            CommandOutput output = context.executor().execute(List.of(
                    "docker", "exec", a.container(),
                    "systemctl", "is-enabled", "--quiet", a.name()));
            AssertionResult result = checkState(a, output, a.enabled(), "habilitado", 1);
            if (result != null) {
                return result;
            }
        }
        return AssertionResult.approved(a);
    }

    /**
     * `0` é o único código positivo; o código do "não" é específico (3 para is-active,
     * 1 para is-enabled). Qualquer outro desfecho — 4 para unit inexistente, ou a falha
     * do `docker exec` quando o container não está no ar — reprova, para um serviço
     * ausente nunca aprovar como "parado".
     */
    private AssertionResult checkState(
            Assertion.SystemdService a, CommandOutput output, boolean expected,
            String adjetivo, int codigoDoNao) {
        if (!output.stderr().isBlank()) {
            return AssertionResult.rejected(a,
                    "não consegui consultar o serviço `" + a.name()
                    + "` — confirme que o container `" + a.container()
                    + "` está no ar: " + CommandText.lastLine(output.stderr()));
        }
        int codigo = output.exitCode();
        if (codigo == 0) {
            return expected
                    ? null
                    : AssertionResult.rejected(a,
                            "o serviço `" + a.name() + "` está " + adjetivo + " e não deveria");
        }
        if (codigo == codigoDoNao) {
            return expected
                    ? AssertionResult.rejected(a,
                            "o serviço `" + a.name() + "` não está " + adjetivo)
                    : null;
        }
        return AssertionResult.rejected(a,
                "a unit `" + a.name() + "` não existe no container `" + a.container() + "`");
    }

    AssertionResult evaluateFile(Assertion.LinuxFile a) {
        CommandOutput output = context.executor().execute(List.of(
                "docker", "exec", a.container(),
                "stat", "-c", "%a %U %G", a.path()));
        if (!output.success()) {
            return AssertionResult.rejected(a,
                    "o caminho `" + a.path() + "` não existe no container `"
                    + a.container() + "`");
        }
        String[] campos = output.stdout().strip().split("\\s+");
        if (campos.length < 3) {
            return AssertionResult.rejected(a, "não consegui ler o `stat` de `" + a.path() + "`");
        }
        if (a.modo() != null && !campos[0].equals(a.modo())) {
            return AssertionResult.rejected(a, "o modo de `" + a.path() + "` é `" + campos[0] + "`");
        }
        if (a.dono() != null && !campos[1].equals(a.dono())) {
            return AssertionResult.rejected(a, "o dono de `" + a.path() + "` é `" + campos[1] + "`");
        }
        if (a.grupo() != null && !campos[2].equals(a.grupo())) {
            return AssertionResult.rejected(a, "o grupo de `" + a.path() + "` é `" + campos[2] + "`");
        }
        return AssertionResult.approved(a);
    }
}
