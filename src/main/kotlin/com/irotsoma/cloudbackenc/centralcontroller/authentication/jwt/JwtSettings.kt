/*
 * Copyright (C) 2016-2019  Irotsoma, LLC
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
 * Created by irotsoma on 4/10/2019.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
/**
*
* Configuration object for encryption extensions.
* Loads application.properties settings that start with "jwt".
 *
 * @property keyStore The location of the keystore to be used for creating JWT tokens
 * @property keyStorePassword Password for the keyStore
 * @property keyStoreType Type of Keystore (e.g. PKCS12)
 * @property keyAlias The alias for the private key for creating JWT tokens
 * @property keyPassword Password for the key as defined by keyAlias
 * @property algorithm The algorithm to use for creating JWT tokens
 * @property expiration Defines the default expiration period in seconds for a JWT token
 * @property disabled Set to true to disable JWT tokens completely allowing only login through account credentials.
 *
 * @author Justin Zak
*/
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("jwt")
class JwtSettings {
    lateinit var keyStore: String
    var keyStorePassword: String? = null
    lateinit var keyStoreType: String
    lateinit var keyAlias: String
    var keyPassword: String? = null
    lateinit var algorithm: String
    var expiration: Long? = null
    var disabled: Boolean = false
}