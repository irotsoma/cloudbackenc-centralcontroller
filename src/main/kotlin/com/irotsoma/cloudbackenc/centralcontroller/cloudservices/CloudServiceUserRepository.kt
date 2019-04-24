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
 * Created by irotsoma on 12/7/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.cloudservices

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

/**
 * JPA repository for link between users and cloud services.
 *
 * @author Justin Zak
 */
@Repository
interface CloudServiceUserRepository : JpaRepository<CloudServiceUserObject, Long> {
    /**
     * retrieve all records for an internal user
     *
     * @param userId The db ID of the internal user
     * @returns A List of instances of [CloudServiceUserObject] representing the database records or null if the user has no records
     */
    fun findByUserId(userId: Long) : List<CloudServiceUserObject>?
    /**
     * retrieve all records for an internal user for a specific cloud service extension
     *
     * @param userId The database ID of the internal user
     * @param cloudServiceUuid The UUID of the cloud service extension
     * @returns A List of instances of [CloudServiceUserObject] representing the database records or null if the user has no records
     */
    fun findByUserIdAndCloudServiceUuid(userId: Long, cloudServiceUuid: UUID): List<CloudServiceUserObject>?
    /**
     * retrieve a record for an internal user for a specific cloud service extension and associated cloud service username
     *
     * @param userId The database ID of the internal user
     * @param cloudServiceUuid The UUID of the cloud service extension
     * @param cloudServiceUsername The username used to log in to the cloud service
     * @returns An instance of [CloudServiceUserObject] representing the database record or null if the user has no records
     */
    fun findByUserIdAndCloudServiceUuidAndCloudServiceUsername(userId: Long, cloudServiceUuid: String, cloudServiceUsername: String?): CloudServiceUserObject?
    /**
     * retrieve a list of all of the unique internal users who have configured cloud service logins
     *
     * @returns A List of instances of database user IDs representing the database records or null if there are no records
     */
    @Query("SELECT DISTINCT userId FROM CloudServiceUserObject")
    fun findDistinctUserId(): List<Long>?
}