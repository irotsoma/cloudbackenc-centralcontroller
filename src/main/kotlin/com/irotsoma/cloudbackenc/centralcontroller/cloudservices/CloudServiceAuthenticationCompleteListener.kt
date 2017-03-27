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

/**
 * Created by irotsoma on 2/1/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.cloudservices

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.common.CloudBackEncUser
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceAuthenticationRefreshListener
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

/**
 *
 *
 * @author Justin Zak
 */
@Component
class CloudServiceAuthenticationCompleteListener() :CloudServiceAuthenticationRefreshListener {
    override final var user: CloudBackEncUser? = null
    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager
    @Autowired
    private lateinit var userCloudServiceRepository: UserCloudServiceRepository
    constructor(user: CloudBackEncUser):this(){
        this.user = user
    }

    override fun onChange(cloudServiceUuid: UUID, newState: CloudServiceUser.STATE) {
        //TODO implement updating userCloudServiceRepository when auth finishes


        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}