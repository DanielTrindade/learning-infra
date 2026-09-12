package dev.learninginfra.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.io.UncheckedIOException;

/**
 * Traduz exceções para RFC 7807 com `detail` legível. As mensagens do
 * {@code GerenciadorDeCenarioAtivo} explicam exatamente o que fazer quando o Iniciar
 * falha; sem este tratador, o corpo padrão do Spring as descartaria.
 */
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> erroDeStatus(ResponseStatusException error) {
        String detail = error.getReason() == null
                ? error.getStatusCode().toString()
                : error.getReason();
        return ResponseEntity.status(error.getStatusCode())
                .body(problema(error.getStatusCode(), detail));
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail erroDeEstado(IllegalStateException error) {
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, error.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail argumentoInvalido(IllegalArgumentException error) {
        return problema(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler(UncheckedIOException.class)
    ProblemDetail erroDeIo(UncheckedIOException error) {
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, error.getMessage());
    }

    private ProblemDetail problema(org.springframework.http.HttpStatusCode status, String detail) {
        ProblemDetail problema = ProblemDetail.forStatus(status);
        problema.setDetail(detail == null ? status.toString() : detail);
        return problema;
    }
}
