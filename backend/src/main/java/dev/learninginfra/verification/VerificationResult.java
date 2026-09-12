package dev.learninginfra.verification;

import java.util.List;

public record VerificationResult(boolean completed, List<AssertionResult> assertions) {
}
