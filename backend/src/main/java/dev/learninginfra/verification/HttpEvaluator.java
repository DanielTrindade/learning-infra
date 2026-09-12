package dev.learninginfra.verification;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

/** Asserções que fazem uma requisição HTTP direta ao serviço do Cenário. */
class HttpEvaluator {

    private final VerificationContext context;

    HttpEvaluator(VerificationContext context) {
        this.context = context;
    }

    AssertionResult evaluateStatus(Assertion.HttpResponds a) {
        Attempt attempt = find(a.url());
        if (!attempt.success()) {
            return AssertionResult.rejected(a, attempt.failure());
        }
        return attempt.response().statusCode() == a.status()
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a, "respondeu " + attempt.response().statusCode());
    }

    AssertionResult evaluateBody(Assertion.HttpBodyContains a) {
        Attempt attempt = find(a.url());
        if (!attempt.success()) {
            return AssertionResult.rejected(a, attempt.failure());
        }
        return attempt.response().body().contains(a.text())
                ? AssertionResult.approved(a)
                : AssertionResult.rejected(a, "respondeu, mas sem o texto esperado");
    }

    private Attempt find(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(context.espera())
                    .GET()
                    .build();
            return new Attempt(
                    context.http().send(request, HttpResponse.BodyHandlers.ofString()), null);
        } catch (HttpTimeoutException e) {
            // Alguém escuta em url, mas não respondeu a tempo. Diagnóstico bem diferente
            // de "não há ninguém aí" — normalmente é uma dependência travada.
            return new Attempt(null,
                    "não respondeu em " + context.espera().toSeconds()
                    + "s — está lento ou travado");
        } catch (IOException e) {
            return new Attempt(null, "nada respondeu em " + url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Attempt(null, "verificação interrompida");
        }
    }

    /** Resposta obtida, ou a razão pela qual não veio — as duas são informação para o leitor. */
    private record Attempt(HttpResponse<String> response, String failure) {
        boolean success() {
            return response != null;
        }
    }
}
