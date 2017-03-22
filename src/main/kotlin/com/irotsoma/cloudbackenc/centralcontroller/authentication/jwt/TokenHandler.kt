/**
 * Created by irotsoma on 3/17/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication.jwt

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import java.util.*


/**
 *
 *
 * @author Justin Zak
 */
@Component
class TokenHandler {

    @Value("\${jwt.secret}")
    private var secret: String? = null
        set(value) {
            Base64.getEncoder().encodeToString(value?.toByteArray())
        }
    @Value("\${jwt.expiration}")
    private var expiration: Long = 3600000L
    @Autowired
    private lateinit var userService: UserAccountDetailsManager

    fun parseUserFromToken(token: String): User {
        val username = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .body
                .subject
        return userService.loadUserByUsername(username) as User
    }

    fun createTokenForUser(user: User): String {
        val now = Date()
        val expiration = Date(now.time + expiration)
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(user.username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact()
    }
}