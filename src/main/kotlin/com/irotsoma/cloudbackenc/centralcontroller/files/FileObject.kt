/*
 * Copyright (C) 2017  Irotsoma, LLC
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

@Entity
@Table(name="file")
class FileObject(@Column(name = "file_uuid", unique = true, nullable = false)
                 var fileUuid: String,

                 @Column(name="owner_uuid", nullable = false)
                 var ownerUuid: String,

                 @Column(name="owner_file_uuid", nullable = false)
                 var ownerFileUuid: String,

                 @ElementCollection(fetch = FetchType.EAGER)
                 @CollectionTable(name = "file_cloud_service", joinColumns = arrayOf(JoinColumn(name = "file_id", referencedColumnName = "id")))
                 @OrderBy("last_updated DESC")
                 var cloudServiceFileList: List<CloudServiceFileObject>?
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}