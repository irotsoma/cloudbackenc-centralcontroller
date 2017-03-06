/*
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceFactoryRepository
import com.irotsoma.cloudbackenc.centralcontroller.files.*
import com.irotsoma.cloudbackenc.common.FileMetadata
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceException
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Paths
import java.util.*

@RestController
@RequestMapping("/files")
class FileController {
    /** kotlin-logging implementation*/
    companion object: KLogging()
    @Autowired
    lateinit var cloudServiceFilesSettings: CloudServiceFilesSettings

    @Autowired
    lateinit var cloudServiceFileRepository: CloudServiceFileRepository

    @Autowired
    lateinit var cloudServiceFactoryRepository: CloudServiceFactoryRepository

    @Autowired
    lateinit var fileRepository: FileRepository

    @Autowired
    private lateinit var userAccountDetailsManager: UserAccountDetailsManager

    @RequestMapping(method = arrayOf(RequestMethod.POST), produces = arrayOf("application/json"))
    @ResponseBody fun receiveNewFile(@RequestParam("metadata") request: FileMetadata, @RequestParam("file") file: MultipartFile){

        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUser = userAccountDetailsManager.userRepository.findByUsername(authorizedUser.name) ?: throw CloudServiceException("Authenticated user could not be found.")
        var fileObject = fileRepository.findByOwnerUuidAndOwnerFileUuid(request.senderId.toString(),request.senderFileId.toString())

        if (fileObject!=null){
            if (((fileObject.cloudServiceFileList?.size ?:0) > cloudServiceFilesSettings.maxFileVersions) && ((fileObject.cloudServiceFileList?.size ?: 0) !=0) ) {
                //if there are already too many file versions, then delete the oldest one(s) (last one(s) due to order by statement)
                for (x in fileObject.cloudServiceFileList!!.size - 1 downTo cloudServiceFilesSettings.maxFileVersions) {
                    //find the cloud service file object id
                    val deleteItem = fileObject.cloudServiceFileList!![x].id ?: -1
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
                                val deleteSuccess = cloudServiceFactory.newInstance().cloudServiceFileIOService.delete(fileToDelete.locator, currentUser.cloudBackEncUser())
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
            fileObject = FileObject(fileUuid = UUID.randomUUID().toString(), ownerUuid = request.senderId.toString(), ownerFileUuid = request.senderFileId.toString(), cloudServiceFileList = null)
            fileRepository.saveAndFlush(fileObject)
        }
        val fileDistributor = FileDistributor()
        val serviceToSendTo = fileDistributor.determineBestLocation(file.size)
        val cloudServiceFactory = cloudServiceFactoryRepository.cloudServiceExtensions[serviceToSendTo]


        if (cloudServiceFactory == null) {
            //TODO: handle case where plugin is not installed or uuid is not valid


        } else {
            val tempFile = createTempFile(fileObject.fileUuid)
            file.transferTo(tempFile)
            //Make the path from the fileUuid + version number + the file Uuid used by the sender
            val cloudServiceFilePath = "/${fileObject.fileUuid}/${(fileObject.cloudServiceFileList?.size ?:0)+1}/${fileObject.ownerFileUuid}"
            //TODO: make these async
            val uploadSuccess = cloudServiceFactory.newInstance().cloudServiceFileIOService.upload(tempFile, Paths.get(cloudServiceFilePath), currentUser.cloudBackEncUser())
            if (uploadSuccess != null){
                val cloudServiceFile = CloudServiceFileObject(fileObject.id!!, serviceToSendTo.toString(), cloudServiceFilePath,Date())
                cloudServiceFileRepository.save(cloudServiceFile)
            }
            tempFile.deleteOnExit()
        }







        //TODO: wait for async processes (delete and upload as applicable) until timeout expires

    }







}