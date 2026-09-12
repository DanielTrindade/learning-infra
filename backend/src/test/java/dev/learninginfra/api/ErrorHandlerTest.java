package dev.learninginfra.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlerTest {

    private final ErrorHandler tratador = new ErrorHandler();

    @Test
    void erroDeEstadoCarregaODetalhe() {
        ProblemDetail problema = tratador.erroDeEstado(
                new IllegalStateException("não consegui subir o Compose do Cenário"));

        assertEquals(500, problema.getStatus());
        assertEquals("não consegui subir o Compose do Cenário", problema.getDetail());
    }

    @Test
    void argumentoInvalidoVira400ComDetalhe() {
        ProblemDetail problema = tratador.argumentoInvalido(
                new IllegalArgumentException("a submissão deve conter todas as questões"));

        assertEquals(400, problema.getStatus());
        assertEquals("a submissão deve conter todas as questões", problema.getDetail());
    }

    @Test
    void erroDeIoVira500ComDetalhe() {
        ProblemDetail problema = tratador.erroDeIo(
                new UncheckedIOException("não consegui ler o conteúdo", new IOException()));

        assertEquals(500, problema.getStatus());
        assertEquals("não consegui ler o conteúdo", problema.getDetail());
    }

    @Test
    void erroDeStatusPreservaCodigoERazao() {
        var response = tratador.erroDeStatus(new ResponseStatusException(
                HttpStatus.CONFLICT, "o Cenário ativo é outro"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("o Cenário ativo é outro", response.getBody().getDetail());
    }
}
