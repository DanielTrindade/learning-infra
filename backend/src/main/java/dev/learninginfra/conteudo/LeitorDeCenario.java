package dev.learninginfra.conteudo;

import dev.learninginfra.verificacao.Assercao;
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
public class LeitorDeCenario {

    private static final String DELIMITADOR = "---";
    private static final String ENDPOINT_AWS_LOCAL = "http://127.0.0.1:4566";
    private static final String REGIAO_AWS_LOCAL = "us-east-1";

    public Cenario ler(Path diretorioDoCenario) {
        String bruto = lerArquivo(diretorioDoCenario.resolve("cenario.md"));
        String[] partes = separarFrontmatter(bruto, diretorioDoCenario);

        Map<String, Object> meta = new Yaml().load(partes[0]);
        String corpo = partes[1];

        String contextoKubernetes = textoOpcional(meta, "contextoKubernetes");
        String namespaceKubernetes = textoOpcional(meta, "namespaceKubernetes");
        String manifestosIniciais = textoOpcional(meta, "manifestosIniciais");
        validarMetadadosKubernetes(
                contextoKubernetes, namespaceKubernetes, manifestosIniciais, diretorioDoCenario);
        boolean ministack = booleanoOpcional(meta, "ministack");
        boolean infraestruturaRealAws = booleanoOpcional(meta, "infraestruturaRealAws");
        String inicializacaoAws = textoOpcional(meta, "inicializacaoAws");
        validarMetadadosAws(
                ministack, infraestruturaRealAws, inicializacaoAws, diretorioDoCenario);
        List<Assercao> asercoes = lerAsercoes(
                diretorioDoCenario.resolve("verificacao.yaml"),
                contextoKubernetes,
                namespaceKubernetes,
                ministack);

        return new Cenario(
                exigirTexto(meta, "id"),
                exigirTexto(meta, "titulo"),
                Dificuldade.deTexto(exigirTexto(meta, "dificuldade")),
                lerListaOpcional(meta, "containers"),
                corpo,
                diretorioDoCenario,
                asercoes,
                textoOpcional(meta, "projetoCompose"),
                lerListaOpcional(meta, "volumes"),
                contextoKubernetes,
                namespaceKubernetes,
                manifestosIniciais,
                ministack,
                infraestruturaRealAws,
                inicializacaoAws);
    }

    private void validarMetadadosAws(
            boolean ministack, boolean infraestruturaReal, String inicializacao, Path diretorio) {
        if (!ministack && (infraestruturaReal || inicializacao != null)) {
            throw new IllegalArgumentException(
                    "infraestruturaRealAws e inicializacaoAws exigem ministack: true em " + diretorio);
        }
    }

    private void validarMetadadosKubernetes(
            String contexto, String namespace, String manifestos, Path diretorio) {
        boolean temContexto = contexto != null && !contexto.isBlank();
        boolean temNamespace = namespace != null && !namespace.isBlank();
        if (temContexto != temNamespace) {
            throw new IllegalArgumentException(
                    "contextoKubernetes e namespaceKubernetes devem aparecer juntos em " + diretorio);
        }
        if (manifestos != null && !manifestos.isBlank() && !temContexto) {
            throw new IllegalArgumentException(
                    "manifestosIniciais exige contextoKubernetes e namespaceKubernetes em " + diretorio);
        }
    }

    private String[] separarFrontmatter(String bruto, Path diretorio) {
        String normalizado = bruto.replace("\r\n", "\n").stripLeading();
        if (!normalizado.startsWith(DELIMITADOR + "\n")) {
            throw new IllegalArgumentException("cenario.md sem frontmatter em " + diretorio);
        }
        int fim = normalizado.indexOf("\n" + DELIMITADOR, DELIMITADOR.length());
        if (fim < 0) {
            throw new IllegalArgumentException("frontmatter não fechado em " + diretorio);
        }
        String frontmatter = normalizado.substring(DELIMITADOR.length() + 1, fim);
        String corpo = normalizado.substring(fim + 1 + DELIMITADOR.length()).stripLeading();
        return new String[]{frontmatter, corpo};
    }

