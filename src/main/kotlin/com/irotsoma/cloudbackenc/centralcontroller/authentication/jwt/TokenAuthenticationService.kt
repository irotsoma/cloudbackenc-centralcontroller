/**
 * Created by irotsoma on 3/17/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication.jwt

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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

    @Value("\${jwt.header}")
    private lateinit var header: String

    fun addAuthentication(response: HttpServletResponse, authentication: UserAuthentication): String {
        val user = authentication.getDetails()
        val token = tokenHandler.createTokenForUser(user)
        response.addHeader(header, token)
        return token
    }

    fun getAuthentication(request: HttpServletRequest): Authentication? {
        val token = request.getHeader(header)
        if (token != null) {
            val user = tokenHandler.parseUserFromToken(token)
            if (user != null) {
                return UserAuthentication(user)
            }
        }
        return null
    }
}