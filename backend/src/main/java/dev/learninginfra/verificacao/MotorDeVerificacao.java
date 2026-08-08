package dev.learninginfra.verificacao;

import dev.learninginfra.execucao.ExecutorDeComando;
import dev.learninginfra.execucao.SaidaDeComando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class MotorDeVerificacao {

    /**
     * Generoso de propósito. Um serviço do Cenário pode depender de outro que está
     * morrendo, e aí ele responde só depois do próprio timeout interno — cinco segundos
     * é comum. Um limite apertado reprovaria como "morto" um serviço que está de pé.
     */
    private static final Duration ESPERA_PADRAO = Duration.ofSeconds(10);
    private static final Map<String, String> AMBIENTE_AWS_LOCAL = Map.of(
            "AWS_ACCESS_KEY_ID", "000000000000",
            "AWS_SECRET_ACCESS_KEY", "test",
            "AWS_SESSION_TOKEN", "",
            "AWS_EC2_METADATA_DISABLED", "true");

    private final ExecutorDeComando executor;
    private final Duration espera;
    private final HttpClient http;

    /**
     * O {@code @Autowired} é obrigatório: com mais de um construtor, o Spring não elege
     * nenhum sozinho e procura um construtor sem argumentos, que não existe.
     */
    @Autowired
    public MotorDeVerificacao(ExecutorDeComando executor) {
        this(executor, ESPERA_PADRAO);
    }

    /** Só para teste: permite uma espera curta sem deixar a suíte lenta. */
    MotorDeVerificacao(ExecutorDeComando executor, Duration espera) {
        this.executor = executor;
        this.espera = espera;
        this.http = HttpClient.newBuilder().connectTimeout(espera).build();
    }

    public ResultadoDaVerificacao verificar(List<Assercao> asercoes) {
        List<ResultadoDeAsercao> resultados = asercoes.stream().map(this::avaliar).toList();
        boolean concluido = resultados.stream().allMatch(ResultadoDeAsercao::passou);
        return new ResultadoDaVerificacao(concluido, resultados);
    }

    private ResultadoDeAsercao avaliar(Assercao asercao) {
        return switch (asercao) {
            case Assercao.ContainerRodando a -> avaliarContainer(a);
            case Assercao.ContainerSaudavel a -> avaliarSaude(a);
            case Assercao.ContainerEmRede a -> avaliarEmRede(a);
            case Assercao.HttpResponde a -> avaliarStatus(a);
            case Assercao.HttpCorpoContem a -> avaliarCorpo(a);
            case Assercao.ImagemExiste a -> avaliarImagem(a);
            case Assercao.VolumeExiste a -> avaliarVolume(a);
            case Assercao.ComandoProduz a -> avaliarComando(a);
            case Assercao.KubernetesCondicao a -> avaliarCondicaoKubernetes(a);
            case Assercao.KubernetesJsonpath a -> avaliarJsonpathKubernetes(a);
            case Assercao.KubernetesRbac a -> avaliarRbacKubernetes(a);
            case Assercao.AwsConsulta a -> avaliarConsultaAws(a);
        };
    }

    private ResultadoDeAsercao avaliarConsultaAws(Assercao.AwsConsulta a) {
        List<String> comando = new java.util.ArrayList<>(List.of(
                "aws", "--endpoint-url", a.endpoint(), "--region", a.regiao(),
                "--no-cli-pager", a.servico(), a.operacao()));
        comando.addAll(a.argumentos());
        comando.addAll(List.of("--query", a.consulta(), "--output", "text"));

        SaidaDeComando saida = executor.executar(
                List.copyOf(comando), AMBIENTE_AWS_LOCAL);
        if (!saida.sucesso()) {
            return ResultadoDeAsercao.reprovada(a,
                    "não consegui consultar o MiniStack: " + ultimoDetalhe(saida));
        }
        return saida.stdout().strip().contains(a.esperado())
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a,
                        "valor observado: `" + saida.stdout().strip() + "`");
    }

    private ResultadoDeAsercao avaliarCondicaoKubernetes(Assercao.KubernetesCondicao a) {
        SaidaDeComando saida = executor.executar(List.of(
                "kubectl", "--context", a.contexto(), "--namespace", a.namespace(),
                "wait", a.recurso() + "/" + a.nome(),
                "--for=condition=" + a.condicao() + "=" + a.status(),
                "--timeout=" + a.timeoutSegundos() + "s"));
        return saida.sucesso()
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a,
                        "a Condition não convergiu: " + ultimoDetalhe(saida));
    }

    private ResultadoDeAsercao avaliarJsonpathKubernetes(Assercao.KubernetesJsonpath a) {
        SaidaDeComando saida = executor.executar(List.of(
                "kubectl", "--context", a.contexto(), "--namespace", a.namespace(),
                "wait", a.recurso() + "/" + a.nome(),
                "--for=jsonpath=" + a.expressao() + "=" + a.contem(),
                "--timeout=" + a.timeoutSegundos() + "s"));
        return saida.sucesso()
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a,
                        "a propriedade observada não convergiu: " + ultimoDetalhe(saida));
    }

    private ResultadoDeAsercao avaliarRbacKubernetes(Assercao.KubernetesRbac a) {
        String identidade = "system:serviceaccount:" + a.namespace() + ":" + a.serviceAccount();
        SaidaDeComando saida = executor.executar(List.of(
                "kubectl", "--context", a.contexto(), "auth", "can-i",
                a.verbo(), a.recurso(), "--as=" + identidade, "--namespace", a.namespace()));
        String resposta = saida.stdout().strip().toLowerCase();
        if (!resposta.equals("yes") && !resposta.equals("no")) {
            return ResultadoDeAsercao.reprovada(a,
                    "não consegui consultar a autorização: " + ultimoDetalhe(saida));
        }
        boolean observado = resposta.equals("yes");
        return observado == a.permitido()
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a,
                        observado ? "a identidade tem permissão além do necessário"
                                  : "a identidade ainda não recebeu a permissão necessária");
    }

    private ResultadoDeAsercao avaliarVolume(Assercao.VolumeExiste a) {
        SaidaDeComando saida = executor.executar(List.of("docker", "volume", "inspect", a.nome()));
        return saida.sucesso()
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a, "o volume `" + a.nome() + "` não existe");
    }

    private ResultadoDeAsercao avaliarSaude(Assercao.ContainerSaudavel a) {
        SaidaDeComando saida = executor.executar(List.of(
                "docker", "inspect", "-f", "{{.State.Health.Status}}", a.nome()));
        if (!saida.sucesso()) {
            return ResultadoDeAsercao.reprovada(a, "o container `" + a.nome() + "` não existe");
        }
        String status = saida.stdout().strip();
        return status.equals("healthy")
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a, "não está saudável — status atual: `" + status + "`");
    }

    private ResultadoDeAsercao avaliarEmRede(Assercao.ContainerEmRede a) {
        SaidaDeComando saida = executor.executar(List.of(
                "docker", "inspect", "-f",
                "{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}", a.nome()));
        if (!saida.sucesso()) {
            return ResultadoDeAsercao.reprovada(a, "o container `" + a.nome() + "` não existe");
        }
        boolean pertence = java.util.Arrays.stream(saida.stdout().strip().split("\\s+"))
                .anyMatch(rede -> rede.equals(a.rede()));
        boolean ok = pertence == a.presente();
        return ok
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a,
                        pertence
                                ? "está na rede `" + a.rede() + "` e não deveria"
                                : "não está na rede `" + a.rede() + "`");
    }

    private ResultadoDeAsercao avaliarComando(Assercao.ComandoProduz a) {
        SaidaDeComando saida = executor.executar(a.comando());
        if (!saida.sucesso()) {
            return ResultadoDeAsercao.reprovada(a,
                    "a checagem não completou: " + ultimaLinha(saida.stderr()));
        }
        return saida.stdout().contains(a.contem())
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a, "rodou, mas a saída veio sem o texto esperado");
    }

    private ResultadoDeAsercao avaliarImagem(Assercao.ImagemExiste a) {
        SaidaDeComando saida = executor.executar(
                List.of("docker", "image", "inspect", a.referencia()));
        return saida.sucesso()
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a,
                        "a imagem `" + a.referencia() + "` não foi construída ainda");
    }

    private ResultadoDeAsercao avaliarContainer(Assercao.ContainerRodando a) {
        SaidaDeComando saida = executor.executar(
                List.of("docker", "inspect", "-f", "{{.State.Running}}", a.nome()));
        if (!saida.sucesso()) {
            return ResultadoDeAsercao.reprovada(a, "o container `" + a.nome() + "` não existe");
        }
        boolean rodando = saida.stdout().trim().equals("true");
        return rodando
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a, "existe, mas está parado");
    }

    private ResultadoDeAsercao avaliarStatus(Assercao.HttpResponde a) {
        Tentativa tentativa = buscar(a.url());
        if (!tentativa.sucesso()) {
            return ResultadoDeAsercao.reprovada(a, tentativa.falha());
        }
        return tentativa.resposta().statusCode() == a.status()
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a, "respondeu " + tentativa.resposta().statusCode());
    }

    private ResultadoDeAsercao avaliarCorpo(Assercao.HttpCorpoContem a) {
        Tentativa tentativa = buscar(a.url());
        if (!tentativa.sucesso()) {
            return ResultadoDeAsercao.reprovada(a, tentativa.falha());
        }
        return tentativa.resposta().body().contains(a.texto())
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a, "respondeu, mas sem o texto esperado");
    }

    /**
     * O erro que interessa é a última linha do stderr. O {@code docker} escreve
     * progresso de download de imagem ali também, e sem isto o motivo real da falha
     * aparece afogado sob quatro linhas de "Pulling from library/alpine".
     */
    private String ultimaLinha(String texto) {
        String[] linhas = texto.strip().split("\\R");
        for (int i = linhas.length - 1; i >= 0; i--) {
            if (!linhas[i].isBlank()) {
                return linhas[i].strip();
            }
        }
        return "sem detalhe";
    }

    private String ultimoDetalhe(SaidaDeComando saida) {
        return ultimaLinha(saida.stderr().isBlank() ? saida.stdout() : saida.stderr());
    }

    /** Resposta obtida, ou a razão pela qual não veio — as duas são informação para o leitor. */
    private record Tentativa(HttpResponse<String> resposta, String falha) {
        boolean sucesso() {
            return resposta != null;
        }
    }

    private Tentativa buscar(String url) {
        try {
            HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url))
                    .timeout(espera)
                    .GET()
                    .build();
            return new Tentativa(http.send(requisicao, HttpResponse.BodyHandlers.ofString()), null);
        } catch (HttpTimeoutException e) {
            // Alguém escuta em url, mas não respondeu a tempo. Diagnóstico bem diferente
            // de "não há ninguém aí" — normalmente é uma dependência travada.
            return new Tentativa(null,
                    "não respondeu em " + espera.toSeconds() + "s — está lento ou travado");
        } catch (IOException e) {
            return new Tentativa(null, "nada respondeu em " + url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Tentativa(null, "verificação interrompida");
        }
    }
}
