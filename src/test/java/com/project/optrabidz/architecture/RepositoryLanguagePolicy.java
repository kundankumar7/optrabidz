package com.project.optrabidz.architecture;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

    static List<Violation> inspectTrackedRepository(Path start, List<PolicyEntry> policy)
            throws IOException, InterruptedException {
        Path repositoryRoot = repositoryRoot(start);
        byte[] inventory = runGit(repositoryRoot, "ls-files", "-z");
        List<Violation> violations = new ArrayList<>();

        for (String trackedPath : nullDelimitedUtf8(inventory)) {
            Path relativePath = Path.of(trackedPath).normalize();
            if (relativePath.isAbsolute()) {
                throw inventoryFailure();
            }

            Path absolutePath = repositoryRoot.resolve(relativePath).normalize();
            if (!absolutePath.startsWith(repositoryRoot)) {
                throw inventoryFailure();
            }

            violations.addAll(inspectPath(relativePath, policy));
            if (Files.isSymbolicLink(absolutePath)
                    || !Files.isRegularFile(absolutePath, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }

            byte[] content = Files.readAllBytes(absolutePath);
            if (containsNullByte(content)) {
                continue;
            }

            String text = decodeUtf8(content);
            if (text != null) {
                violations.addAll(inspectText(relativePath, text, policy));
            }
        }

        violations.sort(Comparator.comparing(Violation::path)
                .thenComparing(violation -> violation.line() == null ? 0 : violation.line())
                .thenComparing(Violation::policyId));
        return List.copyOf(violations);
    }

    private static Path repositoryRoot(Path start) throws IOException, InterruptedException {
        String root = decodeRequiredUtf8(runGit(start, "rev-parse", "--show-toplevel")).trim();
        if (root.isEmpty()) {
            throw inventoryFailure();
        }
        return Path.of(root).toAbsolutePath().normalize();
    }

    private static byte[] runGit(Path directory, String... arguments)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));

        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        byte[] output = process.getInputStream().readAllBytes();
        if (process.waitFor() != 0) {
            throw inventoryFailure();
        }
        return output;
    }

    private static List<String> nullDelimitedUtf8(byte[] value) {
        List<String> entries = new ArrayList<>();
        int start = 0;
        for (int index = 0; index <= value.length; index++) {
            if (index < value.length && value[index] != 0) {
                continue;
            }
            if (index > start) {
                entries.add(decodeRequiredUtf8(Arrays.copyOfRange(value, start, index)));
            }
            start = index + 1;
        }
        return List.copyOf(entries);
    }

    private static boolean containsNullByte(byte[] value) {
        for (byte current : value) {
            if (current == 0) {
                return true;
            }
        }
        return false;
    }

    private static String decodeUtf8(byte[] value) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(value))
                    .toString();
        } catch (CharacterCodingException exception) {
            return null;
        }
    }

    private static String decodeRequiredUtf8(byte[] value) {
        String decoded = decodeUtf8(value);
        if (decoded == null) {
            throw inventoryFailure();
        }
        return decoded;
    }

    private static IllegalStateException inventoryFailure() {
        return new IllegalStateException("Unable to inspect tracked repository content");
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
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        List<Token> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentLine = 1;
        int tokenLine = 1;
        int previousCodePoint = -1;

        for (int offset = 0; offset < normalized.length(); ) {
            int codePoint = normalized.codePointAt(offset);
            if (Character.isLetterOrDigit(codePoint)) {
                if (!current.isEmpty() && startsCamelCaseToken(normalized, offset, previousCodePoint)) {
                    addToken(tokens, current, tokenLine);
                }
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
            previousCodePoint = Character.isLetterOrDigit(codePoint) ? codePoint : -1;
            offset += Character.charCount(codePoint);
        }
        addToken(tokens, current, tokenLine);
        return List.copyOf(tokens);
    }

    private static boolean startsCamelCaseToken(
            String value, int offset, int previousCodePoint) {
        int currentCodePoint = value.codePointAt(offset);
        if (!Character.isUpperCase(currentCodePoint)) {
            return false;
        }
        if (Character.isLowerCase(previousCodePoint) || Character.isDigit(previousCodePoint)) {
            return true;
        }

        int nextOffset = offset + Character.charCount(currentCodePoint);
        return Character.isUpperCase(previousCodePoint)
                && nextOffset < value.length()
                && Character.isLowerCase(value.codePointAt(nextOffset));
    }

    private static void addToken(List<Token> tokens, StringBuilder current, int line) {
        if (current.isEmpty()) {
            return;
        }
        tokens.add(new Token(current.toString().toLowerCase(Locale.ROOT), line));
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
