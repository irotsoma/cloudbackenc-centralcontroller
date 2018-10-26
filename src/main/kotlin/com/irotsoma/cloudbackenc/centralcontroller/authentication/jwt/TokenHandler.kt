/*
 * Copyright (C) 2016-2018  Irotsoma, LLC
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
import com.irotsoma.cloudbackenc.centralcontroller.data.TokenObject
import com.irotsoma.cloudbackenc.centralcontroller.data.TokenRepository
import com.irotsoma.cloudbackenc.centralcontroller.data.UserAccountRepository
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
    /** autowired jpa user repository */
    @Autowired
    lateinit var userRepository: UserAccountRepository
    @Autowired
    private lateinit var userService: UserAccountDetailsManager
    @Autowired
    private lateinit var tokenRepository: TokenRepository
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
        } catch (e: UsernameNotFoundException){
            null
        }
        return if (user != null){
            //verify that user is not disabled
            if (user.isEnabled){
                user
            } else {
                null
            }
        } else {
            null
        }
    }
    /**
     * Attempts to parse the User information from a token.
     *
     * @param token The authentication token to parse
     * @return A UUID or null if the token is invalid.
     */
    fun parseUuidFromToken(token: String): UUID? {
        val id =try{
            Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .body
                    .id
        } catch(ignore: Exception) {
            null
        }
        return if (id != null){
            UUID.fromString(id)
        } else {
            null
        }
    }
    /**
     * Verifies that a token is not expired.
     *
     * Checks token value and database value.
     *
     * @param token The authentication token to parse
     * @return true if the token is expired or expiration date is missing or token is not in db, false if the token is still valid.
     */
    fun isTokenExpired(token: String):Boolean{
        val tokenBody = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .body
        val expiration: Date? = tokenBody.expiration
        return (expiration ?: Date(Long.MAX_VALUE) < Date()) && (tokenRepository.findByTokenUuid(UUID.fromString(tokenBody.id))?.expirationDate ?: Date(Long.MAX_VALUE) < Date())
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
        val id = UUID.randomUUID()
        val expiration = Date(now.time + expirationTime)
        //add token to DB
        val newToken = TokenObject(id, userRepository.findByUsername(user.username)!!.id, expiration, true)
        tokenRepository.saveAndFlush(newToken)
        return AuthenticationToken(Jwts.builder()
                .setId(id.toString())
                .setSubject(user.username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact(), expiration)
    }
    /**
     * Looks up the token in the database and returns whether the token is valid. If not in database returns false.
     *
     * @param token The authentication token to parse
     * @return true if the token is marked as valid in the database, false if marked not valid or doesn't exist
     */
    fun isTokenValid(token: String): Boolean{
        val id = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .body
                .id
        return tokenRepository.findByTokenUuid(UUID.fromString(id))?.valid == true
    }
    /**
     * Sets all tokens for this userId to not valid.
     *
     * @param userId The user account ID for which to revoke tokens.
     */
    fun revokeTokensForUser(userId: Long){
        val tokens = tokenRepository.findByUserId(userId)
        tokens.forEach{
            it.valid = false
        }
        if (tokens.isNotEmpty()) {
            tokenRepository.saveAll(tokens)
            tokenRepository.flush()
        }
    }
}