package dev.learninginfra.verificacao;

import dev.learninginfra.execucao.ExecutorDeComando;
import dev.learninginfra.execucao.SaidaDeComando;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class MotorDeVerificacao {

    private final ExecutorDeComando executor;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public MotorDeVerificacao(ExecutorDeComando executor) {
        this.executor = executor;
    }

    public ResultadoDaVerificacao verificar(List<Assercao> asercoes) {
        List<ResultadoDeAsercao> resultados = asercoes.stream().map(this::avaliar).toList();
        boolean concluido = resultados.stream().allMatch(ResultadoDeAsercao::passou);
        return new ResultadoDaVerificacao(concluido, resultados);
    }

    private ResultadoDeAsercao avaliar(Assercao asercao) {
        return switch (asercao) {
            case Assercao.ContainerRodando a -> avaliarContainer(a);
            case Assercao.HttpResponde a -> avaliarStatus(a);
            case Assercao.HttpCorpoContem a -> avaliarCorpo(a);
            case Assercao.ImagemExiste a -> avaliarImagem(a);
        };
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
        return buscar(a.url())
                .map(resposta -> resposta.statusCode() == a.status()
                        ? ResultadoDeAsercao.aprovada(a)
                        : ResultadoDeAsercao.reprovada(a, "respondeu " + resposta.statusCode()))
                .orElseGet(() -> ResultadoDeAsercao.reprovada(a, "nada respondeu em " + a.url()));
    }

    private ResultadoDeAsercao avaliarCorpo(Assercao.HttpCorpoContem a) {
        return buscar(a.url())
                .map(resposta -> resposta.body().contains(a.texto())
                        ? ResultadoDeAsercao.aprovada(a)
                        : ResultadoDeAsercao.reprovada(a, "respondeu, mas sem o texto esperado"))
                .orElseGet(() -> ResultadoDeAsercao.reprovada(a, "nada respondeu em " + a.url()));
    }

    private Optional<HttpResponse<String>> buscar(String url) {
        try {
            HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            return Optional.of(http.send(requisicao, HttpResponse.BodyHandlers.ofString()));
        } catch (IOException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}
