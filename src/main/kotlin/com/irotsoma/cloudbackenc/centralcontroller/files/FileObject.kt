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

    @Column(name="file_uuid", unique=true,nullable=false)
    var fileUuid: String? = null

    @Column(name="owner_uuid", nullable = false)
    var ownerUuid: String? = null

    @Column(name="owner_file_uuid", nullable = false)
    var ownerFileUuid: String? = null

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "file_cloudservice", joinColumns = arrayOf(JoinColumn(name = "file_id", referencedColumnName = "id")))
    var cloudserviceList: List<FileCloudService>? = null

}