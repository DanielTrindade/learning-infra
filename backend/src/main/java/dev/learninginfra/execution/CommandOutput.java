package dev.learninginfra.execution;

public record CommandOutput(int exitCode, String stdout, String stderr) {

    public boolean success() {
        return exitCode == 0;
    }
}
