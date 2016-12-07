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



}