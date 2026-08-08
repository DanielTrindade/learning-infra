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
        "learninginfra.diretorio-de-conteudo=src/test/resources/conteudo-fixture",
        "learninginfra.arquivo-de-progresso=target/progresso-de-trilha-teste.json"
})
class TrilhaControllerTest {

    @Autowired
    WebApplicationContext contexto;

    @BeforeEach
    void limparProgresso() throws Exception {
        Files.deleteIfExists(Path.of("target/progresso-de-trilha-teste.json"));
    }

    MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void listaTrilhasComProgressoCalculadoNoBackend() throws Exception {
        mvc().perform(get("/api/trilhas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("docker"))
                .andExpect(jsonPath("$[0].fundamentos.estado").value("NAO_INICIADO"))
                .andExpect(jsonPath("$[0].cenarios[0].id")
                        .value("docker/01-servir-html-nginx"))
                .andExpect(jsonPath("$[0].concluidos").value(0))
                .andExpect(jsonPath("$[0].total").value(2))
                .andExpect(jsonPath("$[0].percentual").value(0))
                .andExpect(jsonPath("$[0].concluida").value(false))
                .andExpect(jsonPath("$[1].id").value("kubernetes"))
                .andExpect(jsonPath("$[1].fundamentos").doesNotExist());
    }

    @Test
    void entregaArtigoEQuestoesSemVazarGabarito() throws Exception {
        mvc().perform(get("/api/trilhas/docker/fundamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markdown").isNotEmpty())
                .andExpect(jsonPath("$.aproveitamentoMinimo").value(80))
                .andExpect(jsonPath("$.questoes.length()").value(2))
                .andExpect(jsonPath("$.questoes[0].alternativaCorreta").doesNotExist())
                .andExpect(jsonPath("$.questoes[0].explicacao").doesNotExist());
    }

    @Test
    void corrigeEDevolveFeedbackCompletoSomenteDepoisDaSubmissao() throws Exception {
        mvc().perform(post("/api/trilhas/docker/questionario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"respostas": {
                                  "imagem-ou-container": "imagem",
                                  "persistencia": "volume"
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentual").value(100))
                .andExpect(jsonPath("$.aprovado").value(true))
                .andExpect(jsonPath("$.tentativas").value(1))
                .andExpect(jsonPath("$.feedback[0].alternativaCorreta").value("imagem"))
                .andExpect(jsonPath("$.feedback[0].explicacao").isNotEmpty());

        mvc().perform(post("/api/trilhas/docker/questionario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"respostas": {
                                  "imagem-ou-container": "container",
                                  "persistencia": "camada"
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentual").value(0))
                .andExpect(jsonPath("$.aprovado").value(false))
                .andExpect(jsonPath("$.melhorPercentual").value(100))
                .andExpect(jsonPath("$.tentativas").value(2));
    }

    @Test
    void traduzAusenciasEEntradaInvalidaParaOsStatusCorretos() throws Exception {
        mvc().perform(get("/api/trilhas/inexistente/fundamentos"))
                .andExpect(status().isNotFound());
        mvc().perform(get("/api/trilhas/kubernetes/fundamentos"))
                .andExpect(status().isNotFound());
        mvc().perform(post("/api/trilhas/docker/questionario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"respostas\": {\"imagem-ou-container\": \"imagem\"}}"))
                .andExpect(status().isBadRequest());
    }
}
