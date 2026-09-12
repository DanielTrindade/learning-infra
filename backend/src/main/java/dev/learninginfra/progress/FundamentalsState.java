package dev.learninginfra.progress;

/** O estado dos Fundamentos derivado do progresso, num lugar só. */
public enum FundamentalsState {
    NOT_STARTED, IN_PROGRESS, COMPLETED;

    public static FundamentalsState from(FundamentalsProgress progress) {
        if (progress.completed()) {
            return COMPLETED;
        }
        return progress.attempts() == 0 ? NOT_STARTED : IN_PROGRESS;
    }
}
