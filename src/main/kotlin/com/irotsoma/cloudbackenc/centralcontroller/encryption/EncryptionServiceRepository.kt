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

package com.irotsoma.cloudbackenc.centralcontroller.encryption

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.readValue
import com.irotsoma.cloudbackenc.centralcontroller.VersionedExtensionFactoryClass
import com.irotsoma.cloudbackenc.centralcontroller.encryption.EncryptionServicesSettings
import com.irotsoma.cloudbackenc.common.encryptionservice.*
import com.irotsoma.cloudbackenc.common.logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.jar.JarFile
import javax.annotation.PostConstruct

/**
 * Created by irotsoma on 8/18/2016.
 *
 * Imports and stores information about installed Encryption Service Extensions
 */
@Component
open class EncryptionServiceRepository : ApplicationContextAware {
    companion object { val LOG by logger() }
    //inject settings
    @Autowired lateinit var encryptionServicesSettings: EncryptionServicesSettings
    var encryptionServiceExtensions = emptyMap<UUID,Class<EncryptionServiceFactory>>()
    var encryptionServiceNames = EncryptionServiceExtensionList()
    //application context must be set before
    lateinit var _applicationContext : ConfigurableApplicationContext
    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        _applicationContext = applicationContext as ConfigurableApplicationContext? ?: throw EncryptionServiceException("Application context in EncryptionServiceRepository is null.")
    }

    @PostConstruct
    fun loadDynamicServices() {
        //external config extension directory
        val extensionsDirectory: File = File(encryptionServicesSettings.directory)
        if (!extensionsDirectory.isDirectory || !extensionsDirectory.canRead()) {
            LOG.warn("Extensions directory is missing or unreadable. ${extensionsDirectory.absolutePath}")
            return
        }
        //internal resources extension directory (packaged extensions or test extensions)
        val resourcesExtensionsDirectory: File? = try {File(javaClass.classLoader?.getResource("extensions")?.file)} catch (e:Exception) {null}

        val jarURLs : HashMap<UUID,URL> = HashMap()
        val factoryClasses: HashMap<UUID,VersionedExtensionFactoryClass> = HashMap()

        for (jar in (extensionsDirectory.listFiles{directory, name -> (!File(directory,name).isDirectory && name.endsWith(".jar"))} ?: arrayOf<File>()).plus(resourcesExtensionsDirectory?.listFiles{ directory, name -> (!File(directory,name).isDirectory && name.endsWith(".jar"))} ?: arrayOf<File>())) {
            try {
                val jarFile = JarFile(jar)
                //read config file from jar if present
                val jarFileEntry = jarFile.getEntry(encryptionServicesSettings.configFileName)
                if (jarFileEntry == null) {
                    LOG.debug("Extension file missing config file named ${encryptionServicesSettings.configFileName}. Skipping jar file: ${jar.absolutePath}")
                }
                else {
                    //get Json config file data
                    val jsonValue = jarFile.getInputStream(jarFileEntry).reader().readText()
                    val mapper = ObjectMapper().registerModule(KotlinModule())
                    val mapperData: EncryptionServiceExtensionConfig = mapper.readValue(jsonValue)
                    //add values to maps for consumption later
                    val encryptionServiceUUID = UUID.fromString(mapperData.serviceUUID)
                    if (factoryClasses.containsKey(encryptionServiceUUID)){
                        //if the UUID is already in the map check to see if it's a newer version.  If so replace, the existing one, otherwise ignore the new one.
                        if (factoryClasses[encryptionServiceUUID]!!.version < mapperData.releaseVersion){
                            factoryClasses.replace(encryptionServiceUUID, VersionedExtensionFactoryClass("${mapperData.packageName}.${mapperData.factoryClass}", mapperData.releaseVersion))
                            jarURLs.replace(encryptionServiceUUID,jar.toURI().toURL())
                        }
                    } else {
                        //if the UUID is not in the map add it
                        factoryClasses.put(encryptionServiceUUID, VersionedExtensionFactoryClass("${mapperData.packageName}.${mapperData.factoryClass}", mapperData.releaseVersion))
                        jarURLs.put(encryptionServiceUUID,jar.toURI().toURL())
                        encryptionServiceNames.add(EncryptionServiceExtension(encryptionServiceUUID, mapperData.serviceName))
                    }

                }
            } catch (e: MissingKotlinParameterException) {
                LOG.warn("Encryption service extension configuration file is missing a required field.  This extension will be unavailable: ${jar.name}.  Error Message: ${e.message}")
            } catch (e: Exception) {
                LOG.warn("Error processing encryption service extension file. This extension will be unavailable: ${jar.name}.   Error Message: ${e.message}")
            }
        }
        //create a class loader with all of the jars
        val classLoader = URLClassLoader(jarURLs.values.toTypedArray(),_applicationContext.classLoader)
        //cycle through all of the classes, make sure they inheritors EncryptionServiceFactory, and add them to the list
        for ((key, value) in factoryClasses) {
            try{
                val gdClass = classLoader.loadClass(value.canonicalName)
                //verify instance of gdClass is a EncryptionServiceFactory
                if (gdClass.newInstance() is EncryptionServiceFactory) {
                    //add to list -- suppress warning about unchecked class as we did that in the if statement for an instance but it can't be done directly
                    encryptionServiceExtensions = encryptionServiceExtensions.plus(Pair(key, @Suppress("UNCHECKED_CAST")(gdClass as Class<EncryptionServiceFactory>)))
                }
                else {
                    LOG.warn("Error loading encryption service extension: Factory is not an instance of EncryptionServiceFactory: $value" )
                }
            } catch(e: ClassNotFoundException){
                LOG.warn("Error loading encryption service extension: $value: ${e.message}")
            }
        }
    }
}

