/*
 * Created by irotsoma on 12/7/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceFactoryRepository
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.UserCloudServiceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

/**
*
*
* @author Justin Zak
*/
@Component
open class FileDistributor {
    @Autowired
    lateinit var cloudServiceFactoryRepository: CloudServiceFactoryRepository

    @Autowired
    lateinit var userCloudServiceRepository: UserCloudServiceRepository


    fun determineBestLocation(id: Long): UUID?{
        //TODO: Implement and return cloud service uuid







        return null
    }

}