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

/*
 * Created by irotsoma on 2/1/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.cloudservices

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.common.CloudBackEncUser
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceAuthenticationRefreshListener
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.util.*

/**
 * Implementation of listener that updates the database to the appropriate login status for a particular cloud service
 *
 * @param user The internal user associated with the cloud service.  Required in order to appropriately update the database.
 * @author Justin Zak
 */
@Lazy
@Component
class CloudServiceAuthenticationCompleteListener(user: CloudBackEncUser) : CloudServiceAuthenticationRefreshListener {
    /**
     * CloudBackEncUser instance that identifies the internal user associated with the listener.
     */
    override final var user: CloudBackEncUser? = user
    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager
    @Autowired
    private lateinit var userCloudServiceRepository: UserCloudServiceRepository

    /**
     * Called when the authentication state changes to update the status in the database.
     *
     * @param cloudServiceUuid UUID of the cloud service extension.
     * @param newState Updated authentication state.
     */
    override fun onChange(cloudServiceUuid: UUID, newState: CloudServiceUser.STATE) {
        if (user != null) {
            val userId = userAccountDetailsManager.userRepository.findByUsername(user!!.username)?.id ?: return
            val cloudServiceUserInfo = userCloudServiceRepository.findByUserIdAndCloudServiceUuid(userId, cloudServiceUuid.toString()) ?: return
            cloudServiceUserInfo.loggedIn = newState == CloudServiceUser.STATE.LOGGED_IN
            userCloudServiceRepository.save(cloudServiceUserInfo)
        }
    }
}