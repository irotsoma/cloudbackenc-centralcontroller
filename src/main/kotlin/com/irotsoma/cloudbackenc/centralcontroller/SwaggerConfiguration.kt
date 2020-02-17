/*
 * Copyright (C) 2016-2020  Irotsoma, LLC
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

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

/**
 * Configuration class for Swagger API documentation
 *
 * @property restV1Path properties configurable path for the current version of the Rest API
 */
@Configuration
@EnableSwagger2
class SwaggerConfiguration {

    //TODO:Make swagger docs externally toggleable
    @Value("\${centralcontroller.api.v1.path}")
    var restV1Path: String = "/"

    /** Sets the specific options for Swagger */
    @Bean
    fun api():Docket = Docket(DocumentationType.SWAGGER_2)
            .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.regex("$restV1Path.*"))
                .build().apiInfo(
                    ApiInfoBuilder().title("Central Controller REST Api")
                            .license("LGPL")
                            .licenseUrl("https://www.gnu.org/licenses/lgpl-3.0.html")
                            .version("1") //change api version if new version is added
                            .build()
            )
}