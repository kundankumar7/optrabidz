package com.project.optrabidz.architecture;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class RepositoryLanguagePolicy {

    private RepositoryLanguagePolicy() {}

    static PolicyEntry policyEntry(String id, String clearText) {
        List<Token> tokens = tokenize(clearText);
        return new PolicyEntry(id, tokens.size(), fingerprint(join(tokens, 0, tokens.size())));
    }

    static List<Violation> inspectText(Path path, String content, List<PolicyEntry> policy) {
        List<Token> tokens = tokenize(content);
        List<Violation> violations = new ArrayList<>();

        for (PolicyEntry entry : policy) {
            Integer line = firstMatchingLine(tokens, entry);
            if (line != null) {
                violations.add(new Violation(repositoryPath(path), line, entry.id()));
            }
        }

        return List.copyOf(violations);
    }

    static List<Violation> inspectPath(Path path, List<PolicyEntry> policy) {
        String repositoryPath = repositoryPath(path);
        List<Token> tokens = tokenize(repositoryPath);
        List<Violation> violations = new ArrayList<>();

        for (PolicyEntry entry : policy) {
            if (firstMatchingLine(tokens, entry) != null) {
                violations.add(new Violation(repositoryPath, null, entry.id()));
            }
        }

        return List.copyOf(violations);
    }

    private static Integer firstMatchingLine(List<Token> tokens, PolicyEntry entry) {
        for (int start = 0; start + entry.tokenCount() <= tokens.size(); start++) {
            if (fingerprint(join(tokens, start, entry.tokenCount())).equals(entry.fingerprint())) {
                return tokens.get(start).line();
            }
        }
        return null;
    }

    private static String join(List<Token> tokens, int start, int count) {
        StringBuilder value = new StringBuilder();
        for (int index = start; index < start + count; index++) {
            if (!value.isEmpty()) {
                value.append(' ');
            }
            value.append(tokens.get(index).value());
        }
        return value.toString();
    }

    private static List<Token> tokenize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        List<Token> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentLine = 1;
        int tokenLine = 1;

        for (int offset = 0; offset < normalized.length(); ) {
            int codePoint = normalized.codePointAt(offset);
            if (Character.isLetterOrDigit(codePoint)) {
                if (current.isEmpty()) {
                    tokenLine = currentLine;
                }
                current.appendCodePoint(codePoint);
            } else {
                addToken(tokens, current, tokenLine);
                if (codePoint == '\n') {
                    currentLine++;
                }
            }
            offset += Character.charCount(codePoint);
        }
        addToken(tokens, current, tokenLine);
        return List.copyOf(tokens);
    }

    private static void addToken(List<Token> tokens, StringBuilder current, int line) {
        if (current.isEmpty()) {
            return;
        }
        tokens.add(new Token(current.toString(), line));
        current.setLength(0);
    }

    private static String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String repositoryPath(Path path) {
        return path.toString().replace('\\', '/');
    }

    record PolicyEntry(String id, int tokenCount, String fingerprint) {}

    record Violation(String path, Integer line, String policyId) {

        @Override
        public String toString() {
            String location = line == null ? path : path + ":" + line;
            return location + " violates repository language policy [" + policyId + "]";
        }
    }

    private record Token(String value, int line) {}
}
