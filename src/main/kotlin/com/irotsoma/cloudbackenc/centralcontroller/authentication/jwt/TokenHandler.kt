/*
 * Copyright (C) 2016-2020  Irotsoma, LLC
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
import com.irotsoma.cloudbackenc.common.encryption.EncryptionException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.util.*
import javax.annotation.PostConstruct


/**
 * Authentication Token functionality
 *
 * @author Justin Zak
 */
@Lazy
@Component
class TokenHandler {
    //The encoding keys info pulled from application settings
    @Autowired
    private lateinit var jwtSettings: JwtSettings
    @Autowired
    private lateinit var userRepository: UserAccountRepository
    @Autowired
    private lateinit var userService: UserAccountDetailsManager
    @Autowired
    private lateinit var tokenRepository: TokenRepository
    private var keyStore: KeyStore? = null
    private var privateKey: PrivateKey? = null

    @PostConstruct
    private fun getKeyStore(){
        try {
            keyStore = KeyStore.getInstance(jwtSettings.keyStoreType)
            keyStore?.load(FileInputStream(jwtSettings.keyStore), jwtSettings.keyStorePassword?.toCharArray())
        } catch (e: Exception){
            throw EncryptionException("Unable to load JWT keystore.", e)
        }
        if (keyStore?.containsAlias(jwtSettings.keyAlias) != true){
            throw EncryptionException("Key alias does not exist in JWT keystore.")
        }
        privateKey = keyStore!!.getKey(jwtSettings.keyAlias,jwtSettings.keyPassword?.toCharArray()) as PrivateKey

    }

    /**
     * Attempts to parse the User information from a token.
     *
     * @param token The authentication token to parse
     * @return A Spring User object containing the user information loaded from the database or null if the token or username is invalid.
     */
    fun parseUserFromToken(token: String): User? {
        val username = Jwts.parser()
                .setSigningKey(privateKey)
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
                    .setSigningKey(privateKey)
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
                .setSigningKey(privateKey)
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
        //set the expiration, default to 1 hour from now
        val expiration = Date(now.time + ((jwtSettings.expiration ?: 3600) * 1000))
        //add token to DB
        val newToken = TokenObject(id, userRepository.findByUsername(user.username)!!.id, expiration, true)
        tokenRepository.saveAndFlush(newToken)

        return AuthenticationToken(Jwts.builder()
                .claim("roles", user.authorities.map{ it.toString() })
                .setId(id.toString())
                .setSubject(user.username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.values().find { it.jcaName == jwtSettings.algorithm }, privateKey)
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
                .setSigningKey(privateKey)
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