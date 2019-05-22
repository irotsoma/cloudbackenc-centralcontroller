/*
 * Copyright (C) 2016-2019  Irotsoma, LLC
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
import com.irotsoma.cloudbackenc.centralcontroller.cloudservices.CloudServicesSettings
import com.irotsoma.cloudbackenc.centralcontroller.data.*
import com.irotsoma.cloudbackenc.centralcontroller.encryption.EncryptionExtensionRepository
import com.irotsoma.cloudbackenc.centralcontroller.files.CloudServiceFilesSettings
import com.irotsoma.cloudbackenc.centralcontroller.files.FileDistributor
import com.irotsoma.cloudbackenc.common.Utilities.hashFile
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceException
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceFactory
import com.irotsoma.cloudbackenc.common.encryption.*
import mu.KLogging
import org.apache.commons.io.FilenameUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
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
 *
 * @author Justin Zak
 * @property cloudServiceFilesSettings Autowired settings for properties starting with cloudservicefiles
 * @property cloudServiceFileRepository JPA repository representing versioned files encrypted and sent to a cloud service
 * @property cloudServiceFactoryRepository Repository of cloud service extensions
 * @property fileRepository JPA repository representing files processed by the central controller
 * @property userAccountDetailsManager Autowired instance of user account manager
 * @property encryptionExtensionRepository Repository of encryption extensions
 */
