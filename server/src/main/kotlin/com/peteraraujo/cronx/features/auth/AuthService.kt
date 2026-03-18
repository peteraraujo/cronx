package com.peteraraujo.cronx.features.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.peteraraujo.cronx.Configuration
import java.util.*

object AuthService {
    private val isProduction = Configuration.appEnvVar.get() == "production"
    private val adminHash = Configuration.adminPasswordHash.get()
    private val jwtSecret = Configuration.jwtSecretVar.get()
    private val jwtIssuer = Configuration.jwtIssuerVar.get()
    private val jwtAudience = Configuration.jwtAudienceVar.get()



    fun verifyPassword(plainPassword: String): Boolean {
        if (isProduction) {
            if (adminHash.isBlank()) {
                throw IllegalStateException("${Configuration.adminPasswordHash.name} is missing in production")
            }
            return try {
                BCrypt.verifyer().verify(plainPassword.toCharArray(), adminHash).verified
            } catch (_: Exception) {
                false
            }
        } else {
            return plainPassword == "admin" || (adminHash.isNotBlank() && BCrypt.verifyer().verify(plainPassword.toCharArray(), adminHash).verified)
        }
    }

    fun generateToken(): String {
        return JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("role", "admin")
            .withExpiresAt(Date(System.currentTimeMillis() + Configuration.jwtRefreshMilis.get().toLong()))
            .sign(Algorithm.HMAC256(jwtSecret))
    }
}
