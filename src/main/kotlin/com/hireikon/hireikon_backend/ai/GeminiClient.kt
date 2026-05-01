package com.hireikon.hireikon_backend.ai

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hireikon.hireikon_backend.shared.AiProcessingException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.Base64

@Component
class GeminiClient(
    @Value("\${app.gemini.api-key}") private val apiKey: String,
    @Value("\${app.gemini.model}") private val model: String,
    private val objectMapper: ObjectMapper
) {
    private val webClient = WebClient.builder()
        .baseUrl("https://generativelanguage.googleapis.com/v1beta")
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .build()

    fun prompt(systemInstruction: String, userPrompt: String): String {
        val requestBody = mapOf(
            "system_instruction" to mapOf(
                "parts" to listOf(mapOf("text" to systemInstruction))
            ),
            "contents" to listOf(
                mapOf(
                    "role" to "user",
                    "parts" to listOf(mapOf("text" to userPrompt))
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to 0.2,       // low temperature = more deterministic JSON output
                "maxOutputTokens" to 8192
            )
        )

        return callGemini(requestBody)
    }

    fun promptWithPdf(systemInstruction: String, userPrompt: String, pdfBytes: ByteArray): String {
        val base64Pdf = Base64.getEncoder().encodeToString(pdfBytes)

        val requestBody = mapOf(
            "system_instruction" to mapOf(
                "parts" to listOf(mapOf("text" to systemInstruction))
            ),
            "contents" to listOf(
                mapOf(
                    "role" to "user",
                    "parts" to listOf(
                        mapOf(
                            "inline_data" to mapOf(
                                "mime_type" to "application/pdf",
                                "data" to base64Pdf
                            )
                        ),
                        mapOf("text" to userPrompt)
                    )
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to 0.1,       // even lower for parsing — we want exact data extraction
                "maxOutputTokens" to 8192
            )
        )

        return callGemini(requestBody)
    }

    private fun callGemini(requestBody: Map<String, Any>): String {
        val rawResponse = try {
            webClient.post()
                .uri("/models/$model:generateContent?key=$apiKey")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus({ it.isError }) { res ->
                    res.bodyToMono<String>().map { body ->
                        AiProcessingException("Gemini API error ${res.statusCode()}: $body")
                    }
                }
                .bodyToMono<String>()
                .block()
                ?: throw AiProcessingException("Gemini returned empty response")
        } catch (ex: AiProcessingException) {
            throw ex
        } catch (ex: Exception) {
            throw AiProcessingException("Failed to call Gemini API: ${ex.message}", ex)
        }

        // Extract the text content from Gemini's response envelope:
        // { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
        return try {
            val root = objectMapper.readValue(rawResponse, object : TypeReference<Map<String, Any>>() {})
            val candidates = root["candidates"] as List<*>
            val first = candidates[0] as Map<*, *>
            val content = first["content"] as Map<*, *>
            val parts = content["parts"] as List<*>
            val part = parts[0] as Map<*, *>
            part["text"] as String
        } catch (ex: Exception) {
            throw AiProcessingException("Failed to parse Gemini response structure: $rawResponse", ex)
        }
    }

    fun <T> parseJson(response: String, clazz: Class<T>): T {
        val cleaned = response
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
            // Gemini sometimes renders emails as markdown links: [email](mailto:email) -> email
            .replace(Regex("""\[([^]]+)]\(mailto:[^)]+\)""")) { it.groupValues[1] }
            // Also strip generic markdown links: [text](url) -> text
            .replace(Regex("""\[([^]]+)]\([^)]+\)""")) { it.groupValues[1] }

        return try {
            objectMapper.readValue(cleaned, clazz)
        } catch (ex: Exception) {
            throw AiProcessingException("Failed to parse AI response as JSON: $cleaned", ex)
        }
    }
}