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
 * Created by irotsoma on 12/7/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.cloudservices

import javax.persistence.*

/**
 * JPA entity representing the link between a user and a cloud service including login status.
 *
 * @property id Database generated ID for the record.
 * @property cloudServiceUuid UUID of the cloud service to link to the user.
 * @property userId ID for the user.
 * @property loggedIn Last known login status of the user for this cloud service.
 * @author Justin Zak
 */
@Entity
@Table(name="user_cloud_service")
class UserCloudService(@Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long,
                       @Column(name="cloud_service_uuid",nullable=false) var cloudServiceUuid: String,
                       @Column(name="user_id",nullable=false) var userId: Long,
                       @Column(name="logged_in",nullable=false) var loggedIn: Boolean = false)