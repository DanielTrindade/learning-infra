package dev.learninginfra.lifecycle;

import dev.learninginfra.content.Scenario;
import dev.learninginfra.execution.CommandExecutor;
import dev.learninginfra.execution.CommandOutput;
import dev.learninginfra.execution.CommandText;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Sobe o MiniStack pinado do Cenário AWS e espera a inicialização concluir. */
class AwsPreparer {

    private static final String MINISTACK_IMAGE =
            "ministackorg/ministack:1.4.13-full@sha256:"
            + "07c3bc4b0eeb8f68669f2352cd624ccb09b477ab78b8b21ba1461f2035f3f610";

    private static final String READY_SCRIPT = """
            import json, sys, time, urllib.request
            for _ in range(48):
                try:
                    with urllib.request.urlopen('http://127.0.0.1:4566/_ministack/ready', timeout=1) as resposta:
                        estado = json.load(resposta)
                    if estado.get('status') == 'completed':
                        sys.exit(0 if estado.get('failed', 0) == 0 else 2)
                except Exception:
                    pass
                time.sleep(0.5)
            sys.exit(1)
            """;

    private final CommandExecutor executor;
    private final AwsLocalCleanup limpeza;

    AwsPreparer(CommandExecutor executor) {
        this.executor = executor;
        this.limpeza = new AwsLocalCleanup(executor);
    }

    void prepare(Scenario scenario, Path work) {
        if (!scenario.usesAws()) {
            return;
        }

        limpeza.clean("preparar um ambiente limpo");

        List<String> command = new ArrayList<>(List.of(
                "docker", "run", "--detach", "--pull=never",
                "--name", AwsLocalCleanup.CONTAINER_MINISTACK,
                "--label", "learning-infra.aws=true",
                "--publish", "127.0.0.1:4566:4566",
                "--env", "PERSIST_STATE=0",
                "--env", "S3_PERSIST=0",
                "--env", "RDS_PERSIST=0"));

        if (scenario.usesRealAwsInfrastructure()) {
            command.addAll(List.of(
                    "--volume", "/var/run/docker.sock:/var/run/docker.sock"));
        }

        if (scenario.hasAwsInitialization()) {
            Path inicializacao = work.resolve(scenario.awsInitialization()).normalize();
            if (!inicializacao.startsWith(work) || !Files.isDirectory(inicializacao)) {
                throw new IllegalStateException(
                        "inicializacaoAws aponta para fora do workspace ou não é um diretório: "
                        + scenario.awsInitialization());
            }
            command.addAll(List.of(
                    "--mount", "type=bind,source=" + inicializacao
                            + ",target=/etc/localstack/init/ready.d,readonly"));
        }

        command.add(MINISTACK_IMAGE);
        CommandOutput started = executor.execute(List.copyOf(command));
        if (!started.success()) {
            throw new IllegalStateException(
                    "não consegui iniciar o MiniStack para o Cenário " + scenario.id() + ": "
                    + CommandText.lastDetail(started) + " — confirme que a imagem `"
                    + MINISTACK_IMAGE + "` foi baixada e que a porta 4566 está livre");
        }

        CommandOutput ready = executor.execute(List.of(
                "docker", "exec", AwsLocalCleanup.CONTAINER_MINISTACK,
                "python", "-c", READY_SCRIPT));
        if (!ready.success()) {
            CommandOutput logs = executor.execute(List.of(
                    "docker", "logs", "--tail", "20", AwsLocalCleanup.CONTAINER_MINISTACK));
            throw new IllegalStateException(
                    "o MiniStack não concluiu a inicialização do Cenário " + scenario.id() + ": "
                    + CommandText.lastDetail(logs) + " — consulte `docker logs "
                    + AwsLocalCleanup.CONTAINER_MINISTACK + "`");
        }
    }
}
