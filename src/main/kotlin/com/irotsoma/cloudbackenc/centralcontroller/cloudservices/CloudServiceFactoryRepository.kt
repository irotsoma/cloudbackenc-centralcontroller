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

package com.irotsoma.cloudbackenc.centralcontroller.cloudservices
/*
 * Created by irotsoma on 6/20/2016.
 */

import com.irotsoma.cloudbackenc.common.ExtensionRepository
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceException
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceExtension
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceFactory
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

/**
 * Imports and stores information about installed Cloud Service Extensions
 *
 * @author Justin Zak
 * @property applicationContext Stores the current application context.  Set automatically by Spring.
 * @property cloudServicesSettings Loads configuration options from external properties
 */

@Component
class CloudServiceFactoryRepository : ExtensionRepository(), ApplicationContextAware {

    /** kotlin-logging implementation*/
    companion object: KLogging()
    //inject settings
    @Autowired lateinit var cloudServicesSettings: CloudServicesSettings

    lateinit var applicationContext : ConfigurableApplicationContext
    /** Used by Spring to set the current application context. */
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext as ConfigurableApplicationContext
    }
    /** Initialize the extensionSettings and parentClassLoader and call loadDynamicServices from the super class */
    @PostConstruct
    fun configure(){
        extensionSettings = cloudServicesSettings
        parentClassLoader = applicationContext.classLoader ?: throw CloudServiceException("Application Classloader in CloudServiceRepository is null.")
        loadDynamicServices<CloudServiceFactory, CloudServiceExtension>()
    }
 }

