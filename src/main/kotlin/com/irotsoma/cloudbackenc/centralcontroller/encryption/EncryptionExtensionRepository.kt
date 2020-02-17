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
/* Created by irotsoma on 8/18/2016. */

package com.irotsoma.cloudbackenc.centralcontroller.encryption
import com.irotsoma.cloudbackenc.common.ExtensionRepository
import com.irotsoma.cloudbackenc.common.encryption.EncryptionException
import com.irotsoma.cloudbackenc.common.encryption.EncryptionExtension
import com.irotsoma.cloudbackenc.common.encryption.EncryptionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

/**
 *
 *
 * Implements ExtensionRepository for encryption extensions
 *
 * @author Justin Zak
 * @property applicationContext Stores the current application context.  Set automatically by Spring.
 * @property encryptionExtensionSettings Loads configuration options from external properties
 */
@Component
class EncryptionExtensionRepository : ExtensionRepository(), ApplicationContextAware {

    @Autowired
    lateinit var encryptionExtensionSettings: EncryptionExtensionSettings

    lateinit var applicationContext : ConfigurableApplicationContext

    /** Used by Spring to set the current application context. */
    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        this.applicationContext = applicationContext as ConfigurableApplicationContext? ?: throw EncryptionException("Application context in EncryptionExtensionRepository is null.")
    }

    /** Initialize the extensionSettings and parentClassLoader and call loadDynamicServices from the super class */
    @PostConstruct
    fun configure(){
        extensionSettings = encryptionExtensionSettings
        parentClassLoader = applicationContext.classLoader ?: throw EncryptionException("Application Classloader in EncryptionExtensionRepository is null.")
        loadDynamicServices<EncryptionFactory,EncryptionExtension>()
    }
}

