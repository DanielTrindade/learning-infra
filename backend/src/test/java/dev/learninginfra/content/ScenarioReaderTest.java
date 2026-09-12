package dev.learninginfra.content;

import dev.learninginfra.verification.Assertion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioReaderTest {

    @TempDir
    Path directory;

    private void escreverCenarioCompleto() throws Exception {
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: docker/01-servir-html-nginx
                titulo: Servir um HTML seu com nginx
                dificuldade: guiado
                containers: [lab-web]
                ---
                # Servir um HTML seu com nginx

                Texto didático aqui.
                """);
        Files.writeString(directory.resolve("verificacao.yaml"), """
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

        Scenario scenario = new ScenarioReader().read(directory);

        assertEquals("docker/01-servir-html-nginx", scenario.id());
        assertEquals("Servir um HTML seu com nginx", scenario.title());
        assertEquals(Difficulty.GUIDED, scenario.difficulty());
        assertEquals(List.of("lab-web"), scenario.containers());
    }

    @Test
    void separaOCorpoDoFrontmatter() throws Exception {
        escreverCenarioCompleto();

        Scenario scenario = new ScenarioReader().read(directory);

        assertTrue(scenario.markdown().startsWith("# Servir um HTML seu com nginx"));
        assertFalse(scenario.markdown().contains("dificuldade:"));
    }

    @Test
    void leAsTresAsercoes() throws Exception {
        escreverCenarioCompleto();

        List<Assertion> assertions = new ScenarioReader().read(directory).assertions();

        assertEquals(3, assertions.size());
        assertEquals(new Assertion.ContainerRunning("lab-web"), assertions.get(0));
        assertEquals(new Assertion.HttpResponds("http://localhost:8080", 200), assertions.get(1));
        assertEquals(new Assertion.HttpBodyContains("http://localhost:8080", "Meu primeiro container"),
                assertions.get(2));
    }

    @Test
    void leOProjetoComposeQuandoDeclarado() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: docker/03
                titulo: Com compose
                dificuldade: guiado
                projetoCompose: lab-03
                ---
                # corpo
                """);

        assertEquals("lab-03", new ScenarioReader().read(directory).composeProject());
    }

    @Test
    void projetoComposeEhNuloQuandoAusente() throws Exception {
        escreverCenarioCompleto();

        assertNull(new ScenarioReader().read(directory).composeProject());
    }

    @Test
    void leAsercaoDeImagem() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: imagem_existe
                    referencia: lab-app:1.0
                """);

        List<Assertion> assertions = new ScenarioReader().read(directory).assertions();

        assertEquals(List.of(new Assertion.ImageExists("lab-app:1.0")), assertions);
    }

    @Test
    void leAsercoesDeVolumeEDeComando() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: volume_existe
                    nome: lab-05-dados
                  - tipo: comando_produz
                    descricao: o dado sobreviveu ao container
                    comando: ["docker", "run", "--rm", "alpine", "echo", "tamandua"]
                    contem: tamandua
                """);

        List<Assertion> assertions = new ScenarioReader().read(directory).assertions();

        assertEquals(new Assertion.VolumeExists("lab-05-dados"), assertions.get(0));
        assertEquals(
                new Assertion.CommandProduces(
                        List.of("docker", "run", "--rm", "alpine", "echo", "tamandua"),
                        "tamandua",
                        "o dado sobreviveu ao container"),
                assertions.get(1));
    }

    @Test
    void leAsercoesDeSaudeEDeRede() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_saudavel
                    nome: lab-08-db-1
                  - tipo: container_em_rede
                    nome: lab-07-db-1
                    rede: lab-07-interna
                    presente: true
                """);

        List<Assertion> assertions = new ScenarioReader().read(directory).assertions();

        assertEquals(new Assertion.ContainerHealthy("lab-08-db-1"), assertions.get(0));
        assertEquals(
                new Assertion.ContainerInNetwork("lab-07-db-1", "lab-07-interna", true),
                assertions.get(1));
    }

    @Test
    void leContainerConfiguracaoComCamposOpcionais() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_configuracao
                    nome: lab-09-app-1
                    usuario: node
                    somenteLeitura: true
                    capabilitiesRemovidas: [ALL]
                """);

        var assertion = new ScenarioReader().read(directory).assertions().getFirst();

        assertEquals(
                new Assertion.ContainerConfiguration(
                        "lab-09-app-1", "node", true, java.util.List.of("ALL")),
                assertion);
    }

    @Test
    void leImagemNoRegistryComDescricaoDoCenario() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: imagem_no_registry
                    referencia: localhost:5000/lab-10-app:1.0
                    descricao: a v1.0 está publicada no registry
                """);

        var assertion = new ScenarioReader().read(directory).assertions().getFirst();

        assertEquals(
                new Assertion.ImageInRegistry("localhost:5000/lab-10-app:1.0",
                        "a v1.0 está publicada no registry"),
                assertion);
    }

    @Test
    void leOsVolumesDeclarados() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: docker/05
                titulo: Com volume
                dificuldade: assistido
                volumes: [lab-05-dados]
                ---
                # corpo
                """);

        assertEquals(List.of("lab-05-dados"), new ScenarioReader().read(directory).volumes());
    }

    @Test
    void leMetadadosDeKubernetes() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("cenario.md"), """
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

        Scenario scenario = new ScenarioReader().read(directory);

        assertEquals("docker-desktop", scenario.kubernetesContext());
        assertEquals("learning-infra-k8s-06", scenario.kubernetesNamespace());
        assertEquals("setup", scenario.initialManifests());
        assertTrue(scenario.usesKubernetes());
        assertTrue(scenario.hasInitialManifests());
    }

    @Test
    void recusaNamespaceKubernetesSemContexto() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: kubernetes/01
                titulo: Incompleto
                dificuldade: guiado
                namespaceKubernetes: learning-infra-k8s-01
                ---
                # corpo
                """);

        var error = assertThrows(
                IllegalArgumentException.class, () -> new ScenarioReader().read(directory));

        assertTrue(error.getMessage().contains("devem aparecer juntos"));
    }

    @Test
    void leAsercoesTipadasDeKubernetesComContextoDoCenario() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: kubernetes/10-rbac
                titulo: RBAC
                dificuldade: assistido
                contextoKubernetes: docker-desktop
                namespaceKubernetes: li-k8s-10
                ---
                # corpo
                """);
        Files.writeString(directory.resolve("verificacao.yaml"), """
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

        List<Assertion> assertions = new ScenarioReader().read(directory).assertions();

        assertEquals(3, assertions.size());
        assertEquals("docker-desktop", ((Assertion.KubernetesCondition) assertions.get(0)).context());
        assertEquals(12, ((Assertion.KubernetesCondition) assertions.get(0)).timeoutSeconds());
        assertEquals(10, ((Assertion.KubernetesJsonpath) assertions.get(1)).timeoutSeconds());
        assertFalse(((Assertion.KubernetesRbac) assertions.get(2)).allowed());
    }

    @Test
    void leMetadadosEAsercaoDeAwsSemAceitarEndpointDoConteudo() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("cenario.md"), """
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
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: aws_consulta
                    servico: s3api
                    operacao: get-bucket-versioning
                    argumentos: ["--bucket", "learning-infra-arquivos"]
                    consulta: Status
                    esperado: Enabled
                    descricao: o bucket mantém histórico de versões
                """);

        Scenario scenario = new ScenarioReader().read(directory);
        var assertion = (Assertion.AwsQuery) scenario.assertions().getFirst();

        assertTrue(scenario.usesAws());
        assertTrue(scenario.usesRealAwsInfrastructure());
        assertEquals("init", scenario.awsInitialization());
        assertEquals("http://127.0.0.1:4566", assertion.endpoint());
        assertEquals("us-east-1", assertion.region());
        assertEquals(List.of("--bucket", "learning-infra-arquivos"), assertion.arguments());
    }

    @Test
    void recusaAsercaoAwsForaDeUmCenarioMinistack() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: aws_consulta
                    servico: s3api
                    operacao: list-buckets
                    consulta: "Buckets[0].Name"
                    esperado: arquivos
                    descricao: existe um bucket
                """);

        var error = assertThrows(
                IllegalArgumentException.class, () -> new ScenarioReader().read(directory));

        assertTrue(error.getMessage().contains("ministack: true"));
    }

    @Test
    void recusaTipoDeAsercaoDesconhecido() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_falando_grego
                    nome: lab-web
                """);

        var error = assertThrows(IllegalArgumentException.class, () -> new ScenarioReader().read(directory));
        assertTrue(error.getMessage().contains("container_falando_grego"));
    }

    @Test
    void leOContainerLinuxDeclarado() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: linux/08-um-programa-vira-servico
                titulo: Um programa vira serviço
                dificuldade: guiado
                projetoCompose: linux-08
                containerLinux: learning-infra-linux
                ---
                # corpo
                """);

        Scenario scenario = new ScenarioReader().read(directory);

        assertEquals("learning-infra-linux", scenario.linuxContainer());
        assertTrue(scenario.usesLinux());
    }

    @Test
    void cenarioSemTerraformNaoDeclaraDiretorio() throws Exception {
        escreverCenarioCompleto();

        Scenario scenario = new ScenarioReader().read(directory);

        assertFalse(scenario.usesTerraform());
        assertNull(scenario.terraformDirectory());
    }

    @Test
    void terraformSemDiretorioUsaARaizDoWorkspace() throws Exception {
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: iac/01-primeiro-apply
                titulo: Primeiro apply
                dificuldade: guiado
                terraform: true
                ---
                # Primeiro apply
                """);
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_rodando
                    nome: mirante-web
                """);

        Scenario scenario = new ScenarioReader().read(directory);

        assertTrue(scenario.usesTerraform());
        assertEquals(".", scenario.terraformDirectory());
    }

    @Test
    void diretorioTerraformExigeTerraformLigado() throws Exception {
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: iac/01-primeiro-apply
                titulo: Primeiro apply
                dificuldade: guiado
                diretorioTerraform: infra
                ---
                # Primeiro apply
                """);
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_rodando
                    nome: mirante-web
                """);

        var error = assertThrows(
                IllegalArgumentException.class, () -> new ScenarioReader().read(directory));

        assertTrue(error.getMessage().contains("terraform: true"));
    }

    @Test
    void diretorioTerraformNaoPodeEscaparDoWorkspace() throws Exception {
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: iac/01-primeiro-apply
                titulo: Primeiro apply
                dificuldade: guiado
                terraform: true
                diretorioTerraform: ../fora
                ---
                # Primeiro apply
                """);
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: container_rodando
                    nome: mirante-web
                """);

        var error = assertThrows(
                IllegalArgumentException.class, () -> new ScenarioReader().read(directory));

        assertTrue(error.getMessage().contains("relativo"));
    }

    @Test
    void montaAsercaoDeEstadoDoTerraformComODiretorioDoFrontmatter() throws Exception {
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: iac/01-primeiro-apply
                titulo: Primeiro apply
                dificuldade: guiado
                terraform: true
                diretorioTerraform: infra
                ---
                # Primeiro apply
                """);
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: terraform_estado
                    endereco: docker_container.web
                    atributo: name
                    esperado: mirante-web
                    descricao: o container está sob gestão do Terraform
                """);

        Assertion assertion = new ScenarioReader().read(directory).assertions().getFirst();

        assertInstanceOf(Assertion.TerraformState.class, assertion);
        var state = (Assertion.TerraformState) assertion;
        assertEquals("infra", state.directory());
        assertEquals("docker_container.web", state.address());
        assertEquals("name", state.attribute());
        assertEquals("mirante-web", state.expected());
    }

    @Test
    void asercaoDeTerraformExigeTerraformLigado() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: terraform_estado
                    endereco: docker_container.web
                    descricao: o container está sob gestão do Terraform
                """);

        var error = assertThrows(
                IllegalArgumentException.class, () -> new ScenarioReader().read(directory));

        assertTrue(error.getMessage().contains("terraform: true"));
    }

    @Test
    void terraformEstadoComEsperadoExigeAtributo() throws Exception {
        escreverCenarioTerraform();
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: terraform_estado
                    endereco: docker_container.web
                    esperado: mirante-web
                    descricao: o container está sob gestão do Terraform
                """);

        var error = assertThrows(
                IllegalArgumentException.class, () -> new ScenarioReader().read(directory));

        assertTrue(error.getMessage().contains("atributo e esperado"));
    }

    @Test
    void terraformEstadoComAtributoExigeEsperado() throws Exception {
        escreverCenarioTerraform();
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: terraform_estado
                    endereco: docker_container.web
                    atributo: name
                    descricao: o container está sob gestão do Terraform
                """);

        var error = assertThrows(
                IllegalArgumentException.class, () -> new ScenarioReader().read(directory));

        assertTrue(error.getMessage().contains("atributo e esperado"));
    }

    private void escreverCenarioTerraform() throws Exception {
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: iac/01-primeiro-apply
                titulo: Primeiro apply
                dificuldade: guiado
                terraform: true
                ---
                # Primeiro apply
                """);
    }

    @Test
    void montaAsercaoDeServicoSystemdComOContainerDoFrontmatter() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: linux/08-um-programa-vira-servico
                titulo: Um programa vira serviço
                dificuldade: guiado
                containerLinux: learning-infra-linux
                ---
                # corpo
                """);
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: servico_systemd
                    nome: catalogo.service
                    ativo: true
                    habilitado: true
                    descricao: o catálogo vira serviço
                """);

        var assertion = (Assertion.SystemdService) new ScenarioReader().read(directory).assertions().getFirst();

        assertEquals("learning-infra-linux", assertion.container());
        assertEquals("catalogo.service", assertion.name());
        assertTrue(assertion.active());
        assertTrue(assertion.enabled());
    }

    @Test
    void asercaoDeServicoSystemdExigeContainerLinux() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: servico_systemd
                    nome: catalogo.service
                    ativo: true
                    descricao: o catálogo vira serviço
                """);

        var error = assertThrows(
                IllegalArgumentException.class, () -> new ScenarioReader().read(directory));

        assertTrue(error.getMessage().contains("containerLinux"));
    }

    @Test
    void servicoSystemdRejeitaAtivoNaoBooleano() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: linux/08-um-programa-vira-servico
                titulo: Um programa vira serviço
                dificuldade: guiado
                containerLinux: learning-infra-linux
                ---
                # corpo
                """);
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: servico_systemd
                    nome: catalogo.service
                    ativo: "true"
                    descricao: o catálogo vira serviço
                """);

        var error = assertThrows(
                IllegalArgumentException.class, () -> new ScenarioReader().read(directory));

        assertTrue(error.getMessage().contains("booleano"));
    }

    @Test
    void servicoSystemdSemAtivoNemHabilitadoEhRejeitado() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: linux/08-um-programa-vira-servico
                titulo: Um programa vira serviço
                dificuldade: guiado
                containerLinux: learning-infra-linux
                ---
                # corpo
                """);
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: servico_systemd
                    nome: catalogo.service
                    descricao: o catálogo vira serviço
                """);

        var error = assertThrows(
                IllegalArgumentException.class, () -> new ScenarioReader().read(directory));

        assertTrue(error.getMessage().contains("ativo ou habilitado"));
    }

    @Test
    void montaAsercaoDeArquivoLinuxComCamposNulosNaoVerificados() throws Exception {
        escreverCenarioCompleto();
        Files.writeString(directory.resolve("cenario.md"), """
                ---
                id: linux/13-acesso-minimo
                titulo: Acesso mínimo
                dificuldade: autonomo
                containerLinux: learning-infra-linux
                ---
                # corpo
                """);
        Files.writeString(directory.resolve("verificacao.yaml"), """
                asercoes:
                  - tipo: arquivo_linux
                    caminho: /etc/sudoers.d/plantao
                    modo: "440"
                    dono: root
                    descricao: a regra de sudo não é editável por quem ela beneficia
                """);

        var assertion = (Assertion.LinuxFile) new ScenarioReader().read(directory).assertions().getFirst();

        assertEquals("440", assertion.modo());
        assertEquals("root", assertion.dono());
        assertNull(assertion.grupo());
    }
}
