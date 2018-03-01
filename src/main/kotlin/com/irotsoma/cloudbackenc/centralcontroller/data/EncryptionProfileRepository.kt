/*
 * Copyright (C) 2016-2018  Irotsoma, LLC
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

package com.irotsoma.cloudbackenc.centralcontroller.data

import org.springframework.data.jpa.repository.JpaRepository

/**
 * JPA repository for encryption configuration profiles that will be linked with files sent to a cloud service
 *
 * @author Justin Zak
 */
interface EncryptionProfileRepository: JpaRepository<EncryptionProfile,Long> {
    /**
     * retrieve a record by the db ID
     *
     * @param id The database ID of the record to retrieve
     * @returns An instance of [EncryptionProfile] representing the database record or null if the ID was not found
     */
    //fun findById(id:Long): EncryptionProfile?
}