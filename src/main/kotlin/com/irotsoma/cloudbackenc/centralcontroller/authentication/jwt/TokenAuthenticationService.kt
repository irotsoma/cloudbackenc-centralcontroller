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