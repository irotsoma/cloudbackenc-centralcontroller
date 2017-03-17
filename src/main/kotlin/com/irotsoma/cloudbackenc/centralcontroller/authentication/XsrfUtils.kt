/**
 * Created by irotsoma on 3/17/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication

import java.math.BigInteger
import java.security.SecureRandom

/**
 *
 *
 * @author Justin Zak
 */
class XsrfUtils {

    /**
     * @return a new unique cross-site request forgery token
     */
    fun newToken(): String {
        return BigInteger(130, SecureRandom()).toString(32)
    }

    /**
     * Compares two cross-site request forgery tokens. Will always return false if the expected token is
     * null to prevent new session hijacking.

     * @param expectedToken the value of the original XSRF token
     * *
     * @param actualToken the value received from the client
     * *
     * @return true if the expected token is non-null and the tokens match, false otherwise
     */
    fun isValid(expectedToken: String?, actualToken: String): Boolean {
        return expectedToken != null && expectedToken == actualToken
    }

    companion object {
        val XSRF_KEY = "xsrf-token"
    }
}