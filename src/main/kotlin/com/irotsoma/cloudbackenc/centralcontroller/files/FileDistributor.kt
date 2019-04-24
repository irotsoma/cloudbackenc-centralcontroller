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
 * Created by irotsoma on 12/7/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceFactoryRepository
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceUserRepository
import com.irotsoma.cloudbackenc.centralcontroller.data.UserAccountObject
import com.irotsoma.cloudbackenc.centralcontroller.data.UserAccountRepository
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceFactory
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*

/**
 * Determines the best cloud service provider to which to distribute files.
 *
 * @author Justin Zak
 * @property cloudServiceFactoryRepository Autowired repository for cloud service extensions
 * @property cloudServiceUserRepository Autowired jpa repository of settings for logging in to cloud services
 * @property userAccountDetailsManager Autowired instance of user account manager
 * @property currentSpaceAvailable A map indexed by user ID containing a map of available space indexed by cloud service extension UUIDs
*/
@Service
class FileDistributor {
    /** kotlin-logging implementation*/
    companion object: KLogging(){
        /** interval for checking the space available in configured cloud services */
        const val delay = 43200000L //check every 12 hours
    }

    @Autowired
    lateinit var cloudServiceFactoryRepository: CloudServiceFactoryRepository

    @Autowired
    lateinit var cloudServiceUserRepository: CloudServiceUserRepository

    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager

    @Autowired
    private lateinit var userRepository: UserAccountRepository
    val currentSpaceAvailable = HashMap<Long,HashMap<UUID, Long>>()

    /**
     * function to automatically determine the best location to store a file of a given size
     *
     * @param user the UserAccountObject of the internal user to which the file belongs
     * @param fileSize The size of the file to be stored in bytes
     */
    fun determineBestLocation(user: UserAccountObject, fileSize: Long, excludeList:List<UUID> = emptyList()): UUID?{
        // currently just finds the service with the most space that's not in the excludeList and returns it as long as it is more than the fileSize
        // TODO: Implement more logic such as service max file size, distributing versions of the same file to different services, etc
        val sortedSpaceAvailable = currentSpaceAvailable[user.id]?.filter {it.key !in excludeList}?.toList()?.sortedBy { (_, value) -> value}?.toMap()

        return if (sortedSpaceAvailable?.values?.last() ?:0 > fileSize) sortedSpaceAvailable?.keys?.last() else null
    }
    /** Scheduled task that locally caches information about the cloud service such as space available */
    @Scheduled(fixedDelay = delay)
    private fun checkAvailableSpacePeriodically(){
        currentSpaceAvailable.clear()
        for (userId in cloudServiceUserRepository.findDistinctUserId() ?: emptyList()) {
            currentSpaceAvailable[userId] = HashMap()
            for ((key, value) in cloudServiceFactoryRepository.extensions) {
                try {
                    val factory = value.getDeclaredConstructor().newInstance() as CloudServiceFactory
                    if (cloudServiceUserRepository.findByUserIdAndCloudServiceUuid(userId, factory.extensionUuid) != null) {
                        val user = userRepository.findById(userId)
                        if (user.isPresent) {
                            val space = factory.cloudServiceFileIOService.availableSpace(user.get().cloudBackEncUser())
                            //if availableSpace returns null, then it either failed or is unavailable, so retain the previous value if present or ignore if not
                            if (space != null) {
                                currentSpaceAvailable[userId]!![key] = space
                            }
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
     * @param user the UserAccountObject of the internal user to which the file belongs
     * @param cloudServiceUuid The cloud service provider to refresh
     */
    fun updateAvailableSpace (user: UserAccountObject, cloudServiceUuid: UUID){
        try {
            val factory = cloudServiceFactoryRepository.extensions[cloudServiceUuid]?.getDeclaredConstructor()?.newInstance() as CloudServiceFactory?
            if (cloudServiceUserRepository.findByUserIdAndCloudServiceUuid(user.id, cloudServiceUuid) != null && factory != null) {
                val userAccount = userRepository.findById(user.id)
                if (userAccount.isPresent) {
                    val space = factory.cloudServiceFileIOService.availableSpace(user.cloudBackEncUser())
                    //if availableSpace returns null, then it either failed or is unavailable, so retain the previous value if present or ignore if not
                    if (space != null) {
                        currentSpaceAvailable[user.id]!!.plus(Pair(cloudServiceUuid, cloudServiceUuid))
                        if (!currentSpaceAvailable.containsKey(user.id)) {
                            currentSpaceAvailable[user.id] = HashMap()
                        }
                        if (currentSpaceAvailable[user.id]!!.contains(cloudServiceUuid)) {
                            currentSpaceAvailable[user.id]!!.replace(cloudServiceUuid, space)
                        } else {
                            currentSpaceAvailable[user.id]!!.put(cloudServiceUuid, space)
                        }
                    }

                }
            }
        } catch(ignore:Exception){}


    }

}