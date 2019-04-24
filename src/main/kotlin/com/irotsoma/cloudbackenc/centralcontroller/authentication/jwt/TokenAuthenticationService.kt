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
 * Created by irotsoma on 3/17/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication.jwt

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

/**
 * Validates that a token in the Authorization Bearer header is valid for the user.
 *
 * @author Justin Zak
 */
@Lazy
@Service
class TokenAuthenticationService {

    @Autowired
    private lateinit var jwtSettings: JwtSettings

    @Autowired
    private lateinit var tokenHandler: TokenHandler

    /**
     * Validates authentication token in an Authentication: Bearer header.
     *
     * @param request REST request to validate.
     * @return A Spring Authentication object containing the user information or null if the token is invalid or expired or the user is invalid
     */
    fun getAuthentication(request: HttpServletRequest): Authentication? {
        if (jwtSettings.disabled){
            //if the jwt functionality is disabled in properties just skip this
            return null
        } else {
            //parse authorization bearer header
            val token = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
            val splitToken = token.split(' ')
            if (splitToken.size == 2 && splitToken[0].toUpperCase() == "BEARER") {
                //verify token is not expired
                if (!tokenHandler.isTokenExpired(splitToken[1]) && tokenHandler.isTokenValid(splitToken[1])) {
                    //parse user from token
                    val user = tokenHandler.parseUserFromToken(splitToken[1]) ?: return null
                    return UserAuthentication(user)
                } else {
                    return null
                }
            } else {
                return null
            }
        }
    }
}