    @SuppressWarnings("unchecked")
    private List<Assercao> lerAsercoes(
            Path arquivo, String contextoKubernetes, String namespaceKubernetes, boolean ministack) {
        Map<String, Object> raiz = new Yaml().load(lerArquivo(arquivo));
        List<Map<String, Object>> itens = (List<Map<String, Object>>) raiz.get("asercoes");
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("verificacao.yaml sem asserções: " + arquivo);
        }
        List<Assercao> asercoes = new ArrayList<>();
        for (Map<String, Object> item : itens) {
            asercoes.add(montarAsercao(
                    item, arquivo, contextoKubernetes, namespaceKubernetes, ministack));
        }
        return List.copyOf(asercoes);
    }

    private Assercao montarAsercao(
            Map<String, Object> item,
            Path arquivo,
            String contextoKubernetes,
            String namespaceKubernetes,
            boolean ministack) {
        String tipo = exigirTexto(item, "tipo");
        return switch (tipo) {
            case "container_rodando" -> new Assercao.ContainerRodando(exigirTexto(item, "nome"));
            case "http_responde" -> new Assercao.HttpResponde(
                    exigirTexto(item, "url"), (Integer) item.getOrDefault("status", 200));
            case "http_corpo_contem" -> new Assercao.HttpCorpoContem(
                    exigirTexto(item, "url"), exigirTexto(item, "texto"));
            case "imagem_existe" -> new Assercao.ImagemExiste(exigirTexto(item, "referencia"));
            case "volume_existe" -> new Assercao.VolumeExiste(exigirTexto(item, "nome"));
            case "container_saudavel" -> new Assercao.ContainerSaudavel(exigirTexto(item, "nome"));
            case "container_em_rede" -> new Assercao.ContainerEmRede(
                    exigirTexto(item, "nome"),
                    exigirTexto(item, "rede"),
                    exigirBooleano(item, "presente"));
            case "container_configuracao" -> new Assercao.ContainerConfiguracao(
                    exigirTexto(item, "nome"),
                    textoOpcional(item, "usuario"),
                    (Boolean) item.getOrDefault("somenteLeitura", null),
                    lerListaOpcional(item, "capabilitiesRemovidas"));
            case "comando_produz" -> new Assercao.ComandoProduz(
                    lerLista(item, "comando"),
                    exigirTexto(item, "contem"),
                    exigirTexto(item, "descricao"));
            case "kubernetes_condicao" -> {
                exigirMetadadosKubernetes(contextoKubernetes, namespaceKubernetes, tipo, arquivo);
                yield new Assercao.KubernetesCondicao(
                        contextoKubernetes,
                        namespaceKubernetes,
                        exigirTexto(item, "recurso"),
                        exigirTexto(item, "nome"),
                        exigirTexto(item, "condicao"),
                        item.getOrDefault("status", "True").toString(),
                        (Integer) item.getOrDefault("timeout", 10),
                        exigirTexto(item, "descricao"));
            }
            case "kubernetes_jsonpath" -> {
                exigirMetadadosKubernetes(contextoKubernetes, namespaceKubernetes, tipo, arquivo);
                yield new Assercao.KubernetesJsonpath(
                        contextoKubernetes,
                        namespaceKubernetes,
                        exigirTexto(item, "recurso"),
                        exigirTexto(item, "nome"),
                        exigirTexto(item, "expressao"),
                        exigirTexto(item, "contem"),
                        (Integer) item.getOrDefault("timeout", 10),
                        exigirTexto(item, "descricao"));
            }
            case "kubernetes_rbac" -> {
                exigirMetadadosKubernetes(contextoKubernetes, namespaceKubernetes, tipo, arquivo);
                yield new Assercao.KubernetesRbac(
                        contextoKubernetes,
                        namespaceKubernetes,
                        exigirTexto(item, "serviceAccount"),
                        exigirTexto(item, "verbo"),
                        exigirTexto(item, "recurso"),
                        exigirBooleano(item, "permitido"),
                        exigirTexto(item, "descricao"));
            }
            case "aws_consulta" -> {
                exigirMinistack(ministack, tipo, arquivo);
                yield new Assercao.AwsConsulta(
                        ENDPOINT_AWS_LOCAL,
                        REGIAO_AWS_LOCAL,
                        exigirTexto(item, "servico"),
                        exigirTexto(item, "operacao"),
                        lerListaOpcional(item, "argumentos"),
                        exigirTexto(item, "consulta"),
                        exigirTexto(item, "esperado"),
                        exigirTexto(item, "descricao"));
            }
            default -> throw new IllegalArgumentException(
                    "tipo de asserção desconhecido: " + tipo + " em " + arquivo);
        };
    }

    private void exigirMinistack(boolean ministack, String tipo, Path arquivo) {
        if (!ministack) {
            throw new IllegalArgumentException(
                    "a Asserção " + tipo + " exige ministack: true em " + arquivo);
        }
    }

    private void exigirMetadadosKubernetes(
            String contexto, String namespace, String tipo, Path arquivo) {
        if (contexto == null || namespace == null) {
            throw new IllegalArgumentException(
                    "a Asserção " + tipo + " exige contextoKubernetes e "
                    + "namespaceKubernetes em " + arquivo);
        }
    }

    private boolean exigirBooleano(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        if (!(valor instanceof Boolean booleano)) {
            throw new IllegalArgumentException("campo booleano obrigatório ausente: " + chave);
        }
        return booleano;
    }

    private boolean booleanoOpcional(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        if (valor == null) {
            return false;
        }
        if (!(valor instanceof Boolean booleano)) {
            throw new IllegalArgumentException("campo deve ser booleano: " + chave);
        }
        return booleano;
    }

    @SuppressWarnings("unchecked")
    private List<String> lerListaOpcional(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        return valor == null ? List.of() : List.copyOf((List<String>) valor);
    }

    @SuppressWarnings("unchecked")
    private List<String> lerLista(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        if (valor == null) {
            throw new IllegalArgumentException("campo obrigatório ausente: " + chave);
        }
        return List.copyOf((List<String>) valor);
    }

    private String textoOpcional(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        return valor == null ? null : valor.toString();
    }

    private String exigirTexto(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        if (valor == null) {
            throw new IllegalArgumentException("campo obrigatório ausente: " + chave);
        }
        return valor.toString();
    }

    private String lerArquivo(Path caminho) {
        try {
            return Files.readString(caminho);
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui ler " + caminho, e);
        }
    }
}
