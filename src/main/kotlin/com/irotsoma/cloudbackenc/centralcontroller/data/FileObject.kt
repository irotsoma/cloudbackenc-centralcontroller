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
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.data

import java.util.*
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
                 var fileUuid: UUID,

                 @Column(name="user_id", nullable = false)
                 var userId: Long,

                 @ElementCollection(fetch = FetchType.EAGER)
                 @CollectionTable(name = "cloud_service_file", joinColumns = arrayOf(JoinColumn(name = "file_uuid")))
                 //order by version to allow for adding +1 to get the new version number and for deleting entries above the save limit
                 @OrderBy("version ASC")
                 var cloudServiceFileList: List<CloudServiceFileObject>?
)