package dev.learninginfra.verification;

import dev.learninginfra.execution.CommandOutput;
import dev.learninginfra.execution.CommandText;

/** O escape hatch do vocabulário: roda um comando e confere a saída. */
class CommandEvaluator {

    private final VerificationContext context;

    CommandEvaluator(VerificationContext context) {
        this.context = context;
    }

    AssertionResult avaliar(Assertion.CommandProduces a) {
        CommandOutput output = context.executor().execute(a.command());
        if (!output.success()) {
            return AssertionResult.rejected(a,
                    "a checagem não completou: " + CommandText.lastLine(output.stderr()));
        }
        return output.stdout().contains(a.contains())
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a, "rodou, mas a saída veio sem o texto esperado");
    }
}
