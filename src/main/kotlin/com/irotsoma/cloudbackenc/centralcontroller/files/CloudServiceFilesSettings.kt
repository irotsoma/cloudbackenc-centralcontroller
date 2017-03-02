/**
 * Created by irotsoma on 12/22/16.
 */
package com.irotsoma.cloudbackenc.centralcontroller.files

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Settings related to cloud service files.
 *
 * @author Justin Zak
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("cloudservicefiles")
class CloudServiceFilesSettings{
    var maxFileVersions: Int = 0
}