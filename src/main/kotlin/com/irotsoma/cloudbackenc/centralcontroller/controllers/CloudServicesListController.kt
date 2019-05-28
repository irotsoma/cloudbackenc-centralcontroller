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
 * Created by irotsoma on 7/12/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceFactoryRepository
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceUserRepository
import com.irotsoma.cloudbackenc.centralcontroller.controllers.exceptions.CloudBackEncUserNotFound
import com.irotsoma.cloudbackenc.centralcontroller.data.UserAccountRepository
import com.irotsoma.cloudbackenc.common.CloudBackEncRoles
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceException
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceExtension
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceExtensionList
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*


/**
 * REST Controller for getting a list of cloud service extensions currently installed.
 *
 * Use: GET method to /cloud-services to get all installed extensions.
 * Use: GET method to /cloud-services/{username} to get installed extensions with with the user has previously interacted.
 *
 * @author Justin Zak
 */
@Lazy
@RequestMapping("\${centralcontroller.api.v1.path}/cloud-services")
@RestController
class CloudServicesListController {
    /** kotlin-logging implementation*/
    private companion object: KLogging()

    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager
    @Autowired
    private lateinit var userRepository: UserAccountRepository
    @Autowired
    private lateinit var cloudServiceFactoryRepository: CloudServiceFactoryRepository
    @Autowired
    private lateinit var cloudServiceUserRepository: CloudServiceUserRepository
    /**
     * GET method for retrieving a list of Cloud Service Extensions currently installed.
     */
    @RequestMapping(method = [RequestMethod.GET],produces = ["application/json"])
    @ResponseBody fun getCloudServices() : CloudServiceExtensionList {
        //copy the values of the extension configs in the repository to a CloudServiceExtensionConfigList and mask the factory class and package name since they aren't required and otherwise might cause security issues if shared
        return CloudServiceExtensionList(cloudServiceFactoryRepository.extensionConfigs.values.map{it as CloudServiceExtension}.apply{this.forEach{it.factoryClass = ""; it.packageName=""}})
    }
    /**
     * GET method for retrieving a list of Cloud Service Extensions currently installed which the user logged in (though the login may have expired).
     *
     * @returns An instance of CloudServiceExtensionList in a REST response
     */
    @RequestMapping(path=["/user","/user/{username}"],method = [(RequestMethod.GET)],produces = ["application/json"])
    @Secured("ROLE_USER","ROLE_ADMIN")
    fun getUserCloudServices(@PathVariable(value="username", required = false) username :String?) : ResponseEntity<CloudServiceExtensionList> {
        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUser = userRepository.findByUsername(authorizedUser.name) ?: throw CloudServiceException("Authenticated user could not be found.")
        val user = if (username.isNullOrBlank()){
                currentUser
            } else {
                userRepository.findByUsername(username) ?: throw CloudBackEncUserNotFound()
            }
        //authorized user requesting the list must either be the user in the request or be an admin
        if (!((user.username == authorizedUser.name) || (currentUser.roles?.contains(CloudBackEncRoles.ROLE_ADMIN) == true))) {
            return ResponseEntity(CloudServiceExtensionList(), HttpStatus.FORBIDDEN)
        }
        val userCloudServiceList = cloudServiceUserRepository.findByUserId(user.id) ?: return ResponseEntity(CloudServiceExtensionList(), HttpStatus.OK)
        //return only services that are currently installed filtered to those where the user is currently logged in
        val filteredCloudServices = cloudServiceFactoryRepository.extensionConfigs.filter{ extensionConfig -> extensionConfig.key in userCloudServiceList.filter{it.loggedIn}.map{it.cloudServiceUuid}}

        return ResponseEntity(CloudServiceExtensionList(filteredCloudServices.values.map{it as CloudServiceExtension}.apply{this.forEach{it.factoryClass = ""; it.packageName=""}}), HttpStatus.OK)
    }



}