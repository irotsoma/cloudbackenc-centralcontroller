/*
 * Copyright (C) 2016-2017  Irotsoma, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

/*
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.authentication.UserAccountDetailsManager
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServiceFactoryRepository
import com.irotsoma.cloudbackenc.centralcontroller.files.*
import com.irotsoma.cloudbackenc.common.cloudservicesserviceinterface.CloudServiceException
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Paths
import java.util.*

/**
 * A REST controller for interacting with backup files.
 */
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

    /**
     * Call to send a file to a cloud service.  Can be either a new file or a new version of an existing file.
     *
     * @param fileUuid Send only if this is to be a new version for an existing file.  Returned from previous call to this controller.
     * @param file The file to send to the cloud service.
     * @returns A UUID for the file.  Must be sent in subsequent calls to identify a file as a new version of an existing file rather than a new file.
     */
    @RequestMapping(method = arrayOf(RequestMethod.POST), produces = arrayOf("application/json"))
    @ResponseBody fun receiveNewFile(@RequestParam("uuid") fileUuid: UUID?, @RequestParam("file") file: MultipartFile): ResponseEntity<UUID> {

        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUser = userAccountDetailsManager.userRepository.findByUsername(authorizedUser.name) ?: throw CloudServiceException("Authenticated user could not be found.")
        var fileObject = if (fileUuid != null) {
            fileRepository.findByFileUuid(fileUuid.toString())
        } else {
            null
        }

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
            fileObject = FileObject(fileUuid = UUID.randomUUID().toString(), userId = currentUser.id!!, cloudServiceFileList = null)
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
            //Make the path from the fileUuid + version number
            val cloudServiceFilePath = "/${fileObject.fileUuid}/${(fileObject.cloudServiceFileList?.size ?:0)+1}"
            //TODO: make these async
            val uploadSuccess = cloudServiceFactory.newInstance().cloudServiceFileIOService.upload(tempFile, Paths.get(cloudServiceFilePath), currentUser.cloudBackEncUser())
            if (uploadSuccess != null){
                val cloudServiceFile = CloudServiceFileObject(fileObject.fileUuid, serviceToSendTo.toString(), cloudServiceFilePath,Date())
                cloudServiceFileRepository.save(cloudServiceFile)
            }
            tempFile.deleteOnExit()
        }


        //TODO: wait for async processes (delete and upload as applicable) until timeout expires


        return ResponseEntity(UUID.fromString(fileObject.fileUuid), HttpStatus.OK)
    }







}