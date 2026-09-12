package dev.learninginfra.verification;

public sealed interface Assertion {

    String description();

    record ContainerRunning(String name) implements Assertion {
        @Override
        public String description() {
            return "o container `" + name + "` está rodando";
        }
    }

    record HttpResponds(String url, int status) implements Assertion {
        @Override
        public String description() {
            return "GET " + url + " responde " + status;
        }
    }

    record HttpBodyContains(String url, String text) implements Assertion {
        @Override
        public String description() {
            return "o corpo de " + url + " contém \"" + text + "\"";
        }
    }

    record ImageExists(String reference) implements Assertion {
        @Override
        public String description() {
            return "a imagem `" + reference + "` existe localmente";
        }
    }

    record VolumeExists(String name) implements Assertion {
        @Override
        public String description() {
            return "o volume `" + name + "` existe";
        }
    }

    record ContainerHealthy(String name) implements Assertion {
        @Override
        public String description() {
            return "o container `" + name + "` está saudável";
        }
    }

    record ContainerInNetwork(String name, String rede, boolean present) implements Assertion {
        @Override
        public String description() {
            return present
                    ? "o container `" + name + "` está na rede `" + rede + "`"
                    : "o container `" + name + "` não está na rede `" + rede + "`";
        }
    }

    /**
     * Verifica a configuração de execução de um container: usuário, filesystem somente
     * leitura e capabilities removidas. Campos nulos não são verificados. A descrição
     * deriva dos campos presentes.
     */
    record ContainerConfiguration(
            String name,
            String user,
            Boolean readOnly,
            java.util.List<String> droppedCapabilities) implements Assertion {

        @Override
        public String description() {
            java.util.List<String> partes = new java.util.ArrayList<>();
            if (user != null) {
                partes.add("roda como `" + user + "`");
            }
            if (readOnly != null && readOnly) {
                partes.add("com filesystem somente leitura");
            }
            if (droppedCapabilities != null && !droppedCapabilities.isEmpty()) {
                partes.add("sem as capabilities " + droppedCapabilities);
            }
            return "o container `" + name + "` " + String.join(", ", partes);
        }
    }

    /** A imagem está publicada no registry referenciado. A descrição vem do Cenário. */
    record ImageInRegistry(String reference, String description) implements Assertion {
    }

    /**
     * O escape hatch do vocabulário: roda um comando e confere a saída. A descrição vem
     * do Cenário, e não do comando, porque o comando costuma revelar a resposta do
     * exercício. O acessor do componente já implementa {@link Assercao#descricao()}.
     */
    record CommandProduces(java.util.List<String> command, String contains, String description)
            implements Assertion {
    }

    record KubernetesCondition(
            String context,
            String namespace,
            String resource,
            String name,
            String condition,
            String status,
            int timeoutSeconds,
            String description) implements Assertion {
    }

    record KubernetesJsonpath(
            String context,
            String namespace,
            String resource,
            String name,
            String expression,
            String contains,
            int timeoutSeconds,
            String description) implements Assertion {
    }

    record KubernetesRbac(
            String context,
            String namespace,
            String serviceAccount,
            String verb,
            String resource,
            boolean allowed,
            String description) implements Assertion {
    }

    record AwsQuery(
            String endpoint,
            String region,
            String service,
            String operation,
            java.util.List<String> arguments,
            String query,
            String expected,
            Comparison comparison,
            String description) implements Assertion {
    }

    /**
     * Um endereço está no state do Terraform e, quando {@code atributo} vem preenchido,
     * com o valor esperado. Prova que o recurso nasceu do código, e não de um comando
     * digitado à mão.
     *
     * <p>O {@code diretorio} é relativo ao diretório de trabalho e chega injetado pelo
     * frontmatter do Cenário; o {@link MotorDeVerificacao} resolve e confina o caminho.
     * O YAML do Cenário não escolhe onde o Terraform roda.
     */
    record TerraformState(
            String directory,
            String address,
            String attribute,
            String expected,
            String description) implements Assertion {

        public TerraformState {
            if ((attribute == null) != (expected == null)) {
                throw new IllegalArgumentException(
                        "atributo e esperado devem ser informados juntos em terraform_estado");
            }
        }
    }

    /**
     * O `plan` não encontra nenhuma mudança pendente. É a Asserção que distingue uma
     * infraestrutura descrita por código de uma infraestrutura construída à mão: prova
     * idempotência e ausência de drift numa afirmação só.
     */
    record TerraformCleanPlan(String directory, String description) implements Assertion {
    }

    /**
     * Um serviço systemd está ativo e habilitado dentro do container Linux do Cenário.
     * Compara códigos de saída — 0 é o único positivo — porque `is-active` de uma unit
     * parada imprime `inactive`, e uma checagem por substring aprovaria o serviço parado.
     */
    record SystemdService(
            String container,
            String name,
            Boolean active,
            Boolean enabled,
            String description) implements Assertion {

        public SystemdService {
            if (active == null && enabled == null) {
                throw new IllegalArgumentException(
                        "ativo ou habilitado deve ser informado em servico_systemd");
            }
        }
    }

    /**
     * Um arquivo ou diretório dentro do container Linux tem o modo, o dono e o grupo
     * esperados. Campos nulos não são verificados. A descrição vem do Cenário.
     */
    record LinuxFile(
            String container,
            String path,
            String modo,
            String dono,
            String grupo,
            String description) implements Assertion {
    }
}
