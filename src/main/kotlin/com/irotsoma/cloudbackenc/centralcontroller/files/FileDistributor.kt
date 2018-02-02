/*
 * Copyright (C) 2016-2018  Irotsoma, LLC
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
 * Created by irotsoma on 12/7/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceFactoryRepository
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.UserCloudServiceRepository
import com.irotsoma.cloudbackenc.centralcontroller.data.UserAccount
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceFactory
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*

/**
 * Determines the best cloud service provider to which to distribute files.
 *
 * @author Justin Zak
 * @property cloudServiceFactoryRepository Autowired repository for cloud service extensions
 * @property userCloudServiceRepository Autowired jpa repository of settings for logging in to cloud services
 * @property userAccountDetailsManager Autowired instance of user account manager
 * @property spaceAvailable A map indexed by user ID containing a map of available space indexed by cloud service extension UUIDs
*/
@Component
class FileDistributor {
    /** kotlin-logging implementation*/
    companion object: KLogging(){
        /** interval for checking the space available in configured cloud services */
        const val delay = 43200000L //check every 12 hours
    }

    @Autowired
    lateinit var cloudServiceFactoryRepository: CloudServiceFactoryRepository

    @Autowired
    lateinit var userCloudServiceRepository: UserCloudServiceRepository

    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager

    val spaceAvailable = HashMap<Long,HashMap<UUID, Long>>()

    /**
     * function to automatically determine the best location to store a file of a given size
     *
     * @param user the UserAccount of the internal user to which the file belongs
     * @param fileSize The size of the file to be stored in bytes
     */
    fun determineBestLocation(user: UserAccount, fileSize: Long, excludeList:List<UUID> = emptyList()): UUID?{
        // currently just finds the service with the most space that's not in the excludeList and returns it as long as it is more than the fileSize
        // TODO: Implement more logic such as service max file size, distributing versions of the same file to different services, etc
        val sortedSpaceAvailable = spaceAvailable[user.id]?.filter {it.key !in excludeList}?.toList()?.sortedBy { (_, value) -> value}?.toMap()

        return if (sortedSpaceAvailable?.values?.last() ?:0 > fileSize) sortedSpaceAvailable?.keys?.last() else null
    }
    /** Scheduled task that locally caches information about the cloud service such as space available */
    @Scheduled(fixedDelay = delay)
    private fun checkAvailableSpacePeriodically(){
        spaceAvailable.clear()
        for (userId in userCloudServiceRepository.findDistinctUserId() ?: emptyList()) {
            spaceAvailable[userId] = HashMap()
            for ((key, value) in cloudServiceFactoryRepository.extensions) {
                try {
                    val factory = value.newInstance() as CloudServiceFactory
                    if (userCloudServiceRepository.findByUserIdAndCloudServiceUuid(userId, factory.extensionUuid) != null) {
                        val user = userAccountDetailsManager.userRepository.findById(userId)
                        if (user != null) {
                            val space = factory.cloudServiceFileIOService.availableSpace(user.cloudBackEncUser())
                            spaceAvailable[userId]!![key] = space
                        }
                    }
                } catch(ignore:Exception){}
            }
        }
    }

    /**
     * Allows for requesting an update of a single cloud service provider for a single user.  This should be called
     * after sending files to update the cache.
     *
     * @param user the UserAccount of the internal user to which the file belongs
     * @param cloudServiceUuid The cloud service provider to refresh
     */
    fun updateAvailableSpace (user: UserAccount, cloudServiceUuid: UUID){
        try {
            val factory = cloudServiceFactoryRepository.extensions[cloudServiceUuid]?.newInstance() as CloudServiceFactory?
            if (userCloudServiceRepository.findByUserIdAndCloudServiceUuid(user.id, cloudServiceUuid) != null && factory != null) {
                val userAccount = userAccountDetailsManager.userRepository.findById(user.id)
                if (userAccount != null) {
                    val space = factory.cloudServiceFileIOService.availableSpace(user.cloudBackEncUser())
                    spaceAvailable[user.id]!!.plus(Pair(cloudServiceUuid, cloudServiceUuid))
                    if (!spaceAvailable.containsKey(user.id)){
                        spaceAvailable[user.id]= HashMap()
                    }
                    if (spaceAvailable[user.id]!!.contains(cloudServiceUuid)){
                        spaceAvailable[user.id]!!.replace(cloudServiceUuid,space)
                    } else {
                        spaceAvailable[user.id]!!.put(cloudServiceUuid, space)
                    }

                }
            }
        } catch(ignore:Exception){}


    }

}