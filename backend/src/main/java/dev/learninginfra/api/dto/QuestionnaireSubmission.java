package dev.learninginfra.api.dto;

import java.util.Map;

public record QuestionnaireSubmission(Map<String, String> answers) {
}
