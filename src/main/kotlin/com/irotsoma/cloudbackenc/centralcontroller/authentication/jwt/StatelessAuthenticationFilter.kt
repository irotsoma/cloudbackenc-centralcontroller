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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest


/**
 * Authentication filter that validates a token using a TokenAuthenticationService
 *
 * @author Justin Zak
 */
@Component
class StatelessAuthenticationFilter : GenericFilterBean() {
    @Autowired
    private lateinit var tokenAuthenticationService: TokenAuthenticationService

    /**
     * Calls authentication service to verify if the request has a valid token and then continues the filter chain.
     *
     * @param request REST request to check for authentication header.
     * @param response REST response object
     * @param filterChain Filter chain for the current transaction.
     */
    override fun doFilter(request: ServletRequest, response: ServletResponse, filterChain: FilterChain) {
        //check for a valid authentication header and set it in the security context if it exists
        val authentication = tokenAuthenticationService.getAuthentication(request as HttpServletRequest)
        if (authentication != null) {
            SecurityContextHolder.getContext().authentication = authentication
        }
        //call the filter chain to continue the chain of authentication filters
        filterChain.doFilter(request, response)
    }
}