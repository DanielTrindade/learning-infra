package dev.learninginfra.conteudo;

import dev.learninginfra.verificacao.Assercao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeitorDeCenarioTest {

    @TempDir
    Path diretorio;

    private void escreverCenarioCompleto() throws Exception {
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: docker/01-servir-html-nginx
                titulo: Servir um HTML seu com nginx
                dificuldade: guiado
                containers: [lab-web]
                ---
                # Servir um HTML seu com nginx

                Texto didático aqui.
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_rodando
                    nome: lab-web
                  - tipo: http_responde
                    url: http://localhost:8080
                    status: 200
                  - tipo: http_corpo_contem
                    url: http://localhost:8080
                    texto: Meu primeiro container
                """);
    }

    @Test
    void leMetadadosDoFrontmatter() throws Exception {
        escreverCenarioCompleto();

        Cenario cenario = new LeitorDeCenario().ler(diretorio);

        assertEquals("docker/01-servir-html-nginx", cenario.id());
        assertEquals("Servir um HTML seu com nginx", cenario.titulo());
        assertEquals(Dificuldade.GUIADO, cenario.dificuldade());
        assertEquals(List.of("lab-web"), cenario.containers());
    }

    @Test
    void separaOCorpoDoFrontmatter() throws Exception {
        escreverCenarioCompleto();

        Cenario cenario = new LeitorDeCenario().ler(diretorio);

        assertTrue(cenario.markdown().startsWith("# Servir um HTML seu com nginx"));
        assertFalse(cenario.markdown().contains("dificuldade:"));
    }

    @Test
    void leAsTresAsercoes() throws Exception {
        escreverCenarioCompleto();

        List<Assercao> asercoes = new LeitorDeCenario().ler(diretorio).asercoes();

        assertEquals(3, asercoes.size());
        assertEquals(new Assercao.ContainerRodando("lab-web"), asercoes.get(0));
        assertEquals(new Assercao.HttpResponde("http://localhost:8080", 200), asercoes.get(1));
        assertEquals(new Assercao.HttpCorpoContem("http://localhost:8080", "Meu primeiro container"),
                asercoes.get(2));
    }

    @Test
    void leOProjetoComposeQuandoDeclarado() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: docker/03
                titulo: Com compose
                dificuldade: guiado
                projetoCompose: lab-03
                ---
                # corpo
                """);

        assertEquals("lab-03", new LeitorDeCenario().ler(diretorio).projetoCompose());
    }

    @Test
    void projetoComposeEhNuloQuandoAusente() throws Exception {
        escreverCenarioCompleto();

        assertNull(new LeitorDeCenario().ler(diretorio).projetoCompose());
    }

    @Test
    void leAsercaoDeImagem() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: imagem_existe
                    referencia: lab-app:1.0
                """);

        List<Assercao> asercoes = new LeitorDeCenario().ler(diretorio).asercoes();

        assertEquals(List.of(new Assercao.ImagemExiste("lab-app:1.0")), asercoes);
    }

    @Test
    void leAsercoesDeVolumeEDeComando() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: volume_existe
                    nome: lab-05-dados
                  - tipo: comando_produz
                    descricao: o dado sobreviveu ao container
                    comando: ["docker", "run", "--rm", "alpine", "echo", "tamandua"]
                    contem: tamandua
                """);

        List<Assercao> asercoes = new LeitorDeCenario().ler(diretorio).asercoes();

        assertEquals(new Assercao.VolumeExiste("lab-05-dados"), asercoes.get(0));
        assertEquals(
                new Assercao.ComandoProduz(
                        List.of("docker", "run", "--rm", "alpine", "echo", "tamandua"),
                        "tamandua",
                        "o dado sobreviveu ao container"),
                asercoes.get(1));
    }

    @Test
    void leOsVolumesDeclarados() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: docker/05
                titulo: Com volume
                dificuldade: assistido
                volumes: [lab-05-dados]
                ---
                # corpo
                """);

        assertEquals(List.of("lab-05-dados"), new LeitorDeCenario().ler(diretorio).volumes());
    }

    @Test
    void recusaTipoDeAsercaoDesconhecido() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_falando_grego
                    nome: lab-web
                """);

        var erro = assertThrows(IllegalArgumentException.class, () -> new LeitorDeCenario().ler(diretorio));
        assertTrue(erro.getMessage().contains("container_falando_grego"));
    }
}
