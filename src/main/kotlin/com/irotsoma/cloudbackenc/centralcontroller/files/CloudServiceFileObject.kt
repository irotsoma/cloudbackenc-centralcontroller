/*
 * Copyright (C) 2016-2017  Irotsoma, LLC
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
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import java.util.*
import javax.persistence.*

@Entity
@Table(name="cloud_service_file")
class CloudServiceFileObject(@Column(name="file_uuid", nullable = false) var fileUuid: String,
                             @Column(name="cloud_service_uuid", nullable = false) var cloudServiceUuid: String,
                             @Column(name="locator", nullable = false)var locator: String,
                             @Column(name="last_updated", nullable = false) var lastUpdated: Date) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

}