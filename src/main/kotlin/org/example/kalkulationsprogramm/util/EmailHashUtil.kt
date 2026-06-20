package org.example.kalkulationsprogramm.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object EmailHashUtil {
    @JvmStatic
    fun hashAddress(address: String?): String? {
        if (address == null) return null
        return sha256Hex(address.trim().lowercase())
    }

    private fun sha256Hex(input: String): String =
        try {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray(StandardCharsets.UTF_8))
            digest.joinToString(separator = "") { "%02x".format(it) }
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
}
