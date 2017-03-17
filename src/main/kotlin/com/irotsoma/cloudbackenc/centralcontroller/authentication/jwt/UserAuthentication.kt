/**
 * Created by irotsoma on 3/17/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication.jwt

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User



/**
 *
 *
 * @author Justin Zak
 */
class UserAuthentication(private val user: User) : Authentication {
    private var authenticated = true

    override fun getName(): String {
        return user.username
    }

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return user.authorities
    }

    override fun getCredentials(): Any {
        return user.password
    }

    override fun getDetails(): User {
        return user
    }

    override fun getPrincipal(): Any {
        return user.username
    }

    override fun isAuthenticated(): Boolean {
        return authenticated
    }

    override fun setAuthenticated(authenticated: Boolean) {
        this.authenticated = authenticated
    }
}