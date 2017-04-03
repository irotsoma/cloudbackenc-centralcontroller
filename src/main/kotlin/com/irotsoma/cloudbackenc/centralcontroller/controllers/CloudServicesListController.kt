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
 * Created by irotsoma on 7/12/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountRepository
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceFactoryRepository
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.UserCloudServiceRepository
import com.irotsoma.cloudbackenc.centralcontroller.controllers.exceptions.CloudBackEncUserNotFound
import com.irotsoma.cloudbackenc.common.CloudBackEncRoles
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceException
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceExtensionList
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.*


/**
 * REST Controller for getting a list of cloud service extensions currently installed.
 *
 * Use: GET method to /cloud-services to get all installed extensions.
 * Use: GET method to /cloud-services/{username} to get installed extensions with with the user has previously interacted.
 *
 * @author Justin Zak
 */
@RestController
class CloudServicesListController {
    /** kotlin-logging implementation*/
    companion object: KLogging()

    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager
    @Autowired
    private lateinit var userRepository: UserAccountRepository
    @Autowired
    private lateinit var cloudServiceFactoryRepository: CloudServiceFactoryRepository
    @Autowired
    private lateinit var userCloudServiceRepository: UserCloudServiceRepository

    /**
     * GET method for retrieving a list of Cloud Service Extensions currently installed.
     */
    @RequestMapping("/cloud-services",method = arrayOf(RequestMethod.GET),produces = arrayOf("application/json"))
    @ResponseBody fun getCloudServices() : CloudServiceExtensionList {
        return cloudServiceFactoryRepository.cloudServiceNames
    }
    /**
     * GET method for retrieving a list of Cloud Service Extensions currently installed which the user logged in (though the login may have expired).
     */
    @RequestMapping("/cloud-services/{username}",method = arrayOf(RequestMethod.GET),produces = arrayOf("application/json"))
    fun getUserCloudServices(@PathVariable(value="username") username :String?) : ResponseEntity<CloudServiceExtensionList> {
        //return an empty list if the user doesn't exist
        val user = userRepository.findByUsername(username?: throw CloudBackEncUserNotFound()) ?: throw CloudBackEncUserNotFound()
        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUser = userAccountDetailsManager.userRepository.findByUsername(authorizedUser.name) ?: throw CloudServiceException("Authenticated user could not be found.")

        //authorized user requesting the list must either be the user in the request or be an admin
        if (!((user.username == authorizedUser.name) || (currentUser.roles?.contains(CloudBackEncRoles.ROLE_ADMIN)?:false)))
        {
            return ResponseEntity(null, HttpStatus.FORBIDDEN)
        }

        val userCloudServiceList = userCloudServiceRepository.findByUserId(user.id) ?: return ResponseEntity(CloudServiceExtensionList(), HttpStatus.OK)
        //return only services that are currently installed filtered to those where the user is currently logged in
        val cloudServices = cloudServiceFactoryRepository.cloudServiceNames.filter{ it.uuid in userCloudServiceList.filter{it.loggedIn}.map{UUID.fromString(it.cloudServiceUuid)}}

        return ResponseEntity(CloudServiceExtensionList(ArrayList(cloudServices)), HttpStatus.OK)
    }

}