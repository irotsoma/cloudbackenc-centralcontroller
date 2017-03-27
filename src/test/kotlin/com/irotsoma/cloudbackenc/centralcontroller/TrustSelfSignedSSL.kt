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

package com.irotsoma.cloudbackenc.centralcontroller

import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Created by irotsoma on 8/4/2016.
 *
 * Run before making a call to a server with a self signed certificate.  Should only be used for testing.
 * From:  http://stackoverflow.com/a/7447273/1583160
 */

fun trustSelfSignedSSL() {
    try {
        val ctx = SSLContext.getInstance("TLS")
        val tm = object : X509TrustManager {
            override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
            }
            override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
            }
            override fun getAcceptedIssuers(): Array<out X509Certificate>? {
                return null
            }
        }
        ctx.init(null, arrayOf<TrustManager>(tm), null)
        SSLContext.setDefault(ctx)
    } catch (ex: Exception) {
        ex.printStackTrace()
    }

}