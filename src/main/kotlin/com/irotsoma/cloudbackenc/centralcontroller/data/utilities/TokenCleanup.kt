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
 * Created by irotsoma on 10/26/2018.
 */
package com.irotsoma.cloudbackenc.centralcontroller.data.utilities

import com.irotsoma.cloudbackenc.centralcontroller.data.TokenRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*

/**
 * Period service to clean out any tokens that expired more than 1 day ago.
 */
@Component
class TokenCleanup {

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Scheduled(fixedDelayString = "\${centralcontroller.tokens.clean.interval}")
    fun doCleanup(){
        //find all tokens expired for more than 7 days
        val tokens = tokenRepository.findAllByExpirationDateLessThan(Date(Date().time - 1 * 24 * 3600 * 1000))
        //delete them all
        tokenRepository.deleteAll(tokens)
        tokenRepository.flush()
    }
}