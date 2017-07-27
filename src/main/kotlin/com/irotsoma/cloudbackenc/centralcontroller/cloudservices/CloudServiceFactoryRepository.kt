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

package com.irotsoma.cloudbackenc.centralcontroller.cloudservices
/*
 * Created by irotsoma on 6/20/2016.
 */
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.readValue
import com.irotsoma.cloudbackenc.common.VersionedExtensionFactoryClass
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceExtension
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceExtensionConfig
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceExtensionList
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceFactory
import mu.KLogging
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
 * Imports and stores information about installed Cloud Service Extensions
 *
 * @property applicationContext Stores the current application context.  Set automatically when autowired.
 * @property cloudServiceExtensions A map of CloudServiceFactory Class objects indexed by UUID.
 * @property cloudServiceNames A list of CloudServiceExtension objects stored as a CloudServiceExtensionList for serialization convenience.
 *
 * @author Justin Zak
 */
//TODO: make this generic and put it in the common library for use by both encryption and cloud service extensions
//TODO: refactor to register threads using these classes and allow for reloading them when the jar files change

@Component
class CloudServiceFactoryRepository : ApplicationContextAware {
    /** kotlin-logging implementation*/
    companion object: KLogging()
    //inject settings
    @Autowired lateinit var cloudServicesSettings: CloudServicesSettings
    var cloudServiceExtensions  = emptyMap<UUID,Class<CloudServiceFactory>>()
    var cloudServiceNames = CloudServiceExtensionList()
    lateinit var applicationContext : ConfigurableApplicationContext
    /**
     * Used by Spring Autowiring to set the current application context.
     */
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext as ConfigurableApplicationContext
    }

    /**
     * Loads any Cloud Service Extensions from the extension directory after autowiring constructs the object.
     */
    @PostConstruct
    fun loadDynamicServices() {
        //external config extension directory
        val extensionsDirectory: File? = File(cloudServicesSettings.directory)

        //internal resources extension directory (packaged extensions or test extensions)
        val resourcesExtensionsDirectory: File? = try{ File(javaClass.classLoader.getResource("extensions")?.file) }
                                                  catch (ignore: NullPointerException) { null }
        if ((!(extensionsDirectory?.isDirectory ?: false) || !(extensionsDirectory?.canRead() ?: false)) && ((!(resourcesExtensionsDirectory?.isDirectory ?: false) || !(resourcesExtensionsDirectory?.canRead() ?: false)))) {
            logger.warn{"Extensions directory is missing or unreadable."}
            logger.warn{"Config directory: ${extensionsDirectory?.absolutePath}"}
            logger.warn{"Resource directory: ${resourcesExtensionsDirectory?.absolutePath}"}
            return
        }
        logger.trace{"Config extension directory:  ${extensionsDirectory?.absolutePath}"}
        logger.trace{"Resources extension directory:  ${resourcesExtensionsDirectory?.absolutePath}"}
        val jarURLs : HashMap<UUID,URL> = HashMap()
        val factoryClasses: HashMap<UUID, VersionedExtensionFactoryClass> = HashMap()

        //cycle through all files in the extensions directories
        for (jar in (extensionsDirectory?.listFiles{directory, name -> (!File(directory,name).isDirectory && name.endsWith(".jar"))} ?: arrayOf<File>()).plus(resourcesExtensionsDirectory?.listFiles{directory, name -> (!File(directory,name).isDirectory && name.endsWith(".jar"))} ?: arrayOf<File>())) {
            try {
                logger.info{"Loading extension jar file: ${jar.absolutePath}"}

                val jarFile = JarFile(jar)
                //read config file from jar if present
                val jarFileEntry = jarFile.getEntry(cloudServicesSettings.configFileName)
                if (jarFileEntry == null) {
                    logger.warn{"Extension missing config file named ${cloudServicesSettings.configFileName}. Skipping jar file: ${jar.absolutePath}"}
                }
                else {
                    //get Json config file data
                    val jsonValue = jarFile.getInputStream(jarFileEntry).reader().readText()
                    val mapper = ObjectMapper().registerModule(KotlinModule())
                    val mapperData: CloudServiceExtensionConfig = mapper.readValue(jsonValue)
                    //add values to maps for consumption later
                    val cloudServiceUUID = UUID.fromString(mapperData.serviceUuid)
                    if (factoryClasses.containsKey(cloudServiceUUID)){
                        //if the UUID is already in the map check to see if it's a newer version.  If so replace, the existing one, otherwise ignore the new one.
                        if (factoryClasses[cloudServiceUUID]!!.version < mapperData.releaseVersion){
                            factoryClasses.replace(cloudServiceUUID, VersionedExtensionFactoryClass("${mapperData.packageName}.${mapperData.factoryClass}", mapperData.releaseVersion))
                            jarURLs.replace(cloudServiceUUID,jar.toURI().toURL())
                        }
                    } else {
                        //if the UUID is not in the map add it
                        factoryClasses.put(cloudServiceUUID, VersionedExtensionFactoryClass("${mapperData.packageName}.${mapperData.factoryClass}", mapperData.releaseVersion))
                        jarURLs.put(cloudServiceUUID,jar.toURI().toURL())
                        cloudServiceNames.add(CloudServiceExtension(cloudServiceUUID, mapperData.serviceName, mapperData.requiresUsername, mapperData.requiresPassword))
                    }
                }
            } catch (e: MissingKotlinParameterException) {
                logger.warn{"Cloud service extension configuration file is missing a required field.  This extension will be unavailable: ${jar.name}.  Error Message: ${e.message}"}
            } catch (e: Exception) {
                logger.warn{"Error processing cloud service extension file. This extension will be unavailable: ${jar.name}.   Error Message: ${e.message}"}
            }
        }
        //create a class loader with all of the jars
        val classLoader = URLClassLoader(jarURLs.values.toTypedArray(), applicationContext.classLoader)
        //cycle through all of the classes, make sure they inheritors CloudServiceFactory, and add them to the list
        for ((key, value) in factoryClasses) {
            try {
                val gdClass = classLoader.loadClass(value.canonicalName)
                //verify instance of gdClass is a CloudServiceFactory
                if (gdClass.newInstance() is CloudServiceFactory) {
                    //add to list -- suppress warning about unchecked class as we did that in the if statement for an instance but it can't be done directly
                    cloudServiceExtensions = cloudServiceExtensions.plus(Pair(key, @Suppress("UNCHECKED_CAST")(gdClass as Class<CloudServiceFactory>)))
                }
                else {
                    logger.warn{"Error loading cloud service extension: Factory is not an instance of CloudServiceFactory: $value" }
                }
            } catch(e: ClassNotFoundException){
                logger.warn{"Error loading cloud service extension: $value: ${e.message}"}
            }

        }
    }
}

