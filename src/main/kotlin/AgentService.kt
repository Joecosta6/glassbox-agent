package com.glassbox

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*

/**
 * Evento que mandamos al cliente.
 * type es el "event" del SSE (ej: "surface_update", "data_update", "begin_render")
 * payload es JSON serializado que el cliente parsea.
 */
data class AgentEvent(val type: String, val payload: String)

class AgentService {
    private val apiKey: String = System.getenv("GEMINI_API_KEY") ?: "PEGA_TU_KEY_AQUI"
    private val model = "gemini-2.5-flash"
    private val client = HttpClient(CIO)
    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun streamResponse(prompt: String, stateJson: String): Flow<AgentEvent> = flow {
        // 1. Evento "thinking" inmediato para que la UI muestre algo
        emit(AgentEvent("thinking", """{"message":"Pensando..."}"""))

        // 2. Llamada al LLM
        val rawResponse = try {
            callGemini(prompt, stateJson)
        } catch (e: Exception) {
            println("⚠️ Gemini falló: ${e.message}. Usando fallback.")
            fallbackUI(prompt)
        }

        // 3. Limpiar respuesta: a veces los LLM agregan ```json wrappers
        val cleanedResponse = rawResponse
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        // 4. Parsear y dividir en eventos AG-UI
        try {
            val parsed = json.parseToJsonElement(cleanedResponse).jsonObject

            parsed["surfaceUpdate"]?.let {
                emit(AgentEvent("surface_update", it.toString()))
                delay(150)
            }

            parsed["dataModelUpdate"]?.let {
                emit(AgentEvent("data_update", it.toString()))
                delay(150)
            }

            emit(AgentEvent("begin_render", """{"ready":true}"""))
        } catch (e: Exception) {
            println("⚠️ Parse error: ${e.message}")
            println("⚠️ Respuesta cruda: $cleanedResponse")
            // Recurrir a fallback si el parse falla
            val fallbackParsed = json.parseToJsonElement(fallbackUI(prompt)).jsonObject
            fallbackParsed["surfaceUpdate"]?.let {
                emit(AgentEvent("surface_update", it.toString()))
            }
            emit(AgentEvent("begin_render", """{"ready":true}"""))
        }
    }

    private suspend fun callGemini(prompt: String, stateJson: String): String {
        if (apiKey == "PEGA_TU_KEY_AQUI") {
            throw IllegalStateException("GEMINI_API_KEY no configurada. Exporta la variable de entorno.")
        }

        val userPrompt = """
            Usuario pregunta: "$prompt"
            Estado actual de la UI: $stateJson

            Genera la respuesta A2UI en JSON. SOLO JSON, sin markdown, sin explicación.
        """.trimIndent()

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        addJsonObject { put("text", "$A2UI_SYSTEM_PROMPT\n\n$userPrompt") }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.7)
                put("responseMimeType", "application/json")
            }
        }

        val response: HttpResponse = client.post(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        ) {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        val responseBody = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseBody).jsonObject

        val text = responseJson["candidates"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("content")
            ?.jsonObject?.get("parts")
            ?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("text")
            ?.jsonPrimitive?.content
            ?: throw Exception("No text in Gemini response: $responseBody")

        return text
    }

    /**
     * UI de respaldo si el LLM falla. CRÍTICO para no quemar el demo en vivo.
     */
    private fun fallbackUI(prompt: String): String {
        return """
        {
          "surfaceUpdate": {
            "components": {
              "root": { "type": "Column", "children": ["title", "msg", "btn"] },
              "title": { "type": "Text", "props": { "value": "Modo respaldo activo", "style": "title" } },
              "msg": { "type": "Text", "props": { "value": "Recibí: $prompt", "style": "body" } },
              "btn": { "type": "Button", "props": { "label": "Reintentar", "actionId": "retry", "style": "primary" } }
            }
          },
          "dataModelUpdate": { "data": {} }
        }
        """.trimIndent()
    }
}
