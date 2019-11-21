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
 * Created by irotsoma on 10/20/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.controllers.exceptions

import com.irotsoma.cloudbackenc.common.RestException
import com.irotsoma.cloudbackenc.common.RestExceptionExceptions

/**
 * Custom exception for trying to create a duplicate user.
 */
class DuplicateUserException : RestException(RestExceptionExceptions.DUPLICATE_USER)
/**
 * Custom exception for trying to access /cloud-services with an invalid UUID
 */
class InvalidCloudServiceUuidException : RestException(RestExceptionExceptions.INVALID_CLOUD_SERVICE_UUID)

/**
 * Custom exception for trying to access a URL with a user parameter with an invalid user ID
 */
class CloudBackEncUserNotFound : RestException(RestExceptionExceptions.USER_NOT_FOUND)

/**
 * Custom exception for invalid email address format
 */
class InvalidEmailAddressException : RestException(RestExceptionExceptions.INVALID_EMAIL_ADDRESS)

/**
 * Custom exception for exceptions during the authentication process.  Used for internal server errors not for invalid credentials.
 */
class AuthenticationException : RestException(RestExceptionExceptions.AUTHENTICATION_EXCEPTION)

/**
 * Custom exception for an unauthorized user attempting to access a secure resource.
 */
class UserUnauthorizedException : RestException(RestExceptionExceptions.USER_UNAUTHORIZED)

/**
 * Custom exception for generic invalid request.
 */
class InvalidRequestException : RestException(RestExceptionExceptions.INVALID_REQUEST)

