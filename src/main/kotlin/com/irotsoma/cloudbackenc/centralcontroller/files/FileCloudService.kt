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

    var fileId: Long? = null

    var cloudserviceUuid: String? = null

    var lastUpdated: Date? = null
}