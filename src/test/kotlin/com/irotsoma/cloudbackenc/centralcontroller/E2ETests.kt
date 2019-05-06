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
 * Created by irotsoma on 4/22/2019.
 */
package com.irotsoma.cloudbackenc.centralcontroller

import com.irotsoma.cloudbackenc.centralcontroller.controllers.exceptions.DuplicateUserException
import com.irotsoma.cloudbackenc.common.AuthenticationToken
import com.irotsoma.cloudbackenc.common.CloudBackEncRoles
import com.irotsoma.cloudbackenc.common.CloudBackEncUser
import com.irotsoma.cloudbackenc.common.UserAccountState
import com.irotsoma.cloudbackenc.common.encryption.EncryptionAlgorithmTypes
import com.irotsoma.cloudbackenc.common.encryption.EncryptionProfile
import com.irotsoma.cloudbackenc.common.encryption.EncryptionSymmetricEncryptionAlgorithms
import com.irotsoma.cloudbackenc.common.encryption.EncryptionSymmetricKeyAlgorithms
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.client.HttpClientErrorException

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
class E2ETests {
    @LocalServerPort
    private var port: Int = 0
    @Value("\${server.ssl.key-store}")
    private var useSSL: String? = null
    @Value("\${centralcontroller.api.v1.path}")
    private var apiV1Path: String = ""
    var protocol: String = "http"
    var restTemplate: TestRestTemplate = TestRestTemplate()
    var user:CloudBackEncUser? = null
    var userToken: AuthenticationToken? = null

    val userUsername = "e2etestuser"
    val userPassword = "E2etestuser!"

    val adminUsername = "admin"
    val adminPassword = "insecurepassword"

    @Test
    fun e2eUser(){

        createUser()

        setupRestTemplate(user!!.username, user!!.password)
        val userTokenResponse = restTemplate.getForEntity("$protocol://localhost:$port$apiV1Path/auth", AuthenticationToken::class.java)
        assert(userTokenResponse.body != null) { "User login failed while getting auth token." }
        userToken = userTokenResponse.body

        createEncryptionProfile()

        //TODO: add more tests

        setupRestTemplate(adminUsername, adminPassword)
        restTemplate.delete("$protocol://localhost:$port$apiV1Path/users/${user!!.username}")
    }

    fun createEncryptionProfile(){
        val encryptionProfile = EncryptionProfile(EncryptionAlgorithmTypes.SYMMETRIC, EncryptionSymmetricEncryptionAlgorithms.AES, EncryptionSymmetricKeyAlgorithms.AES,128,128, null)
        setupRestTemplate(user!!.username,user!!.password)
        val responseEntity = restTemplate.postForEntity("$protocol://localhost:$port$apiV1Path/users/encryption",encryptionProfile,Any::class.java)
        assert(responseEntity.statusCode == HttpStatus.CREATED)
    }


    fun createUser(){
        setupRestTemplate(adminUsername, adminPassword)
        user = CloudBackEncUser(userUsername, userPassword, null, UserAccountState.ACTIVE, listOf(CloudBackEncRoles.ROLE_USER))
        try {
            val result = restTemplate.postForEntity("$protocol://localhost:$port$apiV1Path/users", HttpEntity(user!!), CloudBackEncUser::class.java)
            assert(result.statusCode == HttpStatus.CREATED) {"Error creating new user: ${result.statusCode.reasonPhrase}"}
        } catch (e: HttpClientErrorException){
            if (e.rawStatusCode == 401) {
                assert(false) {"Could not create user. Admin authorization failed."}
            } else {
                throw e
            }
        } catch (e: DuplicateUserException){
            assert(false) {"Username in e2e settings file already exists."}
        }
    }


    fun setupRestTemplate(username: String?, password:String?){
        if (useSSL!=null && useSSL!="") {
            protocol= "https"
            trustSelfSignedSSL()
            restTemplate = TestRestTemplate(username, password, TestRestTemplate.HttpClientOption.SSL)
        } else {
            protocol = "http"
            restTemplate = TestRestTemplate(username, password)
        }
    }

}