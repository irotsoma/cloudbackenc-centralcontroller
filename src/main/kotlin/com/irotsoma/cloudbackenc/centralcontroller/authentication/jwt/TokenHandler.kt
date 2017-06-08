/*
 * Copyright (C) 2016-2017  Irotsoma, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

/*
 * Created by irotsoma on 3/17/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication.jwt

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.common.AuthenticationToken
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import java.util.*


/**
 * Authentication Token functionality
 *
 * @author Justin Zak
 */
@Component
class TokenHandler {

    /**
     * The encoding secret pulled from application settings and Base64 encoded.
     */
    @Value("\${jwt.secret}")
    private var secret: String? = null
        set(value) {
            Base64.getEncoder().encodeToString(value?.toByteArray())
        }
    /**
     * Expiration time in milliseconds for a token pulled from application settings or defaulted to 1 hour
     */
    @Value("\${jwt.expiration}")
    private var expirationTime: Long = 3600000L
    @Autowired
    private lateinit var userService: UserAccountDetailsManager

    /**
     * Attempts to parse the User information from a token.
     *
     * @param token The authentication token to parse
     * @return A Spring User object containing the user information loaded from the database or null if the token or username is invalid.
     */
    fun parseUserFromToken(token: String): User? {
        val username = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .body
                .subject ?: return null
        val user = try {
            userService.loadUserByUsername(username) as User
        } catch (e:UsernameNotFoundException){
            null
        }
        if (user != null){
            //verify that user is not disabled
            if (user.isEnabled){
                return user
            } else {
                return null
            }
        } else {
            return null
        }
    }

    /**
     * Verifies that a token is not expired.
     *
     * @param token The authentication token to parse
     * @return true if the token is expired, false if the token is still valid.
     */
    fun isTokenExpired(token: String):Boolean{
        val expiration = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .body
                .expiration
        return expiration < Date()
    }

    /**
     * Generates a new token for a user setting the expiration date/time based on the application settings.
     *
     * @param user A Spring User object containing the user for whom to generate the token
     */
    fun createTokenForUser(user: User): AuthenticationToken? {
        try{
            userService.loadUserByUsername(user.username)
        } catch (e: UsernameNotFoundException){
            return null
        }
        val now = Date()
        val expiration = Date(now.time + expirationTime)
        return AuthenticationToken(Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(user.username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact(), expiration)
    }
}