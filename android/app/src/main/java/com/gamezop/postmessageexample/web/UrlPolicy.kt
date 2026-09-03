package com.gamezop.postmessageexample.web

import com.gamezop.postmessageexample.model.GameMode
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object UrlPolicy {
    fun validationError(value: String, mode: GameMode = GameMode.INDIVIDUAL): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "Enter a Gamezop HTTPS URL"

        val uri = runCatching { URI(trimmed) }.getOrNull()
            ?: return "Enter a valid HTTPS URL"
        if (!uri.scheme.equals("https", ignoreCase = true)) return "Only HTTPS URLs are accepted"
        if (uri.host.isNullOrBlank()) return "The URL must include a host"
        if (uri.userInfo != null) return "URLs containing embedded credentials are not accepted"
        if (mode == GameMode.BATTLES) return battlesRoomDetailsError(uri)
        return null
    }

    private fun battlesRoomDetailsError(uri: URI): String? {
        val encodedValues = uri.rawQuery
            ?.split('&')
            ?.mapNotNull { component ->
                val separator = component.indexOf('=')
                val rawName = if (separator >= 0) component.substring(0, separator) else component
                if (decodeQueryComponent(rawName) != ROOM_DETAILS_PARAMETER) return@mapNotNull null
                if (separator >= 0) component.substring(separator + 1) else ""
            }
            .orEmpty()

        if (encodedValues.isEmpty()) return "Battles URL must include roomDetails"
        if (encodedValues.size > 1) return "Battles URL must include roomDetails only once"

        val encodedRoomDetails = decodeQueryComponent(encodedValues.single())
            ?: return "roomDetails is not valid URL encoding"
        if (encodedRoomDetails.isBlank()) return "roomDetails cannot be empty"

        val decodedRoomDetails = decodeBase64(encodedRoomDetails)
            ?: return "roomDetails must be valid Base64"
        val json = runCatching { JSONObject(decodedRoomDetails.decodeToString()) }.getOrNull()
            ?: return "Decoded roomDetails must be a JSON object"
        if (json.length() == 0) return "Decoded roomDetails cannot be empty"

        return null
    }

    private fun decodeQueryComponent(value: String): String? =
        runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrNull()

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeBase64(value: String): ByteArray? {
        val normalized = value.trim().let { it + "=".repeat((4 - it.length % 4) % 4) }
        return runCatching { Base64.Default.decode(normalized) }.getOrNull()
            ?: runCatching { Base64.UrlSafe.decode(normalized) }.getOrNull()
    }

    fun isAllowedNavigation(launchUrl: String, candidateUrl: String): Boolean {
        val launch = runCatching { URI(launchUrl) }.getOrNull() ?: return false
        val candidate = runCatching { URI(candidateUrl) }.getOrNull() ?: return false
        if (!candidate.scheme.equals("https", ignoreCase = true)) return false

        val candidateHost = candidate.host?.lowercase() ?: return false
        val launchHost = launch.host?.lowercase()
        return candidateHost == launchHost || isDocumentedGamezopHost(candidateHost)
    }

    fun isDocumentedGamezopHost(host: String): Boolean {
        val normalized = host.lowercase()
        return TRUSTED_DOMAIN_SUFFIXES.any { domain ->
            normalized == domain || normalized.endsWith(".$domain")
        }
    }

    private val TRUSTED_DOMAIN_SUFFIXES = setOf("gamezop.com", "umogames.com")
    private const val ROOM_DETAILS_PARAMETER = "roomDetails"
}
