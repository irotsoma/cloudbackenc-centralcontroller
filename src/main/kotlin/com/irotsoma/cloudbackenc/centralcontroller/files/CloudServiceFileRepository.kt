/**
 * Created by irotsoma on 12/22/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import org.springframework.data.jpa.repository.JpaRepository

/**
 *
 *
 * @author Justin Zak
 */
interface CloudServiceFileRepository : JpaRepository<CloudServiceFileObject, Long> {
    fun findById(id: Long) : CloudServiceFileObject?
}