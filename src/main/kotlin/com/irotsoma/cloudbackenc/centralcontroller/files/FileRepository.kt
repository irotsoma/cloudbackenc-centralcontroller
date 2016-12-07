/*
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import org.springframework.data.jpa.repository.JpaRepository

interface FileRepository : JpaRepository<FileObject, Long> {
    fun findByFileUuid(fileUuid: String): FileObject?
}