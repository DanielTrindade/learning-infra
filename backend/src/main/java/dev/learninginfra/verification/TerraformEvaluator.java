package dev.learninginfra.verification;

import dev.learninginfra.execution.CommandOutput;
import dev.learninginfra.execution.CommandText;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Asserções que leem o state e planejam sem mutar a infraestrutura do Cenário. */
class TerraformEvaluator {

    /**
     * O `terraform plan` carrega plugins e conversa com o provider; em máquina fria
     * passa fácil dos trinta segundos padrão do executor, sem estar travado.
     */
    private static final Duration TEMPO_LIMITE = Duration.ofMinutes(2);

    private final VerificationContext context;

    TerraformEvaluator(VerificationContext context) {
        this.context = context;
    }

    AssertionResult evaluateState(Assertion.TerraformState a) {
        Path directory = terraformDirectoryOf(a.directory());
        if (directory == null) {
            return AssertionResult.rejected(a,
                    "o Cenário aponta para fora do diretório de trabalho");
        }
        CommandOutput output = context.executor().execute(List.of(
                "terraform", "-chdir=" + directory, "state", "show", "-no-color",
                addressForCommandLine(a.address(), noWindows())), TEMPO_LIMITE);
        if (!output.success()) {
            return AssertionResult.rejected(a,
                    "`" + a.address() + "` não está no state — o recurso não nasceu do código");
        }
        if (a.attribute() == null) {
            return AssertionResult.approved(a);
        }
        String observed = valueInState(output.stdout(), a.attribute());
        if (observed == null) {
            return AssertionResult.rejected(a,
                    "o atributo `" + a.attribute() + "` não aparece no state de `"
                    + a.address() + "`");
        }
        return observed.equals(a.expected())
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a,
                        "`" + a.attribute() + "` no state é `" + observed + "`");
    }

    /**
     * O `-detailed-exitcode` separa três desfechos que um booleano confundiria: 0 é
     * convergido, 2 é divergente e qualquer outro é falha de execução. O `-lock=false`
     * evita reprovar por causa de um lock esquecido: planejar aqui é leitura, não
     * mutação.
     */
    AssertionResult evaluateCleanPlan(Assertion.TerraformCleanPlan a) {
        Path directory = terraformDirectoryOf(a.directory());
        if (directory == null) {
            return AssertionResult.rejected(a,
                    "o Cenário aponta para fora do diretório de trabalho");
        }
        CommandOutput output = context.executor().execute(List.of(
                "terraform", "-chdir=" + directory, "plan",
                "-detailed-exitcode", "-input=false", "-no-color", "-lock=false"),
                TEMPO_LIMITE);
        return switch (output.exitCode()) {
            case 0 -> AssertionResult.approved(a);
            case 2 -> AssertionResult.rejected(a,
                    "ainda há mudanças pendentes — código e realidade divergem");
            default -> AssertionResult.rejected(a,
                    "não consegui planejar — confirme que você rodou `terraform init` neste "
                    + "diretório: " + CommandText.lastDetail(output));
        };
    }

    /**
     * O `state show` imprime `chave = valor` indentado, com aspas em texto. Interessa a
     * primeira ocorrência: blocos aninhados repetem nomes comuns como `name`.
     */
    private String valueInState(String output, String attribute) {
        Matcher achado = Pattern.compile(
                        "^\\s*" + Pattern.quote(attribute) + "\\s*=\\s*(.+?)\\s*$",
                        Pattern.MULTILINE)
                .matcher(output);
        if (!achado.find()) {
            return null;
        }
        String value = achado.group(1);
        return value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1)
                : value;
    }

    /**
     * Confina o Terraform ao diretório de trabalho. O Cenário declara um caminho
     * relativo; se ele escapar, a Verificação reprova em vez de rodar `terraform` num
     * diretório arbitrário da máquina do leitor.
     */
    private Path terraformDirectoryOf(String relativo) {
        Path root = context.raizDeTrabalho().toAbsolutePath().normalize();
        Path alvo = root.resolve(relativo).normalize();
        return alvo.startsWith(root) ? alvo : null;
    }

    /**
     * O `terraform state list` devolve endereços com aspas embutidas, como
     * {@code module.ambiente["producao"].docker_container.web}. No Windows o
     * {@link ProcessBuilder} só cita argumentos com espaço, então as aspas atravessam a
     * linha de comando cruas e o runtime do Go as desfaz antes de o Terraform ver o
     * argumento — que chega como {@code module.ambiente[producao]...} e é recusado.
     * Escapar com barra invertida é o que sobrevive à viagem. Fora do Windows não há
     * linha de comando intermediária e o endereço vai como está.
     */
    static String addressForCommandLine(String address, boolean windows) {
        return windows ? address.replace("\"", "\\\"") : address;
    }

    private static boolean noWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}
