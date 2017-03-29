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

import javax.persistence.*

/**
 * JPA object that represents files including linking to their cloud service storage location.
 *
 * @property fileUuid the unique ID of the file created when the file is first uploaded to the file controller.
 * @property userId The ID of the user that owns the file.
 * @property cloudServiceFileList A list of locations where the various versions of this file are stored on cloud service providers.
 */
@Entity
@Table(name="file")
class FileObject(@Id @Column(name = "file_uuid", unique = true, nullable = false)
                 var fileUuid: String,

                 @Column(name="user_id", nullable = false)
                 var userId: Long,

                 @ElementCollection(fetch = FetchType.EAGER)
                 @CollectionTable(name = "file_cloud_service", joinColumns = arrayOf(JoinColumn(name = "file_uuid")))
                 @OrderBy("last_updated DESC")
                 var cloudServiceFileList: List<CloudServiceFileObject>?
)