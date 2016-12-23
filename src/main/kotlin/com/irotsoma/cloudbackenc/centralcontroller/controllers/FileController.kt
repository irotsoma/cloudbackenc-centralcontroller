/*
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceFactoryRepository
import com.irotsoma.cloudbackenc.centralcontroller.files.CloudServiceFileRepository
import com.irotsoma.cloudbackenc.centralcontroller.files.CloudServiceFilesSettings
import com.irotsoma.cloudbackenc.centralcontroller.files.FileRepository
import com.irotsoma.cloudbackenc.common.FileMetadata
import com.irotsoma.cloudbackenc.common.logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/files")
open class FileController {
    companion object { val LOG by logger() }

    @Autowired
    lateinit var cloudServiceFilesSettings: CloudServiceFilesSettings

    @Autowired
    lateinit var cloudServiceFileRepository: CloudServiceFileRepository

    @Autowired
    lateinit var cloudServiceFactoryRepository: CloudServiceFactoryRepository

    @Autowired
    lateinit var fileRepository: FileRepository

    @RequestMapping(method = arrayOf(RequestMethod.POST), produces = arrayOf("application/json"))
    @ResponseBody fun receiveNewFile(@RequestParam("metadata") request: FileMetadata, @RequestParam("file") file: MultipartFile){
        val existingFile = fileRepository.findByOwnerUuidAndOwnerFileUuid(request.senderId.toString(),request.senderFileId.toString())

        if (existingFile!=null){
            if (((existingFile.cloudServiceFileList?.size ?:0) > cloudServiceFilesSettings.maxFileVersions) && ((existingFile.cloudServiceFileList?.size ?: 0) !=0) ) {
                //if there are already too many file versions, then delete the oldest one(s) (last one(s) due to order by statement)
                for (x in existingFile.cloudServiceFileList!!.size - 1 downTo cloudServiceFilesSettings.maxFileVersions) {
                    //find the cloud service file object id
                    val deleteItem = existingFile.cloudServiceFileList!![x].id ?: -1
                    if (deleteItem > 0) {
                        //find the object to be deleted
                        val fileToDelete = cloudServiceFileRepository.findById(deleteItem)
                        if (fileToDelete == null) {
                            //TODO: handle odd case where the item is not found (exception probably)


                        } else {
                            val cloudServiceFactory = cloudServiceFactoryRepository.cloudServiceExtensions[UUID.fromString(fileToDelete.cloudServiceUuid)]
                            if (cloudServiceFactory == null) {
                                //TODO: handle case where plugin is not installed or uuid is not valid


                            } else {
                                //TODO: create proper exception when path is null
                                //delete the file using the plugin service

                                //TODO: make these async
                                val deleteSuccess = cloudServiceFactory.newInstance().cloudServiceFileIOService.delete(fileToDelete.path!!)
                                if (deleteSuccess) {
                                    //delete the entry from the database
                                    cloudServiceFileRepository.delete(deleteItem)
                                }

                            }
                        }
                    }
                }

            }

        } else {
            //TODO: create new entry in file repository
        }
        //TODO: save file as temp file
        //TODO: upload file using file distributor (async)




        //TODO: wait for async processes (delete and upload as applicable) until timeout expires

    }







}