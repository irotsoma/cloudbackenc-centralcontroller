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

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User



/**
 * Implementation of Spring Authentication class from a Spring User object
 *
 * @property user An instance of Spring User that holds the user information.
 * @author Justin Zak
 */
class UserAuthentication(private val user: User) : Authentication {

    /**
     * Stores the authentication status of the user.
     *
     * Default = enabled status of user
     */
    private var authenticated = user.isEnabled

    /**
     * returns the username
     */
    override fun getName(): String {
        return user.username
    }
    /**
     * returns the authorities
     */
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return user.authorities
    }
    /**
     * returns the password
     */
    override fun getCredentials(): Any {
        return user.password
    }
    /**
     * returns the User object
     */
    override fun getDetails(): User {
        return user
    }
    /**
     * returns the username
     */
    override fun getPrincipal(): Any {
        return user.username
    }

    /**
     * returns the authentication status
     */
    override fun isAuthenticated(): Boolean {
        return authenticated
    }

    /**
     * sets the authentication status to a new value
     */
    override fun setAuthenticated(authenticated: Boolean) {
        this.authenticated = authenticated
    }
}