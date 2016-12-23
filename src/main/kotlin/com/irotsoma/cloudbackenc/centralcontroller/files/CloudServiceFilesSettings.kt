/**
 * Created by irotsoma on 12/22/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 *
 *
 * @author Justin Zak
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("cloudservicefiles")
open class CloudServiceFilesSettings{
    var maxFileVersions: Int = 0
}