/**
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
 *
 *
 * @author Justin Zak
 */
@Component
class StatelessAuthenticationFilter : GenericFilterBean() {
    @Autowired
    private lateinit var tokenAuthenticationService: TokenAuthenticationService

    override fun doFilter(request: ServletRequest, response: ServletResponse, filterChain: FilterChain) {
        val authentication = tokenAuthenticationService.getAuthentication(request as HttpServletRequest)

        SecurityContextHolder.getContext().authentication = authentication
        if (authentication != null) {
            filterChain.doFilter(request, response)
            SecurityContextHolder.getContext().authentication = null
                
        }
    }
}