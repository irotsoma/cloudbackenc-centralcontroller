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
 * Created by irotsoma on 7/12/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountRepository
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceFactoryRepository
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.UserCloudServiceRepository
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceExtensionList
import com.irotsoma.cloudbackenc.common.logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*


/**
 * REST Controller for getting a list of cloud service extensions currently installed.
 *
 * Use GET method to /cloud-services URL.
 *
 * @author Justin Zak
 */
@RestController
@RequestMapping("/cloud-services",produces = arrayOf("application/json"))
open class CloudServicesListController {
    companion object { val LOG by logger() }

    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager
    @Autowired
    private lateinit var userRepository: UserAccountRepository
    @Autowired
    private lateinit var cloudServiceFactoryRepository: CloudServiceFactoryRepository
    @Autowired
    private lateinit var userCloudServiceRepository: UserCloudServiceRepository

    @RequestMapping(method = arrayOf(RequestMethod.GET))
    @ResponseBody fun getCloudServices(@RequestParam("user") username :String?) : CloudServiceExtensionList {


        if (username == null) {
            return cloudServiceFactoryRepository.cloudServiceNames
        } else {
            //TODO: Change this case to a separate URI like /logged-in

            //TODO: check permission to see if logged in user is the requested one or this is an admin


            return AvailableCloudServices(username)
        }
    }


    fun AvailableCloudServices(username: String) : CloudServiceExtensionList {

        //return an empty list if the user doesn't exist
        val user = userRepository.findByUsername(username) ?: return CloudServiceExtensionList()

        val userCloudServiceList = userCloudServiceRepository.findByUserId(user.id!!)

        //TODO: return only logged in cloud services
        return CloudServiceExtensionList()
    }
}