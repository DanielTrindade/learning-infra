package dev.learninginfra.verification;

import dev.learninginfra.execution.CommandOutput;
import dev.learninginfra.execution.CommandText;

import java.util.List;

/** Asserções que esperam convergência no cluster Kubernetes do Cenário. */
class KubernetesEvaluator {

    private final VerificationContext context;

    KubernetesEvaluator(VerificationContext context) {
        this.context = context;
    }

    AssertionResult evaluateCondition(Assertion.KubernetesCondition a) {
        CommandOutput output = context.executor().execute(List.of(
                "kubectl", "--context", a.context(), "--namespace", a.namespace(),
                "wait", a.resource() + "/" + a.name(),
                "--for=condition=" + a.condition() + "=" + a.status(),
                "--timeout=" + a.timeoutSeconds() + "s"));
        return output.success()
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a,
                        "a Condition não convergiu: " + CommandText.lastDetail(output));
    }

    AssertionResult evaluateJsonpath(Assertion.KubernetesJsonpath a) {
        CommandOutput output = context.executor().execute(List.of(
                "kubectl", "--context", a.context(), "--namespace", a.namespace(),
                "wait", a.resource() + "/" + a.name(),
                "--for=jsonpath=" + a.expression() + "=" + a.contains(),
                "--timeout=" + a.timeoutSeconds() + "s"));
        return output.success()
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a,
                        "a propriedade observada não convergiu: "
                        + CommandText.lastDetail(output));
    }

    AssertionResult evaluateRbac(Assertion.KubernetesRbac a) {
        String identity = "system:serviceaccount:" + a.namespace() + ":" + a.serviceAccount();
        CommandOutput output = context.executor().execute(List.of(
                "kubectl", "--context", a.context(), "auth", "can-i",
                a.verb(), a.resource(), "--as=" + identity, "--namespace", a.namespace()));
        String response = output.stdout().strip().toLowerCase();
        if (!response.equals("yes") && !response.equals("no")) {
            return AssertionResult.rejected(a,
                    "não consegui consultar a autorização: "
                    + CommandText.lastDetail(output));
        }
        boolean observed = response.equals("yes");
        return observed == a.allowed()
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a,
                        observed ? "a identidade tem permissão além do necessário"
                                  : "a identidade ainda não recebeu a permissão necessária");
    }
}
