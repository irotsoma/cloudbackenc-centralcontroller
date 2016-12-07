/*
 * Created by irotsoma on 12/7/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceFactoryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
*
*
* @author Justin Zak
*/
@Component
open class FileDistributor {
    @Autowired
    lateinit var cloudServiceFactoryRepository: CloudServiceFactoryRepository


}