package com.peteraraujo.cronx.features.scheduler

import com.peteraraujo.cronx.ApiRoutes
import com.peteraraujo.cronx.models.PresetRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configurePresetRoutes() {
    routing {
        authenticate("auth-jwt") {
            get("${ApiRoutes.PRESETS}") {
                val presets = PresetService.getAllPresets()
                call.respond(presets)
            }

            post("${ApiRoutes.PRESETS}") {
                val req = call.receive<PresetRequest>()
                val id = PresetService.createPreset(req)
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }

            put("${ApiRoutes.PRESETS}/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val req = call.receive<PresetRequest>()
                PresetService.updatePreset(id, req)
                call.respond(HttpStatusCode.OK)
            }

            delete("${ApiRoutes.PRESETS}/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                PresetService.deletePreset(id)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
