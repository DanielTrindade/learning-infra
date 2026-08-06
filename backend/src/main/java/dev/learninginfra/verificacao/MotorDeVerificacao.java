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

@Service
public class MotorDeVerificacao {

    /**
     * Generoso de propósito. Um serviço do Cenário pode depender de outro que está
     * morrendo, e aí ele responde só depois do próprio timeout interno — cinco segundos
     * é comum. Um limite apertado reprovaria como "morto" um serviço que está de pé.
     */
    private static final Duration ESPERA_PADRAO = Duration.ofSeconds(10);

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
            case Assercao.HttpResponde a -> avaliarStatus(a);
            case Assercao.HttpCorpoContem a -> avaliarCorpo(a);
            case Assercao.ImagemExiste a -> avaliarImagem(a);
            case Assercao.VolumeExiste a -> avaliarVolume(a);
            case Assercao.ComandoProduz a -> avaliarComando(a);
        };
    }

    private ResultadoDeAsercao avaliarVolume(Assercao.VolumeExiste a) {
        SaidaDeComando saida = executor.executar(List.of("docker", "volume", "inspect", a.nome()));
        return saida.sucesso()
                ? ResultadoDeAsercao.aprovada(a)
                : ResultadoDeAsercao.reprovada(a, "o volume `" + a.nome() + "` não existe");
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
