/**
 * Created by irotsoma on 3/10/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication

import io.jsonwebtoken.*
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import java.time.Instant
import java.util.*


/**
 *
 *
 * @author Justin Zak
 */
class JwtAuthenticationProvider(private var secret: String) : AuthenticationProvider {

    companion object {

        const val TOKEN_DURATION_SECONDS = (60 * 60 * 24 * 7).toLong() // 1 week
        const val TOKEN_CREATION_BUFFER_SECONDS = (60 * 5).toLong() // 5 min
        const val ISSUER_ID = "CentralController"
    }
    override fun authenticate(authentication: Authentication): Authentication {
        val jwtAuth = authentication as JwtAuthentication
        val jws: Jws<Claims>
        try {
            jws = Jwts.parser()
                    .requireIssuer(ISSUER_ID)
                    .setSigningKey(secret)
                    .parseClaimsJws(jwtAuth.getToken())
        } catch (ex: ExpiredJwtException) {
            throw BadCredentialsException("The token is not valid")
        } catch (ex: UnsupportedJwtException) {
            throw BadCredentialsException("The token is not valid")
        } catch (ex: MalformedJwtException) {
            throw BadCredentialsException("The token is not valid")
        } catch (ex: SignatureException) {
            throw BadCredentialsException("The token is not valid")
        } catch (ex: IllegalArgumentException) {
            throw BadCredentialsException("The token is not valid")
        }

        val checkDate = Date.from(Instant.now())
        val expirationDate = jws.body.expiration
        if (null == expirationDate || checkDate.after(expirationDate)) {
            throw BadCredentialsException("The token is expired")
        }
        val notBeforeDate = jws.body.notBefore
        if (null == notBeforeDate || checkDate.before(notBeforeDate)) {
            throw BadCredentialsException("The token not before date is invalid")
        }
        jwtAuth.setTokenClaims(jws.body)
        jwtAuth.isAuthenticated = true
        return jwtAuth
    }

    override fun supports(authentication: Class<*>): Boolean {
        return JwtAuthentication::class.java.isAssignableFrom(authentication)
    }

    fun createJWTToken(username: String): String {
        return Jwts.builder()
                .setSubject(username)
                .setIssuer(ISSUER_ID)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(TOKEN_DURATION_SECONDS)))
                .setNotBefore(Date.from(Instant.now().minusSeconds(TOKEN_CREATION_BUFFER_SECONDS)))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact()
    }


}