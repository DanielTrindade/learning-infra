package dev.learninginfra.content;

import dev.learninginfra.verification.Assertion;
import dev.learninginfra.verification.Comparison;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ScenarioReader {

    private static final String DELIMITADOR = "---";
    private static final String ENDPOINT_AWS_LOCAL = "http://127.0.0.1:4566";
    private static final String REGIAO_AWS_LOCAL = "us-east-1";

    public Scenario read(Path scenarioDirectory) {
        String raw = readFile(scenarioDirectory.resolve("cenario.md"));
        String[] partes = splitFrontmatter(raw, scenarioDirectory);

        Map<String, Object> meta = new Yaml().load(partes[0]);
        String corpo = partes[1];

        String kubernetesContext = optionalText(meta, "contextoKubernetes");
        String kubernetesNamespace = optionalText(meta, "namespaceKubernetes");
        String initialManifests = optionalText(meta, "manifestosIniciais");
        validateKubernetesMetadata(
                kubernetesContext, kubernetesNamespace, initialManifests, scenarioDirectory);
        boolean ministack = optionalBoolean(meta, "ministack");
        boolean usesRealAwsInfrastructure = optionalBoolean(meta, "infraestruturaRealAws");
        String awsInitialization = optionalText(meta, "inicializacaoAws");
        validateAwsMetadata(
                ministack, usesRealAwsInfrastructure, awsInitialization, scenarioDirectory);
        boolean terraform = optionalBoolean(meta, "terraform");
        String terraformDirectory = optionalText(meta, "diretorioTerraform");
        validateTerraformMetadata(terraform, terraformDirectory, scenarioDirectory);
        if (terraform && terraformDirectory == null) {
            terraformDirectory = ".";
        }
        String linuxContainer = optionalText(meta, "containerLinux");
        List<Assertion> assertions = readAssertions(
                scenarioDirectory.resolve("verificacao.yaml"),
                kubernetesContext,
                kubernetesNamespace,
                ministack,
                terraformDirectory,
                linuxContainer);

        return new Scenario(
                requireText(meta, "id"),
                requireText(meta, "titulo"),
                Difficulty.fromText(requireText(meta, "dificuldade")),
                optionalList(meta, "containers"),
                corpo,
                scenarioDirectory,
                assertions,
                new Scenario.ComposeConfiguration(
                        optionalText(meta, "projetoCompose"),
                        optionalList(meta, "volumes")),
                new Scenario.KubernetesConfiguration(
                        kubernetesContext, kubernetesNamespace, initialManifests),
                new Scenario.AwsConfiguration(
                        ministack, usesRealAwsInfrastructure, awsInitialization),
                new Scenario.TerraformConfiguration(terraform, terraformDirectory),
                new Scenario.LinuxConfiguration(linuxContainer));
    }

    private void validateAwsMetadata(
            boolean ministack, boolean infraestruturaReal, String inicializacao, Path directory) {
        if (!ministack && (infraestruturaReal || inicializacao != null)) {
            throw new IllegalArgumentException(
                    "infraestruturaRealAws e inicializacaoAws exigem ministack: true em " + directory);
        }
    }

    /**
     * O diretório do Terraform é relativo ao diretório de trabalho e nunca escapa dele.
     * A checagem é textual porque o caminho vem do conteúdo, e um `Path` absoluto no
     * Windows pode não parecer absoluto para o Java quando começa só com barra.
     */
    private void validateTerraformMetadata(
            boolean terraform, String terraformDirectory, Path directory) {
        if (!terraform && terraformDirectory != null) {
            throw new IllegalArgumentException(
                    "diretorioTerraform exige terraform: true em " + directory);
        }
        if (terraformDirectory == null) {
            return;
        }
        boolean absoluto = terraformDirectory.startsWith("/")
                || terraformDirectory.startsWith("\\")
                || terraformDirectory.matches("(?i)^[a-z]:.*");
        boolean escapa = Path.of(terraformDirectory).normalize().startsWith("..");
        if (absoluto || escapa) {
            throw new IllegalArgumentException(
                    "diretorioTerraform deve ser relativo ao workspace e não pode escapar dele em "
                            + directory);
        }
    }

    private void validateKubernetesMetadata(
            String context, String namespace, String manifestos, Path directory) {
        boolean temContexto = context != null && !context.isBlank();
        boolean temNamespace = namespace != null && !namespace.isBlank();
        if (temContexto != temNamespace) {
            throw new IllegalArgumentException(
                    "contextoKubernetes e namespaceKubernetes devem aparecer juntos em " + directory);
        }
        if (manifestos != null && !manifestos.isBlank() && !temContexto) {
            throw new IllegalArgumentException(
                    "manifestosIniciais exige contextoKubernetes e namespaceKubernetes em " + directory);
        }
    }

    private String[] splitFrontmatter(String raw, Path directory) {
        String normalizado = raw.replace("\r\n", "\n").stripLeading();
        if (!normalizado.startsWith(DELIMITADOR + "\n")) {
            throw new IllegalArgumentException("cenario.md sem frontmatter em " + directory);
        }
        int fim = normalizado.indexOf("\n" + DELIMITADOR, DELIMITADOR.length());
        if (fim < 0) {
            throw new IllegalArgumentException("frontmatter não fechado em " + directory);
        }
        String frontmatter = normalizado.substring(DELIMITADOR.length() + 1, fim);
        String corpo = normalizado.substring(fim + 1 + DELIMITADOR.length()).stripLeading();
        return new String[]{frontmatter, corpo};
    }

    @SuppressWarnings("unchecked")
    private List<Assertion> readAssertions(
            Path file,
            String kubernetesContext,
            String kubernetesNamespace,
            boolean ministack,
            String terraformDirectory,
            String linuxContainer) {
        Map<String, Object> root = new Yaml().load(readFile(file));
        List<Map<String, Object>> items = (List<Map<String, Object>>) root.get("asercoes");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("verificacao.yaml sem asserções: " + file);
        }
        List<Assertion> assertions = new ArrayList<>();
        for (Map<String, Object> item : items) {
            assertions.add(buildAssertion(
                    item,
                    file,
                    kubernetesContext,
                    kubernetesNamespace,
                    ministack,
                    terraformDirectory,
                    linuxContainer));
        }
        return List.copyOf(assertions);
    }

    private Assertion buildAssertion(
            Map<String, Object> item,
            Path file,
            String kubernetesContext,
            String kubernetesNamespace,
            boolean ministack,
            String terraformDirectory,
            String linuxContainer) {
        String tipo = requireText(item, "tipo");
        return switch (tipo) {
            case "container_rodando" -> new Assertion.ContainerRunning(requireText(item, "nome"));
            case "http_responde" -> new Assertion.HttpResponds(
                    requireText(item, "url"), (Integer) item.getOrDefault("status", 200));
            case "http_corpo_contem" -> new Assertion.HttpBodyContains(
                    requireText(item, "url"), requireText(item, "texto"));
            case "imagem_existe" -> new Assertion.ImageExists(requireText(item, "referencia"));
            case "imagem_no_registry" -> new Assertion.ImageInRegistry(
                    requireText(item, "referencia"),
                    requireText(item, "descricao"));
            case "volume_existe" -> new Assertion.VolumeExists(requireText(item, "nome"));
            case "container_saudavel" -> new Assertion.ContainerHealthy(requireText(item, "nome"));
            case "container_em_rede" -> new Assertion.ContainerInNetwork(
                    requireText(item, "nome"),
                    requireText(item, "rede"),
                    requireBoolean(item, "presente"));
            case "container_configuracao" -> new Assertion.ContainerConfiguration(
                    requireText(item, "nome"),
                    optionalText(item, "usuario"),
                    (Boolean) item.getOrDefault("somenteLeitura", null),
                    optionalList(item, "capabilitiesRemovidas"));
            case "comando_produz" -> new Assertion.CommandProduces(
                    requireList(item, "comando"),
                    requireText(item, "contem"),
                    requireText(item, "descricao"));
            case "kubernetes_condicao" -> {
                requireKubernetesMetadata(kubernetesContext, kubernetesNamespace, tipo, file);
                yield new Assertion.KubernetesCondition(
                        kubernetesContext,
                        kubernetesNamespace,
                        requireText(item, "recurso"),
                        requireText(item, "nome"),
                        requireText(item, "condicao"),
                        item.getOrDefault("status", "True").toString(),
                        (Integer) item.getOrDefault("timeout", 10),
                        requireText(item, "descricao"));
            }
            case "kubernetes_jsonpath" -> {
                requireKubernetesMetadata(kubernetesContext, kubernetesNamespace, tipo, file);
                yield new Assertion.KubernetesJsonpath(
                        kubernetesContext,
                        kubernetesNamespace,
                        requireText(item, "recurso"),
                        requireText(item, "nome"),
                        requireText(item, "expressao"),
                        requireText(item, "contem"),
                        (Integer) item.getOrDefault("timeout", 10),
                        requireText(item, "descricao"));
            }
            case "kubernetes_rbac" -> {
                requireKubernetesMetadata(kubernetesContext, kubernetesNamespace, tipo, file);
                yield new Assertion.KubernetesRbac(
                        kubernetesContext,
                        kubernetesNamespace,
                        requireText(item, "serviceAccount"),
                        requireText(item, "verbo"),
                        requireText(item, "recurso"),
                        requireBoolean(item, "permitido"),
                        requireText(item, "descricao"));
            }
            case "aws_consulta" -> {
                requireMinistack(ministack, tipo, file);
                yield new Assertion.AwsQuery(
                        ENDPOINT_AWS_LOCAL,
                        REGIAO_AWS_LOCAL,
                        requireText(item, "servico"),
                        requireText(item, "operacao"),
                        optionalList(item, "argumentos"),
                        requireText(item, "consulta"),
                        requireText(item, "esperado"),
                        Comparison.fromText(optionalText(item, "comparacao")),
                        requireText(item, "descricao"));
            }
            case "terraform_estado" -> {
                requireTerraform(terraformDirectory, tipo, file);
                yield new Assertion.TerraformState(
                        terraformDirectory,
                        requireText(item, "endereco"),
                        optionalText(item, "atributo"),
                        optionalText(item, "esperado"),
                        requireText(item, "descricao"));
            }
            case "terraform_plano_limpo" -> {
                requireTerraform(terraformDirectory, tipo, file);
                yield new Assertion.TerraformCleanPlan(
                        terraformDirectory,
                        requireText(item, "descricao"));
            }
            case "servico_systemd" -> {
                requireLinuxContainer(linuxContainer, tipo, file);
                yield new Assertion.SystemdService(
                        linuxContainer,
                        requireText(item, "nome"),
                        optionalBooleanOrNull(item, "ativo"),
                        optionalBooleanOrNull(item, "habilitado"),
                        requireText(item, "descricao"));
            }
            case "arquivo_linux" -> {
                requireLinuxContainer(linuxContainer, tipo, file);
                yield new Assertion.LinuxFile(
                        linuxContainer,
                        requireText(item, "caminho"),
                        optionalText(item, "modo"),
                        optionalText(item, "dono"),
                        optionalText(item, "grupo"),
                        requireText(item, "descricao"));
            }
            default -> throw new IllegalArgumentException(
                    "tipo de asserção desconhecido: " + tipo + " em " + file);
        };
    }

    private void requireTerraform(String terraformDirectory, String tipo, Path file) {
        if (terraformDirectory == null) {
            throw new IllegalArgumentException(
                    "a Asserção " + tipo + " exige terraform: true em " + file);
        }
    }

    private void requireLinuxContainer(String linuxContainer, String tipo, Path file) {
        if (linuxContainer == null || linuxContainer.isBlank()) {
            throw new IllegalArgumentException(
                    "a Asserção " + tipo + " exige containerLinux em " + file);
        }
    }

    private void requireMinistack(boolean ministack, String tipo, Path file) {
        if (!ministack) {
            throw new IllegalArgumentException(
                    "a Asserção " + tipo + " exige ministack: true em " + file);
        }
    }

    private void requireKubernetesMetadata(
            String context, String namespace, String tipo, Path file) {
        if (context == null || namespace == null) {
            throw new IllegalArgumentException(
                    "a Asserção " + tipo + " exige contextoKubernetes e "
                    + "namespaceKubernetes em " + file);
        }
    }

    private boolean requireBoolean(Map<String, Object> mapa, String chave) {
        Object value = mapa.get(chave);
        if (!(value instanceof Boolean booleano)) {
            throw new IllegalArgumentException("campo booleano obrigatório ausente: " + chave);
        }
        return booleano;
    }

    private boolean optionalBoolean(Map<String, Object> mapa, String chave) {
        Object value = mapa.get(chave);
        if (value == null) {
            return false;
        }
        if (!(value instanceof Boolean booleano)) {
            throw new IllegalArgumentException("campo deve ser booleano: " + chave);
        }
        return booleano;
    }

    private Boolean optionalBooleanOrNull(Map<String, Object> mapa, String chave) {
        Object value = mapa.get(chave);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Boolean booleano)) {
            throw new IllegalArgumentException("campo deve ser booleano: " + chave);
        }
        return booleano;
    }

    @SuppressWarnings("unchecked")
    private List<String> optionalList(Map<String, Object> mapa, String chave) {
        Object value = mapa.get(chave);
        return value == null ? List.of() : List.copyOf((List<String>) value);
    }

    @SuppressWarnings("unchecked")
    private List<String> requireList(Map<String, Object> mapa, String chave) {
        Object value = mapa.get(chave);
        if (value == null) {
            throw new IllegalArgumentException("campo obrigatório ausente: " + chave);
        }
        return List.copyOf((List<String>) value);
    }

    private String optionalText(Map<String, Object> mapa, String chave) {
        Object value = mapa.get(chave);
        return value == null ? null : value.toString();
    }

    private String requireText(Map<String, Object> mapa, String chave) {
        Object value = mapa.get(chave);
        if (value == null) {
            throw new IllegalArgumentException("campo obrigatório ausente: " + chave);
        }
        return value.toString();
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui ler " + path, e);
        }
    }
}
