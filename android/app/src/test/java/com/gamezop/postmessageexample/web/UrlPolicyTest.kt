package com.gamezop.postmessageexample.web

import com.gamezop.postmessageexample.model.GameMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder
import java.util.Base64

class UrlPolicyTest {
    @Test
    fun `accepts valid https URL`() {
        assertNull(UrlPolicy.validationError("https://1234.play.gamezop.com/game"))
    }

    @Test
    fun `rejects missing invalid and insecure URLs`() {
        assertTrue(UrlPolicy.validationError("") != null)
        assertTrue(UrlPolicy.validationError("not a url") != null)
        assertTrue(UrlPolicy.validationError("http://play.gamezop.com") != null)
        assertTrue(UrlPolicy.validationError("https://user:secret@example.com") != null)
    }

    @Test
    fun `battles accepts URL encoded standard Base64 room details JSON`() {
        val roomDetails = """{"roomId":"ABC01"}"""
        val encoded = URLEncoder.encode(
            Base64.getEncoder().encodeToString(roomDetails.toByteArray()),
            Charsets.UTF_8.name(),
        )

        assertNull(
            UrlPolicy.validationError(
                "https://1234.play.gamezop.com/game?roomDetails=$encoded",
                GameMode.BATTLES,
            ),
        )
    }

    @Test
    fun `battles accepts unpadded URL safe Base64 room details JSON`() {
        val roomDetails = """{"roomId":"ABC01","allowBots":false}"""
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(roomDetails.toByteArray())

        assertNull(
            UrlPolicy.validationError(
                "https://1234.play.gamezop.com/game?foo=bar&roomDetails=$encoded",
                GameMode.BATTLES,
            ),
        )
    }

    @Test
    fun `battles rejects missing invalid duplicate and non object room details`() {
        val nonObject = Base64.getEncoder().encodeToString("[]".toByteArray())
        val emptyObject = Base64.getEncoder().encodeToString("{}".toByteArray())

        assertTrue(UrlPolicy.validationError("https://play.gamezop.com/game", GameMode.BATTLES) != null)
        assertTrue(UrlPolicy.validationError("https://play.gamezop.com/game?roomDetails=%%%", GameMode.BATTLES) != null)
        assertTrue(UrlPolicy.validationError("https://play.gamezop.com/game?roomDetails=not-base64", GameMode.BATTLES) != null)
        assertTrue(UrlPolicy.validationError("https://play.gamezop.com/game?roomDetails=$nonObject", GameMode.BATTLES) != null)
        assertTrue(UrlPolicy.validationError("https://play.gamezop.com/game?roomDetails=$emptyObject", GameMode.BATTLES) != null)
        assertTrue(
            UrlPolicy.validationError(
                "https://play.gamezop.com/game?roomDetails=$nonObject&roomDetails=$nonObject",
                GameMode.BATTLES,
            ) != null,
        )
    }

    @Test
    fun `individual does not require room details`() {
        assertNull(
            UrlPolicy.validationError(
                "https://1234.play.gamezop.com/game",
                GameMode.INDIVIDUAL,
            ),
        )
    }

    @Test
    fun `allows launch host and documented Gamezop hosts`() {
        val launch = "https://partner-games.example/game"
        assertTrue(UrlPolicy.isAllowedNavigation(launch, "https://partner-games.example/next"))
        assertTrue(UrlPolicy.isAllowedNavigation(launch, "https://1234.play.gamezop.com/game"))
        assertTrue(UrlPolicy.isAllowedNavigation(launch, "https://games.umogames.com/game"))
    }

    @Test
    fun `blocks off-host insecure and deceptive hosts`() {
        val launch = "https://partner-games.example/game"
        assertFalse(UrlPolicy.isAllowedNavigation(launch, "https://example.org"))
        assertFalse(UrlPolicy.isAllowedNavigation(launch, "http://partner-games.example"))
        assertFalse(UrlPolicy.isAllowedNavigation(launch, "https://gamezop.com.evil.example"))
    }
}
