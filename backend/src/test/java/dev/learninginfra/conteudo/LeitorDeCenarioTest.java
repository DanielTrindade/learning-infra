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
    void leAsercoesDeSaudeEDeRede() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_saudavel
                    nome: lab-08-db-1
                  - tipo: container_em_rede
                    nome: lab-07-db-1
                    rede: lab-07-interna
                    presente: true
                """);

        List<Assercao> asercoes = new LeitorDeCenario().ler(diretorio).asercoes();

        assertEquals(new Assercao.ContainerSaudavel("lab-08-db-1"), asercoes.get(0));
        assertEquals(
                new Assercao.ContainerEmRede("lab-07-db-1", "lab-07-interna", true),
                asercoes.get(1));
    }

    @Test
    void leContainerConfiguracaoComCamposOpcionais() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_configuracao
                    nome: lab-09-app-1
                    usuario: node
                    somenteLeitura: true
                    capabilitiesRemovidas: [ALL]
                """);

        var asercao = new LeitorDeCenario().ler(diretorio).asercoes().getFirst();

        assertEquals(
                new Assercao.ContainerConfiguracao(
                        "lab-09-app-1", "node", true, java.util.List.of("ALL")),
                asercao);
    }

    @Test
    void leImagemNoRegistryComDescricaoDoCenario() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: imagem_no_registry
                    referencia: localhost:5000/lab-10-app:1.0
                    descricao: a v1.0 está publicada no registry
                """);

        var asercao = new LeitorDeCenario().ler(diretorio).asercoes().getFirst();

        assertEquals(
                new Assercao.ImagemNoRegistry("localhost:5000/lab-10-app:1.0",
                        "a v1.0 está publicada no registry"),
                asercao);
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
    void leMetadadosDeKubernetes() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: kubernetes/06-probes
                titulo: Uma aplicação viva mas indisponível
                dificuldade: mestre
                contextoKubernetes: docker-desktop
                namespaceKubernetes: learning-infra-k8s-06
                manifestosIniciais: setup
                ---
                # corpo
                """);

        Cenario cenario = new LeitorDeCenario().ler(diretorio);

        assertEquals("docker-desktop", cenario.contextoKubernetes());
        assertEquals("learning-infra-k8s-06", cenario.namespaceKubernetes());
        assertEquals("setup", cenario.manifestosIniciais());
        assertTrue(cenario.usaKubernetes());
        assertTrue(cenario.temManifestosIniciais());
    }

    @Test
    void recusaNamespaceKubernetesSemContexto() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: kubernetes/01
                titulo: Incompleto
                dificuldade: guiado
                namespaceKubernetes: learning-infra-k8s-01
                ---
                # corpo
                """);

        var erro = assertThrows(
                IllegalArgumentException.class, () -> new LeitorDeCenario().ler(diretorio));

        assertTrue(erro.getMessage().contains("devem aparecer juntos"));
    }

    @Test
    void leAsercoesTipadasDeKubernetesComContextoDoCenario() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: kubernetes/10-rbac
                titulo: RBAC
                dificuldade: assistido
                contextoKubernetes: docker-desktop
                namespaceKubernetes: li-k8s-10
                ---
                # corpo
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: kubernetes_condicao
                    recurso: deployment
                    nome: web
                    condicao: Available
                    timeout: 12
                    descricao: o Deployment está disponível
                  - tipo: kubernetes_jsonpath
                    recurso: pod
                    nome: web
                    expressao: "{.status.phase}"
                    contem: Running
                    descricao: o Pod está rodando
                  - tipo: kubernetes_rbac
                    serviceAccount: auditor
                    verbo: delete
                    recurso: pods
                    permitido: false
                    descricao: o auditor não apaga Pods
                """);

        List<Assercao> asercoes = new LeitorDeCenario().ler(diretorio).asercoes();

        assertEquals(3, asercoes.size());
        assertEquals("docker-desktop", ((Assercao.KubernetesCondicao) asercoes.get(0)).contexto());
        assertEquals(12, ((Assercao.KubernetesCondicao) asercoes.get(0)).timeoutSegundos());
        assertEquals(10, ((Assercao.KubernetesJsonpath) asercoes.get(1)).timeoutSegundos());
        assertFalse(((Assercao.KubernetesRbac) asercoes.get(2)).permitido());
    }

    @Test
    void leMetadadosEAsercaoDeAwsSemAceitarEndpointDoConteudo() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: aws/02-s3
                titulo: S3
                dificuldade: guiado
                ministack: true
                infraestruturaRealAws: true
                inicializacaoAws: init
                ---
                # corpo
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: aws_consulta
                    servico: s3api
                    operacao: get-bucket-versioning
                    argumentos: ["--bucket", "learning-infra-arquivos"]
                    consulta: Status
                    esperado: Enabled
                    descricao: o bucket mantém histórico de versões
                """);

        Cenario cenario = new LeitorDeCenario().ler(diretorio);
        var asercao = (Assercao.AwsConsulta) cenario.asercoes().getFirst();

        assertTrue(cenario.usaAws());
        assertTrue(cenario.infraestruturaRealAws());
        assertEquals("init", cenario.inicializacaoAws());
        assertEquals("http://127.0.0.1:4566", asercao.endpoint());
        assertEquals("us-east-1", asercao.regiao());
        assertEquals(List.of("--bucket", "learning-infra-arquivos"), asercao.argumentos());
    }

    @Test
    void recusaAsercaoAwsForaDeUmCenarioMinistack() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: aws_consulta
                    servico: s3api
                    operacao: list-buckets
                    consulta: "Buckets[0].Name"
                    esperado: arquivos
                    descricao: existe um bucket
                """);

        var erro = assertThrows(
                IllegalArgumentException.class, () -> new LeitorDeCenario().ler(diretorio));

        assertTrue(erro.getMessage().contains("ministack: true"));
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

    @Test
    void cenarioSemTerraformNaoDeclaraDiretorio() throws Exception {
        escreverCenarioCompleto();

        Cenario cenario = new LeitorDeCenario().ler(diretorio);

        assertFalse(cenario.terraform());
        assertNull(cenario.diretorioTerraform());
    }

    @Test
    void terraformSemDiretorioUsaARaizDoWorkspace() throws Exception {
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: iac/01-primeiro-apply
                titulo: Primeiro apply
                dificuldade: guiado
                terraform: true
                ---
                # Primeiro apply
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_rodando
                    nome: mirante-web
                """);

        Cenario cenario = new LeitorDeCenario().ler(diretorio);

        assertTrue(cenario.terraform());
        assertEquals(".", cenario.diretorioTerraform());
    }

    @Test
    void diretorioTerraformExigeTerraformLigado() throws Exception {
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: iac/01-primeiro-apply
                titulo: Primeiro apply
                dificuldade: guiado
                diretorioTerraform: infra
                ---
                # Primeiro apply
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_rodando
                    nome: mirante-web
                """);

        var erro = assertThrows(
                IllegalArgumentException.class, () -> new LeitorDeCenario().ler(diretorio));

        assertTrue(erro.getMessage().contains("terraform: true"));
    }

    @Test
    void diretorioTerraformNaoPodeEscaparDoWorkspace() throws Exception {
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: iac/01-primeiro-apply
                titulo: Primeiro apply
                dificuldade: guiado
                terraform: true
                diretorioTerraform: ../fora
                ---
                # Primeiro apply
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_rodando
                    nome: mirante-web
                """);

        var erro = assertThrows(
                IllegalArgumentException.class, () -> new LeitorDeCenario().ler(diretorio));

        assertTrue(erro.getMessage().contains("relativo"));
    }

    @Test
    void montaAsercaoDeEstadoDoTerraformComODiretorioDoFrontmatter() throws Exception {
        Files.writeString(diretorio.resolve("cenario.md"), """
                ---
                id: iac/01-primeiro-apply
                titulo: Primeiro apply
                dificuldade: guiado
                terraform: true
                diretorioTerraform: infra
                ---
                # Primeiro apply
                """);
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: terraform_estado
                    endereco: docker_container.web
                    atributo: name
                    esperado: mirante-web
                    descricao: o container está sob gestão do Terraform
                """);

        Assercao asercao = new LeitorDeCenario().ler(diretorio).asercoes().getFirst();

        assertInstanceOf(Assercao.TerraformEstado.class, asercao);
        var estado = (Assercao.TerraformEstado) asercao;
        assertEquals("infra", estado.diretorio());
        assertEquals("docker_container.web", estado.endereco());
        assertEquals("name", estado.atributo());
        assertEquals("mirante-web", estado.esperado());
    }

    @Test
    void asercaoDeTerraformExigeTerraformLigado() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(diretorio.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: terraform_estado
                    endereco: docker_container.web
                    descricao: o container está sob gestão do Terraform
                """);

        var erro = assertThrows(
                IllegalArgumentException.class, () -> new LeitorDeCenario().ler(diretorio));

        assertTrue(erro.getMessage().contains("terraform: true"));
    }
}
