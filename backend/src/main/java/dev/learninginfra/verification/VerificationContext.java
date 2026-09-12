package dev.learninginfra.verification;

import dev.learninginfra.execution.CommandExecutor;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;

/**
 * O que todo avaliador precisa: acesso ao executor de processos, à espera configurada,
 * ao cliente HTTP e à raiz que confina o Terraform.
 */
record VerificationContext(
        CommandExecutor executor,
        Duration espera,
        HttpClient http,
        Path raizDeTrabalho) {
}
