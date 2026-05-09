package com.glassbox

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*

fun Application.configureRouting() {
    val agent = AgentService()

    routing {
        get("/") {
            call.respondText("Glassbox agent is alive 👁️")
        }

        // SSE endpoint principal: el cliente Android se conecta aquí.
        // Recibe el prompt y el estado actual como query params, y stremea eventos AG-UI.
        sse("/agent/stream") {
            val prompt = call.request.queryParameters["prompt"] ?: "hola"
            val stateJson = call.request.queryParameters["state"] ?: "{}"

            try {
                agent.streamResponse(prompt, stateJson).collect { event ->
                    send(ServerSentEvent(
                        event = event.type,
                        data = event.payload
                    ))
                }
            } catch (e: Exception) {
                send(ServerSentEvent(
                    event = "error",
                    data = """{"message":"${e.message?.replace("\"", "\\\"")}"}"""
                ))
            }
        }
    }
}
