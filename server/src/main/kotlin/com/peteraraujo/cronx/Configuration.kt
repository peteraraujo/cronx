package com.peteraraujo.cronx

object Configuration {

    // App
    val appEnvVar = EnvVar("APP_ENV", "dev")
    val postingRate = EnvVar("APP_SLEEP_MILIS", "180000") // 3 minutes
    val adminPasswordHash = EnvVar("ADMIN_PASSWORD_HASH", default = "")

    // DB
    val dbUrlVar = EnvVar("DB_HOST", "localhost")
    val dbPortVar = EnvVar("DB_PORT", "3306")
    val dbNameVar = EnvVar("DB_NAME", "x_post_scheduler")

    val dbUserVar = EnvVar("DB_USER", "root")
    val dbPasswordVar = EnvVar("DB_PASSWORD", "")

    // App JWT
    val jwtSecretVar = EnvVar("JWT_SECRET", "jwt-secret")
    val jwtIssuerVar = EnvVar("JWT_ISSUER", "jwt-issuer")
    val jwtAudienceVar = EnvVar("JWT_AUDIENCE", "jwt-audience")
    val jwtRealmVar = EnvVar("JWT_REALM", "jwt-realm")

    val jwtRefreshMilis = EnvVar("JWT_REFRESH_MILIS", "86400000") // 1 day

    // X Credentials for App
    val xAppKeyVar = EnvVar("X_APP_KEY", "")
    val xAppSecretVar = EnvVar("X_APP_SECRET", "")

    // X Credentials for User
    val xUserAccessVar = EnvVar("X_USER_ACCESS", "")
    val xUserSecretVar = EnvVar("X_USER_SECRET", "")

    // X Config
    val xCreatePostRatePerDayVar = EnvVar("X_CREATE_POST_RATE_PER_DAY", "17")





    data class EnvVar(
        val name: String,
        val default: String
    ) {
        fun get(): String = System.getenv(name) ?: default
    }

}
