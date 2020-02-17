/*
 * Copyright (C) 2016-2020  Irotsoma, LLC
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
 * Created by irotsoma on 8/15/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.data

import org.hibernate.annotations.Where
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository object for storing user accounts
 *
 * @author Justin Zak
 */
@Repository
interface UserAccountRepository : JpaRepository<UserAccountObject, Long> {
    /**
     * retrieve a record by the username of the user
     *
     * @param username The username of the user to retrieve
     * @returns An instance of [UserAccountObject] representing the database record or null if the username was not found
     */
    @Where(clause="state = 'active'")
    fun findByUsername(username: String): UserAccountObject?
    /**
     * retrieve a record by the user's email address
     *
     * @param email The email address of the user to retrieve
     * @returns An instance of [UserAccountObject] representing the database record or null if the email address was not found
     */
    @Where(clause="state = 'active'")
    fun findByEmail (email: String): UserAccountObject?
    /**
     * retrieve a record by a specific default encryption profile.
     * This is used by the periodic cleanup routine.
     *
     * @param profile The EncryptionProfileObject to search users for.
     * @returns An instance of [UserAccountObject] representing the database record or empty list if the [EncryptionProfileObject] was not found
     */
    fun findByDefaultEncryptionProfile(profile: EncryptionProfileObject): List<UserAccountObject>
}