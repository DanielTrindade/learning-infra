package dev.learninginfra.verification;

import dev.learninginfra.execution.CommandOutput;
import dev.learninginfra.execution.CommandText;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Asserções contra o MiniStack. Endpoint, região e credenciais sintéticas são fixados
 * aqui e o conteúdo nunca os substitui.
 */
class AwsEvaluator {

    private static final Map<String, String> AMBIENTE_LOCAL = Map.of(
            "AWS_ACCESS_KEY_ID", "000000000000",
            "AWS_SECRET_ACCESS_KEY", "test",
            "AWS_SESSION_TOKEN", "",
            "AWS_EC2_METADATA_DISABLED", "true");

    private final VerificationContext context;

    AwsEvaluator(VerificationContext context) {
        this.context = context;
    }

    AssertionResult evaluateQuery(Assertion.AwsQuery a) {
        List<String> command = new java.util.ArrayList<>(List.of(
                "aws", "--endpoint-url", a.endpoint(), "--region", a.region(),
                "--no-cli-pager", a.service(), a.operation()));
        command.addAll(a.arguments());
        command.addAll(List.of("--query", a.query(), "--output", "text"));

        CommandOutput output = context.executor().execute(
                List.copyOf(command), AMBIENTE_LOCAL);
        if (!output.success()) {
            return AssertionResult.rejected(a,
                    "não consegui consultar o MiniStack: " + CommandText.lastDetail(output));
        }
        String observed = output.stdout().strip();
        boolean confere = switch (a.comparison()) {
            case EXACT -> Arrays.stream(observed.split("\\R"))
                    .map(String::strip)
                    .anyMatch(line -> line.equals(a.expected()));
            case CONTAINS -> observed.contains(a.expected());
        };
        return confere
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a, "valor observado: `" + observed + "`");
    }
}
