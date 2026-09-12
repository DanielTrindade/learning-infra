package dev.learninginfra.verification;

public record AssertionResult(String description, boolean passed, String detail) {

    public static AssertionResult approved(Assertion assertion) {
        return new AssertionResult(assertion.description(), true, "");
    }

    public static AssertionResult rejected(Assertion assertion, String detail) {
        return new AssertionResult(assertion.description(), false, detail);
    }
}
