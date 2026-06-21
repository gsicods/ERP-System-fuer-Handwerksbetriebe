package org.example.kalkulationsprogramm.service

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPairGenerator
import java.security.Security
import java.security.interfaces.ECPublicKey
import java.util.Base64
import kotlin.math.max
import kotlin.math.min

object VapidKeyGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        Security.addProvider(BouncyCastleProvider())
        val parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1")
        val keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC")
        keyPairGenerator.initialize(parameterSpec)
        val keyPair = keyPairGenerator.generateKeyPair()

        val publicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keyPair.public.encoded)
        val privateKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keyPair.private.encoded)

        val ecPub = keyPair.public as ECPublicKey
        val x = ecPub.w.affineX.toByteArray()
        val y = ecPub.w.affineY.toByteArray()
        val rawPoint = ByteArray(65)
        rawPoint[0] = 0x04
        System.arraycopy(x, max(0, x.size - 32), rawPoint, 1 + max(0, 32 - x.size), min(32, x.size))
        System.arraycopy(y, max(0, y.size - 32), rawPoint, 33 + max(0, 32 - y.size), min(32, y.size))
        val rawPubKey = Base64.getUrlEncoder().withoutPadding().encodeToString(rawPoint)

        println("=== VAPID Key Pair Generated ===")
        println()
        println("Public Key (for application-local.properties):")
        println(publicKey)
        println()
        println("Private Key (for application-local.properties - NEVER commit!):")
        println(privateKey)
        println()
        println("Raw Public Key (65-byte uncompressed EC point, for browser debug):")
        println(rawPubKey)
        println()
        println("Add to application-local.properties:")
        println("  push.vapid.public-key=$publicKey")
        println("  push.vapid.private-key=$privateKey")
        println("  push.vapid.subject=mailto:your@email.com")
    }
}
