/**
 * Created by irotsoma on 3/10/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.GenericFilterBean
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 *
 *
 * @author Justin Zak
 */
class TokenFilter(private var jwtAuthenticationProvider: JwtAuthenticationProvider) : GenericFilterBean() {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        val createdAuth = SecurityContextHolder.getContext().authentication
        if (null != createdAuth && createdAuth.isAuthenticated) {
            if (createdAuth.principal is UserAccount) {
                val theUser = createdAuth.principal as UserAccount
                val jwtToken = jwtAuthenticationProvider.createJWTToken(theUser.username)
                httpResponse.setHeader("X-AuthToken", jwtToken)
            }
        }
        chain.doFilter(request, response)
    }
}