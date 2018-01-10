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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*

/**
* Determines the best cloud service provider to which to distribute files.
*
* @author Justin Zak
*/
@Component
class FileDistributor {
    @Autowired
    lateinit var cloudServiceFactoryRepository: CloudServiceFactoryRepository

    @Autowired
    lateinit var userCloudServiceRepository: UserCloudServiceRepository

    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager


    companion object{
        //check every 12 hours
        const val delay = 43200000L
    }
    val spaceAvailable = HashMap<Long,HashMap<Long,CloudServiceFactory>>()
    fun determineBestLocation(user: UserAccount, fileSize: Long): CloudServiceFactory?{
        // currently just finds the service with the most space and returns it as long as it is more than the fileSize
        //TODO: Implement more logic such as service max file size, etc
        val sortedSpaceAvailable = spaceAvailable[user.id]?.toSortedMap()

        return if (sortedSpaceAvailable?.lastKey() ?: 0 >fileSize) sortedSpaceAvailable?.get(sortedSpaceAvailable.lastKey()) else null
    }

    @Scheduled(fixedDelay = delay)
    fun checkAvailableSpacePeriodically(){
        spaceAvailable.clear()
        for (userId in userCloudServiceRepository.findDistinctUserId() ?: emptyList()) {
            spaceAvailable[userId] = HashMap()
            for ((key, value) in cloudServiceFactoryRepository.extensions) {
                try {
                    val factory = value.newInstance() as CloudServiceFactory
                    if (userCloudServiceRepository.findByUserIdAndCloudServiceUuid(userId, factory.extensionUuid.toString()) != null) {
                        val user = userAccountDetailsManager.userRepository.findById(userId)
                        if (user != null) {
                            val space = factory.cloudServiceFileIOService.availableSpace(user.cloudBackEncUser())
                            spaceAvailable[userId]!!.plus(Pair(space, factory))
                        }
                    }
                } catch(ignore:Exception){}
            }
        }
    }

}