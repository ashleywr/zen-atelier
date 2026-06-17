package com.sanhiruzu.atelier.resources;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AtelierResourceDataTest {
    private static final Pattern OPTIONAL_TAG_ENTRY =
            Pattern.compile("\\{\\s*\"id\"\\s*:\\s*\"(?:#)?(?!minecraft:)([a-z0-9_.-]+):[^\"}]+\"(?!\\s*,\\s*\"required\"\\s*:\\s*false)", Pattern.DOTALL);

    @Test
    void structureCharmHasItemModel() {
        Path project = Path.of(System.getProperty("project.dir", "."));
        Path model = project.resolve("src/main/resources/assets/zen_atelier/models/item/structure_charm.json");

        assertTrue(Files.exists(model), "structure_charm is a player item and must have an item model");
    }

    @Test
    void optionalModTagEntriesAreMarkedOptional() throws IOException {
        Path project = Path.of(System.getProperty("project.dir", "."));
        Path tags = project.resolve("src/main/resources/data/zen_atelier/tags");
        if (!Files.exists(tags)) {
            return;
        }

        List<Path> invalid = new ArrayList<>();
        try (Stream<Path> files = Files.walk(tags)) {
            files.filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> assertOptionalEntries(path, invalid));
        }

        assertTrue(invalid.isEmpty(), "Optional mod tag entries must include required=false: " + invalid);
    }

    private static void assertOptionalEntries(Path path, List<Path> invalid) {
        try {
            Matcher matcher = OPTIONAL_TAG_ENTRY.matcher(Files.readString(path));
            if (matcher.find()) {
                invalid.add(path);
            }
        } catch (IOException exception) {
            throw new AssertionError("Failed to read " + path, exception);
        }
    }
}
