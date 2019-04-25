/*
 *  Copyright (C) 2019  Irotsoma, LLC
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.irotsoma.cloudbackenc"
version = "0.4-SNAPSHOT"

val commonVersion = "0.4-SNAPSHOT"
val apacheCommonsValidatorVersion = "1.6"
val apacheCommonsIoVersion = "1.3.2"
val kotlinLoggingVersion = "1.6.22"
val jjwtVersion = "0.9.1"
val jacksonVersion="2.9.8"
val swaggerVersion="2.9.2"
val javaMailVersion="1.6.3"
val junitVersion="5.5.0-M1"

plugins {
    val kotlinVersion = "1.3.30"
    val springBootVersion = "2.1.2.RELEASE"
    val liquibaseGradleVersion = "2.0.1"
    val dokkaVersion = "0.9.17"
    java
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
    id("com.github.hierynomus.license") version "0.15.0"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("org.jetbrains.dokka") version dokkaVersion
    signing
    id("org.springframework.boot") version springBootVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.noarg") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
    id("org.liquibase.gradle") version liquibaseGradleVersion
}

//try to pull repository credentials from either properties or environment variables
val repoUsername = project.findProperty("ossrhUsername")?.toString() ?: System.getenv("ossrhUsername") ?: ""
val repoPassword = project.findProperty("ossrhPassword")?.toString() ?: System.getenv("ossrhPassword") ?: ""

extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

repositories {
    mavenCentral()
    jcenter()
    gradlePluginPortal()
    //if the current project is a snapshot then allow using snapshot versions from maven central  Having this as an if prevents accidentally building a release version with snapshot dependencies.
    if (!(project.extra["isReleaseVersion"] as Boolean)) {
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

signing {
    setRequired({
        (project.extra["isReleaseVersion"] as Boolean)
    })
}

if (!(project.extra["isReleaseVersion"] as Boolean)) {
    configurations.all {
        // check for updates every build
        resolutionStrategy {
            cacheDynamicVersionsFor(0, "seconds")
            cacheChangingModulesFor(0, "seconds")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    //spring boot
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    implementation( "org.springframework.boot:spring-boot-starter-actuator")
    implementation( "org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation( "org.springframework.boot:spring-boot-starter-security")
    implementation( "org.springframework.boot:spring-boot-starter-data-jpa")
    implementation( "org.springframework.boot:spring-boot-starter-mail")
    //common classes
    implementation("com.irotsoma.cloudbackenc:common:$commonVersion")
    implementation( "com.irotsoma.cloudbackenc.common:cloudservices:$commonVersion")
    implementation( "com.irotsoma.cloudbackenc.common:encryption:$commonVersion")
    //jackson
    implementation( "com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation( "com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation( "com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation( "com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    //h2
    implementation( "com.h2database:h2")
    //mariaDb
    implementation( "org.mariadb.jdbc:mariadb-java-client")
    //liquibase
    implementation( "org.liquibase:liquibase-core")
    //apache commons
    implementation( "org.apache.commons:commons-io:$apacheCommonsIoVersion")
    implementation( "commons-validator:commons-validator:$apacheCommonsValidatorVersion")
    //jjwt
    implementation( "io.jsonwebtoken:jjwt:$jjwtVersion")
    //logging
    implementation( "io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    //swagger
    implementation( "io.springfox:springfox-swagger2:$swaggerVersion")
    implementation( "io.springfox:springfox-swagger-ui:$swaggerVersion")
    //java mail
    implementation( "com.sun.mail:jakarta.mail:$javaMailVersion")
    //test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

sourceSets["main"].resources.srcDirs.add(file("$buildDir/license-reports/"))

//exclude spring boot logging as it will conflict with the slf4j used by kotlin logging
configurations.all{
    exclude(module = "spring-boot-starter-logging")
    exclude(module = "logback-classic")
}

//this section downloads some reports regarding the licenses of various dependencies and includes them in the
// META-INF/licenses folder
license {
    ignoreFailures = true
    mapping("kt", "JAVADOC_STYLE")
    mapping("fxml", "XML_STYLE")
    excludes(arrayListOf<String>("**/*.json", "**/LICENSE", "**/*license*.html", "**/*license*.xml", "**/COPYING", "**/COPYING.LESSER", "**/*.jar", "**/*.dat", "**/*.p12"))
}
tasks.register<Copy>("copyLicenseReports") {
    from(file("$buildDir/reports/license/"))
    into(file("$buildDir/license-reports/META-INF/licenses"))
    mustRunAfter("downloadLicenses")
}
tasks.assemble{ 
    dependsOn("downloadLicenses") 
    dependsOn("copyLicenseReports") 
}

//javadoc stuff for Kotlin
val dokka by tasks.getting(DokkaTask::class) {
    outputDirectory = "$buildDir/docs/javadoc"
    jdkVersion = 8
    reportUndocumented = true
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.register<Jar>("javadocJar"){
    dependsOn("dokka")
    archiveClassifier.set("javadoc")
    from(dokka.outputDirectory)
}
kapt.includeCompileClasspath = false