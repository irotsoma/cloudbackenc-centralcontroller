/*
 * Copyright (C) 2016-2020  Irotsoma, LLC
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
 * Created by irotsoma on 7/13/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers.exceptions

import com.irotsoma.cloudbackenc.common.RestException
import com.irotsoma.cloudbackenc.common.RestExceptionExceptions
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceException
import com.irotsoma.cloudbackenc.common.encryption.EncryptionException
import mu.KLogging
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import javax.servlet.http.HttpServletResponse

/**
 * Controller advice for custom exceptions.  Allows for customizing the messages returned to the REST client.
 */
@ControllerAdvice
class CustomExceptionHandler : ResponseEntityExceptionHandler() {
    /** kotlin-logging implementation */
    private companion object: KLogging()
    /**
     * Generates a message for instances of CloudServiceException thrown by any REST controllers.
     */
    @ExceptionHandler(CloudServiceException::class)
    fun handleCloudServiceException(response: HttpServletResponse, exception: CloudServiceException) : String?{
        logger.error("Cloud Service Exception: ${exception.message}")
        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.message)
        return exception.message
    }
    /**
     * Returns a RestExceptionException.
     */
    @ExceptionHandler(RestException::class)
    @ResponseBody
    fun handleRestException(response: HttpServletResponse, exception: RestException) : RestExceptionExceptions {
        logger.error("Rest Exception: ${exception.type.friendlyMessage(LocaleContextHolder.getLocale())}")
        response.status = exception.type.httpStatusCode()
        return exception.type
    }
    /**
     * Generates a message for instances of EncryptionException thrown by any REST controllers.
     */
    @ExceptionHandler(EncryptionException::class)
    fun handleEncryptionException(response: HttpServletResponse, exception: EncryptionException) : String?{
        logger.error("Encryption Exception: ${exception.message}")
        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.message)
        return exception.message
    }
}