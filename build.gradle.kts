group = "pathfinder"
version = "1.0"

plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.spring") version "2.2.10"
    kotlin("plugin.jpa") version "2.2.10"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
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
    implementation("com.h2database", "h2", "2.3.232")
    implementation("pw.chew", "jda-chewtils", "2.1")
    implementation("net.dv8tion", "JDA", "5.3.0")
    implementation("no.mather.ttrpg", "dice-syntax", "0.2.0")
    implementation("org.postgresql", "postgresql", "42.7.7")
    implementation("org.springframework.boot", "spring-boot-starter-actuator")
    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("org.springframework.boot", "spring-boot-starter-web")
}
