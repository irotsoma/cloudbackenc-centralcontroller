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

import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceRepository
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceExtensionList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody


/**
 * REST Controller for getting a list of cloud service extensions currently installed.
 *
 * Use GET method to /cloud-services URL.
 *
 * @author Justin Zak
 */
@Controller
@RequestMapping("/cloud-services",produces = arrayOf("application/json"))
open class CloudServicesListController {

    @Autowired
    private lateinit var cloudServiceRepository: CloudServiceRepository

    @RequestMapping(method = arrayOf(RequestMethod.GET))
    @ResponseBody fun getCloudServices() : CloudServiceExtensionList {
        return cloudServiceRepository.cloudServiceNames
    }
}