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

package com.irotsoma.cloudbackenc.centralcontroller.data

import java.util.*
import javax.persistence.*

/**
 * JPA entity representing the configuration settings for an encryption service.
 *
 * @property id Database generated ID for the record.
 * @property encryptionServiceUuid UUID of the encryption service extension
 * @property encryptionIsSymmetric Defines if the encryption algorithm used is symmetric or asymmetric
 * @property encryptionAlgorithm The encryption algorithm used
 * @property encryptionKeyAlgorithm The encryption key algorithm used
 * @property encryptionBlockSize The block size used for block cypher algorithms
 * @property secretKey The encryption key for symmetric algorithms or the private key for asymmetric algorithms
 * @property publicKey The public key for asymmetric algorithms, null for symmetric algorithms
 */
@Entity
@Table(name = "encryption_profile")
class EncryptionProfileObject(@Column(name = "encryption_service_uuid", nullable = true, updatable = false) val encryptionServiceUuid:UUID?,
                              @Column(name = "encryption_is_symmetric", nullable = false, updatable = false) val encryptionIsSymmetric:Boolean,
                              @Column(name = "encryption_algorithm", nullable = false, updatable = false) val encryptionAlgorithm:String,
                              @Column(name = "encryption_key_algorithm", nullable = false, updatable = false) val encryptionKeyAlgorithm:String,
                              @Column(name = "encryption_block_size", nullable = true, updatable = false) val encryptionBlockSize:Int?,
                              @Column(name = "secret_key", nullable = false, updatable = false) val secretKey: ByteArray,
                              @Column(name = "public_key", nullable = true, updatable = false) val publicKey: ByteArray?
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = -1
}