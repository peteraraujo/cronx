package com.peteraraujo.cronx.features.library

import com.peteraraujo.cronx.ApiRoutes
import com.peteraraujo.cronx.models.ContentRequest
import com.peteraraujo.cronx.models.TagMode
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureLibraryRoutes() {
    routing {
        authenticate("auth-jwt") {

            get(ApiRoutes.LIBRARY) {
                try {
                    // Params
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                    val query = call.request.queryParameters["query"]
                    val tags = call.request.queryParameters["tags"]?.split(",")?.filter { it.isNotBlank() }
                    val tagMode = call.request.queryParameters["tagMode"]?.let { TagMode.valueOf(it) }
                    val sort = call.request.queryParameters["sort"]

                    val libraryPage = LibraryService.getLibraryPage(page, pageSize, query, tags, tagMode, sort)
                    call.respond(libraryPage)
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
                }
            }

            get(ApiRoutes.LIBRARY_TAGS) {
                try {
                    val tags = LibraryService.getAllTags()
                    call.respond(tags)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
                }
            }

            post(ApiRoutes.LIBRARY) {
                val request = call.receive<ContentRequest>()
                val newId = LibraryService.createContent(request)
                call.respond(HttpStatusCode.Created, mapOf("id" to newId))
            }

            put("${ApiRoutes.LIBRARY}/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<ContentRequest>()
                LibraryService.updateContent(id, request)
                call.respond(HttpStatusCode.OK)
            }

            delete("${ApiRoutes.LIBRARY}/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                LibraryService.deleteContent(id)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
