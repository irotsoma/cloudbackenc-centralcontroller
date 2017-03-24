/*
 * Copyright (C) 2017  Irotsoma, LLC
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

/**
 * Created by irotsoma on 3/17/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication.jwt

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 *
 *
 * @author Justin Zak
 */
@Component
class TokenAuthenticationService {

    @Autowired
    private lateinit var tokenHandler: TokenHandler

    fun addAuthentication(response: HttpServletResponse, authentication: UserAuthentication): String {
        val user = authentication.details
        val token = tokenHandler.createTokenForUser(user)
        response.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
        return token
    }

    fun getAuthentication(request: HttpServletRequest): Authentication? {
        //TODO: Add expiration to tokens

        val token = request.getHeader(HttpHeaders.AUTHORIZATION)
        val splitToken = token.split(' ')
        if (splitToken.size == 2 && splitToken[0].toUpperCase() == "BEARER"){
            val user = tokenHandler.parseUserFromToken(splitToken[1])
            return UserAuthentication(user)
        } else {
            return null
        }
    }
}