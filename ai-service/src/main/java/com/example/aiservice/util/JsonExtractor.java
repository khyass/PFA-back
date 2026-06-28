package com.example.aiservice.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for extracting and cleaning JSON from LLM responses.
 * Handles common issues: markdown fences, preamble text, trailing content.
 */
@Slf4j
public final class JsonExtractor {

    private JsonExtractor() {}

    /**
     * Extracts a JSON array from raw LLM output.
     * Strips markdown fences, finds the outermost [...] block.
     *
     * @param raw the raw LLM response
     * @return cleaned JSON array string, or null if not found
     */
    public static String extractJsonArray(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String cleaned = stripMarkdownFences(raw).trim();

        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');

        if (start == -1) {
            log.warn("No JSON array found in response (length={})", raw.length());
            return null;
        }

        // Handle unclosed arrays (common LLM issue)
        if (end == -1 || end <= start) {
            String partial = cleaned.substring(start).trim();
            log.warn("JSON array not properly closed, attempting to fix");
            // Try to close the array
            if (partial.endsWith(",")) {
                partial = partial.substring(0, partial.length() - 1);
            }
            return partial + "]";
        }

        return cleaned.substring(start, end + 1);
    }

    /**
     * Extracts a JSON object from raw LLM output.
     * Strips markdown fences, finds the outermost {...} block.
     *
     * @param raw the raw LLM response
     * @return cleaned JSON object string, or null if not found
     */
    public static String extractJsonObject(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String cleaned = stripMarkdownFences(raw).trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start == -1 || end == -1 || start >= end) {
            log.warn("No JSON object found in response (length={})", raw.length());
            return null;
        }

        return cleaned.substring(start, end + 1);
    }

    /**
     * Strips markdown code fences (```json ... ``` or ``` ... ```) from a string.
     */
    public static String stripMarkdownFences(String raw) {
        if (raw == null) return null;
        return raw
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
    }
}
