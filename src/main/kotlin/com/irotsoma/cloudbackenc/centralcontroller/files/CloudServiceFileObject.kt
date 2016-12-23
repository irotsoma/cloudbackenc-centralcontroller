/*
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import java.util.*
import javax.persistence.*

@Entity
@Table(name="cloud_service_file")
class CloudServiceFileObject() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name="file_id", nullable = false)
    var fileId: Long? = null

    @Column(name="cloud_service_uuid", nullable = false)
    var cloudServiceUuid: String? = null

    @Column(name="path", nullable = false)
    var path: String? = null

    @Column(name="last_updated", nullable = false)
    var lastUpdated: Date? = null

    constructor(fileId:Long, cloudServiceUuid:String, path: String, lastUpdated:Date):this(){
        this.fileId = fileId
        this.cloudServiceUuid = cloudServiceUuid
        this.path = path
        this.lastUpdated = lastUpdated
    }
}