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
 * Created by irotsoma on 12/7/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.cloudservices

import java.util.*
import javax.persistence.*

/**
 * JPA entity representing the link between a user and a cloud service including login status.
 *
 * @property id Database generated ID for the record.
 * @property cloudServiceUuid UUID of the cloud service to link to the user.
 * @property userId Internal user ID to which this configuration belongs.
 * @property cloudServiceUsername The username used to log into the cloud service.
 * @property loggedIn Last known login status of the user for this cloud service.
 * @author Justin Zak
 */
@Entity
@Table(name="user_cloud_service")
data class UserCloudService(@Column(name="cloud_service_uuid",nullable=false, updatable = false) val cloudServiceUuid: UUID,
                            @Column(name="user_id",nullable=false, updatable = false) val userId: Long,
                            @Column(name="cloud_service_username", nullable = true, updatable = false) val cloudServiceUsername: String?,
                            @Column(name="logged_in",nullable=false) var loggedIn: Boolean = false){
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long = -1
}