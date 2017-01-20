/**
 * Created by irotsoma on 12/7/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.cloudservices

import org.springframework.data.jpa.repository.JpaRepository

/**
 *
 *
 * @author Justin Zak
 */

interface UserCloudServiceRepository : JpaRepository<UserCloudService, Long> {
    fun findByUserId(userId: Long) : List<UserCloudService>?
    fun findByUserIdAndCloudServiceUuid(userId: Long, cloudServiceUuid: String): UserCloudService?
}