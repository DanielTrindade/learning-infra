package dev.learninginfra.progress;

import com.fasterxml.jackson.annotation.JsonAlias;

public record FundamentalsProgress(
        @JsonAlias("tentativas") int attempts,
        @JsonAlias("melhorPercentual") int bestScore,
        @JsonAlias("concluidoEm") String completedAt) {

    public static FundamentalsProgress empty() {
        return new FundamentalsProgress(0, 0, null);
    }

    public FundamentalsProgress register(
            int score, boolean passed, String instant) {
        String firstApproval = completedAt;
        if (passed && firstApproval == null) {
            firstApproval = instant;
        }
        return new FundamentalsProgress(
                attempts + 1,
                Math.max(bestScore, score),
                firstApproval);
    }

    public boolean completed() {
        return completedAt != null;
    }
}
