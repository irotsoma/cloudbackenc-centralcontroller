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

package com.irotsoma.cloudbackenc.centralcontroller

import com.irotsoma.cloudbackenc.centralcontroller.encryption.EncryptionExtensionRepository
import com.irotsoma.cloudbackenc.common.encryption.EncryptionFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.util.*

/**
 * Tests related to encryption
 */
@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.DEFINED_PORT)
class EncryptionTests {

    @Autowired
    lateinit var encryptionExtensionRepository:EncryptionExtensionRepository

    /**
     * Tests if the encryption repository can load its default extension properly
     */
    @Test
    fun testEncryptionRepository(){
        val encryptionFactoryClass = encryptionExtensionRepository.extensions[UUID.fromString(encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid)] ?: error("Could not load default encryption extension.")
        val encryptionFactory = encryptionFactoryClass.newInstance()
        assert(encryptionFactory is EncryptionFactory)
        assert(encryptionFactory.extensionUuid == UUID.fromString(encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid))
    }
}