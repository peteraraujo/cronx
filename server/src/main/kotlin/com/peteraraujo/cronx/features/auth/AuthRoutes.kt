package com.peteraraujo.cronx.features.auth

import com.peteraraujo.cronx.ApiRoutes
import com.peteraraujo.cronx.models.AuthResponse
import com.peteraraujo.cronx.models.LoginRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

fun Application.configureAuthRoutes() {
    val logger = LoggerFactory.getLogger("AuthRoutes")

    routing {
        post(ApiRoutes.LOGIN) {
            try {
                val request = call.receive<LoginRequest>()

                if (AuthService.verifyPassword(request.passwordHash)) {
                    val token = AuthService.generateToken()
                    call.respond(AuthResponse(token))
                } else {
                    logger.warn("Failed login attempt")
                    call.respond(HttpStatusCode.Unauthorized, "Invalid Credentials")
                }
            } catch (e: Exception) {
                logger.error("Login error", e)
                call.respond(HttpStatusCode.BadRequest, "Invalid Request Format")
            }
        }
    }
}
