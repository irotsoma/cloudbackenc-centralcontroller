/*
 * Copyright (C) 2016-2018  Irotsoma, LLC
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
 * Created by irotsoma on 8/15/2016.
 */
package com.irotsoma.cloudbackenc.centralcontroller.authentication

import com.irotsoma.cloudbackenc.centralcontroller.authentication.jwt.StatelessAuthenticationFilter
import com.irotsoma.cloudbackenc.centralcontroller.data.UserAccount
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter


/**
 * Security configuration for REST controllers
 *
 * @author Justin Zak
 * @property userDetailsManager Autowired instance of user account manager
 * @property statelessAuthenticationFilter Autowired authentication filter that verifies the JWT token
 * @property restPath properties configurable path for the current version of the Rest API
 */
@Configuration
@EnableWebSecurity
class WebSecurityConfiguration : WebSecurityConfigurerAdapter() {

    @Value("\${centralcontroller.api.v1.path}")
    var restPath: String = ""
    @Autowired
    lateinit var userDetailsManager: UserAccountDetailsManager
    @Autowired
    lateinit var statelessAuthenticationFilter: StatelessAuthenticationFilter
    /** Adds the user account manager to REST controllers with a password encoder for hashing passwords */
    override fun configure(auth: AuthenticationManagerBuilder){
        auth.userDetailsService(this.userDetailsManager).passwordEncoder(UserAccount.PASSWORD_ENCODER)
    }
    /** Security configuration settings for REST controllers */
    override fun configure(http: HttpSecurity){
        http
            .authorizeRequests()
                .antMatchers("/h2-console/**").permitAll() //TODO: turn off access to H2 console
                //TODO: remove swagger paths if disabled
                .antMatchers("/swagger-ui.html").permitAll() //allow access to the swagger UI documentation
                .antMatchers("/webjars/springfox-swagger-ui/**").permitAll()
                .antMatchers("/swagger-resources/**").permitAll()
                .antMatchers("/v2/api-docs/**").permitAll()
                .antMatchers(HttpMethod.GET,"$restPath/cloud-services").permitAll() //allow access to list of installed cloud services
                //.anyRequest().authenticated() //but anything else requires authentication
                .and()
            .httpBasic() //allow basic username/password authentication
                .and()
            .headers()
                .frameOptions().disable() //needed to get h2 console working
                .and()
            //TODO:Add a filter before this that checks for the file controller certificate
            .addFilterBefore(statelessAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java) //add token authentication filter
            .csrf().disable()
    }
}