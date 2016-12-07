/*
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import java.util.*
import javax.persistence.*

@Entity
@Table(name="file_cloud_service")
class FileCloudService() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    @Column(name="file_id", nullable = false)
    var fileId: Long? = null
    @Column(name="cloud_service_uuid", nullable = false)
    var cloudServiceUuid: String? = null
    @Column(name="last_updated", nullable = false)
    var lastUpdated: Date? = null
}