/*
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import java.util.*
import javax.persistence.*

@Entity
@Table(name="file_cloudservice")
class FileCloudService() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    @Column(name="file_id", nullable = false)
    var fileId: Long? = null
    @Column(name="cloudservice_uuid", nullable = false)
    var cloudserviceUuid: String? = null
    @Column(name="last_updated", nullable = false)
    var lastUpdated: Date? = null
}