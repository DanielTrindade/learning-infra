package dev.learninginfra.api;

import dev.learninginfra.api.dto.FundamentosDetalhados;
import dev.learninginfra.api.dto.ResultadoDoQuestionarioDto;
import dev.learninginfra.api.dto.SubmissaoDoQuestionario;
import dev.learninginfra.api.dto.TrilhaResumo;
import dev.learninginfra.progresso.ProgressoDosFundamentos;
import dev.learninginfra.progresso.RepositorioDeProgresso;
import dev.learninginfra.trilha.CatalogoDeTrilhas;
import dev.learninginfra.trilha.Trilha;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/trilhas")
public class TrilhaController {

    private final CatalogoDeTrilhas catalogo;
    private final RepositorioDeProgresso progressos;

    public TrilhaController(
            CatalogoDeTrilhas catalogo,
            RepositorioDeProgresso progressos) {
        this.catalogo = catalogo;
        this.progressos = progressos;
    }

    @GetMapping
    public List<TrilhaResumo> listar() {
        var progresso = progressos.carregar();
        return catalogo.listar().stream()
                .map(trilha -> TrilhaResumo.de(trilha, progresso))
                .toList();
    }

    @GetMapping("/{id}/fundamentos")
    public FundamentosDetalhados fundamentos(@PathVariable String id) {
        Trilha trilha = exigirTrilha(id);
        if (trilha.fundamentos() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Fundamentos não publicados para a Trilha: " + id);
        }
        var progresso = progressos.carregar().fundamentos()
                .getOrDefault(id, ProgressoDosFundamentos.vazio());
        return FundamentosDetalhados.de(id, trilha.fundamentos(), progresso);
    }

    @PostMapping("/{id}/questionario")
    public ResultadoDoQuestionarioDto responder(
            @PathVariable String id,
            @RequestBody SubmissaoDoQuestionario submissao) {
        Trilha trilha = exigirTrilha(id);
        if (trilha.fundamentos() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Fundamentos não publicados para a Trilha: " + id);
        }
        try {
            return ResultadoDoQuestionarioDto.de(
                    catalogo.responder(id, submissao == null ? null : submissao.respostas()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private Trilha exigirTrilha(String id) {
        return catalogo.buscar(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Trilha não encontrada: " + id));
    }
}
