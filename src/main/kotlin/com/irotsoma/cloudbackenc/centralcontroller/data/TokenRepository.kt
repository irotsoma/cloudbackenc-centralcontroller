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
 * Created by irotsoma on 10/26/2018.
 */
package com.irotsoma.cloudbackenc.centralcontroller.data

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

/**
 * JPA repository for storing security token metadata.
 *
 * @author Justin Zak
 */
interface TokenRepository: JpaRepository<TokenObject, UUID> {
    /**
     * Retrieve a token by its UUID
     *
     * @param tokenUuid The UUID of the token to retrieve
     * @return An instance of [TokenObject] representing the token or null if not found.
     */
    fun findByTokenUuid(tokenUuid: UUID): TokenObject?

    /**
     * Retrieve all tokens assigned to a particular user
     *
     * @param userId The user ID of the user to retrieve tokens for
     * @return A list of [TokenObject]s
     */
    fun findByUserId(userId: Long): List<TokenObject>

    /**
     * Retrieve all tokens with an expiration date less than a certain date. Handy for cleaning out expired tokens from the DB
     *
     * @param date The expiration date before which to retrieve tokens
     * @return A list of [TokenObject]s
     */
    fun findAllByExpirationDateLessThan(date: Date): List<TokenObject>
}