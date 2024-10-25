group = "pathfinder"
version = "1.0"

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    kotlin("plugin.jpa") version "1.9.23"
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://m2.chew.pro/releases") }
    maven {
        url = uri("https://maven.pkg.github.com/TheMather1/dice-syntax")
        credentials {
            username = "token"
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("com.h2database", "h2", "2.2.224")
    implementation("pw.chew", "jda-chewtils", "2.0")
    implementation("javax.servlet", "jstl", "1.2")
    implementation("net.dv8tion", "JDA", "5.0.0-beta.23")
    implementation("no.mather.ttrpg", "dice-syntax", "0.1.2")
    implementation("org.springframework.boot", "spring-boot-starter-actuator")
    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("org.springframework.boot", "spring-boot-starter-web")
//    implementation("org.springframework.boot", "spring-boot-starter-oauth2-client")
//    implementation("org.springframework.boot", "spring-boot-starter-web")
//    implementation("org.springframework.boot", "spring-boot-starter-tomcat")
//    implementation("org.apache.taglibs", "taglibs-standard-impl", "1.2.5")
//    implementation("org.apache.tomcat.embed", "tomcat-embed-jasper")
}