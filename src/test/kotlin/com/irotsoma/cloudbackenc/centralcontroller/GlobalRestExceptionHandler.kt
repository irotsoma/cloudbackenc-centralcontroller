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
 * Created by irotsoma on 10/25/2019.
 */
package com.irotsoma.cloudbackenc.centralcontroller

import com.irotsoma.cloudbackenc.common.RestException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Used to interpret rest exceptions and return the friendly error message as a header. Also avoids JSON parsing exceptions by the rest template.
 */
@ControllerAdvice
internal class GlobalRestExceptionHandler {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RestException::class)
    fun <T : Any?> handle(exception: RestException, httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse, locale: Locale): ResponseEntity<T> {
        return ResponseEntity.badRequest().header("RestException",exception.type.friendlyMessage(locale)).build()
    }
}