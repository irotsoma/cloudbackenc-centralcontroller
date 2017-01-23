/*
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import java.util.*
import javax.persistence.*

@Entity
@Table(name="cloud_service_file")
class CloudServiceFileObject(@Column(name="file_id", nullable = false) var fileId: Long,
                             @Column(name="cloud_service_uuid", nullable = false) var cloudServiceUuid: String,
                             @Column(name="path", nullable = false)var path: String,
                             @Column(name="last_updated", nullable = false) var lastUpdated: Date) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

}