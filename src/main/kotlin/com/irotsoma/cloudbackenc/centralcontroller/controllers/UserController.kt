/*
 * Copyright (C) 2016-2020  Irotsoma, LLC
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
 * Created by irotsoma on 9/22/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.controllers.exceptions.*
import com.irotsoma.cloudbackenc.centralcontroller.data.EncryptionProfileObject
import com.irotsoma.cloudbackenc.centralcontroller.data.EncryptionProfileRepository
import com.irotsoma.cloudbackenc.centralcontroller.data.UserAccountObject
import com.irotsoma.cloudbackenc.centralcontroller.data.UserAccountRepository
import com.irotsoma.cloudbackenc.centralcontroller.encryption.EncryptionExtensionRepository
import com.irotsoma.cloudbackenc.common.CloudBackEncRoles
import com.irotsoma.cloudbackenc.common.CloudBackEncUser
import com.irotsoma.cloudbackenc.common.UserAccountState
import com.irotsoma.cloudbackenc.common.encryption.*
import mu.KLogging
import org.apache.commons.validator.routines.EmailValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.context.annotation.Lazy
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder
import java.security.KeyPair
import java.util.*
import javax.crypto.SecretKey
import javax.mail.MessagingException
import javax.servlet.http.HttpServletResponse

/**
 * REST controller for managing users.
 *
 * @author Justin Zak
 * @property javaMailSender autowired Spring email sender service
 * @property userAccountDetailsManager autowired user account service
 * @property messageSource autowired message source for localization
 * @property apiPath autowired path for the current api version
 */
@Lazy
@RestController
@RequestMapping("\${centralcontroller.api.v1.path}/users")
class UserController {
    /** kotlin-logging implementation */
    private companion object: KLogging()
    @Autowired
    @Suppress("SpringJavaInjectionPointsAutowiringInspection") //suppress error caused by mail config being in external properties file
    private lateinit var javaMailSender: JavaMailSender
    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager
    @Autowired
    private lateinit var encryptionProfileRepository: EncryptionProfileRepository
    @Autowired
    private lateinit var messageSource: MessageSource
    @Value("\${centralcontroller.api.v1.path}")
    private lateinit var apiPath: String
    @Value("\${encryptionextensions.defaultExtensionUuid}")
    private lateinit var defaultEncryptionService: String
    @Value("\${spring.application.name}")
    private lateinit var appName: String
    @Autowired
    private lateinit var encryptionExtensionRepository: EncryptionExtensionRepository
    @Autowired
    private lateinit var userRepository: UserAccountRepository

