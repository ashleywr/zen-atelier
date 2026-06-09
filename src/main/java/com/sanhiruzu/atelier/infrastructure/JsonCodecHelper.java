package com.sanhiruzu.atelier.infrastructure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper utilities for JSON codec operations.
 * Simplifies serialization and deserialization of complex objects.
 */
public class JsonCodecHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonCodecHelper.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Encode an object to JSON using a codec.
     */
    public static <T> JsonElement encode(T value, Codec<T> codec) {
        var result = codec.encodeStart(JsonOps.INSTANCE, value);
        if (result.result().isPresent()) {
            return result.result().get();
        }
        throw new IllegalArgumentException("Failed to encode value: " + result.error());
    }

    /**
     * Decode a JSON element to an object using a codec.
     */
    public static <T> T decode(JsonElement json, Codec<T> codec) {
        var result = codec.parse(JsonOps.INSTANCE, json);
        if (result.result().isPresent()) {
            return result.result().get();
        }
        throw new IllegalArgumentException("Failed to decode JSON: " + result.error());
    }

    /**
     * Encode an object to a JSON string.
     */
    public static <T> String encodeToString(T value, Codec<T> codec) {
        return encode(value, codec).toString();
    }

    /**
     * Decode a JSON string to an object.
     */
    public static <T> T decodeFromString(String json, Codec<T> codec) {
        JsonElement element = JsonParser.parseString(json);
        return decode(element, codec);
    }

    /**
     * Encode an object to a compact JSON string (no whitespace).
     */
    public static <T> String encodeCompact(T value, Codec<T> codec) {
        return GsonHelper.toStableString(encode(value, codec));
    }

    /**
     * Safely decode with a fallback value if parsing fails.
     */
    public static <T> T decodeSafe(String json, Codec<T> codec, T fallback) {
        try {
            return decodeFromString(json, codec);
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Save an object to a JSON file using a codec.
     */
    public static <T> void save(Path path, Codec<T> codec, T value) throws IOException {
        try {
            JsonElement json = encode(value, codec);
            String content = GSON.toJson(json);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        } catch (IOException e) {
            LOGGER.error("Failed to save to {}", path, e);
            throw e;
        }
    }

    /**
     * Load an object from a JSON file using a codec.
     */
    public static <T> T load(Path path, Codec<T> codec) throws IOException {
        try {
            String content = Files.readString(path);
            return decodeFromString(content, codec);
        } catch (IOException e) {
            LOGGER.error("Failed to load from {}", path, e);
            throw e;
        }
    }

    /**
     * Load with a fallback default if the file doesn't exist or fails to parse.
     */
    public static <T> T loadOrDefault(Path path, Codec<T> codec, T defaultValue) {
        try {
            if (Files.exists(path)) {
                return load(path, codec);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load from {}, using default value", path, e);
        }
        return defaultValue;
    }
}
