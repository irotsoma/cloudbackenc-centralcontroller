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
 * Created by irotsoma on 11/21/2019.
 */
package com.irotsoma.cloudbackenc.centralcontroller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.irotsoma.cloudbackenc.common.RestExceptionExceptions
import org.apache.commons.io.IOUtils
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResponseErrorHandler
import java.io.StringWriter


class TestResponseErrorHandler: ResponseErrorHandler {
    var restException: RestExceptionExceptions? = null
    override fun hasError(httpResponse: ClientHttpResponse): Boolean {
        return (
                httpResponse.statusCode.series() == HttpStatus.Series.CLIENT_ERROR
                        || httpResponse.statusCode.series() == HttpStatus.Series.SERVER_ERROR)
    }

    override fun handleError(response: ClientHttpResponse) {
        val mapper = ObjectMapper().registerModule(KotlinModule())
        //copy body to a string as jackson readValue will close the stream
        val writer = StringWriter()
        IOUtils.copy(response.body, writer)
        restException = mapper.readValue<RestExceptionExceptions>(writer.toString())
    }

}