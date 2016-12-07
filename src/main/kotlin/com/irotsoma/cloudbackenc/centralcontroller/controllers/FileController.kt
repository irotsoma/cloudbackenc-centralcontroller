/*
 * Created by irotsoma on 12/6/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.centralcontroller.files.FileRepository
import com.irotsoma.cloudbackenc.common.FileMetadata
import com.irotsoma.cloudbackenc.common.logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/files")
open class FileController {
    companion object { val LOG by logger() }

    @Autowired
    lateinit var fileRepostitory: FileRepository

    @RequestMapping(method = arrayOf(RequestMethod.POST), produces = arrayOf("application/json"))
    @ResponseBody fun receiveNewFile(@RequestParam("metadata") request: FileMetadata, @RequestParam("file") file: MultipartFile){








    }







}