    /** Post method for creating new users (Admin only) */
    @RequestMapping(method = [RequestMethod.POST], produces = ["application/json"])
    @Secured("ROLE_ADMIN")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@RequestBody user: CloudBackEncUser, uriComponentsBuilder: UriComponentsBuilder, response: HttpServletResponse) {
        //val authorizedUser = SecurityContextHolder.getContext().authentication
        //check to see if there is a duplicate user
        if (userRepository.findAllByUsername(user.username) != null){
            throw DuplicateUserException()
        }

        //TODO: check format of user ID which must contain only certain characters (probably alphanumeric plus _ and -)
        //TODO: check password format based on configurable pattern in properties file
        if (!user.email.isNullOrBlank()) {
            if (!EmailValidator.getInstance().isValid(user.email)) {
                throw InvalidEmailAddressException()
            }
        }
        //create and save new user
        val newUserAccount = UserAccountObject(user.username, user.password, user.email, user.state, user.roles)
        userRepository.saveAndFlush(newUserAccount)
        //send email to user
        val locale = LocaleContextHolder.getLocale()
        if (!user.email.isNullOrBlank()) {
            val mail = javaMailSender.createMimeMessage()
            try {
                val helper = MimeMessageHelper(mail, true)
                helper.setTo(user.email!!)
                helper.setSubject(messageSource.getMessage("centralcontroller.usercontroller.registration.email.subject", arrayOf(appName), locale))
                helper.setText(messageSource.getMessage("centralcontroller.usercontroller.registration.email.body", arrayOf(user.username), locale))
                javaMailSender.send(mail)
            } catch (e: MessagingException) {
                e.printStackTrace() //TODO: create a custom exception here
            } catch (e: NoSuchMessageException){
                e.printStackTrace() //TODO: create a custom exception here
            }
        }
    }

    /**
     * PUT method for updating user information (Admin or affected user only)
     */
    @RequestMapping(method = [RequestMethod.PUT], produces = ["application/json"])
    @Secured("ROLE_USER","ROLE_ADMIN")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    fun updateUser(@RequestBody updatedUser:CloudBackEncUser, response: HttpServletResponse) : CloudBackEncUser{
        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUserAccount = userAccountDetailsManager.loadUserByUsername(authorizedUser.name)
        //authorized user requesting the update must either be the user in the request or be an admin
        val isAdmin = currentUserAccount.authorities.contains(GrantedAuthority{CloudBackEncRoles.ROLE_ADMIN.name})
        if (!((updatedUser.username == authorizedUser.name) || (isAdmin))) {
            throw UserUnauthorizedException()
        }
        //
        try{
            val userToUpdate = userRepository.findAllByUsername(updatedUser.username) ?: throw CloudBackEncUserNotFound()
            //update password if included
            if (updatedUser.password != CloudBackEncUser.PASSWORD_MASKED){
                userToUpdate.password = updatedUser.password
            }
            //update email if included
            if (updatedUser.email != null && updatedUser.email != "") {
                //TODO: verify email format
                userToUpdate.email = updatedUser.email
            }
            //update enabled state if different
            if (updatedUser.state != userToUpdate.state) {
                //should use delete method so throw invalid request exception
                if (updatedUser.state == UserAccountState.DELETED) {
                    throw InvalidRequestException()
                }
                //if trying to change the state of an existing user to active, must be admin
                if (updatedUser.state == UserAccountState.ACTIVE && !isAdmin) {
                    throw UserUnauthorizedException()
                }
                userToUpdate.state = updatedUser.state
            }
            //update roles if not empty and different from existing
            if (updatedUser.roles.isNotEmpty()) {
                //can't assign test role
                if (updatedUser.roles.contains(CloudBackEncRoles.ROLE_TEST) && userToUpdate.roles?.contains(CloudBackEncRoles.ROLE_TEST) != true) {
                    throw InvalidRequestException()
                }
                //only admins can add the admin role
                if (updatedUser.roles.contains(CloudBackEncRoles.ROLE_ADMIN) && userToUpdate.roles?.contains(CloudBackEncRoles.ROLE_ADMIN) != true && !isAdmin) {
                    throw UserUnauthorizedException()
                }
                userToUpdate.roles = updatedUser.roles
            }
            userRepository.saveAndFlush(userToUpdate)
        } catch(e: UsernameNotFoundException){
            throw CloudBackEncUserNotFound()
        }
        val locale = LocaleContextHolder.getLocale()
        //TODO: check format of user ID which must contain only certain characters (probably alphanumeric plus _ and -)
        //TODO: check password format based on configurable pattern in properties file
        if (!updatedUser.email.isNullOrBlank()) {
            if (!EmailValidator.getInstance().isValid(updatedUser.email)) {
                throw InvalidEmailAddressException()
            }
        }
        //send email to user
        if (!updatedUser.email.isNullOrBlank()) {
            val mail = javaMailSender.createMimeMessage()
            try {
                val helper = MimeMessageHelper(mail, true)
                helper.setTo(updatedUser.email!!)
                helper.setSubject(messageSource.getMessage("centralcontroller.usercontroller.update.email.subject", arrayOf(appName), locale))
                helper.setText(messageSource.getMessage("centralcontroller.usercontroller.update.email.body", arrayOf(updatedUser.username, appName), locale))
                javaMailSender.send(mail)
            } catch (e: MessagingException) {
                e.printStackTrace() //TODO: create a custom exception here
            } catch (e: NoSuchMessageException){
                e.printStackTrace() //TODO: create a custom exception here
            } catch (e: MailSendException){
                e.printStackTrace() //TODO: create a custom exception here
            }
        }
        return updatedUser.maskedPasswordInstance()
    }

    /**
     * DELETE method for deleting a user from the system (Admin only)
     */
    @RequestMapping("/{username}", method = [RequestMethod.DELETE], produces = ["application/json"])
    @Secured("ROLE_ADMIN")
    @ResponseStatus(HttpStatus.OK)
    fun deleteUser(@PathVariable username: String){
        val requestedUser = userRepository.findByUsername(username) ?: throw CloudBackEncUserNotFound()
        userRepository.delete(requestedUser)
    }

    /**
     * GETs the user's information (Admin or affected user only)
     */
    @RequestMapping(value=["/{username}", "/", ""], method = [RequestMethod.GET], produces = ["application/json"])
    @Secured("ROLE_USER","ROLE_ADMIN")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    fun getUser(@PathVariable(required=false) username: String?, response: HttpServletResponse) : CloudBackEncUser {
        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUserAccount = userAccountDetailsManager.loadUserByUsername(authorizedUser.name)
        val requestedUser = userRepository.findByUsername(if (username.isNullOrBlank()) {currentUserAccount.username} else {username})
        //if the user is an admin then check if the requested user is found otherwise if not admin respond with
        //forbidden even if the requested user is not found to prevent non-admins from spamming to get valid usernames
        if (currentUserAccount.authorities.contains(GrantedAuthority{CloudBackEncRoles.ROLE_ADMIN.name})){
            if (requestedUser == null){
                throw CloudBackEncUserNotFound()
            }
        } else if (requestedUser?.username != authorizedUser.name) {
            throw UserUnauthorizedException()
        }
        return requestedUser!!.cloudBackEncUser()
    }

    /**
     * Sets up or allows a user to change their default encryption settings profile (Admin or affected user only)
     */
    @RequestMapping(value=["/{username}/encryption","/encryption"], method = [RequestMethod.POST], produces = ["application/json"])
    @Secured("ROLE_USER","ROLE_ADMIN")
    @ResponseStatus(HttpStatus.CREATED)
    fun createEncryptionProfile(@PathVariable(required=false) username: String?, @RequestBody profile: EncryptionProfile, response: HttpServletResponse){
        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUserAccount = userAccountDetailsManager.loadUserByUsername(authorizedUser.name)
        val requestedUser = userRepository.findByUsername(if (username.isNullOrBlank()){currentUserAccount.username}else{username})
        //if the user is an admin then check if the requested user is found otherwise if not admin respond with
        //forbidden even if the requested user is not found to prevent non-admins from spamming to get valid usernames
        if (currentUserAccount.authorities.contains(GrantedAuthority{CloudBackEncRoles.ROLE_ADMIN.name})){
            if (requestedUser == null){
                throw CloudBackEncUserNotFound()
            }
        } else if (requestedUser?.username != authorizedUser.name) {
            throw UserUnauthorizedException()
        }
        //validate combinations of information in encryption profile
        if (profile.encryptionType == EncryptionAlgorithmTypes.SYMMETRIC){
            if (profile.encryptionBlockSize !in (profile.encryptionAlgorithm as EncryptionSymmetricEncryptionAlgorithms).validBlockSizes()) {
                throw EncryptionException("Invalid block size: ${profile.encryptionBlockSize} for ${profile.encryptionAlgorithm.value}")
            }
            if ((profile.encryptionAlgorithm as EncryptionSymmetricEncryptionAlgorithms).keyAlgorithm() != (profile.encryptionKeyAlgorithm as EncryptionSymmetricKeyAlgorithms)){
                throw EncryptionException("Encryption algorithm / key algorithm mismatch: ${profile.encryptionAlgorithm.value} / ${profile.encryptionKeyAlgorithm.value}")
            }
            if (profile.encryptionKeySize !in (profile.encryptionKeyAlgorithm as EncryptionSymmetricKeyAlgorithms).validKeyLengths()){
                throw EncryptionException("Invalid key size for key algorithm ${profile.encryptionKeyAlgorithm.value}: ${profile.encryptionKeySize}")
            }
        } else if (profile.encryptionType == EncryptionAlgorithmTypes.ASYMMETRIC) {
            if ((profile.encryptionAlgorithm as EncryptionAsymmetricEncryptionAlgorithms).keyAlgorithm() != (profile.encryptionKeyAlgorithm as EncryptionAsymmetricKeyAlgorithms)) {
                throw EncryptionException("Encryption algorithm / key algorithm mismatch: ${profile.encryptionAlgorithm.value} / ${profile.encryptionKeyAlgorithm.value}")
            }
        } else  {
            throw EncryptionException ("Encryption type is not supported: ${profile.encryptionType.value}")
        }

        val encryptionUuid = profile.encryptionServiceUuid?: UUID.fromString(defaultEncryptionService)
        val encryptionFactory = (encryptionExtensionRepository.extensions[encryptionUuid])?.getDeclaredConstructor()?.newInstance() as EncryptionFactory? ?: encryptionExtensionRepository.extensions[UUID.fromString(encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid)]?.getDeclaredConstructor()?.newInstance() as EncryptionFactory? ?: throw EncryptionException("Unable to create the requested or the default encryption factory.")
        var secretKey: SecretKey? = null
        var keyPair: KeyPair? = null
        when {
            profile.encryptionType == EncryptionAlgorithmTypes.SYMMETRIC -> secretKey = encryptionFactory.encryptionKeyService.generateSymmetricKey(profile.encryptionKeyAlgorithm as EncryptionSymmetricKeyAlgorithms,
                    profile.encryptionKeySize?:(profile.encryptionAlgorithm as EncryptionSymmetricKeyAlgorithms).validKeyLengths().last())
            profile.encryptionType == EncryptionAlgorithmTypes.ASYMMETRIC -> keyPair = encryptionFactory.encryptionKeyService.generateAsymmetricKeys(profile.encryptionKeyAlgorithm as EncryptionAsymmetricKeyAlgorithms,
                    profile.encryptionKeySize ?: (profile.encryptionAlgorithm as EncryptionAsymmetricKeyAlgorithms).validKeyLengths().last())
            else -> throw EncryptionException("Only symmetric encryption is currently supported.")
        }

        val encryptionProfileObject = EncryptionProfileObject(encryptionUuid ,
                profile.encryptionType.value,
                profile.encryptionAlgorithm.value,
                profile.encryptionKeyAlgorithm.value,
                profile.encryptionBlockSize,
                secretKey?.encoded ?: keyPair?.private?.encoded ?:
                    throw EncryptionException("Error generating keys. Null returned by key generator. Algorithm ${profile.encryptionKeyAlgorithm.value}; Encryption service UUID: $encryptionUuid"),
                keyPair?.public?.encoded)
        val encryptionProfileId = encryptionProfileRepository.saveAndFlush(encryptionProfileObject)
        requestedUser!!.defaultEncryptionProfile = encryptionProfileId
        userRepository.saveAndFlush(requestedUser)
    }
}