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
 * Created by irotsoma on 7/13/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceAuthenticationCompleteListener
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceFactoryRepository
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.UserCloudServiceRepository
import com.irotsoma.cloudbackenc.centralcontroller.controllers.exceptions.InvalidCloudServiceUUIDException
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceException
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceFactory
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceUser
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.net.MalformedURLException
import java.net.URL
import java.util.*

/**
 * Rest Controller that takes an instance of CloudServiceUser as JSON, calls the login method of the requested cloud
 * service as identified in the URL by UUID, and returns an instance of CloudServiceUser with the userId and login
 * token.
 *
 * Use POST method to /cloud_service/login/{uuid} where {uuid} is the uuid returned from the cloud service list
 * controller for the extension.
 *
 * @author Justin Zak
 */
@RestController
class CloudServiceLoginController {
    /** kotlin-logging implementation*/
    companion object: KLogging()

    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager
    @Autowired
    private lateinit var cloudServiceFactoryRepository: CloudServiceFactoryRepository
    @Autowired
    private lateinit var userCloudServiceRepository: UserCloudServiceRepository
    @Autowired
    lateinit var messageSource: MessageSource

    /**
     * Calls the login function of the cloud service.
     *
     * @param uuid The UUID of the cloud service extension.
     * @param user A CloudServiceUser object that contains the user information for the cloud service.
     * @return CloudServiceUser.STATE value indicating the login state.
     */
    @RequestMapping("cloud-services/login/{uuid}", method = [RequestMethod.POST], produces = ["application/json"])
    fun login(@PathVariable(value="uuid")uuid: UUID, @RequestBody user: CloudServiceUser) : ResponseEntity<CloudServiceUser.STATE> {
        val locale = LocaleContextHolder.getLocale()
        val cloudServiceFactory = cloudServiceFactoryRepository.extensions[uuid]?.factoryClass ?: throw InvalidCloudServiceUUIDException()
        val authenticationService = cloudServiceFactory.newInstance().authenticationService
        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUser = userAccountDetailsManager.userRepository.findByUsername(authorizedUser.name) ?: throw CloudServiceException("Authenticated user could not be found.")
        val response : CloudServiceUser.STATE
        //debug message: ignore and let it default to null if URL is invalid or missing
        try {
            URL(user.authorizationCallbackURL)
        } catch (e: MalformedURLException){
            logger.warn{messageSource.getMessage("centralcontroller.cloudservices.error.callback.invalid", emptyArray(), locale)}
        }
        //launch extension's login service
        //TODO: figure out how to make this async with a timeout
        try {
            val listener = CloudServiceAuthenticationCompleteListener(currentUser.cloudBackEncUser(), if (user.username.isEmpty()){null}else{user.username}, userAccountDetailsManager, userCloudServiceRepository)
            authenticationService.cloudServiceAuthenticationRefreshListener = listener
            response = authenticationService.login(user, currentUser.cloudBackEncUser())
        } catch (e:Exception ){
            logger.warn{"${messageSource.getMessage("centralcontroller.cloudservices.error.login", emptyArray(), locale)} Error during login process. ${e.message}"}
            throw CloudServiceException(e.message, e)
        }
        val status: HttpStatus =
                when(response){
                    CloudServiceUser.STATE.LOGGED_IN -> HttpStatus.OK
                    CloudServiceUser.STATE.AWAITING_AUTHORIZATION -> HttpStatus.ACCEPTED
                    else -> HttpStatus.BAD_REQUEST
                }

        return ResponseEntity(response, status)

    }

    @RequestMapping("cloud-services/logout/{uuid}", method = arrayOf(RequestMethod.GET), produces = arrayOf("application/json"))
    fun logout(@PathVariable(value="uuid")uuid: UUID) : ResponseEntity<CloudServiceUser.STATE> {
        val locale = LocaleContextHolder.getLocale()
        val cloudServiceFactory = cloudServiceFactoryRepository.extensions[uuid]?.factoryClass ?: throw InvalidCloudServiceUUIDException()
        val authenticationService = cloudServiceFactory.newInstance().authenticationService
        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUser = userAccountDetailsManager.userRepository.findByUsername(authorizedUser.name) ?: throw CloudServiceException("Authenticated user could not be found.")

        try {
            val listener = CloudServiceAuthenticationCompleteListener(currentUser.cloudBackEncUser(), null, userAccountDetailsManager, userCloudServiceRepository)
            authenticationService.cloudServiceAuthenticationRefreshListener = listener
            authenticationService.logout(CloudServiceUser("", null, uuid.toString(), null), currentUser.cloudBackEncUser())
        } catch (e:Exception ){
            logger.warn{"${messageSource.getMessage("centralcontroller.cloudservices.error.login", emptyArray(), locale)} Error during login process. ${e.message}"}
            throw CloudServiceException(e.message, e)
        }
        return ResponseEntity(CloudServiceUser.STATE.LOGGED_OUT, HttpStatus.OK)
    }

}