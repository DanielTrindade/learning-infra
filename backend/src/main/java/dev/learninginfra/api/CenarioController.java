package dev.learninginfra.api;

import dev.learninginfra.api.dto.CenarioDetalhado;
import dev.learninginfra.ciclodevida.GerenciadorDeCenarioAtivo;
import dev.learninginfra.conteudo.Cenario;
import dev.learninginfra.conteudo.RepositorioDeCenarios;
import dev.learninginfra.progresso.RepositorioDeProgresso;
import dev.learninginfra.verificacao.MotorDeVerificacao;
import dev.learninginfra.verificacao.ResultadoDaVerificacao;
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
public class CenarioController {

    private final RepositorioDeCenarios cenarios;
    private final RepositorioDeProgresso progressos;
    private final GerenciadorDeCenarioAtivo gerenciador;
    private final MotorDeVerificacao motor;

    public CenarioController(RepositorioDeCenarios cenarios, RepositorioDeProgresso progressos,
                             GerenciadorDeCenarioAtivo gerenciador, MotorDeVerificacao motor) {
        this.cenarios = cenarios;
        this.progressos = progressos;
        this.gerenciador = gerenciador;
        this.motor = motor;
    }

    @GetMapping("/cenarios")
    public List<CenarioDetalhado> listar() {
        return cenarios.listar().stream().map(this::detalhar).toList();
    }

    @GetMapping("/cenarios/{trilha}/{slug}")
    public CenarioDetalhado buscar(@PathVariable String trilha, @PathVariable String slug) {
        return detalhar(exigir(trilha, slug));
    }

    @PostMapping("/cenarios/{trilha}/{slug}/iniciar")
    public Map<String, String> iniciar(@PathVariable String trilha, @PathVariable String slug) {
        Cenario cenario = exigir(trilha, slug);
        return Map.of("diretorioDeTrabalho", gerenciador.iniciar(cenario).toString());
    }

    @PostMapping("/cenarios/{trilha}/{slug}/verificar")
    public ResultadoDaVerificacao verificar(@PathVariable String trilha, @PathVariable String slug) {
        Cenario cenario = exigir(trilha, slug);
        ResultadoDaVerificacao resultado = motor.verificar(cenario.asercoes());
        if (resultado.concluido()) {
            gerenciador.marcarConcluido(cenario);
        }
        return resultado;
    }

    private Cenario exigir(String trilha, String slug) {
        return cenarios.buscar(trilha + "/" + slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "cenário não encontrado: " + trilha + "/" + slug));
    }

    private CenarioDetalhado detalhar(Cenario cenario) {
        var progresso = progressos.carregar();
        return CenarioDetalhado.de(
                cenario,
                cenario.id().equals(progresso.cenarioAtivo()),
                progresso.concluidos().containsKey(cenario.id()));
    }
}
