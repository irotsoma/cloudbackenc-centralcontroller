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
 * Created by irotsoma on 7/13/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers

import com.irotsoma.cloudbackenc.common.RestException
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceException
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import javax.servlet.http.HttpServletResponse

/**
 * Controller advice for custom exceptions.  Allows for customizing the messages returned to the REST client.
 */
@ControllerAdvice
class CloudServiceExceptionHandler : ResponseEntityExceptionHandler() {
    /**
     * Generates a message for instances of CloudServiceException thrown by any REST controllers.
     */
    @ExceptionHandler(CloudServiceException::class)
    fun handleCloudServiceException(response: HttpServletResponse, exception: CloudServiceException) : String?{
        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.message)
        return exception.message
    }
    /**
     * Generates a localized message for instances of RestException thrown by any REST controllers.
     */
    @ExceptionHandler(RestException::class)
    fun handleRestException(response: HttpServletResponse, exception: RestException) : String?{
        response.sendError(exception.type.httpStatusCode(),exception.type.friendlyMessage(LocaleContextHolder.getLocale()))
        return exception.type.friendlyMessage(LocaleContextHolder.getLocale())
    }
}