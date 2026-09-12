package dev.learninginfra.api;

import dev.learninginfra.api.dto.ScenarioDetail;
import dev.learninginfra.lifecycle.ActiveScenarioManager;
import dev.learninginfra.content.Scenario;
import dev.learninginfra.content.ScenarioRepository;
import dev.learninginfra.progress.Progress;
import dev.learninginfra.progress.ProgressRepository;
import dev.learninginfra.verification.VerificationEngine;
import dev.learninginfra.verification.VerificationResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScenarioController {

    private final ScenarioRepository scenarios;
    private final ProgressRepository progressRepository;
    private final ActiveScenarioManager manager;
    private final VerificationEngine engine;

    public ScenarioController(ScenarioRepository scenarios, ProgressRepository progressRepository,
                             ActiveScenarioManager manager, VerificationEngine engine) {
        this.scenarios = scenarios;
        this.progressRepository = progressRepository;
        this.manager = manager;
        this.engine = engine;
    }

    @GetMapping("/scenarios")
    public List<ScenarioDetail> list() {
        var progress = progressRepository.load();
        return scenarios.list().stream()
                .map(scenario -> detail(scenario, progress))
                .toList();
    }

    @GetMapping("/scenarios/{track}/{slug}")
    public ScenarioDetail find(@PathVariable String track, @PathVariable String slug) {
        return detail(require(track, slug), progressRepository.load());
    }

    @PostMapping("/scenarios/{track}/{slug}/start")
    public Map<String, String> start(@PathVariable String track, @PathVariable String slug) {
        Scenario scenario = require(track, slug);
        return Map.of("workingDirectory", manager.start(scenario).toString());
    }

    @PostMapping("/scenarios/{track}/{slug}/verify")
    public VerificationResult verify(@PathVariable String track, @PathVariable String slug) {
        Scenario scenario = require(track, slug);
        requireActiveScenario(scenario);
        VerificationResult result = engine.verify(scenario.assertions());
        if (result.completed()) {
            manager.markCompleted(scenario);
        }
        return result;
    }

    /**
     * A Verificação só vale contra o ambiente do Cenário Ativo. Sem esta guarda, um
     * Cenário nunca iniciado poderia ser aprovado por sobra do ambiente anterior — a
     * invariante do ADR 0002 ficaria só na convenção do README.
     */
    private void requireActiveScenario(Scenario scenario) {
        String active = manager.activeScenario().orElse(null);
        if (scenario.id().equals(active)) {
            return;
        }
        String motivo = active == null
                ? "nenhum Cenário está ativo"
                : "o Cenário ativo é `" + active + "`";
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                motivo + " — clique em Iniciar cenário antes de verificar `" + scenario.id() + "`");
    }

    private Scenario require(String track, String slug) {
        return scenarios.find(track + "/" + slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "cenário não encontrado: " + track + "/" + slug));
    }

    private ScenarioDetail detail(Scenario scenario, Progress progress) {
        return ScenarioDetail.from(
                scenario,
                scenario.id().equals(progress.activeScenario()),
                progress.completed().containsKey(scenario.id()));
    }
}
