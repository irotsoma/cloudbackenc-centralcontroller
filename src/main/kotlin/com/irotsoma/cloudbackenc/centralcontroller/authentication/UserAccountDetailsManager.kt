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
 * Created by irotsoma on 8/15/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication

import com.irotsoma.cloudbackenc.common.CloudBackEncRoles
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component

/**
 * User Account Details Service with Autowired Repositories
 *
 * For use by authentication in spring boot controllers
 *
 * @author Justin Zak
 */
@Component
open class UserAccountDetailsManager : UserDetailsService {
    @Autowired
    lateinit var userRepository: UserAccountRepository

    /**
     * Loads a user from the database by searching for the username and returns a Spring User object.
     *
     * @param username The username to search for in the database.
     * @returns An instance of Spring UserDetails with the user's information.
     */
    override fun loadUserByUsername(username: String): UserDetails {
        val userAccount = userRepository.findByUsername(username) ?: throw UsernameNotFoundException(" '$username'")
        return User(userAccount.username, userAccount.password, userAccount.enabled, true,true,true, userAccount.roles?.let {getRoles(it)})
    }

    /**
     * Translates the list of user roles from a list of CloudBackEncRoles to a list of GrantedAuthority
     *
     * @param roles The list of roles as instances of CloudBackEncRoles.
     * @returns The list of roles as a list of GrantedAuthority objects.
     */
    fun getRoles(roles: Collection<CloudBackEncRoles>) : List<GrantedAuthority>{
        var roleNames :Array<String> = emptyArray()
        for (role in roles){
            roleNames = roleNames.plus(role.name)
        }
        return AuthorityUtils.createAuthorityList(*roleNames)
    }
}