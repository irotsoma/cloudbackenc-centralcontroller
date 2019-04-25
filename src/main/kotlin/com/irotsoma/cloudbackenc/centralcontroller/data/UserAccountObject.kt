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
 * Created by irotsoma on 8/15/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.irotsoma.cloudbackenc.common.CloudBackEncRoles
import com.irotsoma.cloudbackenc.common.CloudBackEncUser
import com.irotsoma.cloudbackenc.common.UserAccountState
import mu.KLogging
import org.hibernate.annotations.ResultCheckStyle
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import javax.persistence.*

/**
 * JPA User Account Object
 *
 * @author Justin Zak
 * @property id Database-generated ID for the user.
 * @property username Username of the user.
 * @property password User password encoded using BCrypt.
 * @property email (Optional) email address for the user to receive notifications.
 * @property state Indicates if a user is enabled in the system.
 * @property roleList A list of roles for the user.
 * @property roles roleList translated into CloudBackEncRoles
 * @property defaultEncryptionProfile The encryption settings preferred by this user.
 */
@Entity
@Table(name = "user_account")
@SQLDelete(sql = "UPDATE user_account SET state = 'DELETED' WHERE id = ?", check = ResultCheckStyle.COUNT)
@Where(clause = "state != 'DELETED'")
class UserAccountObject(@Column(name = "username", nullable = false, updatable = false) val username: String,
                        password: String,
                        @Column(name = "email", nullable = true) var email: String?,
                        @Column(name = "state", nullable = false) @Enumerated(EnumType.STRING) var state: UserAccountState,
                        roles: List<CloudBackEncRoles>) {
    /** kotlin-logging implementation */
    companion object : KLogging() {
        /** the type of password encoder used to hash passwords before storing them */
        val PASSWORD_ENCODER: PasswordEncoder = BCryptPasswordEncoder()
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    var id: Long = -1

    @JsonIgnore
    @Column(name="password", nullable=false)
    var password: String? = PASSWORD_ENCODER.encode(password)
        set(value) {
            field = PASSWORD_ENCODER.encode(value)
        }
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = arrayOf(JoinColumn(name = "user_id", referencedColumnName = "id")))
    @Column(name="role")
    private var roleList: List<String>? = roles.map{it.name}
    var roles: List<CloudBackEncRoles>?
        set(value){
            roleList = value?.map{it.name}
        }
        get(){
            return roleList?.mapNotNull {
                try {
                    CloudBackEncRoles.valueOf(it)
                } catch (e:IllegalArgumentException){
                    logger.warn{"The value $it is not a valid user role for user $username.  Ignoring value."}
                    null
                }
            }
        }

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "default_encryption_profile_id", referencedColumnName = "id")
    var defaultEncryptionProfile: EncryptionProfileObject? = null

    /**
     * Convenience method that returns a CloudBackEncUser object with the password masked
     */
    fun cloudBackEncUser(): CloudBackEncUser{
        return CloudBackEncUser(username, CloudBackEncUser.PASSWORD_MASKED, email, state, roles?: emptyList())
    }
}