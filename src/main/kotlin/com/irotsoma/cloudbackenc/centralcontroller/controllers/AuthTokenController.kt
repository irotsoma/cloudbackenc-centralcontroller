/**
 * Created by irotsoma on 3/20/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.authentication.jwt.TokenHandler
import com.irotsoma.cloudbackenc.common.AuthenticationToken
import com.irotsoma.cloudbackenc.common.CloudBackEncUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/**
 *
 *
 * @author Justin Zak
 */
@RestController
class AuthTokenController {

    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager
    @Autowired
    private lateinit var tokenHandler: TokenHandler

    @RequestMapping("auth", method = arrayOf(RequestMethod.POST), produces = arrayOf("application/json"))
    @Secured("ROLE_ADMIN")
    fun getTokenForOther(@RequestBody user: CloudBackEncUser): ResponseEntity<AuthenticationToken>{
        val token = tokenHandler.createTokenForUser(userAccountDetailsManager.loadUserByUsername(user.username) as User)
        return ResponseEntity(AuthenticationToken(token), HttpStatus.OK)
    }
    @RequestMapping("auth", method = arrayOf(RequestMethod.GET), produces = arrayOf("application/json"))
    fun getToken(): ResponseEntity<AuthenticationToken>{
        val authorizedUser = SecurityContextHolder.getContext().authentication
        val token = tokenHandler.createTokenForUser(userAccountDetailsManager.loadUserByUsername(authorizedUser.name) as User)
        return ResponseEntity(AuthenticationToken(token), HttpStatus.OK)
    }

}