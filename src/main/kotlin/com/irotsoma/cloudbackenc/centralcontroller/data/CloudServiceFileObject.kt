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
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.data

import java.util.*
import javax.persistence.*

/**
 * JPA entity representing a file stored on a cloud service provider.
 *
 * @property id Database generated ID for the record.
 * @property fileUuid The UUID of a file as generated when the file was uploaded to the file controller.
 * @property cloudServiceUuid the UUID of the cloud service extension that controls the cloud service interface operations.
 * @property locator The URI, ID, or other string that uniquely locates a file in a cloud service.
 * @property lastUpdated The date and time of the last update to the file (usually just the upload date/time).
 */

@Entity
@Table(name="cloud_service_file")
class CloudServiceFileObject(@Column(name="file_uuid", nullable = false)
                             var fileUuid: UUID,

                             @Column(name="cloud_service_uuid", nullable = false)
                             var cloudServiceUuid: String,

                             @Column(name="locator", nullable = false)
                             var locator: String,

                             @Column(name="version", nullable = false)
                             var version: Long,

                             @Column(name="last_updated", nullable = false)
                             var lastUpdated: Date,

                             @ManyToOne(cascade = [CascadeType.ALL])
                             @JoinColumn(name = "encryption_profile_id", referencedColumnName = "id", nullable = false)
                             var encryptionProfile: EncryptionProfile,

                             @Column(name="initialization_vector", nullable = true)
                             var initializationVector: ByteArray? = null,

                             @Column(name="original_hash", nullable = false)
                             var originalHash: String,

                             @Column(name="encrypted_hash", nullable = false)
                             var encryptedHash: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = -1
}