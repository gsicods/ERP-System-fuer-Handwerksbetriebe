package org.example.kalkulationsprogramm.config

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.BadJWTException
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.net.URL
import java.util.Date

@Component
class CloudflareAccessJwtFilter : OncePerRequestFilter() {
    @Value("\${cloudflare.access.enabled:false}")
    private var enabled: Boolean = false

    @Value("\${cloudflare.access.team-domain:}")
    private var teamDomain: String = ""

    @Value("\${cloudflare.access.application-aud:}")
    private var applicationAud: String = ""

    @Volatile
    private var processor: ConfigurableJWTProcessor<SecurityContext>? = null

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        if (!enabled) {
            chain.doFilter(request, response)
            return
        }

        val token = request.getHeader(HEADER)
        if (token.isNullOrBlank()) {
            log.warn("CF-Access-JWT fehlt fuer {}", request.requestURI)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "CF-Access-JWT fehlt")
            return
        }

        try {
            getProcessor().process(token, null)
            chain.doFilter(request, response)
        } catch (e: Exception) {
            log.warn("CF-Access-JWT ungueltig fuer {}: {}", request.requestURI, e.message)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "CF-Access-JWT ungueltig")
        }
    }

    @Throws(Exception::class)
    private fun getProcessor(): ConfigurableJWTProcessor<SecurityContext> {
        processor?.let { return it }
        synchronized(this) {
            processor?.let { return it }
            if (teamDomain.isBlank()) {
                throw IllegalStateException("cloudflare.access.team-domain muss gesetzt sein")
            }
            if (applicationAud.isBlank()) {
                throw IllegalStateException("cloudflare.access.application-aud muss gesetzt sein")
            }
            val jwkUrl = URL("https://$teamDomain/cdn-cgi/access/certs")
            val jwkSource = JWKSourceBuilder.create<SecurityContext>(jwkUrl).retrying(true).build()
            val jwtProcessor = DefaultJWTProcessor<SecurityContext>()
            jwtProcessor.jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource)
            val expectedIssuer = "https://$teamDomain"
            val expectedAud = applicationAud
            jwtProcessor.jwtClaimsSetVerifier = { claims: JWTClaimsSet, _: SecurityContext? ->
                if (expectedIssuer != claims.issuer) {
                    throw BadJWTException("Falscher Issuer: ${claims.issuer}")
                }
                if (claims.audience == null || !claims.audience.contains(expectedAud)) {
                    throw BadJWTException("Falsche Audience")
                }
                val exp = claims.expirationTime
                if (exp == null || exp.before(Date())) {
                    throw BadJWTException("Token abgelaufen")
                }
            }
            processor = jwtProcessor
            return jwtProcessor
        }
    }

    fun overrideProcessorForTesting(testProcessor: ConfigurableJWTProcessor<SecurityContext>) {
        processor = testProcessor
    }

    companion object {
        private const val HEADER = "Cf-Access-Jwt-Assertion"
        private val log = LoggerFactory.getLogger(CloudflareAccessJwtFilter::class.java)
    }
}
