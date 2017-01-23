/*
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import javax.persistence.*

@Entity
@Table(name="file")
class FileObject(@Column(name = "file_uuid", unique = true, nullable = false)
                 var fileUuid: String,

                 @Column(name="owner_uuid", nullable = false)
                 var ownerUuid: String,

                 @Column(name="owner_file_uuid", nullable = false)
                 var ownerFileUuid: String,

                 @ElementCollection(fetch = FetchType.EAGER)
                 @CollectionTable(name = "file_cloud_service", joinColumns = arrayOf(JoinColumn(name = "file_id", referencedColumnName = "id")))
                 @OrderBy("last_updated DESC")
                 var cloudServiceFileList: List<CloudServiceFileObject>?
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}