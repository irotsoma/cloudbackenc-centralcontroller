/*
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import javax.persistence.*

@Entity
@Table(name="files")
class FileObject() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var fileUuid: String? = null

    var ownerUuid: String? = null

    var ownerFileUuid: String? = null

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "file_cloudservice", joinColumns = arrayOf(JoinColumn(name = "fileId", referencedColumnName = "id")))
    var cloudserviceList: List<FileCloudService>? = null

}