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
 * Created by irotsoma on 2/1/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.cloudservices

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.data.UserAccountRepository
import com.irotsoma.cloudbackenc.common.CloudBackEncUser
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceAuthenticationRefreshListener
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceUser
import java.util.*

/**
 * Implementation of listener that updates the database to the appropriate login status for a particular cloud service
 * This should not be a spring bean as the extension may not implement spring so beans must be passed as parameters instead of autowired
 *
 * @property user The internal user associated with the cloud service.  Required in order to appropriately update the database.
 * @property cloudServiceUsername The username used for logging in to the cloud service.
 * @property userAccountDetailsManager Instance of user account manager
 * @property cloudServiceUserRepository JPA repository that stores the login information for a cloud service
 * @property userRepository JPA repository that stores internal user information
 * @author Justin Zak
 */
class CloudServiceAuthenticationCompleteListener(override var user: CloudBackEncUser?, private val userAccountDetailsManager: UserAccountDetailsManager, private val userRepository: UserAccountRepository, override var cloudServiceUsername: String?, private val cloudServiceUserRepository: CloudServiceUserRepository) : CloudServiceAuthenticationRefreshListener {

    /**
     * Called when the authentication state changes to update the status in the database.
     *
     * @param cloudServiceUuid UUID of the cloud service extension.
     * @param newState Updated authentication state.
     */
    override fun onChange(cloudServiceUuid: UUID, newState: CloudServiceUser.STATE) {
        if (user != null) {
            val userId = userRepository.findByUsername(user!!.username)?.id ?: return
            var cloudServiceUserInfo = cloudServiceUserRepository.findByUserIdAndCloudServiceUuidAndCloudServiceUsername(userId, cloudServiceUuid.toString(),if(cloudServiceUsername.isNullOrEmpty()){null}else{cloudServiceUsername})
            if (cloudServiceUserInfo == null){
                cloudServiceUserInfo = CloudServiceUserObject(cloudServiceUuid, userId, if(cloudServiceUsername.isNullOrEmpty()){null}else{cloudServiceUsername})
            }
            cloudServiceUserInfo.loggedIn = newState == CloudServiceUser.STATE.LOGGED_IN
            cloudServiceUserRepository.save(cloudServiceUserInfo)
        }
    }
}