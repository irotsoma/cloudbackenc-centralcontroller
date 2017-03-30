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
 * Created by irotsoma on 3/20/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.authentication.jwt.TokenHandler
import com.irotsoma.cloudbackenc.centralcontroller.controllers.exceptions.AuthenticationException
import com.irotsoma.cloudbackenc.common.AuthenticationToken
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/**
 * Rest controller that generates an auth token for a user to allow for background tasks to access the user
 * account without having to store the password (eg. FileController).
 *
 * @author Justin Zak
 */
@RequestMapping("auth")
@RestController
class AuthTokenController {

    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager
    @Autowired
    private lateinit var tokenHandler: TokenHandler

    /**
     * Get method available only to admin users that allows creating a login token for any user.
     *
     * @param username Username for which the token will be generated.
     */
    @RequestMapping("/{username}", method = arrayOf(RequestMethod.GET), produces = arrayOf("application/json"))
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
    @RequestMapping(method = arrayOf(RequestMethod.GET), produces = arrayOf("application/json"))
    fun getToken(): ResponseEntity<AuthenticationToken>{
        val authorizedUser = SecurityContextHolder.getContext().authentication
        val token = tokenHandler.createTokenForUser(userAccountDetailsManager.loadUserByUsername(authorizedUser.name) as User)
        if (token!=null) {
            return ResponseEntity(token, HttpStatus.OK)
        } else {
            throw AuthenticationException()
        }
    }

}