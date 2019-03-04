/*
 * Copyright (C) 2016-2019  Irotsoma, LLC
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
 * Created by irotsoma on 3/20/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.authentication.jwt.TokenHandler
import com.irotsoma.cloudbackenc.centralcontroller.controllers.exceptions.AuthenticationException
import com.irotsoma.cloudbackenc.centralcontroller.data.TokenRepository
import com.irotsoma.cloudbackenc.centralcontroller.data.UserAccountRepository
import com.irotsoma.cloudbackenc.common.AuthenticationToken
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.*

/**
 * Rest controller that generates an auth token for a user to allow for background tasks to access the user
 * account without having to store the password (eg. FileController).
 *
 * @author Justin Zak
 */
@RequestMapping("\${centralcontroller.api.v1.path}/auth")
@RestController
class AuthTokenController {
    /** autowired jpa user repository */
    @Autowired
    lateinit var userRepository: UserAccountRepository
    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager
    @Autowired
    private lateinit var tokenHandler: TokenHandler
    @Autowired
    private lateinit var tokenRepository: TokenRepository

    /**
     * Get method available only to admin users that allows creating a login token for any user.
     *
     * @param username Username for which the token will be generated.
     */
    @RequestMapping("/{username}", method = [RequestMethod.GET], produces = ["application/json"])
    @Secured("ROLE_ADMIN")
    fun getTokenForOther(@PathVariable username: String): ResponseEntity<AuthenticationToken>{
        val token = tokenHandler.createTokenForUser(userAccountDetailsManager.loadUserByUsername(username) as User)
        if (token!=null) {
            return ResponseEntity(token, HttpStatus.OK)
        } else {
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    /**
     * GET method which retrieves an auth token for the currently logged in user.  Also can be used to refresh tokens
     * that have not expired yet by logging in with a valid token.
     */
    @RequestMapping(method = [RequestMethod.GET], produces = ["application/json"])
    @Secured ("ROLE_USER", "ROLE_ADMIN")
    fun getToken(): ResponseEntity<AuthenticationToken>{
        val authorizedUser: Authentication = SecurityContextHolder.getContext().authentication ?: return ResponseEntity(HttpStatus.UNAUTHORIZED)
        val token = tokenHandler.createTokenForUser(userAccountDetailsManager.loadUserByUsername(authorizedUser.name) as User)
        if (token!=null) {
            return ResponseEntity(token, HttpStatus.OK)
        } else {
            throw AuthenticationException()
        }
    }
    /**
     * Delete method available to all that allows for invalidating a specific token.
     *
     * @param token The token to be invalidated.
     */
    @RequestMapping(method = [RequestMethod.DELETE], params = ["token"])
    fun invalidateToken(@RequestParam("token") token: String): ResponseEntity<Any>{
        val tokenUuid = tokenHandler.parseUuidFromToken(token)?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val tokenObject=tokenRepository.findByTokenUuid(tokenUuid) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        tokenObject.valid = false
        tokenRepository.saveAndFlush(tokenObject)
        return ResponseEntity(HttpStatus.OK)
    }

    /**
     * Delete method to invalidate all tokens for the currently logged in user.
     */
    @RequestMapping(method = [RequestMethod.DELETE])
    @Secured ("ROLE_USER", "ROLE_ADMIN")
    fun invalidateTokens(): ResponseEntity<Any>{
        val authorizedUser: Authentication = SecurityContextHolder.getContext().authentication ?: return ResponseEntity(HttpStatus.UNAUTHORIZED)
        val userId = userRepository.findByUsername(authorizedUser.name)?.id ?: return ResponseEntity(HttpStatus.UNAUTHORIZED)
        tokenHandler.revokeTokensForUser(userId)
        return ResponseEntity(HttpStatus.OK)
    }
    /**
     * Delete method available only to admin users that allows invalidating the tokens of a given user.
     *
     * @param username Username for which the tokens will be invalidated.
     */
    @RequestMapping("/{username}", method = [RequestMethod.DELETE])
    @Secured ("ROLE_ADMIN")
    fun invalidateTokensForOther(@PathVariable username: String): ResponseEntity<Any>{
        val userId = userRepository.findByUsername(username)?.id ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        tokenHandler.revokeTokensForUser(userId)
        return ResponseEntity(HttpStatus.OK)
    }


}