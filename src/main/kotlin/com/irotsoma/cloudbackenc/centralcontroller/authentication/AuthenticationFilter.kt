/**
 * Created by irotsoma on 3/10/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication

import org.springframework.security.authentication.AuthenticationManager
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
class AuthenticationFilter(private var authenticationManager: AuthenticationManager) : GenericFilterBean() {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        val authHeader = httpRequest.getHeader("Authorization")
        var authInfo: Array<String>? = null
        if (null != authHeader) {
            authInfo = authHeader.split(" ").toTypedArray()
        }
        if (null != authInfo && authInfo.size == 2 && authInfo[0].toUpperCase().startsWith("BEARER")) {
            // retrieve authentication details from request
            val token = JwtAuthentication(authInfo[1])
            // Make sure we're authenticated
            try {
                val auth = authenticationManager.authenticate(token)
                SecurityContextHolder.getContext().authentication = auth
                httpResponse.setHeader("X-AuthToken", authInfo[1])
            } catch (ex: Exception) {
                println("Exception: " + ex.message)
                SecurityContextHolder.getContext().authentication = null
            }

            chain.doFilter(request, response)
            SecurityContextHolder.getContext().authentication = null
        } else {
            chain.doFilter(request, response)
        }
    }
}