package dev.learninginfra.content;

import dev.learninginfra.verification.Assertion;

import java.nio.file.Path;
import java.util.List;

/**
 * Um Cenário publicado. A configuração de infraestrutura fica agrupada por trilha: só
 * o que o Cenário declara é ligado, e cada grupo responde pelas suas próprias perguntas.
 */
public record Scenario(
        String id,
        String title,
        Difficulty difficulty,
        List<String> containers,
        String markdown,
        Path directory,
        List<Assertion> assertions,
        ComposeConfiguration compose,
        KubernetesConfiguration kubernetes,
        AwsConfiguration aws,
        TerraformConfiguration terraform,
        LinuxConfiguration linux) {

    public Scenario {
        containers = containers == null ? List.of() : List.copyOf(containers);
        assertions = assertions == null ? List.of() : List.copyOf(assertions);
    }

    /** Cenário sem nenhuma infraestrutura declarada. */
    public static Scenario simples(
            String id, String title, Difficulty difficulty, List<String> containers,
            String markdown, Path directory, List<Assertion> assertions) {
        return new Scenario(id, title, difficulty, containers, markdown, directory, assertions,
                ComposeConfiguration.nenhuma(), KubernetesConfiguration.nenhuma(),
                AwsConfiguration.nenhuma(), TerraformConfiguration.nenhuma(),
                LinuxConfiguration.nenhuma());
    }

    public record ComposeConfiguration(String projeto, List<String> volumes) {

        public ComposeConfiguration {
            volumes = volumes == null ? List.of() : List.copyOf(volumes);
        }

        public static ComposeConfiguration nenhuma() {
            return new ComposeConfiguration(null, List.of());
        }

        public static ComposeConfiguration de(String projeto) {
            return new ComposeConfiguration(projeto, List.of());
        }

        public boolean active() {
            return projeto != null && !projeto.isBlank();
        }
    }

    public record KubernetesConfiguration(
            String context, String namespace, String initialManifests) {

        public static KubernetesConfiguration nenhuma() {
            return new KubernetesConfiguration(null, null, null);
        }

        public boolean active() {
            return context != null && !context.isBlank()
                    && namespace != null && !namespace.isBlank();
        }

        public boolean hasInitialManifests() {
            return initialManifests != null && !initialManifests.isBlank();
        }
    }

    public record AwsConfiguration(boolean ministack, boolean infraestruturaReal, String inicializacao) {

        public static AwsConfiguration nenhuma() {
            return new AwsConfiguration(false, false, null);
        }

        public boolean active() {
            return ministack;
        }

        public boolean hasInitialization() {
            return inicializacao != null && !inicializacao.isBlank();
        }
    }

    public record TerraformConfiguration(boolean active, String directory) {

        public static TerraformConfiguration nenhuma() {
            return new TerraformConfiguration(false, null);
        }
    }

    public record LinuxConfiguration(String container) {

        public static LinuxConfiguration nenhuma() {
            return new LinuxConfiguration(null);
        }

        public boolean active() {
            return container != null && !container.isBlank();
        }
    }

    public boolean usesCompose() {
        return compose.active();
    }

    public String composeProject() {
        return compose.projeto();
    }

    public List<String> volumes() {
        return compose.volumes();
    }

    public boolean usesDocker() {
        return id.startsWith("docker/");
    }

    public boolean usesKubernetes() {
        return kubernetes.active();
    }

    public String kubernetesContext() {
        return kubernetes.context();
    }

    public String kubernetesNamespace() {
        return kubernetes.namespace();
    }

    public boolean hasInitialManifests() {
        return kubernetes.hasInitialManifests();
    }

    public String initialManifests() {
        return kubernetes.initialManifests();
    }

    public boolean usesAws() {
        return aws.active();
    }

    public boolean usesRealAwsInfrastructure() {
        return aws.infraestruturaReal();
    }

    public String awsInitialization() {
        return aws.inicializacao();
    }

    public boolean hasAwsInitialization() {
        return aws.hasInitialization();
    }

    public boolean usesTerraform() {
        return terraform.active();
    }

    public String terraformDirectory() {
        return terraform.directory();
    }

    public boolean usesLinux() {
        return linux.active();
    }

    public String linuxContainer() {
        return linux.container();
    }

    public Path workspace() {
        return directory.resolve("workspace");
    }
}
