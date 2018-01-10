/*
 * Copyright (C) 2016-2018  Irotsoma, LLC
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
import com.irotsoma.cloudbackenc.centralcontroller.data.*
import com.irotsoma.cloudbackenc.centralcontroller.encryption.EncryptionExtensionRepository
import com.irotsoma.cloudbackenc.centralcontroller.files.CloudServiceFilesSettings
import com.irotsoma.cloudbackenc.centralcontroller.files.FileDistributor
import com.irotsoma.cloudbackenc.common.Utilities.hashFile
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceException
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceFactory
import com.irotsoma.cloudbackenc.common.encryption.EncryptionAlgorithms
import com.irotsoma.cloudbackenc.common.encryption.EncryptionAsymmetricEncryptionAlgorithms
import com.irotsoma.cloudbackenc.common.encryption.EncryptionFactory
import com.irotsoma.cloudbackenc.common.encryption.EncryptionSymmetricEncryptionAlgorithms
import mu.KLogging
import org.apache.commons.io.FilenameUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Paths
import java.security.Key
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

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

    @Autowired
    private lateinit var encryptionExtensionRepository: EncryptionExtensionRepository

    /**
     * Call to send a file to a cloud service.  Can be either a new file or a new version of an existing file.
     *
     * @param fileUuid Send only if this is to be a new version for an existing file.  Returned from previous call to this controller.
     * @param file The file to send to the cloud service.
     * @return A UUID for the file.  Must be sent in subsequent calls to identify a file as a new version of an existing file rather than a new file.
     */
    @RequestMapping(method = arrayOf(RequestMethod.POST), produces = arrayOf("application/json"))
    @ResponseBody fun receiveNewFile(@RequestParam("uuid") fileUuid: UUID?, @RequestParam("hash") hash:String?, @RequestParam("file") file: MultipartFile): ResponseEntity<Pair<UUID,Long>> {

        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUser = userAccountDetailsManager.userRepository.findByUsername(authorizedUser.name) ?: throw CloudServiceException("Authenticated user could not be found.")
        var fileObject: FileObject? = if (fileUuid != null) {
            fileRepository.findByFileUuid(fileUuid.toString())
        } else {
            null
        }

        if (fileObject!=null){
            val modifiableList = ArrayList(fileObject.cloudServiceFileList)
            if ((modifiableList.size > cloudServiceFilesSettings.maxFileVersions) && (modifiableList.size != 0)) {
                //if there are already too many file versions, then delete the oldest one(s) (first one(s) due to order by statement)
                var indexToDelete = 0
                while ((modifiableList.size > cloudServiceFilesSettings.maxFileVersions) && (indexToDelete < modifiableList.size)){
                    //find the cloud service file object id
                    val deleteItem = modifiableList[indexToDelete].id
                    if (deleteItem > 0) {
                        //find the object to be deleted
                        val fileToDelete = cloudServiceFileRepository.findById(deleteItem)
                        if (fileToDelete == null) {
                            //handle error in finding file in database gracefully by skipping deleting the file.
                            logger.error("Unable to find file with ID: $deleteItem")
                            indexToDelete++
                        } else {
                            val cloudServiceFactoryClass = cloudServiceFactoryRepository.extensions[UUID.fromString(fileToDelete.cloudServiceUuid)]
                            if (cloudServiceFactoryClass == null) {
                                logger.error("Unable to load cloud service factory with UUID: ${fileToDelete.cloudServiceUuid}")
                                indexToDelete++
                            } else {
                                //delete the file using the plugin service
                                try {
                                    val factory = cloudServiceFactoryClass.newInstance() as CloudServiceFactory
                                    val deleteSuccess = factory.cloudServiceFileIOService.delete(fileToDelete.locator, currentUser.cloudBackEncUser())
                                    if (deleteSuccess) {
                                        //delete the entry from the database
                                        cloudServiceFileRepository.delete(deleteItem)
                                        modifiableList.removeAt(indexToDelete)
                                    } else {
                                        logger.error("Unable to delete file from cloud service.  Cloud Service: ${factory.extensionName};  File Locator: ${fileToDelete.locator}")
                                        indexToDelete++
                                    }
                                } catch (e: Exception){
                                    logger.error("Unable to delete file from cloud service.  Factory: ${cloudServiceFactoryClass.canonicalName};  File locator: ${fileToDelete.locator}")
                                    indexToDelete++
                                }
                            }
                        }
                    }
                }
            }
        } else {
            fileObject = FileObject(fileUuid = UUID.randomUUID(), userId = currentUser.id, cloudServiceFileList = null)
            fileRepository.saveAndFlush(fileObject)
        }
        val fileDistributor = FileDistributor()
        val cloudServiceFactory = fileDistributor.determineBestLocation(currentUser, file.size)

        if (cloudServiceFactory == null) {
            logger.error("Unable find a cloud service to which to upload file with uuid: ${fileObject.fileUuid}.")
            return ResponseEntity(Pair(fileObject.fileUuid,-1L), HttpStatus.INTERNAL_SERVER_ERROR)
        } else {
            val tempFile = createTempFile(fileObject.fileUuid.toString())
            file.transferTo(tempFile)
            val originalHash = hashFile(tempFile)
            if ((!hash.isNullOrEmpty()) && (hash != originalHash)){
                logger.error("Transferred file does not match hash. File may have been corrupted during transfer.")
                return ResponseEntity(Pair(fileObject.fileUuid,-1L), HttpStatus.INTERNAL_SERVER_ERROR)
            }
            val secureRandom = SecureRandom.getInstanceStrong()

            val encryptionUuid = currentUser.defaultEncryptionProfile?.encryptionServiceUuid ?: UUID.fromString(encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid)
            val encryptionFactory = (encryptionExtensionRepository.extensions[encryptionUuid])?.newInstance() as EncryptionFactory? ?: encryptionExtensionRepository.extensions[UUID.fromString(encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid)]?.newInstance() as EncryptionFactory?
            if (encryptionFactory == null){
                logger.error("Unable to create the requested or the default encryption factory.")
                return ResponseEntity(Pair(fileObject.fileUuid,-1L), HttpStatus.INTERNAL_SERVER_ERROR)
            }
            var encryptionProfile = currentUser.defaultEncryptionProfile
            if (encryptionProfile == null){
                //if the user doesn't have a default encryption profile, then use the default settings of the extension
                logger.warn{"User ${currentUser.username} does not have a default encryption profile.  Using system defaults which may be insecure."}
                val secretKey = try {
                    encryptionFactory.encryptionKeyService.generateSymmetricKey()
                } catch (e: Exception){
                    logger.error("Unable to generate a key with the default key algorithm using the extension with UUID: ${encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid}")
                    return ResponseEntity(Pair(fileObject.fileUuid,-1L), HttpStatus.INTERNAL_SERVER_ERROR)
                }
                if (secretKey == null){
                    logger.error("Unable to generate a key with the default key algorithm using the extension with UUID: ${encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid}")
                    return ResponseEntity(Pair(fileObject.fileUuid,-1L), HttpStatus.INTERNAL_SERVER_ERROR)
                }
                val validAlgorithms = try {
                     EncryptionSymmetricEncryptionAlgorithms.values().filter { it.keyAlgorithm() == EncryptionSymmetricEncryptionAlgorithms.valueOf(secretKey.algorithm) }
                } catch(e: Exception){
                    logger.error("Unable to determine the default key algorithm or a list of valid encryption algorithms for the default key algorithm using the extension with UUID: ${encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid}")
                    return ResponseEntity(Pair(fileObject.fileUuid,-1L), HttpStatus.INTERNAL_SERVER_ERROR)
                }
                if (validAlgorithms.isEmpty()){
                    logger.error("Unable to determine a list of valid encryption algorithms for the default key algorithm using the extension with UUID: ${encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid}")
                    return ResponseEntity(Pair(fileObject.fileUuid,-1L), HttpStatus.INTERNAL_SERVER_ERROR)
                }
                //guess that the last valid algorithm is the most secure one as well as the last in the list of valid block sizes and default to that one
                encryptionProfile = EncryptionProfile(encryptionServiceUuid = null,encryptionIsSymmetric = true,encryptionAlgorithm = validAlgorithms.last().value,encryptionKeyAlgorithm = validAlgorithms.last().keyAlgorithm().value,encryptionBlockSize = validAlgorithms.last().validBlockSizes().last(), secretKey = secretKey.encoded, publicKey = null)
            }

            //load factory if it hasn't already been loaded
            val encryptionKey: Key
            val encryptionAlgorithm: EncryptionAlgorithms
            if (encryptionProfile.encryptionIsSymmetric) {
                encryptionAlgorithm = EncryptionSymmetricEncryptionAlgorithms.valueOf(encryptionProfile.encryptionAlgorithm)
                encryptionKey = SecretKeySpec(encryptionProfile.secretKey, encryptionProfile.encryptionKeyAlgorithm)
            } else {
                encryptionAlgorithm = EncryptionAsymmetricEncryptionAlgorithms.valueOf(encryptionProfile.encryptionAlgorithm)
                val x509publicKey = X509EncodedKeySpec(encryptionProfile.publicKey)
                encryptionKey = KeyFactory.getInstance(encryptionProfile.encryptionKeyAlgorithm).generatePublic(x509publicKey)
            }

            val encryptedFile = File.createTempFile(FilenameUtils.getName(tempFile.path), ".enc.tmp")
            var ivParameterSpec: IvParameterSpec? = null
            if (encryptionProfile.encryptionBlockSize < 1){
                val ivByteArray = ByteArray(encryptionProfile.encryptionBlockSize/8)
                secureRandom.nextBytes(ivByteArray)
                ivParameterSpec =  IvParameterSpec(ivByteArray)
            }

            encryptionFactory.encryptionFileService.encrypt(tempFile.inputStream(), encryptedFile.outputStream(), encryptionKey, encryptionAlgorithm, ivParameterSpec, secureRandom)

            val encryptedHash = hashFile(encryptedFile)


            // Make the path from the fileUuid + version number
            val fileVersion = (fileObject.cloudServiceFileList?.last()?.version ?:0) + 1
            val cloudServiceFilePath = "/${fileObject.fileUuid}/$fileVersion"
            val uploadSuccess = cloudServiceFactory.cloudServiceFileIOService.upload(tempFile, Paths.get(cloudServiceFilePath), currentUser.cloudBackEncUser())
            if (uploadSuccess != null){
                //if the file upload was successful, add the entry to the database
                val cloudServiceFile = CloudServiceFileObject(fileObject.fileUuid, cloudServiceFactory.extensionUuid.toString(), cloudServiceFilePath, fileVersion, Date(),encryptionProfile, ivParameterSpec?.iv, originalHash, encryptedHash)
                cloudServiceFileRepository.save(cloudServiceFile)
            }
            tempFile.deleteOnExit()
            return ResponseEntity(Pair(fileObject.fileUuid,fileVersion), HttpStatus.OK)
        }

    }
}