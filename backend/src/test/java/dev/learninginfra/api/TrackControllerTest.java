package dev.learninginfra.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "learning-infra.content-directory=src/test/resources/conteudo-fixture",
        "learning-infra.progress-file=target/progresso-de-trilha-teste.json"
})
class TrackControllerTest {

    @Autowired
    WebApplicationContext context;

    @BeforeEach
    void limparProgresso() throws Exception {
        Files.deleteIfExists(Path.of("target/progresso-de-trilha-teste.json"));
    }

    MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void listaTrilhasComProgressoCalculadoNoBackend() throws Exception {
        mvc().perform(get("/api/tracks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("docker"))
                .andExpect(jsonPath("$[0].fundamentals.state").value("NOT_STARTED"))
                .andExpect(jsonPath("$[0].scenarios[0].id")
                        .value("docker/01-servir-html-nginx"))
                .andExpect(jsonPath("$[0].completed").value(0))
                .andExpect(jsonPath("$[0].total").value(2))
                .andExpect(jsonPath("$[0].score").value(0))
                .andExpect(jsonPath("$[0].allCompleted").value(false))
                .andExpect(jsonPath("$[1].id").value("kubernetes"))
                .andExpect(jsonPath("$[1].fundamentals").doesNotExist());
    }

    @Test
    void entregaArtigoEQuestoesSemVazarGabarito() throws Exception {
        mvc().perform(get("/api/tracks/docker/fundamentals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markdown").isNotEmpty())
                .andExpect(jsonPath("$.minimumScore").value(80))
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].correctOption").doesNotExist())
                .andExpect(jsonPath("$.questions[0].explanation").doesNotExist());
    }

    @Test
    void corrigeEDevolveFeedbackCompletoSomenteDepoisDaSubmissao() throws Exception {
        mvc().perform(post("/api/tracks/docker/questionnaire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers": {
                                  "imagem-ou-container": "imagem",
                                  "persistencia": "volume"
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(100))
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.attempts").value(1))
                .andExpect(jsonPath("$.feedback[0].correctOption").value("imagem"))
                .andExpect(jsonPath("$.feedback[0].explanation").isNotEmpty());

        mvc().perform(post("/api/tracks/docker/questionnaire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers": {
                                  "imagem-ou-container": "container",
                                  "persistencia": "camada"
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.bestScore").value(100))
                .andExpect(jsonPath("$.attempts").value(2));
    }

    @Test
    void traduzAusenciasEEntradaInvalidaParaOsStatusCorretos() throws Exception {
        mvc().perform(get("/api/tracks/inexistente/fundamentals"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                        "não encontrada")));
        mvc().perform(get("/api/tracks/kubernetes/fundamentals"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").isNotEmpty());
        mvc().perform(post("/api/tracks/docker/questionnaire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"respostas\": {\"imagem-ou-container\": \"imagem\"}}"))
                .andExpect(status().isBadRequest());
    }
}
