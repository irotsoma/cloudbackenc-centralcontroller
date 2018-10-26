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

/*
 * Created by irotsoma on 10/26/2018.
 */
package com.irotsoma.cloudbackenc.centralcontroller.data

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name="token")
class TokenObject(@Id @Column(name="token_uuid", nullable = false)
                  var tokenUuid: UUID,

                  @Column(name="user_id", nullable = false)
                  var userId: Long,

                  @Column(name="expiration_date", nullable = false)
                  var expirationDate: Date,

                  @Column(name="valid", nullable = false)
                  var valid: Boolean)