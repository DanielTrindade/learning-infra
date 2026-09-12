package dev.learninginfra.api;

import dev.learninginfra.api.dto.FundamentalsDetail;
import dev.learninginfra.api.dto.QuestionnaireResultDto;
import dev.learninginfra.api.dto.QuestionnaireSubmission;
import dev.learninginfra.api.dto.TrackSummary;
import dev.learninginfra.progress.FundamentalsProgress;
import dev.learninginfra.progress.ProgressRepository;
import dev.learninginfra.track.TrackCatalog;
import dev.learninginfra.track.QuestionnaireGrader;
import dev.learninginfra.track.Track;
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
@RequestMapping("/api/tracks")
public class TrackController {

    private final TrackCatalog catalog;
    private final ProgressRepository progressRepository;
    private final QuestionnaireGrader grader;

    public TrackController(
            TrackCatalog catalog,
            ProgressRepository progressRepository,
            QuestionnaireGrader grader) {
        this.catalog = catalog;
        this.progressRepository = progressRepository;
        this.grader = grader;
    }

    @GetMapping
    public List<TrackSummary> list() {
        var progress = progressRepository.load();
        return catalog.list().stream()
                .map(track -> TrackSummary.from(track, progress))
                .toList();
    }

    @GetMapping("/{id}/fundamentals")
    public FundamentalsDetail fundamentals(@PathVariable String id) {
        Track track = requireTrack(id);
        if (track.fundamentals() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Fundamentos não publicados para a Trilha: " + id);
        }
        var progress = progressRepository.load().fundamentals()
                .getOrDefault(id, FundamentalsProgress.empty());
        return FundamentalsDetail.from(id, track.fundamentals(), progress);
    }

    @PostMapping("/{id}/questionnaire")
    public QuestionnaireResultDto answer(
            @PathVariable String id,
            @RequestBody QuestionnaireSubmission submissao) {
        Track track = requireTrack(id);
        if (track.fundamentals() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Fundamentos não publicados para a Trilha: " + id);
        }
        try {
            return QuestionnaireResultDto.from(
                    grader.answer(id, submissao == null ? null : submissao.answers()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private Track requireTrack(String id) {
        return catalog.find(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Trilha não encontrada: " + id));
    }
}
