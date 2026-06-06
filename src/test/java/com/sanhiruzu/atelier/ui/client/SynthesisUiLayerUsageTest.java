package com.sanhiruzu.atelier.ui.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisUiLayerUsageTest {
    private static final Path UI_CLIENT_ROOT = Path.of("src/main/java/com/sanhiruzu/atelier/ui/client");
    private static final String LAYER_FILE = "SynthesisUiLayer.java";
    private static final Pattern TRANSLATE_CALL = Pattern.compile(
            "pose\\s*\\(\\s*\\)\\s*\\.\\s*translate\\s*\\((.*?)\\)",
            Pattern.DOTALL
    );
    private static final Pattern ZERO_LITERAL = Pattern.compile("[+-]?0+(?:\\.0+)?(?:[fFdDlL])?");

    @Test
    void synthesisUiNonZeroZTranslationsUseNamedLayers() throws IOException {
        List<String> violations = new ArrayList<>();
        try (var paths = Files.walk(UI_CLIENT_ROOT)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals(LAYER_FILE))
                    .toList()) {
                collectViolations(path, violations);
            }
        }

        assertThat(violations)
                .as("Use SynthesisUiLayer.<layer>.run(...) for nonzero UI z translations. Zero-z 2D transforms are allowed.")
                .isEmpty();
    }

    private static void collectViolations(Path path, List<String> violations) throws IOException {
        String source = Files.readString(path);
        Matcher matcher = TRANSLATE_CALL.matcher(source);
        while (matcher.find()) {
            List<String> arguments = splitArguments(matcher.group(1));
            if (arguments.size() < 3) {
                continue;
            }
            String zArgument = arguments.get(2).trim();
            if (!ZERO_LITERAL.matcher(zArgument).matches()) {
                violations.add(relativeLine(path, source, matcher.start())
                        + " uses raw z translation argument '" + zArgument + "'");
            }
        }
    }

    private static List<String> splitArguments(String arguments) {
        ArrayList<String> parts = new ArrayList<>();
        int start = 0;
        int depth = 0;
        for (int i = 0; i < arguments.length(); i++) {
            char current = arguments.charAt(i);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth = Math.max(0, depth - 1);
            } else if (current == ',' && depth == 0) {
                parts.add(arguments.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(arguments.substring(start));
        return parts;
    }

    private static String relativeLine(Path path, String source, int offset) {
        long line = source.substring(0, offset).chars()
                .filter(ch -> ch == '\n')
                .count() + 1;
        return UI_CLIENT_ROOT.relativize(path) + ":" + line;
    }
}
