package com.example.aiservice.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonExtractorTest {

    @Test
    void extractJsonArray_withCleanJson_returnsArray() {
        String raw = "[{\"offerId\": \"123\", \"score\": 85}]";
        String result = JsonExtractor.extractJsonArray(raw);
        assertThat(result).isEqualTo("[{\"offerId\": \"123\", \"score\": 85}]");
    }

    @Test
    void extractJsonArray_withMarkdownFences_stripsAndReturns() {
        String raw = "```json\n[{\"offerId\": \"abc\", \"score\": 90}]\n```";
        String result = JsonExtractor.extractJsonArray(raw);
        assertThat(result).isEqualTo("[{\"offerId\": \"abc\", \"score\": 90}]");
    }

    @Test
    void extractJsonArray_withPreambleText_extractsArray() {
        String raw = "Here are the results:\n[{\"offerId\": \"x\", \"score\": 50}]\nHope this helps!";
        String result = JsonExtractor.extractJsonArray(raw);
        assertThat(result).isEqualTo("[{\"offerId\": \"x\", \"score\": 50}]");
    }

    @Test
    void extractJsonArray_withNull_returnsNull() {
        assertThat(JsonExtractor.extractJsonArray(null)).isNull();
    }

    @Test
    void extractJsonArray_withBlank_returnsNull() {
        assertThat(JsonExtractor.extractJsonArray("   ")).isNull();
    }

    @Test
    void extractJsonArray_withNoArray_returnsNull() {
        assertThat(JsonExtractor.extractJsonArray("No JSON here")).isNull();
    }

    @Test
    void extractJsonObject_withCleanJson_returnsObject() {
        String raw = "{\"technicalQuestions\": [], \"behavioralQuestions\": []}";
        String result = JsonExtractor.extractJsonObject(raw);
        assertThat(result).isEqualTo("{\"technicalQuestions\": [], \"behavioralQuestions\": []}");
    }

    @Test
    void extractJsonObject_withMarkdownFences_stripsAndReturns() {
        String raw = "```json\n{\"key\": \"value\"}\n```";
        String result = JsonExtractor.extractJsonObject(raw);
        assertThat(result).isEqualTo("{\"key\": \"value\"}");
    }

    @Test
    void extractJsonObject_withSurroundingText_extractsObject() {
        String raw = "Here is the response:\n{\"technicalQuestions\": [{\"question\": \"q1\", \"answerOutline\": \"a1\"}], \"behavioralQuestions\": []}\nDone.";
        String result = JsonExtractor.extractJsonObject(raw);
        assertThat(result).contains("\"technicalQuestions\"");
        assertThat(result).startsWith("{");
        assertThat(result).endsWith("}");
    }

    @Test
    void extractJsonObject_withNull_returnsNull() {
        assertThat(JsonExtractor.extractJsonObject(null)).isNull();
    }

    @Test
    void extractJsonObject_withNoObject_returnsNull() {
        assertThat(JsonExtractor.extractJsonObject("just text")).isNull();
    }

    @Test
    void stripMarkdownFences_removesJsonFences() {
        String raw = "```json\n{\"x\": 1}\n```";
        assertThat(JsonExtractor.stripMarkdownFences(raw)).isEqualTo("{\"x\": 1}");
    }

    @Test
    void stripMarkdownFences_removesPlainFences() {
        String raw = "```\n[1,2,3]\n```";
        assertThat(JsonExtractor.stripMarkdownFences(raw)).isEqualTo("[1,2,3]");
    }

    @Test
    void stripMarkdownFences_withNull_returnsNull() {
        assertThat(JsonExtractor.stripMarkdownFences(null)).isNull();
    }
}
