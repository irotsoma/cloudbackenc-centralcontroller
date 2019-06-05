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
 * Created by irotsoma on 7/13/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceAuthenticationCompleteListener
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceFactoryRepository
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceUserRequestRepository
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServicesSettings
import com.irotsoma.cloudbackenc.centralcontroller.controllers.exceptions.InvalidCloudServiceUuidException
import com.irotsoma.cloudbackenc.centralcontroller.data.UserAccountRepository
import com.irotsoma.cloudbackenc.common.cloudservices.*
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Lazy
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.net.MalformedURLException
import java.net.URL
import java.util.*

/**
 * Rest Controller that takes an instance of CloudServiceAuthenticationRequest as JSON, calls the login method of the requested cloud
 * service as identified in the URL by UUID, and returns an instance of CloudServiceAuthenticationRequest with the userId and login
 * token.
 *
 * Use POST method to /cloud_service/login/{uuid} where {uuid} is the uuid returned from the cloud service list
 * controller for the extension.
 *
 * @author Justin Zak
 * @property userAccountDetailsManager Autowired instance of user account manager
 * @property cloudServiceFactoryRepository Repository that holds the installed cloud service extensions.
 * @property cloudServiceUserRepository JPA repository that represents the configured login information for cloud services.
 * @property messageSource Autowired MessageSource for localization strings.
 */
@Lazy
@RequestMapping("\${centralcontroller.api.v1.path}/cloud-services")
@RestController
class CloudServiceLoginController {
    /** kotlin-logging implementation*/
    private companion object: KLogging()

    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager
    @Autowired
    private lateinit var cloudServiceFactoryRepository: CloudServiceFactoryRepository
    @Autowired
    private lateinit var cloudServiceUserRepository: CloudServiceUserRequestRepository
    @Autowired
    lateinit var messageSource: MessageSource
    @Autowired
    private lateinit var userRepository: UserAccountRepository
    @Autowired
    private lateinit var cloudServicesSettings:CloudServicesSettings

