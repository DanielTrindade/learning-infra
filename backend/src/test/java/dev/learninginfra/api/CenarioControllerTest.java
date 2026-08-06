package dev.learninginfra.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "learninginfra.diretorio-de-conteudo=src/test/resources/conteudo-fixture",
        "learninginfra.arquivo-de-progresso=target/progresso-de-teste.json"
})
class CenarioControllerTest {

    @Autowired
    WebApplicationContext contexto;

    MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void listaOsCenariosDisponiveis() throws Exception {
        mvc().perform(get("/api/cenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("docker/01-servir-html-nginx"))
                .andExpect(jsonPath("$[0].dificuldade").value("GUIADO"));
    }

    @Test
    void devolveODetalheComOMarkdown() throws Exception {
        mvc().perform(get("/api/cenarios/docker/01-servir-html-nginx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Servir um HTML seu com nginx"))
                .andExpect(jsonPath("$.markdown").isNotEmpty())
                .andExpect(jsonPath("$.asercoes.length()").value(3));
    }

    @Test
    void naoVazaAImplementacaoDaAsercao() throws Exception {
        mvc().perform(get("/api/cenarios/docker/01-servir-html-nginx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asercoes[0]").value("o container `lab-web` está rodando"));
    }

    @Test
    void devolve404ParaCenarioInexistente() throws Exception {
        mvc().perform(get("/api/cenarios/docker/nao-existe"))
                .andExpect(status().isNotFound());
    }
}
