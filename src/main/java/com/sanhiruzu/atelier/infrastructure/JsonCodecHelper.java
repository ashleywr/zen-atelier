package com.sanhiruzu.atelier.infrastructure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.util.GsonHelper;

/**
 * Helper utilities for JSON codec operations.
 * Simplifies serialization and deserialization of complex objects.
 */
public class JsonCodecHelper {

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
}