@Lazy
@RestController
@RequestMapping("\${centralcontroller.api.v1.path}/cloud-services/files")
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

    @Autowired
    private lateinit var userRepository: UserAccountRepository

    @Autowired
    private lateinit var cloudServicesSettings: CloudServicesSettings
    /**
     * Call to send a file to a cloud service.  Can be either a new file or a new version of an existing file.
     *
     * @param fileUuid Send only if this is to be a new version for an existing file.  Returned from previous call to this controller.
     * @param file The file to send to the cloud service.
     * @return A UUID for the file.  Must be sent in subsequent calls to identify a file as a new version of an existing file rather than a new file.
     */
    @RequestMapping(method = [RequestMethod.POST], produces = ["application/json"])
    @Secured("ROLE_USER","ROLE_ADMIN")
    @ResponseBody fun receiveNewFile(@RequestParam("uuid") fileUuid: UUID?, @RequestParam("hash") hash:String?, @RequestParam("file") file: MultipartFile): ResponseEntity<Pair<UUID,Long>> {

        val authorizedUser = SecurityContextHolder.getContext().authentication
        val currentUser = userRepository.findByUsername(authorizedUser.name) ?: throw CloudServiceException("Authenticated user could not be found.")
        var fileObject: FileObject? = if (fileUuid != null) {
            fileRepository.findByFileUuid(fileUuid)
        } else {
            null
        }

        if (fileObject!=null){
            val fileList = ArrayList(fileObject.cloudServiceFileList)
            if ((fileList.size > cloudServiceFilesSettings.maxFileVersions) && (fileList.size != 0)) {
                //if there are already too many file versions, then delete the oldest one(s) (first one(s) due to order by statement)
                var indexToDelete = 0
                while ((fileList.size > cloudServiceFilesSettings.maxFileVersions) && (indexToDelete < fileList.size)){
                    //find the cloud service file object id
                    val deleteItem = fileList[indexToDelete].id
                    if (deleteItem > 0) {
                        //find the object to be deleted
                        val fileToDelete = cloudServiceFileRepository.findById(deleteItem)
                        if (!fileToDelete.isPresent) {
                            //handle error in finding file in database gracefully by skipping deleting the file.
                            logger.error("Unable to find file with ID: $deleteItem")
                            indexToDelete++
                        } else {
                            val cloudServiceUuid = UUID.fromString(fileToDelete.get().cloudServiceUuid)
                            val cloudServiceFactoryClass = cloudServiceFactoryRepository.extensions[cloudServiceUuid]
                            if (cloudServiceFactoryClass == null) {
                                logger.error("Unable to load cloud service factory with UUID: ${fileToDelete.get().cloudServiceUuid}")
                                indexToDelete++
                            } else {
                                //delete the file using the plugin service
                                try {
                                    val factory = cloudServiceFactoryClass.getDeclaredConstructor().newInstance() as CloudServiceFactory
                                    if (!cloudServicesSettings.cloudServicesSecrets[cloudServiceUuid.toString()]?.clientId.isNullOrBlank()){
                                        factory.additionalSettings["clientId"] = cloudServicesSettings.cloudServicesSecrets[cloudServiceUuid.toString()]?.clientId!!
                                    }
                                    if (!cloudServicesSettings.cloudServicesSecrets[cloudServiceUuid.toString()]?.clientSecret.isNullOrBlank()){
                                        factory.additionalSettings["clientSecret"] = cloudServicesSettings.cloudServicesSecrets[cloudServiceUuid.toString()]?.clientSecret!!
                                    }
                                    val deleteSuccess = factory.cloudServiceFileIOService.delete(fileToDelete.get().toCloudServiceFile(), currentUser.cloudBackEncUser())
                                    if (deleteSuccess) {
                                        //delete the entry from the database
                                        cloudServiceFileRepository.delete(fileToDelete.get())
                                        fileList.removeAt(indexToDelete)
                                    } else {
                                        logger.error("Unable to delete file from cloud service.  Cloud Service: ${factory.extensionName};  File Locator: ${fileToDelete.get().locator}")
                                        indexToDelete++
                                    }
                                } catch (e: Exception){
                                    logger.error("Unable to delete file from cloud service.  Factory: ${cloudServiceFactoryClass.canonicalName};  File locator: ${fileToDelete.get().locator}")
                                    indexToDelete++
                                }
                            }
                        }
                    }
                } //TODO: if it was unable to delete enough files to maintain the maxFileVersions, add a process to try again later and evenually just purge the db entries if it can never get rid of them.  Might even be good to push this whole thing to an async process to make attempts to get rid of the oldest first and if it can never get rid of it, then soft delete it from the DB and move on
            }
        } else {
            fileObject = FileObject(fileUuid = UUID.randomUUID(), userId = currentUser.id, cloudServiceFileList = null)
            fileRepository.saveAndFlush(fileObject)
        }
        val fileDistributor = FileDistributor()
        val erroredList = ArrayList<UUID>()
        var cloudServiceUuid = fileDistributor.determineBestLocation(currentUser, file.size)
        while (cloudServiceUuid != null) {
            val cloudServiceFactory = cloudServiceFactoryRepository.extensions[cloudServiceUuid]?.getDeclaredConstructor()?.newInstance() as CloudServiceFactory?
            if (cloudServiceFactory == null) {
                logger.error("Unable to load the cloud service extension with UUID: $cloudServiceUuid for file with UUID: ${fileObject.fileUuid}.")
                return ResponseEntity(Pair(fileObject.fileUuid, -1L), HttpStatus.INTERNAL_SERVER_ERROR)
            }
            if (!cloudServicesSettings.cloudServicesSecrets[cloudServiceUuid.toString()]?.clientId.isNullOrBlank()){
                cloudServiceFactory.additionalSettings["clientId"] = cloudServicesSettings.cloudServicesSecrets[cloudServiceUuid.toString()]?.clientId!!
            }
            if (!cloudServicesSettings.cloudServicesSecrets[cloudServiceUuid.toString()]?.clientSecret.isNullOrBlank()){
                cloudServiceFactory.additionalSettings["clientSecret"] = cloudServicesSettings.cloudServicesSecrets[cloudServiceUuid.toString()]?.clientSecret!!
            }
            val tempFile = createTempFile(fileObject.fileUuid.toString())
            file.transferTo(tempFile)

            val fileVersion = sendFile(cloudServiceFactory = cloudServiceFactory, fileObject = fileObject, currentUser = currentUser, localFile = tempFile, reportedHash = hash)
            if (fileVersion == -1L) {
                erroredList.add(cloudServiceUuid)
                cloudServiceUuid = fileDistributor.determineBestLocation(currentUser, file.size, erroredList)
            } else {
                return ResponseEntity(Pair(fileObject.fileUuid, fileVersion), HttpStatus.OK)
            }
        }
        logger.error("Unable find a cloud service to which to upload file with UUID: ${fileObject.fileUuid}.")
        return ResponseEntity(Pair(fileObject.fileUuid, -1L), HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private fun sendFile(fileObject:FileObject, cloudServiceFactory: CloudServiceFactory, currentUser: UserAccountObject, localFile: File, reportedHash: String?):Long{

        val originalHash = hashFile(localFile)
        if ((!reportedHash.isNullOrEmpty()) && (reportedHash != originalHash)){
            logger.error("Transferred file does not match hash. File may have been corrupted during transfer.")
            return -1L
        }
        val secureRandom = SecureRandom.getInstanceStrong()

        val encryptionUuid = currentUser.defaultEncryptionProfile?.encryptionServiceUuid ?: UUID.fromString(encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid)
        val encryptionFactory = (encryptionExtensionRepository.extensions[encryptionUuid])?.getDeclaredConstructor()?.newInstance() as EncryptionFactory? ?: encryptionExtensionRepository.extensions[UUID.fromString(encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid)]?.getDeclaredConstructor()?.newInstance() as EncryptionFactory?
        if (encryptionFactory == null){
            logger.error("Unable to create the requested or the default encryption factory.")
            return -1L
        }
        var encryptionProfile = currentUser.defaultEncryptionProfile
        if (encryptionProfile == null){
            //if the user doesn't have a default encryption profile, then use the default settings of the extension
            logger.warn{"User ${currentUser.username} does not have a default encryption profile.  Using system defaults which may be insecure."}
            val secretKey = try {
                encryptionFactory.encryptionKeyService.generateSymmetricKey()
            } catch (e: Exception){
                logger.error("Unable to generate a key with the default key algorithm using the extension with UUID: ${encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid}")
                return -1L
            }
            if (secretKey == null){
                logger.error("Unable to generate a key with the default key algorithm using the extension with UUID: ${encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid}")
                return -1L
            }
            val validAlgorithms = try {
                EncryptionSymmetricEncryptionAlgorithms.values().filter { it.keyAlgorithm() == EncryptionSymmetricKeyAlgorithms.valueOf(secretKey.algorithm) }
            } catch(e: Exception){
                logger.error("Unable to determine the default key algorithm or a list of valid encryption algorithms for the default key algorithm using the extension with UUID: ${encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid}")
                return -1L
            }
            if (validAlgorithms.isEmpty()){
                logger.error("Unable to determine a list of valid encryption algorithms for the default key algorithm using the extension with UUID: ${encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid}")
                return -1L
            }
            //guess that the last valid algorithm is the most secure one as well as the last in the list of valid block sizes and default to that one
            encryptionProfile = EncryptionProfileObject(null, EncryptionAlgorithmTypes.SYMMETRIC.value,encryptionAlgorithm = validAlgorithms.last().value,encryptionKeyAlgorithm = validAlgorithms.last().keyAlgorithm().value,encryptionBlockSize = validAlgorithms.last().validBlockSizes().last(), secretKey = secretKey.encoded, publicKey = null)
        }

        //load factory if it hasn't already been loaded
        val encryptionKey: Key
        val encryptionAlgorithm: EncryptionAlgorithms
        if (encryptionProfile.encryptionType == EncryptionAlgorithmTypes.SYMMETRIC.value) {
            encryptionAlgorithm = EncryptionSymmetricEncryptionAlgorithms.valueOf(encryptionProfile.encryptionAlgorithm)
            encryptionKey = SecretKeySpec(encryptionProfile.secretKey, encryptionProfile.encryptionKeyAlgorithm)
        } else {
            encryptionAlgorithm = EncryptionAsymmetricEncryptionAlgorithms.valueOf(encryptionProfile.encryptionAlgorithm)
            val x509publicKey = X509EncodedKeySpec(encryptionProfile.publicKey)
            encryptionKey = KeyFactory.getInstance(encryptionProfile.encryptionKeyAlgorithm).generatePublic(x509publicKey)
        }

        val encryptedFile = File.createTempFile(FilenameUtils.getName(localFile.path), ".enc.tmp")
        var ivParameterSpec: IvParameterSpec? = null
        if ((encryptionProfile.encryptionBlockSize ?:0) > 0){
            val ivByteArray = ByteArray((encryptionProfile.encryptionBlockSize ?:0)/8)
            secureRandom.nextBytes(ivByteArray)
            ivParameterSpec =  IvParameterSpec(ivByteArray)
        }

        encryptionFactory.encryptionStreamService.encrypt(localFile.inputStream(), encryptedFile.outputStream(), encryptionKey, encryptionAlgorithm, ivParameterSpec, secureRandom)

        val encryptedHash = hashFile(encryptedFile)

        // Make the path from the fileUuid + version number
        val fileVersion = (fileObject.cloudServiceFileList?.last()?.version ?:0) + 1
        val cloudServiceFilePath = "/${fileObject.fileUuid}/$fileVersion"
        val uploadResponse = cloudServiceFactory.cloudServiceFileIOService.upload(localFile, Paths.get(cloudServiceFilePath), currentUser.cloudBackEncUser())
        if (uploadResponse != null){
            //if the file upload was successful, add the entry to the database
            val cloudServiceFile = CloudServiceFileObject(fileObject.fileUuid, cloudServiceFactory.extensionUuid.toString(), uploadResponse.fileId ?: cloudServiceFilePath, cloudServiceFilePath, fileVersion, Date(),encryptionProfile, ivParameterSpec?.iv, originalHash, encryptedHash)
            cloudServiceFileRepository.save(cloudServiceFile)
        }
        localFile.deleteOnExit()
        return fileVersion
    }
}