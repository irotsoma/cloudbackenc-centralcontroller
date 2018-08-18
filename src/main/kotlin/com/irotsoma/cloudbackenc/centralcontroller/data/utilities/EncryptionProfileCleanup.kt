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
 * Created by irotsoma on 8/17/2018.
 */
package com.irotsoma.cloudbackenc.centralcontroller.data.utilities

import com.irotsoma.cloudbackenc.centralcontroller.data.CloudServiceFileRepository
import com.irotsoma.cloudbackenc.centralcontroller.data.EncryptionProfileRepository
import com.irotsoma.cloudbackenc.centralcontroller.data.UserAccountRepository
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Cleans up the unused encryption profiles in the database
 *
 * @author Justin Zak
 * @property cloudServiceFileRepository JPA repository representing versioned files encrypted and sent to a cloud service
 * @property userAccountRepository JPA repository representing user accounts
 * @property encryptionProfileRepository JPA repository representing encryption configuration profiles
 */
@Component
class EncryptionProfileCleanup {
    /** kotlin-logging implementation*/
    companion object : KLogging()

    @Autowired
    lateinit var encryptionProfileRepository: EncryptionProfileRepository

    @Autowired
    lateinit var cloudServiceFileRepository: CloudServiceFileRepository

    @Autowired
    lateinit var userAccountRepository: UserAccountRepository

    /**
     * Periodically delete encryption profiles if there are no user accounts or cloud service files using it anymore.
     */
    @Scheduled(fixedDelayString = "\${centralcontroller.encryptionProfiles.clean.interval}")
    fun doCleanup(){
        logger.trace{"Starting encryption profile cleanup..."}
        for(profile in encryptionProfileRepository.findAll()) {
            if (userAccountRepository.findByDefaultEncryptionProfile(profile).isEmpty()){
                if (cloudServiceFileRepository.findByEncryptionProfile(profile).isEmpty()){
                    encryptionProfileRepository.delete(profile)
                }
            }


        }
    }


}