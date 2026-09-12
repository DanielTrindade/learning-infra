package dev.learninginfra.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "learning-infra.content-directory=src/test/resources/conteudo-fixture",
        "learning-infra.progress-file=target/progresso-de-teste.json"
})
class ScenarioControllerTest {

    @Autowired
    WebApplicationContext context;

    @BeforeEach
    void limparProgresso() throws Exception {
        Files.deleteIfExists(Path.of("target/progresso-de-teste.json"));
    }

    MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void listaOsCenariosDisponiveis() throws Exception {
        mvc().perform(get("/api/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("docker/01-servir-html-nginx"))
                .andExpect(jsonPath("$[0].difficulty").value("GUIDED"));
    }

    @Test
    void devolveODetalheComOMarkdown() throws Exception {
        mvc().perform(get("/api/scenarios/docker/01-servir-html-nginx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Servir um HTML seu com nginx"))
                .andExpect(jsonPath("$.markdown").isNotEmpty())
                .andExpect(jsonPath("$.assertions.length()").value(3));
    }

    @Test
    void naoVazaAImplementacaoDaAsercao() throws Exception {
        mvc().perform(get("/api/scenarios/docker/01-servir-html-nginx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assertions[0]").value("o container `lab-web` está rodando"));
    }

    @Test
    void devolve404ParaCenarioInexistente() throws Exception {
        mvc().perform(get("/api/scenarios/docker/nao-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void verificarSemCenarioAtivoDevolve409() throws Exception {
        mvc().perform(post("/api/scenarios/docker/01-servir-html-nginx/verify"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                        "Iniciar cenário antes de verificar")));
    }

    @Test
    void verificarCenarioAtivoExecutaAsAssercoes() throws Exception {
        Path progress = Path.of("target/progresso-de-teste.json");
        Files.createDirectories(progress.getParent());
        Files.writeString(progress, """
                {"cenarioAtivo": "docker/01-servir-html-nginx", "concluidos": {}}
                """);

        mvc().perform(post("/api/scenarios/docker/01-servir-html-nginx/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assertions.length()").value(3));
    }
}
