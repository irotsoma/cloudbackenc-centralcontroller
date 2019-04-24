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
 * Created by irotsoma on 12/22/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.data

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository for cloud service file information.
 *
 * @author Justin Zak
 */
@Repository
interface CloudServiceFileRepository : JpaRepository<CloudServiceFileObject, Long> {
    /**
     * retrieve a record by a specific encryption profile.
     * This is used by the periodic cleanup routine.
     *
     * @param profile The EncryptionProfileObject to search files for.
     * @return An instance of [CloudServiceFileObject] representing the database record or empty list if the [EncryptionProfileObject] was not found
     */
    fun findByEncryptionProfile(profile: EncryptionProfileObject): List<CloudServiceFileObject>
}