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
    override var user: CloudBackEncUser? = null
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