    /**
     * Rest POST service which calls the login function of the cloud service.
     *
     * @param uuid The UUID of the cloud service extension.
     * @param user A CloudServiceAuthenticationRequest object that contains the user information for the cloud service.
     * @return CloudServiceAuthenticationState value indicating the login state.
     */
    @RequestMapping("/login/{uuid}", method = [RequestMethod.POST], produces = ["application/json"])
    @Secured("ROLE_USER","ROLE_ADMIN")
    fun login(@PathVariable(value="uuid")uuid: UUID, @RequestBody user: CloudServiceAuthenticationRequest) : ResponseEntity<CloudServiceAuthenticationResponse> {
        val locale = LocaleContextHolder.getLocale()
        val cloudServiceFactory = cloudServiceFactoryRepository.extensions[uuid]  ?: throw InvalidCloudServiceUuidException()
        val cloudServiceFactoryInstance = cloudServiceFactory.getDeclaredConstructor().newInstance() as CloudServiceFactory
        if (!cloudServicesSettings.cloudServicesSecrets[uuid.toString()]?.clientId.isNullOrBlank()){
            cloudServiceFactoryInstance.additionalSettings["clientId"] = cloudServicesSettings.cloudServicesSecrets[uuid.toString()]?.clientId!!
        }
        if (!cloudServicesSettings.cloudServicesSecrets[uuid.toString()]?.clientSecret.isNullOrBlank()){
            cloudServiceFactoryInstance.additionalSettings["clientSecret"] = cloudServicesSettings.cloudServicesSecrets[uuid.toString()]?.clientSecret!!
        }
        val authenticationService = cloudServiceFactoryInstance.authenticationService
        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUser = userRepository.findByUsername(authorizedUser.name) ?: throw CloudServiceException("Authenticated user could not be found.")
        val response : CloudServiceAuthenticationResponse
        //debug message: ignore and let it default to null if URL is invalid or missing
        try {
            URL(user.authorizationCallbackURL)
        } catch (e: MalformedURLException){
            logger.warn{messageSource.getMessage("centralcontroller.cloudservices.error.callback.invalid", emptyArray(), locale)}
        }
        //launch extension's login service
        val listener = CloudServiceAuthenticationCompleteListener(currentUser.cloudBackEncUser(), userAccountDetailsManager, userRepository, if (user.username.isEmpty()) {
            null
        } else {
            user.username
        }, cloudServiceUserRepository)
        authenticationService.cloudServiceAuthenticationRefreshListener = listener

        //TODO: make this async with a timeout
        try {
            response = authenticationService.login(CloudServiceAuthenticationRequest(user.username, user.password, uuid.toString(), user.authorizationCallbackURL, user.replyWithAuthorizationUrl), currentUser.cloudBackEncUser())
        } catch (e: Exception) {
            logger.warn { "${messageSource.getMessage("centralcontroller.cloudservices.error.login", emptyArray(), locale)} Error during login process. ${e.message}" }
            throw CloudServiceException(e.message, e)
        }
        val status: HttpStatus =
                when (response.cloudServiceAuthenticationState) {
                    CloudServiceAuthenticationState.LOGGED_IN -> HttpStatus.OK
                    CloudServiceAuthenticationState.AWAITING_AUTHORIZATION -> HttpStatus.ACCEPTED
                    else -> HttpStatus.BAD_REQUEST
                }
        return ResponseEntity(response, status)
    }
    /**
     * Rest POST service which calls the logout function of the cloud service.
     *
     * @param uuid The UUID of the cloud service extension.
     * @param user A CloudServiceAuthenticationRequest object that contains the user information for the cloud service.
     * @return CloudServiceAuthenticationState value indicating the login state.
     */
    @RequestMapping("/logout/{uuid}", method = [RequestMethod.POST], produces = ["application/json"])
    @Secured("ROLE_USER","ROLE_ADMIN")
    fun logout(@PathVariable(value="uuid")uuid: UUID, @RequestBody user: CloudServiceAuthenticationRequest) : ResponseEntity<CloudServiceAuthenticationState> {
        val locale = LocaleContextHolder.getLocale()
        val cloudServiceFactory = cloudServiceFactoryRepository.extensions[uuid] ?: throw InvalidCloudServiceUuidException()
        val cloudServiceFactoryInstance = cloudServiceFactory.getDeclaredConstructor().newInstance() as CloudServiceFactory
        if (!cloudServicesSettings.cloudServicesSecrets[uuid.toString()]?.clientId.isNullOrBlank()){
            cloudServiceFactoryInstance.additionalSettings["clientId"] = cloudServicesSettings.cloudServicesSecrets[uuid.toString()]?.clientId!!
        }
        if (!cloudServicesSettings.cloudServicesSecrets[uuid.toString()]?.clientSecret.isNullOrBlank()){
            cloudServiceFactoryInstance.additionalSettings["clientSecret"] = cloudServicesSettings.cloudServicesSecrets[uuid.toString()]?.clientSecret!!
        }
        val authenticationService = cloudServiceFactoryInstance.authenticationService
        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUser = userRepository.findByUsername(authorizedUser.name) ?: throw CloudServiceException("Authenticated user could not be found.")

        try {
            val listener = CloudServiceAuthenticationCompleteListener(currentUser.cloudBackEncUser(), userAccountDetailsManager, userRepository, null, cloudServiceUserRepository)
            authenticationService.cloudServiceAuthenticationRefreshListener = listener
            authenticationService.logout(user, currentUser.cloudBackEncUser())
        } catch (e:Exception ){
            logger.warn{"${messageSource.getMessage("centralcontroller.cloudservices.error.login", emptyArray(), locale)} Error during login process. ${e.message}"}
            throw CloudServiceException(e.message, e)
        }
        return ResponseEntity(CloudServiceAuthenticationState.LOGGED_OUT, HttpStatus.OK)
    }

}