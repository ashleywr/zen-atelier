package com.sanhiruzu.atelier.ui.patchouli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PatchouliTextResourceTest {
    private static final Path BOOK_ROOT = Path.of("src/main/resources/assets/zen_atelier/patchouli_books/room_journal/en_us");
    private static final Path ENTRY_ROOT = BOOK_ROOT.resolve("entries");
    private static final Path BOOK_JSON = Path.of("src/main/resources/data/zen_atelier/patchouli_books/room_journal/book.json");
    private static final Path ASSET_ROOT = Path.of("src/main/resources/assets/zen_atelier");
    private static final Pattern TEXTURE_REFERENCE = Pattern.compile("\"zen_atelier:(textures/[^\"]+\\.png)\"");
    private static final Pattern ENTRY_REFERENCE = Pattern.compile("\"(zen_atelier:(?:basics|rooms)/[^\"]+)\"");

    @Test
    void roomJournalTranslationsUsePatchouliFormattingMacros() throws IOException {
        String lang = Files.readString(Path.of("src/main/resources/assets/zen_atelier/lang/en_us.json"));

        assertThat(lang).doesNotContain("\u00a7");
        assertThat(lang.lines()
                .filter(line -> line.contains("patchouli.zen_atelier.room_journal"))
                .filter(line -> line.contains("\\n")))
                .isEmpty();
    }

    @Test
    void textureIconsReferencedByRoomJournalExist() throws IOException {
        try (var files = Files.walk(BOOK_ROOT)) {
            java.util.List<Path> patchouliJson = new java.util.ArrayList<>(files
                    .filter(path -> path.toString().endsWith(".json"))
                    .toList());
            patchouliJson.add(BOOK_JSON);

            assertThat(patchouliJson.stream()
                    .flatMap(path -> textureReferences(path).stream())
                    .map(ASSET_ROOT::resolve)
                    .filter(path -> !Files.exists(path))
                    .toList())
                    .isEmpty();
        }
    }

    @Test
    void relationPagesOnlyReferenceAlwaysVisibleEntries() throws IOException {
        java.util.Set<String> alwaysVisibleEntries = new java.util.HashSet<>();
        try (var files = Files.walk(ENTRY_ROOT)) {
            for (Path path : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                String json = Files.readString(path);
                if (!json.contains("\"flag\"")) {
                    alwaysVisibleEntries.add(entryId(path));
                }
            }
        }

        try (var files = Files.walk(ENTRY_ROOT)) {
            assertThat(files
                    .filter(path -> path.toString().endsWith(".json"))
                    .flatMap(path -> entryReferences(path).stream())
                    .filter(reference -> !alwaysVisibleEntries.contains(reference))
                    .toList())
                    .isEmpty();
        }
    }

    private static java.util.List<String> textureReferences(Path path) {
        try {
            String json = Files.readString(path);
            Matcher matcher = TEXTURE_REFERENCE.matcher(json);
            java.util.List<String> references = new java.util.ArrayList<>();
            while (matcher.find()) {
                references.add(matcher.group(1));
            }
            return references;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }

    private static java.util.List<String> entryReferences(Path path) {
        try {
            String json = Files.readString(path);
            Matcher matcher = ENTRY_REFERENCE.matcher(json);
            java.util.List<String> references = new java.util.ArrayList<>();
            while (matcher.find()) {
                references.add(matcher.group(1));
            }
            return references;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }

    private static String entryId(Path path) {
        String relative = ENTRY_ROOT.relativize(path).toString()
                .replace('\\', '/')
                .replace(".json", "");
        return "zen_atelier:" + relative;
    }
}
