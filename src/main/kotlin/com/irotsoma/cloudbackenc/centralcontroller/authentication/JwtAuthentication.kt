/**
 * Created by irotsoma on 3/10/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication

import io.jsonwebtoken.Claims
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.*


/**
 *
 *
 * @author Justin Zak
 */

 class JwtAuthentication(tString: String) : Authentication {
    override fun setAuthenticated(isAuthenticated: Boolean) {
        authenticated
    }

    val ROLES_CLAIM_NAME = "roles"

    private var tokenString: String = tString
    private var tokenClaims: Claims? = null
    private var authorities: List<GrantedAuthority> = ArrayList(0)
    private var authenticated: Boolean = false


    override fun getAuthorities(): Collection<GrantedAuthority> {
        return authorities
    }

    override fun getCredentials(): Any {
        return ""
    }

    override fun getDetails(): Any {
        return tokenClaims!!.toString()
    }

    override fun getPrincipal(): Any {
        return tokenClaims!!.subject
    }

    override fun isAuthenticated(): Boolean {
        return authenticated
    }

    override fun getName(): String {
        return tokenClaims!!.subject
    }

    fun getToken(): String {
        return tokenString
    }
    fun setTokenClaims( tTokenClaims:Claims) {
        this.tokenClaims = tTokenClaims
        val roles = tokenClaims!!.get(ROLES_CLAIM_NAME, Collection::class.java)
        if (null != roles) {
            val authsList = ArrayList<GrantedAuthority>(roles.size)
            roles.mapTo(authsList) { SimpleGrantedAuthority(it.toString()) }
            authorities = Collections.unmodifiableList(authsList)
        } else {
            authorities = Collections.emptyList()
        }
    }
}