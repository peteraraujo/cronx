package com.peteraraujo.cronx.features.scheduler

import com.peteraraujo.cronx.Configuration
import com.peteraraujo.cronx.db.ScheduleQueueTable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.net.URLEncoder
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

object Publisher {

    private val httpClient = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    suspend fun publish(queueId: String): String {
        val appKey = Configuration.xAppKeyVar.get()
        val appSecret = Configuration.xAppSecretVar.get()
        val userAccessToken = Configuration.xUserAccessVar.get()
        val userAccessTokenSecret = Configuration.xUserSecretVar.get()
        val isProduction = Configuration.appEnvVar.get() == "production"

        if (isProduction) {
            if (appKey.isBlank() || appSecret.isBlank() || userAccessToken.isBlank() || userAccessTokenSecret.isBlank()) {
                throw IllegalArgumentException("X API Credentials not configured in environment")
            }
        }

        if (!RateLimitService.canPost("x_create_post", Configuration.xCreatePostRatePerDayVar.get().toInt(), 1440)) {
            throw IllegalStateException("Rate limit exceeded. Post failed.")
        }

        val text = newSuspendedTransaction(Dispatchers.IO) {
            val scheduleRow = ScheduleQueueTable.selectAll()
                .where { ScheduleQueueTable.id eq queueId }
                .single()
            scheduleRow[ScheduleQueueTable.bodyText]
        }

        return if (isProduction) {
            postToX(appKey, appSecret, userAccessToken, userAccessTokenSecret, text)
        } else {
            mockPostToX(appKey.ifBlank { "MOCK_KEY" }, text)
        }
    }

    private suspend fun postToX(
        appKey: String,
        appSecret: String,
        userAccessToken: String,
        userAccessTokenSecret: String,
        text: String
    ): String {
        val url = "https://api.x.com/2/tweets"
        val method = "POST"

        val nonce = UUID.randomUUID().toString().replace("-", "")
        val timestamp = (System.currentTimeMillis() / 1000).toString()

        // JSON body is NOT included in OAuth 1.0a signature for application/json
        val oauthParams = mapOf(
            "oauth_consumer_key" to appKey,
            "oauth_nonce" to nonce,
            "oauth_signature_method" to "HMAC-SHA1",
            "oauth_timestamp" to timestamp,
            "oauth_token" to userAccessToken,
            "oauth_version" to "1.0"
        )

        val signature = generateSignature(method, url, oauthParams, appSecret, userAccessTokenSecret)
        val authorizationHeader = buildAuthHeader(oauthParams, signature)

        val response = httpClient.post(url) {
            header(HttpHeaders.Authorization, authorizationHeader)
            contentType(ContentType.Application.Json)
            setBody(TweetRequest(text = text))
        }

        if (response.status != HttpStatusCode.Created && response.status != HttpStatusCode.OK) {
            val errorBody = response.body<String>()
            throw RuntimeException("X API Error: ${response.status} - $errorBody")
        }

        val responseBody = response.body<TweetResponse>()
        return responseBody.data.id
    }

    private fun mockPostToX(consumerKey: String, text: String): String {
        println("[MOCK] Posting to X using Key: $consumerKey")
        println("[MOCK] Payload: '$text'")
        return "mock_x_id_${System.currentTimeMillis()}"
    }

    private fun generateSignature(
        method: String,
        url: String,
        params: Map<String, String>,
        consumerSecret: String,
        tokenSecret: String
    ): String {
        val sortedParams = params.toSortedMap()
            .map { "${percentEncode(it.key)}=${percentEncode(it.value)}" }
            .joinToString("&")

        val baseString = "$method&${percentEncode(url)}&${percentEncode(sortedParams)}"
        val signingKey = "${percentEncode(consumerSecret)}&${percentEncode(tokenSecret)}"

        return hmacSha1(baseString, signingKey)
    }

    private fun buildAuthHeader(params: Map<String, String>, signature: String): String {
        val allParams = params + ("oauth_signature" to signature)
        return "OAuth " + allParams.map { "${percentEncode(it.key)}=\"${percentEncode(it.value)}\"" }
            .joinToString(", ")
    }

    private fun hmacSha1(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        val secretKeySpec = SecretKeySpec(key.toByteArray(), "HmacSHA1")
        mac.init(secretKeySpec)
        val signedBytes = mac.doFinal(data.toByteArray())
        return Base64.getEncoder().encodeToString(signedBytes)
    }

    private fun percentEncode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }

    @Serializable
    private data class TweetRequest(val text: String)

    @Serializable
    private data class TweetResponse(val data: TweetData)

    @Serializable
    private data class TweetData(val id: String, val text: String)
}
