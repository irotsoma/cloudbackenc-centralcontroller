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
class UserCloudService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name="cloud_service_uuid",nullable=false)
    var cloudServiceUuid: String? = null

    @Column(name="user_id",nullable=false)
    var userId: Long? = null

    @Column(name="logged_in",nullable=false)
    var LoggedIn: Boolean = false
}