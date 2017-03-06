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
 * Created by irotsoma on 9/22/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccount
import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.controllers.exceptions.CloudBackEncUserNotFound
import com.irotsoma.cloudbackenc.centralcontroller.controllers.exceptions.DuplicateUserException
import com.irotsoma.cloudbackenc.centralcontroller.controllers.exceptions.InvalidEmailAddressException
import com.irotsoma.cloudbackenc.common.CloudBackEncRoles
import com.irotsoma.cloudbackenc.common.CloudBackEncUser
import mu.KLogging
import org.apache.commons.validator.routines.EmailValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder
import javax.mail.MessagingException
import javax.servlet.http.HttpServletResponse
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.AdditionalClassPartsProvider


@RestController
@RequestMapping("/users")
class UserController {
    /** kotlin-logging implementation*/
    companion object: KLogging()

    @Suppress("SpringKotlinAutowiring") //TODO: see if this can be removed.  Seems to be giving autowired error, but it works fine
    @Autowired
    private lateinit var javaMailSender: JavaMailSender
    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager
    @Autowired
    lateinit var messageSource: MessageSource

    @RequestMapping(method = arrayOf(RequestMethod.POST), produces = arrayOf("application/json"))
    @Secured("ROLE_ADMIN")
    fun createUser(@RequestBody user: CloudBackEncUser, uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<AdditionalClassPartsProvider.None>{
        val authorizedUser = SecurityContextHolder.getContext().authentication
        val locale = LocaleContextHolder.getLocale()
        try {
            val currentUser = userAccountDetailsManager.loadUserByUsername(authorizedUser.name)
            if (!currentUser.authorities.contains(GrantedAuthority { CloudBackEncRoles.ROLE_ADMIN.name })){
                return ResponseEntity(HttpStatus.FORBIDDEN)
            }
        } catch (e: Exception){
            return ResponseEntity(HttpStatus.FORBIDDEN)
        }

        //check to see if there is a duplicate user
        if (userAccountDetailsManager.userRepository.findByUsername(user.username) != null){
            throw DuplicateUserException()
        }

        //TODO: check format of user ID which must contain only certain characters (probably alphanumeric plus _ and -)
        //TODO: check password format based on configurable pattern in properties file
        if (user.email != null) {
            if (!EmailValidator.getInstance().isValid(user.email)) {
                throw InvalidEmailAddressException()
            }
        }
        //create and save new user
        val newUserAccount = UserAccount(user.username, user.password,user.email, user.enabled, user.roles)
        userAccountDetailsManager.userRepository.saveAndFlush(newUserAccount)
        //TODO: email user
        if (user.email != null) {
            val mail = javaMailSender.createMimeMessage()
            try {
                val helper = MimeMessageHelper(mail, true)
                helper.setTo(user.email)
                helper.setSubject(messageSource.getMessage("centralcontroller.user.controller.registration.email.subject", null, locale))
                helper.setText(messageSource.getMessage("centralcontroller.user.controller.registration.email.body", arrayOf(user.username), locale))
            } catch (e: MessagingException) {
                e.printStackTrace() //TODO: create a custom exception here
            } finally {
            }
            javaMailSender.send(mail)
        }
        //TODO: add email validation step

        //return the path to the user id
        val responseLocation = uriComponentsBuilder.path("/users/{userId}").buildAndExpand(user.username)
        val headers = HttpHeaders()
        headers.location = responseLocation.toUri()
        return ResponseEntity(headers, HttpStatus.CREATED)
    }

    @RequestMapping(method = arrayOf(RequestMethod.PUT), produces = arrayOf("application/json"))
    fun updateUser(@RequestBody updatedUser:CloudBackEncUser, response: HttpServletResponse) : ResponseEntity<CloudBackEncUser>{
        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUser = userAccountDetailsManager.loadUserByUsername(authorizedUser.name)
        //authorized user requesting the update must either be the user in the request or be an admin
        if ((updatedUser.username != authorizedUser.name) || (!currentUser.authorities.contains(GrantedAuthority{CloudBackEncRoles.ROLE_ADMIN.name})))
        {
            return ResponseEntity(null, HttpStatus.NOT_FOUND)
        }
        //
        try{
            val userToUpdate = userAccountDetailsManager.userRepository.findByUsername(updatedUser.username) ?: throw CloudBackEncUserNotFound()
            if (updatedUser.password != CloudBackEncUser.PASSWORD_MASKED){
                userToUpdate.password = updatedUser.password
            }
            if (updatedUser.email != null){
                userToUpdate.email = updatedUser.email
            }
            userAccountDetailsManager.userRepository.saveAndFlush(userToUpdate)
        } catch(e: UsernameNotFoundException){
            throw CloudBackEncUserNotFound()
        }

        //TODO: check format of user ID which must contain only certain characters (probably alphanumeric plus _ and -)
        //TODO: check email address format
        //TODO: check password format based on configurable pattern in properties file

        //TODO: email user
        return ResponseEntity(updatedUser.maskedPasswordInstance(), HttpStatus.OK)
    }


    @RequestMapping("/{userId}", method = arrayOf(RequestMethod.DELETE), produces = arrayOf("application/json"))
    @Secured("ROLE_ADMIN")
    fun deleteUser(@PathVariable userId: String?, @RequestBody updatedUser:CloudBackEncUser) : ResponseEntity<AdditionalClassPartsProvider.None>{
        val authorizedUser = SecurityContextHolder.getContext().authentication.name


        //TODO implement this
        return ResponseEntity(HttpStatus.OK)
    }
    @RequestMapping("/{userId}", method = arrayOf(RequestMethod.GET), produces = arrayOf("application/json"))
    @Secured("ROLE_ADMIN")
    fun getUser(@PathVariable userId: String?, @RequestBody user:CloudBackEncUser) : ResponseEntity<AdditionalClassPartsProvider.None>{
        val authorizedUser = SecurityContextHolder.getContext().authentication.name


        //TODO implement this
        return ResponseEntity(HttpStatus.OK)
    }
}