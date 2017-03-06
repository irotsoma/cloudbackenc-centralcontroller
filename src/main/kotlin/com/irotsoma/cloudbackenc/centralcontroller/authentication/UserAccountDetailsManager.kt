/*
 * Copyright (C) 2016  Irotsoma, LLC
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
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
/*
 * Created by irotsoma on 8/15/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication

import com.irotsoma.cloudbackenc.common.CloudBackEncRoles
import mu.KLogging
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
 * For use by basic authentication in spring boot controllers
 *
 * @author Justin Zak
 */
@Component
open class UserAccountDetailsManager : UserDetailsService {
    /** kotlin-logging implementation*/
    companion object: KLogging()
    @Autowired
    lateinit var userRepository: UserAccountRepository

    override fun loadUserByUsername(username: String): UserDetails {
        val userAccount = userRepository.findByUsername(username) ?: throw UsernameNotFoundException(" '$username'")
        return User(userAccount.username, userAccount.password, userAccount.enabled, true,true,true, userAccount.roles?.let {getRoles(it)})
    }
    fun getRoles(roles: Collection<CloudBackEncRoles>) : List<GrantedAuthority>{
        var roleNames :Array<String> = emptyArray()
        for (role in roles){
            roleNames = roleNames.plus(role.name)
        }
        return AuthorityUtils.createAuthorityList(*roleNames)
    }
}