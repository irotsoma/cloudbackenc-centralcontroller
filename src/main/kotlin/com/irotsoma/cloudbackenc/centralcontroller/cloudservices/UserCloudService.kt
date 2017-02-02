/**
 * Created by irotsoma on 12/7/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.cloudservices

import javax.persistence.*

/**
 *
 *
 * @author Justin Zak
 */
@Entity
@Table(name="user_cloud_service")
class UserCloudService(cloudServiceUuid: String,
                       userId: Long,
                       LoggedIn: Boolean = false) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name="cloud_service_uuid",nullable=false)
    var cloudServiceUuid: String? = cloudServiceUuid

    @Column(name="user_id",nullable=false)
    var userId: Long? = userId

    @Column(name="logged_in",nullable=false)
    var loggedIn: Boolean = LoggedIn
}