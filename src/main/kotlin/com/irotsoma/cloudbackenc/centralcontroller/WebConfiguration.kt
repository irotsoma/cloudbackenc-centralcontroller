/**
 * Created by irotsoma on 1/31/17.
 */
package com.irotsoma.cloudbackenc.centralcontroller

import org.h2.server.web.WebServlet
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 *
 *
 * @author Justin Zak
 */
@Configuration
class WebConfiguration {
    @Bean
    internal fun h2servletRegistration(): ServletRegistrationBean {
        val registrationBean = ServletRegistrationBean(WebServlet())
        registrationBean.addUrlMappings("/console/*")
        return registrationBean
    }
}